package cr0s.warpdrive.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

/**
 * A readable and writable reference to one slot of a player's carried items.
 *
 * The breathing code has to do more than look at air tanks - it drains them, refills them and
 * replaces empty canisters - so enumerating {@link ItemStack} copies is not enough. Wrapping the
 * slot instead lets the same logic work over the main inventory and over any item handler, which is
 * what makes optional Curios support a matter of supplying more slots rather than duplicating the
 * search.
 */
public interface ItemSlotRef {

	@Nonnull
	ItemStack get();

	void set(@Nonnull ItemStack itemStack);

	/** Slot backed by a plain list, as used by the vanilla player inventory. */
	static ItemSlotRef ofList(final NonNullList<ItemStack> list, final int index) {
		return new ItemSlotRef() {
			@Nonnull
			@Override
			public ItemStack get() {
				return list.get(index);
			}

			@Override
			public void set(@Nonnull final ItemStack itemStack) {
				list.set(index, itemStack);
			}
		};
	}

	/** Slot backed by a Forge item handler, as used by Curios and most modded containers. */
	static ItemSlotRef ofHandler(final IItemHandlerModifiable handler, final int index) {
		return new ItemSlotRef() {
			@Nonnull
			@Override
			public ItemStack get() {
				return handler.getStackInSlot(index);
			}

			@Override
			public void set(@Nonnull final ItemStack itemStack) {
				handler.setStackInSlot(index, itemStack);
			}
		};
	}

	/** The held-item slots, so a tank in hand counts like one in the pack. */
	static ItemSlotRef ofHand(final PlayerEntity player, final net.minecraft.util.Hand hand) {
		return new ItemSlotRef() {
			@Nonnull
			@Override
			public ItemStack get() {
				return player.getItemInHand(hand);
			}

			@Override
			public void set(@Nonnull final ItemStack itemStack) {
				player.setItemInHand(hand, itemStack);
			}
		};
	}
}
