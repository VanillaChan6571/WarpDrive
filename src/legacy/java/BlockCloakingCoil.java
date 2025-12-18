package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCloakingCoil extends BlockAbstractBase {
	
	// Metadata values
	// 0 = not linked
	// 1 = inner coil passive
	// 2-7 = outer coil passive
	// 8 = (not used)
	// 9 = inner coil active
	// 10-15 = outer coil active
	
	public static final PropertyBool OUTER = PropertyBool.create("outer");
	
	public BlockCloakingCoil(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(3.5F);
		setTranslationKey("warpdrive.detection.cloaking_coil");
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.CONNECTED, false)
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(OUTER, false)
				                .withProperty(BlockProperties.FACING, EnumFacing.DOWN)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.CONNECTED, BlockProperties.ACTIVE, OUTER, BlockProperties.FACING);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		// 10-15 = connected, active, outer (facing)
		//     9 = connected, active, inner, down
		//     8 = not used (disconnected, active, down)
		//   2-7 = connected, outer, (facing)
		//     1 = connected, inner, down
		//     0 = idle (disconnected, inactive, down)
		final boolean isActive = (metadata & 0x8) != 0;
		final boolean isConnected = (metadata & 0x7) > 0;
		final boolean isOuter = (metadata & 0x7) > 1;
		return getDefaultState()
				       .withProperty(BlockProperties.CONNECTED, isConnected)
				       .withProperty(BlockProperties.ACTIVE, isActive)
				       .withProperty(OUTER, isOuter)
				       .withProperty(BlockProperties.FACING, isOuter ? EnumFacing.byIndex((metadata & 0x7) - 2) : EnumFacing.DOWN);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		if (blockState.getValue(BlockProperties.CONNECTED)) {
			return (blockState.getValue(BlockProperties.ACTIVE) ? 8 : 0)
			       + (blockState.getValue(OUTER) ? 2 + blockState.getValue(BlockProperties.FACING).ordinal() : 1);
		} else if ( !blockState.getValue(OUTER)
		         && blockState.getValue(BlockProperties.FACING) == EnumFacing.DOWN ) {
			return 0;
		} else {
			return 8;
		}
	}
	
	public static void setBlockState(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockStateActual,
	                                 final boolean isConnected, final boolean isActive, final boolean isOuter, final EnumFacing enumFacing) {
		IBlockState blockStateNew = blockStateActual.withProperty(BlockProperties.CONNECTED, isConnected)
		                                            .withProperty(BlockProperties.ACTIVE, isActive)
		                                            .withProperty(OUTER, isOuter);
		if (enumFacing != null) {
			blockStateNew = blockStateNew.withProperty(BlockProperties.FACING, enumFacing);
		}
		if (blockStateActual.getBlock().getMetaFromState(blockStateActual) != blockStateActual.getBlock().getMetaFromState(blockStateNew)) {
			world.setBlockState(blockPos, blockStateNew);
		}
	}
}
