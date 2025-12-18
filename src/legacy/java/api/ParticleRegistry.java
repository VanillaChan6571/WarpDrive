package cr0s.warpdrive.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import net.minecraft.item.EnumRarity;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ParticleRegistry {
	
	private static BiMap<String, Particle> particles = HashBiMap.create();
	
	public static final Particle ION = new Particle("ion") { }.setColor(0xE5FF54).setRarity(EnumRarity.COMMON).setColorIndex(0)
	                                   .setEntityLifespan(200).setRadiationLevel(2.0F).setExplosionStrength(0.3F);
	public static final Particle PROTON = new Particle("proton") { }.setColor(0xE5FF54).setRarity(EnumRarity.COMMON).setColorIndex(1)
	                                      .setEntityLifespan(200).setRadiationLevel(4.0F).setExplosionStrength(0.5F);
	public static final Particle ANTIMATTER = new Particle("antimatter") { }.setColor(0x1C3CAF).setRarity(EnumRarity.UNCOMMON).setColorIndex(2)
	                                          .setEntityLifespan(60).setRadiationLevel(10.0F).setExplosionStrength(1.0F);
	public static final Particle STRANGE_MATTER = new Particle("strange_matter") { }.setColor(0xE2414C).setRarity(EnumRarity.RARE).setColorIndex(3)
	                                              .setEntityLifespan(40).setRadiationLevel(14.0F).setExplosionStrength(0.8F);
	// public static final Particle TACHYONS = new Particle("tachyons") { }.setColor(0xE5FF54).setRarity(EnumRarity.EPIC).setColorIndex(4);
	
	static {
		registerParticle(ION);
		registerParticle(PROTON);
		registerParticle(ANTIMATTER);
		registerParticle(STRANGE_MATTER);
		// registerParticle(TACHYONS);
	}
	
	private ParticleRegistry() {
		
	}
	
	public static boolean registerParticle(final Particle particle) {
		if (particles.containsKey(particle.getRegistryName())) {
			FMLLog.getLogger().error(String.format("Mod %s FAILED to register particle %s: it was already registered!", getActiveModId(), particle.getRegistryName()));
			return false;
		}
		particles.put(particle.getRegistryName(), particle);
		
		FMLLog.getLogger().info(String.format("Mod %s has registered particle %s", getActiveModId(), particle.getRegistryName()));
		MinecraftForge.EVENT_BUS.post(new ParticleRegisterEvent(particle.getRegistryName()));
		return true;
	}
	
	private static String getActiveModId() {
		final ModContainer activeModContainer = Loader.instance().activeModContainer();
		return activeModContainer == null ? "minecraft" : activeModContainer.getModId();
	}
	
	public static boolean isParticleRegistered(final Particle particle) {
		return particle != null && particles.containsKey(particle.getRegistryName());
	}
	
	public static boolean isParticleRegistered(final String particleRegistryName) {
		return particles.containsKey(particleRegistryName);
	}
	
	public static Particle getParticle(final String particleRegistryName) {
		return particles.get(particleRegistryName);
	}
	
	public static String getParticleName(final Particle particle) {
		return particle.getRegistryName();
	}
	
	public static String getParticleName(final ParticleStack stack) {
		return stack.getParticle().getRegistryName();
	}
	
	public static ParticleStack getParticleStack(final String particleRegistryName, final int amount) {
		if (!particles.containsKey(particleRegistryName)) {
			return null;
		}
		return new ParticleStack(getParticle(particleRegistryName), amount);
	}
	
	public static Map<String, Particle> getRegisteredParticles()
	{
		return ImmutableMap.copyOf(particles);
	}
		
	public static class ParticleRegisterEvent extends Event {
		public final String particleRegistryName;
		
		public ParticleRegisterEvent(final String particleRegistryName) {
			this.particleRegistryName = particleRegistryName;
		}
	}
}
