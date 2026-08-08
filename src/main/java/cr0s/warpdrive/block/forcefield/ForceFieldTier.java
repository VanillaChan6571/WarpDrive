package cr0s.warpdrive.block.forcefield;

import java.util.Locale;

/** Legacy force-field tier constants, with internal energy mapped directly to FE. */
public enum ForceFieldTier {

	BASIC(30_000, 25.0F, 100.0F),
	ADVANCED(90_000, 50.0F, 150.0F),
	SUPERIOR(150_000, 80.0F, 200.0F);

	private final int maxEnergyStored;
	private final float hardness;
	private final float blastResistance;

	ForceFieldTier(final int maxEnergyStored, final float hardness, final float blastResistance) {
		this.maxEnergyStored = maxEnergyStored;
		this.hardness = hardness;
		this.blastResistance = blastResistance;
	}

	public int getMaxEnergyStored() {
		return maxEnergyStored;
	}

	public float getHardness() {
		return hardness;
	}

	public float getBlastResistance() {
		return blastResistance;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
