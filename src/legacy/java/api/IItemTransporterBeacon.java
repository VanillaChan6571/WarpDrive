package cr0s.warpdrive.api;

import net.minecraft.item.ItemStack;

public interface IItemTransporterBeacon {
	
	boolean isActive(final ItemStack itemStack);
}
