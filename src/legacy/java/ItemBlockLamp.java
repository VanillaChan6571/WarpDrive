package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.block.ItemBlockAbstractBase;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemBlockLamp extends ItemBlockAbstractBase {
	
	public ItemBlockLamp(final Block block) {
		super(block, true, false);
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		final ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		final String variant = "inventory";
		return new ModelResourceLocation(resourceLocation, variant);
	}
}
