package cr0s.warpdrive.block.energy;

import java.util.Locale;

/**
 * Subspace capacitor tiers, from 1.12.2 WarpDriveConfig.CAPACITOR_* indexed by EnumTier.
 *
 * Input and output rates were separate config arrays but identical at every tier, so they are one
 * value here. Internal units map to FE 1:1, as elsewhere in this port.
 */
public enum CapacitorTier {

	BASIC(800_000, 800),
	ADVANCED(4_000_000, 4_000),
	SUPERIOR(20_000_000, 20_000),
	/** Creative: 1.12.2 gave it the superior buffer and an effectively unlimited transfer rate. */
	CREATIVE(20_000_000, Integer.MAX_VALUE / 2);

	private final int maxEnergyStored;
	private final int transferRate;

	CapacitorTier(final int maxEnergyStored, final int transferRate) {
		this.maxEnergyStored = maxEnergyStored;
		this.transferRate = transferRate;
	}

	public int getMaxEnergyStored() {
		return maxEnergyStored;
	}

	public int getTransferRate() {
		return transferRate;
	}

	public boolean isCreative() {
		return this == CREATIVE;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
