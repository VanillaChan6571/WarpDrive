package cr0s.warpdrive.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CelestialBodyFeatureTest {

	@Test
	public void systemsAreDeterministicAndRemainInsideBuildHeight() {
		final CelestialSystemLayout.Body[] first = CelestialSystemLayout.systemBodies(123456789L, -2, 7);
		final CelestialSystemLayout.Body[] second = CelestialSystemLayout.systemBodies(123456789L, -2, 7);
		assertEquals(first.length, second.length);
		assertTrue(first.length == 2 || first.length == 3);

		for (int index = 0; index < first.length; index++) {
			assertEquals(first[index].x, second[index].x);
			assertEquals(first[index].y, second[index].y);
			assertEquals(first[index].z, second[index].z);
			assertEquals(first[index].radius, second[index].radius);
			assertEquals(first[index].moon, second[index].moon);
			assertTrue(first[index].y - first[index].radius >= 1);
			assertTrue(first[index].y + first[index].radius <= 254);
		}
		assertFalse(first[0].moon);
		for (int index = 1; index < first.length; index++) {
			assertTrue(first[index].moon);
		}
	}

	@Test
	public void moonsDoNotIntersectTheirPlanet() {
		for (int regionX = -4; regionX <= 4; regionX++) {
			for (int regionZ = -4; regionZ <= 4; regionZ++) {
				final CelestialSystemLayout.Body[] bodies =
					CelestialSystemLayout.systemBodies(987654321L, regionX, regionZ);
				for (int index = 1; index < bodies.length; index++) {
					final int dx = bodies[0].x - bodies[index].x;
					final int dy = bodies[0].y - bodies[index].y;
					final int dz = bodies[0].z - bodies[index].z;
					final double distance = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
					assertTrue(distance > bodies[0].radius + bodies[index].radius);
				}
			}
		}
	}
}
