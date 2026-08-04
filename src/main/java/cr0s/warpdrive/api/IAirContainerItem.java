package cr0s.warpdrive.api;

import net.minecraft.item.ItemStack;

/**
 * An item that holds breathable air, consumed one breath at a time.
 *
 * Ported unchanged from 1.12.2 so third-party air containers keep working the same way: storage is
 * counted in *breaths*, not ticks, and each breath lasts getAirTicksPerConsumption ticks.
 */
public interface IAirContainerItem {

	/** True if this stack can hold air, i.e. it is an air container that is not already full. */
	boolean canContainAir(ItemStack itemStack);

	/** Number of consumeAir() calls this container accepts when full. */
	int getMaxAirStorage(ItemStack itemStack);

	/** Number of consumeAir() calls left before this container is empty. */
	int getCurrentAirStorage(ItemStack itemStack);

	/** Take a single breath out of the container. */
	ItemStack consumeAir(ItemStack itemStack);

	/** How long one breath lasts, in ticks. 1.12.2 default is 300. */
	int getAirTicksPerConsumption(ItemStack itemStack);

	/** An emptied version of this container. */
	ItemStack getEmptyAirContainer(ItemStack itemStack);

	/** A filled version of this container. */
	ItemStack getFullAirContainer(ItemStack itemStack);
}
