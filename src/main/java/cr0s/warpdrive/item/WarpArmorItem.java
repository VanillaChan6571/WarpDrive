package cr0s.warpdrive.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * WarpDrive armour. A helmet of any tier supplies breathable air, so it is what keeps you alive in
 * vacuum - see VacuumHandler.
 */
public class WarpArmorItem extends ArmorItem {

	private final WarpArmorMaterial warpMaterial;

	public WarpArmorItem(final WarpArmorMaterial material, final EquipmentSlotType slot) {
		super(material, slot, new Properties().tab(ItemGroup.TAB_COMBAT));
		this.warpMaterial = material;
	}

	public WarpArmorMaterial getWarpMaterial() {
		return warpMaterial;
	}

	/**
	 * All tiers share one texture pair, as in 1.12.2: layer 2 is the leggings, layer 1 everything
	 * else. This is the Forge extension point on Item, not a vanilla override.
	 */
	@Override
	public String getArmorTexture(final ItemStack stack, final Entity entity,
	                              final EquipmentSlotType slot, final String type) {
		return "warpdrive:textures/armor/warp_armor_" + (slot == EquipmentSlotType.LEGS ? 2 : 1) + ".png";
	}

	/** True when this entity is wearing a WarpDrive helmet, i.e. has an air supply. */
	public static boolean hasBreathingHelmet(@Nullable final LivingEntity entity) {
		if (entity == null) {
			return false;
		}
		final ItemStack helmet = entity.getItemBySlot(EquipmentSlotType.HEAD);
		return !helmet.isEmpty() && helmet.getItem() instanceof WarpArmorItem;
	}
}
