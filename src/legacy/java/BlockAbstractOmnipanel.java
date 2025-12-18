package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.event.ModelBakeEventHandler;
import cr0s.warpdrive.render.BakedModelOmnipanel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockAbstractOmnipanel extends BlockAbstractBase {
	
	public static final float CENTER_MIN = 7.0F / 16.0F;
	public static final float CENTER_MAX = 9.0F / 16.0F;
	
	protected static AxisAlignedBB AABB_XN_YN = new AxisAlignedBB(0.0F, 0.0F, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, CENTER_MIN, 1.0F, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XN_YP = new AxisAlignedBB(0.0F, CENTER_MIN, CENTER_MIN, CENTER_MAX, 1.0F, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, 1.0F, 1.0F, CENTER_MAX);
	
	protected static AxisAlignedBB AABB_ZN_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, 0.0F, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZP_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, CENTER_MIN, CENTER_MAX, CENTER_MAX, 1.0F);
	protected static AxisAlignedBB AABB_ZN_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, 0.0F, CENTER_MAX, 1.0F, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZP_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, 1.0F, 1.0F);
	
	protected static AxisAlignedBB AABB_XN_ZN = new AxisAlignedBB(0.0F, CENTER_MIN, 0.0F, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP_ZN = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, 0.0F, 1.0F, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XN_ZP = new AxisAlignedBB(0.0F, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, 1.0F);
	protected static AxisAlignedBB AABB_XP_ZP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, 1.0F, CENTER_MAX, 1.0F);
	
	protected static AxisAlignedBB AABB_YN = new AxisAlignedBB(CENTER_MIN, 0.0F, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_YP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, 1.0F, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZN = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, 0.0F, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_ZP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, 1.0F);
	protected static AxisAlignedBB AABB_XN = new AxisAlignedBB(0.0F, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
	protected static AxisAlignedBB AABB_XP = new AxisAlignedBB(CENTER_MIN, CENTER_MIN, CENTER_MIN, 1.0F, CENTER_MAX, CENTER_MAX);
	
	public static final IUnlistedProperty<Boolean> CAN_CONNECT_Y_NEG  = Properties.toUnlisted(PropertyBool.create("canConnectY_neg"));
	public static final IUnlistedProperty<Boolean> CAN_CONNECT_Y_POS  = Properties.toUnlisted(PropertyBool.create("canConnectY_pos"));
	public static final IUnlistedProperty<Boolean> CAN_CONNECT_Z_NEG  = Properties.toUnlisted(PropertyBool.create("canConnectZ_neg"));
	public static final IUnlistedProperty<Boolean> CAN_CONNECT_Z_POS  = Properties.toUnlisted(PropertyBool.create("canConnectZ_pos"));
	public static final IUnlistedProperty<Boolean> CAN_CONNECT_X_NEG  = Properties.toUnlisted(PropertyBool.create("canConnectX_neg"));
	public static final IUnlistedProperty<Boolean> CAN_CONNECT_X_POS  = Properties.toUnlisted(PropertyBool.create("canConnectX_pos"));
	
	public static final IUnlistedProperty<Boolean> HAS_XN_YN  = Properties.toUnlisted(PropertyBool.create("hasXnYn"));
	public static final IUnlistedProperty<Boolean> HAS_XP_YN  = Properties.toUnlisted(PropertyBool.create("hasXpYn"));
	public static final IUnlistedProperty<Boolean> HAS_XN_YP  = Properties.toUnlisted(PropertyBool.create("hasXnYp"));
	public static final IUnlistedProperty<Boolean> HAS_XP_YP  = Properties.toUnlisted(PropertyBool.create("hasXpYp"));
	public static final IUnlistedProperty<Boolean> HAS_XN_ZN  = Properties.toUnlisted(PropertyBool.create("hasXnZn"));
	public static final IUnlistedProperty<Boolean> HAS_XP_ZN  = Properties.toUnlisted(PropertyBool.create("hasXpZn"));
	public static final IUnlistedProperty<Boolean> HAS_XN_ZP  = Properties.toUnlisted(PropertyBool.create("hasXnZp"));
	public static final IUnlistedProperty<Boolean> HAS_XP_ZP  = Properties.toUnlisted(PropertyBool.create("hasXpZp"));
	public static final IUnlistedProperty<Boolean> HAS_ZN_YN  = Properties.toUnlisted(PropertyBool.create("hasZnYn"));
	public static final IUnlistedProperty<Boolean> HAS_ZP_YN  = Properties.toUnlisted(PropertyBool.create("hasZpYn"));
	public static final IUnlistedProperty<Boolean> HAS_ZN_YP  = Properties.toUnlisted(PropertyBool.create("hasZnYp"));
	public static final IUnlistedProperty<Boolean> HAS_ZP_YP  = Properties.toUnlisted(PropertyBool.create("hasZpYp"));
	
	public BlockAbstractOmnipanel(final String registryName, final EnumTier enumTier, final Material material) {
		super(registryName, enumTier, material);
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this,
		                              new IProperty[] { BlockColored.COLOR },
		                              new IUnlistedProperty[] { CAN_CONNECT_Y_NEG, CAN_CONNECT_Y_POS, CAN_CONNECT_Z_NEG, CAN_CONNECT_Z_POS, CAN_CONNECT_X_NEG, CAN_CONNECT_X_POS,
		                                                        HAS_XN_YN, HAS_XP_YN, HAS_XN_YP, HAS_XP_YP, HAS_XN_ZN, HAS_XP_ZN, HAS_XN_ZP, HAS_XP_ZP, HAS_ZN_YN, HAS_ZP_YN, HAS_ZN_YP, HAS_ZP_YP });
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return this.getDefaultState().withProperty(BlockColored.COLOR, EnumDyeColor.byMetadata(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(BlockColored.COLOR).getMetadata();
	}
	
	@Nonnull
	@Override
	public IBlockState getExtendedState(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		if (!(blockState instanceof IExtendedBlockState)) {
			WarpDrive.logger.error(String.format("%s Invalid call to getExtendedState() with invalid state %s %s",
			                                     this, blockState, Commons.format(blockAccess, blockPos)));
			return blockState;
		}
		
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
		
		// get direct connections
		final int maskConnectY_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN);
		final int maskConnectY_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP);
		final int maskConnectZ_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH);
		final int maskConnectZ_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH);
		final int maskConnectX_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.WEST);
		final int maskConnectX_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.EAST);
		
		final boolean canConnectY_neg = maskConnectY_neg > 0;
		final boolean canConnectY_pos = maskConnectY_pos > 0;
		final boolean canConnectZ_neg = maskConnectZ_neg > 0;
		final boolean canConnectZ_pos = maskConnectZ_pos > 0;
		final boolean canConnectX_neg = maskConnectX_neg > 0;
		final boolean canConnectX_pos = maskConnectX_pos > 0;
		final boolean canConnectNone = !canConnectY_neg && !canConnectY_pos && !canConnectZ_neg && !canConnectZ_pos && !canConnectX_neg && !canConnectX_pos;
		
		// get diagonal connections
		final boolean canConnectXn_Y_neg = (maskConnectX_neg > 1 && maskConnectY_neg > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN ) > 0;
		final boolean canConnectXn_Y_pos = (maskConnectX_neg > 1 && maskConnectY_pos > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP   ) > 0;
		final boolean canConnectXn_Z_neg = (maskConnectX_neg > 1 && maskConnectZ_neg > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH) > 0;
		final boolean canConnectXn_Z_pos = (maskConnectX_neg > 1 && maskConnectZ_pos > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH) > 0;
		final boolean canConnectZn_Y_neg = (maskConnectZ_neg > 1 && maskConnectY_neg > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ() - 1), EnumFacing.DOWN ) > 0;
		final boolean canConnectZn_Y_pos = (maskConnectZ_neg > 1 && maskConnectY_pos > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ() - 1), EnumFacing.UP   ) > 0;
		
		final boolean canConnectXp_Y_neg = (maskConnectX_pos > 1 && maskConnectY_neg > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN ) > 0;
		final boolean canConnectXp_Y_pos = (maskConnectX_pos > 1 && maskConnectY_pos > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP   ) > 0;
		final boolean canConnectXp_Z_neg = (maskConnectX_pos > 1 && maskConnectZ_neg > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH) > 0;
		final boolean canConnectXp_Z_pos = (maskConnectX_pos > 1 && maskConnectZ_pos > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH) > 0;
		final boolean canConnectZp_Y_neg = (maskConnectZ_pos > 1 && maskConnectY_neg > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ() + 1), EnumFacing.DOWN ) > 0;
		final boolean canConnectZp_Y_pos = (maskConnectZ_pos > 1 && maskConnectY_pos > 1) || getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ() + 1), EnumFacing.UP   ) > 0;
		
		// get panels
		final boolean hasXnYn = canConnectNone || (canConnectX_neg && canConnectY_neg && canConnectXn_Y_neg);
		final boolean hasXpYn = canConnectNone || (canConnectX_pos && canConnectY_neg && canConnectXp_Y_neg);
		final boolean hasXnYp = canConnectNone || (canConnectX_neg && canConnectY_pos && canConnectXn_Y_pos);
		final boolean hasXpYp = canConnectNone || (canConnectX_pos && canConnectY_pos && canConnectXp_Y_pos);
		
		final boolean hasXnZn = canConnectNone || (canConnectX_neg && canConnectZ_neg && canConnectXn_Z_neg);
		final boolean hasXpZn = canConnectNone || (canConnectX_pos && canConnectZ_neg && canConnectXp_Z_neg);
		final boolean hasXnZp = canConnectNone || (canConnectX_neg && canConnectZ_pos && canConnectXn_Z_pos);
		final boolean hasXpZp = canConnectNone || (canConnectX_pos && canConnectZ_pos && canConnectXp_Z_pos);
		
		final boolean hasZnYn = canConnectNone || (canConnectZ_neg && canConnectY_neg && canConnectZn_Y_neg);
		final boolean hasZpYn = canConnectNone || (canConnectZ_pos && canConnectY_neg && canConnectZp_Y_neg);
		final boolean hasZnYp = canConnectNone || (canConnectZ_neg && canConnectY_pos && canConnectZn_Y_pos);
		final boolean hasZpYp = canConnectNone || (canConnectZ_pos && canConnectY_pos && canConnectZp_Y_pos);
		
		// build extended state
		return ((IExtendedBlockState) blockState)
				       .withProperty(CAN_CONNECT_Y_NEG, canConnectY_neg)
				       .withProperty(CAN_CONNECT_Y_POS, canConnectY_pos)
				       .withProperty(CAN_CONNECT_Z_NEG, canConnectZ_neg)
				       .withProperty(CAN_CONNECT_Z_POS, canConnectZ_pos)
				       .withProperty(CAN_CONNECT_X_NEG, canConnectX_neg)
				       .withProperty(CAN_CONNECT_X_POS, canConnectX_pos)
				       .withProperty(HAS_XN_YN, hasXnYn)
				       .withProperty(HAS_XP_YN, hasXpYn)
				       .withProperty(HAS_XN_YP, hasXnYp)
				       .withProperty(HAS_XP_YP, hasXpYp)
				       .withProperty(HAS_XN_ZN, hasXnZn)
				       .withProperty(HAS_XP_ZN, hasXpZn)
				       .withProperty(HAS_XN_ZP, hasXnZp)
				       .withProperty(HAS_XP_ZP, hasXpZp)
				       .withProperty(HAS_ZN_YN, hasZnYn)
				       .withProperty(HAS_ZP_YN, hasZpYn)
				       .withProperty(HAS_ZN_YP, hasZnYp)
				       .withProperty(HAS_ZP_YP, hasZpYp);
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, true, false);
	}
	
	@Override
	public int damageDropped(@Nonnull final IBlockState blockState) {
		return blockState.getValue(BlockColored.COLOR).getMetadata();
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			list.add(new ItemStack(this, 1, enumDyeColor.getMetadata()));
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
		
		// register (smart) baked model
		final ResourceLocation registryName = getRegistryName();
		assert registryName != null;
		for (final EnumDyeColor enumDyeColor : BlockColored.COLOR.getAllowedValues()) {
			final String variant = String.format("%s=%s",
			                                     BlockColored.COLOR.getName(), enumDyeColor.getName());
			ModelBakeEventHandler.registerBakedModel(new ModelResourceLocation(registryName, variant), BakedModelOmnipanel.class);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public MapColor getMapColor(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		return MapColor.getBlockColor(blockState.getValue(BlockColored.COLOR));
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isBlockNormalCube(@Nonnull final IBlockState blockState) {
		// not supposed to be called, upstream should use isNormalCube(IBlockState, IBlockAccess, BlockPos) instead
		// practically, Forge still use it in WorldEntitySpawner.isValidEmptySpawnBlock(), Block.getAmbientOcclusionLightValue(), BlockRedstoneWire.getAttachPosition()
		// calling BlockStateContainer$StateImplementation.isBlockNormalCube()
		return false;
	}
	
	@Override
	public boolean isNormalCube(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return false;
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
	@Override
	public boolean isFullBlock(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		return blockAccess.getBlockState(blockPos).getBlock() != this && super.shouldSideBeRendered(blockState, blockAccess, blockPos, facing);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void addCollisionBoxToList(final IBlockState blockState, final @Nonnull World world, final @Nonnull BlockPos blockPos,
	                                  final @Nonnull AxisAlignedBB entityBox, final @Nonnull List<AxisAlignedBB> collidingBoxes,
	                                  final @Nullable Entity entity, final boolean isActualState) {
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
		
		// get direct connections
		final int maskConnectY_neg = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN);
		final int maskConnectY_pos = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP);
		final int maskConnectZ_neg = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH);
		final int maskConnectZ_pos = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH);
		final int maskConnectX_neg = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.WEST);
		final int maskConnectX_pos = getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.EAST);
		
		final boolean canConnectY_neg = maskConnectY_neg > 0;
		final boolean canConnectY_pos = maskConnectY_pos > 0;
		final boolean canConnectZ_neg = maskConnectZ_neg > 0;
		final boolean canConnectZ_pos = maskConnectZ_pos > 0;
		final boolean canConnectX_neg = maskConnectX_neg > 0;
		final boolean canConnectX_pos = maskConnectX_pos > 0;
		final boolean canConnectNone = !canConnectY_neg && !canConnectY_pos && !canConnectZ_neg && !canConnectZ_pos && !canConnectX_neg && !canConnectX_pos;
		
		// get diagonal connections
		final boolean canConnectXn_Y_neg = (maskConnectX_neg > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN ) > 0;
		final boolean canConnectXn_Y_pos = (maskConnectX_neg > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP   ) > 0;
		final boolean canConnectXn_Z_neg = (maskConnectX_neg > 1 && maskConnectZ_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH) > 0;
		final boolean canConnectXn_Z_pos = (maskConnectX_neg > 1 && maskConnectZ_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH) > 0;
		final boolean canConnectZn_Y_neg = (maskConnectZ_neg > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ() - 1), EnumFacing.DOWN ) > 0;
		final boolean canConnectZn_Y_pos = (maskConnectZ_neg > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ() - 1), EnumFacing.UP   ) > 0;
		
		final boolean canConnectXp_Y_neg = (maskConnectX_pos > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN ) > 0;
		final boolean canConnectXp_Y_pos = (maskConnectX_pos > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP   ) > 0;
		final boolean canConnectXp_Z_neg = (maskConnectX_pos > 1 && maskConnectZ_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH) > 0;
		final boolean canConnectXp_Z_pos = (maskConnectX_pos > 1 && maskConnectZ_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH) > 0;
		final boolean canConnectZp_Y_neg = (maskConnectZ_pos > 1 && maskConnectY_neg > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ() + 1), EnumFacing.DOWN ) > 0;
		final boolean canConnectZp_Y_pos = (maskConnectZ_pos > 1 && maskConnectY_pos > 1) || getConnectionMask(world, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ() + 1), EnumFacing.UP   ) > 0;
		
		// get panels
		final boolean hasXnYn = canConnectNone || (canConnectX_neg && canConnectY_neg && canConnectXn_Y_neg);
		final boolean hasXpYn = canConnectNone || (canConnectX_pos && canConnectY_neg && canConnectXp_Y_neg);
		final boolean hasXnYp = canConnectNone || (canConnectX_neg && canConnectY_pos && canConnectXn_Y_pos);
		final boolean hasXpYp = canConnectNone || (canConnectX_pos && canConnectY_pos && canConnectXp_Y_pos);
		
		final boolean hasXnZn = canConnectNone || (canConnectX_neg && canConnectZ_neg && canConnectXn_Z_neg);
		final boolean hasXpZn = canConnectNone || (canConnectX_pos && canConnectZ_neg && canConnectXp_Z_neg);
		final boolean hasXnZp = canConnectNone || (canConnectX_neg && canConnectZ_pos && canConnectXn_Z_pos);
		final boolean hasXpZp = canConnectNone || (canConnectX_pos && canConnectZ_pos && canConnectXp_Z_pos);
		
		final boolean hasZnYn = canConnectNone || (canConnectZ_neg && canConnectY_neg && canConnectZn_Y_neg);
		final boolean hasZpYn = canConnectNone || (canConnectZ_pos && canConnectY_neg && canConnectZp_Y_neg);
		final boolean hasZnYp = canConnectNone || (canConnectZ_neg && canConnectY_pos && canConnectZn_Y_pos);
		final boolean hasZpYp = canConnectNone || (canConnectZ_pos && canConnectY_pos && canConnectZp_Y_pos);
		
		{// z plane
			if (hasXnYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_YN);
			}
			
			if (hasXpYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_YN);
			}
			
			if (hasXnYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_YP);
			}
			
			if (hasXpYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_YP);
			}
		}
		
		{// x plane
			if (hasZnYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZN_YN);
			}
			
			if (hasZpYn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZP_YN);
			}
			
			if (hasZnYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZN_YP);
			}
			
			if (hasZpYp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZP_YP);
			}
		}
		
		{// z plane
			if (hasXnZn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_ZN);
			}
			
			if (hasXpZn) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_ZN);
			}
			
			if (hasXnZp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN_ZP);
			}
			
			if (hasXpZp) {
				addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP_ZP);
			}
		}
		
		// central nodes
		if (canConnectY_neg && !hasXnYn && !hasXpYn && !hasZnYn && !hasZpYn) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_YN);
		}
		if (canConnectY_pos && !hasXnYp && !hasXpYp && !hasZnYp && !hasZpYp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_YP);
		}
		if (canConnectZ_neg && !hasXnZn && !hasXpZn && !hasZnYn && !hasZnYp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZN);
		}
		if (canConnectZ_pos && !hasXnZp && !hasXpZp && !hasZpYn && !hasZpYp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_ZP);
		}
		if (canConnectX_neg && !hasXnYn && !hasXnYp && !hasXnZn && !hasXnZp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XN);
		}
		if (canConnectX_pos && !hasXpYn && !hasXpYp && !hasXpZn && !hasXpZp) {
			addCollisionBoxToList(blockPos, entityBox, collidingBoxes, AABB_XP);
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(blockPos);
		
		// get direct connections
		final int maskConnectY_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() - 1, blockPos.getZ()    ), EnumFacing.DOWN);
		final int maskConnectY_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY() + 1, blockPos.getZ()    ), EnumFacing.UP);
		final int maskConnectZ_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() - 1), EnumFacing.NORTH);
		final int maskConnectZ_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX()    , blockPos.getY()    , blockPos.getZ() + 1), EnumFacing.SOUTH);
		final int maskConnectX_neg = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() - 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.WEST);
		final int maskConnectX_pos = getConnectionMask(blockAccess, mutableBlockPos.setPos(blockPos.getX() + 1, blockPos.getY()    , blockPos.getZ()    ), EnumFacing.EAST);
		
		final boolean canConnectY_neg = maskConnectY_neg > 0;
		final boolean canConnectY_pos = maskConnectY_pos > 0;
		final boolean canConnectZ_neg = maskConnectZ_neg > 0;
		final boolean canConnectZ_pos = maskConnectZ_pos > 0;
		final boolean canConnectX_neg = maskConnectX_neg > 0;
		final boolean canConnectX_pos = maskConnectX_pos > 0;
		final boolean canConnectNone = !canConnectY_neg && !canConnectY_pos && !canConnectZ_neg && !canConnectZ_pos && !canConnectX_neg && !canConnectX_pos;
		
		// x axis
		final float xMin = canConnectNone || canConnectX_neg ? 0.0F : CENTER_MIN;
		final float xMax = canConnectNone || canConnectX_pos ? 1.0F : CENTER_MAX;
		
		// y axis
		final float yMin = canConnectNone || canConnectY_neg ? 0.0F : CENTER_MIN;
		final float yMax = canConnectNone || canConnectY_pos ? 1.0F : CENTER_MAX;
		
		// z axis
		final float zMin = canConnectNone || canConnectZ_neg ? 0.0F : CENTER_MIN;
		final float zMax = canConnectNone || canConnectZ_pos ? 1.0F : CENTER_MAX;
		
		return new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected boolean canSilkHarvest()
	{
		return true;
	}
	
	public int getConnectionMask(final IBlockAccess blockAccess, final BlockPos blockPos, final EnumFacing facing) {
		final IBlockState blockState = blockAccess.getBlockState(blockPos);
		return ( blockState.isFullCube()
		      || blockState.getBlock() instanceof BlockAbstractOmnipanel
		      || blockState.getMaterial() == Material.GLASS
		      || blockState.getBlock() instanceof BlockPane ? 1 : 0 )
		     + (blockState.isSideSolid(blockAccess, blockPos, facing.getOpposite()) ? 2 : 0);
	}
}