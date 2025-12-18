package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public class SoundEvents {
	
	public static final SoundEvent LASER_LOW;
	public static final SoundEvent LASER_MEDIUM;
	public static final SoundEvent LASER_HIGH;
	public static final SoundEvent CLOAK;
	public static final SoundEvent DECLOAK;
	public static final SoundEvent PROJECTING;
	public static final SoundEvent WARP_4_SECONDS;
	public static final SoundEvent WARP_10_SECONDS;
	public static final SoundEvent WARP_30_SECONDS;
    public static final SoundEvent DING;
	public static final SoundEvent SIREN_RAID;
	public static final SoundEvent SIREN_INDUSTRIAL;
	public static final SoundEvent ACCELERATING_LOW;
	public static final SoundEvent ACCELERATING_MEDIUM;
	public static final SoundEvent ACCELERATING_HIGH;
	public static final SoundEvent CHILLER;
	public static final SoundEvent COLLISION_LOW;
	public static final SoundEvent COLLISION_MEDIUM;
	public static final SoundEvent COLLISION_HIGH;
	
	static {
		LASER_LOW = registerSound("lowlaser");
		LASER_MEDIUM = registerSound("midlaser");
		LASER_HIGH = registerSound("hilaser");
		CLOAK = registerSound("cloak");
		DECLOAK = registerSound("decloak");
		PROJECTING = registerSound("projecting");
		WARP_4_SECONDS = registerSound("warp_4s");
		WARP_10_SECONDS = registerSound("warp_10s");
		WARP_30_SECONDS = registerSound("warp_30s");
		DING = registerSound("ding");
	    SIREN_RAID = registerSound("siren_raid");
	    SIREN_INDUSTRIAL = registerSound("siren_industrial");
		ACCELERATING_LOW = registerSound("accelerating_low");
		ACCELERATING_MEDIUM = registerSound("accelerating_medium");
		ACCELERATING_HIGH = registerSound("accelerating_high");
		CHILLER = registerSound("chiller");
		COLLISION_LOW = registerSound("collision_low");
		COLLISION_MEDIUM = registerSound("collision_medium");
		COLLISION_HIGH = registerSound("collision_high");
	}
	
	@SuppressWarnings("EmptyMethod")
	public static void registerSounds() {
		// Dummy method to make sure the static initializer runs
	}
	
	private static SoundEvent registerSound(final String soundName) {
		final ResourceLocation soundID = new ResourceLocation(WarpDrive.MODID, soundName);
		final SoundEvent soundEvent = new SoundEvent(soundID);
		soundEvent.setRegistryName(soundID);
		WarpDrive.register(soundEvent);
		return soundEvent;
	}
}
