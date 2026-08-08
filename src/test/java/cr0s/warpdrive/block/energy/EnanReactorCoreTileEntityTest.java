package cr0s.warpdrive.block.energy;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EnanReactorCoreTileEntityTest {

	@Test
	public void tiersKeepLegacyCapacityGenerationAndGeometry() {
		assertEquals(100_000_000, EnanReactorTier.BASIC.getMaxEnergyStored());
		assertEquals(500_000_000, EnanReactorTier.ADVANCED.getMaxEnergyStored());
		assertEquals(2_000_000_000, EnanReactorTier.SUPERIOR.getMaxEnergyStored());
		assertEquals(64_000, EnanReactorTier.BASIC.getGenerationMaximum());
		assertEquals(192_000, EnanReactorTier.ADVANCED.getGenerationMaximum());
		assertEquals(576_000, EnanReactorTier.SUPERIOR.getGenerationMaximum());
		assertEquals(0, EnanReactorTier.BASIC.getCenterYOffset());
		assertEquals(3, EnanReactorTier.ADVANCED.getCenterYOffset());
		assertEquals(4, EnanReactorTier.SUPERIOR.getCenterYOffset());
		assertEquals(4, EnanReactorFace.getLasers(EnanReactorTier.BASIC).size());
		assertEquals(8, EnanReactorFace.getLasers(EnanReactorTier.ADVANCED).size());
		assertEquals(16, EnanReactorFace.getLasers(EnanReactorTier.SUPERIOR).size());
		assertEquals(8, EnanReactorFace.getFaces(EnanReactorTier.BASIC).size());
		assertEquals(24, EnanReactorFace.getFaces(EnanReactorTier.ADVANCED).size());
		assertEquals(88, EnanReactorFace.getFaces(EnanReactorTier.SUPERIOR).size());
		assertEquals(new BlockPos(0, 0, -2),
			EnanReactorFace.byName("laser.basic.south").getOffset());
		assertEquals(new BlockPos(0, 0, -1),
			EnanReactorFace.byName("laser.basic.south.lens").getOffset());
		assertEquals(new BlockPos(4, 6, 2),
			EnanReactorFace.byName("laser.superior.west++").getOffset());
		assertSame(EnanReactorFace.UNKNOWN, EnanReactorFace.byName("invalid"));
	}

	@Test
	public void generationKeepsLegacyFiveTickEquation() {
		final double[] stable = new double[EnanReactorFace.MAX_INSTABILITIES];
		assertEquals(11, EnanReactorCoreTileEntity.calculateGeneration(
			EnanReactorTier.BASIC, 0, stable));

		final double[] critical = new double[EnanReactorFace.MAX_INSTABILITIES];
		for (int index = 0; index < critical.length; index++) critical[index] = 100.0D;
		assertEquals(320_000, EnanReactorCoreTileEntity.calculateGeneration(
			EnanReactorTier.BASIC, EnanReactorTier.BASIC.getMaxEnergyStored(), critical));
		assertEquals(960_000, EnanReactorCoreTileEntity.calculateGeneration(
			EnanReactorTier.ADVANCED, EnanReactorTier.ADVANCED.getMaxEnergyStored(), critical));
		assertEquals(2_880_000, EnanReactorCoreTileEntity.calculateGeneration(
			EnanReactorTier.SUPERIOR, EnanReactorTier.SUPERIOR.getMaxEnergyStored(), critical));
	}

	@Test
	public void instabilityAndLaserEquationsRetainBounds() {
		assertEquals(0.02D, EnanReactorCoreTileEntity.calculateInstabilityIncrease(
			0.0D, 50_000_000, 100_000_000), 1.0E-12D);
		assertEquals(0.30D, EnanReactorCoreTileEntity.calculateInstabilityIncrease(
			1.0D, 100_000_000, 100_000_000), 1.0E-12D);
		assertEquals(0.02D,
			EnanReactorCoreTileEntity.calculateNaturalStabilization(0.0D), 1.0E-12D);
		assertEquals(0.0D,
			EnanReactorCoreTileEntity.calculateLaserEffect(0, 0.5D, 1.0D), 1.0E-12D);
		assertEquals(0.060D * 20.0D / 0.33D,
			EnanReactorCoreTileEntity.calculateLaserEffect(200_000, 0.5D, 1.0D),
			1.0E-12D);
	}

	@Test
	public void outputPoliciesKeepLegacyGenerationCap() {
		assertEquals(0, EnanReactorCoreTileEntity.calculatePotentialOutput(
			EnanReactorOutputMode.UNLIMITED, 100_000, 1_000, 200, 0, true));
		assertEquals(1_800, EnanReactorCoreTileEntity.calculatePotentialOutput(
			EnanReactorOutputMode.UNLIMITED, 100_000, 1_000, 200, 0, false));
		assertEquals(400, EnanReactorCoreTileEntity.calculatePotentialOutput(
			EnanReactorOutputMode.ABOVE, 100_000, 1_000, 200, 600, false));
		assertEquals(700, EnanReactorCoreTileEntity.calculatePotentialOutput(
			EnanReactorOutputMode.AT_RATE, 100_000, 1_000, 200, 900, false));
		assertTrue(EnanReactorOutputMode.byName("at_rate") == EnanReactorOutputMode.AT_RATE);
	}
}
