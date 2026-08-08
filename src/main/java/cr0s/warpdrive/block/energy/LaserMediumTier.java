package cr0s.warpdrive.block.energy;

import java.util.Locale;

/**
 * Laser-medium tiers from 1.12.2
 * {@code WarpDriveConfig.LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER}.
 *
 * The legacy array also contained a creative value at index zero, but there was no registered
 * creative laser-medium block. Only the three obtainable tiers belong here.
 */
public enum LaserMediumTier {

	BASIC(10_000, 0.5D),
	ADVANCED(30_000, 1.0D),
	SUPERIOR(100_000, 1.5D);

	private final int maxEnergyStored;
	private final double beamFactor;

	LaserMediumTier(final int maxEnergyStored, final double beamFactor) {
		this.maxEnergyStored = maxEnergyStored;
		this.beamFactor = beamFactor;
	}

	public int getMaxEnergyStored() {
		return maxEnergyStored;
	}

	/** Multiplier used by machines when a line of same-tier media is assembled. */
	public double getBeamFactor() {
		return beamFactor;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
