package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.data.ParticleType;
import cr0s.warpdrive.recipe.ParticleShapedRecipe;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AcceleratorCoreTileEntityTest {

	@Test
	public void collisionEnergySelectsLegacyParticleBands() {
		assertEquals(ParticleType.ION, AcceleratorCoreTileEntity.particleForEnergy(0.95D));
		assertEquals(ParticleType.PROTON, AcceleratorCoreTileEntity.particleForEnergy(5.0D));
		assertEquals(ParticleType.ANTIMATTER, AcceleratorCoreTileEntity.particleForEnergy(50.0D));
		assertEquals(ParticleType.STRANGE_MATTER,
			AcceleratorCoreTileEntity.particleForEnergy(150.0D));
	}

	@Test
	public void particleRecipesRetainOriginalConsumptionAmounts() {
		assertEquals(200, ParticleShapedRecipe.getRequiredAmount(ParticleType.ION));
		assertEquals(24, ParticleShapedRecipe.getRequiredAmount(ParticleType.PROTON));
		assertEquals(1_000, ParticleShapedRecipe.getRequiredAmount(ParticleType.ANTIMATTER));
		assertEquals(1_000, ParticleShapedRecipe.getRequiredAmount(ParticleType.STRANGE_MATTER));
	}
}
