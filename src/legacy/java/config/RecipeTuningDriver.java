package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.item.ItemTuningDriver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

// Used to change tuning driver values
public class RecipeTuningDriver implements IRecipe {
	
	private ResourceLocation resourceLocation;
	
	private final ItemStack itemStackTool;
	private final ItemStack itemStackConsumable;
	private final int countDyesExpected;
	private ItemStack itemStackResult = ItemStack.EMPTY;
	private final int size;
	private final ResourceLocation group;
	
	public RecipeTuningDriver(@Nonnull final ResourceLocation group, @Nonnull final ItemStack itemStackTool,
	                          @Nonnull final ItemStack itemStackConsumable, final int countDyesExpected, @Nonnull final String suffix) {
		this.group = group;
		this.itemStackTool = itemStackTool.copy();
		this.itemStackConsumable = itemStackConsumable.copy();
		this.countDyesExpected = countDyesExpected;
		this.size = 1 + (itemStackConsumable.isEmpty() ? 0 : 1) + countDyesExpected;
		
		// add lower priority vanilla Shaped recipe for NEI support
		final Object[] recipe = new Object[size];
		recipe[0] = itemStackTool;
		recipe[1] = itemStackConsumable;
		for (int index = 0; index < countDyesExpected; index++) {
			recipe[2 + index] = "dye";
		}
		WarpDrive.register(new ShapelessOreRecipe(group, itemStackTool, recipe), suffix);
	}
	
	@Override
	public IRecipe setRegistryName(final ResourceLocation resourceLocation) {
		this.resourceLocation = resourceLocation;
		return this;
	}
	
	@Nullable
	@Override
	public ResourceLocation getRegistryName() {
		return resourceLocation;
	}
	
	@Override
	public Class<IRecipe> getRegistryType() {
		return IRecipe.class;
	}
	
	@Override
	@Nonnull
	public String getGroup() {
		return group.toString();
	}
	
	@Override
	public boolean canFit(final int width, final int height) {
		return width * height >= size;
	}
	
	// Returns an Item that is the result of this recipe
	@Nonnull
	@Override
	public ItemStack getCraftingResult(@Nonnull final InventoryCrafting inventoryCrafting) {
		return itemStackResult.copy();
	}
	
	@Nonnull
	@Override
	public ItemStack getRecipeOutput() {
		return itemStackResult;
	}
	
	// check if a recipe matches current crafting inventory
	@Override
	public boolean matches(@Nonnull final InventoryCrafting inventoryCrafting, @Nonnull final World world) {
		ItemStack itemStackInput = null;
		boolean isConsumableFound = false;
		int dye = 0;
		int countDyesFound = 0;
		for (int indexSlot = 0; indexSlot < inventoryCrafting.getSizeInventory(); indexSlot++) {
			final ItemStack itemStackSlot = inventoryCrafting.getStackInSlot(indexSlot);
			
			//noinspection StatementWithEmptyBody
			if (itemStackSlot.isEmpty()) {
				// continue
			} else if (OreDictionary.itemMatches(itemStackSlot, itemStackTool, false)) {
				// too many inputs?
				if (itemStackInput != null) {
					return false;
				}
				itemStackInput = itemStackSlot;
				
			} else if (OreDictionary.itemMatches(itemStackSlot, itemStackConsumable, true)) {
				// too many consumables?
				if (isConsumableFound) {
					return false;
				}
				isConsumableFound = true;
				
			} else {
				// find a matching dye from ore dictionary
				boolean matched = false;
				for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
					final List<ItemStack> itemStackDyes = OreDictionary.getOres(Recipes.oreDyes.get(enumDyeColor));
					for (final ItemStack itemStackDye : itemStackDyes) {
						if (OreDictionary.itemMatches(itemStackSlot, itemStackDye, true)) {
							// match found, update dye combination
							matched = true;
							countDyesFound++;
							dye = dye * 16 + enumDyeColor.getDyeDamage();
						}
					}
				}
				if (!matched) {
					return false;
				}
			}
		}
		
		// missing input
		if (itemStackInput == null) {
			return false;
		}
		
		// missing or too many dyes
		if (countDyesFound != countDyesExpected) {
			return false;
		}
		
		// consumable missing or not required
		if ( (itemStackConsumable != null && !isConsumableFound)
		  || (itemStackConsumable == null &&  isConsumableFound) ) {
			return false;
		}
		
		// build result
		itemStackResult = ItemTuningDriver.setValue(itemStackInput.copy(), dye);
		
		return true;
	}
}
