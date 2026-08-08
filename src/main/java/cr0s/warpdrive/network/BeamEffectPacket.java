package cr0s.warpdrive.network;

import cr0s.warpdrive.client.BeamEffectRenderer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client description of one laser beam. */
public final class BeamEffectPacket {

	private final Vector3d source;
	private final Vector3d target;
	private final float red;
	private final float green;
	private final float blue;
	private final int remainingEnergy;

	public BeamEffectPacket(final Vector3d source, final Vector3d target,
	                        final float red, final float green, final float blue,
	                        final int remainingEnergy) {
		this.source = source;
		this.target = target;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.remainingEnergy = remainingEnergy;
	}

	public static void encode(final BeamEffectPacket packet, final PacketBuffer buffer) {
		buffer.writeDouble(packet.source.x);
		buffer.writeDouble(packet.source.y);
		buffer.writeDouble(packet.source.z);
		buffer.writeDouble(packet.target.x);
		buffer.writeDouble(packet.target.y);
		buffer.writeDouble(packet.target.z);
		buffer.writeFloat(packet.red);
		buffer.writeFloat(packet.green);
		buffer.writeFloat(packet.blue);
		buffer.writeVarInt(Math.max(0, packet.remainingEnergy));
	}

	public static BeamEffectPacket decode(final PacketBuffer buffer) {
		return new BeamEffectPacket(
			new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
			new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
			buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt());
	}

	public static void handle(final BeamEffectPacket packet,
	                          final Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> BeamEffectRenderer.render(packet.source, packet.target,
				packet.red, packet.green, packet.blue, packet.remainingEnergy)));
		context.get().setPacketHandled(true);
	}
}
