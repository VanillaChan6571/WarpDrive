package cr0s.warpdrive.item;

import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** One flattened 1.16 item for each former ship-token metadata value. */
public class ShipTokenItem extends Item {

	private static final String SHIP_NAME_TAG = "shipName";
	private final int schematicId;

	public ShipTokenItem(final int schematicId) {
		super(new Properties().tab(WarpDriveItemGroup.MAIN));
		this.schematicId = schematicId;
	}

	public String getSchematicName(final ItemStack itemStack) {
		final CompoundNBT tag = itemStack.getTag();
		return tag != null && tag.contains(SHIP_NAME_TAG)
		     ? tag.getString(SHIP_NAME_TAG)
		     : Integer.toString(schematicId);
	}

	public static void setSchematicName(final ItemStack itemStack, final String schematicName) {
		itemStack.getOrCreateTag().putString(SHIP_NAME_TAG, schematicName == null ? "" : schematicName);
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                            final List<ITextComponent> tooltip, final ITooltipFlag flag) {
		super.appendHoverText(itemStack, world, tooltip, flag);
		tooltip.add(new TranslationTextComponent("item.warpdrive.ship_token.tooltip",
			getSchematicName(itemStack)));
	}
}
