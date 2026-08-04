package cr0s.warpdrive.damage;

import net.minecraft.util.DamageSource;

/**
 * WarpDrive's own damage sources.
 */
public final class WarpDamageSources {

	/**
	 * Suffocation in vacuum. Bypasses armour deliberately - a sealed suit is the defence, and
	 * plating should not soften it. Matches 1.12.2's damageAsphyxia.
	 */
	public static final DamageSource ASPHYXIA = new DamageSource("warpdrive.asphyxia")
		.bypassArmor()
		.setMagic();

	private WarpDamageSources() {
	}
}
