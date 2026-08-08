package cr0s.warpdrive.block.building;

/** Legacy ship-builder limits, indexed by the three craftable scanner tiers. */
public enum ShipScannerTier {

	BASIC("basic", 24, 3_456),
	ADVANCED("advanced", 48, 13_824),
	SUPERIOR("superior", 96, 110_592);

	private final String name;
	private final int maxSide;
	private final int maxMass;

	ShipScannerTier(final String name, final int maxSide, final int maxMass) {
		this.name = name;
		this.maxSide = maxSide;
		this.maxMass = maxMass;
	}

	public String getName() {
		return name;
	}

	public int getMaxSide() {
		return maxSide;
	}

	public int getMaxMass() {
		return maxMass;
	}
}
