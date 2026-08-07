package cr0s.warpdrive.data;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Creative tabs for WarpDrive.
 *
 * 1.12.2 had two - itemGroup.warpdrive.main and itemGroup.warpdrive.hull - splitting machines from
 * the hull building set, which is sensible once the hull family exists at sixteen colours across
 * three tiers and five shapes. Until those are ported everything fits comfortably in one tab, so
 * the hull tab arrives with the blocks that justify it.
 *
 * The icon is the Ship Core: the one block nothing else in the mod works without. 1.12.2 picked a
 * random item each time instead, which is charming but makes the tab harder to find by eye.
 */
public final class WarpDriveItemGroup {

	/** Named to match the existing itemGroup.warpdrive lang key. */
	public static final ItemGroup MAIN = new ItemGroup("warpdrive") {
		@Nonnull
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(Registration.SHIP_CORE_ITEM.get());
		}
	};

	/** The large coloured structural catalog keeps the main machinery tab usable. */
	public static final ItemGroup HULL = new ItemGroup("warpdrive.hull") {
		@Nonnull
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(Registration.HULL_BLOCK_ITEMS.get("hull.basic.plain-white").get());
		}
	};

	private WarpDriveItemGroup() {
	}
}
