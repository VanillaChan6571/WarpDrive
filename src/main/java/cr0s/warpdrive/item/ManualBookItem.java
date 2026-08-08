package cr0s.warpdrive.item;

import cr0s.warpdrive.compat.PatchouliCompat;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** WarpDrive-owned compatibility item that opens the optional Patchouli manual. */
public class ManualBookItem extends Item {

	public ManualBookItem() {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> use(@Nonnull final World world, @Nonnull final PlayerEntity player,
	                                   @Nonnull final Hand hand) {
		final ItemStack itemStack = player.getItemInHand(hand);
		if (!world.isClientSide) {
			final boolean opened = player instanceof ServerPlayerEntity
				&& PatchouliCompat.openManual((ServerPlayerEntity) player);
			if (!opened) {
				player.displayClientMessage(new TranslationTextComponent(
					"item.warpdrive.book.patchouli_unavailable"), true);
			}
			return opened ? ActionResult.success(itemStack) : ActionResult.fail(itemStack);
		}
		return ActionResult.success(itemStack);
	}

	@Override
	public void appendHoverText(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                            @Nonnull final List<ITextComponent> tooltip,
	                            @Nonnull final ITooltipFlag flag) {
		final String key = PatchouliCompat.isLoaded()
			? "item.warpdrive.book.tooltip.open"
			: "item.warpdrive.book.tooltip.requires_patchouli";
		tooltip.add(new TranslationTextComponent(key).withStyle(TextFormatting.GRAY));
	}
}
