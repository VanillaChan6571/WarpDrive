package cr0s.warpdrive.block.passive;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.material.Material;

public class BlockHighlyAdvancedMachine extends BlockAbstractBase {
	
	public BlockHighlyAdvancedMachine(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(5.0F);
		setTranslationKey("warpdrive.passive.highly_advanced_machine");
	}
}