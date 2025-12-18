package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.BlockAbstractHorizontalSpinningContainer;
import cr0s.warpdrive.block.ItemBlockAbstractBase;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockEnvironmentalSensor extends BlockAbstractHorizontalSpinningContainer {
	
	private static final AxisAlignedBB AABB_DOWN   = new AxisAlignedBB(0.1250D, 0.6875D, 0.1250D, 0.8750D, 1.0000D, 0.8750D);
	private static final AxisAlignedBB AABB_UP     = new AxisAlignedBB(0.1250D, 0.0000D, 0.1250D, 0.8750D, 0.3125D, 0.8750D);
	private static final AxisAlignedBB AABB_NORTH  = new AxisAlignedBB(0.1250D, 0.1250D, 0.6875D, 0.8750D, 0.8750D, 1.0000D);
	private static final AxisAlignedBB AABB_SOUTH  = new AxisAlignedBB(0.1250D, 0.1250D, 0.0000D, 0.8750D, 0.8750D, 0.3125D);
	private static final AxisAlignedBB AABB_WEST   = new AxisAlignedBB(0.6875D, 0.1250D, 0.1250D, 1.0000D, 0.8750D, 0.8750D);
	private static final AxisAlignedBB AABB_EAST   = new AxisAlignedBB(0.0000D, 0.1250D, 0.1250D, 0.3125D, 0.8750D, 0.8750D);
	private static final AxisAlignedBB AABB_FULL   = FULL_BLOCK_AABB;
	
	public BlockEnvironmentalSensor(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.detection.environmental_sensor");
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityEnvironmentalSensor();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return getBlockBoundsFromState(blockState);
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return getBlockBoundsFromState(blockState);
	}
	
	private AxisAlignedBB getBlockBoundsFromState(final IBlockState blockState) {
		if (blockState == null) {
			return AABB_FULL;
		}
		switch (blockState.getValue(BlockProperties.HORIZONTAL_SPINNING).facing) {
		case DOWN : return AABB_DOWN;
		case UP   : return AABB_UP;
		case NORTH: return AABB_NORTH;
		case SOUTH: return AABB_SOUTH;
		case WEST : return AABB_WEST;
		case EAST : return AABB_EAST;
		default: return AABB_FULL;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTopSolid(@Nonnull final IBlockState blockState) {
		final EnumFacing enumFacing = blockState.getValue(BlockProperties.HORIZONTAL_SPINNING).facing;
		return enumFacing == EnumFacing.DOWN;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public BlockFaceShape getBlockFaceShape(@Nonnull final IBlockAccess blockAccess, @Nonnull final IBlockState blockState, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing enumFacing) {
		final EnumFacing enumFacingState = blockState.getValue(BlockProperties.HORIZONTAL_SPINNING).facing;
		return enumFacing == enumFacingState.getOpposite() ? BlockFaceShape.CENTER_BIG : BlockFaceShape.UNDEFINED;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		final BlockPos blockPosSide = blockPos.offset(facing);
		final boolean doesSideBlockRendering = blockAccess.getBlockState(blockPosSide).doesSideBlockRendering(blockAccess, blockPosSide, facing);
		return !doesSideBlockRendering;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@Override
	public boolean doesSideBlockRendering(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing side) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isSideSolid(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing side) {
		final EnumFacing enumFacing = blockState.getValue(BlockProperties.HORIZONTAL_SPINNING).facing;
		return enumFacing.getOpposite() == side;
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, false, true);
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}