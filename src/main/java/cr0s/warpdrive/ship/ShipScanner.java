package cr0s.warpdrive.ship;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.detection.SecurityStationTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveTags;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Ship Scanner - Scans connected blocks starting from Ship Core
 * Uses flood-fill algorithm to detect ship structure
 */
public class ShipScanner {

    // Configuration
    public static final int MAX_SHIP_SIZE = 10000; // Max blocks per ship
    public static final int MAX_SHIP_SIDE = 127; // Legacy creative-tier maximum, excluding the core block
    private static final int MAX_SCAN_RADIUS = 128; // Max distance from core

    private ShipScanner() {
    }

    /**
     * Inspect a configured ship envelope without retaining block states or block-entity NBT.
     * Intended for status screens and preliminary validation.
     */
    public static ShipInspection inspectShip(@Nonnull final World world,
                                              @Nonnull final BlockPos corePos,
                                              @Nonnull final int[] bounds) {
        final BoxScan scan = scanBox(world, corePos, bounds, false);
        return new ShipInspection(scan.success, scan.message, scan.blockCount, scan.envelopeVolume,
            scan.securityStationPos);
    }

    /**
     * Capture the exact immutable snapshot which will be handed to the movement engine. This is
     * deliberately performed on the server thread immediately before movement, after countdown.
     */
    public static ShipScanResult captureShip(@Nonnull final World world,
                                              @Nonnull final BlockPos corePos,
                                              @Nonnull final int[] bounds) {
		return captureShip(world, corePos, bounds, MAX_SHIP_SIZE);
	}

	/** Capture for a caller such as a higher-tier schematic scanner with its own mass ceiling. */
	public static ShipScanResult captureShip(@Nonnull final World world,
	                                         @Nonnull final BlockPos corePos,
	                                         @Nonnull final int[] bounds,
	                                         final int maxBlocks) {
        if (bounds.length != 6) {
            return new ShipScanResult(false, "Invalid ship bounds", Collections.emptyList(),
                0, 0, 0, 0, 0, 0, corePos, 0);
        }
        final BoxScan scan = scanBox(world, corePos, bounds, true, Math.max(1, maxBlocks));
        return new ShipScanResult(scan.success, scan.message,
            scan.success ? scan.blocks : Collections.emptyList(),
            bounds[0], bounds[3], bounds[1], bounds[4], bounds[2], bounds[5], corePos,
            scan.blockCount);
    }

    private static BoxScan scanBox(@Nonnull final World world,
                                   @Nonnull final BlockPos corePos,
                                   @Nonnull final int[] bounds,
                                   final boolean captureBlocks) {
		return scanBox(world, corePos, bounds, captureBlocks, MAX_SHIP_SIZE);
	}

	private static BoxScan scanBox(@Nonnull final World world,
	                               @Nonnull final BlockPos corePos,
	                               @Nonnull final int[] bounds,
	                               final boolean captureBlocks,
	                               final int maxBlocks) {
        if (bounds.length != 6) {
            return BoxScan.failure("Invalid ship bounds", 0);
        }

        final int minX = bounds[0];
        final int minY = bounds[1];
        final int minZ = bounds[2];
        final int maxX = bounds[3];
        final int maxY = bounds[4];
        final int maxZ = bounds[5];
        final long sizeX = (long) maxX - minX + 1L;
        final long sizeY = (long) maxY - minY + 1L;
        final long sizeZ = (long) maxZ - minZ + 1L;
        if (sizeX <= 0L || sizeY <= 0L || sizeZ <= 0L) {
            return BoxScan.failure("Invalid ship bounds", 0);
        }
        if (sizeX > MAX_SHIP_SIDE + 1L
         || sizeY > MAX_SHIP_SIDE + 1L
         || sizeZ > MAX_SHIP_SIDE + 1L) {
            return BoxScan.failure(String.format(
                "Ship exceeds the maximum configured axis span of %d blocks", MAX_SHIP_SIDE), 0);
        }
        final long envelope = sizeX * sizeY * sizeZ;
        if (envelope <= 1L || envelope > Integer.MAX_VALUE) {
            return BoxScan.failure(envelope <= 1L
                ? "Dimensions not set"
                : "Ship envelope is too large", safeInt(envelope));
        }

        if (!world.isInWorldBounds(new BlockPos(minX, minY, minZ))
         || !world.isInWorldBounds(new BlockPos(maxX, maxY, maxZ))) {
            return BoxScan.failure("Ship extends outside the world bounds", safeInt(envelope));
        }
        if (world.getBlockState(corePos).isAir()) {
            return BoxScan.failure("Ship Core is missing from its configured position", safeInt(envelope));
        }

        final List<ShipBlock> blocks = captureBlocks ? new ArrayList<>() : Collections.emptyList();
        final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		int movableBlockCount = 0;
		int blockCount = 0;
        String anchorFailure = null;
        BlockPos securityStationPos = null;
        double securityStationDistance = Double.MAX_VALUE;

        // Chunk-major order avoids bouncing between chunk lookups for every neighbouring X/Z.
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            final int fromX = Math.max(minX, chunkX << 4);
            final int toX = Math.min(maxX, (chunkX << 4) + 15);
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                final int fromZ = Math.max(minZ, chunkZ << 4);
                final int toZ = Math.min(maxZ, (chunkZ << 4) + 15);
                for (int y = minY; y <= maxY; y++) {
                    for (int x = fromX; x <= toX; x++) {
                        for (int z = fromZ; z <= toZ; z++) {
                            mutablePos.set(x, y, z);
                            final BlockState state = world.getBlockState(mutablePos);
                            if (state.isAir()) {
                                continue;
                            }
							if (state.is(WarpDriveTags.SHIP_LEFT_BEHIND)) {
								continue;
							}

							movableBlockCount++;
							if (movableBlockCount > MAX_SHIP_SIZE) {
								return BoxScan.failure(String.format(
									"Ship contains more than %,d movable blocks", MAX_SHIP_SIZE),
									blockCount, safeInt(envelope));
							}
							if (!state.is(WarpDriveTags.SHIP_NO_MASS)) blockCount++;
                            if (blockCount > maxBlocks) {
                                return BoxScan.failure(String.format(
									"Ship mass exceeds %,d blocks", maxBlocks),
                                    blockCount, safeInt(envelope));
                            }

							if (anchorFailure == null && state.is(WarpDriveTags.SHIP_ANCHORS)) {
                                anchorFailure = String.format("Anchor block %s at %d, %d, %d",
                                    registryName(state), x, y, z);
                            }

                            TileEntity tileEntity = null;
                            if (captureBlocks || state.getBlock() == Registration.SECURITY_STATION_BLOCK.get()) {
                                tileEntity = world.getBlockEntity(mutablePos);
                            }
                            if (tileEntity instanceof SecurityStationTileEntity
                             && ((SecurityStationTileEntity) tileEntity).isEnabled()) {
                                final double distance = mutablePos.distSqr(corePos);
                                if (distance < securityStationDistance) {
                                    securityStationDistance = distance;
                                    securityStationPos = mutablePos.immutable();
                                }
                            }

                            if (captureBlocks && anchorFailure == null) {
                                blocks.add(new ShipBlock(mutablePos, state, tileEntity));
                            }
                        }
                    }
                }
            }
        }

		if (movableBlockCount == 0) {
            return BoxScan.failure("No ship blocks found", safeInt(envelope));
        }
        if (anchorFailure != null) {
            return BoxScan.failure(anchorFailure, blockCount, safeInt(envelope));
        }
		if (captureBlocks) {
			blocks.sort(Comparator.comparingInt(ShipScanner::placementPriority));
		}
		return BoxScan.success(String.format(
			"Valid - mass %,d across %,d movable blocks in a %,d block envelope",
			blockCount, movableBlockCount, envelope), blockCount, safeInt(envelope), blocks,
			securityStationPos);
    }

	/** Placement is earliest-to-latest; source removal walks the resulting list backwards. */
	static int placementPriority(@Nonnull final ShipBlock shipBlock) {
		final BlockState state = shipBlock.state;
		if (state.is(WarpDriveTags.PLACE_EARLIEST)) return 0;
		if (state.is(WarpDriveTags.PLACE_EARLIER)) return 1;
		if (state.is(WarpDriveTags.PLACE_NORMAL)) return 2;
		if (state.is(WarpDriveTags.PLACE_LATER)) return 3;
		if (state.is(WarpDriveTags.PLACE_LATEST)) return 4;
		return shipBlock.tileEntityNBT == null ? 2 : 3;
	}

    private static String registryName(final BlockState state) {
        return state.getBlock().getRegistryName() == null
            ? state.getBlock().getClass().getSimpleName()
            : state.getBlock().getRegistryName().toString();
    }

    private static int safeInt(final long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static final class BoxScan {
        private final boolean success;
        private final String message;
        private final int blockCount;
        private final int envelopeVolume;
        private final List<ShipBlock> blocks;
        @Nullable private final BlockPos securityStationPos;

        private BoxScan(final boolean success, final String message, final int blockCount,
                        final int envelopeVolume, final List<ShipBlock> blocks,
                        @Nullable final BlockPos securityStationPos) {
            this.success = success;
            this.message = message;
            this.blockCount = blockCount;
            this.envelopeVolume = envelopeVolume;
            this.blocks = blocks;
            this.securityStationPos = securityStationPos;
        }

        private static BoxScan success(final String message, final int blockCount,
                                       final int envelopeVolume, final List<ShipBlock> blocks,
                                       @Nullable final BlockPos securityStationPos) {
            return new BoxScan(true, message, blockCount, envelopeVolume, blocks, securityStationPos);
        }

        private static BoxScan failure(final String message, final int envelopeVolume) {
            return failure(message, 0, envelopeVolume);
        }

        private static BoxScan failure(final String message, final int blockCount,
                                       final int envelopeVolume) {
            return new BoxScan(false, message, blockCount, envelopeVolume,
                Collections.emptyList(), null);
        }
    }

    public static final class ShipInspection {
        public final boolean success;
        public final String message;
        public final int blockCount;
        public final int envelopeVolume;
        @Nullable public final BlockPos securityStationPos;

        public ShipInspection(final boolean success, final String message,
                              final int blockCount, final int envelopeVolume) {
            this(success, message, blockCount, envelopeVolume, null);
        }

        public ShipInspection(final boolean success, final String message,
                              final int blockCount, final int envelopeVolume,
                              @Nullable final BlockPos securityStationPos) {
            this.success = success;
            this.message = message;
            this.blockCount = blockCount;
            this.envelopeVolume = envelopeVolume;
            this.securityStationPos = securityStationPos;
        }
    }

    /**
     * Scan ship structure starting from the given position
     *
     * @param world World to scan in
     * @param corePos Ship Core position
     * @return ShipScanResult containing all ship blocks and metadata
     */
    public static ShipScanResult scanShip(@Nonnull World world, @Nonnull BlockPos corePos) {
        WarpDrive.logger.info("Starting ship scan at {}", corePos);

        Set<BlockPos> scanned = new HashSet<>();
        Queue<BlockPos> toScan = new ArrayDeque<>();
		List<ShipBlock> shipBlocks = new ArrayList<>();
		int mass = 0;
		String anchorFailure = null;

        // Start from core
        toScan.add(corePos);
        scanned.add(corePos);

        // Bounding box
        int minX = corePos.getX();
        int maxX = corePos.getX();
        int minY = corePos.getY();
        int maxY = corePos.getY();
        int minZ = corePos.getZ();
        int maxZ = corePos.getZ();

        // Flood-fill algorithm
        while (!toScan.isEmpty() && shipBlocks.size() < MAX_SHIP_SIZE) {
            BlockPos pos = toScan.poll();

            // Check distance from core
            if (pos.distSqr(corePos) > MAX_SCAN_RADIUS * MAX_SCAN_RADIUS) {
                continue;
            }

            // Get block state
            BlockState state = world.getBlockState(pos);

            // Skip air blocks
            if (state.isAir()) {
                continue;
            }
			if (state.is(WarpDriveTags.SHIP_LEFT_BEHIND)) {
				continue;
			}
			if (state.is(WarpDriveTags.SHIP_ANCHORS) && anchorFailure == null) {
				anchorFailure = String.format("Anchor block %s at %d, %d, %d",
					registryName(state), pos.getX(), pos.getY(), pos.getZ());
			}
			if (!state.is(WarpDriveTags.SHIP_NO_MASS)) mass++;

            // Add to ship
            TileEntity te = world.getBlockEntity(pos);
            String blockName = state.getBlock().getRegistryName() != null
                ? state.getBlock().getRegistryName().toString() : "unknown";
            DebugLog.log("SCAN", "ShipScanner at {} type={} hasTileEntity={} te={}",
                pos, blockName, state.hasTileEntity(), te != null);
            shipBlocks.add(new ShipBlock(pos, state, te));

            // Update bounding box
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());

            // Scan neighbors (6 directions)
            for (final Direction direction : Direction.values()) {
                final BlockPos neighbor = pos.relative(direction);
                if (!scanned.contains(neighbor)) {
                    scanned.add(neighbor);
                    toScan.add(neighbor);
                }
            }
        }

		shipBlocks.sort(Comparator.comparingInt(ShipScanner::placementPriority));
		boolean success = !shipBlocks.isEmpty() && shipBlocks.size() < MAX_SHIP_SIZE
			&& anchorFailure == null;
        String message;
		if (anchorFailure != null) {
			message = anchorFailure;
		} else if (shipBlocks.size() == 0) {
            message = "No blocks found";
        } else if (shipBlocks.size() >= MAX_SHIP_SIZE) {
            message = String.format("Ship too large! Max %d blocks", MAX_SHIP_SIZE);
            success = false;
        } else {
            message = String.format("Found %d blocks", shipBlocks.size());
        }

        WarpDrive.logger.info("Ship scan complete: {} ({})", message, shipBlocks.size());

        return new ShipScanResult(
            success,
            message,
            shipBlocks,
            minX, maxX,
            minY, maxY,
            minZ, maxZ,
			corePos,
			mass
        );
    }

    /**
     * Result of a ship scan operation
     */
    public static class ShipScanResult {
        public final boolean success;
        public final String message;
        public final List<ShipBlock> blocks;
        public final int minX, maxX;
        public final int minY, maxY;
        public final int minZ, maxZ;
        public final BlockPos corePos;
		private final int blockCount;

        public ShipScanResult(boolean success, String message, List<ShipBlock> blocks,
                              int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
							  BlockPos corePos, int blockCount) {
            this.success = success;
            this.message = message;
            this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.corePos = corePos.immutable();
			this.blockCount = Math.max(0, blockCount);
        }

        public int getVolume() {
            final long volume = ((long) maxX - minX + 1L)
                              * ((long) maxY - minY + 1L)
                              * ((long) maxZ - minZ + 1L);
            return safeInt(volume);
        }

        public int getBlockCount() {
			return blockCount;
        }
    }

    /**
     * Represents a single block in the ship structure
     */
    public static class ShipBlock {
        public final BlockPos pos;
        public final BlockState state;
        @Nullable
        private final CompoundNBT tileEntityNBT;

        public ShipBlock(BlockPos pos, BlockState state, TileEntity tileEntity) {
            this.pos = pos.immutable();
            this.state = state;

            // Save TileEntity NBT immediately during scan
            if (tileEntity != null) {
                this.tileEntityNBT = new CompoundNBT();
                tileEntity.save(this.tileEntityNBT);
                String blockName = state.getBlock().getRegistryName() != null
                    ? state.getBlock().getRegistryName().toString() : "unknown";
                DebugLog.log("SCAN", "ShipBlock saved NBT for {} at {} keys={}",
                    blockName, pos, this.tileEntityNBT.getAllKeys().size());
            } else {
                this.tileEntityNBT = null;
            }
        }

        /** Return a defensive copy so the authoritative scan snapshot cannot be mutated. */
        @Nullable
        public CompoundNBT copyTileEntityNBT() {
            return tileEntityNBT == null ? null : tileEntityNBT.copy();
        }
    }
}
