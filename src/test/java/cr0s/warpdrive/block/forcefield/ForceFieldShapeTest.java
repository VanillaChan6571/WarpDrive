package cr0s.warpdrive.block.forcefield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ForceFieldShapeTest {

	@Test
	public void cubeSeparatesSurfaceAndInterior() {
		final ForceFieldShape.Geometry geometry = ForceFieldShape.CUBE.calculate(
			-1, -1, -1, 1, 1, 1, 1.0F, true);
		assertEquals(26, geometry.perimeter.size());
		assertEquals(1, geometry.interior.size());
	}

	@Test
	public void planeAndTunnelRetainLegacyOpenSurfaces() {
		final ForceFieldShape.Geometry plane = ForceFieldShape.PLANE.calculate(
			-1, -1, -1, 1, 1, 1, 1.0F, true);
		assertEquals(18, plane.perimeter.size());
		assertEquals(9, plane.interior.size());

		final ForceFieldShape.Geometry tunnel = ForceFieldShape.TUNNEL.calculate(
			-1, -1, -1, 1, 1, 1, 1.0F, true);
		assertEquals(24, tunnel.perimeter.size());
		assertEquals(3, tunnel.interior.size());
	}

	@Test
	public void sphereUsesTheLegacyThicknessBand() {
		final ForceFieldShape.Geometry geometry = ForceFieldShape.SPHERE.calculate(
			-1, -1, -1, 1, 1, 1, 1.0F, true);
		assertEquals(18, geometry.perimeter.size());
		assertEquals(1, geometry.interior.size());
		assertTrue(ForceFieldShape.NONE.calculate(
			-1, -1, -1, 1, 1, 1, 1.0F, true).perimeter.isEmpty());
	}
}
