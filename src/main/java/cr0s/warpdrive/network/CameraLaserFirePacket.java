package cr0s.warpdrive.network;

import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.detection.MonitorTileEntity;
import cr0s.warpdrive.block.weapon.LaserCameraTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Client camera controls -> server, with monitor proximity and channel validation. */
public final class CameraLaserFirePacket {

	private final BlockPos monitorPos;
	private final BlockPos cameraPos;
	private final float yaw;
	private final float pitch;

	public CameraLaserFirePacket(final BlockPos monitorPos, final BlockPos cameraPos,
	                             final float yaw, final float pitch) {
		this.monitorPos = monitorPos;
		this.cameraPos = cameraPos;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public static void encode(final CameraLaserFirePacket packet, final PacketBuffer buffer) {
		buffer.writeBlockPos(packet.monitorPos);
		buffer.writeBlockPos(packet.cameraPos);
		buffer.writeFloat(packet.yaw);
		buffer.writeFloat(packet.pitch);
	}

	public static CameraLaserFirePacket decode(final PacketBuffer buffer) {
		return new CameraLaserFirePacket(buffer.readBlockPos(), buffer.readBlockPos(),
			buffer.readFloat(), buffer.readFloat());
	}

	public static void handle(final CameraLaserFirePacket packet,
	                          final Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			final ServerPlayerEntity player = context.get().getSender();
			if (player == null || !Float.isFinite(packet.yaw) || !Float.isFinite(packet.pitch)
			 || player.distanceToSqr(packet.monitorPos.getX() + 0.5D,
				packet.monitorPos.getY() + 0.5D, packet.monitorPos.getZ() + 0.5D) > 64.0D
			 || !player.level.hasChunkAt(packet.monitorPos)
			 || !player.level.hasChunkAt(packet.cameraPos)) return;

			final TileEntity monitorTile = player.level.getBlockEntity(packet.monitorPos);
			final TileEntity cameraTile = player.level.getBlockEntity(packet.cameraPos);
			if (!(monitorTile instanceof MonitorTileEntity)
			 || !(cameraTile instanceof LaserCameraTileEntity)) return;
			final int monitorChannel = ((MonitorTileEntity) monitorTile).getVideoChannel();
			final LaserCameraTileEntity camera = (LaserCameraTileEntity) cameraTile;
			if (!IVideoChannel.isValid(monitorChannel)
			 || camera.getVideoChannel() != monitorChannel) return;

			camera.initiateBeamEmission(MathHelper.wrapDegrees(packet.yaw),
				MathHelper.clamp(packet.pitch, -90.0F, 90.0F));
		});
		context.get().setPacketHandled(true);
	}
}
