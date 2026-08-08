package cr0s.warpdrive.data;

import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CelestialCoordinatesTest {

	@Test
	public void layersAreVerticallyContiguous() {
		final CelestialCoordinates.UniversalPosition overworld = CelestialCoordinates.toUniversal(
			new ResourceLocation(DimensionAltitude.OVERWORLD), 12.0D, 255.0D, -4.0D);
		final CelestialCoordinates.UniversalPosition space = CelestialCoordinates.toUniversal(
			new ResourceLocation(DimensionAltitude.SPACE), 12.0D, 0.0D, -4.0D);
		final CelestialCoordinates.UniversalPosition hyperspace = CelestialCoordinates.toUniversal(
			new ResourceLocation(DimensionAltitude.HYPERSPACE), 1.5D, 0.0D, -0.5D);
		assertNotNull(overworld);
		assertNotNull(space);
		assertNotNull(hyperspace);
		assertEquals(255.0D, overworld.y, 0.0D);
		assertEquals(256.0D, space.y, 0.0D);
		assertEquals(512.0D, hyperspace.y, 0.0D);
		assertEquals(space.x, hyperspace.x, 0.0D);
		assertEquals(space.z, hyperspace.z, 0.0D);
	}

	@Test
	public void hyperspaceUsesEightToOneHorizontalScale() {
		final ResourceLocation space = new ResourceLocation(DimensionAltitude.SPACE);
		final ResourceLocation hyperspace = new ResourceLocation(DimensionAltitude.HYPERSPACE);
		assertEquals(12, CelestialCoordinates.mapHorizontal(space, hyperspace, 100));
		assertEquals(-13, CelestialCoordinates.mapHorizontal(space, hyperspace, -100));
		assertEquals(96, CelestialCoordinates.mapHorizontal(hyperspace, space, 12));
	}

	@Test
	public void unrelatedDimensionsHaveNoUniversalPosition() {
		assertNull(CelestialCoordinates.toUniversal(new ResourceLocation("minecraft:the_end"), 0, 0, 0));
	}
}
