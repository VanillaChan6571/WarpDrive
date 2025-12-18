package cr0s.warpdrive.api;

import cr0s.warpdrive.data.EnumTier;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.IRarity;

public interface IBlockBase {
	
	@Nonnull
	EnumTier getTier(final ItemStack itemStack);
	
	@Nonnull
	IRarity getForgeRarity(final ItemStack itemStack);
	
    @Nullable
    ItemBlock createItemBlock();
    
    void modelInitialisation();
}
