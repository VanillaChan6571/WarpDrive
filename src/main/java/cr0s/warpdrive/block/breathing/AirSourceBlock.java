package cr0s.warpdrive.block.breathing;

import net.minecraft.block.BlockState;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;

/**
 * The air block sitting directly in front of a generator, ported from 1.12.2 BlockAirSource.
 *
 * Always at maximum concentration, and the origin the generator pressure propagates outward from.
 * FACING points away from the generator, which is how the simulation finds the generator behind it
 * and removes the source if that generator has gone.
 */
public class AirSourceBlock extends AbstractAirBlock {

	/**
	 * All six facings. Uses the vanilla property rather than a hand-rolled one: DirectionProperty
	 * .create takes its values as varargs, so declaring it without them yields a property with zero
	 * possible values, which compiles happily and then fails registration.
	 */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public AirSourceBlock() {
		super();
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.DOWN));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<net.minecraft.block.Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** Convenience for the simulation: which way this source points, i.e. away from its generator. */
	public static Direction getFacing(@Nonnull final IBlockReader world, @Nonnull final BlockPos blockPos) {
		final BlockState blockState = world.getBlockState(blockPos);
		return blockState.getBlock() instanceof AirSourceBlock
		     ? blockState.getValue(FACING)
		     : Direction.DOWN;
	}
}
