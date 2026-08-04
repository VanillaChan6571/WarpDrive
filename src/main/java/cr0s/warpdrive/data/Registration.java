package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.CreativeEnergyBlock;
import cr0s.warpdrive.block.CreativeEnergyTileEntity;
import cr0s.warpdrive.block.ShipCoreBlock;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.container.CreativeEnergyContainer;
import cr0s.warpdrive.item.WarpArmorItem;
import cr0s.warpdrive.item.WarpArmorMaterial;
import cr0s.warpdrive.world.AsteroidFeature;
import net.minecraft.block.Block;
import net.minecraft.inventory.EquipmentSlotType;
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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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

	// ===== WORLD GENERATION =====

	public static final RegistryObject<Feature<NoFeatureConfig>> ASTEROID_FIELD =
		FEATURES.register("asteroid_field", () -> new AsteroidFeature(NoFeatureConfig.CODEC));

	/**
	 * Configured form of the asteroid field, built during common setup because it must go into
	 * WorldGenRegistries rather than a Forge registry. BiomeLoadingEvent then attaches it to the
	 * space biome.
	 */
	public static ConfiguredFeature<?, ?> ASTEROID_FIELD_CONFIGURED;

	public static void registerConfiguredFeatures() {
		ASTEROID_FIELD_CONFIGURED = ASTEROID_FIELD.get()
			.configured(NoFeatureConfig.INSTANCE)
			.decorated(Placement.CHANCE.configured(new ChanceConfig(6)));
		Registry.register(WorldGenRegistries.CONFIGURED_FEATURE,
			new ResourceLocation(WarpDrive.MODID, "asteroid_field"), ASTEROID_FIELD_CONFIGURED);
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

	// ===== ITEMS (BlockItems) =====

	public static final RegistryObject<Item> SHIP_CORE_ITEM = ITEMS.register("ship_core",
		() -> new BlockItem(SHIP_CORE_BLOCK.get(), new Item.Properties().tab(ItemGroup.TAB_MISC)));

	// ===== TILE ENTITIES =====

	public static final RegistryObject<TileEntityType<ShipCoreTileEntity>> SHIP_CORE_TILE =
		TILE_ENTITIES.register("ship_core",
			() -> TileEntityType.Builder.of(ShipCoreTileEntity::new, SHIP_CORE_BLOCK.get()).build(null));

	// ===== CREATIVE ENERGY SOURCE =====

	public static final RegistryObject<Block> CREATIVE_ENERGY_BLOCK = BLOCKS.register("creative_energy",
		CreativeEnergyBlock::new);

	public static final RegistryObject<Item> CREATIVE_ENERGY_ITEM = ITEMS.register("creative_energy",
		() -> new BlockItem(CREATIVE_ENERGY_BLOCK.get(), new Item.Properties().tab(ItemGroup.TAB_MISC)));

	public static final RegistryObject<TileEntityType<CreativeEnergyTileEntity>> CREATIVE_ENERGY_TILE =
		TILE_ENTITIES.register("creative_energy",
			() -> TileEntityType.Builder.of(CreativeEnergyTileEntity::new, CREATIVE_ENERGY_BLOCK.get()).build(null));

	public static final RegistryObject<ContainerType<CreativeEnergyContainer>> CREATIVE_ENERGY_CONTAINER =
		CONTAINERS.register("creative_energy",
			() -> IForgeContainerType.create(CreativeEnergyContainer::new));

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
