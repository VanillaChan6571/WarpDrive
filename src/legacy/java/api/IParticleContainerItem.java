package cr0s.warpdrive.api;

import net.minecraft.item.ItemStack;

public interface IParticleContainerItem {
	
	String TAG_PARTICLE = "particle";
	String TAG_AMOUNT_TO_CONSUME = "amountToConsume";
	String TAG_TICK_TO_CONSUME = "tickToConsume";
	
	ParticleStack getParticleStack(final ItemStack container);
	
	int getCapacity(final ItemStack container);
	
	boolean isEmpty(final ItemStack container);
	
	// fills the container and return how much could be transferred or 0 if container is empty or contains different particles
	int fill(final ItemStack container, final ParticleStack resource, final boolean doFill);
	
	// drains the container and return how much could be transferred or null if container is empty or contains different particles
	ParticleStack drain(final ItemStack container, final ParticleStack resource, final boolean doDrain);
	
	// called during recipe match to set amount to consume in next call to getContainerItem
	void setAmountToConsume(final ItemStack container, final int amount);
}