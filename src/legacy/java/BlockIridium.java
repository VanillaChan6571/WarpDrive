package cr0s.warpdrive.block.passive;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.material.Material;

public class BlockIridium extends BlockAbstractBase {
	
	public BlockIridium(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(3.4F);
		setResistance(360.0F * 5 / 3);
		setTranslationKey("warpdrive.passive.iridium_block");
	}
}