package cr0s.warpdrive.block;

import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockLaser extends BlockAbstractContainer {
	
	public BlockLaser(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(50.0F);
		setResistance(20.0F * 5 / 3);
		setTranslationKey("warpdrive.machines.laser");
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityLaser();
	}
}