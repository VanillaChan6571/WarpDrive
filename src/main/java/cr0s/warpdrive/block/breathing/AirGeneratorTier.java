package cr0s.warpdrive.block.breathing;

import java.util.Locale;

/**
 * Air generator tiers and their reach, from 1.12.2
 * WarpDriveConfig.BREATHING_AIR_GENERATION_RANGE_BLOCKS_BY_TIER = { 200, 16, 48, 144 }, indexed by
 * EnumTier where 0 was the creative tier.
 *
 * Range is in blocks of pressure, not a radius: it is the generator pressure seeded at the air
 * source, and propagation loses one per block. So a basic generator fills a volume 16 blocks deep
 * along whatever path the air can actually take, around corners included.
 */
public enum AirGeneratorTier {

	BASIC(16, 1400, 12, 4),
	ADVANCED(48, 21000, 180, 60),
	SUPERIOR(144, 304500, 2610, 870);

	private final int range;
	private final int maxEnergyStored;
	private final int energyPerNewAirBlock;
	private final int energyPerExistingAirBlock;

	AirGeneratorTier(final int range, final int maxEnergyStored,
	                 final int energyPerNewAirBlock, final int energyPerExistingAirBlock) {
		this.range = range;
		this.maxEnergyStored = maxEnergyStored;
		this.energyPerNewAirBlock = energyPerNewAirBlock;
		this.energyPerExistingAirBlock = energyPerExistingAirBlock;
	}

	public int getRange() {
		return range;
	}

	/**
	 * Buffer size, 1.12.2 BREATHING_MAX_ENERGY_STORED_BY_TIER. Sized for roughly six minutes of
	 * autonomy at the refresh cost, so a power cut is survivable rather than instantly fatal.
	 */
	public int getMaxEnergyStored() {
		return maxEnergyStored;
	}

	/** Cost to establish a new air source, 1.12.2 BREATHING_ENERGY_PER_NEW_AIR_BLOCK_BY_TIER. */
	public int getEnergyPerNewAirBlock() {
		return energyPerNewAirBlock;
	}

	/** Cost to keep an existing source alive - roughly a third of establishing one. */
	public int getEnergyPerExistingAirBlock() {
		return energyPerExistingAirBlock;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
