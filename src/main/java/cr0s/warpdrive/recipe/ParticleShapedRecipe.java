package cr0s.warpdrive.recipe;

import com.google.gson.JsonObject;
import cr0s.warpdrive.data.ParticleType;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.item.ElectromagneticCellItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

/** Shaped recipe that verifies and consumes particle quantity while returning the cell. */
public class ParticleShapedRecipe extends ShapedRecipe {

	public ParticleShapedRecipe(final ResourceLocation id, final String group,
	                            final int width, final int height,
	                            final NonNullList<Ingredient> ingredients,
	                            final ItemStack result) {
		super(id, group, width, height, ingredients, result);
	}

	@Override
	public boolean matches(final CraftingInventory inventory, final World world) {
		if (!super.matches(inventory, world)) return false;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			final ItemStack itemStack = inventory.getItem(slot);
			if (!(itemStack.getItem() instanceof ElectromagneticCellItem)) continue;
			final ElectromagneticCellItem cell = (ElectromagneticCellItem) itemStack.getItem();
			if (cell.getParticleType() != null
			 && cell.getAmount(itemStack) < getRequiredAmount(cell.getParticleType())) return false;
		}
		return true;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(final CraftingInventory inventory) {
		final NonNullList<ItemStack> remaining = NonNullList.withSize(
			inventory.getContainerSize(), ItemStack.EMPTY);
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			final ItemStack itemStack = inventory.getItem(slot);
			if (itemStack.getItem() instanceof ElectromagneticCellItem) {
				final ElectromagneticCellItem cell = (ElectromagneticCellItem) itemStack.getItem();
				if (cell.getParticleType() != null) {
					final int left = cell.getAmount(itemStack) - getRequiredAmount(cell.getParticleType());
					remaining.set(slot, left > 0
						? ElectromagneticCellItem.create(cell.getTier(), cell.getParticleType(), left)
						: ElectromagneticCellItem.create(cell.getTier(), null, 0));
					continue;
				}
			}
			if (itemStack.hasContainerItem()) remaining.set(slot, itemStack.getContainerItem());
		}
		return remaining;
	}

	public static int getRequiredAmount(final ParticleType particleType) {
		switch (particleType) {
		case ION: return 200;
		case PROTON: return 24;
		case ANTIMATTER:
		case STRANGE_MATTER: return 1_000;
		default: return Integer.MAX_VALUE;
		}
	}

	@Override public IRecipeSerializer<?> getSerializer() {
		return Registration.PARTICLE_SHAPED_RECIPE.get();
	}

	public static final class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
		implements IRecipeSerializer<ParticleShapedRecipe> {

		private final ShapedRecipe.Serializer vanilla = new ShapedRecipe.Serializer();

		@Override
		public ParticleShapedRecipe fromJson(final ResourceLocation id, final JsonObject json) {
			return convert(vanilla.fromJson(id, json));
		}

		@Nullable
		@Override
		public ParticleShapedRecipe fromNetwork(final ResourceLocation id,
		                                        final PacketBuffer buffer) {
			final ShapedRecipe recipe = vanilla.fromNetwork(id, buffer);
			return recipe == null ? null : convert(recipe);
		}

		@Override
		public void toNetwork(final PacketBuffer buffer, final ParticleShapedRecipe recipe) {
			vanilla.toNetwork(buffer, recipe);
		}

		private static ParticleShapedRecipe convert(final ShapedRecipe recipe) {
			return new ParticleShapedRecipe(recipe.getId(), recipe.getGroup(),
				recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem());
		}
	}
}
