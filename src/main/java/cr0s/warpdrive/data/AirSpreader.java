package cr0s.warpdrive.data;

import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.block.breathing.AirGeneratorTileEntity;
import cr0s.warpdrive.block.breathing.AirSourceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The air simulation, ported from 1.12.2.
 *
 * Run once per due block. Two things happen on each pass:
 *
 *  - Pressure propagation. Generator pressure and void pressure each step one block outward from
 *    the strongest neighbour that is pointing at us, so both spread as gradients without any
 *    flood fill. A block with generator pressure and no void pressure is sealed.
 *  - Concentration diffusion. Air averages toward its neighbours, with a penalty for every
 *    adjacent empty block, which is what makes a hole in the hull drain a room.
 *
 * The asymmetry between growth and decay is deliberate: a sealed volume with a generator gains air
 * faster than a leaking one loses it, so rooms fill decisively but still vent when opened.
 *
 * The cursors are static and reused. This is single-threaded by construction - only the server
 * tick calls it - and allocating seven StateAir objects per block per tick would dominate the cost.
 */
public class AirSpreader {

	private static final StateAir stateCenter = new StateAir(null);
	private static final StateAir[] stateAround = {
		new StateAir(null), new StateAir(null), new StateAir(null),
		new StateAir(null), new StateAir(null), new StateAir(null) };

	/** Horizontal facings, the 1.16.5 stand-in for EnumFacing.HORIZONTALS. */
	private static final Direction[] FACINGS_HORIZONTAL = {
		Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
	private static final Direction[] FACINGS_VERTICAL = { Direction.DOWN, Direction.UP };

	private AirSpreader() {
	}

	public static void execute(final World world, final int x, final int y, final int z) throws ExceptionChunkNotLoaded {
		stateCenter.refresh(world, x, y, z);
		// force a fresh read: the block may have changed since the state was cached
		stateCenter.updateBlockCache(world);

		// a sealed block holds nothing
		if (!stateCenter.isAir()) {
			stateCenter.setConcentration(world, (byte) 0);
			stateCenter.removeGeneratorAndCascade(world);
			stateCenter.removeVoidAndCascade(world);
			return;
		}

		// which way air can move through this block
		final Direction[] directions;
		if (stateCenter.isLeakingHorizontally()) {
			directions = FACINGS_HORIZONTAL;
		} else if (stateCenter.isLeakingVertically()) {
			directions = FACINGS_VERTICAL;
		} else {
			directions = Direction.values();
		}

		int maxPressureGenerator = 0;
		Direction maxDirectionGenerator = null;
		int maxPressureVoid = 0;
		Direction maxDirectionVoid = null;

		final int concentration = stateCenter.concentration;
		int sumConcentration = concentration;
		int maxConcentration = concentration;
		int minConcentration = concentration;
		int airCount = 1;
		int emptyCount = 0;

		for (final Direction direction : directions) {
			final StateAir stateAir = stateAround[direction.ordinal()];
			stateAir.refresh(world,
			                 x + direction.getStepX(),
			                 y + direction.getStepY(),
			                 z + direction.getStepZ());
			if (!stateAir.isAir(direction)) {
				continue;
			}
			airCount++;
			if (stateAir.concentration > 0) {
				sumConcentration += stateAir.concentration;
				maxConcentration = Math.max(maxConcentration, stateAir.concentration);
				minConcentration = Math.min(minConcentration, stateAir.concentration);
			} else {
				emptyCount++;
			}
			// only take pressure from a neighbour that is actually feeding us
			if ( maxPressureGenerator < stateAir.pressureGenerator
			  && (stateAir.isAirSource() || stateAir.directionGenerator != null) ) {
				maxPressureGenerator = stateAir.pressureGenerator;
				maxDirectionGenerator = direction;
			}
			if ( maxPressureVoid < stateAir.pressureVoid
			  && (stateAir.isVoidSource() || stateAir.directionVoid != null) ) {
				maxPressureVoid = stateAir.pressureVoid;
				maxDirectionVoid = direction;
			}
		}

		// propagate or drop generator pressure; sources hold their own
		if (!stateCenter.isAirSource()) {
			if ( stateCenter.pressureGenerator < maxPressureGenerator
			  && maxPressureGenerator > 1 ) {
				stateCenter.setGeneratorAndUpdateVoid(world, (short) (maxPressureGenerator - 1), maxDirectionGenerator);
			} else if (stateCenter.pressureGenerator != 0) {
				stateCenter.removeGeneratorAndCascade(world);
				refreshNeighbours(world, x, y, z, directions);
			}
		}

		if (!stateCenter.isVoidSource()) {
			if ( stateCenter.pressureVoid < maxPressureVoid
			  && maxPressureVoid > 1 ) {
				stateCenter.setVoid((short) (maxPressureVoid - 1), maxDirectionVoid);
			} else if (stateCenter.pressureVoid != 0) {
				stateCenter.removeVoidAndCascade(world);
				refreshNeighbours(world, x, y, z, directions);
			}
		}

		// nothing to diffuse
		if (sumConcentration == 0) {
			return;
		}

		// leaking to empty neighbours costs air, with some jitter so a leak looks unsteady
		if (emptyCount > 0) {
			if (concentration < 4) {
				sumConcentration -= emptyCount + (world.random.nextBoolean() ? 0 : emptyCount);
			} else if (concentration < 8) {
				sumConcentration -= emptyCount;
			} else if (concentration < 12) {
				sumConcentration -= airCount;
			}
		}
		if (sumConcentration < 0) {
			sumConcentration = 0;
		}

		// A sealed volume being fed by a generator grows; anything else merely averages out.
		final boolean isGrowth = stateCenter.pressureGenerator > 0
		                      && (stateCenter.pressureVoid == 0 || stateCenter.isAirSource())
		                      && maxConcentration - minConcentration > 2;

		int midConcentration;
		int newConcentration;
		if (isGrowth) {
			midConcentration = (int) Math.ceil(sumConcentration / (float) airCount);
			newConcentration = sumConcentration - midConcentration * (airCount - 1);
			newConcentration = Math.max(Math.max(concentration + 1, maxConcentration - 1), newConcentration - 20);
		} else {
			midConcentration = (int) Math.floor(sumConcentration / (float) airCount);
			newConcentration = sumConcentration - midConcentration * (airCount - 1);
			if (emptyCount > 0) {
				newConcentration = Math.max(0, newConcentration - 5);
			}
		}

		// depressurisation caps what a leaking block can hold; over-pressure gives a small bonus
		if (stateCenter.pressureVoid > 0) {
			midConcentration = Math.min(midConcentration, 160);
			newConcentration = Math.min(newConcentration, 160);
		} else if (stateCenter.pressureGenerator > 20 && newConcentration > 16) {
			midConcentration += 2;
			newConcentration += 2;
		}

		midConcentration = clampConcentration(midConcentration);
		if (newConcentration < 0) {
			newConcentration = 0;
		} else if (isGrowth && newConcentration > maxConcentration) {
			newConcentration = Math.max(0, maxConcentration);
		} else if (!isGrowth && newConcentration > maxConcentration - 1) {
			newConcentration = Math.max(0, maxConcentration - 1);
		}
		newConcentration = clampConcentration(newConcentration);

		applyCentre(world, x, y, z, concentration, newConcentration);
		applyNeighbours(world, directions, midConcentration, isGrowth);
	}

	private static int clampConcentration(final int value) {
		if (value < 0) {
			return 0;
		}
		return Math.min(value, AirData.CONCENTRATION_MAX);
	}

	private static void refreshNeighbours(final World world, final int x, final int y, final int z,
	                                      final Direction[] directions) throws ExceptionChunkNotLoaded {
		for (final Direction direction : directions) {
			stateAround[direction.ordinal()].refresh(world,
				x + direction.getStepX(),
				y + direction.getStepY(),
				z + direction.getStepZ());
		}
	}

	private static void applyCentre(final World world, final int x, final int y, final int z,
	                                final int concentration, final int newConcentration) {
		if (concentration == newConcentration) {
			// An air flow block with no air left is a stale leftover - clear it
			if (stateCenter.isAirFlow() && newConcentration == 0) {
				stateCenter.setConcentration(world, (byte) newConcentration);
			}
			return;
		}

		if (!stateCenter.isAirSource()) {
			// never raise concentration without a generator in range
			if (stateCenter.directionGenerator != null || concentration > newConcentration) {
				stateCenter.setConcentration(world, (byte) newConcentration);
			}
			return;
		}

		// An air source is only valid while its generator is still behind it, facing the same way.
		// Otherwise the generator has been broken or rotated and the source must go.
		if (!hasMatchingGenerator(world, x, y, z)) {
			stateCenter.removeAirSource(world);
		}
	}

	private static boolean hasMatchingGenerator(final World world, final int x, final int y, final int z) {
		final BlockState blockStateSource = stateCenter.getBlockState(world);
		if (!(blockStateSource.getBlock() instanceof AirSourceBlock)) {
			return false;
		}
		final Direction facingSource = blockStateSource.getValue(AirSourceBlock.FACING);
		final BlockPos posGenerator = new BlockPos(
			x - facingSource.getStepX(),
			y - facingSource.getStepY(),
			z - facingSource.getStepZ());

		final TileEntity tileEntity = world.getBlockEntity(posGenerator);
		if (!(tileEntity instanceof AirGeneratorTileEntity)) {
			return false;
		}
		return ((AirGeneratorTileEntity) tileEntity).getFacing() == facingSource;
	}

	private static void applyNeighbours(final World world, final Direction[] directions,
	                                    final int midConcentration, final boolean isGrowth) {
		for (final Direction direction : directions) {
			final StateAir stateAir = stateAround[direction.ordinal()];
			// never overwrite a source, and never pull neighbours down while growing
			if ( !stateAir.isAirFlow()
			  && !(stateAir.isAir(direction) && !stateAir.isAirSource()) ) {
				continue;
			}
			if ( stateAir.concentration != midConcentration
			  && (!isGrowth || stateAir.concentration < midConcentration) ) {
				stateAir.setConcentration(world, (byte) midConcentration);
			} else if (midConcentration == 0 && stateAir.concentration != 0) {
				stateAir.setConcentration(world, (byte) midConcentration);
			}
		}
	}

	/** Drop cached chunk references at end of tick, so chunk unloading is safe. */
	public static void clearCache() {
		stateCenter.clearCache();
		for (final StateAir stateAir : stateAround) {
			stateAir.clearCache();
		}
	}
}
