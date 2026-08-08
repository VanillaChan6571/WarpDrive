package cr0s.warpdrive.item;

import cr0s.warpdrive.block.atomic.AcceleratorTier;
import cr0s.warpdrive.data.ParticleType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class PlasmaTorchStorageTest {

	@Test
	public void capacitiesMatchLegacyTiers() {
		assertEquals(200, PlasmaTorchStorage.capacity(AcceleratorTier.BASIC));
		assertEquals(400, PlasmaTorchStorage.capacity(AcceleratorTier.ADVANCED));
		assertEquals(800, PlasmaTorchStorage.capacity(AcceleratorTier.SUPERIOR));
	}

	@Test
	public void fillClampsAndRejectsMixedParticles() {
		assertEquals(200, PlasmaTorchStorage.fillTransfer(
			null, 0, ParticleType.ION, 250, 200));
		assertEquals(50, PlasmaTorchStorage.fillTransfer(
			ParticleType.ION, 150, ParticleType.ION, 100, 200));
		assertEquals(0, PlasmaTorchStorage.fillTransfer(
			ParticleType.ION, 100, ParticleType.PROTON, 100, 200));
	}

	@Test
	public void drainClampsAndRequiresMatchingParticles() {
		assertEquals(250, PlasmaTorchStorage.drainTransfer(
			ParticleType.ANTIMATTER, 300, ParticleType.ANTIMATTER, 250, 400));
		assertEquals(300, PlasmaTorchStorage.drainTransfer(
			ParticleType.ANTIMATTER, 300, ParticleType.ANTIMATTER, 500, 400));
		assertEquals(0, PlasmaTorchStorage.drainTransfer(
			ParticleType.ANTIMATTER, 300, ParticleType.ION, 500, 400));
	}

	@Test
	public void recipeConsumptionCannotUnderflow() {
		assertEquals(51, PlasmaTorchStorage.remainingAfterConsumption(75, 24, 200));
		assertEquals(0, PlasmaTorchStorage.remainingAfterConsumption(75, 100, 200));
	}

	@Test
	public void legacyParticleNamesRemainReadable() {
		assertSame(ParticleType.STRANGE_MATTER,
			PlasmaTorchStorage.parseParticleName("strange_matter"));
		assertSame(ParticleType.PROTON, PlasmaTorchStorage.parseParticleName("PROTON"));
		assertNull(PlasmaTorchStorage.parseParticleName("tachyons"));
		assertNull(PlasmaTorchStorage.parseParticleName(""));
	}
}
