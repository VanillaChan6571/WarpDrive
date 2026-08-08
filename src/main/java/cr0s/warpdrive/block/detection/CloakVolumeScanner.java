package cr0s.warpdrive.block.detection;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.world.server.ServerWorld;

/** Incremental field-volume scan, bounded so a large cloak never stalls one server tick. */
final class CloakVolumeScanner {

	private final BlockPos min;
	private final BlockPos max;
	private final boolean fullyTransparent;
	private int x;
	private int y;
	private int z;
	private int nonAirBlocks;
	private boolean finished;

	CloakVolumeScanner(final BlockPos min, final BlockPos max, final boolean fullyTransparent) {
		this.min = min.immutable();
		this.max = max.immutable();
		this.fullyTransparent = fullyTransparent;
		x = min.getX();
		y = min.getY();
		z = min.getZ();
	}

	boolean scan(final ServerWorld world, final int budget) {
		if (finished) return true;
		final BlockPos.Mutable cursor = new BlockPos.Mutable();
		for (int scanned = 0; scanned < budget && !finished; scanned++) {
			cursor.set(x, y, z);
			if (world.hasChunkAt(cursor)) {
				// Tier 1 follows isAir so replaceable mod gases do not cost power. Tier 2 mirrors
				// the legacy stricter test and pays for every non-vanilla-air block.
				if (fullyTransparent
					? world.getBlockState(cursor).getBlock() != Blocks.AIR
					: !world.getBlockState(cursor).isAir()) nonAirBlocks++;
			}
			advance();
		}
		return finished;
	}

	private void advance() {
		if (++y <= max.getY()) return;
		y = min.getY();
		if (++x <= max.getX()) return;
		x = min.getX();
		if (++z <= max.getZ()) return;
		finished = true;
	}

	int getNonAirBlocks() { return nonAirBlocks; }
}
