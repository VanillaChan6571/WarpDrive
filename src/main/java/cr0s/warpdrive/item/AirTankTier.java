package cr0s.warpdrive.item;

import java.util.Locale;

/**
 * Air tank tiers and their capacities, in breaths.
 *
 * Capacities are the 1.12.2 values from WarpDriveConfig.BREATHING_AIR_TANK_CAPACITY_BY_TIER,
 * { 20, 32, 64, 128 }, indexed by EnumTier where 0 was the creative tier - so the three craftable
 * tiers are 32/64/128. At 300 ticks per breath that is 8, 16 and 32 minutes of vacuum endurance.
 */
public enum AirTankTier {

	BASIC(32),
	ADVANCED(64),
	SUPERIOR(128);

	/** Breaths held when full - also the item's damage range, so the model overrides line up. */
	private final int capacity;

	AirTankTier(final int capacity) {
		this.capacity = capacity;
	}

	public int getCapacity() {
		return capacity;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
