package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

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

	// TODO: Add more registries as needed in later phases:
	// - ENTITIES (for EntityLaserExploder, EntityNPC, etc.)
	// - SOUND_EVENTS
	// - POTIONS
	// - ENCHANTMENTS
	// - BIOMES
	// - DIMENSIONS

	/**
	 * Call this during mod construction to register all deferred registers
	 */
	public static void init() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		TILE_ENTITIES.register(modEventBus);

		WarpDrive.logger.info("Deferred registers initialized");
	}
}
