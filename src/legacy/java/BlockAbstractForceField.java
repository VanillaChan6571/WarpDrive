package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public abstract class BlockAbstractForceField extends BlockAbstractContainer {
	
	BlockAbstractForceField(final String registryName, final EnumTier enumTier, final Material material) {
		super(registryName, enumTier, material);
		
		setHardness(WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[enumTier.getIndex()] * 5.0F / 3.0F);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(@Nonnull final IBlockState blockState) {
		return EnumPushReaction.BLOCK;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected boolean canSilkHarvest() {
		return false;
	}
	
	@Override
	public void onEMP(@Nonnull final World world, @Nonnull final BlockPos blockPos, final float efficiency) {
		super.onEMP(world, blockPos, efficiency * (1.0F - 0.2F * (enumTier.getIndex() - 1)));
	}
}
