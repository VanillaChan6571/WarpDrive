package cr0s.warpdrive.block.movement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransporterCoreTileEntityTest {

	@Test
	public void acquiringEnergyKeepsLegacyRangeAndScannerEquation() {
		assertEquals(30, TransporterCoreTileEntity.calculateAcquiringEnergy(0, 1));
		assertEquals(170, TransporterCoreTileEntity.calculateAcquiringEnergy(90, 5));
	}

	@Test
	public void energizingEnergyKeepsLegacyRangeAndEntityMassEquation() {
		assertEquals(10_000,
			TransporterCoreTileEntity.calculateEnergizingEnergy(0, 0.0D));
		assertEquals(40_000,
			TransporterCoreTileEntity.calculateEnergizingEnergy(90, 2.0D));
	}

	@Test
	public void persistedStateNamesRoundTrip() {
		for (final TransporterState state : TransporterState.values()) {
			assertEquals(state, TransporterState.byName(state.getSerializedName()));
		}
		assertEquals(TransporterState.DISABLED, TransporterState.byName("corrupt"));
	}
}
