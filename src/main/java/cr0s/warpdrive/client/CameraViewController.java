package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.detection.CameraBlock;
import cr0s.warpdrive.block.detection.MonitorTileEntity;
import cr0s.warpdrive.block.weapon.LaserCameraBlock;
import cr0s.warpdrive.data.CameraType;
import cr0s.warpdrive.data.VideoChannelRegistry;
import cr0s.warpdrive.network.CameraLaserFirePacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/** Client-only monitor view, zoom controls and laser-camera targeting. */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, value = Dist.CLIENT,
	bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraViewController {

	private static final ResourceLocation SIMPLE_OVERLAY = new ResourceLocation(
		WarpDrive.MODID, "textures/blocks/detection/camera-overlay.png");
	private static final ResourceLocation LASER_OVERLAY = new ResourceLocation(
		WarpDrive.MODID, "textures/blocks/weapon/laser_camera-overlay.png");
	private static final double[] ZOOM_DIVISORS = { 1.0D, 1.5D, 3.0D, 4.5D };
	private static final double[] SENSITIVITY_DIVISORS = { 2.0D, 3.0D, 6.0D, 9.0D };

	@Nullable private static BlockPos monitorPos;
	@Nullable private static VideoChannelRegistry.Endpoint endpoint;
	@Nullable private static ArmorStandEntity cameraProxy;
	@Nullable private static PointOfView previousPointOfView;
	private static double previousFov;
	private static double previousSensitivity;
	private static int zoomIndex;
	private static int bootTicks;
	private static int fireCooldown;

	private CameraViewController() { }

	public static void open(final BlockPos requestedMonitorPos, final int videoChannel) {
		final Minecraft minecraft = Minecraft.getInstance();
		final ClientPlayerEntity player = minecraft.player;
		if (player == null || minecraft.level == null) return;
		final VideoChannelRegistry.Endpoint requestedEndpoint =
			VideoChannelRegistry.find(minecraft.level, videoChannel);
		if (requestedEndpoint == null) {
			player.displayClientMessage(new TranslationTextComponent(
				"warpdrive.monitor.camera_not_found", videoChannel), false);
			return;
		}
		if (cameraProxy != null) exit();

		monitorPos = requestedMonitorPos.immutable();
		endpoint = requestedEndpoint;
		previousPointOfView = minecraft.options.getCameraType();
		previousFov = minecraft.options.fov;
		previousSensitivity = minecraft.options.sensitivity;
		zoomIndex = 0;
		bootTicks = 20;
		fireCooldown = 0;

		cameraProxy = new ArmorStandEntity(minecraft.level, 0.0D, 0.0D, 0.0D);
		cameraProxy.setInvisible(true);
		cameraProxy.setNoGravity(true);
		updateProxy(minecraft, player);
		minecraft.options.setCameraType(PointOfView.FIRST_PERSON);
		applyZoom(minecraft);
		minecraft.setCameraEntity(cameraProxy);
		player.displayClientMessage(new TranslationTextComponent("warpdrive.monitor.viewing_camera",
			videoChannel, requestedEndpoint.getBlockPos().getX(),
			requestedEndpoint.getBlockPos().getY(), requestedEndpoint.getBlockPos().getZ()), false);
	}

	private static void applyZoom(final Minecraft minecraft) {
		minecraft.options.fov = previousFov / ZOOM_DIVISORS[zoomIndex];
		minecraft.options.sensitivity = previousSensitivity / SENSITIVITY_DIVISORS[zoomIndex];
	}

	private static void zoom() {
		if (cameraProxy == null) return;
		zoomIndex = (zoomIndex + 1) % ZOOM_DIVISORS.length;
		applyZoom(Minecraft.getInstance());
	}

	private static void exit() {
		if (cameraProxy == null) return;
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) minecraft.setCameraEntity(minecraft.player);
		if (previousPointOfView != null) minecraft.options.setCameraType(previousPointOfView);
		minecraft.options.fov = previousFov;
		minecraft.options.sensitivity = previousSensitivity;
		cameraProxy = null;
		previousPointOfView = null;
		monitorPos = null;
		endpoint = null;
	}

	private static boolean validate(final Minecraft minecraft) {
		if (cameraProxy == null || monitorPos == null || endpoint == null
		 || minecraft.player == null || !minecraft.player.isAlive()
		 || minecraft.level == null || minecraft.getCameraEntity() != cameraProxy
		 || !minecraft.level.hasChunkAt(monitorPos)) return false;
		final TileEntity tileEntity = minecraft.level.getBlockEntity(monitorPos);
		return tileEntity instanceof MonitorTileEntity
		    && ((MonitorTileEntity) tileEntity).getVideoChannel() == endpoint.getVideoChannel()
		    && VideoChannelRegistry.isAlive(minecraft.level, endpoint);
	}

	private static void updateProxy(final Minecraft minecraft, final ClientPlayerEntity player) {
		if (cameraProxy == null || endpoint == null || minecraft.level == null) return;
		final BlockPos cameraPos = endpoint.getBlockPos();
		final BlockState blockState = minecraft.level.getBlockState(cameraPos);
		Direction facing = Direction.NORTH;
		if (blockState.hasProperty(CameraBlock.FACING)) {
			facing = blockState.getValue(CameraBlock.FACING);
		} else if (blockState.hasProperty(LaserCameraBlock.FACING)) {
			facing = blockState.getValue(LaserCameraBlock.FACING);
		}
		final Vector3d lens = Vector3d.atCenterOf(cameraPos).add(
			facing.getStepX() * 0.51D, facing.getStepY() * 0.51D, facing.getStepZ() * 0.51D);
		cameraProxy.setPosAndOldPos(lens.x, lens.y - cameraProxy.getEyeHeight(), lens.z);
		cameraProxy.yRotO = cameraProxy.yRot = player.yRot;
		cameraProxy.xRotO = cameraProxy.xRot = player.xRot;
		cameraProxy.setYHeadRot(player.yRot);
		cameraProxy.setYBodyRot(player.yRot);
	}

	@SubscribeEvent
	public static void onClientTick(final TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || cameraProxy == null) return;
		final Minecraft minecraft = Minecraft.getInstance();
		if (!validate(minecraft)) {
			exit();
			return;
		}
		if (bootTicks > 0) bootTicks--;
		updateProxy(minecraft, minecraft.player);
		if (endpoint != null && endpoint.getCameraType() == CameraType.LASER
		 && minecraft.options.keyJump.isDown()) {
			if (fireCooldown <= 0 && monitorPos != null) {
				WarpDriveNetwork.CHANNEL.sendToServer(new CameraLaserFirePacket(
					monitorPos, endpoint.getBlockPos(), minecraft.player.yRot, minecraft.player.xRot));
				fireCooldown = 10;
			} else {
				fireCooldown--;
			}
		} else {
			fireCooldown = 0;
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onInputUpdate(final InputUpdateEvent event) {
		if (cameraProxy == null || event.getPlayer() != Minecraft.getInstance().player) return;
		final MovementInput input = event.getMovementInput();
		input.leftImpulse = 0.0F;
		input.forwardImpulse = 0.0F;
		input.up = input.down = input.left = input.right = false;
		input.jumping = false;
		input.shiftKeyDown = false;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onClickInput(final InputEvent.ClickInputEvent event) {
		if (cameraProxy == null) return;
		if (event.isAttack()) {
			zoom();
			event.setSwingHand(false);
			event.setCanceled(true);
		} else if (event.isUseItem()) {
			if (bootTicks <= 0) exit();
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRenderOverlay(final RenderGameOverlayEvent.Post event) {
		if (cameraProxy == null || endpoint == null
		 || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
		final Minecraft minecraft = Minecraft.getInstance();
		final int width = event.getWindow().getGuiScaledWidth();
		final int height = event.getWindow().getGuiScaledHeight();
		final MatrixStack matrixStack = event.getMatrixStack();
		minecraft.getTextureManager().bind(endpoint.getCameraType() == CameraType.LASER
			? LASER_OVERLAY : SIMPLE_OVERLAY);
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		AbstractGui.blit(matrixStack, 0, 0, width, height,
			0.0F, 0.0F, 256, 256, 256, 256);
		final String controls = endpoint.getCameraType() == CameraType.LASER
			? "LMB: zoom  RMB: exit  Space: fire"
			: "LMB: zoom  RMB: exit";
		minecraft.font.drawShadow(matrixStack, controls,
			(width - minecraft.font.width(controls)) / 2.0F, height - 18.0F, 0xFFFFFFFF);
	}

	@SubscribeEvent
	public static void onLogout(final ClientPlayerNetworkEvent.LoggedOutEvent event) {
		exit();
		VideoChannelRegistry.clear();
	}
}
