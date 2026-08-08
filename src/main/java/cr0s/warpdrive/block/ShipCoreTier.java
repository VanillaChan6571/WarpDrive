package cr0s.warpdrive.block;

import java.util.Locale;

/** 1.12.2 ship-core limits by tier. */
public enum ShipCoreTier {

	BASIC(500_000, 24, 64, 3_456),
	ADVANCED(10_000_000, 48, 1_728, 13_824),
	SUPERIOR(100_000_000, 96, 6_912, 110_592);

	private final int capacity;
	private final int maximumSide;
	private final int minimumMass;
	private final int maximumMass;

	ShipCoreTier(final int capacity, final int maximumSide,
	             final int minimumMass, final int maximumMass) {
		this.capacity = capacity;
		this.maximumSide = maximumSide;
		this.minimumMass = minimumMass;
		this.maximumMass = maximumMass;
	}

	public int getCapacity() { return capacity; }
	public int getMaximumSide() { return maximumSide; }
	public int getMinimumMass() { return minimumMass; }
	public int getMaximumMass() { return maximumMass; }
	public int getLegacyIndex() { return ordinal() + 1; }
	public String getName() { return name().toLowerCase(Locale.ROOT); }
}
