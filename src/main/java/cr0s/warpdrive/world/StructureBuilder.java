package cr0s.warpdrive.world;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.BlockState;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Builds large structures without stalling the server.
 *
 * The split is deliberate, and follows the one hard constraint: world storage is not thread-safe,
 * so block placement must happen on the server thread. Everything *before* that - sphere maths,
 * ore distribution, building the block list - is pure arithmetic and runs on a worker.
 *
 * Placement is then spread across ticks at BLOCKS_PER_TICK, the same budget 1.12.2's
 * EntitySphereGen used, so a planet materialises over a second rather than freezing a tick.
 *
 * Mods like FAWE go further and write chunk sections directly off-thread, but that means owning
 * lighting, neighbour updates, block entity lifecycle and thread safety against the ticking
 * server, through version-specific internals. Not worth it until measurement says the sliced
 * apply is actually the bottleneck - at which point only applyChunk() below needs replacing.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StructureBuilder {

	/** Same per-tick budget as 1.12.2 EntitySphereGen. */
	private static final int BLOCKS_PER_TICK = 5000;

	private static final Map<RegistryKey<World>, Deque<PendingBuild>> QUEUES = new HashMap<>();

	private StructureBuilder() {
	}

	/** A block to place. Deliberately plain data so it can be produced off-thread. */
	public static final class PlacedBlock {
		public final BlockPos pos;
		public final BlockState state;

		public PlacedBlock(final BlockPos pos, final BlockState state) {
			this.pos = pos;
			this.state = state;
		}
	}

	private static final class PendingBuild {
		private final String label;
		private final CompletableFuture<List<PlacedBlock>> computation;
		private List<PlacedBlock> blocks;
		private int cursor;

		private PendingBuild(final String label, final CompletableFuture<List<PlacedBlock>> computation) {
			this.label = label;
			this.computation = computation;
		}
	}

	/**
	 * Queue a structure. The supplier runs on a background thread and MUST NOT touch the world -
	 * it may only compute positions and states.
	 */
	public static void submit(final ServerWorld world, final String label,
	                          final Supplier<List<PlacedBlock>> computation) {
		final CompletableFuture<List<PlacedBlock>> future =
			CompletableFuture.supplyAsync(computation, Util.backgroundExecutor());
		synchronized (QUEUES) {
			QUEUES.computeIfAbsent(world.dimension(), key -> new ArrayDeque<>())
				.add(new PendingBuild(label, future));
		}
		DebugLog.log("WORLDGEN", "queued structure '{}' in {}", label, world.dimension().location());
	}

	@SubscribeEvent
	public static void onWorldTick(final TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.world.isClientSide) {
			return;
		}
		if (!(event.world instanceof ServerWorld)) {
			return;
		}
		final ServerWorld world = (ServerWorld) event.world;

		final Deque<PendingBuild> queue;
		synchronized (QUEUES) {
			queue = QUEUES.get(world.dimension());
			if (queue == null || queue.isEmpty()) {
				return;
			}
		}

		int budget = BLOCKS_PER_TICK;
		while (budget > 0) {
			final PendingBuild build;
			synchronized (QUEUES) {
				build = queue.peek();
			}
			if (build == null) {
				break;
			}

			// Still being computed on the worker - come back next tick
			if (build.blocks == null) {
				if (!build.computation.isDone()) {
					break;
				}
				try {
					build.blocks = build.computation.join();
				} catch (final Exception exception) {
					WarpDrive.logger.error("Structure '" + build.label + "' failed to compute", exception);
					DebugLog.log("WORLDGEN", "structure '{}' failed: {}", build.label, exception);
					removeHead(queue);
					continue;
				}
				DebugLog.log("WORLDGEN", "structure '{}' computed: {} blocks", build.label, build.blocks.size());
			}

			budget -= applySlice(world, build, budget);

			if (build.cursor >= build.blocks.size()) {
				DebugLog.log("WORLDGEN", "structure '{}' complete ({} blocks)", build.label, build.blocks.size());
				removeHead(queue);
			}
		}
	}

	/** Place up to `budget` blocks; returns how many were consumed. */
	private static int applySlice(final ServerWorld world, final PendingBuild build, final int budget) {
		final int end = Math.min(build.cursor + budget, build.blocks.size());
		final int start = build.cursor;
		for (int index = start; index < end; index++) {
			final PlacedBlock block = build.blocks.get(index);
			// Only fill empty space, so a structure never eats a player's build
			if (world.getBlockState(block.pos).isAir()) {
				world.setBlock(block.pos, block.state, 2);
			}
		}
		build.cursor = end;
		return end - start;
	}

	private static void removeHead(final Deque<PendingBuild> queue) {
		synchronized (QUEUES) {
			queue.poll();
		}
	}

	/**
	 * Convenience: compute a rough sphere off-thread. Useful for moons and planets, which are the
	 * cases big enough to need this at all.
	 */
	public static List<PlacedBlock> sphere(final BlockPos centre, final int radius,
	                                       final java.util.function.BiFunction<BlockPos, Double, BlockState> material) {
		final List<PlacedBlock> blocks = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (distance > radius) {
						continue;
					}
					final BlockPos pos = centre.offset(dx, dy, dz);
					final BlockState state = material.apply(pos, distance / radius);
					if (state != null) {
						blocks.add(new PlacedBlock(pos, state));
					}
				}
			}
		}
		return blocks;
	}
}
