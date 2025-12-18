package cr0s.warpdrive.api;

import net.minecraft.entity.EntityLivingBase;

public interface IBreathingHelmet {
	
	// Called when checking armors, before checking for air containers
	boolean canBreath(EntityLivingBase entityLivingBase);
} 