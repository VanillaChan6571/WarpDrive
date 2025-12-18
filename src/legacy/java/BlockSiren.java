package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractHorizontalSpinningContainer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockSiren extends BlockAbstractHorizontalSpinningContainer {
	
	private static final AxisAlignedBB AABB_INDUSTRIAL_DOWN_NORTH  = new AxisAlignedBB(0.0000D, 0.1875D, 0.1875D, 1.0000D, 0.8125D, 0.8125D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_DOWN_SOUTH  = new AxisAlignedBB(0.0000D, 0.1875D, 0.1875D, 1.0000D, 0.8125D, 0.8125D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_DOWN_WEST   = new AxisAlignedBB(0.1875D, 0.1875D, 0.0000D, 0.8125D, 0.8125D, 1.0000D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_DOWN_EAST   = new AxisAlignedBB(0.1875D, 0.1875D, 0.0000D, 0.8125D, 0.8125D, 1.0000D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_UP_NORTH    = new AxisAlignedBB(0.0000D, 0.1875D, 0.1875D, 1.0000D, 0.8125D, 0.8125D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_UP_SOUTH    = new AxisAlignedBB(0.0000D, 0.1875D, 0.1875D, 1.0000D, 0.8125D, 0.8125D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_UP_WEST     = new AxisAlignedBB(0.1875D, 0.1875D, 0.0000D, 0.8125D, 0.8125D, 1.0000D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_UP_EAST     = new AxisAlignedBB(0.1875D, 0.1875D, 0.0000D, 0.8125D, 0.8125D, 1.0000D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_NORTH       = new AxisAlignedBB(0.0000D, 0.1875D, 0.1875D, 1.0000D, 0.8125D, 0.8125D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_SOUTH       = new AxisAlignedBB(0.0000D, 0.1875D, 0.1875D, 1.0000D, 0.8125D, 0.8125D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_WEST        = new AxisAlignedBB(0.1875D, 0.1875D, 0.0000D, 0.8125D, 0.8125D, 1.0000D);
	private static final AxisAlignedBB AABB_INDUSTRIAL_EAST        = new AxisAlignedBB(0.1875D, 0.1875D, 0.0000D, 0.8125D, 0.8125D, 1.0000D);
	
	private static final AxisAlignedBB AABB_MILITARY_DOWN    = new AxisAlignedBB(0.0000D, 0.3125D, 0.0000D, 1.0000D, 0.6875D, 1.0000D);
	private static final AxisAlignedBB AABB_MILITARY_UP      = new AxisAlignedBB(0.0000D, 0.3125D, 0.0000D, 1.0000D, 0.6875D, 1.0000D);
	private static final AxisAlignedBB AABB_MILITARY_NORTH   = new AxisAlignedBB(0.0000D, 0.3125D, 0.4375D, 1.0000D, 0.6875D, 0.8125D);
	private static final AxisAlignedBB AABB_MILITARY_SOUTH   = new AxisAlignedBB(0.0000D, 0.3125D, 0.1875D, 1.0000D, 0.6875D, 0.5625D);
	private static final AxisAlignedBB AABB_MILITARY_WEST    = new AxisAlignedBB(0.4375D, 0.3125D, 0.0000D, 0.8125D, 0.6875D, 1.0000D);
	private static final AxisAlignedBB AABB_MILITARY_EAST    = new AxisAlignedBB(0.1875D, 0.3125D, 0.0000D, 0.5625D, 0.6875D, 1.0000D);
	private static final AxisAlignedBB AABB_FULL             = FULL_BLOCK_AABB;
	
	private final boolean isIndustrial;
	
	public BlockSiren(final String registryName, final EnumTier enumTier, final boolean isIndustrial) {
		super(registryName, enumTier, Material.IRON);
		
		this.isIndustrial = isIndustrial;
		setTranslationKey("warpdrive.detection.siren_" + (isIndustrial ? "industrial" : "military") + "." + enumTier.getName());
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntitySiren();
	}
	
	public boolean getIsIndustrial() {
		return isIndustrial;
	}
	
	@Override
	public int damageDropped(@Nonnull final IBlockState blockState) {
		return 0;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int getLightOpacity(@Nonnull final IBlockState blockState) {
		return 0;
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
		if (isIndustrial) {
			switch (blockState.getValue(BlockProperties.HORIZONTAL_SPINNING)) {
			case DOWN_NORTH : return AABB_INDUSTRIAL_DOWN_NORTH;
			case DOWN_SOUTH : return AABB_INDUSTRIAL_DOWN_SOUTH;
			case DOWN_WEST  : return AABB_INDUSTRIAL_DOWN_WEST;
			case DOWN_EAST  : return AABB_INDUSTRIAL_DOWN_EAST;
			case UP_NORTH   : return AABB_INDUSTRIAL_UP_NORTH;
			case UP_SOUTH   : return AABB_INDUSTRIAL_UP_SOUTH;
			case UP_WEST    : return AABB_INDUSTRIAL_UP_WEST;
			case UP_EAST    : return AABB_INDUSTRIAL_UP_EAST;
			case NORTH      : return AABB_INDUSTRIAL_NORTH;
			case SOUTH      : return AABB_INDUSTRIAL_SOUTH;
			case WEST       : return AABB_INDUSTRIAL_WEST;
			case EAST       : return AABB_INDUSTRIAL_EAST;
			default: return AABB_FULL;
			}
		} else {
			switch (blockState.getValue(BlockProperties.HORIZONTAL_SPINNING).facing) {
			case DOWN : return AABB_MILITARY_DOWN;
			case UP   : return AABB_MILITARY_UP;
			case NORTH: return AABB_MILITARY_NORTH;
			case SOUTH: return AABB_MILITARY_SOUTH;
			case WEST : return AABB_MILITARY_WEST;
			case EAST : return AABB_MILITARY_EAST;
			default: return AABB_FULL;
			}
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
	public boolean doesSideBlockRendering(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos, final EnumFacing side) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isSideSolid(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing side) {
		final EnumFacing enumFacing = blockState.getValue(BlockProperties.HORIZONTAL_SPINNING).facing;
		return enumFacing.getOpposite() == side;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		final int range = MathHelper.floor(WarpDriveConfig.SIREN_RANGE_BLOCKS_BY_TIER[enumTier.getIndex()]);
		final String unlocalizedName_withoutTier = getTranslationKey().replace("." + enumTier.getName(), "");
		Commons.addTooltip(list, new TextComponentTranslation(unlocalizedName_withoutTier + ".tooltip.usage",
		                                                      new WarpDriveText(Commons.getStyleValue(), range) ).getFormattedText());
	}
}
