package cr0s.warpdrive;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	/** Kept for binary/source compatibility; runtime code should use {@link #getVersion()}. */
	public static final String VERSION = "@version@";
	public static final Logger logger = LogManager.getLogger(MODID);
	private static final Pattern MINECRAFT_VERSION_PREFIX =
		Pattern.compile("^\\d+\\.\\d+(?:\\.\\d+)?-");
	private static final Pattern SEMANTIC_VERSION =
		Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

	/** Actual expanded Forge mod metadata version, rather than the old unreplaced Java token. */
	public static String getVersion() {
		try {
			return net.minecraftforge.fml.ModList.get().getModContainerById(MODID)
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse(VERSION);
		} catch (final RuntimeException exception) {
			// Unit tests and very early bootstrap can run before ModList is ready.
			return VERSION;
		}
	}

	/** Legacy computer API shape: semantic WarpDrive major, minor and patch numbers. */
	public static Integer[] getVersionNumbers() {
		return parseVersionNumbers(getVersion());
	}

	static Integer[] parseVersionNumbers(final String rawVersion) {
		final String version = MINECRAFT_VERSION_PREFIX.matcher(
			rawVersion == null ? "" : rawVersion).replaceFirst("");
		final Matcher matcher = SEMANTIC_VERSION.matcher(version);
		if (!matcher.find()) {
			return new Integer[]{ 0, 0, 0 };
		}
		return new Integer[]{
			Integer.parseInt(matcher.group(1)),
			Integer.parseInt(matcher.group(2)),
			Integer.parseInt(matcher.group(3)) };
	}

	public WarpDrive() {
		// Open the dedicated debug log first so registration problems are captured too
		cr0s.warpdrive.debug.DebugLog.init();

		// Initialize deferred registration system (1.16.5 pattern)
		cr0s.warpdrive.data.Registration.init();

		// Keep computer automation optional. The native Ship Controller works without CC:Tweaked,
		// and reflection prevents the JVM from resolving any CC-linked compatibility class when the
		// optional mod is absent.
		if (net.minecraftforge.fml.ModList.get().isLoaded("computercraft")) {
			try {
				Class.forName("cr0s.warpdrive.compat.ComputerCraftCompat")
					.getMethod("register").invoke(null);
			} catch (final ReflectiveOperationException exception) {
				throw new IllegalStateException("Unable to initialise CC:Tweaked compatibility", exception);
			}
		}

		// Client-only settings; harmlessly ignored on a dedicated server, which never loads them
		net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
			net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
			cr0s.warpdrive.config.ClientConfig.SPEC, "warpdrive-client.toml");

		// Register event handlers
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
			cr0s.warpdrive.event.VirtualAssistantChatHandler::onServerChat);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
			cr0s.warpdrive.event.ParticleCellEventHandler::onItemExpire);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueInterModComms);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

		logger.info("WarpDrive Reimagined initialized!");
	}

	/** Curios needs its slot request during inter-mod enqueue; a no-op when Curios is absent. */
	private void enqueueInterModComms(final net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent event) {
		cr0s.warpdrive.compat.CuriosCompat.registerSlots();
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		cr0s.warpdrive.network.WarpDriveNetwork.init();
		cr0s.warpdrive.event.CloakEventHandler.register();
		// Must be deferred to setup: WorldGenRegistries is not safe to touch during construction
		event.enqueueWork(cr0s.warpdrive.data.Registration::registerChunkGenerators);
		event.enqueueWork(cr0s.warpdrive.data.Registration::registerConfiguredFeatures);
		event.enqueueWork(cr0s.warpdrive.block.movement.ChunkLoaderTicketManager::register);
		logger.info("WarpDrive common setup complete");
	}

	private void clientSetup(final FMLClientSetupEvent event) {
			event.enqueueWork(() -> {
			cr0s.warpdrive.client.ClientCloakManager.register();
			net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntityRenderer(
				cr0s.warpdrive.data.Registration.FORCE_FIELD_TILE.get(),
				cr0s.warpdrive.client.ForceFieldTileEntityRenderer::new);
			ScreenManager.register(
				cr0s.warpdrive.data.Registration.CREATIVE_ENERGY_CONTAINER.get(),
				cr0s.warpdrive.client.CreativeEnergyScreen::new);
			ScreenManager.register(
				cr0s.warpdrive.data.Registration.SHIP_CONTROLLER_CONTAINER.get(),
				cr0s.warpdrive.client.ShipControllerScreen::new);
			cr0s.warpdrive.client.ClientDimensionRendering.register();

			// Restore metadata-era catalog variants through lightweight item predicates. These are
			// evaluated only for item rendering and do not add blockstates.
			cr0s.warpdrive.data.Registration.LEGACY_CATALOG_ITEMS.forEach((name, item) -> {
				if (name.startsWith("electromagnetic_cell.") && !name.endsWith("-empty")) {
					net.minecraft.item.ItemModelsProperties.register(
						item.get(), new net.minecraft.util.ResourceLocation(MODID, "fill"),
						(itemStack, world, entity) ->
							cr0s.warpdrive.item.ElectromagneticCellItem.getFillLevel(itemStack));
				} else if (name.startsWith("plasma_torch.")) {
					net.minecraft.item.ItemModelsProperties.register(
						item.get(), new net.minecraft.util.ResourceLocation(MODID, "fill"),
						(itemStack, world, entity) ->
							cr0s.warpdrive.item.PlasmaTorchItem.getFillLevel(itemStack));
				}
			});
			cr0s.warpdrive.data.Registration.HULL_SLAB_ITEMS.values().forEach(item ->
				net.minecraft.item.ItemModelsProperties.register(
					item.get(), new net.minecraft.util.ResourceLocation(MODID, "slab_variant"),
					(itemStack, world, entity) ->
						cr0s.warpdrive.item.CatalogVariantBlockItem.getVariant(itemStack)));
			cr0s.warpdrive.data.Registration.FORCE_FIELD_PROJECTOR_ITEMS.values().forEach(item ->
				net.minecraft.item.ItemModelsProperties.register(
					item.get(), new net.minecraft.util.ResourceLocation(MODID, "projector_variant"),
					(itemStack, world, entity) ->
						cr0s.warpdrive.item.CatalogVariantBlockItem.getVariant(itemStack)));
			net.minecraft.item.ItemModelsProperties.register(
				cr0s.warpdrive.data.Registration.TRANSPORTER_BEACON_ITEM.get(),
				new net.minecraft.util.ResourceLocation(MODID, "active"),
				(itemStack, world, entity) ->
					cr0s.warpdrive.block.movement.TransporterBeaconBlockItem.isActive(itemStack)
						? 1.0F : 0.0F);
			net.minecraft.client.Minecraft.getInstance().getItemColors().register(
				cr0s.warpdrive.item.CatalogAirShieldItem::getTintColor,
				cr0s.warpdrive.data.Registration.AIR_SHIELD_ITEM.get());

			// Air blocks are translucent blue. Without this they render on the solid layer and the
			// texture's alpha is ignored, so the volume would appear as opaque cubes.
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.AIR_FLOW_BLOCK.get(),
				net.minecraft.client.renderer.RenderType.translucent());
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.AIR_SOURCE_BLOCK.get(),
				net.minecraft.client.renderer.RenderType.translucent());
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.AIR_SHIELD_BLOCK.get(),
				net.minecraft.client.renderer.RenderType.translucent());

			// Legacy transparent/cutout blocks. Render-layer selection was formerly supplied by
			// Block#getRenderLayer; in 1.16 it is a client registration concern.
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.BEDROCK_GLASS.get(),
				net.minecraft.client.renderer.RenderType.cutout());
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.VOID_SHELL_GLASS.get(),
				net.minecraft.client.renderer.RenderType.translucent());
			cr0s.warpdrive.data.Registration.ELECTROMAGNET_BLOCKS.forEach((name, block) -> {
				if (name.endsWith(".glass")) {
					net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
						block.get(), net.minecraft.client.renderer.RenderType.translucent());
				}
			});
			cr0s.warpdrive.data.Registration.DECORATIVE_BLOCKS.values().forEach(block ->
				net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
					block.get(), net.minecraft.client.renderer.RenderType.translucent()));
			cr0s.warpdrive.data.Registration.GAS_BLOCKS.values().forEach(block ->
				net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
					block.get(), net.minecraft.client.renderer.RenderType.translucent()));
			cr0s.warpdrive.data.Registration.HULL_GLASS_BLOCKS.values().forEach(block ->
				net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
					block.get(), net.minecraft.client.renderer.RenderType.translucent()));
			cr0s.warpdrive.data.Registration.HULL_OMNIPANELS.values().forEach(block ->
				net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
					block.get(), net.minecraft.client.renderer.RenderType.translucent()));
			cr0s.warpdrive.data.Registration.FORCE_FIELD_BLOCKS.values().forEach(block ->
				net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
					block.get(), net.minecraft.client.renderer.RenderType.translucent()));
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.TRANSPORTER_BEACON_BLOCK.get(),
				net.minecraft.client.renderer.RenderType.translucent());
			cr0s.warpdrive.data.Registration.LEGACY_CATALOG_BLOCKS.values().forEach(block ->
				net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
					block.get(), net.minecraft.client.renderer.RenderType.cutout()));
			net.minecraft.client.renderer.RenderTypeLookup.setRenderLayer(
				cr0s.warpdrive.data.Registration.LAMP_BLOCKS.get("bubble").get(),
				net.minecraft.client.renderer.RenderType.translucent());
		});
		logger.info("WarpDrive client setup complete");
	}
}
