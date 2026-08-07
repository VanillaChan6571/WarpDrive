package cr0s.warpdrive.item;

import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * Visual-only electromagnetic cell used while the particle storage logic is still deferred.
 *
 * 1.12 exposed six charged stacks for every particle type. Keeping those as tagged creative
 * stacks preserves the JEI/catalog presentation without creating another registry item for every
 * fill level.
 */
public class CatalogElectromagneticCellItem extends Item {

	private static final String TAG_FILL_INDEX = "catalogFillIndex";
	private static final float[] FILL_LEVELS = { 0.1F, 0.3F, 0.5F, 0.7F, 0.9F, 1.0F };

	public CatalogElectromagneticCellItem() {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
	}

	@Override
	public void fillItemCategory(final ItemGroup group, final NonNullList<ItemStack> items) {
		if (!allowdedIn(group)) {
			return;
		}
		for (int index = 0; index < FILL_LEVELS.length; index++) {
			final ItemStack itemStack = new ItemStack(this);
			itemStack.getOrCreateTag().putInt(TAG_FILL_INDEX, index);
			items.add(itemStack);
		}
	}

	public static float getFillLevel(final ItemStack itemStack) {
		if (!itemStack.hasTag()) {
			return FILL_LEVELS[0];
		}
		final int index = itemStack.getTag().getInt(TAG_FILL_INDEX);
		return FILL_LEVELS[Math.max(0, Math.min(FILL_LEVELS.length - 1, index))];
	}
}
