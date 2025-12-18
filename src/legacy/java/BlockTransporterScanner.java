package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;

public class BlockTransporterScanner extends BlockAbstractBase {
	
	protected static final AxisAlignedBB AABB_HALF_DOWN   = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 0.50D, 1.00D);
	
	public BlockTransporterScanner(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.movement.transporter_scanner");
		setLightOpacity(255);
		this.useNeighborBrightness = true;
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, metadata != 0);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(BlockProperties.ACTIVE) ? 1 : 0;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public BlockFaceShape getBlockFaceShape(@Nonnull final IBlockAccess blockAccess, @Nonnull final IBlockState blockState, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing enumFacing) {
		return enumFacing == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isSideSolid(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing enumFacing) {
		return enumFacing == EnumFacing.DOWN;
	}
	
	@Override
	public int getLightValue(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return blockState.getValue(BlockProperties.ACTIVE) ? 13 : 0;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return AABB_HALF_DOWN;
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return AABB_HALF_DOWN;
	}
	
	// return null or empty collection if it's invalid
	public Collection<BlockPos> getValidContainment(final World world, final BlockPos blockPos) {
		final ArrayList<BlockPos> vContainments = new ArrayList<>(8);
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
		boolean isScannerPosition = true;
		for (int x = blockPos.getX() - 1; x <= blockPos.getX() + 1; x++)  {
			for (int z = blockPos.getZ() - 1; z <= blockPos.getZ() + 1; z++) {
				// check base block is containment or scanner in checker pattern
				mutableBlockPos.setPos(x, blockPos.getY(), z);
				final Block blockBase = world.getBlockState(mutableBlockPos).getBlock();
				if ( !(blockBase instanceof BlockTransporterContainment)
				  && (!isScannerPosition || !(blockBase instanceof BlockTransporterScanner)) ) {
					return null;
				}
				isScannerPosition = !isScannerPosition;
				
				// check 2 above blocks are air
				mutableBlockPos.move(EnumFacing.UP);
				if (!world.isAirBlock(mutableBlockPos)) {
					return null;
				}
				mutableBlockPos.move(EnumFacing.UP);
				if (!world.isAirBlock(mutableBlockPos)) {
					return null;
				}
				
				// save containment position
				if (blockBase instanceof BlockTransporterContainment) {
					vContainments.add(new BlockPos(x, blockPos.getY(), z));
				}
			}
		}
		return vContainments;
	}
}