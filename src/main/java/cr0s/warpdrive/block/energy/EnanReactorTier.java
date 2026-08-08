package cr0s.warpdrive.block.energy;

import java.util.Locale;

/**
 * Obtainable enantiomorphic-reactor tiers and their 1.12.2 balance constants.
 *
 * <p>The legacy configuration arrays also had a creative slot at index zero, but no creative
 * reactor core was registered. Keeping only the three real blocks avoids an unreachable tier
 * while preserving the values players could actually use.</p>
 */
public enum EnanReactorTier {

	BASIC(100_000_000, 6, 4, 64_000, 6, 0.10D, 3, 4.0F, 7.0F, 0),
	ADVANCED(500_000_000, 12, 4, 192_000, 8, 0.10D, 3, 5.0F, 9.0F, 3),
	SUPERIOR(2_000_000_000, 24, 4, 576_000, 10, 0.10D, 3, 6.0F, 11.0F, 4);

	private final int maxEnergyStored;
	private final int maxLasersPerSecond;
	private final int generationMinimum;
	private final int generationMaximum;
	private final int explosionRadius;
	private final double explosionRemovalChance;
	private final int explosionCount;
	private final float explosionStrengthMinimum;
	private final float explosionStrengthMaximum;
	private final int centerYOffset;

	EnanReactorTier(final int maxEnergyStored, final int maxLasersPerSecond,
	                final int generationMinimum, final int generationMaximum,
	                final int explosionRadius, final double explosionRemovalChance,
	                final int explosionCount, final float explosionStrengthMinimum,
	                final float explosionStrengthMaximum, final int centerYOffset) {
		this.maxEnergyStored = maxEnergyStored;
		this.maxLasersPerSecond = maxLasersPerSecond;
		this.generationMinimum = generationMinimum;
		this.generationMaximum = generationMaximum;
		this.explosionRadius = explosionRadius;
		this.explosionRemovalChance = explosionRemovalChance;
		this.explosionCount = explosionCount;
		this.explosionStrengthMinimum = explosionStrengthMinimum;
		this.explosionStrengthMaximum = explosionStrengthMaximum;
		this.centerYOffset = centerYOffset;
	}

	public int getMaxEnergyStored() { return maxEnergyStored; }
	public int getMaxLasersPerSecond() { return maxLasersPerSecond; }
	public int getGenerationMinimum() { return generationMinimum; }
	public int getGenerationMaximum() { return generationMaximum; }
	public int getExplosionRadius() { return explosionRadius; }
	public double getExplosionRemovalChance() { return explosionRemovalChance; }
	public int getExplosionCount() { return explosionCount; }
	public float getExplosionStrengthMinimum() { return explosionStrengthMinimum; }
	public float getExplosionStrengthMaximum() { return explosionStrengthMaximum; }
	public int getCenterYOffset() { return centerYOffset; }
	public String getName() { return name().toLowerCase(Locale.ROOT); }
}
