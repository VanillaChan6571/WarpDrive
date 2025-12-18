package cr0s.warpdrive.block;

import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumHorizontalSpinning;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public abstract class BlockAbstractHorizontalSpinningContainer extends BlockAbstractContainer {
	
	protected BlockAbstractHorizontalSpinningContainer(final String registryName, final EnumTier enumTier, final Material material) {
		super(registryName, enumTier, material);
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(BlockProperties.HORIZONTAL_SPINNING, EnumHorizontalSpinning.NORTH)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, BlockProperties.HORIZONTAL_SPINNING);
	}
	
	// Metadata Facing  Spinning
	// 0        NORTH   NORTH
	// 1        SOUTH   SOUTH
	// 2        WEST    WEST
	// 3        EAST    EAST
	// 4        DOWN    NORTH
	// 5        DOWN    SOUTH
	// 6        DOWN    WEST
	// 7        DOWN    EAST
	// 8        UP      NORTH
	// 9        UP      SOUTH
	// 10       UP      WEST
	// 11       UP      EAST
	// 12 to 15 ?       ?
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.HORIZONTAL_SPINNING, EnumHorizontalSpinning.get(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		final EnumHorizontalSpinning enumHorizontalSpinning = blockState.getValue(BlockProperties.HORIZONTAL_SPINNING);
		return enumHorizontalSpinning.ordinal();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAbstractMachine) {
			return blockState
					       .withProperty(BlockProperties.ACTIVE, ((TileEntityAbstractMachine) tileEntity).isEnabled);
		} else {
			return blockState;
		}
	}
}
