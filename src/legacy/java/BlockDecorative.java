package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.EnumDecorativeType;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDecorative extends BlockAbstractBase {
	
	public static final PropertyEnum<EnumDecorativeType> TYPE = PropertyEnum.create("type", EnumDecorativeType.class);
	private static ItemStack[] itemStackCache;
	
	public BlockDecorative(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setLightOpacity(0);
		setHardness(1.5f);
		setTranslationKey("warpdrive.decoration.decorative.");
		
		setDefaultState(getDefaultState()
				                .withProperty(TYPE, EnumDecorativeType.PLAIN)
		               );
		itemStackCache = new ItemStack[EnumDecorativeType.length];
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, TYPE);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				.withProperty(TYPE, EnumDecorativeType.byMetadata(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(TYPE).ordinal();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockDecorative(this);
	}
	
	@Override
	public void getSubBlocks(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		for (final EnumDecorativeType enumDecorativeType : EnumDecorativeType.values()) {
			list.add(new ItemStack(this, 1, enumDecorativeType.ordinal()));
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public Material getMaterial(final IBlockState blockState) {
		return blockState == null || blockState.getValue(TYPE) != EnumDecorativeType.GLASS ?  Material.IRON :  Material.GLASS;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int getLightOpacity(@Nonnull final IBlockState blockState) {
		return  blockState.getValue(TYPE) != EnumDecorativeType.GLASS ? 255 : 0;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return blockState.getValue(TYPE) != EnumDecorativeType.GLASS;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTranslucent(@Nonnull final IBlockState blockState) {
		return blockState.getValue(TYPE) == EnumDecorativeType.GLASS;
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
		final IBlockState blockStateSide = blockAccess.getBlockState(blockPosSide);
		if (blockStateSide.getBlock().isAir(blockStateSide, blockAccess, blockPosSide)) {
			return true;
		}
		if ( blockStateSide.getBlock() instanceof BlockDecorative
		  && blockState.getValue(TYPE) == EnumDecorativeType.GLASS
		  && blockStateSide.getValue(TYPE) == EnumDecorativeType.GLASS ) {
			return false;
		}
		final boolean doesSideBlockRendering = blockAccess.getBlockState(blockPosSide).doesSideBlockRendering(blockAccess, blockPosSide, facing.getOpposite());
		return !doesSideBlockRendering;
	}
	
	@Override
	public int damageDropped(@Nonnull final IBlockState blockState) {
		return blockState.getBlock().getMetaFromState(blockState);
	}
	
	@Nonnull
	public static ItemStack getItemStack(final EnumDecorativeType enumDecorativeType) {
		if (enumDecorativeType != null) {
			final int damage = enumDecorativeType.ordinal();
			if (itemStackCache[damage] == null) {
				itemStackCache[damage] = new ItemStack(WarpDrive.blockDecorative, 1, damage);
			}
			return itemStackCache[damage];
		}
		return ItemStack.EMPTY;
	}
	
	@Nonnull
	public static ItemStack getItemStackNoCache(@Nonnull final EnumDecorativeType enumDecorativeType, final int amount) {
		return new ItemStack(WarpDrive.blockDecorative, amount, enumDecorativeType.ordinal());
	}
}
