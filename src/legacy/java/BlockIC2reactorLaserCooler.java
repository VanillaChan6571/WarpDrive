package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.BlockAbstractRotatingContainer;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockIC2reactorLaserCooler extends BlockAbstractRotatingContainer {
	
	public BlockIC2reactorLaserCooler(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.energy.ic2_reactor_laser_cooler");
		ignoreFacingOnPlacement = true;
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world,final  int metadata) {
		return new TileEntityIC2reactorLaserMonitor();
	}
}
