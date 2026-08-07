package cr0s.warpdrive.item;

import cr0s.warpdrive.data.Registration;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

/**
 * Armour tiers, carried over unchanged from 1.12.2 WarpDrive.java:
 *
 *   BASIC    "rubber"       durability x6,  protection {1,2,3,1}, enchantability 12, toughness 0.0
 *   ADVANCED "ceramic"      durability x18, protection {2,6,5,2}, enchantability  9, toughness 1.0
 *   SUPERIOR "carbon_fiber" durability x40, protection {3,6,8,3}, enchantability 10, toughness 2.5
 *
 * The protection arrays are ordered feet, legs, chest, head - the same order vanilla uses - so the
 * superior helmet ends up at 40 x 11 = 440 durability, +3 armour and 2.5 toughness.
 */
public enum WarpArmorMaterial implements IArmorMaterial {

	BASIC("basic", 6, new int[]{ 1, 2, 3, 1 }, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, "rubber"),
	ADVANCED("advanced", 18, new int[]{ 2, 6, 5, 2 }, 9, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, "ceramic"),
	SUPERIOR("superior", 40, new int[]{ 3, 6, 8, 3 }, 10, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5F, "carbon_fiber");

	/** Vanilla's per-slot durability multipliers: feet, legs, chest, head. */
	private static final int[] HEALTH_PER_SLOT = { 13, 15, 16, 11 };

	private final String name;
	private final int durabilityFactor;
	private final int[] protection;
	private final int enchantmentValue;
	private final SoundEvent equipSound;
	private final float toughness;
	private final String repairComponent;

	WarpArmorMaterial(final String name, final int durabilityFactor, final int[] protection,
	                  final int enchantmentValue, final SoundEvent equipSound, final float toughness,
	                  final String repairComponent) {
		this.name = name;
		this.durabilityFactor = durabilityFactor;
		this.protection = protection;
		this.enchantmentValue = enchantmentValue;
		this.equipSound = equipSound;
		this.toughness = toughness;
		this.repairComponent = repairComponent;
	}

	@Override
	public int getDurabilityForSlot(final EquipmentSlotType slot) {
		return HEALTH_PER_SLOT[slot.getIndex()] * durabilityFactor;
	}

	@Override
	public int getDefenseForSlot(final EquipmentSlotType slot) {
		return protection[slot.getIndex()];
	}

	@Override
	public int getEnchantmentValue() {
		return enchantmentValue;
	}

	@Override
	public SoundEvent getEquipSound() {
		return equipSound;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.of(Registration.COMPONENTS.get(repairComponent).get());
	}

	@Override
	public String getName() {
		return "warpdrive:" + name;
	}

	@Override
	public float getToughness() {
		return toughness;
	}

	@Override
	public float getKnockbackResistance() {
		return 0.0F;
	}
}
