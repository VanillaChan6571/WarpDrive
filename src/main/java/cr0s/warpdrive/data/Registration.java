package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.breathing.AirFlowBlock;
import cr0s.warpdrive.block.breathing.AirGeneratorBlock;
import cr0s.warpdrive.block.breathing.AirGeneratorTier;
import cr0s.warpdrive.block.breathing.AirGeneratorTileEntity;
import cr0s.warpdrive.block.breathing.AirShieldBlock;
import cr0s.warpdrive.block.breathing.AirSourceBlock;
import cr0s.warpdrive.block.atomic.AcceleratorComponentBlock;
import cr0s.warpdrive.block.atomic.AcceleratorCoreBlock;
import cr0s.warpdrive.block.atomic.AcceleratorCoreTileEntity;
import cr0s.warpdrive.block.atomic.AcceleratorControlPointBlock;
import cr0s.warpdrive.block.atomic.AcceleratorControlPointTileEntity;
import cr0s.warpdrive.block.atomic.AcceleratorTier;
import cr0s.warpdrive.block.atomic.ChillerBlock;
import cr0s.warpdrive.block.atomic.ParticlesInjectorTileEntity;
import cr0s.warpdrive.block.building.ShipScannerBlock;
import cr0s.warpdrive.block.building.ShipScannerTier;
import cr0s.warpdrive.block.building.ShipScannerTileEntity;
import cr0s.warpdrive.block.CreativeEnergyBlock;
import cr0s.warpdrive.block.CreativeEnergyTileEntity;
import cr0s.warpdrive.block.SimpleBlock;
import cr0s.warpdrive.block.SimpleGlassBlock;
import cr0s.warpdrive.block.ShipCoreBlock;
import cr0s.warpdrive.block.ShipCoreTier;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.block.ShipControllerBlock;
import cr0s.warpdrive.block.ShipControllerTileEntity;
import cr0s.warpdrive.block.decoration.GasBlock;
import cr0s.warpdrive.block.detection.SirenBlock;
import cr0s.warpdrive.block.detection.SirenStyle;
import cr0s.warpdrive.block.detection.SirenTier;
import cr0s.warpdrive.block.detection.SirenTileEntity;
import cr0s.warpdrive.block.detection.SpeakerBlock;
import cr0s.warpdrive.block.detection.SpeakerTier;
import cr0s.warpdrive.block.detection.SpeakerTileEntity;
import cr0s.warpdrive.block.detection.CameraBlock;
import cr0s.warpdrive.block.detection.CameraTileEntity;
import cr0s.warpdrive.block.detection.MonitorBlock;
import cr0s.warpdrive.block.detection.MonitorTileEntity;
import cr0s.warpdrive.block.detection.EnvironmentalSensorBlock;
import cr0s.warpdrive.block.detection.EnvironmentalSensorTileEntity;
import cr0s.warpdrive.block.detection.BiometricScannerBlock;
import cr0s.warpdrive.block.detection.BiometricScannerTileEntity;
import cr0s.warpdrive.block.detection.SecurityStationBlock;
import cr0s.warpdrive.block.detection.SecurityStationTileEntity;
import cr0s.warpdrive.block.detection.CloakingCoilBlock;
import cr0s.warpdrive.block.detection.CloakingCoreBlock;
import cr0s.warpdrive.block.detection.CloakingCoreTileEntity;
import cr0s.warpdrive.block.detection.VirtualAssistantBlock;
import cr0s.warpdrive.block.detection.VirtualAssistantTier;
import cr0s.warpdrive.block.detection.VirtualAssistantTileEntity;
import cr0s.warpdrive.block.detection.RadarBlock;
import cr0s.warpdrive.block.detection.RadarTileEntity;
import cr0s.warpdrive.block.energy.CapacitorBlock;
import cr0s.warpdrive.block.energy.CapacitorTier;
import cr0s.warpdrive.block.energy.CapacitorTileEntity;
import cr0s.warpdrive.block.energy.LaserMediumBlock;
import cr0s.warpdrive.block.energy.LaserMediumTier;
import cr0s.warpdrive.block.energy.LaserMediumTileEntity;
import cr0s.warpdrive.block.energy.EnanReactorCoreBlock;
import cr0s.warpdrive.block.energy.EnanReactorCoreTileEntity;
import cr0s.warpdrive.block.energy.EnanReactorLaserBlock;
import cr0s.warpdrive.block.energy.EnanReactorLaserTileEntity;
import cr0s.warpdrive.block.energy.EnanReactorTier;
import cr0s.warpdrive.block.forcefield.ForceFieldBlock;
import cr0s.warpdrive.block.forcefield.ForceFieldProjectorBlock;
import cr0s.warpdrive.block.forcefield.ForceFieldProjectorTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldRelayBlock;
import cr0s.warpdrive.block.forcefield.ForceFieldRelayTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldTier;
import cr0s.warpdrive.block.forcefield.ForceFieldTileEntity;
import cr0s.warpdrive.block.decoration.LampBlock;
import cr0s.warpdrive.block.hull.HullOmnipanelBlock;
import cr0s.warpdrive.block.collection.MiningLaserBlock;
import cr0s.warpdrive.block.collection.MiningLaserTileEntity;
import cr0s.warpdrive.block.collection.LaserTreeFarmBlock;
import cr0s.warpdrive.block.collection.LaserTreeFarmTileEntity;
import cr0s.warpdrive.block.movement.LiftBlock;
import cr0s.warpdrive.block.movement.LiftTileEntity;
import cr0s.warpdrive.block.movement.ChunkLoaderBlock;
import cr0s.warpdrive.block.movement.ChunkLoaderTier;
import cr0s.warpdrive.block.movement.ChunkLoaderTileEntity;
import cr0s.warpdrive.block.movement.TransporterContainmentBlock;
import cr0s.warpdrive.block.movement.TransporterScannerBlock;
import cr0s.warpdrive.block.movement.TransporterCoreBlock;
import cr0s.warpdrive.block.movement.TransporterCoreTileEntity;
import cr0s.warpdrive.block.movement.TransporterBeaconBlock;
import cr0s.warpdrive.block.movement.TransporterBeaconBlockItem;
import cr0s.warpdrive.block.movement.TransporterBeaconTileEntity;
import cr0s.warpdrive.block.weapon.LaserBlock;
import cr0s.warpdrive.block.weapon.LaserCameraBlock;
import cr0s.warpdrive.block.weapon.LaserCameraTileEntity;
import cr0s.warpdrive.block.weapon.LaserTileEntity;
import cr0s.warpdrive.block.weapon.WeaponControllerBlock;
import cr0s.warpdrive.block.weapon.WeaponControllerTileEntity;
import cr0s.warpdrive.container.CreativeEnergyContainer;
import cr0s.warpdrive.container.ShipControllerContainer;
import cr0s.warpdrive.item.AirTankItem;
import cr0s.warpdrive.item.AirTankTier;
import cr0s.warpdrive.item.CatalogAirShieldItem;
import cr0s.warpdrive.item.ElectromagneticCellItem;
import cr0s.warpdrive.item.CatalogVariantBlockItem;
import cr0s.warpdrive.item.ShipTokenItem;
import cr0s.warpdrive.item.PlasmaTorchItem;
import cr0s.warpdrive.item.ManualBookItem;
import cr0s.warpdrive.item.TuningDriverItem;
import cr0s.warpdrive.item.TuningForkItem;
import cr0s.warpdrive.item.WarpArmorItem;
import cr0s.warpdrive.item.WarpArmorMaterial;
import cr0s.warpdrive.item.WrenchItem;
import cr0s.warpdrive.recipe.TuningDriverRecipe;
import cr0s.warpdrive.recipe.ParticleShapedRecipe;
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
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
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

	public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS =
		DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, WarpDrive.MODID);

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
	public static final Map<TuningDriverItem.Mode, RegistryObject<Item>> TUNING_DRIVERS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_SHAPES = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_UPGRADES = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> LEGACY_CATALOG_ITEMS = new LinkedHashMap<>();

	private static Item createLegacyCatalogItem(final String name) {
		if ("book".equals(name)) {
			return new ManualBookItem();
		}
		if (name.startsWith("electromagnetic_cell.")) {
			final int separator = name.indexOf('-');
			final AcceleratorTier tier = AcceleratorTier.valueOf(
				name.substring("electromagnetic_cell.".length(), separator).toUpperCase(Locale.ROOT));
			final String particleName = name.substring(separator + 1);
			return new ElectromagneticCellItem(tier, "empty".equals(particleName) ? null
				: ParticleType.valueOf(particleName.toUpperCase(Locale.ROOT)));
		}
		if (name.startsWith("plasma_torch.")) {
			return new PlasmaTorchItem(AcceleratorTier.valueOf(
				name.substring("plasma_torch.".length()).toUpperCase(Locale.ROOT)));
		}
		return new Item(new Item.Properties().tab(WarpDriveItemGroup.MAIN));
	}

	public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench", WrenchItem::new);

	public static final RegistryObject<SpecialRecipeSerializer<TuningDriverRecipe>> TUNING_DRIVER_VIDEO_RECIPE =
		RECIPE_SERIALIZERS.register("tuning_driver_video_channel",
			() -> new SpecialRecipeSerializer<>(id ->
				new TuningDriverRecipe(id, TuningDriverItem.Mode.VIDEO_CHANNEL, 7)));

	public static final RegistryObject<SpecialRecipeSerializer<TuningDriverRecipe>> TUNING_DRIVER_BEAM_RECIPE =
		RECIPE_SERIALIZERS.register("tuning_driver_beam_frequency",
			() -> new SpecialRecipeSerializer<>(id ->
				new TuningDriverRecipe(id, TuningDriverItem.Mode.BEAM_FREQUENCY, 4)));

	public static final RegistryObject<SpecialRecipeSerializer<TuningDriverRecipe>> TUNING_DRIVER_CONTROL_RECIPE =
		RECIPE_SERIALIZERS.register("tuning_driver_control_channel",
			() -> new SpecialRecipeSerializer<>(id ->
				new TuningDriverRecipe(id, TuningDriverItem.Mode.CONTROL_CHANNEL, 7)));

	public static final RegistryObject<IRecipeSerializer<?>> PARTICLE_SHAPED_RECIPE =
		RECIPE_SERIALIZERS.register("particle_shaped", ParticleShapedRecipe.Serializer::new);

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

		for (final TuningDriverItem.Mode mode : TuningDriverItem.Mode.values()) {
			TUNING_DRIVERS.put(mode, ITEMS.register("tuning_driver-" + mode.getRegistrySuffix(),
				() -> new TuningDriverItem(mode)));
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

		// Flattened legacy tools and particle containers.
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
			"plasma_torch.basic", "plasma_torch.advanced", "plasma_torch.superior" }) {
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

	// ===== ENERGY =====
	// Subspace capacitors: a real buffer with per-face routing, one block per tier.

	public static final Map<String, RegistryObject<Block>> CAPACITOR_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> CAPACITOR_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> LASER_MEDIUM_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> LASER_MEDIUM_ITEMS = new LinkedHashMap<>();

	static {
		for (final CapacitorTier tier : CapacitorTier.values()) {
			final String name = "capacitor." + tier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name, () -> new CapacitorBlock(tier));
			CAPACITOR_BLOCKS.put(name, block);
			CAPACITOR_ITEMS.put(name, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
		for (final LaserMediumTier tier : LaserMediumTier.values()) {
			final String name = "laser_medium." + tier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name, () -> new LaserMediumBlock(tier));
			LASER_MEDIUM_BLOCKS.put(name, block);
			LASER_MEDIUM_ITEMS.put(name, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	public static final RegistryObject<TileEntityType<CapacitorTileEntity>> CAPACITOR_TILE =
		TILE_ENTITIES.register("capacitor",
			() -> TileEntityType.Builder.of(CapacitorTileEntity::new,
				CAPACITOR_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	public static final RegistryObject<TileEntityType<AirGeneratorTileEntity>> AIR_GENERATOR_TILE =
		TILE_ENTITIES.register("air_generator",
			() -> TileEntityType.Builder.of(AirGeneratorTileEntity::new,
				AIR_GENERATOR_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	public static final RegistryObject<TileEntityType<LaserMediumTileEntity>> LASER_MEDIUM_TILE =
		TILE_ENTITIES.register("laser_medium",
			() -> TileEntityType.Builder.of(LaserMediumTileEntity::new,
				LASER_MEDIUM_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	// The laser cannon consumes the medium line directly; it does not expose an FE capability.
	public static final RegistryObject<Block> LASER_BLOCK =
		BLOCKS.register("laser", LaserBlock::new);
	public static final RegistryObject<Item> LASER_ITEM =
		ITEMS.register("laser",
			() -> new BlockItem(LASER_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<LaserTileEntity>> LASER_TILE =
		TILE_ENTITIES.register("laser",
			() -> TileEntityType.Builder.of(LaserTileEntity::new, LASER_BLOCK.get()).build(null));

	public static final RegistryObject<Block> LASER_CAMERA_BLOCK =
		BLOCKS.register("laser_camera", LaserCameraBlock::new);
	public static final RegistryObject<Item> LASER_CAMERA_ITEM =
		ITEMS.register("laser_camera",
			() -> new BlockItem(LASER_CAMERA_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<LaserCameraTileEntity>> LASER_CAMERA_TILE =
		TILE_ENTITIES.register("laser_camera",
			() -> TileEntityType.Builder.of(LaserCameraTileEntity::new, LASER_CAMERA_BLOCK.get()).build(null));

	public static final RegistryObject<Block> WEAPON_CONTROLLER_BLOCK =
		BLOCKS.register("weapon_controller", WeaponControllerBlock::new);
	public static final RegistryObject<Item> WEAPON_CONTROLLER_ITEM =
		ITEMS.register("weapon_controller", () -> new BlockItem(WEAPON_CONTROLLER_BLOCK.get(),
			new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<WeaponControllerTileEntity>> WEAPON_CONTROLLER_TILE =
		TILE_ENTITIES.register("weapon_controller", () -> TileEntityType.Builder.of(
			WeaponControllerTileEntity::new, WEAPON_CONTROLLER_BLOCK.get()).build(null));

	public static final RegistryObject<Block> CAMERA_BLOCK =
		BLOCKS.register("camera", CameraBlock::new);
	public static final RegistryObject<Item> CAMERA_ITEM =
		ITEMS.register("camera",
			() -> new BlockItem(CAMERA_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<CameraTileEntity>> CAMERA_TILE =
		TILE_ENTITIES.register("camera",
			() -> TileEntityType.Builder.of(CameraTileEntity::new, CAMERA_BLOCK.get()).build(null));

	public static final RegistryObject<Block> MONITOR_BLOCK =
		BLOCKS.register("monitor", MonitorBlock::new);
	public static final RegistryObject<Item> MONITOR_ITEM =
		ITEMS.register("monitor",
			() -> new BlockItem(MONITOR_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<MonitorTileEntity>> MONITOR_TILE =
		TILE_ENTITIES.register("monitor",
			() -> TileEntityType.Builder.of(MonitorTileEntity::new, MONITOR_BLOCK.get()).build(null));

	public static final RegistryObject<Block> ENVIRONMENTAL_SENSOR_BLOCK =
		BLOCKS.register("environmental_sensor", EnvironmentalSensorBlock::new);
	public static final RegistryObject<Item> ENVIRONMENTAL_SENSOR_ITEM =
		ITEMS.register("environmental_sensor", () -> new BlockItem(ENVIRONMENTAL_SENSOR_BLOCK.get(),
			new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<EnvironmentalSensorTileEntity>> ENVIRONMENTAL_SENSOR_TILE =
		TILE_ENTITIES.register("environmental_sensor", () -> TileEntityType.Builder.of(
			EnvironmentalSensorTileEntity::new, ENVIRONMENTAL_SENSOR_BLOCK.get()).build(null));

	public static final RegistryObject<Block> BIOMETRIC_SCANNER_BLOCK =
		BLOCKS.register("biometric_scanner", BiometricScannerBlock::new);
	public static final RegistryObject<Item> BIOMETRIC_SCANNER_ITEM =
		ITEMS.register("biometric_scanner", () -> new BlockItem(BIOMETRIC_SCANNER_BLOCK.get(),
			new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<BiometricScannerTileEntity>> BIOMETRIC_SCANNER_TILE =
		TILE_ENTITIES.register("biometric_scanner", () -> TileEntityType.Builder.of(
			BiometricScannerTileEntity::new, BIOMETRIC_SCANNER_BLOCK.get()).build(null));

	public static final RegistryObject<Block> SECURITY_STATION_BLOCK =
		BLOCKS.register("security_station", SecurityStationBlock::new);
	public static final RegistryObject<Item> SECURITY_STATION_ITEM =
		ITEMS.register("security_station", () -> new BlockItem(SECURITY_STATION_BLOCK.get(),
			new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<SecurityStationTileEntity>> SECURITY_STATION_TILE =
		TILE_ENTITIES.register("security_station", () -> TileEntityType.Builder.of(
			SecurityStationTileEntity::new, SECURITY_STATION_BLOCK.get()).build(null));

	public static final Map<String, RegistryObject<Block>> VIRTUAL_ASSISTANT_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> VIRTUAL_ASSISTANT_ITEMS = new LinkedHashMap<>();

	static {
		for (final VirtualAssistantTier tier : VirtualAssistantTier.values()) {
			final String name = "virtual_assistant." + tier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name,
				() -> new VirtualAssistantBlock(tier));
			VIRTUAL_ASSISTANT_BLOCKS.put(name, block);
			VIRTUAL_ASSISTANT_ITEMS.put(name, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	public static final RegistryObject<TileEntityType<VirtualAssistantTileEntity>> VIRTUAL_ASSISTANT_TILE =
		TILE_ENTITIES.register("virtual_assistant", () -> TileEntityType.Builder.of(
			VirtualAssistantTileEntity::new,
			VIRTUAL_ASSISTANT_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new))
			.build(null));

	public static final RegistryObject<Block> RADAR_BLOCK = BLOCKS.register("radar", RadarBlock::new);
	public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar",
		() -> new BlockItem(RADAR_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<RadarTileEntity>> RADAR_TILE =
		TILE_ENTITIES.register("radar",
			() -> TileEntityType.Builder.of(RadarTileEntity::new, RADAR_BLOCK.get()).build(null));

	public static final RegistryObject<Block> MINING_LASER_BLOCK =
		BLOCKS.register("mining_laser", MiningLaserBlock::new);
	public static final RegistryObject<Item> MINING_LASER_ITEM =
		ITEMS.register("mining_laser",
			() -> new BlockItem(MINING_LASER_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<MiningLaserTileEntity>> MINING_LASER_TILE =
		TILE_ENTITIES.register("mining_laser",
			() -> TileEntityType.Builder.of(MiningLaserTileEntity::new, MINING_LASER_BLOCK.get()).build(null));

	public static final RegistryObject<Block> LASER_TREE_FARM_BLOCK =
		BLOCKS.register("laser_tree_farm", LaserTreeFarmBlock::new);
	public static final RegistryObject<Item> LASER_TREE_FARM_ITEM =
		ITEMS.register("laser_tree_farm",
			() -> new BlockItem(LASER_TREE_FARM_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<LaserTreeFarmTileEntity>> LASER_TREE_FARM_TILE =
		TILE_ENTITIES.register("laser_tree_farm",
			() -> TileEntityType.Builder.of(LaserTreeFarmTileEntity::new, LASER_TREE_FARM_BLOCK.get()).build(null));

	public static final RegistryObject<Block> LIFT_BLOCK =
		BLOCKS.register("lift", LiftBlock::new);
	public static final RegistryObject<Item> LIFT_ITEM =
		ITEMS.register("lift",
			() -> new BlockItem(LIFT_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<TileEntityType<LiftTileEntity>> LIFT_TILE =
		TILE_ENTITIES.register("lift",
			() -> TileEntityType.Builder.of(LiftTileEntity::new, LIFT_BLOCK.get()).build(null));

	// ===== SHIP SCANNERS =====
	public static final Map<String, RegistryObject<Block>> SHIP_SCANNER_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> SHIP_SCANNER_ITEMS = new LinkedHashMap<>();

	static {
		for (final ShipScannerTier tier : ShipScannerTier.values()) {
			final String name = "ship_scanner." + tier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name, () -> new ShipScannerBlock(tier));
			SHIP_SCANNER_BLOCKS.put(tier.getName(), block);
			SHIP_SCANNER_ITEMS.put(tier.getName(), ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	public static final RegistryObject<TileEntityType<ShipScannerTileEntity>> SHIP_SCANNER_TILE =
		TILE_ENTITIES.register("ship_scanner", () -> TileEntityType.Builder.of(
			ShipScannerTileEntity::new, SHIP_SCANNER_BLOCKS.values().stream()
				.map(RegistryObject::get).toArray(Block[]::new)).build(null));

	public static final Map<String, RegistryObject<Block>> SIREN_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> SIREN_ITEMS = new LinkedHashMap<>();

	static {
		for (final SirenStyle style : SirenStyle.values()) {
			for (final SirenTier tier : SirenTier.values()) {
				final String name = "siren_" + style.getName() + "." + tier.getName();
				final RegistryObject<Block> block = BLOCKS.register(name, () -> new SirenBlock(style, tier));
				SIREN_BLOCKS.put(name, block);
				SIREN_ITEMS.put(name, ITEMS.register(name,
					() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
			}
		}
	}

	public static final RegistryObject<TileEntityType<SirenTileEntity>> SIREN_TILE =
		TILE_ENTITIES.register("siren",
			() -> TileEntityType.Builder.of(SirenTileEntity::new,
				SIREN_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	public static final Map<String, RegistryObject<Block>> SPEAKER_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> SPEAKER_ITEMS = new LinkedHashMap<>();

	static {
		for (final SpeakerTier tier : SpeakerTier.values()) {
			final String name = "speaker." + tier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name, () -> new SpeakerBlock(tier));
			SPEAKER_BLOCKS.put(name, block);
			SPEAKER_ITEMS.put(name, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	public static final RegistryObject<TileEntityType<SpeakerTileEntity>> SPEAKER_TILE =
		TILE_ENTITIES.register("speaker",
			() -> TileEntityType.Builder.of(SpeakerTileEntity::new,
				SPEAKER_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	// ===== FORCE FIELDS =====

	public static final Map<String, RegistryObject<Block>> FORCE_FIELD_PROJECTOR_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_PROJECTOR_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> FORCE_FIELD_RELAY_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_RELAY_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> FORCE_FIELD_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> FORCE_FIELD_ITEMS = new LinkedHashMap<>();

	static {
		for (final ForceFieldTier tier : ForceFieldTier.values()) {
			final String tierName = tier.getName();
			final String projectorName = "projector." + tierName;
			final RegistryObject<Block> projector = BLOCKS.register(projectorName,
				() -> new ForceFieldProjectorBlock(tier));
			FORCE_FIELD_PROJECTOR_BLOCKS.put(tierName, projector);
			FORCE_FIELD_PROJECTOR_ITEMS.put(tierName, ITEMS.register(projectorName,
				() -> new CatalogVariantBlockItem(projector.get(), WarpDriveItemGroup.MAIN,
					2, "single", "double")));

			final String relayName = "force_field_relay." + tierName;
			final RegistryObject<Block> relay = BLOCKS.register(relayName,
				() -> new ForceFieldRelayBlock(tier));
			FORCE_FIELD_RELAY_BLOCKS.put(tierName, relay);
			FORCE_FIELD_RELAY_ITEMS.put(tierName, ITEMS.register(relayName,
				() -> new BlockItem(relay.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));

			final String fieldName = "force_field." + tierName;
			final RegistryObject<Block> field = BLOCKS.register(fieldName,
				() -> new ForceFieldBlock(tier));
			FORCE_FIELD_BLOCKS.put(tierName, field);
			// Kept registered for world/save compatibility, but projected blocks stay out of creative.
			FORCE_FIELD_ITEMS.put(tierName, ITEMS.register(fieldName,
				() -> new BlockItem(field.get(), new Item.Properties())));
		}
	}

	public static final RegistryObject<TileEntityType<ForceFieldProjectorTileEntity>> FORCE_FIELD_PROJECTOR_TILE =
		TILE_ENTITIES.register("force_field_projector",
			() -> TileEntityType.Builder.of(ForceFieldProjectorTileEntity::new,
				FORCE_FIELD_PROJECTOR_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	public static final RegistryObject<TileEntityType<ForceFieldRelayTileEntity>> FORCE_FIELD_RELAY_TILE =
		TILE_ENTITIES.register("force_field_relay",
			() -> TileEntityType.Builder.of(ForceFieldRelayTileEntity::new,
				FORCE_FIELD_RELAY_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

	public static final RegistryObject<TileEntityType<ForceFieldTileEntity>> FORCE_FIELD_TILE =
		TILE_ENTITIES.register("force_field",
			() -> TileEntityType.Builder.of(ForceFieldTileEntity::new,
				FORCE_FIELD_BLOCKS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

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
	public static final RegistryObject<SoundEvent> SOUND_CLOAK = sound("cloak");
	public static final RegistryObject<SoundEvent> SOUND_DECLOAK = sound("decloak");
	public static final RegistryObject<SoundEvent> SOUND_COLLISION = sound("collision_medium");
	public static final RegistryObject<SoundEvent> SOUND_LASER_LOW = sound("lowlaser");
	public static final RegistryObject<SoundEvent> SOUND_LASER_MEDIUM = sound("midlaser");
	public static final RegistryObject<SoundEvent> SOUND_LASER_HIGH = sound("hilaser");
	public static final RegistryObject<SoundEvent> SOUND_SIREN_INDUSTRIAL = sound("siren_industrial");
	public static final RegistryObject<SoundEvent> SOUND_SIREN_RAID = sound("siren_raid");
	public static final RegistryObject<SoundEvent> SOUND_PROJECTING = sound("projecting");

	// ===== BLOCKS =====

	public static final RegistryObject<Block> SHIP_CORE_BLOCK = BLOCKS.register("ship_core",
		ShipCoreBlock::new);
	public static final RegistryObject<Block> SHIP_CONTROLLER_BLOCK = BLOCKS.register("ship_controller",
		ShipControllerBlock::new);
	public static final Map<String, RegistryObject<Block>> SHIP_CORE_TIER_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> SHIP_CORE_TIER_ITEMS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Block>> SHIP_CONTROLLER_TIER_BLOCKS = new LinkedHashMap<>();
	public static final Map<String, RegistryObject<Item>> SHIP_CONTROLLER_TIER_ITEMS = new LinkedHashMap<>();

	// ===== ITEMS (BlockItems) =====

	public static final RegistryObject<Item> SHIP_CORE_ITEM = ITEMS.register("ship_core",
		() -> new BlockItem(SHIP_CORE_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
	public static final RegistryObject<Item> SHIP_CONTROLLER_ITEM = ITEMS.register("ship_controller",
		() -> new BlockItem(SHIP_CONTROLLER_BLOCK.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN)));

	static {
		for (final ShipCoreTier tier : ShipCoreTier.values()) {
			final String coreName = "ship_core." + tier.getName();
			final RegistryObject<Block> core = BLOCKS.register(coreName, () -> new ShipCoreBlock(tier));
			SHIP_CORE_TIER_BLOCKS.put(coreName, core);
			SHIP_CORE_TIER_ITEMS.put(coreName, ITEMS.register(coreName,
				() -> new BlockItem(core.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));

			final String controllerName = "ship_controller." + tier.getName();
			final RegistryObject<Block> controller = BLOCKS.register(controllerName,
				() -> new ShipControllerBlock(tier));
			SHIP_CONTROLLER_TIER_BLOCKS.put(controllerName, controller);
			SHIP_CONTROLLER_TIER_ITEMS.put(controllerName, ITEMS.register(controllerName,
				() -> new BlockItem(controller.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
	}

	// ===== TILE ENTITIES =====

	public static final RegistryObject<TileEntityType<ShipCoreTileEntity>> SHIP_CORE_TILE =
		TILE_ENTITIES.register("ship_core",
			() -> TileEntityType.Builder.of(ShipCoreTileEntity::new,
				java.util.stream.Stream.concat(java.util.stream.Stream.of(SHIP_CORE_BLOCK.get()),
					SHIP_CORE_TIER_BLOCKS.values().stream().map(RegistryObject::get))
					.toArray(Block[]::new)).build(null));
	public static final RegistryObject<TileEntityType<ShipControllerTileEntity>> SHIP_CONTROLLER_TILE =
		TILE_ENTITIES.register("ship_controller",
			() -> TileEntityType.Builder.of(ShipControllerTileEntity::new,
				java.util.stream.Stream.concat(java.util.stream.Stream.of(SHIP_CONTROLLER_BLOCK.get()),
					SHIP_CONTROLLER_TIER_BLOCKS.values().stream().map(RegistryObject::get))
					.toArray(Block[]::new)).build(null));

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
	public static final Map<String, RegistryObject<Block>> CHILLER_BLOCKS = new LinkedHashMap<>();
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
	public static final RegistryObject<Block> ACCELERATOR_CONTROL_POINT_BLOCK;
	public static final RegistryObject<Item> ACCELERATOR_CONTROL_POINT_ITEM;
	public static final RegistryObject<Block> ACCELERATOR_CORE_BLOCK;
	public static final RegistryObject<Item> ACCELERATOR_CORE_ITEM;
	public static final RegistryObject<TileEntityType<AcceleratorCoreTileEntity>> ACCELERATOR_CORE_TILE;
	public static final RegistryObject<Block> PARTICLES_INJECTOR_BLOCK;
	public static final RegistryObject<Item> PARTICLES_INJECTOR_ITEM;
	public static final RegistryObject<Block> PARTICLES_COLLIDER_BLOCK;
	public static final RegistryObject<Item> PARTICLES_COLLIDER_ITEM;
	public static final RegistryObject<TileEntityType<AcceleratorControlPointTileEntity>> ACCELERATOR_CONTROL_POINT_TILE;
	public static final RegistryObject<TileEntityType<ParticlesInjectorTileEntity>> PARTICLES_INJECTOR_TILE;
	public static final RegistryObject<Block> TRANSPORTER_CONTAINMENT_BLOCK;
	public static final RegistryObject<Item> TRANSPORTER_CONTAINMENT_ITEM;
	public static final RegistryObject<Block> TRANSPORTER_SCANNER_BLOCK;
	public static final RegistryObject<Item> TRANSPORTER_SCANNER_ITEM;
	public static final RegistryObject<Block> TRANSPORTER_CORE_BLOCK;
	public static final RegistryObject<Item> TRANSPORTER_CORE_ITEM;
	public static final RegistryObject<TileEntityType<TransporterCoreTileEntity>> TRANSPORTER_CORE_TILE;
	public static final RegistryObject<Block> TRANSPORTER_BEACON_BLOCK;
	public static final RegistryObject<Item> TRANSPORTER_BEACON_ITEM;
	public static final RegistryObject<TileEntityType<TransporterBeaconTileEntity>> TRANSPORTER_BEACON_TILE;
	public static final Map<ChunkLoaderTier, RegistryObject<Block>> CHUNK_LOADER_BLOCKS =
		new LinkedHashMap<>();
	public static final Map<ChunkLoaderTier, RegistryObject<Item>> CHUNK_LOADER_ITEMS =
		new LinkedHashMap<>();
	public static final RegistryObject<TileEntityType<ChunkLoaderTileEntity>> CHUNK_LOADER_TILE;
	public static final RegistryObject<Block> CLOAKING_COIL_BLOCK;
	public static final RegistryObject<Item> CLOAKING_COIL_ITEM;
	public static final RegistryObject<Block> CLOAKING_CORE_BLOCK;
	public static final RegistryObject<Item> CLOAKING_CORE_ITEM;
	public static final RegistryObject<TileEntityType<CloakingCoreTileEntity>> CLOAKING_CORE_TILE;
	public static final Map<EnanReactorTier, RegistryObject<Block>> ENAN_REACTOR_CORE_BLOCKS =
		new LinkedHashMap<>();
	public static final Map<EnanReactorTier, RegistryObject<Item>> ENAN_REACTOR_CORE_ITEMS =
		new LinkedHashMap<>();
	public static final RegistryObject<TileEntityType<EnanReactorCoreTileEntity>>
		ENAN_REACTOR_CORE_TILE;
	public static final RegistryObject<Block> ENAN_REACTOR_LASER_BLOCK;
	public static final RegistryObject<Item> ENAN_REACTOR_LASER_ITEM;
	public static final RegistryObject<TileEntityType<EnanReactorLaserTileEntity>>
		ENAN_REACTOR_LASER_TILE;

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
		final int variantCount = name.startsWith("projector.") ? 2 : 1;
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
		ACCELERATOR_CONTROL_POINT_BLOCK = BLOCKS.register("accelerator_control_point",
			() -> new AcceleratorControlPointBlock(false));
		ACCELERATOR_CONTROL_POINT_ITEM = ITEMS.register("accelerator_control_point",
			() -> new BlockItem(ACCELERATOR_CONTROL_POINT_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		PARTICLES_INJECTOR_BLOCK = BLOCKS.register("particles_injector",
			() -> new AcceleratorControlPointBlock(true));
		PARTICLES_INJECTOR_ITEM = ITEMS.register("particles_injector",
			() -> new BlockItem(PARTICLES_INJECTOR_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		PARTICLES_COLLIDER_BLOCK = BLOCKS.register("particles_collider", AcceleratorComponentBlock::new);
		PARTICLES_COLLIDER_ITEM = ITEMS.register("particles_collider",
			() -> new BlockItem(PARTICLES_COLLIDER_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		ACCELERATOR_CONTROL_POINT_TILE = TILE_ENTITIES.register("accelerator_control_point",
			() -> TileEntityType.Builder.of(AcceleratorControlPointTileEntity::new,
				ACCELERATOR_CONTROL_POINT_BLOCK.get()).build(null));
		PARTICLES_INJECTOR_TILE = TILE_ENTITIES.register("particles_injector",
			() -> TileEntityType.Builder.of(ParticlesInjectorTileEntity::new,
				PARTICLES_INJECTOR_BLOCK.get()).build(null));
		ACCELERATOR_CORE_BLOCK = BLOCKS.register("accelerator_core", AcceleratorCoreBlock::new);
		ACCELERATOR_CORE_ITEM = ITEMS.register("accelerator_core",
			() -> new BlockItem(ACCELERATOR_CORE_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		ACCELERATOR_CORE_TILE = TILE_ENTITIES.register("accelerator_core",
			() -> TileEntityType.Builder.of(AcceleratorCoreTileEntity::new,
				ACCELERATOR_CORE_BLOCK.get()).build(null));
		TRANSPORTER_CONTAINMENT_BLOCK = BLOCKS.register("transporter_containment",
			TransporterContainmentBlock::new);
		TRANSPORTER_CONTAINMENT_ITEM = ITEMS.register("transporter_containment",
			() -> new BlockItem(TRANSPORTER_CONTAINMENT_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		TRANSPORTER_SCANNER_BLOCK = BLOCKS.register("transporter_scanner", TransporterScannerBlock::new);
		TRANSPORTER_SCANNER_ITEM = ITEMS.register("transporter_scanner",
			() -> new BlockItem(TRANSPORTER_SCANNER_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		TRANSPORTER_CORE_BLOCK = BLOCKS.register("transporter_core", TransporterCoreBlock::new);
		TRANSPORTER_CORE_ITEM = ITEMS.register("transporter_core",
			() -> new BlockItem(TRANSPORTER_CORE_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1)));
		TRANSPORTER_CORE_TILE = TILE_ENTITIES.register("transporter_core",
			() -> TileEntityType.Builder.of(TransporterCoreTileEntity::new,
				TRANSPORTER_CORE_BLOCK.get()).build(null));
		TRANSPORTER_BEACON_BLOCK = BLOCKS.register("transporter_beacon", TransporterBeaconBlock::new);
		TRANSPORTER_BEACON_ITEM = ITEMS.register("transporter_beacon",
			() -> new TransporterBeaconBlockItem(TRANSPORTER_BEACON_BLOCK.get()));
		TRANSPORTER_BEACON_TILE = TILE_ENTITIES.register("transporter_beacon",
			() -> TileEntityType.Builder.of(TransporterBeaconTileEntity::new,
				TRANSPORTER_BEACON_BLOCK.get()).build(null));
		for (final ChunkLoaderTier chunkLoaderTier : ChunkLoaderTier.values()) {
			final String name = "chunk_loader." + chunkLoaderTier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name,
				() -> new ChunkLoaderBlock(chunkLoaderTier));
			CHUNK_LOADER_BLOCKS.put(chunkLoaderTier, block);
			CHUNK_LOADER_ITEMS.put(chunkLoaderTier, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties().tab(WarpDriveItemGroup.MAIN))));
		}
		CHUNK_LOADER_TILE = TILE_ENTITIES.register("chunk_loader",
			() -> TileEntityType.Builder.of(ChunkLoaderTileEntity::new,
				CHUNK_LOADER_BLOCKS.values().stream().map(RegistryObject::get)
					.toArray(Block[]::new)).build(null));
		CLOAKING_COIL_BLOCK = BLOCKS.register("cloaking_coil", CloakingCoilBlock::new);
		CLOAKING_COIL_ITEM = ITEMS.register("cloaking_coil",
			() -> new BlockItem(CLOAKING_COIL_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		CLOAKING_CORE_BLOCK = BLOCKS.register("cloaking_core", CloakingCoreBlock::new);
		CLOAKING_CORE_ITEM = ITEMS.register("cloaking_core",
			() -> new BlockItem(CLOAKING_CORE_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		CLOAKING_CORE_TILE = TILE_ENTITIES.register("cloaking_core",
			() -> TileEntityType.Builder.of(CloakingCoreTileEntity::new,
				CLOAKING_CORE_BLOCK.get()).build(null));
		for (final EnanReactorTier reactorTier : EnanReactorTier.values()) {
			final String name = "enan_reactor_core." + reactorTier.getName();
			final RegistryObject<Block> block = BLOCKS.register(name,
				() -> new EnanReactorCoreBlock(reactorTier));
			ENAN_REACTOR_CORE_BLOCKS.put(reactorTier, block);
			ENAN_REACTOR_CORE_ITEMS.put(reactorTier, ITEMS.register(name,
				() -> new BlockItem(block.get(), new Item.Properties()
					.tab(WarpDriveItemGroup.MAIN))));
		}
		ENAN_REACTOR_CORE_TILE = TILE_ENTITIES.register("enan_reactor_core",
			() -> TileEntityType.Builder.of(EnanReactorCoreTileEntity::new,
				ENAN_REACTOR_CORE_BLOCKS.get(EnanReactorTier.BASIC).get(),
				ENAN_REACTOR_CORE_BLOCKS.get(EnanReactorTier.ADVANCED).get(),
				ENAN_REACTOR_CORE_BLOCKS.get(EnanReactorTier.SUPERIOR).get()).build(null));
		ENAN_REACTOR_LASER_BLOCK = BLOCKS.register("enan_reactor_laser",
			EnanReactorLaserBlock::new);
		ENAN_REACTOR_LASER_ITEM = ITEMS.register("enan_reactor_laser",
			() -> new BlockItem(ENAN_REACTOR_LASER_BLOCK.get(),
				new Item.Properties().tab(WarpDriveItemGroup.MAIN)));
		ENAN_REACTOR_LASER_TILE = TILE_ENTITIES.register("enan_reactor_laser",
			() -> TileEntityType.Builder.of(EnanReactorLaserTileEntity::new,
				ENAN_REACTOR_LASER_BLOCK.get()).build(null));

		final String[] tiers = { "basic", "advanced", "superior" };
		for (int index = 0; index < tiers.length; index++) {
			final String tier = tiers[index];
			final AcceleratorTier acceleratorTier = AcceleratorTier.values()[index];
			final float hardness = 5.0F + index;
			final float resistance = new float[]{ 6.0F, 10.0F, 13.0F }[index];

			final String plainName = "electromagnet." + tier + ".plain";
			ELECTROMAGNET_BLOCKS.put(plainName, registerBlockWithItem(plainName,
				() -> new SimpleBlock(metal(hardness, resistance))));

			final String glassName = "electromagnet." + tier + ".glass";
			ELECTROMAGNET_BLOCKS.put(glassName, registerBlockWithItem(glassName,
				() -> new SimpleGlassBlock(glass(hardness, resistance))));

			final String chillerName = "chiller." + tier;
			CHILLER_BLOCKS.put(chillerName, registerBlockWithItem(chillerName,
				() -> new ChillerBlock(acceleratorTier)));
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

		// Machines still in this list are placeholders. Their legacy art remains placeable and visible
		// in creative/JEI until each registration is replaced by its functional block and tile entity.
		for (final String name : new String[]{
			"ic2_reactor_laser_cooler" }) {
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
		RECIPE_SERIALIZERS.register(modEventBus);

		WarpDrive.logger.info("Deferred registers initialized");
	}
}
