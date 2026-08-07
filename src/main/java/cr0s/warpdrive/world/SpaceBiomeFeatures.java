package cr0s.warpdrive.world;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.Registration;
import net.minecraft.world.gen.GenerationStage;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Attaches the asteroid field to the space biome.
 *
 * Done through BiomeLoadingEvent rather than listing the feature in the biome JSON: a configured
 * feature registered from code is not present in WorldGenRegistries early enough for a datapack
 * biome to reference it by id, and the resulting failure is a silent missing feature.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpaceBiomeFeatures {

	private SpaceBiomeFeatures() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onBiomeLoading(final BiomeLoadingEvent event) {
		if (event.getName() == null
		 || !WarpDrive.MODID.equals(event.getName().getNamespace())
		 || !"space".equals(event.getName().getPath())) {
			return;
		}
		if (Registration.ASTEROID_FIELD_CONFIGURED == null) {
			WarpDrive.logger.warn("Asteroid feature not configured yet; space biome will be empty");
			return;
		}
		event.getGeneration()
			.getFeatures(GenerationStage.Decoration.UNDERGROUND_ORES)
			.add(() -> Registration.ASTEROID_FIELD_CONFIGURED);

		if (Registration.CELESTIAL_BODIES_CONFIGURED != null) {
			// Native feature generation is preferable to post-generation tick queues here: every
			// chunk builds only its deterministic slice of a planet/moon system.
			event.getGeneration()
				.getFeatures(GenerationStage.Decoration.SURFACE_STRUCTURES)
				.add(() -> Registration.CELESTIAL_BODIES_CONFIGURED);
		}

		if (Registration.GIANT_ASTEROID_CONFIGURED != null) {
			// Placed in an earlier stage so the scattered fields can settle around it rather than
			// being overwritten by it
			event.getGeneration()
				.getFeatures(GenerationStage.Decoration.SURFACE_STRUCTURES)
				.add(() -> Registration.GIANT_ASTEROID_CONFIGURED);
		}
		WarpDrive.logger.info("Space generation features attached to {}", event.getName());
	}
}
