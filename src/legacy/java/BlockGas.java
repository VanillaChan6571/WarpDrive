package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.block.ItemBlockAbstractBase;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumGasColor;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Random;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockGas extends BlockAbstractBase {
	
	public static final PropertyEnum<EnumGasColor> COLOR = PropertyEnum.create("color", EnumGasColor.class);
	
	public BlockGas(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.FIRE);
		
		setHardness(0.0F);
		setTranslationKey("warpdrive.decoration.gas");
		
		setDefaultState(getDefaultState()
				                .withProperty(COLOR, EnumGasColor.RED)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, COLOR);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				.withProperty(COLOR, EnumGasColor.get(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(COLOR).ordinal();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, true, false);
	}
	
	@Override
	public void getSubBlocks(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		for (final EnumGasColor enumGasColor : EnumGasColor.values()) {
			list.add(new ItemStack(this, 1, enumGasColor.ordinal()));
		}
	}
	
	@Override
	public int damageDropped(@Nonnull final IBlockState blockState) {
		return blockState.getBlock().getMetaFromState(blockState);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(@Nonnull final IBlockState blockState) {
		return true;
	}
	
	@Override
	public boolean isAir(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return NULL_AABB;
	}
	
	@Override
	public boolean isReplaceable(@Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@Override
	public boolean canPlaceBlockAt(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@Override
	public boolean canCollideCheck(@Nonnull final IBlockState blockState, final boolean hitIfLiquid) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(@Nonnull final IBlockState blockState) {
		return EnumPushReaction.DESTROY;
	}
	
	@Nonnull
	@Override
	public Item getItemDropped(@Nonnull final IBlockState blockState, @Nonnull final Random rand, final int fortune) {
		return Items.AIR;
	}
	
	@Override
	public int quantityDropped(@Nonnull final Random random) {
		return 0;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTranslucent(@Nonnull final IBlockState blockState) {
		return true;
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
		final BlockPos blockPosSide = blockPos.offset(facing);
		final EnumFacing opposite = facing.getOpposite();
		final IBlockState blockStateSide = blockAccess.getBlockState(blockPosSide);
		if (blockStateSide.getBlock() instanceof BlockGas) {
			return blockState.getValue(COLOR) != blockStateSide.getValue(COLOR);
		}
		return !blockStateSide.doesSideBlockRendering(blockAccess, blockPosSide, opposite);
	}
	
	@Override
	public boolean isCollidable() {
		return false;
	}
	
	@Override
	public void onBlockAdded(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		// Gas blocks are only allowed in space
		if (CelestialObjectManager.hasAtmosphere(world, blockPos.getX(), blockPos.getZ())) {
			world.setBlockToAir(blockPos);
		}
	}
}