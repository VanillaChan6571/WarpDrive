package cr0s.warpdrive.api;

import cr0s.warpdrive.data.EnumTier;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public interface IItemBase {
	
	// wrapper for Forge ItemExpireEvent
	void onEntityExpireEvent(final EntityItem entityItem, final ItemStack itemStack);
	
	@Nonnull
	EnumTier getTier(final ItemStack itemStack);
	
	// getForgeRarity is defined in Item
	
	@SideOnly(Side.CLIENT)
	void modelInitialisation();
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	ModelResourceLocation getModelResourceLocation(final ItemStack itemStack);
}