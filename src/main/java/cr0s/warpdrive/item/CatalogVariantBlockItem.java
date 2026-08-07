package cr0s.warpdrive.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/** A block item which restores visual-only creative subtypes formerly encoded as item metadata. */
public class CatalogVariantBlockItem extends BlockItem {

	private static final String TAG_VARIANT = "catalogVariant";
	private final int variantCount;
	private final String[] translationSuffixes;

	public CatalogVariantBlockItem(final Block block, final ItemGroup itemGroup,
	                               final int variantCount, final String... translationSuffixes) {
		super(block, new Item.Properties().tab(itemGroup));
		this.variantCount = variantCount;
		this.translationSuffixes = translationSuffixes;
	}

	@Override
	public void fillItemCategory(final ItemGroup group, final NonNullList<ItemStack> items) {
		if (!allowdedIn(group)) {
			return;
		}
		for (int variant = 0; variant < variantCount; variant++) {
			final ItemStack itemStack = new ItemStack(this);
			itemStack.getOrCreateTag().putInt(TAG_VARIANT, variant);
			items.add(itemStack);
		}
	}

	@Override
	public ITextComponent getName(final ItemStack itemStack) {
		final int variant = getVariant(itemStack);
		if (variant >= 0 && variant < translationSuffixes.length) {
			return new TranslationTextComponent(getDescriptionId() + "." + translationSuffixes[variant]);
		}
		return super.getName(itemStack);
	}

	public static int getVariant(final ItemStack itemStack) {
		return itemStack.hasTag() ? itemStack.getTag().getInt(TAG_VARIANT) : 0;
	}
}
