package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.BlockAbstractRotatingContainer;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockEnanReactorLaser extends BlockAbstractRotatingContainer {
	
	public BlockEnanReactorLaser(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setResistance(60.0F * 5 / 3);
		setTranslationKey("warpdrive.energy.enan_reactor_laser");
		ignoreFacingOnPlacement = true;
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityEnanReactorLaser();
	}
}