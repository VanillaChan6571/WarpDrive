package cr0s.warpdrive.api;

import net.minecraft.entity.LivingEntity;

/**
 * A helmet that can supply air, checked before the wearer's air containers are touched.
 *
 * Ported from 1.12.2, with EntityLivingBase updated to LivingEntity.
 */
public interface IBreathingHelmet {

	/** Called while checking armour, before looking for air containers. */
	boolean canBreath(LivingEntity entityLiving);
}
