package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.breathing.AirFlowBlock;
import cr0s.warpdrive.block.breathing.AirGeneratorBlock;
import cr0s.warpdrive.block.breathing.AirGeneratorTier;
import cr0s.warpdrive.block.breathing.AirGeneratorTileEntity;
import cr0s.warpdrive.block.breathing.AirShieldBlock;
import cr0s.warpdrive.block.breathing.AirSourceBlock;
import cr0s.warpdrive.block.CreativeEnergyBlock;
import cr0s.warpdrive.block.CreativeEnergyTileEntity;
import cr0s.warpdrive.block.SimpleBlock;
import cr0s.warpdrive.block.SimpleGlassBlock;
import cr0s.warpdrive.block.ShipCoreBlock;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.block.ShipControllerBlock;
import cr0s.warpdrive.block.ShipControllerTileEntity;
import cr0s.warpdrive.block.decoration.GasBlock;
import cr0s.warpdrive.block.decoration.LampBlock;
import cr0s.warpdrive.block.hull.HullOmnipanelBlock;
import cr0s.warpdrive.container.CreativeEnergyContainer;
import cr0s.warpdrive.container.ShipControllerContainer;
import cr0s.warpdrive.item.AirTankItem;
import cr0s.warpdrive.item.AirTankTier;
import cr0s.warpdrive.item.CatalogAirShieldItem;
import cr0s.warpdrive.item.CatalogElectromagneticCellItem;
import cr0s.warpdrive.item.CatalogVariantBlockItem;
import cr0s.warpdrive.item.ShipTokenItem;
import cr0s.warpdrive.item.TuningForkItem;
import cr0s.warpdrive.item.WarpArmorItem;
import cr0s.warpdrive.item.WarpArmorMaterial;
import cr0s.warpdrive.item.WrenchItem;
import cr0s.warpdrive.world.AsteroidFeature;
import cr0s.warpdrive.world.CelestialBodyFeature;
import cr0s.warpdrive.world.GiantAsteroidFeature;
import cr0s.warpdrive.world.VoidChunkGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.DyeColor;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.placement.ChanceConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.ToolType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Centralized registration system for WarpDrive using DeferredRegister pattern (1.16.5)
 *
 * This replaces the old RegistryEvent-based registration from 1.12.2
 */
public class Registration {

	// Deferred Registers for all registry types
	public static final DeferredRegister<Block> BLOCKS =
		DeferredRegister.create(ForgeRegistries.BLOCKS, WarpDrive.MODID);

	public static final DeferredRegister<Item> ITEMS =
		DeferredRegister.create(ForgeRegistries.ITEMS, WarpDrive.MODID);

	public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
		DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, WarpDrive.MODID);

	public static final DeferredRegister<ContainerType<?>> CONTAINERS =
		DeferredRegister.create(ForgeRegistries.CONTAINERS, WarpDrive.MODID);

	public static final DeferredRegister<SoundEvent> SOUNDS =
		DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WarpDrive.MODID);

	public static final DeferredRegister<Feature<?>> FEATURES =
		DeferredRegister.create(ForgeRegistries.FEATURES, WarpDrive.MODID);

	// ===== FLATTENED 1.12 ITEMS =====
	// Metadata subtypes became individual registry entries in 1.13's flattening. Their ids match
	// the surviving per-subtype model filenames, so no client-side metadata model hook is needed.

	public static final String[] COMPONENT_NAMES = {
		"memory_crystal", "memory_cluster",
		"diamond_crystal", "emerald_crystal",
		"capacitive_crystal", "capacitive_cluster",
		"ender_coil", "diamond_coil", "computer_interface",
		"bone_charcoal", "activated_carbon", "air_canister_empty",
		"flat_screen", "holographic_projector",
		"glass_tank", "motor", "pump",
		"lens", "zoom", "diffraction_grating",
		"power_interface", "superconductor",
		"laser_medium_empty", "electromagnetic_projector", "reactor_core",
		"raw_rubber", "rubber", "biopulp", "biofiber",
		"raw_ceramic", "ceramic",
		"raw_carbon_fiber", "raw_carbon_mesh", "carbon_fiber" };

	public static final Map<String, RegistryObject<Item>> COMPONENTS = new LinkedHashMap<>();

	static {
		for (final String component : COMPONENT_NAMES) {
			final String registryName = "component-" + component;
			COMPONENTS.put(component, ITEMS.register(registryName,
				() -> new Item(new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	private static final int[] SHIP_TOKEN_IDS = {
		0, 1, 2, 3, 4, 5,
		10, 11, 12, 13, 14, 15,
		20, 21, 22, 23, 24, 25,
		30, 31, 32, 33, 34, 35,
		40, 41, 42, 43, 44, 45 };

	public static final Map<Integer, RegistryObject<Item>> SHIP_TOKENS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> TUNING_FORKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_SHAPES = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_UPGRADES = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> LEGACY_CATALOG_ITEMS = new LinkedHashMap<>();

	private static Item createLegacyCatalogItem(final String name) {
		if (name.startsWith("electromagnetic_cell.")) {
			return name.endsWith("-empty")
				? new Item(new Item.Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1))
				: new CatalogElectromagneticCellItem();
		}
		return new Item(new Item.Properties().tab(WarpDriveItemGroup.MAIN));
	}

	public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", WrenchItem::new);

	static {
		for (final int schematicId : SHIP_TOKEN_IDS) {
			SHIP_TOKENS.put(schematicId, ITEMS.register("ship_token-" + schematicId,
				() -> new ShipTokenItem(schematicId)));
		}

		for (final DyeColor color : DyeColor.values()) {
			// 1.12 called light gray "silver"; keep the filename/registry spelling for its assets.
			final String name = color == DyeColor.LIGHT_GRAY ? "silver" : color.getName();
			TUNING_FORKS.put(name, ITEMS.register("tuning_fork-" + name,
				() -> new TuningForkItem(color)));
		}

		for (final String shape : new String[]{
			"sphere", "cylinder_h", "cylinder_v", "cube", "plane", "tube", "tunnel" }) {
			FORCE_FIELD_SHAPES.put(shape, ITEMS.register("force_field_shape-" + shape,
				() -> new Item(new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}

		for (final String upgrade : new String[]{
			"attraction", "breaking", "camouflage", "cooling", "fusion", "heating",
			"inversion", "item_port", "pumping", "range", "repulsion", "rotation",
			"shock", "silencer", "speed", "stabilization", "thickness", "translation" }) {
			FORCE_FIELD_UPGRADES.put(upgrade, ITEMS.register("force_field_upgrade-" + upgrade,
				() -> new Item(new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}

		// Art-complete 1.12.2 tools and containers whose machine behavior is intentionally deferred.
		for (final String name : new String[]{
			"book",
			"electromagnetic_cell.basic-empty", "electromagnetic_cell.basic-ion",
			"electromagnetic_cell.basic-proton", "electromagnetic_cell.basic-antimatter",
			"electromagnetic_cell.basic-strange_matter",
			"electromagnetic_cell.advanced-empty", "electromagnetic_cell.advanced-ion",
			"electromagnetic_cell.advanced-proton", "electromagnetic_cell.advanced-antimatter",
			"electromagnetic_cell.advanced-strange_matter",
			"electromagnetic_cell.superior-empty", "electromagnetic_cell.superior-ion",
			"electromagnetic_cell.superior-proton", "electromagnetic_cell.superior-antimatter",
			"electromagnetic_cell.superior-strange_matter",
			"ic2_reactor_laser_focus",
			"plasma_torch.basic", "plasma_torch.advanced", "plasma_torch.superior",
			"tuning_driver-beam_frequency", "tuning_driver-control_channel",
			"tuning_driver-video_channel" }) {
			LEGACY_CATALOG_ITEMS.put(name, ITEMS.register(name,
				() -> createLegacyCatalogItem(name)));
		}
	}

	// ===== ARMOUR =====
	// Three tiers x four slots. Stats live in WarpArmorMaterial and match 1.12.2 exactly.

	private static final EquipmentSlotType[] ARMOR_SLOTS = {
		EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET
	};
	private static final String[] ARMOR_SLOT_NAMES = { "helmet", "chestplate", "leggings", "boots" };

	public static final Map<String, RegistryObject<Item>> ARMOR = new LinkedHashMap<>();

	static {
		for (final WarpArmorMaterial material : WarpArmorMaterial.values()) {
			final String tier = material.name().toLowerCase(Locale.ROOT);
			for (int index = 0; index < ARMOR_SLOTS.length; index++) {
				final EquipmentSlotType slot = ARMOR_SLOTS[index];
				final String name = "warp_armor_" + tier + "_" + ARMOR_SLOT_NAMES[index];
				ARMOR.put(name, ITEMS.register(name, () -> new WarpArmorItem(material, slot)));
			}
		}
	}

	// ===== AIR TANKS =====
	// Registry names keep the 1.12.2 dot form ("air_tank.basic") so the surviving model files and
	// their six-stage damage overrides resolve without touching the asset pack.

	public static final Map<String, RegistryObject<Item>> AIR_TANKS = new LinkedHashMap<>();

	static {
		for (final AirTankTier tier : AirTankTier.values()) {
			final String name = "air_tank." + tier.getName();
			AIR_TANKS.put(name, ITEMS.register(name, () -> new AirTankItem(tier)));
		}
	}

	// ===== BREATHING =====
	// No BlockItems: these are placed by the air simulation, never by hand.

	public static final RegistryObject<Block> AIR_FLOW_BLOCK =
		BLOCKS.register("air_flow", AirFlowBlock::new);

	public static final RegistryObject<Block> AIR_SOURCE_BLOCK =
		BLOCKS.register("air_source", AirSourceBlock::new);

	/** Seals air while letting entities walk through - an airlock without a door. */
	public static final RegistryObject<Block> AIR_SHIELD_BLOCK =
		BLOCKS.register("air_shield", AirShieldBlock::new);

	public static final RegistryObject<Item> AIR_SHIELD_ITEM =
		ITEMS.register("air_shield",
			() -> new CatalogAirShieldItem(AIR_SHIELD_BLOCK.get(), WarpDriveItemGroup.MAIN));

	/** Air generators, one per tier. Registry names keep the 1.12.2 dotted form. */
	public static final Map<String, RegistryObject<Block>> AIR_GENERATOR_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> AIR_GENERATOR_ITEMS = new LinkedHashMap<>();

	static {
		for (final AirGeneratorTier tier : AirGeneratorTier.values()) {
			final String name = "air_generator." + tier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name, () -> new AirGeneratorBlock(tier));
			AIR_GENERATOR_BLOCKS.put(name, block);
			AIR_GENERATOR_ITEMS.put(name, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	public static final RegistryObject<TileEntityType<AirGeneratorTileEntity>> AIR_GENERATOR_TILE =
		TILE_ENTITIES.register("air_generator",
			() -> TileEntityType.Builder.of(AirGeneratorTileEntity::new,
				AIR_GENERATOR_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	// ===== WORLD GENERATION =====

	public static final RegistryObject<Feature<NoFeatureConfig>> ASTEROID_FIELD =
		FEATURES.register("asteroid_field", () -> new AsteroidFeature(NoFeatureConfig.CODEC));

	public static final RegistryObject<Feature<NoFeatureConfig>> GIANT_ASTEROID =
		FEATURES.register("giant_asteroid", () -> new GiantAsteroidFeature(NoFeatureConfig.CODEC));

	public static final RegistryObject<Feature<NoFeatureConfig>> CELESTIAL_BODIES =
		FEATURES.register("celestial_bodies", () -> new CelestialBodyFeature(NoFeatureConfig.CODEC));

	public static ConfiguredFeature<?, ?> GIANT_ASTEROID_CONFIGURED;
	public static ConfiguredFeature<?, ?> CELESTIAL_BODIES_CONFIGURED;

	/**
	 * Configured form of the asteroid field, built during common setup because it must go into
	 * WorldGenRegistries rather than a Forge registry. BiomeLoadingEvent then attaches it to the
	 * space biome.
	 */
	public static ConfiguredFeature<?, ?> ASTEROID_FIELD_CONFIGURED;

	/**
	 * Chunk generator codec, needed before any dimension JSON referencing it is parsed at world
	 * load - registering during common setup is early enough.
	 */
	public static void registerChunkGenerators() {
		Registry.register(Registry.CHUNK_GENERATOR,
			new ResourceLocation(WarpDrive.MODID, "void"), VoidChunkGenerator.CODEC);
	}

	public static void registerConfiguredFeatures() {
		ASTEROID_FIELD_CONFIGURED = ASTEROID_FIELD.get()
			.configured(NoFeatureConfig.INSTANCE)
			// Runs on every chunk; AsteroidFeature itself decides using village-style grid
			// spacing. A chance decorator was the wrong tool - independent per-chunk rolls clump,
			// which is what made the field look dense however low the rate went.
			.decorated(Placement.CHANCE.configured(new ChanceConfig(1)));
		Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,
			new ResourceLocation(WarpDrive.MODID, "asteroid_field"), ASTEROID_FIELD_CONFIGURED);

		GIANT_ASTEROID_CONFIGURED = GIANT_ASTEROID.get()
			.configured(NoFeatureConfig.INSTANCE)
			.decorated(Placement.CHANCE.configured(new ChanceConfig(1)));
		Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,
			new ResourceLocation(WarpDrive.MODID, "giant_asteroid"), GIANT_ASTEROID_CONFIGURED);

		CELESTIAL_BODIES_CONFIGURED = CELESTIAL_BODIES.get()
			.configured(NoFeatureConfig.INSTANCE)
			.decorated(Placement.CHANCE.configured(new ChanceConfig(1)));
		Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,
			new ResourceLocation(WarpDrive.MODID, "celestial_bodies"), CELESTIAL_BODIES_CONFIGURED);
	}

	// ===== SOUNDS =====
	// The .ogg files and assets/warpdrive/sounds.json survived from 1.12.2 - these are the
	// original WarpDrive sounds, and warp_4s/10s/30s were authored as countdown charge-ups.

	private static RegistryObject<SoundEvent> sound(final String name) {
		return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(WarpDrive.MODID, name)));
	}

	// Selection matches 1.12.2 TileEntityShipCore: <10s warmup -> warp_4s, >29s -> warp_30s,
	// otherwise warp_10s. warp_4s is rarely heard because warmup scales with ship size.
	public static final RegistryObject<SoundEvent> SOUND_WARP_4S = sound("warp_4s");
	public static final RegistryObject<SoundEvent> SOUND_WARP_10S = sound("warp_10s");
	public static final RegistryObject<SoundEvent> SOUND_WARP_30S = sound("warp_30s");
	public static final RegistryObject<SoundEvent> SOUND_CHILLER = sound("chiller");
	public static final RegistryObject<SoundEvent> SOUND_DECLOAK = sound("decloak");
	public static final RegistryObject<SoundEvent> SOUND_COLLISION = sound("collision_medium");

	// ===== BLOCKS =====

	public static final RegistryObject<Block> SHIP_CORE_BLOCK = BLOCKS.register("ship_core",
		ShipCoreBlock::new);
	public static final RegistryObject<Block> SHIP_CONTROLLER_BLOCK = BLOCKS.register("ship_controller",
		ShipControllerBlock::new);

	// ===== ITEMS (BlockItems) =====

	public static final RegistryObject<Item> SHIP_CORE_ITEM = ITEMS.register("ship_core",
		() -> new BlockItem(SHIP_CORE_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<Item> SHIP_CONTROLLER_ITEM = ITEMS.register("ship_controller",
		() -> new BlockItem(SHIP_CONTROLLER_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));

	// ===== TILE ENTITIES =====

	public static final RegistryObject<TileEntityType<ShipCoreTileEntity>> SHIP_CORE_TILE =
		TILE_ENTITIES.register("ship_core",
			() -> TileEntityType.Builder.of(ShipCoreTileEntity::new, SHIP_CORE_BLOCK.get()).build(null));
	public static final RegistryObject<TileEntityType<ShipControllerTileEntity>> SHIP_CONTROLLER_TILE =
		TILE_ENTITIES.register("ship_controller",
			() -> TileEntityType.Builder.of(ShipControllerTileEntity::new,
				SHIP_CONTROLLER_BLOCK.get()).build(null));

	public static final RegistryObject<ContainerType<ShipControllerContainer>> SHIP_CONTROLLER_CONTAINER =
		CONTAINERS.register("ship_controller",
			() -> IForgeContainerType.create(ShipControllerContainer::new));

	// ===== CREATIVE ENERGY SOURCE =====

	public static final RegistryObject<Block> CREATIVE_ENERGY_BLOCK = BLOCKS.register("creative_energy",
		CreativeEnergyBlock::new);

	public static final RegistryObject<Item> CREATIVE_ENERGY_ITEM = ITEMS.register("creative_energy",
		() -> new BlockItem(CREATIVE_ENERGY_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));

	public static final RegistryObject<TileEntityType<CreativeEnergyTileEntity>> CREATIVE_ENERGY_TILE =
		TILE_ENTITIES.register("creative_energy",
			() -> TileEntityType.Builder.of(CreativeEnergyTileEntity::new, CREATIVE_ENERGY_BLOCK.get()).build(null));

	public static final RegistryObject<ContainerType<CreativeEnergyContainer>> CREATIVE_ENERGY_CONTAINER =
		CONTAINERS.register("creative_energy",
			() -> IForgeContainerType.create(CreativeEnergyContainer::new));

	// ===== SIMPLE LEGACY BLOCKS =====
	// Machines with tile entities remain intentionally absent until their behaviour is ported. The
	// entries below were passive materials/casings or have small, self-contained behaviour.

	public static final Map<String, RegistryObject<Block>> LEGACY_SIMPLE_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> LEGACY_SIMPLE_BLOCK_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> ELECTROMAGNET_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> DECORATIVE_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> GAS_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> LAMP_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> LEGACY_CATALOG_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> LEGACY_CATALOG_BLOCK_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> HULL_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> HULL_BLOCK_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> HULL_GLASS_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> HULL_OMNIPANELS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> HULL_SLAB_ITEMS = new LinkedHashMap<>();

	private static RegistryObject<Block> registerBlockWithItem(final String name,
	                                                          final Supplier<? extends Block> supplier) {
		final RegistryObject<Block> block = BLOCKS.register(name, supplier);
		LEGACY_SIMPLE_BLOCKS.put(name, block);
		LEGACY_SIMPLE_BLOCK_ITEMS.put(name, ITEMS.register(name,
			() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		return block;
	}

	private static RegistryObject<Block> registerCatalogBlock(final String name) {
		final RegistryObject<Block> block = BLOCKS.register(name,
			() -> new SimpleBlock(metal(3.5F, 10.0F)));
		LEGACY_CATALOG_BLOCKS.put(name, block);
		final int variantCount = name.startsWith("projector.")
			|| (name.startsWith("capacitor.") && !name.endsWith(".creative")) ? 2 : 1;
		LEGACY_CATALOG_BLOCK_ITEMS.put(name, ITEMS.register(name, () -> {
			if (name.startsWith("projector.")) {
				return new CatalogVariantBlockItem(block.get(), WarpDriveItemGroup.MAIN,
					2, "single", "double");
			}
			if (variantCount > 1) {
				return new CatalogVariantBlockItem(block.get(), WarpDriveItemGroup.MAIN, variantCount);
			}
			return new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN));
		}));
		return block;
	}

	private static RegistryObject<Block> registerHullSlab(final String name,
	                                                     final Supplier<? extends Block> supplier) {
		final RegistryObject<Block> block = BLOCKS.register(name, supplier);
		HULL_BLOCKS.put(name, block);
		final RegistryObject<Item> item = ITEMS.register(name,
			() -> new CatalogVariantBlockItem(block.get(), WarpDriveItemGroup.HULL, 8));
		HULL_BLOCK_ITEMS.put(name, item);
		HULL_SLAB_ITEMS.put(name, item);
		return block;
	}

	private static RegistryObject<Block> registerHullBlock(final String name,
	                                                      final Supplier<? extends Block> supplier) {
		final RegistryObject<Block> block = BLOCKS.register(name, supplier);
		HULL_BLOCKS.put(name, block);
		HULL_BLOCK_ITEMS.put(name, ITEMS.register(name,
			() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.HULL))));
		return block;
	}

	private static AbstractBlock.Properties hull(final int tier, final boolean glass) {
		final float[] hardness = { 25.0F, 50.0F, 80.0F };
		final float[] resistance = { 60.0F, 90.0F, 120.0F };
		return AbstractBlock.Properties.of(glass ? Material.GLASS : Material.STONE)
			.strength(hardness[tier], resistance[tier])
			.sound(glass ? SoundType.GLASS : SoundType.STONE)
			.requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE);
	}

	private static AbstractBlock.Properties metal(final float hardness, final float resistance) {
		return AbstractBlock.Properties.of(Material.METAL)
			.strength(hardness, resistance)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE);
	}

	private static AbstractBlock.Properties glass(final float hardness, final float resistance) {
		return AbstractBlock.Properties.of(Material.GLASS)
			.strength(hardness, resistance)
			.sound(SoundType.GLASS)
			.noOcclusion();
	}

	public static final RegistryObject<Block> BEDROCK_GLASS = registerBlockWithItem("bedrock_glass",
		() -> new SimpleGlassBlock(glass(-1.0F, 6_000_000.0F).sound(SoundType.STONE), false));
	public static final RegistryObject<Block> HIGHLY_ADVANCED_MACHINE = registerBlockWithItem("highly_advanced_machine",
		() -> new SimpleBlock(metal(5.0F, 10.0F)));
	public static final RegistryObject<Block> IRIDIUM_BLOCK = registerBlockWithItem("iridium_block",
		() -> new SimpleBlock(metal(3.4F, 600.0F)));
	public static final RegistryObject<Block> WARP_ISOLATION = registerBlockWithItem("warp_isolation",
		() -> new SimpleBlock(metal(3.5F, 10.0F)));
	public static final RegistryObject<Block> VOID_SHELL_PLAIN = registerBlockWithItem("void_shell.plain",
		() -> new SimpleBlock(metal(5.0F, 6.0F)));
	public static final RegistryObject<Block> VOID_SHELL_GLASS = registerBlockWithItem("void_shell.glass",
		() -> new SimpleGlassBlock(glass(5.0F, 6.0F)));

	static {
		final String[] tiers = { "basic", "advanced", "superior" };
		for (int index = 0; index < tiers.length; index++) {
			final String tier = tiers[index];
			final float hardness = 5.0F + index;
			final float resistance = new float[]{ 6.0F, 10.0F, 13.0F }[index];

			final String plainName = "electromagnet." + tier + ".plain";
			ELECTROMAGNET_BLOCKS.put(plainName, registerBlockWithItem(plainName,
				() -> new SimpleBlock(metal(hardness, resistance))));

			final String glassName = "electromagnet." + tier + ".glass";
			ELECTROMAGNET_BLOCKS.put(glassName, registerBlockWithItem(glassName,
				() -> new SimpleGlassBlock(glass(hardness, resistance))));
		}

		for (final String type : new String[]{
			"plain", "grated", "glass", "stripes_black_down", "stripes_black_up",
			"stripes_yellow_down", "stripes_yellow_up" }) {
			final String name = "plain".equals(type) ? "decorative" : "decorative-" + type;
			final RegistryObject<Block> block = "glass".equals(type)
				? registerBlockWithItem(name, () -> new SimpleGlassBlock(glass(1.5F, 10.0F)))
				: registerBlockWithItem(name, () -> new SimpleBlock(metal(1.5F, 10.0F)));
			DECORATIVE_BLOCKS.put(type, block);
		}

		for (final String color : new String[]{
			"blue", "red", "green", "yellow", "dark", "darkness", "white", "milk",
			"orange", "siren", "gray", "violet" }) {
			final String name = "red".equals(color) ? "gas" : "gas-" + color;
			GAS_BLOCKS.put(color, registerBlockWithItem(name, GasBlock::new));
		}

		LAMP_BLOCKS.put("bubble", registerBlockWithItem("lamp_bubble",
			() -> new LampBlock(LampBlock.Style.BUBBLE)));
		LAMP_BLOCKS.put("flat", registerBlockWithItem("lamp_flat",
			() -> new LampBlock(LampBlock.Style.FLAT)));
		LAMP_BLOCKS.put("long", registerBlockWithItem("lamp_long",
			() -> new LampBlock(LampBlock.Style.LONG)));

		// Functional machine logic is deliberately deferred, but every legacy machine with complete
		// art remains placeable and visible in creative/JEI during the port.
		for (final String name : new String[]{
			"accelerator_control_point", "accelerator_core", "biometric_scanner", "camera",
			"capacitor.basic", "capacitor.advanced", "capacitor.superior", "capacitor.creative",
			"chiller.basic", "chiller.advanced", "chiller.superior",
			"chunk_loader.basic", "chunk_loader.advanced", "chunk_loader.superior",
			"cloaking_coil", "cloaking_core",
			"enan_reactor_core.basic", "enan_reactor_core.advanced", "enan_reactor_core.superior",
			"enan_reactor_laser", "environmental_sensor",
			"force_field.basic", "force_field.advanced", "force_field.superior",
			"force_field_relay.basic", "force_field_relay.advanced", "force_field_relay.superior",
			"ic2_reactor_laser_cooler", "laser", "laser_camera",
			"laser_medium.basic", "laser_medium.advanced", "laser_medium.superior",
			"laser_tree_farm", "lift", "mining_laser", "monitor",
			"particles_collider", "particles_injector",
			"projector.basic", "projector.advanced", "projector.superior",
			"radar", "security_station",
			"ship_controller.basic", "ship_controller.advanced", "ship_controller.superior",
			"ship_core.basic", "ship_core.advanced", "ship_core.superior",
			"ship_scanner.basic", "ship_scanner.advanced", "ship_scanner.superior",
			"siren_industrial.basic", "siren_industrial.advanced", "siren_industrial.superior",
			"siren_military.basic", "siren_military.advanced", "siren_military.superior",
			"speaker.basic", "speaker.advanced", "speaker.superior",
			"transporter_beacon", "transporter_containment", "transporter_core",
			"transporter_scanner", "virtual_assistant.basic", "virtual_assistant.advanced",
			"virtual_assistant.superior", "weapon_controller" }) {
			registerCatalogBlock(name);
		}

		final String[] hullTiers = { "basic", "advanced", "superior" };
		final String[] colors = {
			"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
			"silver", "cyan", "purple", "blue", "brown", "green", "red", "black" };
		for (int tierIndex = 0; tierIndex < hullTiers.length; tierIndex++) {
			final int tier = tierIndex;
			final String tierName = hullTiers[tierIndex];
			for (final String color : colors) {
				final String plainName = "hull." + tierName + ".plain-" + color;
				final RegistryObject<Block> plain = registerHullBlock(plainName,
					() -> new SimpleBlock(hull(tier, false)));
				registerHullBlock("hull." + tierName + ".tiled-" + color,
					() -> new SimpleBlock(hull(tier, false)));

				final String glassName = "hull." + tierName + ".glass-" + color;
				final RegistryObject<Block> glassBlock = registerHullBlock(glassName,
					() -> new SimpleGlassBlock(hull(tier, true).noOcclusion()));
				HULL_GLASS_BLOCKS.put(glassName, glassBlock);

				final String omnipanelName = "hull." + tierName + ".omnipanel-" + color;
				final RegistryObject<Block> omnipanel = registerHullBlock(omnipanelName,
					() -> new HullOmnipanelBlock(hull(tier, true)));
				HULL_OMNIPANELS.put(omnipanelName, omnipanel);

				registerHullSlab("hull." + tierName + ".slab_" + color,
					() -> new SlabBlock(hull(tier, false)));
				registerHullBlock("hull." + tierName + ".stairs_" + color,
					() -> new StairsBlock(() -> plain.get().defaultBlockState(), hull(tier, false)));
			}
		}
	}

	// TODO: Add more registries as needed in later phases:
	// - ENTITIES (for ship-related entities)
	// - SOUND_EVENTS (warp sound effects)
	// - DIMENSIONS (Space dimension)

	/**
	 * Call this during mod construction to register all deferred registers
	 */
	public static void init() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		TILE_ENTITIES.register(modEventBus);
		CONTAINERS.register(modEventBus);
		SOUNDS.register(modEventBus);
		FEATURES.register(modEventBus);

		WarpDrive.logger.info("Deferred registers initialized");
	}
}
