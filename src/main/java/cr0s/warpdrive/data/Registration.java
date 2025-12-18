package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.ShipCoreBlock;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
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

		WarpDrive.logger.info("Deferred registers initialized");
	}
}
