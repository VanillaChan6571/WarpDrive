package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

/** Registers the persistent-ticket bootstrap validation required by Forge 1.16. */
public final class ChunkLoaderTicketManager {

	private ChunkLoaderTicketManager() { }

	public static void register() {
		ForgeChunkManager.setForcedChunkLoadingCallback(WarpDrive.MODID,
			ChunkLoaderTicketManager::validateTickets);
	}

	private static void validateTickets(final net.minecraft.world.server.ServerWorld world,
	                                    final ForgeChunkManager.TicketHelper helper) {
		helper.getBlockTickets().forEach((owner, chunks) -> {
			// A valid loader always owns its own chunk. Without this invariant Forge could never load
			// the tile that gets the opportunity to rebuild or release the rest of its rectangle.
			final long ownerChunk = ChunkPos.asLong(owner.getX() >> 4, owner.getZ() >> 4);
			if (!chunks.getFirst().contains(ownerChunk) && !chunks.getSecond().contains(ownerChunk)) {
				WarpDrive.logger.warn("Discarding malformed chunk-loader tickets at {} in {}",
					owner, world.dimension().location());
				helper.removeAllTickets(owner);
			}
		});
	}
}
