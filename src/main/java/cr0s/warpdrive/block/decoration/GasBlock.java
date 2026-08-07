package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.data.DimensionAltitude;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Non-colliding coloured space gas.
 *
 * Each former metadata colour is now its own registry entry. The block remains replaceable and
 * removes itself in atmospheric dimensions, matching the useful part of the 1.12 behaviour.
 */
public class GasBlock extends Block {

	public GasBlock() {
		// A custom air state still uses its model render shape in 1.16's chunk dispatcher. Marking it
		// as air preserves the legacy gameplay checks without making the coloured model invisible.
		super(AbstractBlock.Properties.of(Material.AIR)
			.air()
			.noCollission()
			.noDrops()
			.noOcclusion()
			.strength(0.0F));
	}

	@Override
	public boolean canBeReplaced(@Nonnull final BlockState blockState,
	                             @Nonnull final BlockItemUseContext context) {
		return true;
	}

	@Override
	public void onPlace(@Nonnull final BlockState blockState, @Nonnull final World world,
	                    @Nonnull final BlockPos blockPos, @Nonnull final BlockState previousState,
	                    final boolean movedByPiston) {
		if (!world.isClientSide && !DimensionAltitude.isSpaceOrHyperspace(world)) {
			world.removeBlock(blockPos, false);
		}
	}

	@Override
	public boolean skipRendering(@Nonnull final BlockState blockState,
	                             @Nonnull final BlockState adjacentState,
	                             @Nonnull final net.minecraft.util.Direction side) {
		// Flattening made each colour a separate block; preserve the visible boundary between
		// different colours while culling faces within one continuous cloud.
		return adjacentState.is(this);
	}
}
