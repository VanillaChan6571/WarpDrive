package cr0s.warpdrive.block.movement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChunkLoaderBoundsTest {

	@Test
	public void defaultLoaderOwnsAndChargesForItsChunk() {
		final ChunkLoaderBounds bounds = ChunkLoaderBounds.centered(0);
		assertEquals(1, bounds.area());
		assertEquals(8, bounds.energyRequired(0));
		assertTrue(bounds.fits(0));
	}

	@Test
	public void rangeUpgradesLimitAreaRatherThanIndividualAxes() {
		final ChunkLoaderBounds square = ChunkLoaderBounds.centered(2);
		final ChunkLoaderBounds strip = ChunkLoaderBounds.fromRequested(12, 12, 0, 0);
		final ChunkLoaderBounds tooLarge = ChunkLoaderBounds.fromRequested(12, 13, 0, 0);
		assertEquals(25, square.area());
		assertEquals(25, strip.area());
		assertTrue(strip.fits(2));
		assertFalse(tooLarge.fits(2));
	}

	@Test
	public void requestedSignsAndExtremeInputsAreNormalised() {
		final ChunkLoaderBounds bounds = ChunkLoaderBounds.fromRequested(
			5, -6, Integer.MIN_VALUE, 2_000);
		assertEquals(-5, bounds.getNegativeX());
		assertEquals(6, bounds.getPositiveX());
		assertEquals(-1_000, bounds.getNegativeZ());
		assertEquals(1_000, bounds.getPositiveZ());
	}

	@Test
	public void efficiencyUpgradesApplyTenPercentStepsAndClamp() {
		final ChunkLoaderBounds bounds = ChunkLoaderBounds.centered(1);
		assertEquals(72, bounds.energyRequired(0));
		assertEquals(36, bounds.energyRequired(5));
		assertEquals(36, bounds.energyRequired(99));
	}

	@Test
	public void rangeUpgradeAreaProgressionMatchesLegacyConfiguration() {
		assertEquals(1, ChunkLoaderBounds.maximumArea(0));
		assertEquals(9, ChunkLoaderBounds.maximumArea(1));
		assertEquals(25, ChunkLoaderBounds.maximumArea(2));
		assertEquals(25, ChunkLoaderBounds.maximumArea(99));
	}
}
