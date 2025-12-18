package cr0s.warpdrive.event;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.data.ForceFieldRegistry;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.OfflineAvatarManager;
import cr0s.warpdrive.data.StateAir;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkEvent.Unload;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;

public class ChunkHandler {
	
	private static final long CHUNK_HANDLER_UNLOADED_CHUNK_MAX_AGE_MS = 30000L;
	
	// persistent properties
	private static final TIntObjectHashMap<TLongObjectHashMap<ChunkData>> registryClient = new TIntObjectHashMap<>(32);
	private static final TIntObjectHashMap<TLongObjectHashMap<ChunkData>> registryServer = new TIntObjectHashMap<>(32);
	
	// computed properties
	public static long delayLogging = 0;
	
	/* event catchers */
	@SubscribeEvent
	public void onLoadWorld(@Nonnull final WorldEvent.Load event) {
		if (event.getWorld().isRemote || event.getWorld().provider.getDimension() == 0) {
			if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
				WarpDrive.logger.info(String.format("%s world %s load.",
				                                    event.getWorld().isRemote ? "Client" : "Server",
				                                    Commons.format(event.getWorld())));
			}
		}
		
		if ( !event.getWorld().isRemote
		  && event.getWorld().provider.getDimension() == 0 ) {
			// load registries
			final String filename = String.format("%s/%s.dat", event.getWorld().getSaveHandler().getWorldDirectory().getPath(), WarpDrive.MODID);
			final NBTTagCompound tagCompound = Commons.readNBTFromFile(filename);
			ForceFieldRegistry.readFromNBT(tagCompound);
			GlobalRegionManager.readFromNBT(tagCompound);
			OfflineAvatarManager.readFromNBT(tagCompound);
			
			// enforce vanilla's WorldBorder diameter consistency
			final WorldBorder worldBorder = event.getWorld().getWorldBorder();
			final double maxWorldBorder = CelestialObjectManager.getMaxWorldBorder(event.getWorld());
			WarpDrive.logger.info(String.format("Checking vanilla WorldBorder size (%.1f m) against celestial map maximum border (%.1f m)",
			                                    worldBorder.getDiameter(), maxWorldBorder ));
			if (worldBorder.getDiameter() < maxWorldBorder) {
				worldBorder.setTransition(maxWorldBorder);
				WarpDrive.logger.warn(String.format("Vanilla WorldBorder size was too small, it has been adjusted to %.1f m!",
				                                    worldBorder.getDiameter() ));
			}
		}
	}
	
	// new chunks aren't loaded
	public static void onGenerated(@Nonnull final World world, final int chunkX, final int chunkZ) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk [%d, %d] generating",
			                                    world.isRemote ? "Client" : "Server",
			                                    Commons.format(world),
			                                    chunkX, chunkZ));
		}
		
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), chunkX, chunkZ, true);
		assert chunkData != null;
		// (world can load a non-generated chunk, or the chunk be regenerated, so we reset only as needed)
		if (!chunkData.isLoaded()) { 
			chunkData.load(new NBTTagCompound(), world);
		}
	}
	
	// (server side only)
	@SubscribeEvent
	public void onLoadChunkData(@Nonnull final ChunkDataEvent.Load event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s loading data (1)", 
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, true);
		assert chunkData != null;
		chunkData.load(event.getData(), event.getWorld());
	}
	
	// (called after data loading, or before a late generation, or on client side) 
	@SubscribeEvent
	public void onLoadChunk(@Nonnull final ChunkEvent.Load event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s loaded (2)",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, true);
		assert chunkData != null;
		if (!chunkData.isLoaded()) {
			chunkData.load(new NBTTagCompound(), event.getWorld());
		}
	}
	/*
	// (server side only)
	@SubscribeEvent
	public void onWatchChunk(ChunkWatchEvent.Watch event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("World %s chunk %s watch by %s",
			                                    Commons.format(event.getPlayer().world),
			                                    event.getChunk(),
			                                    event.getPlayer()));
		}
	}
	/**/
	// (server side only)
	// not called when chunk wasn't changed since last save?
	@SubscribeEvent
	public void onSaveChunkData(@Nonnull final ChunkDataEvent.Save event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s save data",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, false);
		if (chunkData != null) {
			chunkData.save(event.getData());
		} else if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.error(String.format("%s world %s chunk %s is saving data without loading it first!",
			                                     event.getWorld().isRemote ? "Client" : "Server",
			                                     Commons.format(event.getWorld()),
			                                     event.getChunk().getPos()));
		}
	}
	
	// (server side only)
	@SubscribeEvent
	public void onSaveWorld(@Nonnull final WorldEvent.Save event) {
		if (event.getWorld().provider.getDimension() != 0) {
			return;
		}
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s saved.",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld())));
		}
		
		if (event.getWorld().isRemote) {
			return;
		}
		
		// save registries
		final String filename = String.format("%s/%s.dat", event.getWorld().getSaveHandler().getWorldDirectory().getPath(), WarpDrive.MODID);
		final NBTTagCompound tagCompound = new NBTTagCompound();
		ForceFieldRegistry.writeToNBT(tagCompound);
		GlobalRegionManager.writeToNBT(tagCompound);
		OfflineAvatarManager.writeToNBT(tagCompound);
		Commons.writeNBTToFile(filename, tagCompound);
	}
	
	@SubscribeEvent
	public void onUnloadWorld(@Nonnull final WorldEvent.Unload event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s unload",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld())));
		}
		
		// get dimension data
		LocalProfiler.updateCallStat("onUnloadWorld");
		final TIntObjectHashMap<TLongObjectHashMap<ChunkData>> registry = event.getWorld().isRemote ? registryClient : registryServer;
		final TLongObjectHashMap<ChunkData> mapRegistryItems = registry.get(event.getWorld().provider.getDimension());
		if (mapRegistryItems != null) {
			// unload chunks during shutdown
			for (final Object object : mapRegistryItems.values()) {
				final ChunkData chunkData = (ChunkData) object;
				if (chunkData.isLoaded()) {
					chunkData.unload();
				}
			}
		}
		
		// @TODO unload registries
	}
	
	
	// (not called when closing SSP game)
	@SubscribeEvent
	public void onUnloadChunk(@Nonnull final Unload event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s unload",
			                                    event.getWorld().isRemote ? "Client" : "Server",
			                                    Commons.format(event.getWorld()),
			                                    event.getChunk().getPos()));
		}
		
		final ChunkData chunkData = getChunkData(event.getWorld().isRemote, event.getWorld().provider.getDimension(), event.getChunk().x, event.getChunk().z, false);
		if (chunkData != null) {
			chunkData.unload();
		} else if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.error(String.format("%s world %s chunk %s is unloading without loading it first!", 
			                                     event.getWorld().isRemote ? "Client" : "Server",
			                                     Commons.format(event.getWorld()),
			                                     event.getChunk().getPos()));
		}
	}
	/*
	// (not called when closing SSP game)
	// warning: will return invalid world when switching dimensions
	@SubscribeEvent
	public void onUnwatchChunk(ChunkWatchEvent.UnWatch event) {
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			WarpDrive.logger.info(String.format("%s world %s chunk %s unwatch by %s",
			                                    event.player.world.isRemote ? "Client" : "Server",
			                                    Commons.format(event.player.world),
			                                    event.chunk,
			                                    event.player));
		}
	}
	/**/
	@SubscribeEvent
	public void onWorldTick(@Nonnull final WorldTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		updateTick(event.world);
	}
	
	@SuppressWarnings("ConstantConditions")
	public static void onBlockUpdated(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		if (!world.isRemote) {
			final ChunkData chunkData = getChunkData(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
			if (chunkData != null) {
				chunkData.onBlockUpdated(blockPos.getX(), blockPos.getY(), blockPos.getZ());
			} else {
				if (Commons.throttleMe("ChunkHandler block updating in non-loaded chunk")) {
					WarpDrive.logger.error(String.format("%s block updating %s, while chunk isn't loaded!",
					                                     world.isRemote ? "Client" : "Server",
					                                     Commons.format(world, blockPos)));
					Commons.dumpAllThreads();
				}
			}
		}
	}
	
	/* internal access */
	/**
	 * Return null and spam logs if chunk isn't already generated or loaded 
	 */
	public static ChunkData getChunkData(@Nonnull final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), x, y, z);
		if (chunkData == null) {
			if (Commons.throttleMe("ChunkHandler get data from an non-loaded chunk")) {
				WarpDrive.logger.error(String.format("Trying to get data from an non-loaded chunk in %s %s",
				                                     world.isRemote ? "Client" : "Server",
				                                     Commons.format(world, x, y, z)));
				LocalProfiler.printCallStats();
				Commons.dumpAllThreads();
			}
			assert false;
		}
		return chunkData;
	}
	
	/**
	 * Return null if chunk isn't already generated or loaded
	 */
	private static ChunkData getChunkData(final boolean isRemote, final int dimensionId, final int x, final int y, final int z) {
		assert y >= -1 && y <= 256;   // includes 1 block tolerance for mirroring
		return getChunkData(isRemote, dimensionId, x >> 4, z >> 4, false);
	}
	
	@Nullable
	private static ChunkData getChunkData(final boolean isRemote, final int dimensionId, final int xChunk, final int zChunk, final boolean doCreate) {
		// get dimension data
		LocalProfiler.updateCallStat("getChunkData");
		final TIntObjectHashMap<TLongObjectHashMap<ChunkData>> registry = isRemote ? registryClient : registryServer;
		TLongObjectHashMap<ChunkData> mapRegistryItems = registry.get(dimensionId);
		// (lambda expressions are forcing synchronisation, so we don't use them here)
		if (mapRegistryItems == null) {
			if (!doCreate) {
				return null;
			}
			mapRegistryItems = new TLongObjectHashMap<>(2048);
			registry.put(dimensionId, mapRegistryItems);
		}
		// get chunk data
		final long index = ChunkPos.asLong(xChunk, zChunk);
		ChunkData chunkData = mapRegistryItems.get(index);
		// (lambda expressions are forcing synchronisation, so we don't use them here)
		if (chunkData == null) {
			if (!doCreate) {
				if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
					WarpDrive.logger.info(String.format("getChunkData(%s, %d, %d, %d, false) returning null",
					                                     isRemote, dimensionId, xChunk, zChunk));
				}
				return null;
			}
			chunkData = new ChunkData(xChunk, zChunk);
			if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
				WarpDrive.logger.info(String.format("%s world DIM%d chunk %s is being added to the registry",
				                                    isRemote ? "Client" : "Server",
				                                    dimensionId,
				                                    chunkData.getChunkCoords()));
			}
			if (Commons.isSafeThread()) {
				mapRegistryItems.put(index, chunkData);
			} else if (Commons.throttleMe("ChunkHandler added to the registry outside main thread")) {
				WarpDrive.logger.error(String.format("%s world DIM%d chunk %s is being added to the registry outside main thread!",
				                                    isRemote ? "Client" : "Server",
				                                    dimensionId,
				                                    chunkData.getChunkCoords()));
				Commons.dumpAllThreads();
				mapRegistryItems.put(index, chunkData);
			}
		}
		return chunkData;
	}
	
	private static boolean isLoaded(@Nonnull final TLongObjectHashMap<ChunkData> mapRegistryItems, final int xChunk, final int zChunk) {
		// get chunk data
		final long index = ChunkPos.asLong(xChunk, zChunk);
		final ChunkData chunkData = mapRegistryItems.get(index);
		return chunkData != null && chunkData.isLoaded();
	}
	
	/* commons */
	public static boolean isLoaded(@Nonnull final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), x, y, z);
		return chunkData != null && chunkData.isLoaded();
	}
	
	public static boolean isLoaded(@Nonnull final World world, final int xChunk, final int zChunk) {
		final ChunkData chunkData = getChunkData(world.isRemote, world.provider.getDimension(), xChunk, zChunk, false);
		return chunkData != null && chunkData.isLoaded();
	}
	
	/* air handling */
	@Nullable
	public static StateAir getStateAir(@Nonnull final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world, x, y, z);
		if (chunkData == null) {
			// chunk isn't loaded, skip it
			return null;
		}
		try {
			return chunkData.getStateAir(world, x, y, z);
		} catch (final ExceptionChunkNotLoaded exceptionChunkNotLoaded) {
			WarpDrive.logger.warn(String.format("Aborting air evaluation: chunk isn't loaded %s",
			                                    Commons.format(world, x, y, z)));
			return null;
		}
	}
	
	private static void updateTick(@Nonnull final World world) {
		// get dimension data
		LocalProfiler.updateCallStat("updateTick");
		final TIntObjectHashMap<TLongObjectHashMap<ChunkData>> registry = world.isRemote ? registryClient : registryServer;
		final TLongObjectHashMap<ChunkData> mapRegistryItems = registry.get(world.provider.getDimension());
		if (mapRegistryItems == null) {
			return;
		}
		final int[] countLoaded = { 0 };
		final int[] countRemoved = { 0 };
		final long timeForRemoval = System.currentTimeMillis() - CHUNK_HANDLER_UNLOADED_CHUNK_MAX_AGE_MS;
		final long timeForThrottle = System.currentTimeMillis() + 200;
		final long sizeBefore = mapRegistryItems.size();
		final long[] indexCurrent = { 0L };
		
		try {
			mapRegistryItems.retainEntries(new TLongObjectProcedure<ChunkData>() {
				@Override
				public final boolean execute(final long key, final ChunkData chunkData) {
					indexCurrent[0] = key;
					// update loaded chunks, remove old unloaded chunks
					if (chunkData.isLoaded()) {
						countLoaded[0]++;
						if (System.currentTimeMillis() < timeForThrottle) {
							updateTickLoopStep(world, mapRegistryItems, chunkData);
						}
					} else if (chunkData.timeUnloaded < timeForRemoval) {
						if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
							WarpDrive.logger.info(String.format("%s world %s chunk %s is being removed from updateTick (size is %d)",
							                                    world.isRemote ? "Client" : "Server",
							                                    Commons.format(world),
							                                    chunkData.getChunkCoords(),
							                                    mapRegistryItems.size()));
						}
						countRemoved[0]++;
						return false;
					}
					return true;
				}
			});
			
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("%s world %s had an exception, maybe some chunks changed outside main thread? (size %d -> %d, loaded %d, removed %d, index 0x%X x %d z %d)",
			                                     world.isRemote ? "Client" : "Server",
			                                     Commons.format(world),
			                                     sizeBefore, mapRegistryItems.size(), countLoaded[0], countRemoved[0],
			                                     indexCurrent[0], indexCurrent[0] & 0xFFFFFFFFL, (indexCurrent[0] >> 32) & 0xFFFFFFFFL));
			LocalProfiler.printCallStats();
		}
		
		if (WarpDriveConfig.LOGGING_CHUNK_HANDLER) {
			if (world.provider.getDimension() == 0) {
				delayLogging = (delayLogging + 1) % 4096;
			}
			if (delayLogging == 1) {
				WarpDrive.logger.info(String.format("Dimension %d has %d / %d chunks loaded",
				                                    world.provider.getDimension(),
				                                    countLoaded[0],
				                                    mapRegistryItems.size()));
			}
		}
	}
	
	// apparently, the GC triggers sooner when using sub-function here?
	private static void updateTickLoopStep(@Nonnull final World world, @Nonnull final TLongObjectHashMap<ChunkData> mapRegistryItems, @Nonnull final ChunkData chunkData) {
		final ChunkPos chunkCoordIntPair = chunkData.getChunkCoords();
		// skip empty chunks (faster and more frequent)
		// ship chunk with unloaded neighbours
		if ( chunkData.isNotEmpty()
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x + 1, chunkCoordIntPair.z)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x - 1, chunkCoordIntPair.z)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x, chunkCoordIntPair.z + 1)
		  && isLoaded(mapRegistryItems, chunkCoordIntPair.x, chunkCoordIntPair.z - 1) ) {
			chunkData.updateTick(world);
		}
	}
}