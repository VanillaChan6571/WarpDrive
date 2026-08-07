package cr0s.warpdrive.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShipMovementTypeTest {

	@Test
	public void legacyEnergyFactorsArePreserved() {
		assertEquals(6_100, ShipMovementType.PLANET_MOVING.energyRequired(100, 50));
		assertEquals(16_000, ShipMovementType.PLANET_TAKEOFF.energyRequired(100, 50));
		assertEquals(16_000, ShipMovementType.PLANET_LANDING.energyRequired(100, 50));
		assertEquals(7_000, ShipMovementType.SPACE_MOVING.energyRequired(100, 50));
		assertEquals(10_600, ShipMovementType.HYPERSPACE_MOVING.energyRequired(100, 50));
		assertEquals(10_000_000, ShipMovementType.HYPERSPACE_ENTERING.energyRequired(100, 50));
		assertEquals(10_000_000, ShipMovementType.HYPERSPACE_EXITING.energyRequired(100, 50));
	}

	@Test
	public void inverseEnergyRangeMatchesForwardCost() {
		assertEquals(50, ShipMovementType.PLANET_MOVING.maximumDistanceForEnergy(100, 6_100));
		assertEquals(49, ShipMovementType.PLANET_MOVING.maximumDistanceForEnergy(100, 6_099));
		assertEquals(0, ShipMovementType.HYPERSPACE_ENTERING.maximumDistanceForEnergy(100, 9_999_999));
		assertEquals(30_000_000,
			ShipMovementType.HYPERSPACE_ENTERING.maximumDistanceForEnergy(100, 10_000_000));
	}

	@Test
	public void massBasedRangeMatchesLegacyDefaults() {
		assertEquals(150, ShipMovementType.PLANET_MOVING.maximumDistance(1_000));
		assertEquals(200, ShipMovementType.SPACE_MOVING.maximumDistance(1_000));
		assertEquals(700, ShipMovementType.HYPERSPACE_MOVING.maximumDistance(1_000));
	}
}
