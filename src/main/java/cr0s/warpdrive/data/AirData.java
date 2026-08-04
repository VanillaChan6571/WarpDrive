package cr0s.warpdrive.data;

/**
 * Packed layout of the per-block air state, one int per block.
 *
 * Straight from 1.12.2 StateAir, where these constants lived alongside the stateful accessor. They
 * are split out here so the chunk storage can be built and tested without dragging in the whole
 * simulation - the layout itself is pure format with no behaviour.
 *
 * <pre>
 *   bits  0-4   concentration        0..31
 *   bits  5-7   generator direction  facing index, toward the generator
 *   bits  8-15  generator pressure   0..255, decreasing with distance from a source
 *   bits 16-23  void pressure        0..255, decreasing with distance from open space
 *   bits 24-26  void direction       facing index, toward the void
 *   bits 28-30  block classification see BLOCK_* below
 * </pre>
 *
 * The top bit is unused: Java has no unsigned int, and sign games here would be a needless hazard.
 */
public final class AirData {

	/** Default for a block we have not inspected yet - block classification is UNKNOWN. */
	public static final int AIR_DEFAULT = 0x060000C0;

	/** Bits actually in use; anything else is masked off on load to survive format changes. */
	public static final int USED_MASK                 = 0b01110111111111111111111100001111;

	public static final int CONCENTRATION_MASK        = 0b00000000000000000000000000011111;
	public static final int CONCENTRATION_MAX         = 0b00000000000000000000000000011111;
	public static final int GENERATOR_DIRECTION_MASK  = 0b00000000000000000000000011100000;
	public static final int GENERATOR_PRESSURE_MASK   = 0b00000000000000001111111100000000;
	public static final int VOID_PRESSURE_MASK        = 0b00000000111111110000000000000000;
	public static final int VOID_DIRECTION_MASK       = 0b00000111000000000000000000000000;
	public static final int BLOCK_MASK                = 0b01110000000000000000000000000000;

	public static final int GENERATOR_DIRECTION_SHIFT = 5;
	public static final int GENERATOR_PRESSURE_SHIFT  = 8;
	public static final int VOID_PRESSURE_SHIFT       = 16;
	public static final int VOID_DIRECTION_SHIFT      = 24;

	public static final int GENERATOR_PRESSURE_MAX    = 255;
	public static final int VOID_PRESSURE_MAX         = 255;

	/** Not inspected yet. */
	public static final int BLOCK_UNKNOWN             = 0b00000000000000000000000000000000;
	/** Any full, sealing block: stone and friends. */
	public static final int BLOCK_SEALER              = 0b00010000000000000000000000000000;
	/** Vanilla air or void, or modded replaceable air - an air block can be placed here. */
	public static final int BLOCK_AIR_PLACEABLE       = 0b00100000000000000000000000000000;
	/** A WarpDrive air flow block is already here. */
	public static final int BLOCK_AIR_FLOW            = 0b00110000000000000000000000000000;
	/** A WarpDrive air source block is here. */
	public static final int BLOCK_AIR_SOURCE          = 0b01000000000000000000000000000000;
	/** Sealed horizontally, leaks vertically - glass panes and similar. */
	public static final int BLOCK_AIR_NON_PLACEABLE_V = 0b01010000000000000000000000000000;
	/** Sealed vertically, leaks horizontally - enchantment tables, fluids, tilled dirt. */
	public static final int BLOCK_AIR_NON_PLACEABLE_H = 0b01100000000000000000000000000000;
	/** Leaks in every direction - crops, piping. */
	public static final int BLOCK_AIR_NON_PLACEABLE   = 0b01110000000000000000000000000000;

	/**
	 * A block is only worth ticking while it holds air or is under pressure. With all these bits
	 * clear there is nothing left to simulate, so the tick is skipped.
	 */
	public static final int TICKING_MASK = VOID_PRESSURE_MASK | GENERATOR_PRESSURE_MASK | CONCENTRATION_MASK;

	private AirData() {
	}

	/**
	 * True when this block carries nothing worth storing, so its chunk segment need not exist.
	 *
	 * Note the second clause: an air flow block is never "empty", even with no air left in it. The
	 * block is physically placed in the world, so its state has to be remembered or nothing would
	 * ever come back to remove it.
	 */
	public static boolean isEmptyData(final int dataAir) {
		return (dataAir & TICKING_MASK) == 0
		    && (dataAir & BLOCK_MASK) != BLOCK_AIR_FLOW;
	}

	public static int getConcentration(final int dataAir) {
		return dataAir & CONCENTRATION_MASK;
	}

	public static int getBlockType(final int dataAir) {
		return dataAir & BLOCK_MASK;
	}

	/**
	 * Facing index remap for one clockwise quarter turn: DOWN and UP are unchanged, and the four
	 * horizontals cycle. Index 6 is null and index 7 unused, both left alone.
	 */
	private static final int[] ROTATED_DIRECTION = { 0, 1, 5, 4, 2, 3, 6, 7 };

	private static int rotateDirection(final int direction, final int rotationSteps) {
		int result = direction;
		for (int step = 0; step < (rotationSteps & 3); step++) {
			result = ROTATED_DIRECTION[result & 7];
		}
		return result;
	}

	/**
	 * Rotate packed air state by quarter turns, for when a ship jumps with a rotation.
	 *
	 * Only the two stored directions move. Pressures and concentration are scalar and survive a
	 * rotation untouched - but the directions point back toward the generator and the void, so
	 * leaving them unrotated would send propagation the wrong way after the jump.
	 */
	public static int rotate(final int dataAir, final int rotationSteps) {
		if ((rotationSteps & 3) == 0) {
			return dataAir;
		}
		final int dataNoDirection = dataAir & ~(GENERATOR_DIRECTION_MASK | VOID_DIRECTION_MASK);
		final int directionGenerator = rotateDirection(
			(dataAir & GENERATOR_DIRECTION_MASK) >> GENERATOR_DIRECTION_SHIFT, rotationSteps);
		final int directionVoid = rotateDirection(
			(dataAir & VOID_DIRECTION_MASK) >> VOID_DIRECTION_SHIFT, rotationSteps);
		return dataNoDirection
		     | (directionGenerator << GENERATOR_DIRECTION_SHIFT)
		     | (directionVoid << VOID_DIRECTION_SHIFT);
	}
}
