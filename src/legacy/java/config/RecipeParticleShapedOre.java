package cr0s.warpdrive.config;

import cr0s.warpdrive.api.IParticleContainerItem;
import cr0s.warpdrive.api.ParticleStack;

import javax.annotation.Nonnull;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.CraftingHelper.ShapedPrimer;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

// Adds support for IParticleContainerItem ingredients
// Adds support for potions ingredients
// Loosely inspired from vanilla ShapedOreRecipe
public class RecipeParticleShapedOre extends ShapedOreRecipe {
	
	public RecipeParticleShapedOre(final ResourceLocation group, final Block result, final Object... recipe){ this(group, new ItemStack(result), CraftingHelper.parseShaped(recipe)); }
	public RecipeParticleShapedOre(final ResourceLocation group, final Item result, final Object... recipe){ this(group, new ItemStack(result), CraftingHelper.parseShaped(recipe)); }
	public RecipeParticleShapedOre(final ResourceLocation group, @Nonnull final ItemStack result, final Object... recipe) { this(group, result, CraftingHelper.parseShaped(recipe)); }
	public RecipeParticleShapedOre(final ResourceLocation group, @Nonnull final ItemStack result, final ShapedPrimer primer) {
		super(group, result, primer);
		
		// add lower priority vanilla Shaped recipe for NEI support
		// WarpDrive.register(new ShapedOreRecipe(group, result, primer));
	}
	
	@Override
	protected boolean checkMatch(@Nonnull final InventoryCrafting inventoryCrafting, final int startX, final int startY, final boolean mirror) {
		for (int x = 0; x < inventoryCrafting.getWidth(); x++) {
			for (int y = 0; y < inventoryCrafting.getHeight(); y++) {
				final int subX = x - startX;
				final int subY = y - startY;
				Ingredient target = Ingredient.EMPTY;
				
				if (subX >= 0 && subY >= 0 && subX < width && subY < height) {
					if (mirror) {
						target = input.get(width - subX - 1 + subY * width);
					} else {
						target = input.get(subX + subY * width);
					}
				}
				
				final ItemStack itemStackSlot = inventoryCrafting.getStackInRowAndColumn(x, y);
				final ItemStack[] itemStackTargets = target.getMatchingStacks();
				if (itemStackTargets.length == 1) {// simple ingredient
					final ItemStack itemStackTarget = itemStackTargets[0];
					if ( !itemStackSlot.isEmpty()
					  && itemStackSlot.hasTagCompound() ) {
						if ( itemStackSlot.getItem() instanceof IParticleContainerItem
						  && itemStackTarget.getItem() instanceof IParticleContainerItem ) {
							final IParticleContainerItem particleContainerItemSlot = (IParticleContainerItem) itemStackSlot.getItem();
							final ParticleStack particleStackSlot = particleContainerItemSlot.getParticleStack(itemStackSlot);
							
							final IParticleContainerItem particleContainerItemTarget = (IParticleContainerItem) itemStackTarget.getItem();
							final ParticleStack particleStackTarget = particleContainerItemTarget.getParticleStack(itemStackTarget);
							
							// reject different particles or insufficient quantity
							if (!particleStackSlot.containsParticle(particleStackTarget)) {
								particleContainerItemSlot.setAmountToConsume(itemStackSlot, 0);
								return false;
							}
							
							// it's a match! => mark quantity to consume
							particleContainerItemSlot.setAmountToConsume(itemStackSlot, particleStackTarget.getAmount());
							continue;
							
						} else if ( itemStackSlot.getItem() instanceof ItemPotion
						         && itemStackTarget.getItem() instanceof ItemPotion ) {
							final List<PotionEffect> potionEffectsSlot = PotionUtils.getEffectsFromStack(itemStackSlot);
							final List<PotionEffect> potionEffectsTarget = PotionUtils.getEffectsFromStack(itemStackTarget);
							
							// reject different amount of potion effects 
							if (potionEffectsSlot.size() != potionEffectsTarget.size()) {
								return false;
							}
							
							// verify matching effects
							for (final PotionEffect potionEffectTarget : potionEffectsTarget) {
								if (!potionEffectsSlot.contains(potionEffectTarget)) {
									return false;
								}
							}
							
							// it's a match!
							continue;
						}
						// (single item stack but neither particle nor potion => default to ore dictionary matching)
					}
					
					if (!OreDictionary.itemMatches(itemStackTarget, itemStackSlot, false)) {
						return false;
					}
					
				} else if (!target.apply(itemStackSlot)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
}
