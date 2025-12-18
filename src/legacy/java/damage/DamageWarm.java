package cr0s.warpdrive.damage;

import net.minecraft.util.DamageSource;

public class DamageWarm extends DamageSource {

	public DamageWarm() {
		super("warpdrive.warm");
		
		setDamageBypassesArmor();
		setDamageIsAbsolute();
	}
	
}
