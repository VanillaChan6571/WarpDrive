package cr0s.warpdrive.damage;

import net.minecraft.util.DamageSource;

public class DamageCold extends DamageSource {

	public DamageCold() {
		super("warpdrive.cold");
		
		setDamageBypassesArmor();
		setDamageIsAbsolute();
	}
	
}
