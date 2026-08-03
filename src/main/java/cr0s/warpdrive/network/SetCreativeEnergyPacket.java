package cr0s.warpdrive.network;

import cr0s.warpdrive.block.CreativeEnergyTileEntity;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * GUI -> server: set the creative energy source's output rate and face.
 */
public class SetCreativeEnergyPacket {

	private final BlockPos pos;
	private final int rate;
	private final int face;

	public SetCreativeEnergyPacket(final BlockPos pos, final int rate, final Direction face) {
		this.pos = pos;
		this.rate = rate;
		this.face = face.get3DDataValue();
	}

	private SetCreativeEnergyPacket(final BlockPos pos, final int rate, final int face) {
		this.pos = pos;
		this.rate = rate;
		this.face = face;
	}

	public static void encode(final SetCreativeEnergyPacket packet, final PacketBuffer buffer) {
		buffer.writeBlockPos(packet.pos);
		buffer.writeVarInt(packet.rate);
		buffer.writeByte(packet.face);
	}

	public static SetCreativeEnergyPacket decode(final PacketBuffer buffer) {
		return new SetCreativeEnergyPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readByte());
	}

	public static void handle(final SetCreativeEnergyPacket packet, final Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			final ServerPlayerEntity player = context.get().getSender();
			if (player == null) {
				return;
			}

			// Never trust the client: check range and clamp the value here rather than in the GUI
			if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) > 64.0) {
				DebugLog.log("ENERGY", "rejected configure from {} - too far from {}",
					player.getGameProfile().getName(), packet.pos);
				return;
			}

			final TileEntity tileEntity = player.level.getBlockEntity(packet.pos);
			if (!(tileEntity instanceof CreativeEnergyTileEntity)) {
				DebugLog.log("ENERGY", "rejected configure - no creative energy source at {}", packet.pos);
				return;
			}

			final int rate = Math.max(0, Math.min(CreativeEnergyTileEntity.MAX_RATE, packet.rate));
			final Direction direction = packet.face >= 0 && packet.face < Direction.values().length
				? Direction.from3DDataValue(packet.face)
				: Direction.DOWN;

			((CreativeEnergyTileEntity) tileEntity).configure(rate, direction);
		});
		context.get().setPacketHandled(true);
	}
}
