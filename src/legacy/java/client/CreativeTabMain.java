package cr0s.warpdrive.client;

import cr0s.warpdrive.item.ItemShipToken;

import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class CreativeTabMain extends CreativeTabAbstractBase {
	
	public CreativeTabMain(final String label) {
		super(label, 2861);
	}
	
	@Nonnull
	@Override
	@SideOnly(Side.CLIENT)
	public ItemStack createIcon() {
		return ItemShipToken.getItemStack(random);
    }
}
