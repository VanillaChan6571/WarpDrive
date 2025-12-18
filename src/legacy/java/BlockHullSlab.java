package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Random;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockHullSlab extends BlockAbstractBase implements IBlockBase, IDamageReceiver {
	
	// Metadata values are
	// 0-5 for plain slabs orientations
	// 6-11 for tiled slabs orientations
	// 12 for plain double slab
	// 13-15 for tiled double slabs
	
	private static final AxisAlignedBB AABB_HALF_DOWN   = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 0.50D, 1.00D);
	private static final AxisAlignedBB AABB_HALF_UP     = new AxisAlignedBB(0.00D, 0.50D, 0.00D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_HALF_NORTH  = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 1.00D, 0.50D);
	private static final AxisAlignedBB AABB_HALF_SOUTH  = new AxisAlignedBB(0.00D, 0.00D, 0.50D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_HALF_EAST   = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 0.50D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_HALF_WEST   = new AxisAlignedBB(0.50D, 0.00D, 0.00D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_FULL        = FULL_BLOCK_AABB;
	
	public static final PropertyEnum<EnumVariant> VARIANT = PropertyEnum.create("variant", EnumVariant.class);
	
	final EnumTier enumTier;
	private final IBlockState blockStateHull;
	
	public BlockHullSlab(@Nonnull final String registryName, @Nonnull final EnumTier enumTier, @Nonnull final IBlockState blockStateHull) {
		super(registryName, enumTier, Material.ROCK);
		
		this.fullBlock = false;
		this.setLightOpacity(0);
		this.useNeighborBrightness = true;
		
		this.enumTier = enumTier;
		this.blockStateHull = blockStateHull;
		setHardness(WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()] * 5.0F / 3.0F);
		setHarvestLevel("pickaxe", WarpDriveConfig.HULL_HARVEST_LEVEL[enumTier.getIndex()]);
		setTranslationKey("warpdrive.hull." + enumTier.getName() + ".slab." + EnumDyeColor.byMetadata(blockStateHull.getBlock().getMetaFromState(blockStateHull)).getTranslationKey());
		
		setDefaultState(getDefaultState()
				                .withProperty(VARIANT, EnumVariant.PLAIN_DOWN)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, VARIANT);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
		        .withProperty(VARIANT, EnumVariant.get(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(VARIANT).ordinal();
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(@Nonnull final IBlockState blockState) {
		return EnumPushReaction.BLOCK;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		list.add(new ItemStack(this, 1, 0));
		list.add(new ItemStack(this, 1, 2));
		list.add(new ItemStack(this, 1, 6));
		list.add(new ItemStack(this, 1, 8));
		list.add(new ItemStack(this, 1, 12));
		list.add(new ItemStack(this, 1, 13));
		list.add(new ItemStack(this, 1, 14));
		list.add(new ItemStack(this, 1, 15));
	}
	
	@Override
	public int damageDropped(@Nonnull final IBlockState blockState) {
		final int metadata = getMetaFromState(blockState);
		return metadata <= 1 ? 0    // plain horizontal
		     : metadata <= 5 ? 2    // plain vertical
		     : metadata <= 7 ? 6    // tiled horizontal
		     : metadata <= 11 ? 8   // tiled vertical
		     : metadata;            // others
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int getLightOpacity(@Nonnull final IBlockState blockState) {
		final EnumVariant variant = blockState.getValue(VARIANT);
		if ( variant.getIsDouble()
		  || variant.getFacing().getYOffset() != 0 ) {
			return 255;
		}
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
		return blockState.getValue(VARIANT).getAxisAlignedBB();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTopSolid(@Nonnull final IBlockState blockState) {
		final EnumVariant variant = blockState.getValue(VARIANT);
		return variant.getIsDouble() || variant.getFacing() == EnumFacing.UP;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public BlockFaceShape getBlockFaceShape(@Nonnull final IBlockAccess blockAccess, @Nonnull final IBlockState blockState, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing enumFacing) {
		final EnumVariant variant = blockState.getValue(VARIANT);
		if (variant.getIsDouble()) {
			return BlockFaceShape.SOLID;
		}
		return enumFacing == variant.getFacing() ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(@Nonnull final IBlockState blockState) {
		return blockState.getValue(VARIANT).getIsDouble();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(@Nonnull final IBlockState blockState) {
		return blockState.getValue(VARIANT).getIsDouble();
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                        final float hitX, final float hitY, final float hitZ, final int metadata,
	                                        @Nonnull final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final IBlockState blockState = getStateFromMeta(metadata);
		
		// full block?
		if (blockState.getValue(VARIANT).getIsDouble()) {
			return blockState;
		}
		
		// horizontal slab?
		if (metadata == 0) {
			// reuse vanilla logic
			final EnumVariant variant = (facing != EnumFacing.DOWN && (facing == EnumFacing.UP || hitY <= 0.5F) ? EnumVariant.PLAIN_DOWN : EnumVariant.PLAIN_UP);
			return blockState.withProperty(VARIANT, variant);
		} else if (metadata == 6) {
			// reuse vanilla logic
			final EnumVariant variant = (facing != EnumFacing.DOWN && (facing == EnumFacing.UP || hitY <= 0.5F) ? EnumVariant.TILED_DOWN : EnumVariant.TILED_UP);
			return blockState.withProperty(VARIANT, variant);
		}
		// vertical slab?
		if (metadata == 2) {
			if (facing != EnumFacing.DOWN && facing != EnumFacing.UP) {
				switch(facing) {
				case NORTH: return blockState.withProperty(VARIANT, EnumVariant.PLAIN_SOUTH);
				case SOUTH: return blockState.withProperty(VARIANT, EnumVariant.PLAIN_NORTH);
				case WEST: return blockState.withProperty(VARIANT, EnumVariant.PLAIN_EAST);
				case EAST: return blockState.withProperty(VARIANT, EnumVariant.PLAIN_WEST);
				}
			}
			// is X the furthest away from center?
			if (Math.abs(hitX - 0.5F) > Math.abs(hitZ - 0.5F)) {
				// west (4) vs east (5)
				final EnumVariant variant = hitX > 0.5F ? EnumVariant.PLAIN_EAST : EnumVariant.PLAIN_WEST;
				return blockState.withProperty(VARIANT, variant);
			}
			// north (2) vs south (3)
			final EnumVariant variant = hitZ > 0.5F ? EnumVariant.PLAIN_SOUTH : EnumVariant.PLAIN_NORTH;
			return blockState.withProperty(VARIANT, variant);
		}
		if (metadata == 8) {
			if (facing != EnumFacing.DOWN && facing != EnumFacing.UP) {
				switch(facing) {
				case NORTH: return blockState.withProperty(VARIANT, EnumVariant.TILED_SOUTH);
				case SOUTH: return blockState.withProperty(VARIANT, EnumVariant.TILED_NORTH);
				case WEST: return blockState.withProperty(VARIANT, EnumVariant.TILED_EAST);
				case EAST: return blockState.withProperty(VARIANT, EnumVariant.TILED_WEST);
				}
			}
			// is X the furthest away from center?
			if (Math.abs(hitX - 0.5F) > Math.abs(hitZ - 0.5F)) {
				// west (4) vs east (5)
				final EnumVariant variant = hitX > 0.5F ? EnumVariant.TILED_EAST : EnumVariant.TILED_WEST;
				return blockState.withProperty(VARIANT, variant);
			}
			// north (2) vs south (3)
			final EnumVariant variant = hitZ > 0.5F ? EnumVariant.TILED_SOUTH : EnumVariant.TILED_NORTH;
			return blockState.withProperty(VARIANT, variant);
		}
		return getStateById(metadata);
	}
	
	@Override
	public int quantityDropped(@Nonnull final Random random) {
		return 1;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		final EnumVariant variant = blockState.getValue(VARIANT);
		final BlockPos blockPosSide = blockPos.offset(facing);
		final boolean doesSideBlockRendering = blockAccess.getBlockState(blockPosSide).doesSideBlockRendering(blockAccess, blockPosSide, facing.getOpposite());
		if (facing != variant.getFacing()) {
			return !doesSideBlockRendering;
		}
		return !doesSideBlockRendering;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return blockState.getValue(VARIANT).getIsDouble();
	}
	
	@Override
	public boolean doesSideBlockRendering(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos, final EnumFacing side) {
		final EnumVariant variant = blockState.getValue(VARIANT);
		return variant.getIsDouble() || variant.getFacing() == side;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isSideSolid(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing side) {
		final EnumFacing enumFacing = blockState.getValue(VARIANT).getFacing();
		return enumFacing == side;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public MapColor getMapColor(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		return blockStateHull.getMapColor(blockAccess, blockPos);
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(final ItemStack itemStack) {
		return enumTier;
	}
	
	@Nonnull
	@Override
	public IRarity getForgeRarity(@Nonnull final ItemStack itemStack) {
		return enumTier.getForgeRarity();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockHullSlab(this);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		// no operation
	}
	
	@Override
	public float getBlockHardness(final IBlockState blockState, final World world, final BlockPos blockPos,
	                              final DamageSource damageSource, final int damageParameter, final Vector3 damageDirection, final int damageLevel) {
		// TODO: adjust hardness to damage type/color
		return WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()];
	}
	
	@Override
	public int applyDamage(final IBlockState blockState, final World world, final BlockPos blockPos,
	                       final DamageSource damageSource, final int damageParameter, final Vector3 damageDirection, final int damageLevel) {
		if (damageLevel <= 0) {
			return 0;
		}
		if (enumTier == EnumTier.BASIC) {
			world.setBlockToAir(blockPos);
		} else {
			world.setBlockState(blockPos, WarpDrive.blockHulls_slab[enumTier.getIndex() - 1][blockStateHull.getBlock().getMetaFromState(blockStateHull)]
			                              .getDefaultState()
			                              .withProperty(VARIANT, blockState.getValue(VARIANT)), 2);
		}
		return 0;
	}
	
	public enum EnumVariant implements IStringSerializable {
		PLAIN_DOWN  ("plain_down"  , false, true , EnumFacing.DOWN , AABB_HALF_DOWN),
		PLAIN_UP    ("plain_up"    , false, true , EnumFacing.UP   , AABB_HALF_UP),
		PLAIN_NORTH ("plain_north" , false, true , EnumFacing.NORTH, AABB_HALF_NORTH),
		PLAIN_SOUTH ("plain_south" , false, true , EnumFacing.SOUTH, AABB_HALF_SOUTH),
		PLAIN_WEST  ("plain_west"  , false, true , EnumFacing.WEST , AABB_HALF_EAST),
		PLAIN_EAST  ("plain_east"  , false, true , EnumFacing.EAST , AABB_HALF_WEST),
		
		TILED_DOWN  ("tiled_down"  , false, false, EnumFacing.DOWN , AABB_HALF_DOWN),
		TILED_UP    ("tiled_up"    , false, false, EnumFacing.UP   , AABB_HALF_UP),
		TILED_NORTH ("tiled_north" , false, false, EnumFacing.NORTH, AABB_HALF_NORTH),
		TILED_SOUTH ("tiled_south" , false, false, EnumFacing.SOUTH, AABB_HALF_SOUTH),
		TILED_WEST  ("tiled_west"  , false, false, EnumFacing.WEST , AABB_HALF_EAST),
		TILED_EAST  ("tiled_east"  , false, false, EnumFacing.EAST , AABB_HALF_WEST),
		
		PLAIN_FULL  ("plain_full"  , true , true , EnumFacing.DOWN , AABB_FULL),
		TILED_FULL_X("tiled_full_x", true , false, EnumFacing.DOWN , AABB_FULL),
		TILED_FULL_Y("tiled_full_y", true , false, EnumFacing.DOWN , AABB_FULL),
		TILED_FULL_Z("tiled_full_z", true , false, EnumFacing.DOWN , AABB_FULL);
		
		private final String name;
		private final boolean isDouble;
		private final boolean isPlain;
		private final EnumFacing facing;
		private final AxisAlignedBB axisAlignedBB;
		
		// cached values
		public static final int length;
		private static final HashMap<Integer, EnumVariant> ID_MAP = new HashMap<>();
		
		static {
			length = EnumVariant.values().length;
			for (final EnumVariant variant : values()) {
				ID_MAP.put(variant.ordinal(), variant);
			}
		}
		
		EnumVariant(@Nonnull final String name, final boolean isDouble, final boolean isPlain, @Nonnull final EnumFacing facing, @Nonnull final AxisAlignedBB axisAlignedBB) {
			this.name = name;
			this.isDouble = isDouble;
			this.isPlain = isPlain;
			this.facing = facing;
			this.axisAlignedBB = axisAlignedBB;
		}
		
		public static EnumVariant get(final int metadata) {
			return ID_MAP.get(metadata);
		}
		
		@Nonnull
		@Override
		public String getName()
		{
			return name;
		}
		
		public boolean getIsDouble()
		{
			return isDouble;
		}
		
		public boolean getIsPlain()
		{
			return isPlain;
		}
		
		public EnumFacing getFacing() {
			return facing;
		}
		
		public AxisAlignedBB getAxisAlignedBB() {
			return axisAlignedBB;
		}
	}
}
