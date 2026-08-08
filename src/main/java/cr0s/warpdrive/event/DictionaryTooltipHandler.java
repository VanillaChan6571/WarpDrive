package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shows the three player-facing item behaviours supplied by the former Dictionary. */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DictionaryTooltipHandler {

	private DictionaryTooltipHandler() {
	}

	@SubscribeEvent
	public static void onItemTooltip(final ItemTooltipEvent event) {
		final ItemStack itemStack = event.getItemStack();
		if (itemStack.getItem().is(WarpDriveTags.BREATHING_HELMETS)) {
			event.getToolTip().add(line("tooltip.warpdrive.behaviour.breathing_helmet"));
		}
		if (itemStack.getItem().is(WarpDriveTags.FLY_IN_SPACE)) {
			event.getToolTip().add(line("tooltip.warpdrive.behaviour.fly_in_space"));
		}
		if (itemStack.getItem().is(WarpDriveTags.NO_FALL_DAMAGE)) {
			event.getToolTip().add(line("tooltip.warpdrive.behaviour.no_fall_damage"));
		}
	}

	private static IFormattableTextComponent line(final String key) {
		return new TranslationTextComponent(key).withStyle(TextFormatting.DARK_GRAY);
	}
}
