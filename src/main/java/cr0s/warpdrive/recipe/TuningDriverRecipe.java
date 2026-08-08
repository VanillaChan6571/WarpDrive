package cr0s.warpdrive.recipe;

import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.item.TuningDriverItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;

/**
 * Encodes dye colours as hexadecimal digits into a tuning driver, matching RecipeTuningDriver.
 * Dyes are read in crafting-slot order because that ordering defined the legacy channel value.
 */
public class TuningDriverRecipe extends SpecialRecipe {

	private final TuningDriverItem.Mode mode;
	private final int dyeCount;

	public TuningDriverRecipe(final ResourceLocation id, final TuningDriverItem.Mode mode,
	                         final int dyeCount) {
		super(id);
		this.mode = mode;
		this.dyeCount = dyeCount;
	}

	@Override
	public boolean matches(final CraftingInventory inventory, final World world) {
		return findResult(inventory) != null;
	}

	@Nonnull
	@Override
	public ItemStack assemble(final CraftingInventory inventory) {
		final ItemStack result = findResult(inventory);
		return result == null ? ItemStack.EMPTY : result;
	}

	private ItemStack findResult(final CraftingInventory inventory) {
		ItemStack driver = ItemStack.EMPTY;
		boolean foundRedstone = false;
		int foundDyes = 0;
		int value = 0;

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			final ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.getItem() instanceof TuningDriverItem
				&& ((TuningDriverItem) stack.getItem()).getMode() == mode) {
				if (!driver.isEmpty()) {
					return null;
				}
				driver = stack;
			} else if (stack.getItem() == Items.REDSTONE) {
				if (foundRedstone) {
					return null;
				}
				foundRedstone = true;
			} else if (getLegacyDyeDamage(stack) >= 0) {
				foundDyes++;
				value = value * 16 + getLegacyDyeDamage(stack);
			} else {
				return null;
			}
		}

		if (driver.isEmpty() || !foundRedstone || foundDyes != dyeCount) {
			return null;
		}
		return TuningDriverItem.setValue(driver.copy(), value);
	}

	/** Forge dye tags preserve the old ore-dictionary support for equivalent modded dyes. */
	private static int getLegacyDyeDamage(final ItemStack stack) {
		if (stack.getItem().is(Tags.Items.DYES_BLACK))      return 0;
		if (stack.getItem().is(Tags.Items.DYES_RED))        return 1;
		if (stack.getItem().is(Tags.Items.DYES_GREEN))      return 2;
		if (stack.getItem().is(Tags.Items.DYES_BROWN))      return 3;
		if (stack.getItem().is(Tags.Items.DYES_BLUE))       return 4;
		if (stack.getItem().is(Tags.Items.DYES_PURPLE))     return 5;
		if (stack.getItem().is(Tags.Items.DYES_CYAN))       return 6;
		if (stack.getItem().is(Tags.Items.DYES_LIGHT_GRAY)) return 7;
		if (stack.getItem().is(Tags.Items.DYES_GRAY))       return 8;
		if (stack.getItem().is(Tags.Items.DYES_PINK))       return 9;
		if (stack.getItem().is(Tags.Items.DYES_LIME))       return 10;
		if (stack.getItem().is(Tags.Items.DYES_YELLOW))     return 11;
		if (stack.getItem().is(Tags.Items.DYES_LIGHT_BLUE)) return 12;
		if (stack.getItem().is(Tags.Items.DYES_MAGENTA))    return 13;
		if (stack.getItem().is(Tags.Items.DYES_ORANGE))     return 14;
		if (stack.getItem().is(Tags.Items.DYES_WHITE))      return 15;
		return -1;
	}

	@Override
	public boolean canCraftInDimensions(final int width, final int height) {
		return width * height >= dyeCount + 2;
	}

	@Nonnull
	@Override
	public IRecipeSerializer<?> getSerializer() {
		switch (mode) {
		case VIDEO_CHANNEL:
			return Registration.TUNING_DRIVER_VIDEO_RECIPE.get();
		case BEAM_FREQUENCY:
			return Registration.TUNING_DRIVER_BEAM_RECIPE.get();
		case CONTROL_CHANNEL:
		default:
			return Registration.TUNING_DRIVER_CONTROL_RECIPE.get();
		}
	}
}
