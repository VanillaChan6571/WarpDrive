package cr0s.warpdrive.client;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public abstract class CreativeTabAbstractBase extends CreativeTabs {
	
	static protected Random random = new Random();
	private ItemStack itemStack = ItemStack.EMPTY;
	private long timeLastChange;
	private long period;
	
	public CreativeTabAbstractBase(final String label, final long period) {
		super(label);
		
		this.period = period;
	}
	
	@Nonnull
	@Override
	public ItemStack getIcon() {
		final long timeCurrent = System.currentTimeMillis();
		if (timeLastChange < timeCurrent) {
			timeLastChange = timeCurrent + period;
			itemStack = createIcon();
		}
		return itemStack;
	}
}
