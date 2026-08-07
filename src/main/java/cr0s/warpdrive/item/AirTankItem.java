package cr0s.warpdrive.item;

import cr0s.warpdrive.api.IAirContainerItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A bottled supply of breathable air, as in 1.12.2.
 *
 * Storage is expressed through the item's damage value rather than NBT, which is what the 1.12.2
 * version did and what the surviving model files already expect: air_tank.<tier>.json overrides on
 * the damage predicate at 0/20/40/60/80/100%, so the tank visibly empties as it is used. Damage 0
 * is full, damage == maxDamage is empty.
 *
 * The registry name deliberately keeps the dot - "air_tank.basic" - so those original model and
 * texture paths resolve without having to rewrite the asset pack.
 */
public class AirTankItem extends Item implements IAirContainerItem {

	/** Ticks of air per breath. 1.12.2 BREATHING_AIR_TANK_BREATH_DURATION_TICKS. */
	public static final int BREATH_DURATION_TICKS = 300;

	private final AirTankTier tier;

	public AirTankItem(final AirTankTier tier) {
		super(new Properties()
			.tab(WarpDriveItemGroup.MAIN)
			.stacksTo(1)
			.durability(tier.getCapacity()));
		this.tier = tier;
	}

	public AirTankTier getTier() {
		return tier;
	}

	@Override
	public boolean canContainAir(final ItemStack itemStack) {
		if (itemStack == null || itemStack.getItem() != this) {
			return false;
		}
		// Room to spare: anything short of full can still be topped up
		return itemStack.getDamageValue() > 0;
	}

	@Override
	public int getMaxAirStorage(final ItemStack itemStack) {
		if (itemStack == null || itemStack.getItem() != this) {
			return 0;
		}
		return itemStack.getMaxDamage();
	}

	@Override
	public int getCurrentAirStorage(final ItemStack itemStack) {
		if (itemStack == null || itemStack.getItem() != this) {
			return 0;
		}
		return itemStack.getMaxDamage() - itemStack.getDamageValue();
	}

	@Override
	public ItemStack consumeAir(final ItemStack itemStack) {
		if (itemStack == null || itemStack.getItem() != this) {
			return itemStack;
		}
		// Set damage directly rather than going through hurt(), both to bypass Unbreaking - as
		// 1.12.2 noted - and so an emptied tank stops at maximum damage instead of breaking
		itemStack.setDamageValue(Math.min(itemStack.getMaxDamage(), itemStack.getDamageValue() + 1));
		return itemStack;
	}

	@Override
	public int getAirTicksPerConsumption(final ItemStack itemStack) {
		if (itemStack != null && itemStack.getItem() != this) {
			return 0;
		}
		return BREATH_DURATION_TICKS;
	}

	@Override
	public ItemStack getEmptyAirContainer(final ItemStack itemStack) {
		if (itemStack != null && itemStack.getItem() != this) {
			return itemStack;
		}
		final ItemStack result = new ItemStack(this);
		result.setDamageValue(result.getMaxDamage());
		return result;
	}

	@Override
	public ItemStack getFullAirContainer(final ItemStack itemStack) {
		if (itemStack != null && itemStack.getItem() != this) {
			return itemStack;
		}
		return new ItemStack(this);
	}

	/** Offer both a full and an empty tank in creative, as 1.12.2 did through getSubItems. */
	@Override
	public void fillItemCategory(final ItemGroup group, final NonNullList<ItemStack> items) {
		if (!allowdedIn(group)) {
			return;
		}
		items.add(getFullAirContainer(null));
		items.add(getEmptyAirContainer(null));
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                            final List<ITextComponent> tooltip, final ITooltipFlag flag) {
		super.appendHoverText(itemStack, world, tooltip, flag);

		final int breaths = getCurrentAirStorage(itemStack);
		final int seconds = breaths * BREATH_DURATION_TICKS / 20;
		tooltip.add(new StringTextComponent(TextFormatting.AQUA + String.format(
			"%d / %d breaths - %d:%02d of air", breaths, getMaxAirStorage(itemStack),
			seconds / 60, seconds % 60)));
	}
}
