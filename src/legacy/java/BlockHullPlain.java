package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumHullPlainType;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockColored;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockHullPlain extends BlockAbstractBase implements IDamageReceiver {
	
	final EnumHullPlainType enumHullPlainType;
	
	public BlockHullPlain(final String registryName, final EnumTier enumTier, final EnumHullPlainType enumHullPlainType) {
		super(registryName, enumTier, Material.ROCK);
		
		this.enumHullPlainType = enumHullPlainType;
		setHardness(WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()] * 5.0F / 3.0F);
		setHarvestLevel("pickaxe", WarpDriveConfig.HULL_HARVEST_LEVEL[enumTier.getIndex()]);
		setTranslationKey("warpdrive.hull." + enumTier.getName() + ".plain.");
		setDefaultState(getDefaultState()
				                .withProperty(BlockColored.COLOR, EnumDyeColor.WHITE)
		               );
		setCreativeTab(WarpDrive.creativeTabHull);
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockHull(this);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(@Nonnull final IBlockState blockState) {
		return EnumPushReaction.BLOCK;
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
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public MapColor getMapColor(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		return MapColor.getBlockColor(blockState.getValue(BlockColored.COLOR));
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
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockColored.COLOR);
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
			world.setBlockState(blockPos, WarpDrive.blockHulls_plain[enumTier.getIndex() - 1][enumHullPlainType.ordinal()]
			                              .getDefaultState()
			                              .withProperty(BlockColored.COLOR, blockState.getValue(BlockColored.COLOR)), 2);
		}
		return 0;
	}
}
