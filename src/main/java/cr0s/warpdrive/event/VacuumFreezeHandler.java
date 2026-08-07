package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.AirClassifier;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Exposed water freezes in vacuum; exposed lava cools to obsidian.
 *
 * Not a 1.12.2 feature - the original explicitly disabled the ice pass in space by overriding
 * canDoRainSnowIce to false, so water simply flowed there. This is new behaviour, added because
 * flowing water in hard vacuum looks wrong once you have air pressure modelled everywhere else.
 *
 * The gate is what makes it work rather than being a nuisance: a fluid only freezes when it is
 * NOT in breathable air. Pressurise a room and its water stays liquid; open that room to space and
 * it locks up. So the air system decides, which means a plumbing run inside a hull behaves normally
 * and only an actual breach freezes anything.
 *
 * Vanilla's own ice pass is unusable for this. It lives in ServerWorld.tickChunk, fires on a 1-in-16
 * roll at the motion-blocking heightmap position only, and is driven by biome temperature - so it
 * would never touch water inside a hull, which is precisely where it matters here.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VacuumFreezeHandler {

	/** How often a loaded chunk is considered, in ticks. */
	private static final int FREEZE_INTERVAL_TICKS = 40;

	/** Positions sampled per chunk per pass. Sparse on purpose - freezing should creep, not snap. */
	private static final int SAMPLES_PER_CHUNK = 3;

	private static final Random RANDOM = new Random();

	private VacuumFreezeHandler() {
	}

	@SubscribeEvent
	public static void onWorldTick(final TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		final World world = event.world;
		if (!(world instanceof ServerWorld) || !ChunkHandler.isSimulated(world)) {
			return;
		}
		if (world.getGameTime() % FREEZE_INTERVAL_TICKS != 0) {
			return;
		}

		final ServerWorld serverWorld = (ServerWorld) world;
		// Only walk chunks that already carry air state: those are the ones near a ship, and the
		// only places a player is likely to have plumbing worth freezing
		for (final ChunkPos chunkPos : ChunkHandler.getTrackedChunkPositions(world)) {
			final IChunk chunk = serverWorld.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
			if (chunk == null) {
				continue;
			}
			for (int sample = 0; sample < SAMPLES_PER_CHUNK; sample++) {
				freezeCandidate(serverWorld, chunkPos);
			}
		}
	}

	private static void freezeCandidate(final ServerWorld world, final ChunkPos chunkPos) {
		final BlockPos blockPos = new BlockPos(
			chunkPos.getMinBlockX() + RANDOM.nextInt(16),
			RANDOM.nextInt(world.getMaxBuildHeight()),
			chunkPos.getMinBlockZ() + RANDOM.nextInt(16));

		final BlockState blockState = world.getBlockState(blockPos);
		final FluidState fluidState = blockState.getFluidState();
		if (fluidState.isEmpty()) {
			return;
		}

		// Only source blocks solidify. Freezing flowing fluid mid-run leaves floating shells where
		// the source keeps feeding a stream that no longer has anywhere to go.
		if (!fluidState.isSource()) {
			return;
		}

		if (isPressurised(world, blockPos)) {
			return;
		}

		final Fluid fluid = fluidState.getType();
		final BlockState frozen;
		if (fluid == Fluids.WATER) {
			frozen = Blocks.ICE.defaultBlockState();
		} else if (fluid == Fluids.LAVA) {
			frozen = Blocks.OBSIDIAN.defaultBlockState();
		} else {
			return;   // leave modded fluids alone rather than guess at their solid form
		}

		world.setBlockAndUpdate(blockPos, frozen);
		DebugLog.log("AIR", "vacuum froze {} at {} {} {}",
			fluid == Fluids.WATER ? "water" : "lava",
			blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	/**
	 * True when breathable air reaches this position.
	 *
	 * Checks the fluid's own neighbours rather than the fluid block itself: a fluid never holds an
	 * air block, so asking about it directly would report every pool as exposed, including one
	 * sitting in the middle of a pressurised room.
	 */
	private static boolean isPressurised(final World world, final BlockPos blockPos) {
		for (final Direction direction : Direction.values()) {
			if (AirClassifier.isAirBlock(world, blockPos.relative(direction))) {
				return true;
			}
		}
		return false;
	}
}
