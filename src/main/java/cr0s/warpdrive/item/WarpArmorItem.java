package cr0s.warpdrive.item;

import cr0s.warpdrive.api.IBreathingHelmet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * WarpDrive armour. A helmet of any tier is a sealed breathing helmet - see BreathingManager, which
 * additionally requires the full four-piece set and an air tank to draw from.
 */
public class WarpArmorItem extends ArmorItem implements IBreathingHelmet {

	private final WarpArmorMaterial warpMaterial;

	public WarpArmorItem(final WarpArmorMaterial material, final EquipmentSlotType slot) {
		super(material, slot, new Properties().tab(WarpDriveItemGroup.MAIN));
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

	/**
	 * Every tier seals, so any WarpDrive helmet can supply air. What it cannot do on its own is
	 * keep you alive: BreathingManager also requires the rest of the suit and a tank to draw from.
	 */
	@Override
	public boolean canBreath(@Nullable final LivingEntity entityLiving) {
		return entityLiving != null;
	}
}
