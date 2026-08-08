package cr0s.warpdrive.network;

import cr0s.warpdrive.client.ClientCloakManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client declaration or removal of one rectangular cloaked area. */
public final class CloakPacket {

	public final BlockPos core;
	public final BlockPos min;
	public final BlockPos max;
	public final boolean transparent;
	public final boolean uncloaking;

	public CloakPacket(final BlockPos core, final BlockPos min, final BlockPos max,
	                   final boolean transparent, final boolean uncloaking) {
		this.core = core.immutable();
		this.min = min.immutable();
		this.max = max.immutable();
		this.transparent = transparent;
		this.uncloaking = uncloaking;
	}

	public static void encode(final CloakPacket packet, final PacketBuffer buffer) {
		buffer.writeBlockPos(packet.core);
		buffer.writeBlockPos(packet.min);
		buffer.writeBlockPos(packet.max);
		buffer.writeBoolean(packet.transparent);
		buffer.writeBoolean(packet.uncloaking);
	}

	public static CloakPacket decode(final PacketBuffer buffer) {
		return new CloakPacket(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(),
			buffer.readBoolean(), buffer.readBoolean());
	}

	public static void handle(final CloakPacket packet,
	                          final Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> ClientCloakManager.handle(packet)));
		context.get().setPacketHandled(true);
	}
}
