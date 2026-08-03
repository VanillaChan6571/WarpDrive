package cr0s.warpdrive.ship;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Ship Scanner - Scans connected blocks starting from Ship Core
 * Uses flood-fill algorithm to detect ship structure
 */
public class ShipScanner {

    // Configuration
    private static final int MAX_SHIP_SIZE = 10000; // Max blocks per ship
    private static final int MAX_SCAN_RADIUS = 128; // Max distance from core

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
        Queue<BlockPos> toScan = new LinkedList<>();
        List<ShipBlock> shipBlocks = new ArrayList<>();

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
            for (BlockPos neighbor : getNeighbors(pos)) {
                if (!scanned.contains(neighbor)) {
                    scanned.add(neighbor);
                    toScan.add(neighbor);
                }
            }
        }

        boolean success = shipBlocks.size() > 0 && shipBlocks.size() < MAX_SHIP_SIZE;
        String message;
        if (shipBlocks.size() == 0) {
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
            corePos
        );
    }

    /**
     * Get 6 neighboring positions (cardinal directions)
     */
    private static List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(
            pos.above(),    // +Y
            pos.below(),    // -Y
            pos.north(),    // -Z
            pos.south(),    // +Z
            pos.west(),     // -X
            pos.east()      // +X
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

        public ShipScanResult(boolean success, String message, List<ShipBlock> blocks,
                              int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                              BlockPos corePos) {
            this.success = success;
            this.message = message;
            this.blocks = blocks;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.corePos = corePos;
        }

        public int getVolume() {
            return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }

        public int getBlockCount() {
            return blocks.size();
        }
    }

    /**
     * Represents a single block in the ship structure
     */
    public static class ShipBlock {
        public final BlockPos pos;
        public final BlockState state;
        public final CompoundNBT tileEntityNBT; // Saved NBT data (null if no TE)

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
    }
}
