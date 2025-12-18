package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockHullGlass extends BlockColored implements IBlockBase, IDamageReceiver {
	
	private final EnumTier enumTier;
	
	public BlockHullGlass(@Nonnull final String registryName, @Nonnull final EnumTier enumTier) {
		super(Material.GLASS);
		
		this.enumTier = enumTier;
		setHardness(WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()] * 5.0F / 3.0F);
		setHarvestLevel("pickaxe", WarpDriveConfig.HULL_HARVEST_LEVEL[enumTier.getIndex()]);
		setSoundType(SoundType.GLASS);
		setCreativeTab(WarpDrive.creativeTabHull);
		setTranslationKey("warpdrive.hull." + enumTier.getName() + ".glass.");
		setRegistryName(registryName);
		WarpDrive.register(this, new ItemBlockHull(this));
		
		setLightLevel(10.0F / 15.0F);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(@Nonnull final IBlockState blockState) {
		return EnumPushReaction.BLOCK;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	// return true to support pressure plates, etc. since it's glass material
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTopSolid(@Nonnull final IBlockState blockState) {
		return true;
	}
	
	// return false to give full rendering transparency (but loose vertical redstone wire)
	@SuppressWarnings("deprecation")
	@Override
	public boolean isBlockNormalCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	// return true to give proper door placement since it's glass material
	@SuppressWarnings("deprecation")
	@Override
	public boolean isNormalCube(@Nonnull final IBlockState blockState) {
		return true;
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(final ItemStack itemStack) {
		return enumTier;
	}
	
	@Nonnull
	@Override
	public IRarity getForgeRarity(@Nonnull final ItemStack itemStack) {
		return getTier(itemStack).getForgeRarity();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockHull(this);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		// no operation
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
		final EnumFacing opposite = facing.getOpposite();
		if ( blockStateSide.getBlock() instanceof BlockGlass
		  || blockStateSide.getBlock() instanceof BlockHullGlass ) {
			return blockState.getBlock().getMetaFromState(blockState)
				!= blockStateSide.getBlock().getMetaFromState(blockStateSide);
		}
		return !blockStateSide.doesSideBlockRendering(blockAccess, blockPosSide, opposite);
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
			world.setBlockState(blockPos, WarpDrive.blockHulls_glass[enumTier.getIndex() - 1]
			                              .getDefaultState()
			                              .withProperty(COLOR, blockState.getValue(COLOR)), 2);
		}
		return 0;
	}
}
