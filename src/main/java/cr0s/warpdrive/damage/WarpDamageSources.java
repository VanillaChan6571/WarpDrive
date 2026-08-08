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

	/** Absolute heat damage used by live accelerator chillers. */
	public static final DamageSource WARM = new DamageSource("warpdrive.warm")
		.bypassArmor()
		.bypassMagic();

	/** Radiation released by uncontained accelerator particles. */
	public static final DamageSource IRRADIATION = new DamageSource("warpdrive.irradiation")
		.bypassArmor()
		.setMagic();

	/** Matter-stream decoherence from a weak transporter lock. */
	public static final DamageSource TELEPORTATION = new DamageSource("warpdrive.teleportation")
		.bypassArmor()
		.setMagic();

	private WarpDamageSources() {
	}
}
