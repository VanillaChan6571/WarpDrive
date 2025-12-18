package cr0s.warpdrive.damage;

import net.minecraft.util.DamageSource;

public class DamageLaser extends DamageSource {

	public DamageLaser() {
		super("warpdrive.laser");
		
		setDamageBypassesArmor();
		setDamageIsAbsolute();
	}
	
}
