package cr0s.warpdrive.block.breathing;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;

/**
 * Base for WarpDrive's air blocks, ported from 1.12.2 BlockAbstractAir.
 *
 * These are placed in the world by the air simulation to mark where breathable air actually is.
 * They must behave as close to vanilla air as possible - invisible, non-solid, replaceable - while
 * still being a distinct block the simulation can recognise and count.
 *
 * Marked as air via Properties.air(), so BlockState.isAir() is true and the rest of the game treats
 * these positions as empty: mobs spawn, plants grow, and blocks can be placed straight into them.
 */
public abstract class AbstractAirBlock extends Block {

	protected AbstractAirBlock() {
		super(AbstractBlock.Properties.of(Material.AIR)
			.noCollission()
			.noDrops()
			.instabreak()
			.noOcclusion()
			.air());
	}

	/**
	 * Hide every face except those meeting empty space.
	 *
	 * This is the whole visual language of the system, ported from 1.12.2. A face bordering another
	 * air block is interior and hidden, and a face against a wall is hidden too - so a sealed room
	 * is completely invisible. What remains visible is precisely the boundary between air and
	 * vacuum: you watch a translucent blue front advance out of the generator as the room fills,
	 * and if there is a hole it never stops showing, because there is always a frontier.
	 *
	 * In other words the render is the leak indicator. A room is "secured" exactly when it
	 * disappears.
	 */
	@Override
	public boolean skipRendering(@Nonnull final BlockState blockState,
	                             @Nonnull final BlockState adjacentState, @Nonnull final Direction side) {
		// our own air: interior face
		if (adjacentState.getBlock() instanceof AbstractAirBlock) {
			return true;
		}
		// only draw against genuinely empty space
		final Block adjacent = adjacentState.getBlock();
		return !(adjacent == Blocks.AIR || adjacent == Blocks.CAVE_AIR || adjacent == Blocks.VOID_AIR);
	}

	@Nonnull
	@Override
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		return VoxelShapes.empty();
	}

	/** Anything may be built straight into air, exactly as with vanilla air. */
	@Override
	public boolean canBeReplaced(@Nonnull final BlockState blockState, @Nonnull final BlockItemUseContext context) {
		return true;
	}

	/** Pistons destroy air rather than pushing it. */
	@Nonnull
	@Override
	public PushReaction getPistonPushReaction(@Nonnull final BlockState blockState) {
		return PushReaction.DESTROY;
	}

	@Override
	public boolean propagatesSkylightDown(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                                      @Nonnull final BlockPos blockPos) {
		return true;
	}
}
