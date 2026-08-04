package cr0s.warpdrive.ship;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Warp Engine - Handles actual ship movement
 * Moves ship blocks from current position to destination
 */
public class WarpEngine {

    // CC:Tweaked writes these with exactly this casing. The previous code looked for
    // "computerID"/"label", which never matched any key, so the merge below was dead code and the
    // diagnostics reported computerID=missing for an ID that was present the whole time.
    private static final String CC_KEY_ID = "ComputerId";
    private static final String CC_KEY_LABEL = "Label";

    private static final Rotation[] ROTATIONS = {
        Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90
    };

    /** Rotate a ship-relative offset around the core by the given rotation. */
    private static BlockPos rotateOffset(final BlockPos offset, final Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:        return new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case CLOCKWISE_180:       return new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case COUNTERCLOCKWISE_90: return new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default:                  return offset;
        }
    }

    /** Destination position for a ship block, accounting for rotation about the core. */
    private static BlockPos destinationOf(final BlockPos pos, final ShipScanner.ShipScanResult scan,
                                          final Rotation rotation, final int destX, final int destY, final int destZ) {
        final BlockPos offset = pos.subtract(scan.corePos);
        final BlockPos rotated = rotateOffset(offset, rotation);
        return new BlockPos(destX + rotated.getX(), destY + rotated.getY(), destZ + rotated.getZ());
    }

    /**
     * Execute warp jump - move ship to destination
     *
     * @param world World to operate in
     * @param scanResult Ship structure to move
     * @param destX Destination X coordinate
     * @param destY Destination Y coordinate
     * @param destZ Destination Z coordinate
     * @return Result of the warp operation
     */
    public static WarpResult executeWarp(@Nonnull World world,
                                         @Nonnull ShipScanner.ShipScanResult scanResult,
                                         int destX, int destY, int destZ,
                                         int energyAfterJump) {
        return executeWarp(world, scanResult, destX, destY, destZ, energyAfterJump, 0);
    }

    /**
     * @param rotationSteps quarter turns clockwise applied around the ship core, 0..3
     */
    public static WarpResult executeWarp(@Nonnull World world,
                                         @Nonnull ShipScanner.ShipScanResult scanResult,
                                         int destX, int destY, int destZ,
                                         int energyAfterJump,
                                         int rotationSteps) {
        return executeWarp(world, world, scanResult, destX, destY, destZ, energyAfterJump, rotationSteps);
    }

    /**
     * Cross-dimension capable warp.
     *
     * @param sourceWorld world the ship currently occupies
     * @param destWorld   world it is moving into; may be the same instance
     * @param rotationSteps quarter turns clockwise applied around the ship core, 0..3
     */
    public static WarpResult executeWarp(@Nonnull World sourceWorld,
                                         @Nonnull World destWorld,
                                         @Nonnull ShipScanner.ShipScanResult scanResult,
                                         int destX, int destY, int destZ,
                                         int energyAfterJump,
                                         int rotationSteps) {
        final Rotation rotation = ROTATIONS[((rotationSteps % 4) + 4) % 4];
        final boolean crossWorld = sourceWorld != destWorld;

        DebugLog.log("JUMP", "WarpEngine.executeWarp ENTERED - from {} in {} to {}, {}, {} in {} energyAfterJump={}",
            scanResult.corePos, dimensionOf(sourceWorld), destX, destY, destZ, dimensionOf(destWorld), energyAfterJump);
        WarpDrive.logger.info("Executing warp from {} to {}, {}, {}{}",
            scanResult.corePos, destX, destY, destZ,
            crossWorld ? " (" + dimensionOf(sourceWorld) + " -> " + dimensionOf(destWorld) + ")" : "");


        // Calculate offset
        int offsetX = destX - scanResult.corePos.getX();
        int offsetY = destY - scanResult.corePos.getY();
        int offsetZ = destZ - scanResult.corePos.getZ();

        // Capture up-to-date TileEntity NBT before we move anything (state may have changed after scan)
        Map<BlockPos, CompoundNBT> currentNbtByPos = new HashMap<>();
        DebugLog.log("JUMP", "Starting NBT capture for {} blocks", scanResult.blocks.size());
		for (ShipScanner.ShipBlock shipBlock : scanResult.blocks) {
			TileEntity te = sourceWorld.getBlockEntity(shipBlock.pos);
			String blockName = shipBlock.state.getBlock().getRegistryName() != null
				? shipBlock.state.getBlock().getRegistryName().toString() : "unknown";
			DebugLog.log("JUMP", "Block at {} type={} hasTileEntity={} te={}",
				shipBlock.pos, blockName, shipBlock.state.hasTileEntity(), te != null);

			if (te != null) {
				CompoundNBT nbt = new CompoundNBT();
				te.save(nbt);
				currentNbtByPos.put(shipBlock.pos, nbt);
				DebugLog.log("JUMP", "Saved NBT for {} at {} keys={}",
					blockName, shipBlock.pos, nbt.getAllKeys().size());
				maybeLogCCComputer("save", shipBlock.state, shipBlock.pos, nbt);
				logTileEntityState("save", shipBlock.state, shipBlock.pos, nbt);
			} else if (shipBlock.tileEntityNBT != null) {
				CompoundNBT nbt = shipBlock.tileEntityNBT.copy();
				currentNbtByPos.put(shipBlock.pos, nbt);
				DebugLog.log("JUMP", "Saved fallback (scan) NBT for {} at {} keys={}",
					blockName, shipBlock.pos, nbt.getAllKeys().size());
				maybeLogCCComputer("save-fallback", shipBlock.state, shipBlock.pos, nbt);
				logTileEntityState("save-fallback", shipBlock.state, shipBlock.pos, nbt);
			}
		}
        DebugLog.log("JUMP", "NBT capture complete: {} TileEntities saved", currentNbtByPos.size());

        // Phase 1: Detect entities on ship
        WarpDrive.logger.info("Phase 1: Detecting entities on ship...");
        AxisAlignedBB shipBounds = new AxisAlignedBB(
            scanResult.minX, scanResult.minY, scanResult.minZ,
            scanResult.maxX + 1, scanResult.maxY + 1, scanResult.maxZ + 1
        );
        List<Entity> entities = sourceWorld.getEntities((Entity) null, shipBounds);
        WarpDrive.logger.info("Found {} entities on ship", entities.size());

        // Phase 2: Check destination collision
        WarpDrive.logger.info("Phase 2: Checking destination collision...");
        final Set<BlockPos> sourcePositions = new HashSet<>();
        for (ShipScanner.ShipBlock shipBlock : scanResult.blocks) {
            sourcePositions.add(shipBlock.pos);
        }
        for (ShipScanner.ShipBlock shipBlock : scanResult.blocks) {
            BlockPos newPos = destinationOf(shipBlock.pos, scanResult, rotation, destX, destY, destZ);

            // Check world bounds
            if (!destWorld.isInWorldBounds(newPos)) {
                WarpDrive.logger.warn("Warp failed: destination out of world bounds");
                return new WarpResult(false, "Destination out of world bounds");
            }

            // Check for collision (if block at destination is not part of ship)
            // Across worlds the ship vacates nothing at the destination, so there is no exemption
            BlockState destState = destWorld.getBlockState(newPos);
            if (!destState.isAir() && (crossWorld || !isVacatedByShip(newPos, sourcePositions))) {
                WarpDrive.logger.warn("Warp failed: collision at {}", newPos);
                return new WarpResult(false, String.format("Collision at %d, %d, %d",
                    newPos.getX(), newPos.getY(), newPos.getZ()));
            }
        }

        // Before we mutate the world, prepare the Ship Core NBT with post-warp energy and cleared scan state
        CompoundNBT coreNbt = currentNbtByPos.get(scanResult.corePos);
        if (coreNbt != null) {
            coreNbt.putInt("Energy", Math.max(0, energyAfterJump));
            // ShipScanned/ShipBlocks were written here previously. ShipCoreTileEntity.load() reads
            // neither - those fields disappeared when the API moved to setDimensions/setMovement -
            // so they only showed up as phantom keys in NBT dumps.
            currentNbtByPos.put(scanResult.corePos, coreNbt);
        }

        // Phase 3: Clear original positions.
        // Ordering matters. Within one world the ship can overlap itself on a short move, so the
        // source must be cleared before placing or the move overwrites its own blocks. Across
        // worlds no overlap is possible, so we place first and clear afterwards - if placement
        // fails there, the ship still exists at the origin instead of being destroyed.
        if (!crossWorld) {
            WarpDrive.logger.info("Phase 3: Clearing original positions (same world)...");
            clearSource(sourceWorld, scanResult);
        }

        // Phase 4: Place blocks at new positions
        WarpDrive.logger.info("Phase 4: Placing blocks at destination...");
        for (ShipScanner.ShipBlock shipBlock : scanResult.blocks) {
            BlockPos newPos = destinationOf(shipBlock.pos, scanResult, rotation, destX, destY, destZ);
            String blockName = shipBlock.state.getBlock().getRegistryName() != null
                ? shipBlock.state.getBlock().getRegistryName().toString() : "unknown";

            // Ensure destination chunk is loaded before placing
            destWorld.getChunk(newPos);

            // Rotate the state as well as the position, so stairs/pistons/chests keep facing the
            // same way relative to the ship rather than staying stuck to world axes
            final BlockState newState = shipBlock.state.rotate(rotation);

            // Place block
            destWorld.setBlock(newPos, newState, 3);

            // Ensure the TileEntity exists immediately after placement
            if (newState.hasTileEntity() && destWorld.getBlockEntity(newPos) == null) {
                DebugLog.log("JUMP", "Creating TileEntity for {} at {}", blockName, newPos);
                TileEntity created = newState.getBlock().createTileEntity(newState, destWorld);
                if (created != null) {
                    // Set position and world before insertion
                    created.setLevelAndPosition(destWorld, newPos);
                    destWorld.setBlockEntity(newPos, created);
                    DebugLog.log("JUMP", "TileEntity created: {}", created.getClass().getSimpleName());
                }
            }

            // Restore TileEntity data if present (saved during scan)
            CompoundNBT latestNbt = currentNbtByPos.getOrDefault(shipBlock.pos, shipBlock.tileEntityNBT);
            if (latestNbt != null) {
                DebugLog.log("JUMP", "Restoring NBT for {} at {} (has {} keys)",
                    blockName, newPos, latestNbt.getAllKeys().size());
                TileEntity newTE = destWorld.getBlockEntity(newPos);
                if (newTE == null && newState.hasTileEntity()) {
                    DebugLog.log("JUMP", "Restore: creating TileEntity for {} at {} because it was missing", blockName, newPos);
                    newTE = newState.getBlock().createTileEntity(newState, destWorld);
                    if (newTE != null) {
                        newTE.setLevelAndPosition(destWorld, newPos);
                        destWorld.setBlockEntity(newPos, newTE);
                        DebugLog.log("JUMP", "Restore: created {}", newTE.getClass().getSimpleName());
                    }
                }
                if (newTE != null) {
                    DebugLog.log("JUMP", "Found TileEntity to restore: {}", newTE.getClass().getSimpleName());
                    CompoundNBT nbt = latestNbt.copy();
                    // If this is a ComputerCraft computer, attempt to keep the computerID/label by keeping existing values if present
                    // Safety net only: the saved NBT normally already carries these, since the whole
                    // compound is restored below. This covers the case where it somehow did not.
                    if (maybeLogCCComputer("restore-premerge", shipBlock.state, newPos, nbt)) {
                        CompoundNBT existing = new CompoundNBT();
                        newTE.save(existing);
                        if (existing.contains(CC_KEY_ID) && !nbt.contains(CC_KEY_ID)) {
                            nbt.putInt(CC_KEY_ID, existing.getInt(CC_KEY_ID));
                        }
                        if (existing.contains(CC_KEY_LABEL) && !nbt.contains(CC_KEY_LABEL)) {
                            nbt.putString(CC_KEY_LABEL, existing.getString(CC_KEY_LABEL));
                        }
                    }
                    // Update position in NBT
                    nbt.putInt("x", newPos.getX());
                    nbt.putInt("y", newPos.getY());
                    nbt.putInt("z", newPos.getZ());
                    newTE.load(newState, nbt);
                    newTE.setChanged();
                    maybeLogCCComputer("restore", shipBlock.state, newPos, nbt);
                    logTileEntityState("restore", shipBlock.state, newPos, nbt);
                } else if (newState.hasTileEntity()) {
                    DebugLog.log("JUMP", "Failed to create TileEntity for {} at {}", shipBlock.state.getBlock().getRegistryName(), newPos);
                }
            }
        }

        // Phase 5: Teleport entities
        WarpDrive.logger.info("Phase 5: Teleporting {} entities...", entities.size());
        for (Entity entity : entities) {
            if (entity != null && entity.isAlive()) {
                Vector3d oldPos = entity.position();

                // Rotate the entity's offset from the core the same way the blocks were rotated,
                // otherwise passengers end up outside a rotated ship
                double relX = oldPos.x - (scanResult.corePos.getX() + 0.5);
                double relZ = oldPos.z - (scanResult.corePos.getZ() + 0.5);
                double rotX;
                double rotZ;
                switch (rotation) {
                    case CLOCKWISE_90:        rotX = -relZ; rotZ = relX;  break;
                    case CLOCKWISE_180:       rotX = -relX; rotZ = -relZ; break;
                    case COUNTERCLOCKWISE_90: rotX = relZ;  rotZ = -relX; break;
                    default:                  rotX = relX;  rotZ = relZ;  break;
                }

                double newX = destX + 0.5 + rotX;
                double newY = oldPos.y + (destY - scanResult.corePos.getY());
                double newZ = destZ + 0.5 + rotZ;

                entity.yRot += rotation == Rotation.CLOCKWISE_90 ? 90.0F
                    : rotation == Rotation.CLOCKWISE_180 ? 180.0F
                    : rotation == Rotation.COUNTERCLOCKWISE_90 ? -90.0F : 0.0F;

                if (crossWorld && destWorld instanceof ServerWorld) {
                    transferAcrossWorlds(entity, (ServerWorld) destWorld, newX, newY, newZ);
                } else {
                    entity.teleportTo(newX, newY, newZ);
                }
                WarpDrive.logger.debug("Teleported entity {} to {}, {}, {}",
                    entity.getName().getString(), newX, newY, newZ);
            }
        }

        // Cross-world clears last: until this point the ship still exists at the origin, so a
        // failure above leaves it recoverable rather than deleted.
        if (crossWorld) {
            WarpDrive.logger.info("Phase 6: Clearing origin in {}...", dimensionOf(sourceWorld));
            clearSource(sourceWorld, scanResult);
        }

        WarpDrive.logger.info("Warp completed successfully! Moved {} blocks and {} entities",
            scanResult.blocks.size(), entities.size());
        return new WarpResult(true, String.format("Warp successful! Moved %d blocks, %d entities",
            scanResult.blocks.size(), entities.size()));
    }

    /**
     * A destination cell is safe when it is air, or when it is currently occupied by a block this
     * ship is about to vacate - so the test is membership of the SOURCE footprint.
     *
     * Both previous versions compared against the destination footprint instead (directly, or via
     * an inverse offset), which is trivially true for the block being checked and made collision
     * detection a no-op. Using a set also drops this from O(n) per block to O(1).
     */
    private static boolean isVacatedByShip(final BlockPos pos, final Set<BlockPos> sourcePositions) {
        return sourcePositions.contains(pos);
    }

    /**
     * Move one entity into another world.
     *
     * Players and everything else need different calls. A player keeps its instance - Forge
     * forbids replacing it - and is moved with ServerPlayerEntity.teleportTo(ServerWorld, ...).
     * Any other entity is destroyed and recreated by changeDimension(), which returns the new
     * instance; the original is dead afterwards, so nothing may be done with it.
     *
     * The ITeleporter below deliberately performs no portal search and no block placement: the
     * destination is already decided by the warp, so vanilla's placement logic must not run.
     */
    private static void transferAcrossWorlds(final Entity entity, final ServerWorld destWorld,
                                             final double x, final double y, final double z) {
        if (entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) entity).teleportTo(destWorld, x, y, z, entity.yRot, entity.xRot);
            return;
        }

        final Entity moved = entity.changeDimension(destWorld, new ITeleporter() {
            @Override
            public Entity placeEntity(final Entity entityToPlace, final ServerWorld currentWorld,
                                      final ServerWorld destination, final float yaw,
                                      final Function<Boolean, Entity> repositionEntity) {
                final Entity placed = repositionEntity.apply(false);   // false = do not place a portal
                if (placed != null) {
                    placed.teleportTo(x, y, z);
                }
                return placed;
            }
        });
        if (moved == null) {
            WarpDrive.logger.warn("Failed to move entity {} across dimensions", entity.getName().getString());
        }
    }

    private static void clearSource(final World sourceWorld, final ShipScanner.ShipScanResult scanResult) {
        for (final ShipScanner.ShipBlock shipBlock : scanResult.blocks) {
            sourceWorld.setBlock(shipBlock.pos, Blocks.AIR.defaultBlockState(), 2);
            sourceWorld.removeBlockEntity(shipBlock.pos);
        }
    }

    private static String dimensionOf(final World world) {
        return world == null ? "null" : world.dimension().location().toString();
    }

    /**
     * Result of a warp operation
     */
    public static class WarpResult {
        public final boolean success;
        public final String message;

        public WarpResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static boolean maybeLogCCComputer(final String phase, final BlockState state, final BlockPos pos, final CompoundNBT nbt) {
        if (state == null || state.getBlock() == null || state.getBlock().getRegistryName() == null) {
            return false;
        }
        final String name = state.getBlock().getRegistryName().toString();
        // match computer_normal, computer_advanced, computer_command, etc.
        if (name.startsWith("computercraft:computer")) {
            final int ccId = nbt != null && nbt.contains(CC_KEY_ID) ? nbt.getInt(CC_KEY_ID) : -1;
            final String label = nbt != null && nbt.contains(CC_KEY_LABEL) ? nbt.getString(CC_KEY_LABEL) : "";
            DebugLog.log("JUMP", "Warp {} CC computer {} at {} computerID={} label='{}' keys={} rawKeys={}",
                phase, name, pos, ccId == -1 ? "missing" : ccId, label, nbt == null ? 0 : nbt.getAllKeys().size(),
                nbt == null ? "null" : nbt.getAllKeys());
            return true;
        }
        return false;
    }

    private static void logTileEntityState(final String phase, final BlockState state, final BlockPos pos, final CompoundNBT nbt) {
        if (state == null || state.getBlock() == null || state.getBlock().getRegistryName() == null) {
            return;
        }
        String name = state.getBlock().getRegistryName().toString();
        if (name.startsWith("computercraft:computer") || name.contains("ship_core")) {
            DebugLog.log("JUMP", "Warp {} TE {} at {} keys={} hasComputerID={} hasLabel={} rawKeys={}",
                phase, name, pos, nbt == null ? 0 : nbt.getAllKeys().size(),
                nbt != null && nbt.contains(CC_KEY_ID),
                nbt != null && nbt.contains(CC_KEY_LABEL),
                nbt == null ? "null" : nbt.getAllKeys());
        }
    }
}
