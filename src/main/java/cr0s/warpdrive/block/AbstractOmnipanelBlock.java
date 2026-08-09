package cr0s.warpdrive.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A panel that grows into flush sheets, ported from 1.12.2 BlockAbstractOmnipanel.
 *
 * The panel is a thin plane through the middle of the block, present in whichever of the three
 * orthogonal planes its neighbours imply, built from twelve quadrants - three planes of four
 * corners. A corner fills only when both its directions connect AND either both neighbours present
 * a solid face or the diagonal between them connects. That last clause is what keeps a panel
 * meeting stone on one side and a panel above it a beam rather than a sheet.
 *
 * <b>This block has no blockstate properties at all.</b> Connectivity is render data, not game
 * state: 1.12.2 carried it in unlisted properties and built geometry at draw time, and the 1.16.5
 * equivalent is IModelData, which Forge fills in from BlockModelRenderer.renderModel for every
 * block during a section rebuild - tile entity or not.
 *
 * That matters beyond tidiness. Six connections as properties costs 64 states and still cannot
 * express the diagonal term; adding the twelve quadrants would be 262144. Blockstate would also
 * have to be kept current by hand, and vanilla never notifies a block about its diagonals, so every
 * diagonal change would need a manual fan-out. As render data none of that exists: setBlockDirty
 * already expands one block on every axis and dirties each intersecting section, so the twelve edge
 * diagonals rebuild on their own.
 *
 * The shape is derived from the world for the same reason, via Properties.dynamicShape().
 */
public abstract class AbstractOmnipanelBlock extends Block {

	/** The panel plane sits between 7/16 and 9/16, as in 1.12.2. */
	public static final double CENTER_MIN = 7.0D;
	public static final double CENTER_MAX = 9.0D;

	/** Bits 6-17 of the render mask: one per quadrant, in QUADRANT_NAMES order. */
	public static final int QUADRANT_SHIFT = 6;

	/**
	 * The twelve corners, named by the two directions forming each. The order is fixed - the model
	 * layer indexes into it by bit position.
	 */
	public static final String[] QUADRANT_NAMES = {
		"xn_yn", "xp_yn", "xn_yp", "xp_yp",
		"xn_zn", "xp_zn", "xn_zp", "xp_zp",
		"zn_yn", "zp_yn", "zn_yp", "zp_yp" };

	private static final Direction[][] QUADRANT_DIRECTIONS = {
		{ Direction.WEST, Direction.DOWN },  { Direction.EAST, Direction.DOWN },
		{ Direction.WEST, Direction.UP },    { Direction.EAST, Direction.UP },
		{ Direction.WEST, Direction.NORTH }, { Direction.EAST, Direction.NORTH },
		{ Direction.WEST, Direction.SOUTH }, { Direction.EAST, Direction.SOUTH },
		{ Direction.NORTH, Direction.DOWN }, { Direction.SOUTH, Direction.DOWN },
		{ Direction.NORTH, Direction.UP },   { Direction.SOUTH, Direction.UP } };

	private static final double[][] QUADRANT_BOXES = {
		{ 0, 0, CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX },
		{ CENTER_MAX, 0, CENTER_MIN, 16, CENTER_MIN, CENTER_MAX },
		{ 0, CENTER_MAX, CENTER_MIN, CENTER_MIN, 16, CENTER_MAX },
		{ CENTER_MAX, CENTER_MAX, CENTER_MIN, 16, 16, CENTER_MAX },
		{ 0, CENTER_MIN, 0, CENTER_MIN, CENTER_MAX, CENTER_MIN },
		{ CENTER_MAX, CENTER_MIN, 0, 16, CENTER_MAX, CENTER_MIN },
		{ 0, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, 16 },
		{ CENTER_MAX, CENTER_MIN, CENTER_MAX, 16, CENTER_MAX, 16 },
		{ CENTER_MIN, 0, 0, CENTER_MAX, CENTER_MIN, CENTER_MIN },
		{ CENTER_MIN, 0, CENTER_MAX, CENTER_MAX, CENTER_MIN, 16 },
		{ CENTER_MIN, CENTER_MAX, 0, CENTER_MAX, 16, CENTER_MIN },
		{ CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX, 16, 16 } };

	/** Shapes are a pure function of the mask, so they are worth keeping. */
	private static final Map<Integer, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

	protected AbstractOmnipanelBlock(final AbstractBlock.Properties properties) {
		super(properties.dynamicShape());
	}

	// ===== connectivity =====

	/**
	 * 1.12.2 getConnectionMask, kept as an int because the two bits mean different things: bit 0 is
	 * "a panel joins onto this", bit 1 is "that face is genuinely solid". The diagonal test needs to
	 * tell them apart, which a boolean cannot.
	 */
	public static int getConnectionMask(final IBlockReader world, final BlockPos blockPos, final Direction facing) {
		final BlockState blockState = world.getBlockState(blockPos);
		final Block block = blockState.getBlock();
		// Omnipanels join each other but are never solid faces. Asking isFaceSturdy() here would
		// request this neighbour's dynamic collision shape, which computes its connection mask and
		// recursively asks for our shape again until both the server and client overflow their stacks.
		if (block instanceof AbstractOmnipanelBlock) {
			return 1;
		}
		final boolean joins = block instanceof PaneBlock
		                   || blockState.getMaterial() == Material.GLASS
		                   || blockState.isCollisionShapeFullBlock(world, blockPos);
		return (joins ? 1 : 0)
		     + (blockState.isFaceSturdy(world, blockPos, facing.getOpposite()) ? 2 : 0);
	}

	/**
	 * The full eighteen-bit connectivity mask: six connections, then twelve quadrants.
	 *
	 * This is the whole of 1.12.2's getExtendedState, diagonal term included, with none of it
	 * reaching blockstate.
	 */
	public static int computeMask(final IBlockReader world, final BlockPos blockPos) {
		final int[] mask = new int[6];
		int result = 0;
		for (final Direction direction : Direction.values()) {
			final int value = getConnectionMask(world, blockPos.relative(direction), direction);
			mask[direction.ordinal()] = value;
			if (value > 0) {
				result |= 1 << direction.ordinal();
			}
		}
		// The legacy default/inventory form is the union of all three complete centre planes.
		// With no neighbours every quadrant is therefore present, not merely the centre post.
		if ((result & 0x3F) == 0) {
			return result | ((1 << QUADRANT_DIRECTIONS.length) - 1) << QUADRANT_SHIFT;
		}

		for (int index = 0; index < QUADRANT_DIRECTIONS.length; index++) {
			final Direction a = QUADRANT_DIRECTIONS[index][0];
			final Direction b = QUADRANT_DIRECTIONS[index][1];
			final int maskA = mask[a.ordinal()];
			final int maskB = mask[b.ordinal()];
			if (maskA <= 0 || maskB <= 0) {
				continue;
			}
			// Both faces solid, or the diagonal itself joins on. The diagonal is probed along b,
			// matching the original.
			if ( (maskA > 1 && maskB > 1)
			  || getConnectionMask(world, blockPos.relative(a).relative(b), b) > 0 ) {
				result |= 1 << (QUADRANT_SHIFT + index);
			}
		}
		return result;
	}

	public static boolean hasQuadrant(final int mask, final int index) {
		return (mask & 1 << (QUADRANT_SHIFT + index)) != 0;
	}

	public static boolean isConnected(final int mask, final Direction direction) {
		return (mask & 1 << direction.ordinal()) != 0;
	}

	/** True when no corner filled, so the panel is a beam rather than a sheet. */
	public static boolean hasNoQuadrant(final int mask) {
		return (mask >>> QUADRANT_SHIFT) == 0;
	}

	// ===== shape =====

	@Nonnull
	@Override
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		return shapeFor(computeMask(world, blockPos));
	}

	public static VoxelShape shapeFor(final int mask) {
		return SHAPE_CACHE.computeIfAbsent(mask, AbstractOmnipanelBlock::buildShape);
	}

	private static VoxelShape buildShape(final int mask) {
		VoxelShape shape = box(CENTER_MIN, CENTER_MIN, CENTER_MIN,
		                       CENTER_MAX, CENTER_MAX, CENTER_MAX);
		final boolean isolated = (mask & 0x3F) == 0;
		if (isolated || isConnected(mask, Direction.WEST)) {
			shape = VoxelShapes.or(shape, box(0, CENTER_MIN, CENTER_MIN,
			                                  CENTER_MIN, CENTER_MAX, CENTER_MAX));
		}
		if (isolated || isConnected(mask, Direction.EAST)) {
			shape = VoxelShapes.or(shape, box(CENTER_MAX, CENTER_MIN, CENTER_MIN,
			                                  16, CENTER_MAX, CENTER_MAX));
		}
		if (isolated || isConnected(mask, Direction.DOWN)) {
			shape = VoxelShapes.or(shape, box(CENTER_MIN, 0, CENTER_MIN,
			                                  CENTER_MAX, CENTER_MIN, CENTER_MAX));
		}
		if (isolated || isConnected(mask, Direction.UP)) {
			shape = VoxelShapes.or(shape, box(CENTER_MIN, CENTER_MAX, CENTER_MIN,
			                                  CENTER_MAX, 16, CENTER_MAX));
		}
		if (isolated || isConnected(mask, Direction.NORTH)) {
			shape = VoxelShapes.or(shape, box(CENTER_MIN, CENTER_MIN, 0,
			                                  CENTER_MAX, CENTER_MAX, CENTER_MIN));
		}
		if (isolated || isConnected(mask, Direction.SOUTH)) {
			shape = VoxelShapes.or(shape, box(CENTER_MIN, CENTER_MIN, CENTER_MAX,
			                                  CENTER_MAX, CENTER_MAX, 16));
		}
		for (int index = 0; index < QUADRANT_BOXES.length; index++) {
			if (isolated || hasQuadrant(mask, index)) {
				final double[] b = QUADRANT_BOXES[index];
				shape = VoxelShapes.or(shape, box(b[0], b[1], b[2], b[3], b[4], b[5]));
			}
		}
		return shape;
	}

	@Override
	public boolean propagatesSkylightDown(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                                      @Nonnull final BlockPos blockPos) {
		return true;
	}

	@Override
	public boolean isPathfindable(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                              @Nonnull final BlockPos blockPos, @Nonnull final net.minecraft.pathfinding.PathType pathType) {
		return false;
	}
}
