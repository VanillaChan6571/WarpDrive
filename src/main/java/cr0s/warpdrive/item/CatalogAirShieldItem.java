package cr0s.warpdrive.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

/** Restores the sixteen dye-tinted Air Shield stacks exposed by the 1.12 omnipanel. */
public class CatalogAirShieldItem extends CatalogVariantBlockItem {

	private static final int[] COLORS = {
		0xFFFFFF, 0xFF5A02, 0xF269FF, 0x80AAFF,
		0xFFEE3C, 0x90E801, 0xFB0680, 0x2C2C2C,
		0x686868, 0x0FD7FF, 0x5D1072, 0x4351CC,
		0x99572E, 0x75993C, 0xCC4D41, 0x080808
	};

	public CatalogAirShieldItem(final Block block, final ItemGroup itemGroup) {
		super(block, itemGroup, COLORS.length);
	}

	public static int getTintColor(final ItemStack itemStack, final int tintIndex) {
		if (tintIndex != 0) {
			return 0xFFFFFF;
		}
		final int variant = getVariant(itemStack);
		return COLORS[Math.max(0, Math.min(COLORS.length - 1, variant))];
	}
}
