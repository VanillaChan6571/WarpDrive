package cr0s.warpdrive.api;

import net.minecraft.item.ItemStack;

public interface IAirContainerItem {
	
	// Return true if that itemStack is Air compatible (i.e. may or already contains air)
	boolean canContainAir(ItemStack itemStack);
	
	// Return maximum number of calls to consumeAir() when full
	int getMaxAirStorage(ItemStack itemStack);
	
	// Return number of calls to consumeAir() for a single item to empty the container
	int getCurrentAirStorage(ItemStack itemStack);
	
	// Consume a breath of air from the container
	ItemStack consumeAir(ItemStack itemStack);
	
	// Return duration of air for a single call to consumeAir(). Defaults to 300 ticks.
	int getAirTicksPerConsumption(ItemStack itemStack);
	
	// Return an empty air container
	ItemStack getEmptyAirContainer(ItemStack itemStack);
	
	// Return a full air container
	ItemStack getFullAirContainer(ItemStack itemStack);
}
