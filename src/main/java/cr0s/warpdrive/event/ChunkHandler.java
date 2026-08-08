package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.AirSpreader;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.block.energy.EnanReactorCoreTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the per-chunk air state: lifecycle, persistence and the simulation tick.
 *
 * Ported from 1.12.2, keeping its static registry keyed by dimension and chunk position rather than
 * moving to a chunk capability. The registry is what StateAir reaches into from deep inside the
 * propagation, and a capability lookup there would be both slower and more awkward.
 *
 * The map is concurrent because 1.16.5 loads and saves chunks off the server thread - the event
 * that populates an entry may well not be the thread that later reads it. 1.12.2 could assume a
 * single thread here and did; that assumption is the one thing about this class that does not
 * survive the version gap.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkHandler {

	/** Air is only simulated in dimensions where it can actually be lost. */
	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	/** How many chunks to advance per server tick, to bound the cost in a busy world. */
	private static final int CHUNKS_PER_TICK = 64;

	private static final Map<RegistryKey<World>, Map<Long, ChunkData>> REGISTRY = new ConcurrentHashMap<>();

	private ChunkHandler() {
	}

	// ===== lookup =====

	private static Map<Long, ChunkData> registryFor(final World world) {
		return REGISTRY.computeIfAbsent(world.dimension(), key -> new ConcurrentHashMap<>());
	}

	/**
	 * Air state for the chunk containing this position, or null when that chunk is not loaded.
	 *
	 * Returning null rather than creating on demand is deliberate: it is how the simulation learns
	 * to stop at the edge of loaded space instead of dragging chunks into memory behind it.
	 */
	@Nullable
	public static ChunkData getChunkData(final World world, final int x, final int z) {
		if (!isSimulated(world)) {
			return null;
		}
		final long key = ChunkPos.asLong(x >> 4, z >> 4);
		final ChunkData chunkData = registryFor(world).get(key);
		return chunkData != null && chunkData.isLoaded() ? chunkData : null;
	}

	/** Tell the air state that a block changed, so its classification is re-read promptly. */
	public static void onBlockUpdated(final World world, final int x, final int y, final int z) {
		final ChunkData chunkData = getChunkData(world, x, z);
		if (chunkData != null) {
			chunkData.onBlockUpdated(x, y, z);
		}
	}

	public static boolean isSimulated(final World world) {
		if (world == null || world.isClientSide) {
			return false;
		}
		final String dimension = world.dimension().location().toString();
		return SPACE.equals(dimension) || HYPERSPACE.equals(dimension);
	}

	// ===== lifecycle =====

	/**
	 * Air state read from disk but not yet attached to a world.
	 *
	 * ChunkDataEvent.Load is fired during deserialisation, before the chunk belongs to anything -
	 * its only constructor takes no world, so getWorld() is null and the dimension cannot be known
	 * yet. The NBT is therefore parked here and claimed by ChunkEvent.Load, which does have a world.
	 *
	 * Keyed on the chunk instance rather than its position: the same coordinates exist in every
	 * dimension, and two of ours are simulated. A weak map means an entry that is never claimed -
	 * a chunk that fails to finish loading - is collected rather than leaked.
	 */
	private static final Map<IChunk, CompoundNBT> PENDING_LOAD =
		Collections.synchronizedMap(new WeakHashMap<>());

	@SubscribeEvent
	public static void onChunkDataLoad(final ChunkDataEvent.Load event) {
		final CompoundNBT data = event.getData();
		// Only park chunks that actually carry our data; most will not
		if (data != null && data.contains(WarpDrive.MODID)) {
			PENDING_LOAD.put(event.getChunk(), data);
		}
	}

	@SubscribeEvent
	public static void onChunkLoad(final ChunkEvent.Load event) {
		final IChunk chunk = event.getChunk();
		final CompoundNBT pending = PENDING_LOAD.remove(chunk);

		final World world = asWorld(event);
		if (!isSimulated(world)) {
			return;
		}
		final ChunkPos chunkPos = chunk.getPos();
		final ChunkData chunkData = new ChunkData(chunkPos.x, chunkPos.z);
		// Saved state if we have it, defaults otherwise - a freshly generated chunk has no NBT
		chunkData.load(pending != null ? pending : new CompoundNBT());
		registryFor(world).put(ChunkPos.asLong(chunkPos.x, chunkPos.z), chunkData);
	}

	@SubscribeEvent
	public static void onChunkDataSave(final ChunkDataEvent.Save event) {
		final World world = asWorld(event);
		if (!isSimulated(world)) {
			return;
		}
		final ChunkPos chunkPos = event.getChunk().getPos();
		final ChunkData chunkData = registryFor(world).get(ChunkPos.asLong(chunkPos.x, chunkPos.z));
		if (chunkData != null && chunkData.isLoaded()) {
			chunkData.save(event.getData());
		}
	}

	@SubscribeEvent
	public static void onChunkUnload(final ChunkEvent.Unload event) {
		final World world = asWorld(event);
		if (!isSimulated(world)) {
			return;
		}
		final ChunkPos chunkPos = event.getChunk().getPos();
		registryFor(world).remove(ChunkPos.asLong(chunkPos.x, chunkPos.z));
	}

	/** Drop everything for a world when it closes, so a server restart does not leak state. */
	@SubscribeEvent
	public static void onWorldUnload(final WorldEvent.Unload event) {
		if (event.getWorld() instanceof World) {
			REGISTRY.remove(((World) event.getWorld()).dimension());
		}
	}

	@Nullable
	private static World asWorld(final ChunkEvent event) {
		return event.getWorld() instanceof World ? (World) event.getWorld() : null;
	}

	@Nullable
	private static World asWorld(final ChunkDataEvent event) {
		return event.getWorld() instanceof World ? (World) event.getWorld() : null;
	}

	// ===== block changes =====
	// Matches 1.12.2's coverage. Note this is an accelerator, not a correctness requirement: the
	// simulation re-reads the centre block's classification on every pass anyway, so a door opening
	// still vents the room on its next scheduled tick even though no event fires for it.

	@SubscribeEvent
	public static void onBlockBreak(final BlockEvent.BreakEvent event) {
		notifyBlockChanged(event.getWorld(), event.getPos());
	}

	@SubscribeEvent
	public static void onBlockPlace(final BlockEvent.EntityPlaceEvent event) {
		notifyBlockChanged(event.getWorld(), event.getPos());
	}

	@SubscribeEvent
	public static void onMultiPlace(final BlockEvent.EntityMultiPlaceEvent event) {
		for (final BlockSnapshot blockSnapshot : event.getReplacedBlockSnapshots()) {
			notifyBlockChanged(event.getWorld(), blockSnapshot.getPos());
		}
	}

	private static void notifyBlockChanged(final IWorld iWorld, final BlockPos blockPos) {
		if (iWorld instanceof World && blockPos != null) {
			final World world = (World) iWorld;
			onBlockUpdated(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
			EnanReactorCoreTileEntity.notifyBlockChanged(world, blockPos);
		}
	}

	// ===== simulation =====

	@SubscribeEvent
	public static void onWorldTick(final TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		final World world = event.world;
		if (!(world instanceof ServerWorld) || !isSimulated(world)) {
			return;
		}

		final Map<Long, ChunkData> registry = registryFor(world);
		if (registry.isEmpty()) {
			return;
		}

		for (final ChunkData chunkData : registry.values()) {
			if (chunkData.isLoaded()) {
				chunkData.updateTick(world);
			}
		}

		// Cached chunk references inside the reusable StateAir cursors must not survive the tick,
		// or a chunk unload leaves them pointing at freed data
		AirSpreader.clearCache();
	}

	/**
	 * Chunk positions currently carrying air state in this world.
	 *
	 * Iterating these rather than every loaded chunk keeps sweeps bounded to where ships actually
	 * are, which in space is a vanishingly small fraction of what is loaded.
	 */
	public static Iterable<ChunkPos> getTrackedChunkPositions(final World world) {
		final Map<Long, ChunkData> registry = REGISTRY.get(world.dimension());
		if (registry == null) {
			return java.util.Collections.emptyList();
		}
		final java.util.List<ChunkPos> positions = new java.util.ArrayList<>(registry.size());
		for (final ChunkData chunkData : registry.values()) {
			positions.add(chunkData.getChunkPos());
		}
		return positions;
	}

	/** How many chunks currently carry air state in this world - for diagnostics. */
	public static int getTrackedChunkCount(final World world) {
		final Map<Long, ChunkData> registry = REGISTRY.get(world.dimension());
		return registry == null ? 0 : registry.size();
	}
}
