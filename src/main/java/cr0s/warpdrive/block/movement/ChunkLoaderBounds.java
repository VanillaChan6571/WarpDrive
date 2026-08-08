package cr0s.warpdrive.block.movement;

import java.util.Objects;

/** Pure rectangular-range and energy calculations shared by the live chunk loader and its tests. */
public final class ChunkLoaderBounds {

	public static final int MAX_RANGE_UPGRADES = 2;
	public static final int MAX_EFFICIENCY_UPGRADES = 5;
	public static final int ENERGY_PER_CHUNK = 8;
	private static final int MAX_REQUESTED_OFFSET = 1_000;

	private final int negativeX;
	private final int positiveX;
	private final int negativeZ;
	private final int positiveZ;

	private ChunkLoaderBounds(final int negativeX, final int positiveX,
	                          final int negativeZ, final int positiveZ) {
		this.negativeX = negativeX;
		this.positiveX = positiveX;
		this.negativeZ = negativeZ;
		this.positiveZ = positiveZ;
	}

	/** Normalises the legacy API: negative sides are stored negative, positive sides positive. */
	public static ChunkLoaderBounds fromRequested(final int negativeX, final int positiveX,
	                                               final int negativeZ, final int positiveZ) {
		return new ChunkLoaderBounds(-magnitude(negativeX), magnitude(positiveX),
			-magnitude(negativeZ), magnitude(positiveZ));
	}

	public static ChunkLoaderBounds centered(final int rangeUpgradeCount) {
		final int radius = clamp(rangeUpgradeCount, 0, MAX_RANGE_UPGRADES);
		return new ChunkLoaderBounds(-radius, radius, -radius, radius);
	}

	private static int magnitude(final int value) {
		return (int) Math.min(MAX_REQUESTED_OFFSET, Math.abs((long) value));
	}

	private static int clamp(final int value, final int minimum, final int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	public int area() {
		return (positiveX - negativeX + 1) * (positiveZ - negativeZ + 1);
	}

	public static int maximumArea(final int rangeUpgradeCount) {
		final int radius = clamp(rangeUpgradeCount, 0, MAX_RANGE_UPGRADES);
		final int diameter = 1 + 2 * radius;
		return diameter * diameter;
	}

	public boolean fits(final int rangeUpgradeCount) {
		return area() <= maximumArea(rangeUpgradeCount);
	}

	public int energyRequired(final int efficiencyUpgradeCount) {
		final int upgrades = clamp(efficiencyUpgradeCount, 0, MAX_EFFICIENCY_UPGRADES);
		return (int) Math.ceil((1.0D - 0.1D * upgrades) * area() * ENERGY_PER_CHUNK);
	}

	public int getNegativeX() { return negativeX; }
	public int getPositiveX() { return positiveX; }
	public int getNegativeZ() { return negativeZ; }
	public int getPositiveZ() { return positiveZ; }

	@Override
	public boolean equals(final Object object) {
		if (this == object) return true;
		if (!(object instanceof ChunkLoaderBounds)) return false;
		final ChunkLoaderBounds other = (ChunkLoaderBounds) object;
		return negativeX == other.negativeX && positiveX == other.positiveX
		    && negativeZ == other.negativeZ && positiveZ == other.positiveZ;
	}

	@Override
	public int hashCode() {
		return Objects.hash(negativeX, positiveX, negativeZ, positiveZ);
	}
}
