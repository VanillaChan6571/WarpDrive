package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cr0s.warpdrive.WarpDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Persistent textured laser beams, ported from the 1.12.2 {@code EntityFXBeam}. */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, value = Dist.CLIENT,
	bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BeamEffectRenderer {

	private static final ResourceLocation TEXTURE =
		new ResourceLocation(WarpDrive.MODID, "textures/particle/energy_grey.png");
	private static final double MAX_VISIBLE_DISTANCE_SQUARED = 300.0D * 300.0D;
	private static final double HALF_WIDTH = 0.15D;
	private static final int PLANE_COUNT = 3;
	private static final List<Beam> BEAMS = new ArrayList<>();
	private static ClientWorld activeWorld;

	private BeamEffectRenderer() {
	}

	public static void render(final Vector3d source, final Vector3d target,
	                          final float red, final float green, final float blue,
	                          final int remainingEnergy, final int durationTicks) {
		final ClientWorld world = Minecraft.getInstance().level;
		if (world == null || source.distanceToSqr(target) <= 1.0E-8D) return;
		if (activeWorld != world) {
			BEAMS.clear();
			activeWorld = world;
		}
		BEAMS.add(new Beam(source, target, clampColor(red), clampColor(green),
			clampColor(blue), remainingEnergy, Math.max(1, durationTicks)));
	}

	@SubscribeEvent
	public static void onClientTick(final TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) return;
		final ClientWorld world = Minecraft.getInstance().level;
		if (world == null || activeWorld != world) {
			BEAMS.clear();
			activeWorld = world;
			return;
		}
		BEAMS.removeIf(Beam::tick);
	}

	@SubscribeEvent
	public static void onRenderWorldLast(final RenderWorldLastEvent event) {
		if (BEAMS.isEmpty()) return;
		final Minecraft minecraft = Minecraft.getInstance();
		final ClientWorld world = minecraft.level;
		if (world == null || activeWorld != world) {
			BEAMS.clear();
			activeWorld = world;
			return;
		}

		final Vector3d camera = minecraft.gameRenderer.getMainCamera().getPosition();
		final MatrixStack matrixStack = event.getMatrixStack();
		matrixStack.pushPose();
		matrixStack.translate(-camera.x, -camera.y, -camera.z);

		RenderSystem.enableTexture();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		minecraft.getTextureManager().bind(TEXTURE);
		minecraft.getTextureManager().getTexture(TEXTURE).setFilter(true, false);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_REPEAT);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_REPEAT);

		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder buffer = tessellator.getBuilder();
		final Matrix4f matrix = matrixStack.last().pose();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		final double time = world.getGameTime() + event.getPartialTicks();
		final Iterator<Beam> iterator = BEAMS.iterator();
		while (iterator.hasNext()) {
			final Beam beam = iterator.next();
			if (beam.isVisibleFrom(camera)) beam.writeVertices(buffer, matrix, time,
				event.getPartialTicks());
		}
		tessellator.end();

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		minecraft.getTextureManager().bind(AtlasTexture.LOCATION_BLOCKS);
		matrixStack.popPose();
	}

	private static void vertex(final BufferBuilder buffer, final Matrix4f matrix,
	                           final Vector3d position, final float u, final float v,
	                           final float red, final float green, final float blue,
	                           final float alpha) {
		buffer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
			.uv(u, v).color(red, green, blue, alpha).endVertex();
	}

	private static float clampColor(final float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static final class Beam {
		private final Vector3d source;
		private final Vector3d target;
		private final float red;
		private final float green;
		private final float blue;
		private final int maxAge;
		private final double widthScale;
		private int age;

		private Beam(final Vector3d source, final Vector3d target,
		             final float red, final float green, final float blue,
		             final int remainingEnergy, final int maxAge) {
			this.source = source;
			this.target = target;
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.maxAge = maxAge;
			widthScale = remainingEnergy > 900_000 ? 1.5D : 1.0D;
		}

		/** @return true once the beam has expired. */
		private boolean tick() {
			return ++age >= maxAge;
		}

		private boolean isVisibleFrom(final Vector3d camera) {
			return source.distanceToSqr(camera) <= MAX_VISIBLE_DISTANCE_SQUARED
			    || target.distanceToSqr(camera) <= MAX_VISIBLE_DISTANCE_SQUARED;
		}

		private void writeVertices(final BufferBuilder buffer, final Matrix4f matrix,
		                           final double worldTime, final float partialTicks) {
			final Vector3d fullDelta = target.subtract(source);
			final double length = fullDelta.length();
			if (length <= 1.0E-4D) return;
			final Vector3d direction = fullDelta.scale(1.0D / length);
			final Vector3d reference = Math.abs(direction.y) < 0.9D
				? new Vector3d(0.0D, 1.0D, 0.0D) : new Vector3d(1.0D, 0.0D, 0.0D);
			final Vector3d perpendicularA = direction.cross(reference).normalize();
			final Vector3d perpendicularB = direction.cross(perpendicularA).normalize();

			final float interpolatedAge = age + partialTicks;
			final double size = Math.min(interpolatedAge / 4.0D, 1.0D);
			if (size <= 0.0D) return;
			final Vector3d visibleTarget = source.add(fullDelta.scale(size));
			final float alpha = maxAge - interpolatedAge <= 4.0F
				? MathHelper.clamp(0.1F * (maxAge - interpolatedAge), 0.1F, 0.5F) : 0.5F;
			final float vMin = (float) (-worldTime * 0.2D - Math.floor(-worldTime * 0.1D));
			final float vMax = vMin + (float) (length * size);
			final double rotation = Math.toRadians((worldTime * 20.0D) % 360.0D);
			final double halfWidth = HALF_WIDTH * widthScale * size;

			for (int plane = 0; plane < PLANE_COUNT; plane++) {
				final double angle = rotation + plane * Math.PI / PLANE_COUNT;
				final Vector3d offset = perpendicularA.scale(Math.cos(angle) * halfWidth)
					.add(perpendicularB.scale(Math.sin(angle) * halfWidth));
				vertex(buffer, matrix, source.subtract(offset), 0.0F, vMin,
					red, green, blue, alpha);
				vertex(buffer, matrix, visibleTarget.subtract(offset), 0.0F, vMax,
					red, green, blue, alpha);
				vertex(buffer, matrix, visibleTarget.add(offset), 1.0F, vMax,
					red, green, blue, alpha);
				vertex(buffer, matrix, source.add(offset), 1.0F, vMin,
					red, green, blue, alpha);
			}
		}
	}
}
