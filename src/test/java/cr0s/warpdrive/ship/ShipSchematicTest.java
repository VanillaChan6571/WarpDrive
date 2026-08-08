package cr0s.warpdrive.ship;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShipSchematicTest {

	@Test
	public void rotatesRelativePositionsClockwiseAroundTheCore() {
		final BlockPos source = new BlockPos(3, -2, 5);
		assertEquals(new BlockPos(3, -2, 5), ShipSchematic.rotateRelative(source, 0));
		assertEquals(new BlockPos(-5, -2, 3), ShipSchematic.rotateRelative(source, 1));
		assertEquals(new BlockPos(-3, -2, -5), ShipSchematic.rotateRelative(source, 2));
		assertEquals(new BlockPos(5, -2, -3), ShipSchematic.rotateRelative(source, 3));
		assertEquals(new BlockPos(5, -2, -3), ShipSchematic.rotateRelative(source, -1));
	}

	@Test
	public void normalizesOnlySafeSchematicNames() {
		assertEquals("my_ship", ShipSchematic.normalizeFileName("my_ship.schematic"));
		assertEquals("Explorer_One", ShipSchematic.sanitizeGeneratedName(" Explorer One "));
		assertEquals("ship", ShipSchematic.sanitizeGeneratedName("../../"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsPathTraversal() {
		ShipSchematic.normalizeFileName("../outside");
	}
}
