package cr0s.warpdrive.item;

import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** A flattened tuning-fork colour with the original three deterministic channel values. */
public class TuningForkItem extends Item {

	private final int legacyDyeDamage;

	public TuningForkItem(final DyeColor color) {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
		// 1.12 item dye damage was the reverse of the modern block-colour id: black=0, white=15.
		this.legacyDyeDamage = 15 - color.getId();
	}

	public int getVideoChannel() {
		return legacyDyeDamage + 100;
	}

	public int getBeamFrequency() {
		return (legacyDyeDamage + 1) * 10;
	}

	public int getControlChannel() {
		return legacyDyeDamage + 2;
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                            final List<ITextComponent> tooltip, final ITooltipFlag flag) {
		super.appendHoverText(itemStack, world, tooltip, flag);
		tooltip.add(new TranslationTextComponent("item.warpdrive.tuning_fork.tooltip",
			getVideoChannel(), getBeamFrequency(), getControlChannel()));
	}
}
