package cr0s.warpdrive.network;

import cr0s.warpdrive.client.ShipCountdownHud;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: start or cancel a locally-rendered ship countdown. */
public final class ShipCountdownPacket {

	private final BlockPos corePos;
	private final long endGameTick;
	private final boolean active;

	public ShipCountdownPacket(final BlockPos corePos, final long endGameTick, final boolean active) {
		this.corePos = corePos;
		this.endGameTick = endGameTick;
		this.active = active;
	}

	public static ShipCountdownPacket start(final BlockPos corePos, final long endGameTick) {
		return new ShipCountdownPacket(corePos, endGameTick, true);
	}

	public static ShipCountdownPacket stop(final BlockPos corePos) {
		return new ShipCountdownPacket(corePos, 0L, false);
	}

	public static void encode(final ShipCountdownPacket packet, final PacketBuffer buffer) {
		buffer.writeBlockPos(packet.corePos);
		buffer.writeLong(packet.endGameTick);
		buffer.writeBoolean(packet.active);
	}

	public static ShipCountdownPacket decode(final PacketBuffer buffer) {
		return new ShipCountdownPacket(buffer.readBlockPos(), buffer.readLong(), buffer.readBoolean());
	}

	public static void handle(final ShipCountdownPacket packet,
	                          final Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> ShipCountdownHud.update(packet.corePos, packet.endGameTick, packet.active)));
		context.get().setPacketHandled(true);
	}
}
