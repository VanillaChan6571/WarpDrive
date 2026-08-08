package cr0s.warpdrive.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloakPacketTest {

	@Test
	public void roundTripsBoundsAndMode() {
		final CloakPacket original = new CloakPacket(
			new BlockPos(-12, 80, 31), new BlockPos(-70, 4, -28),
			new BlockPos(46, 199, 92), true, false);
		final PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		CloakPacket.encode(original, buffer);

		final CloakPacket decoded = CloakPacket.decode(buffer);
		assertEquals(original.core, decoded.core);
		assertEquals(original.min, decoded.min);
		assertEquals(original.max, decoded.max);
		assertTrue(decoded.transparent);
		assertFalse(decoded.uncloaking);
	}
}
