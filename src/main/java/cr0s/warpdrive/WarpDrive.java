package cr0s.warpdrive;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WarpDrive Reimagined for Minecraft 1.16.5
 *
 * A clean, modern reimplementation of the WarpDrive mod focusing on core features:
 * - Ship structure scanning and warping
 * - Cross-dimension teleportation
 * - Space dimension with asteroids
 * - ComputerCraft integration for ship control
 * - Energy-based warp system
 */
@Mod(WarpDrive.MODID)
public class WarpDrive {

	public static final String MODID = "warpdrive";
	public static final String VERSION = "@version@";
	public static final Logger logger = LogManager.getLogger(MODID);

	public WarpDrive() {
		// Open the dedicated debug log first so registration problems are captured too
		cr0s.warpdrive.debug.DebugLog.init();

		// Initialize deferred registration system (1.16.5 pattern)
		cr0s.warpdrive.data.Registration.init();

		// Client-only settings; harmlessly ignored on a dedicated server, which never loads them
		net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
			net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
			cr0s.warpdrive.config.ClientConfig.SPEC, "warpdrive-client.toml");

		// Register event handlers
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

		logger.info("WarpDrive Reimagined initialized!");
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		cr0s.warpdrive.network.WarpDriveNetwork.init();
		// Must be deferred to setup: WorldGenRegistries is not safe to touch during construction
		event.enqueueWork(cr0s.warpdrive.data.Registration::registerChunkGenerators);
		event.enqueueWork(cr0s.warpdrive.data.Registration::registerConfiguredFeatures);
		logger.info("WarpDrive common setup complete");
	}

	private void clientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ScreenManager.register(
				cr0s.warpdrive.data.Registration.CREATIVE_ENERGY_CONTAINER.get(),
				cr0s.warpdrive.client.CreativeEnergyScreen::new);
			cr0s.warpdrive.client.ClientDimensionRendering.register();

			// Air blocks are translucent blue. Without this they render on the solid layer and the
			// texture's alpha is ignored, so the volume would appear as opaque cubes.
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.AIR_FLOW_BLOCK.get(),
				net.minecraft.client.renderer.RenderType.translucent());
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.AIR_SOURCE_BLOCK.get(),
				net.minecraft.client.renderer.RenderType.translucent());
		});
		logger.info("WarpDrive client setup complete");
	}
}
