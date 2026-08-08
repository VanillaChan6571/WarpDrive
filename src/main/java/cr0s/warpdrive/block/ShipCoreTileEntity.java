package cr0s.warpdrive.block;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.detection.SecurityStationTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.debug.DebugLog;
import cr0s.warpdrive.network.ShipCountdownPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import cr0s.warpdrive.render.BoundingBoxRenderer;
import cr0s.warpdrive.ship.ShipScanner;
import cr0s.warpdrive.ship.WarpEngine;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import cr0s.warpdrive.data.DimensionAltitude;
import cr0s.warpdrive.data.CelestialCoordinates;
import cr0s.warpdrive.data.ShipMovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ship Core TileEntity - Handles authoritative ship logic
 *
 * Implements:
 * - IEnergyStorage for Forge Energy
 * - CC:Tweaked annotations are consumed by the optional compatibility adapter when installed
 */
public class ShipCoreTileEntity extends TileEntity implements ITickableTileEntity {

	/**
	 * Warp isolation, from 1.12.2 WarpDriveConfig.RADAR_*_ISOLATION_*. A ring of isolation blocks
	 * around the core hides the ship from radar, scaling from 12% at two blocks to 100% at sixteen.
	 */
	private static final int ISOLATION_RANGE = 2;
	private static final int ISOLATION_MIN_BLOCKS = 2;
	private static final int ISOLATION_MAX_BLOCKS = 16;
	private static final double ISOLATION_MIN_EFFECT = 0.12D;
	private static final double ISOLATION_MAX_EFFECT = 1.00D;

	private int isolationBlocksCount = 0;
	private double isolationRate = 0.0D;

	// Energy storage
	private int energyStored = 0;
	private static final int UNTIERED_MAX_ENERGY = 10_000_000;
	private static final int MAX_TRANSFER = 10_000;    // 10K FE/t

	// Ship dimensions (classic WarpDrive 1.12.2 style), relative to `facing`
	private int dimFront = 0;
	private int dimBack = 0;
	private int dimLeft = 0;
	private int dimRight = 0;
	private int dimUp = 0;
	private int dimDown = 0;
	private String shipName = "Unnamed Ship";
	private UUID signatureUuid = UUID.randomUUID();

	// Which way the ship's bow points. SOUTH reproduces the original hard-coded mapping exactly
	// (front=+Z, back=-Z, right=+X, left=-X), so existing ships keep their current bounds.
	private Direction facing = Direction.SOUTH;

	// Yaw applied on jump: 0..3 quarter turns clockwise
	private int rotationSteps = 0;

	// Ship command state, mirroring the 1.12.2 controller's model
	private String command = "MANUAL";
	// TileEntityAbstractMachine defaulted this to true in 1.12.2. Starting disabled made every
	// freshly placed 1.16 core select the dull offline textures until a CC command happened.
	private boolean enabled = true;
	private static final int CORE_DATA_VERSION = 2;
	private String targetName = "";

	/** Destination dimension id, or empty for "stay in the current dimension". */
	private String targetDimension = "";

	// Jump sequencing: a countdown before the jump (which also gives force-loaded destination
	// chunks time to finish loading) and a cooldown afterwards.
	public static final int STATE_IDLE = 0;
	public static final int STATE_COUNTDOWN = 1;
	public static final int STATE_COOLDOWN = 2;
	private static final int MAX_FORCED_CHUNKS = 1024;

	// 10s is the normal charge-up. Cross-dimension jumps and giant ships escalate to 30s so the
	// destination world/chunks have time to load and generate. The size threshold is measured in
	// chunks, since that is what costs time.
	public static final int JUMP_DELAY_NORMAL_TICKS = 200;   // 10s -> warp_10s
	public static final int JUMP_DELAY_LARGE_TICKS = 600;    // 30s -> warp_30s
	private static final int LARGE_SHIP_CHUNK_THRESHOLD = 25;

	private int shipState = STATE_IDLE;
	private int countdownRemaining = 0;
	/** Players who received the one-shot HUD start packet, retained so abort can cancel it. */
	private final Set<UUID> countdownHudRecipients = new HashSet<>();
	private int cooldownRemaining = 0;
	private int jumpDelayTicks = JUMP_DELAY_NORMAL_TICKS;
	/** Delay actually in use for the current countdown (may be escalated for dimension/ship size). */
	private int activeJumpDelayTicks = JUMP_DELAY_NORMAL_TICKS;
	private boolean warpSoundPlayed = false;
	/**
	 * Blockstate changes from onLoad are unsafe in 1.16.5: a tile created during another tile's
	 * tick is still pending, so changing its state can make LevelChunk create a replacement before
	 * the restored object is committed. Reconcile appearance from the first normal tick instead.
	 */
	private boolean appearanceSyncPending = true;

	/** Client-side only: last state we logged, to avoid a SYNC line every second. */
	private String lastLoggedSyncSignature = "";
	private int cooldownTicks = 200;    // 10s
	private final Set<Long> forcedChunks = new HashSet<>();
	/** Which world `forcedChunks` belongs to - not necessarily this tile entity's world. */
	private RegistryKey<World> forcedChunksWorld = null;

	// Status calls from the bundled CC UI arrive once a second. A short-lived summary avoids six
	// separate full-volume scans per redraw; the actual jump always takes a fresh NBT snapshot.
	private static final long ASSEMBLY_STATUS_CACHE_TICKS = 100L;
	@Nullable
	private ShipScanner.ShipInspection cachedInspection = null;
	private long cachedInspectionTick = Long.MIN_VALUE;

	// Legacy ship-local movement: forward/back, up/down, right/left. These are deliberately not
	// world X/Y/Z; the horizontal pair is rotated through the core's facing immediately before use.
	private int moveX = 0; // forward (+) / back (-)
	private int moveY = 0; // up (+) / down (-)
	private int moveZ = 0; // right (+) / left (-)

	// Whether the bounding boxes are displayed. Server-authoritative and stored in NBT so the
	// setting survives a jump; the client renders straight off this + the fields above, which
	// means movement edits show up immediately without re-toggling.
	private boolean showBoundingBox = false;

	public ShipCoreTileEntity() {
		super(Registration.SHIP_CORE_TILE.get());
	}

	@Nullable
	private ShipCoreTier getCoreTier() {
		return getCoreTier(getBlockState());
	}

	@Nullable
	private static ShipCoreTier getCoreTier(final BlockState blockState) {
		return blockState.getBlock() instanceof ShipCoreBlock
			? ((ShipCoreBlock) blockState.getBlock()).getTier() : null;
	}

	private int getMaximumEnergyCapacity() {
		final ShipCoreTier tier = getCoreTier();
		return tier == null ? UNTIERED_MAX_ENERGY : tier.getCapacity();
	}

	private static int getMaximumEnergyCapacity(final BlockState blockState) {
		final ShipCoreTier tier = getCoreTier(blockState);
		return tier == null ? UNTIERED_MAX_ENERGY : tier.getCapacity();
	}

	private int getMaximumAxisSize() {
		final ShipCoreTier tier = getCoreTier();
		return tier == null ? ShipScanner.MAX_SHIP_SIDE : tier.getMaximumSide();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		debugCoreNbt("onLoad-enter", null);
		// Facing is a persisted blockstate property in both 1.12.2 and this port. BlockItem has
		// already calculated the player-relative state by the time the tile is loaded, so read it
		// here. Writing the tile's default SOUTH value in this callback races setPlacedBy and was
		// the reason every core eventually snapped south regardless of placement direction.
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof ShipCoreBlock
		 && blockState.hasProperty(ShipCoreBlock.FACING)) {
			facing = blockState.getValue(ShipCoreBlock.FACING);
		}
		appearanceSyncPending = !level.isClientSide;
		if (!level.isClientSide) {
			cr0s.warpdrive.data.GlobalRegionRegistry.registerShip(this);
		}
		debugCoreNbt("onLoad-exit", null);
	}

	@Override
	public void onChunkUnloaded() {
		cr0s.warpdrive.data.GlobalRegionRegistry.unregisterShip(this);
		super.onChunkUnloaded();
	}

	/** Called by ShipCoreBlock after placement so movement and rendering start in the same frame. */
	void setFacingFromBlock(@Nonnull final Direction direction) {
		if (!direction.getAxis().isVertical()) {
			facing = direction;
			setChanged();
			// onLoad runs while BlockItem is placing the block and may briefly apply the tile's
			// default SOUTH value. Re-apply the placement direction after setPlacedBy supplies it.
			syncBlockAppearance();
		}
	}

	private boolean isModelActive() {
		return enabled && !"OFFLINE".equals(command);
	}

	/** Synchronise the two legacy render properties without disturbing the block entity. */
	private void syncBlockAppearance() {
		if (level == null || level.isClientSide) {
			return;
		}
		final BlockState current = level.getBlockState(worldPosition);
		if (!(current.getBlock() instanceof ShipCoreBlock)) {
			return;
		}
		final BlockState wanted = current
			.setValue(ShipCoreBlock.FACING, facing)
			.setValue(ShipCoreBlock.ACTIVE, isModelActive());
		if (!wanted.equals(current)) {
			DebugLog.logSided(level, "CORE-NBT",
				"syncBlockAppearance object={} current={} wanted={} worldObject={} values={}",
				objectId(), current, wanted, objectId(level.getBlockEntity(worldPosition)), coreValues());
			level.setBlock(worldPosition, wanted, 3);
		}
	}

	// Ticking is back (it was removed with the creative auto-charge) because the jump countdown and
	// post-jump cooldown genuinely need per-tick work. Power still comes from real energy blocks.
	@Override
	public void tick() {
		if (level == null || level.isClientSide) {
			return;
		}
		if (appearanceSyncPending) {
			appearanceSyncPending = false;
			syncBlockAppearance();
			// A defensive guard for unusual server implementations: never tick state on an object
			// that was replaced while synchronising its render properties.
			if (isRemoved() || level.getBlockEntity(worldPosition) != this) {
				return;
			}
		}

		if (shipState == STATE_COUNTDOWN) {
			countdownRemaining--;

			// Start the charge-up so it ends as the ship moves
			if (!warpSoundPlayed && countdownRemaining <= warpSoundLengthTicks(activeJumpDelayTicks)) {
				warpSoundPlayed = true;
				// 1.12.2 used volume 4.0 - deliberately above 1.0 to widen the audible radius
				playShipSound(selectWarpSound(activeJumpDelayTicks), 4.0F, 1.0F);
			}

			if (countdownRemaining <= 0) {
				countdownRemaining = 0;
				shipState = STATE_IDLE;
				countdownHudRecipients.clear(); // normal completion expires on clients at the same end tick
				executeScheduledJump();
			} else if (countdownRemaining % 20 == 0) {
				syncToClient();
			}
		} else if (shipState == STATE_COOLDOWN) {
			cooldownRemaining--;
			if (cooldownRemaining <= 0) {
				cooldownRemaining = 0;
				shipState = STATE_IDLE;
				setChanged();
				syncToClient();
				DebugLog.log("JUMP", "cooldown complete at {}", getBlockPos());
			} else if (cooldownRemaining % 20 == 0) {
				syncToClient();
			}
		}
	}

	// ===== ComputerCraft Lua API Methods =====
	// Using @LuaFunction annotation (CC:Tweaked 1.16.5 pattern)

	@LuaFunction
	public final Object[] setDimensions(int front, int back, int left, int right, int up, int down) {
		if (shipState == STATE_COUNTDOWN) {
			return new Object[]{ false, "Cannot change dimensions during a jump countdown" };
		}
		// Validate dimensions
		if (front < 0 || back < 0 || left < 0 || right < 0 || up < 0 || down < 0) {
			return new Object[]{ false, "All dimensions must be >= 0" };
		}
		final int maximumAxisSize = getMaximumAxisSize();
		if ((long) front + back > maximumAxisSize
		 || (long) left + right > maximumAxisSize
		 || (long) up + down > maximumAxisSize) {
			return new Object[]{ false, String.format(
				"Each ship axis must be at most %d blocks, excluding the core",
				maximumAxisSize) };
		}

		final long volume = ((long) front + back + 1L)
		                  * ((long) left + right + 1L)
		                  * ((long) up + down + 1L);

		// Set dimensions
		dimFront = front;
		dimBack = back;
		dimLeft = left;
		dimRight = right;
		dimUp = up;
		dimDown = down;
		invalidateAssemblyInspection();
		setChanged();
		if (level != null && !level.isClientSide) {
			cr0s.warpdrive.data.GlobalRegionRegistry.updateShip(this);
		}

		// Sync to client
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}

		WarpDrive.logger.info("Ship dimensions set: F{} B{} L{} R{} U{} D{} (volume: {})",
			front, back, left, right, up, down, volume);

		return new Object[]{ true, String.format("Dimensions set (volume: %,d blocks)", volume) };
	}

	@LuaFunction
	public final Object[] getDimensions() {
		return new Object[]{ dimFront, dimBack, dimLeft, dimRight, dimUp, dimDown };
	}

	@LuaFunction
	public final int getEnergyStored() {
		return energyStored;
	}

	/** Legacy Lua shape: success, required = ship.getEnergyRequired(). */
	@LuaFunction
	public final Object[] getEnergyRequired() {
		if (getShipVolume() <= 1) {
			return new Object[]{ false, "Dimensions not set" };
		}
		return new Object[]{ true, calculateEnergyRequired() };
	}

	/** Direct value for the native controller, which does not need Lua's success tuple. */
	public int getEnergyRequiredValue() {
		return calculateEnergyRequired();
	}

	/** Legacy: local shipMass, shipVolume = ship.getShipSize() - mass is the solid block count. */
	@LuaFunction
	public final Object[] getShipSize() {
		return new Object[]{ getShipMass(), getShipVolume() };
	}

	/** Number of non-air blocks inside the envelope, from the short-lived status cache. */
	public int getShipMass() {
		return inspectAssembly(false).blockCount;
	}

	@LuaFunction
	public final Object[] addEnergy(int amount) {
		if (amount <= 0) {
			return new Object[]{ false, "Amount must be positive" };
		}

		int added = Math.min(getMaximumEnergyCapacity() - energyStored, amount);
		energyStored += added;
		setChanged();

		WarpDrive.logger.info("Added {} FE to ship core (now: {} FE)", added, energyStored);

		return new Object[]{ true, String.format("Added %,d FE (Total: %,d FE)", added, energyStored) };
	}

	@LuaFunction
	public final Object[] setEnergy(int amount) {
		final int maximumEnergy = getMaximumEnergyCapacity();
		if (amount < 0 || amount > maximumEnergy) {
			return new Object[]{ false, String.format("Amount must be 0-%,d", maximumEnergy) };
		}

		energyStored = amount;
		setChanged();

		WarpDrive.logger.info("Set ship core energy to {} FE", energyStored);

		return new Object[]{ true, String.format("Energy set to %,d FE", energyStored) };
	}

	private int calculateEnergyRequired() {
		return calculateEnergyRequired(getShipMass());
	}

	private int calculateEnergyRequired(final int shipMass) {
		return level == null ? 0 : getMovementType().energyRequired(shipMass, getMovementDistance());
	}

	/** Legacy costs use the ceiling of the Euclidean movement length. */
	private int getMovementDistance() {
		return (int) Math.ceil(Math.sqrt(
			(double) moveX * moveX + (double) moveY * moveY + (double) moveZ * moveZ));
	}

	private int getShipVolume() {
		final long volume = ((long) dimFront + dimBack + 1L)
		                  * ((long) dimLeft + dimRight + 1L)
		                  * ((long) dimUp + dimDown + 1L);
		return volume > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, volume);
	}

	public boolean isBoundingBoxShown() {
		return showBoundingBox;
	}

	public Direction getFacingDirection() {
		return facing;
	}

	public int getRotationStepsValue() {
		return rotationSteps;
	}

	/** Empty means normal movement or an automatic vertical boundary transition. */
	public String getConfiguredTargetDimension() {
		return targetDimension == null ? "" : targetDimension;
	}

	// ===== 1.12.2-compatible status API =====
	// Signatures deliberately match what the legacy warpdriveShipController expects, so the old
	// controller layout can be ported without reshaping every call site.

	/** Core block position. Legacy: ship.getLocalPosition() -> x, y, z */
	@LuaFunction
	public final Object[] getLocalPosition() {
		final BlockPos pos = getBlockPos();
		return new Object[]{ pos.getX(), pos.getY(), pos.getZ() };
	}

	/** Position plus dimension id. Legacy left this as a TODO; implemented here. */
	@LuaFunction
	public final Object[] getLocation() {
		final BlockPos pos = getBlockPos();
		final String dimension = level == null ? "unknown" : level.dimension().location().toString();
		return new Object[]{ pos.getX(), pos.getY(), pos.getZ(), dimension };
	}

	/** Bow direction as a unit vector. Legacy: local dx, dy, dz = ship.getOrientation() */
	@LuaFunction
	public final Object[] getOrientation() {
		return new Object[]{ facing.getStepX(), facing.getStepY(), facing.getStepZ() };
	}

	/** Set the bow direction by name (north/south/east/west). Dimensions are relative to it. */
	@LuaFunction
	public final Object[] setOrientation(final String direction) {
		if (shipState == STATE_COUNTDOWN) {
			return new Object[]{ false, "Cannot change orientation during a jump countdown" };
		}
		final Direction parsed = Direction.byName(direction == null ? "" : direction.toLowerCase());
		if (parsed == null || parsed.getAxis().isVertical()) {
			return new Object[]{ false, "Orientation must be north, south, east or west" };
		}
		facing = parsed;
		invalidateAssemblyInspection();
		setChanged();
		syncBlockAppearance();
		syncToClient();
		return new Object[]{ true, "Bow now faces " + facing };
	}

	/** Legacy: ship.rotationSteps() to read, ship.rotationSteps(n) to set. 0..3 quarter turns. */
	@LuaFunction
	public final Object[] rotationSteps(final Optional<Integer> steps) {
		if (steps.isPresent()) {
			if (shipState == STATE_COUNTDOWN) {
				return new Object[]{ false, "Cannot change rotation during a jump countdown" };
			}
			rotationSteps = ((steps.get() % 4) + 4) % 4;
			setChanged();
			syncToClient();
		}
		return new Object[]{ rotationSteps };
	}

	/** Legacy: local energyStored, energyMax, energyUnits = ship.getEnergyStatus() */
	@LuaFunction
	public final Object[] getEnergyStatus() {
		return new Object[]{ energyStored, getMaximumEnergyCapacity(), "FE" };
	}

	/** Legacy: local success, maxJumpDistance = ship.getMaxJumpDistance() */
	@LuaFunction
	public final Object[] getMaxJumpDistance() {
		final int volume = getShipVolume();
		if (volume <= 1) {
			return new Object[]{ false, "dimensions not set" };
		}
		final int byType = getMaxJumpDistanceByType();
		return new Object[]{ byType >= ShipMovementType.MINIMUM_DISTANCE_BLOCKS, byType };
	}

	/** Range ceiling imposed by the kind of movement and the ship's mass. */
	public int getMaxJumpDistanceByType() {
		return getMovementType().maximumDistance(getShipMass());
	}

	/** Range ceiling imposed by stored energy, using the inverse of the active movement cost. */
	public int getMaxJumpDistanceByEnergy() {
		if (getShipVolume() <= 1) {
			return 0;
		}
		return getMovementType().maximumDistanceForEnergy(getShipMass(), energyStored);
	}

	/** Whichever ceiling binds first. */
	public int getEffectiveMaxJumpDistance() {
		return Math.min(getMaxJumpDistanceByType(), getMaxJumpDistanceByEnergy());
	}

	private void invalidateAssemblyInspection() {
		cachedInspection = null;
		cachedInspectionTick = Long.MIN_VALUE;
	}

	private ShipScanner.ShipInspection inspectAssembly(final boolean forceRefresh) {
		if (getShipVolume() <= 1) {
			return new ShipScanner.ShipInspection(false, "Dimensions not set", 0, getShipVolume());
		}
		if (level == null) {
			return new ShipScanner.ShipInspection(false, "No world", 0, getShipVolume());
		}

		final long now = level.getGameTime();
		if (!forceRefresh && cachedInspection != null
		 && now >= cachedInspectionTick
		 && now - cachedInspectionTick < ASSEMBLY_STATUS_CACHE_TICKS) {
			return cachedInspection;
		}

		cachedInspection = ShipScanner.inspectShip(level, getBlockPos(), getShipBounds());
		final ShipCoreTier tier = getCoreTier();
		if (cachedInspection.success && tier != null) {
			if (cachedInspection.blockCount < tier.getMinimumMass()) {
				cachedInspection = new ShipScanner.ShipInspection(false,
					String.format("Ship mass %,d is below the %s minimum of %,d blocks",
						cachedInspection.blockCount, tier.getName(), tier.getMinimumMass()),
					cachedInspection.blockCount, cachedInspection.envelopeVolume,
					cachedInspection.securityStationPos);
			} else if (cachedInspection.blockCount > tier.getMaximumMass()) {
				cachedInspection = new ShipScanner.ShipInspection(false,
					String.format("Ship mass %,d exceeds the %s maximum of %,d blocks",
						cachedInspection.blockCount, tier.getName(), tier.getMaximumMass()),
					cachedInspection.blockCount, cachedInspection.envelopeVolume,
					cachedInspection.securityStationPos);
			}
		}
		cachedInspectionTick = now;
		updateIsolation();
		if (!level.isClientSide) {
			cr0s.warpdrive.data.GlobalRegionRegistry.updateShip(this);
		}
		return cachedInspection;
	}

	/**
	 * Count the warp isolation blocks around the core and derive the isolation rate, from 1.12.2
	 * TileEntityShipCore.doScanAssembly. Recomputed with the assembly inspection, which is the same
	 * cadence the original used.
	 *
	 * Isolation hides a ship from radar. The loaded-provider global registry consumes this value
	 * probabilistically for every local radar scan.
	 */
	private void updateIsolation() {
		if (level == null) {
			return;
		}
		final BlockPos pos = getBlockPos();
		final int xMin = pos.getX() - ISOLATION_RANGE;
		final int xMax = pos.getX() + ISOLATION_RANGE;
		final int zMin = pos.getZ() - ISOLATION_RANGE;
		final int zMax = pos.getZ() + ISOLATION_RANGE;
		// scanned one block higher than it is low, to encourage isolating floor and ceiling both
		final int yMin = Math.max(0, pos.getY() - ISOLATION_RANGE + 1);
		final int yMax = Math.min(255, pos.getY() + ISOLATION_RANGE + 1);

		final Block blockIsolation = Registration.WARP_ISOLATION.get();
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		int count = 0;
		for (int y = yMin; y <= yMax; y++) {
			for (int x = xMin; x <= xMax; x++) {
				for (int z = zMin; z <= zMax; z++) {
					mutable.set(x, y, z);
					if (level.getBlockState(mutable).getBlock() == blockIsolation) {
						count++;
					}
				}
			}
		}

		isolationBlocksCount = count;
		final double previousRate = isolationRate;
		if (count >= ISOLATION_MIN_BLOCKS) {
			// linear from the minimum effect at the minimum count, capped at 100%
			isolationRate = Math.min(1.0D, ISOLATION_MIN_EFFECT
				+ (count - ISOLATION_MIN_BLOCKS)
				* (ISOLATION_MAX_EFFECT - ISOLATION_MIN_EFFECT)
				/ (ISOLATION_MAX_BLOCKS - ISOLATION_MIN_BLOCKS));
		} else {
			isolationRate = 0.0D;
		}
		if (previousRate != isolationRate) {
			DebugLog.log("JUMP", "isolation at {} is now {} blocks ({}%)",
				pos, isolationBlocksCount, isolationRate * 100.0D);
		}
	}

	/** Number of warp isolation blocks currently surrounding the core. */
	public int getIsolationBlocksCount() {
		return isolationBlocksCount;
	}

	/** How well this ship is hidden from radar, 0.0 to 1.0. For the radar port to read. */
	public double getIsolationRate() {
		return isolationRate;
	}

	private void cacheAssemblySnapshot(final ShipScanner.ShipScanResult snapshot) {
		if (level == null) {
			return;
		}
		cachedInspection = new ShipScanner.ShipInspection(snapshot.success, snapshot.message,
			snapshot.getBlockCount(), snapshot.getVolume());
		cachedInspectionTick = level.getGameTime();
	}

	/** Legacy: isValid, message = ship.getAssemblyStatus() */
	@LuaFunction
	public final Object[] getAssemblyStatus() {
		final ShipScanner.ShipInspection inspection = inspectAssembly(false);
		return new Object[]{ inspection.success, inspection.message };
	}

	/** Force a fresh integrity scan. Kept compatible with the bundled ComputerCraft program. */
	@LuaFunction
	public final Object[] scan() {
		final ShipScanner.ShipInspection inspection = inspectAssembly(true);
		return new Object[]{ inspection.success, inspection.blockCount, inspection.message };
	}

	/** Legacy: local stringPlayers = ship.getAttachedPlayers() */
	@LuaFunction
	public final Object[] getAttachedPlayers() {
		if (level == null) {
			return new Object[]{ "" };
		}
		final int[] b = getShipBounds();
		final AxisAlignedBB box = new AxisAlignedBB(b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1);
		final StringBuilder names = new StringBuilder();
		for (final PlayerEntity player : level.players()) {
			if (box.contains(player.getX(), player.getY(), player.getZ())) {
				if (names.length() > 0) {
					names.append(", ");
				}
				names.append(player.getGameProfile().getName());
			}
		}
		return new Object[]{ names.toString() };
	}

	/** Legacy guard: nil means "no ship core attached". */
	@LuaFunction
	public final Object[] isInterfaced() {
		return new Object[]{ true, "CC:Tweaked peripheral available." };
	}

	/** Common legacy computer API inherited from TileEntityAbstractInterfaced in 1.12.2. */
	@LuaFunction
	public final Object[] getTier() {
		final ShipCoreTier tier = getCoreTier();
		return tier == null
			? new Object[]{ 1, "basic" }
			: new Object[]{ tier.getLegacyIndex(), tier.getName() };
	}

	/** Ship cores have no installable upgrades in the native 1.16 implementation. */
	@LuaFunction
	public final Object[] getUpgrades() {
		return new Object[]{ false, "No installable upgrades." };
	}

	@LuaFunction
	public final Object[] getVersion() {
		return WarpDrive.getVersionNumbers();
	}

	/**
	 * Legacy: ship.isInSpace() / ship.isInHyperspace().
	 */
	@LuaFunction
	public final Object[] isInSpace() {
		return new Object[]{ level != null && "warpdrive:space".equals(level.dimension().location().toString()) };
	}

	@LuaFunction
	public final Object[] isInHyperspace() {
		return new Object[]{ level != null && "warpdrive:hyperspace".equals(level.dimension().location().toString()) };
	}

	/** Legacy: local status, isEnabled, state, energy = ship.state() */
	@LuaFunction
	public final Object[] state() {
		final int volume = getShipVolume();
		final int required = calculateEnergyRequired();
		final String status;
		if (shipState == STATE_COUNTDOWN) {
			status = String.format("Jumping in %.1fs", countdownRemaining / 20.0);
		} else if (shipState == STATE_COOLDOWN) {
			status = String.format("Cooldown %.1fs", cooldownRemaining / 20.0);
		} else if (volume <= 1) {
			status = "Dimensions not set";
		} else if (energyStored < required) {
			status = String.format("Charging %,d / %,d FE", energyStored, required);
		} else {
			status = "Ready";
		}
		return new Object[]{ status, enabled, command, energyStored };
	}

	/** Countdown/cooldown in ticks, for the controller readout. */
	@LuaFunction
	public final Object[] getJumpTimers() {
		return new Object[]{ shipState, countdownRemaining, cooldownRemaining, jumpDelayTicks, cooldownTicks };
	}

	@LuaFunction
	public final Object[] setJumpDelay(final int ticks) {
		jumpDelayTicks = Math.max(0, Math.min(20 * 60, ticks));
		setChanged();
		return new Object[]{ true, String.format("Jump delay %.1fs", jumpDelayTicks / 20.0) };
	}

	@LuaFunction
	public final Object[] setCooldownTime(final int ticks) {
		cooldownTicks = Math.max(0, Math.min(20 * 600, ticks));
		setChanged();
		return new Object[]{ true, String.format("Cooldown %.1fs", cooldownTicks / 20.0) };
	}

	/** Abort a countdown that has not fired yet. */
	@LuaFunction
	public final Object[] abortJump() {
		if (shipState != STATE_COUNTDOWN) {
			return new Object[]{ false, "No jump in progress" };
		}
		stopCountdownHud();
		shipState = STATE_IDLE;
		countdownRemaining = 0;
		forceDestinationChunks(false);
		playShipSound(Registration.SOUND_DECLOAK.get(), 0.8F, 0.8F);   // drive spinning down
		broadcastToOnboard(new StringTextComponent(TextFormatting.YELLOW + "✖ JUMP ABORTED"));
		setChanged();
		syncToClient();
		DebugLog.log("JUMP", "countdown aborted at {}", getBlockPos());
		return new Object[]{ true, "Jump aborted" };
	}

	/** Legacy: ship.command("MANUAL", false) */
	@LuaFunction
	public final Object[] command(final String newCommand, final Optional<Boolean> doEnable) {
		if (newCommand != null && !newCommand.isEmpty()) {
			command = newCommand.toUpperCase();
		}
		final boolean confirmed = doEnable.orElse(false);
		DebugLog.log("SHIP", "command set to {} (confirmed={}, enabled={})", command, confirmed, enabled);
		setChanged();
		syncBlockAppearance();
		if (confirmed && enabled) {
			if ("MANUAL".equals(command)) {
				return jump();
			}
			if ("GATE".equals(command)) {
				return new Object[]{ false,
					"Jump gates are unavailable: the legacy gate-core refactor was never registered or completed." };
			}
		}
		return new Object[]{ command, confirmed };
	}

	/** Legacy machine power switch. Command confirmation, not this switch, starts a jump. */
	@LuaFunction
	public final Object[] enable(final Optional<Boolean> value) {
		if (value.isPresent()) {
			enabled = value.get();
			setChanged();
			syncBlockAppearance();
			DebugLog.log("SHIP", "enable({}) with command={}", enabled, command);
		}
		return new Object[]{ enabled };
	}

	/** Legacy: ship.targetName("") - get or set the jump-gate target name. */
	@LuaFunction
	public final Object[] targetName(final Optional<String> name) {
		if (name.isPresent()) {
			targetName = name.get();
			setChanged();
		}
		return new Object[]{ targetName };
	}

	// --- Legacy call shapes, so the 1.12.2 controller ports without rewriting every call site ---

	/** Legacy: front, right, up = ship.dim_positive([front, right, up]). */
	@LuaFunction
	public final Object[] dim_positive(final Optional<Integer> front,
	                                  final Optional<Integer> right,
	                                  final Optional<Integer> up) {
		if (front.isPresent() && right.isPresent() && up.isPresent()) {
			setDimensions(front.get(), dimBack, dimLeft, right.get(), up.get(), dimDown);
		}
		return new Object[]{ dimFront, dimRight, dimUp };
	}

	/** Legacy: back, left, down = ship.dim_negative([back, left, down]). */
	@LuaFunction
	public final Object[] dim_negative(final Optional<Integer> back,
	                                  final Optional<Integer> left,
	                                  final Optional<Integer> down) {
		if (back.isPresent() && left.isPresent() && down.isPresent()) {
			setDimensions(dimFront, back.get(), left.get(), dimRight, dimUp, down.get());
		}
		return new Object[]{ dimBack, dimLeft, dimDown };
	}

	/** Legacy: ship.movement() to read, ship.movement(x, y, z) to set. */
	@LuaFunction
	public final Object[] movement(final Optional<Integer> x, final Optional<Integer> y, final Optional<Integer> z) {
		if (x.isPresent() && y.isPresent() && z.isPresent()) {
			setMovement(x.get(), y.get(), z.get());
		}
		return new Object[]{ moveX, moveY, moveZ };
	}

	/** Legacy: ship.name() to read, ship.name("x") to set. */
	@LuaFunction
	public final Object[] name(final Optional<String> newName) {
		if (newName.isPresent() && !newName.get().trim().isEmpty()) {
			setName(newName.get());
		}
		return new Object[]{ shipName };
	}

	/** Resolve the world this ship will jump into. Null means the target could not be resolved. */
	@Nullable
	private ServerWorld resolveDestinationWorld() {
		if (!(level instanceof ServerWorld)) {
			return null;
		}
		final ServerWorld current = (ServerWorld) level;
		if (targetDimension == null || targetDimension.isEmpty()) {
			return current;
		}
		final ResourceLocation id = ResourceLocation.tryParse(targetDimension);
		if (id == null) {
			return null;
		}
		return current.getServer().getLevel(RegistryKey.create(Registry.DIMENSION_REGISTRY, id));
	}

	/** Set the destination dimension, e.g. "warpdrive:space". Empty string means stay put. */
	@LuaFunction
	public final Object[] setTargetDimension(final String dimensionId) {
		if (shipState == STATE_COUNTDOWN) {
			return new Object[]{ false, "Cannot change destination dimension during a jump countdown" };
		}
		final String requested = dimensionId == null ? "" : dimensionId.trim();
		if (requested.isEmpty()) {
			targetDimension = "";
			setChanged();
			return new Object[]{ true, "Destination dimension cleared (same dimension)" };
		}
		final String previous = targetDimension;
		targetDimension = requested;
		if (resolveDestinationWorld() == null) {
			targetDimension = previous;
			return new Object[]{ false, "Unknown dimension: " + requested };
		}
		setChanged();
		DebugLog.log("JUMP", "target dimension set to {}", targetDimension);
		return new Object[]{ true, "Destination dimension: " + targetDimension };
	}

	@LuaFunction
	public final Object[] getTargetDimension() {
		if (targetDimension == null || targetDimension.isEmpty()) {
			return new Object[]{ level == null ? "unknown" : level.dimension().location().toString(), false };
		}
		return new Object[]{ targetDimension, true };
	}

	private void syncToClient() {
		if (level != null && !level.isClientSide) {
			debugCoreNbt("syncToClient", null);
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	// ===== Debug reporting =====

	private String objectId() {
		return objectId(this);
	}

	private static String objectId(@Nullable final Object object) {
		return object == null ? "null" : object.getClass().getSimpleName() + "@"
			+ Integer.toHexString(System.identityHashCode(object));
	}

	private String coreValues() {
		return String.format("name='%s' dims=F%d B%d L%d R%d U%d D%d move=%d,%d,%d "
				+ "energy=%d box=%s facing=%s rot=%d enabled=%s command=%s shipState=%d countdown=%d cooldown=%d",
			shipName, dimFront, dimBack, dimLeft, dimRight, dimUp, dimDown,
			moveX, moveY, moveZ, energyStored, showBoundingBox, facing, rotationSteps,
			enabled, command, shipState, countdownRemaining, cooldownRemaining);
	}

	private static String coreTagValues(@Nullable final CompoundNBT nbt) {
		if (nbt == null) {
			return "<no tag>";
		}
		return String.format("keys=%d version=%d name='%s' dims=F%d B%d L%d R%d U%d D%d "
				+ "move=%d,%d,%d energy=%d box=%s facing=%d rot=%d enabled=%s command='%s' "
				+ "shipState=%d countdown=%d cooldown=%d pos=%d,%d,%d",
			nbt.getAllKeys().size(), nbt.getInt("CoreDataVersion"), nbt.getString("ShipName"),
			nbt.getInt("DimFront"), nbt.getInt("DimBack"), nbt.getInt("DimLeft"),
			nbt.getInt("DimRight"), nbt.getInt("DimUp"), nbt.getInt("DimDown"),
			nbt.getInt("MoveX"), nbt.getInt("MoveY"), nbt.getInt("MoveZ"), nbt.getInt("Energy"),
			nbt.getBoolean("ShowBoundingBox"), nbt.getInt("Facing"), nbt.getInt("RotationSteps"),
			nbt.getBoolean("Enabled"), nbt.getString("Command"), nbt.getInt("ShipState"),
			nbt.getInt("CountdownRemaining"), nbt.getInt("CooldownRemaining"),
			nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
	}

	private void debugCoreNbt(final String event, @Nullable final CompoundNBT nbt) {
		final TileEntity worldTile = level == null ? null : level.getBlockEntity(worldPosition);
		// TileEntity.load() runs before Chunk installs the new block entity into a World. Vanilla's
		// getBlockState() tries to resolve its empty cache through level and therefore throws when
		// called in that window. Logging must never be able to make Minecraft reject valid core NBT.
		final Object blockState = level == null ? "<unbound>" : getBlockState();
		DebugLog.logSided(level, "CORE-NBT",
			"{} pos={} object={} removed={} worldObject={} block={} values={} tag={}",
			event, worldPosition, objectId(), isRemoved(), objectId(worldTile), blockState,
			coreValues(), coreTagValues(nbt));
	}

	/**
	 * Full state snapshot. Deliberately includes the logical side: nearly every bug found so far
	 * came down to the client and server disagreeing, and that is invisible from a single value.
	 */
	public String buildDebugReport() {
		final StringBuilder report = new StringBuilder();
		report.append("side          : ").append(level == null ? "no world" : (level.isClientSide ? "CLIENT" : "SERVER")).append('\n');
		report.append("thread        : ").append(Thread.currentThread().getName()).append('\n');
		report.append("corePos       : ").append(getBlockPos()).append('\n');
		report.append("shipName      : ").append(shipName).append('\n');
		report.append("dimensions    : F").append(dimFront).append(" B").append(dimBack)
			.append(" L").append(dimLeft).append(" R").append(dimRight)
			.append(" U").append(dimUp).append(" D").append(dimDown).append('\n');
		report.append("volume        : ").append(getShipVolume()).append('\n');
		report.append("movement      : ").append(moveX).append(", ").append(moveY).append(", ").append(moveZ).append('\n');
		report.append("energyStored  : ").append(energyStored).append(" / ")
			.append(getMaximumEnergyCapacity()).append('\n');
		report.append("energyNeeded  : ").append(calculateEnergyRequired()).append('\n');
		report.append("showBoundBox  : ").append(showBoundingBox).append('\n');
		report.append("CC:Tweaked    : ").append(
			net.minecraftforge.fml.ModList.get().isLoaded("computercraft") ? "available" : "not installed").append('\n');
		if (level != null) {
			report.append("blockAtPos    : ").append(level.getBlockState(getBlockPos()).getBlock().getRegistryName()).append('\n');
		}
		return report.toString();
	}

	/** Writes a full state dump to logs/warpdrive-debug.log and returns it for the Lua caller. */
	@LuaFunction
	public final Object[] debugDump() {
		final String report = buildDebugReport();
		DebugLog.logBlock("DUMP", "Ship Core state dump requested from Lua:", report);
		return new Object[]{ true, report };
	}

	/** Turn the debug log on or off at runtime, so it is not writing during normal play. */
	@LuaFunction
	public final Object[] setDebugLogging(final boolean value) {
		DebugLog.setEnabled(value);
		return new Object[]{ true, "Debug logging " + (value ? "enabled" : "disabled") };
	}

	@LuaFunction
	public final Object[] getDebugLogPath() {
		final java.nio.file.Path path = DebugLog.getLogFile();
		return new Object[]{ path != null, path == null ? "debug log unavailable" : path.toString() };
	}

	/**
	 * Inclusive block bounds {minX, minY, minZ, maxX, maxY, maxZ}, resolved against `facing`.
	 * With facing SOUTH this produces exactly the previous hard-coded axis mapping.
	 */
	public int[] getShipBounds() {
		final BlockPos corePos = getBlockPos();
		final int[] lo = { corePos.getX(), corePos.getY() - dimDown, corePos.getZ() };
		final int[] hi = { corePos.getX(), corePos.getY() + dimUp, corePos.getZ() };

		extend(lo, hi, facing, dimFront);
		extend(lo, hi, facing.getOpposite(), dimBack);
		extend(lo, hi, facing.getClockWise(), dimRight);
		extend(lo, hi, facing.getCounterClockWise(), dimLeft);

		return new int[]{ lo[0], lo[1], lo[2], hi[0], hi[1], hi[2] };
	}

	private static void extend(final int[] lo, final int[] hi, final Direction direction, final int amount) {
		final int dx = direction.getStepX() * amount;
		final int dz = direction.getStepZ() * amount;
		if (dx < 0) {
			lo[0] += dx;
		} else {
			hi[0] += dx;
		}
		if (dz < 0) {
			lo[2] += dz;
		} else {
			hi[2] += dz;
		}
	}

	/** Ship bounds in world space, derived live from the current core position and dimensions. */
	public double[] getShipBoxBounds() {
		final int[] b = getShipBounds();
		return new double[]{ b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1 };
	}

	/** Destination bounds, or null when no movement is set. */
	@Nullable
	public double[] getDestinationBoxBounds() {
		if (moveX == 0 && moveY == 0 && moveZ == 0) {
			return null;
		}
		final int[] bounds = getShipBounds();
		final BlockPos core = getBlockPos();
		final BlockPos movement = getWorldMovement();
		final int[] offsetsX = { bounds[0] - core.getX(), bounds[3] - core.getX() };
		final int[] offsetsZ = { bounds[2] - core.getZ(), bounds[5] - core.getZ() };
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (final int offsetX : offsetsX) {
			for (final int offsetZ : offsetsZ) {
				int rotatedX = offsetX;
				int rotatedZ = offsetZ;
				switch (Math.floorMod(rotationSteps, 4)) {
					case 1: rotatedX = -offsetZ; rotatedZ = offsetX;  break;
					case 2: rotatedX = -offsetX; rotatedZ = -offsetZ; break;
					case 3: rotatedX = offsetZ;  rotatedZ = -offsetX; break;
					default: break;
				}
				final int x = core.getX() + movement.getX() + rotatedX;
				final int z = core.getZ() + movement.getZ() + rotatedZ;
				minX = Math.min(minX, x);
				maxX = Math.max(maxX, x);
				minZ = Math.min(minZ, z);
				maxZ = Math.max(maxZ, z);
			}
		}
		return new double[]{ minX, bounds[1] + moveY, minZ,
			maxX + 1, bounds[4] + moveY + 1, maxZ + 1 };
	}

	/** Convert legacy forward/up/right movement into a world-space delta. */
	private BlockPos getWorldMovement() {
		return new BlockPos(
			facing.getStepX() * moveX - facing.getStepZ() * moveZ,
			moveY,
			facing.getStepZ() * moveX + facing.getStepX() * moveZ);
	}

	/** Keep the client-side renderer's tracked set in step with this tile entity. */
	private void refreshRendererTracking() {
		if (level == null || !level.isClientSide) {
			return;
		}
		if (showBoundingBox) {
			BoundingBoxRenderer.track(getBlockPos());
		} else {
			BoundingBoxRenderer.untrack(getBlockPos());
		}
		// The countdown/cooldown sync fires once a second, so logging every packet buried the
		// interesting lines. Only report when something the client cares about actually changed.
		final String signature = getBlockPos() + "|" + dimFront + "," + dimBack + "," + dimLeft + ","
			+ dimRight + "," + dimUp + "," + dimDown + "|" + moveX + "," + moveY + "," + moveZ
			+ "|" + showBoundingBox + "|" + facing + "|" + rotationSteps;
		if (!signature.equals(lastLoggedSyncSignature)) {
			lastLoggedSyncSignature = signature;
			DebugLog.logSided(level, "SYNC",
				"client state changed at {}: dims=F{} B{} L{} R{} U{} D{} move={},{},{} box={} facing={} rot={}",
				getBlockPos(), dimFront, dimBack, dimLeft, dimRight, dimUp, dimDown,
				moveX, moveY, moveZ, showBoundingBox, facing, rotationSteps);
		}
	}

	@LuaFunction
	public final Object[] setMovement(final int forward, final int up, final int right) {
		if (shipState == STATE_COUNTDOWN) {
			return new Object[]{ false, "Cannot change movement during a jump countdown" };
		}
		if (forward == 0 && up == 0 && right == 0) {
			moveX = 0;
			moveY = 0;
			moveZ = 0;
			setChanged();
			syncToClient();
			return new Object[]{ true, "Movement cleared" };
		}
		// Set the requested movement first: the movement type - and therefore the range - depends on
		// where this jump would end up, so takeoff and landing can only be recognised once it is known
		moveX = forward;
		moveY = up;
		moveZ = right;

		final ShipMovementType movementType = getMovementType();
		final int maximum = getMaxJumpDistanceByType();
		final double requested = Math.sqrt((double) forward * forward
			+ (double) up * up + (double) right * right);

		if (requested < ShipMovementType.MINIMUM_DISTANCE_BLOCKS) {
			return new Object[]{ false, String.format(
				"Movement too small: at least %d block required, up to %d (%s)",
				ShipMovementType.MINIMUM_DISTANCE_BLOCKS, maximum, movementType.getName()) };
		}

		// 1.12.2 treats the configured range as empty travel beyond the hull. Each local axis may
		// therefore include its full ship length plus that range (the exact limits printed by the
		// legacy M/P movement pages).
		final int maximumForward = dimFront + dimBack + 1 + maximum;
		final int maximumUp = dimUp + dimDown + 1 + maximum;
		final int maximumRight = dimRight + dimLeft + 1 + maximum;
		moveX = Math.max(-maximumForward, Math.min(maximumForward, forward));
		moveY = Math.max(-maximumUp, Math.min(maximumUp, up));
		moveZ = Math.max(-maximumRight, Math.min(maximumRight, right));
		final String note = moveX != forward || moveY != up || moveZ != right
			? " (clamped to legacy hull-clearance limits)" : "";

		setChanged();

		// Sync to client so the destination bounding box can be drawn
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}

		WarpDrive.logger.info("Local movement set to F/U/R {}, {}, {} ({}, max {})",
			moveX, moveY, moveZ, movementType.getName(), maximum);

		return new Object[]{ true, String.format("Movement set: %d forward, %d up, %d right%s - %s, range %d to %d blocks",
			moveX, moveY, moveZ, note, movementType.getName(),
			ShipMovementType.MINIMUM_DISTANCE_BLOCKS, maximum), maximum, movementType.getName() };
	}

	/**
	 * What kind of movement the currently configured jump is, which sets its range.
	 *
	 * Order matters: a vertical boundary crossing is takeoff or landing regardless of which
	 * dimension it starts in, and an explicit destination dimension is a hyperspace transition
	 * rather than movement within the current world.
	 */
	public ShipMovementType getMovementType() {
		if (level == null) {
			return ShipMovementType.SPACE_MOVING;
		}
		if (transitionTarget() != null) {
			final int[] b = getShipBounds();
			return b[4] + moveY > DimensionAltitude.ceilingOf(level)
			     ? ShipMovementType.PLANET_TAKEOFF
			     : ShipMovementType.PLANET_LANDING;
		}

		final String current = DimensionAltitude.idOf(level);
		if (targetDimension != null && !targetDimension.isEmpty() && !targetDimension.equals(current)) {
			return DimensionAltitude.HYPERSPACE.equals(targetDimension)
			     ? ShipMovementType.HYPERSPACE_ENTERING
			     : ShipMovementType.HYPERSPACE_EXITING;
		}

		switch (current) {
			case DimensionAltitude.HYPERSPACE: return ShipMovementType.HYPERSPACE_MOVING;
			case DimensionAltitude.SPACE:      return ShipMovementType.SPACE_MOVING;
			default:                           return ShipMovementType.PLANET_MOVING;
		}
	}


	@LuaFunction
	public final Object[] getMovement() {
		return new Object[]{ moveX, moveY, moveZ };
	}

	@LuaFunction
	public final String getName() {
		return shipName;
	}

	public UUID getSignatureUUID() { return signatureUuid; }

	/** Last assembly mass already computed by the status cache; never initiates a recursive scan. */
	public int getKnownShipMass() {
		return cachedInspection == null ? 0 : cachedInspection.blockCount;
	}

	/** Refresh the persistent global-region snapshot from a radar or lifecycle query. */
	public void refreshGlobalRegion() {
		if (level == null || level.isClientSide || isRemoved()) return;
		inspectAssembly(false);
		cr0s.warpdrive.data.GlobalRegionRegistry.updateShip(this);
	}

	@Nullable
	private SecurityStationTileEntity getSecurityStation() {
		if (level == null) return null;
		final ShipScanner.ShipInspection inspection = inspectAssembly(false);
		if (inspection.securityStationPos == null) return null;
		final TileEntity tileEntity = level.getBlockEntity(inspection.securityStationPos);
		return tileEntity instanceof SecurityStationTileEntity
			&& ((SecurityStationTileEntity) tileEntity).isEnabled()
			? (SecurityStationTileEntity) tileEntity : null;
	}

	/** No enabled station means unrestricted crew, exactly as in 1.12.2. */
	public boolean isCrewMember(final PlayerEntity player) {
		final SecurityStationTileEntity securityStation = getSecurityStation();
		return securityStation == null || securityStation.isAttachedPlayer(player);
	}

	@Nullable
	public String getFirstOnlineCrew() {
		final SecurityStationTileEntity securityStation = getSecurityStation();
		return securityStation == null ? null : securityStation.getFirstOnlinePlayer();
	}

	@LuaFunction
	public final Object[] setName(String name) {
		if (name == null || name.trim().isEmpty()) {
			return new Object[]{ false, "Name cannot be empty" };
		}

		shipName = name.trim();
		setChanged();
		if (level != null && !level.isClientSide) {
			cr0s.warpdrive.data.GlobalRegionRegistry.updateShip(this);
		}

		WarpDrive.logger.info("Ship renamed to: {}", shipName);

		return new Object[]{ true, String.format("Ship renamed to '%s'", shipName) };
	}

	@LuaFunction
	public final Object[] jump() {
		DebugLog.log("JUMP", "jump() called from thread: {}", Thread.currentThread().getName());

		if (level == null) {
			DebugLog.log("JUMP", "jump() failed: World not loaded");
			return new Object[]{ false, "World not loaded" };
		}

		if (shipState == STATE_COOLDOWN) {
			return new Object[]{ false, String.format("Drive cooling down: %.1fs remaining", cooldownRemaining / 20.0) };
		}
		if (shipState == STATE_COUNTDOWN) {
			return new Object[]{ false, String.format("Jump already counting down: %.1fs", countdownRemaining / 20.0) };
		}
		if (getMovementDistance() < ShipMovementType.MINIMUM_DISTANCE_BLOCKS) {
			return new Object[]{ false, String.format("Movement must be at least %d block",
				ShipMovementType.MINIMUM_DISTANCE_BLOCKS) };
		}

		// Preliminary server-side inspection gives immediate feedback. It is intentionally not trusted
		// for execution: a fresh NBT snapshot is captured after the countdown.
		final ShipScanner.ShipInspection inspection = inspectAssembly(true);
		if (!inspection.success) {
			return new Object[]{ false, inspection.message };
		}

		final int required = calculateEnergyRequired(inspection.blockCount);
		if (energyStored < required) {
			return new Object[]{
				false,
				String.format("Insufficient energy: %d/%d FE", energyStored, required)
			};
		}

		// Validate the destination up front. WarpEngine checks this too, but only once the
		// countdown has elapsed - failing 10 seconds later for something knowable now is a waste
		// of the player's time (and of a chunk force-load).
		final String destinationProblem = validateDestination();
		if (destinationProblem != null) {
			playShipSound(Registration.SOUND_COLLISION.get(), 1.0F, 1.0F);
			DebugLog.log("JUMP", "jump rejected before countdown: {}", destinationProblem);
			return new Object[]{ false, destinationProblem };
		}
		// Force-load the destination before counting down. Placing blocks into an unloaded chunk
		// silently does nothing, which is why the 1.12.2 version pre-loaded the target area first.
		final int chunks = forceDestinationChunks(true);
		if (chunks <= 0) {
			forceDestinationChunks(false);
			return new Object[]{ false, "Unable to load destination chunks safely" };
		}

		// Changing dimensions always gets the full 30-second charge-up/sound. Giant ships use the
		// same window even within one world because their larger destination takes longer to load.
		final boolean changesDimension = transitionTarget() != null
			|| (targetDimension != null && !targetDimension.isEmpty()
				&& !targetDimension.equals(DimensionAltitude.idOf(level)));
		final boolean needsLongCountdown = changesDimension || chunks >= LARGE_SHIP_CHUNK_THRESHOLD;
		final int delay = needsLongCountdown
			? Math.max(jumpDelayTicks, JUMP_DELAY_LARGE_TICKS)
			: jumpDelayTicks;

		shipState = STATE_COUNTDOWN;
		countdownRemaining = delay;
		activeJumpDelayTicks = delay;
		// The clip is not played yet: it starts once the remaining countdown matches its length,
		// so it climaxes exactly at the jump (same as 1.12.2's soundThreshold check).
		warpSoundPlayed = false;
		setChanged();
		syncToClient();
		startCountdownHud(delay);

		DebugLog.log("JUMP", "jump scheduled: {} ticks countdown ({}), {} destination chunks force-loaded",
			delay, changesDimension ? "dimension change"
				: chunks >= LARGE_SHIP_CHUNK_THRESHOLD ? "large ship" : "normal", chunks);

		return new Object[]{ true, String.format("Jump in %.1fs (%d chunks pre-loaded)",
			delay / 20.0, chunks) };
	}

	/**
	 * Runs when the countdown reaches zero. Already on the server thread (called from tick), so
	 * TileEntity access is safe without marshalling.
	 */
	private void executeScheduledJump() {
		// Authoritative final validation. This exact immutable state/NBT snapshot is passed to the
		// mover, so there is no scan-then-recapture window after the countdown.
		final ShipScanner.ShipScanResult dimensionScan =
			ShipScanner.captureShip(level, getBlockPos(), getShipBounds());
		cacheAssemblySnapshot(dimensionScan);
		if (!dimensionScan.success) {
			DebugLog.log("JUMP", "final integrity check failed: {}", dimensionScan.message);
			playShipSound(Registration.SOUND_COLLISION.get(), 1.0F, 1.0F);
			broadcastToOnboard(new StringTextComponent(
				TextFormatting.RED + "âœ– WARP FAILED: " + dimensionScan.message));
			forceDestinationChunks(false);
			beginCooldown();
			return;
		}

		final int volume = dimensionScan.getVolume();
		final int required = calculateEnergyRequired(dimensionScan.getBlockCount());

		if (energyStored < required) {
			DebugLog.log("JUMP", "final snapshot requires more energy than is available ({} < {})",
				energyStored, required);
			broadcastToOnboard(new StringTextComponent(TextFormatting.RED + String.format(
				"âœ– WARP FAILED: insufficient energy after integrity check (%,d/%,d FE)",
				energyStored, required)));
			forceDestinationChunks(false);
			beginCooldown();
			return;
		}
		final String destinationProblem = validateDestination();
		if (destinationProblem != null) {
			playShipSound(Registration.SOUND_COLLISION.get(), 1.0F, 1.0F);
			broadcastToOnboard(new StringTextComponent(
				TextFormatting.RED + "âœ– WARP FAILED: " + destinationProblem));
			forceDestinationChunks(false);
			beginCooldown();
			return;
		}

		boolean movedSuccessfully = false;
		{
			try {
				DebugLog.log("JUMP", "countdown complete, executing on thread: {}", Thread.currentThread().getName());

				// The Lua API stores forward/up/right. Convert through the bow direction only here,
				// matching the 1.12.2 core's Transformation setup.
				final BlockPos worldMovement = getWorldMovement();
				int destX = getBlockPos().getX() + worldMovement.getX();
				int destY = getBlockPos().getY() + worldMovement.getY();
				int destZ = getBlockPos().getZ() + worldMovement.getZ();

				DebugLog.log("JUMP", "Warp jump initiated: {} local F/U/R {}, {}, {} -> world {}, {}, {} to {}, {}, {}",
					shipName, moveX, moveY, moveZ, worldMovement.getX(), worldMovement.getY(),
					worldMovement.getZ(), destX, destY, destZ);
				DebugLog.log("JUMP", "energyStored={} required={} volume={}",
					energyStored, required, volume);

				// Execute warp using WarpEngine
				int energyAfterJump = Math.max(0, energyStored - required);

				DebugLog.log("JUMP", "About to call WarpEngine.executeWarp with energyAfterJump={}", energyAfterJump);
				debugCoreNbt("pre-warp-source", save(new CompoundNBT()));
				// Takeoff / landing: leaving through the ceiling or floor changes dimension, and the
				// ship re-enters through the opposite boundary of the destination
				final RegistryKey<World> transition = transitionTarget();
				final ServerWorld destWorld = transition != null
					? level.getServer().getLevel(transition)
					: resolveDestinationWorld();

				if (transition != null && destWorld != null) {
					final int[] bounds = getShipBounds();
					final BlockPos core = getBlockPos();
					final boolean ascending = bounds[4] + moveY > DimensionAltitude.ceilingOf(level);
					// Offset from the core to the leading hull edge, so the ship clears the boundary
					// it arrives through instead of materialising inside it
					destY = ascending
						? DimensionAltitude.entryAltitudeAscending(destWorld) + (core.getY() - bounds[1])
						: DimensionAltitude.entryAltitudeDescending(destWorld) - (bounds[4] - core.getY());
					DebugLog.log("JUMP", "{} {} to {}, entering at y={}",
						shipName, ascending ? "taking off" : "landing",
						transition.location(), destY);
				}

				if (destWorld == null) {
					DebugLog.log("JUMP", "aborting: destination dimension '{}' did not resolve", targetDimension);
					forceDestinationChunks(false);
					beginCooldown();
					return;
				}
				if (destWorld != level) {
					destX = CelestialCoordinates.mapHorizontal(level, destWorld, destX);
					destZ = CelestialCoordinates.mapHorizontal(level, destWorld, destZ);
				}
				WarpEngine.WarpResult result = WarpEngine.executeWarp(level, destWorld, dimensionScan, destX, destY, destZ, energyAfterJump, rotationSteps);
				DebugLog.log("JUMP", "WarpEngine.executeWarp returned: success={} message='{}'",
					result.success, result.message);

				if (result.success) {
					movedSuccessfully = true;
					// Update destination core directly to preserve all settings
					BlockPos destCorePos = new net.minecraft.util.math.BlockPos(destX, destY, destZ);
					// Must look in the destination world, which may not be this one
					TileEntity teAtDest = destWorld.getBlockEntity(destCorePos);
					if (teAtDest instanceof ShipCoreTileEntity) {
						ShipCoreTileEntity destCore = (ShipCoreTileEntity) teAtDest;
						destCore.energyStored = energyAfterJump;
						// Preserve dimensions, name, and movement
						destCore.dimFront = dimFront;
						destCore.dimBack = dimBack;
						destCore.dimLeft = dimLeft;
						destCore.dimRight = dimRight;
						destCore.dimUp = dimUp;
						destCore.dimDown = dimDown;
						destCore.shipName = shipName;
						destCore.moveX = moveX;
						destCore.moveY = moveY;
						destCore.moveZ = moveZ;
						destCore.showBoundingBox = showBoundingBox;
						// The hull turned, so the bow turned with it
						Direction rotatedFacing = facing;
						for (int turn = 0; turn < rotationSteps; turn++) {
							rotatedFacing = rotatedFacing.getClockWise();
						}
						destCore.facing = rotatedFacing;
						destCore.rotationSteps = rotationSteps;
						destCore.command = command;
						destCore.enabled = false;   // one-shot: do not immediately re-fire
						destCore.targetName = targetName;
						// Clear the target so the arriving ship does not immediately re-target
						destCore.targetDimension = "";
						// Cooldown lives on the ship that actually exists after the jump
						destCore.jumpDelayTicks = jumpDelayTicks;
						destCore.cooldownTicks = cooldownTicks;
						destCore.invalidateAssemblyInspection();
						destCore.debugCoreNbt("post-warp-copy-before-cooldown",
							destCore.save(new CompoundNBT()));
						destCore.beginCooldown();
						destCore.setChanged();
						destCore.syncBlockAppearance();
						destCore.debugCoreNbt("post-warp-copy-final",
							destCore.save(new CompoundNBT()));

						// Announce from the destination core: its bounds now cover the moved crew
						destCore.broadcastToOnboard(new StringTextComponent(
							TextFormatting.GREEN + "✦ WARP COMPLETE"));

						// Sync to client so bounding boxes can update
						destWorld.sendBlockUpdated(destCorePos, destCore.getBlockState(), destCore.getBlockState(), 3);

						DebugLog.log("JUMP", "Updated destination Ship Core at {}", destCorePos);
					} else {
						WarpDrive.logger.error("Warp succeeded but destination Ship Core TileEntity missing at {}", destCorePos);
						DebugLog.log("JUMP", "ERROR: destination Ship Core TileEntity missing at {}", destCorePos);
					}

					// Clear old block data at original position (so client knows core moved)
					level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), level.getBlockState(getBlockPos()), 3);

					DebugLog.log("JUMP", "{} (Used {} FE)", result.message, required);
				} else {
					WarpDrive.logger.warn("Warp failed: {}", result.message);
					// Collision or out-of-bounds: the original mod had a sound for exactly this
					playShipSound(Registration.SOUND_COLLISION.get(), 1.0F, 1.0F);
					broadcastToOnboard(new StringTextComponent(
						TextFormatting.RED + "✖ WARP FAILED: " + result.message));
				}
			} catch (Exception e) {
				WarpDrive.logger.error("Exception during jump", e);
				DebugLog.log("JUMP", "EXCEPTION during jump: {}", e);
			}
		}

		// Release the force-load: the destination is occupied now, so normal chunk rules apply
		forceDestinationChunks(false);

		// If the origin core still exists (jump failed), cool down here instead
		if (!movedSuccessfully && shipState != STATE_COOLDOWN) {
			beginCooldown();
		}
	}

	/**
	 * Send an action bar message to every player currently inside the hull.
	 *
	 * Uses the same bounds as the jump itself, so the players who get warned are exactly the
	 * players who will be moved.
	 */
	private void broadcastToOnboard(final ITextComponent message) {
		if (level == null || level.isClientSide) {
			return;
		}
		final int[] b = getShipBounds();
		final AxisAlignedBB box = new AxisAlignedBB(b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1);
		for (final PlayerEntity player : level.getEntitiesOfClass(PlayerEntity.class, box)) {
			player.displayClientMessage(message, true);   // true = action bar, above the hotbar
		}
	}

	/** Send one authoritative end tick; clients animate the HUD locally until that tick. */
	private void startCountdownHud(final int delayTicks) {
		if (!(level instanceof ServerWorld)) {
			return;
		}
		countdownHudRecipients.clear();
		final long endTick = level.getGameTime() + delayTicks;
		final int[] b = getShipBounds();
		final AxisAlignedBB box = new AxisAlignedBB(b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1);
		for (final PlayerEntity player : level.getEntitiesOfClass(PlayerEntity.class, box)) {
			if (player instanceof ServerPlayerEntity) {
				final ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
				countdownHudRecipients.add(serverPlayer.getUUID());
				WarpDriveNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
					ShipCountdownPacket.start(getBlockPos(), endTick));
			}
		}
	}

	/** Cancel the HUD for the original recipients even if they have since stepped off the ship. */
	private void stopCountdownHud() {
		if (level instanceof ServerWorld) {
			for (final UUID uuid : countdownHudRecipients) {
				final ServerPlayerEntity player = level.getServer().getPlayerList().getPlayer(uuid);
				if (player != null) {
					WarpDriveNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
						ShipCountdownPacket.stop(getBlockPos()));
				}
			}
		}
		countdownHudRecipients.clear();
	}

	/** Play one of the mod's sounds at the core, audible to everyone nearby. */
	private void playShipSound(final SoundEvent sound, final float volume, final float pitch) {
		if (level == null || level.isClientSide || sound == null) {
			return;
		}
		level.playSound(null, getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
	}

	/**
	 * Mirrors 1.12.2 TileEntityShipCore: warmup under 10s uses warp_4s, over 29s uses warp_30s,
	 * everything between uses warp_10s. In practice 10s is the common case and 30s is for giant
	 * ships, since warmup scales with the ship.
	 */
	private SoundEvent selectWarpSound(final int delayTicks) {
		final int seconds = delayTicks / 20;
		if (seconds < 10) {
			return Registration.SOUND_WARP_4S.get();
		}
		if (seconds > 29) {
			return Registration.SOUND_WARP_30S.get();
		}
		return Registration.SOUND_WARP_10S.get();
	}

	/** Length of the selected clip, in ticks: the sound starts this long before the jump. */
	private int warpSoundLengthTicks(final int delayTicks) {
		final int seconds = delayTicks / 20;
		if (seconds < 10) {
			return 4 * 20;
		}
		if (seconds > 29) {
			return 30 * 20;
		}
		return 10 * 20;
	}

	/**
	 * The dimension this jump would move into by leaving through the ceiling or floor, or null if
	 * it stays inside the current world.
	 *
	 * This is 1.12.2's PLANET_TAKEOFF / PLANET_LANDING expressed as a property of the movement
	 * rather than a mode the pilot selects: fly far enough up and you leave the atmosphere, far
	 * enough down and you re-enter it.
	 *
	 * An explicit destination always wins - asking for a specific dimension means the pilot has
	 * already decided, and silently redirecting them would be worse than refusing.
	 */
	@Nullable
	private RegistryKey<World> transitionTarget() {
		if (level == null || (targetDimension != null && !targetDimension.isEmpty())) {
			return null;
		}
		final int[] b = getShipBounds();
		if (b[1] + moveY < DimensionAltitude.floorOf(level)) {
			return DimensionAltitude.below(level);
		}
		if (b[4] + moveY > DimensionAltitude.ceilingOf(level)) {
			return DimensionAltitude.above(level);
		}
		return null;
	}

	/**
	 * Cheap pre-flight check of where the ship would land. Returns null when the destination looks
	 * usable, otherwise a message explaining why not.
	 */
	@Nullable
	private String validateDestination() {
		if (level == null) {
			return "World not loaded";
		}
		final int[] b = getShipBounds();
		final BlockPos core = getBlockPos();
		final RegistryKey<World> transition = transitionTarget();
		final ServerWorld destinationWorld = transition != null && level.getServer() != null
			? level.getServer().getLevel(transition)
			: resolveDestinationWorld();
		if (destinationWorld == null) {
			return "Unknown destination dimension: "
				+ (transition == null ? targetDimension : transition.location());
		}

		// A boundary transition does not retain its out-of-range source Y. It enters through the
		// opposite edge of the destination world, using the same core altitude calculation as the
		// actual jump below. Pre-flight must validate that mapped position or isInWorldBounds() will
		// reject the deliberately out-of-range source Y before the countdown can even start.
		final int destinationCoreY;
		if (transition != null) {
			final boolean ascending = b[4] + moveY > DimensionAltitude.ceilingOf(level);
			destinationCoreY = ascending
				? DimensionAltitude.entryAltitudeAscending(destinationWorld) + (core.getY() - b[1])
				: DimensionAltitude.entryAltitudeDescending(destinationWorld) - (b[4] - core.getY());
		} else {
			destinationCoreY = core.getY() + moveY;
		}
		final int destMinY = destinationCoreY + b[1] - core.getY();
		final int destMaxY = destinationCoreY + b[4] - core.getY();

		// Leaving through the ceiling or the floor is a dimension transition rather than an error -
		// takeoff and landing, in 1.12.2 terms - provided there is a world on the other side and
		// the pilot has not asked for a specific destination.
		if (destMinY < DimensionAltitude.floorOf(destinationWorld)) {
			return String.format("Destination below the world (y %d)", destMinY);
		}
		if (destMaxY > DimensionAltitude.ceilingOf(destinationWorld)) {
			return String.format("Destination above the build limit (y %d > %d)",
				destMaxY, DimensionAltitude.ceilingOf(destinationWorld));
		}

		// Horizontal extent, honouring rotation, against the world border
		final BlockPos worldMovement = getWorldMovement();
		final int rawDestX = core.getX() + worldMovement.getX();
		final int rawDestZ = core.getZ() + worldMovement.getZ();
		final int destX = destinationWorld == level ? rawDestX
			: CelestialCoordinates.mapHorizontal(level, destinationWorld, rawDestX);
		final int destZ = destinationWorld == level ? rawDestZ
			: CelestialCoordinates.mapHorizontal(level, destinationWorld, rawDestZ);
		final int[] offsetsX = { b[0] - core.getX(), b[3] - core.getX() };
		final int[] offsetsZ = { b[2] - core.getZ(), b[5] - core.getZ() };
		for (final int ox : offsetsX) {
			for (final int oz : offsetsZ) {
				int rx = ox;
				int rz = oz;
				switch (((rotationSteps % 4) + 4) % 4) {
					case 1: rx = -oz; rz = ox;  break;
					case 2: rx = -ox; rz = -oz; break;
					case 3: rx = oz;  rz = -ox; break;
					default: break;
				}
				if (!destinationWorld.isInWorldBounds(new BlockPos(destX + rx, destMinY, destZ + rz))) {
					return "Destination is outside the world";
				}
			}
		}
		return null;
	}

	private void beginCooldown() {
		shipState = STATE_COOLDOWN;
		cooldownRemaining = cooldownTicks;
		countdownRemaining = 0;
		// The drive venting heat after a jump
		playShipSound(Registration.SOUND_CHILLER.get(), 0.7F, 1.0F);
		setChanged();
		syncToClient();
	}

	/**
	 * Force-load (or release) every chunk the ship will occupy at the destination, including
	 * rotation. Returns the number of chunks affected.
	 */
	private int forceDestinationChunks(final boolean add) {
		if (!(level instanceof ServerWorld)) {
			return 0;
		}

		// Release must target the world we actually forced, which is not necessarily this one:
		// after a cross-dimension jump `level` is the destination while the forced chunks may
		// belong to the origin.
		if (!add) {
			if (forcedChunksWorld == null || forcedChunks.isEmpty()) {
				forcedChunks.clear();
				return 0;
			}
			final ServerWorld forcedWorld = ((ServerWorld) level).getServer().getLevel(forcedChunksWorld);
			int released = 0;
			if (forcedWorld != null) {
				for (final Long packed : forcedChunks) {
					final ChunkPos chunkPos = new ChunkPos(packed);
					forcedWorld.setChunkForced(chunkPos.x, chunkPos.z, false);
					released++;
				}
			} else {
				DebugLog.log("JUMP", "cannot release forced chunks: world {} is gone", forcedChunksWorld.location());
			}
			forcedChunks.clear();
			forcedChunksWorld = null;
			return released;
		}

		final RegistryKey<World> transition = transitionTarget();
		final ServerWorld serverWorld = transition == null
			? resolveDestinationWorld()
			: ((ServerWorld) level).getServer().getLevel(transition);
		if (serverWorld == null) {
			DebugLog.log("JUMP", "cannot force chunks: destination dimension '{}' did not resolve",
				transition == null ? targetDimension : transition.location());
			return 0;
		}
		forcedChunksWorld = serverWorld.dimension();

		final int[] bounds = getShipBounds();
		final BlockPos core = getBlockPos();
		final BlockPos worldMovement = getWorldMovement();
		final int rawDestX = core.getX() + worldMovement.getX();
		final int rawDestZ = core.getZ() + worldMovement.getZ();
		final int destX = serverWorld == level ? rawDestX
			: CelestialCoordinates.mapHorizontal(level, serverWorld, rawDestX);
		final int destZ = serverWorld == level ? rawDestZ
			: CelestialCoordinates.mapHorizontal(level, serverWorld, rawDestZ);

		// Rotate the four horizontal corners so a turned ship still covers the right chunks
		final int[] offsetsX = { bounds[0] - core.getX(), bounds[3] - core.getX() };
		final int[] offsetsZ = { bounds[2] - core.getZ(), bounds[5] - core.getZ() };
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (final int ox : offsetsX) {
			for (final int oz : offsetsZ) {
				int rx = ox;
				int rz = oz;
				switch (((rotationSteps % 4) + 4) % 4) {
					case 1: rx = -oz; rz = ox;  break;
					case 2: rx = -ox; rz = -oz; break;
					case 3: rx = oz;  rz = -ox; break;
					default: break;
				}
				minX = Math.min(minX, destX + rx);
				maxX = Math.max(maxX, destX + rx);
				minZ = Math.min(minZ, destZ + rz);
				maxZ = Math.max(maxZ, destZ + rz);
			}
		}

		// One chunk of margin, so blocks on a chunk border are not placed against unloaded terrain
		final int fromX = (minX >> 4) - 1;
		final int toX = (maxX >> 4) + 1;
		final int fromZ = (minZ >> 4) - 1;
		final int toZ = (maxZ >> 4) + 1;

		final long total = (long) (toX - fromX + 1) * (toZ - fromZ + 1);
		if (total > MAX_FORCED_CHUNKS) {
			DebugLog.log("JUMP", "refusing to force-load {} chunks (limit {})", total, MAX_FORCED_CHUNKS);
			return 0;
		}

		for (int cx = fromX; cx <= toX; cx++) {
			for (int cz = fromZ; cz <= toZ; cz++) {
				serverWorld.setChunkForced(cx, cz, true);
				forcedChunks.add(ChunkPos.asLong(cx, cz));
			}
		}
		return forcedChunks.size();
	}

	// ===== Bounding Box Visualization =====

	public void toggleBoundingBoxDisplay(PlayerEntity player) {
		// Block.use() fires on both sides. Handle it server-side only: the flag lives in NBT so it
		// survives a jump, and the client picks it up through the normal sync path.
		if (level == null || level.isClientSide) {
			return;
		}

		if (getShipVolume() <= 1) {
			player.displayClientMessage(
				new StringTextComponent(TextFormatting.RED + "No ship dimensions set! Use setDimensions() first."),
				false
			);
			return;
		}

		showBoundingBox = !showBoundingBox;
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);

		if (!showBoundingBox) {
			player.displayClientMessage(
				new StringTextComponent(TextFormatting.YELLOW + "Bounding boxes hidden"),
				false
			);
		} else if (moveX != 0 || moveY != 0 || moveZ != 0) {
			player.displayClientMessage(
				new StringTextComponent(TextFormatting.GREEN + "Ship (green) " +
					TextFormatting.AQUA + "Destination (cyan)"),
				false
			);
		} else {
			player.displayClientMessage(
				new StringTextComponent(TextFormatting.GREEN + "Ship bounds (green) " +
					TextFormatting.GRAY + "[Set movement to see destination]"),
				false
			);
		}
	}

	// ===== Energy Capability =====

	private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = Math.min(getMaximumEnergyCapacity() - energyStored,
				Math.min(maxReceive, MAX_TRANSFER));
			if (!simulate) {
				energyStored += received;
				setChanged();
			}
			return received;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0; // Ship core doesn't provide energy
		}

		@Override
		public int getEnergyStored() {
			return energyStored;
		}

		@Override
		public int getMaxEnergyStored() {
			return getMaximumEnergyCapacity();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	});

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityEnergy.ENERGY) {
			return energyHandler.cast();
		}

		return super.getCapability(cap, side);
	}

	@Override
	protected void invalidateCaps() {
		super.invalidateCaps();
		energyHandler.invalidate();
	}

	// ===== NBT Serialization =====

	@Override
	public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
		super.load(state, nbt);
		energyStored = Math.max(0, Math.min(getMaximumEnergyCapacity(state), nbt.getInt("Energy")));
		dimFront = nbt.getInt("DimFront");
		dimBack = nbt.getInt("DimBack");
		dimLeft = nbt.getInt("DimLeft");
		dimRight = nbt.getInt("DimRight");
		dimUp = nbt.getInt("DimUp");
		dimDown = nbt.getInt("DimDown");
		shipName = nbt.getString("ShipName");
		if (shipName.isEmpty()) {
			shipName = "Unnamed Ship";
		}
		signatureUuid = nbt.hasUUID("SignatureUUID") ? nbt.getUUID("SignatureUUID") : UUID.randomUUID();
		showBoundingBox = nbt.getBoolean("ShowBoundingBox");
		// Ships saved before facing existed used the fixed +Z mapping, which is SOUTH
		final int facingIndex = nbt.contains("Facing") ? nbt.getInt("Facing") : Direction.SOUTH.get3DDataValue();
		facing = Direction.from3DDataValue(facingIndex);
		if (facing.getAxis().isVertical()) {
			facing = Direction.SOUTH;
		}
		if (nbt.contains("MoveX")) {
			final int savedX = nbt.getInt("MoveX");
			moveY = nbt.getInt("MoveY");
			final int savedZ = nbt.getInt("MoveZ");
			if (nbt.getInt("CoreDataVersion") < 2) {
				// Version 1 accidentally stored world X/Y/Z. Invert the legacy facing transform once
				// so existing 1.16 ships keep the same destination after this correction.
				moveX = facing.getStepX() * savedX + facing.getStepZ() * savedZ;
				moveZ = -facing.getStepZ() * savedX + facing.getStepX() * savedZ;
			} else {
				moveX = savedX;
				moveZ = savedZ;
			}
		} else {
			// Directly imported 1.12.2 NBT already uses ship-local field names.
			moveX = nbt.getInt("moveFront");
			moveY = nbt.getInt("moveUp");
			moveZ = nbt.getInt("moveRight");
		}
		rotationSteps = ((nbt.getInt("RotationSteps") % 4) + 4) % 4;
		command = nbt.contains("Command") ? nbt.getString("Command") : "MANUAL";
		// Builds before CORE_DATA_VERSION accidentally defaulted cores to disabled. Upgrade those
		// saves once so existing placed cores regain the 1.12.2 online/animated appearance.
		enabled = nbt.contains("CoreDataVersion")
		        ? nbt.getBoolean("Enabled")
		        : !"OFFLINE".equals(command);
		targetName = nbt.getString("TargetName");
		targetDimension = nbt.getString("TargetDimension");
		shipState = nbt.getInt("ShipState");
		countdownRemaining = nbt.getInt("CountdownRemaining");
		cooldownRemaining = nbt.getInt("CooldownRemaining");
		jumpDelayTicks = nbt.contains("JumpDelayTicks") ? nbt.getInt("JumpDelayTicks") : JUMP_DELAY_NORMAL_TICKS;
		cooldownTicks = nbt.contains("CooldownTicks") ? nbt.getInt("CooldownTicks") : 200;
		// A countdown cannot survive a reload: its force-loaded chunks are gone, so drop back to idle
		if (shipState == STATE_COUNTDOWN) {
			shipState = STATE_IDLE;
			countdownRemaining = 0;
		}
		invalidateAssemblyInspection();
		debugCoreNbt("load-applied", nbt);
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull CompoundNBT nbt) {
		super.save(nbt);
		nbt.putInt("Energy", energyStored);
		nbt.putInt("DimFront", dimFront);
		nbt.putInt("DimBack", dimBack);
		nbt.putInt("DimLeft", dimLeft);
		nbt.putInt("DimRight", dimRight);
		nbt.putInt("DimUp", dimUp);
		nbt.putInt("DimDown", dimDown);
		nbt.putString("ShipName", shipName);
		nbt.putUUID("SignatureUUID", signatureUuid);
		nbt.putInt("MoveX", moveX);
		nbt.putInt("MoveY", moveY);
		nbt.putInt("MoveZ", moveZ);
		nbt.putBoolean("ShowBoundingBox", showBoundingBox);
		nbt.putInt("Facing", facing.get3DDataValue());
		nbt.putInt("RotationSteps", rotationSteps);
		nbt.putString("Command", command);
		nbt.putBoolean("Enabled", enabled);
		nbt.putInt("CoreDataVersion", CORE_DATA_VERSION);
		nbt.putString("TargetName", targetName);
		nbt.putString("TargetDimension", targetDimension);
		nbt.putInt("ShipState", shipState);
		nbt.putInt("CountdownRemaining", countdownRemaining);
		nbt.putInt("CooldownRemaining", cooldownRemaining);
		nbt.putInt("JumpDelayTicks", jumpDelayTicks);
		nbt.putInt("CooldownTicks", cooldownTicks);
		return nbt;
	}

	// ===== Client Sync =====

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		// Initial sync when the chunk is sent to the client
		final CompoundNBT nbt = save(new CompoundNBT());
		debugCoreNbt("getUpdateTag-out", nbt);
		return nbt;
	}

	// getUpdateTag/handleUpdateTag only cover the initial chunk load. Without these two,
	// level.sendBlockUpdated(...) sends nothing, so the client keeps stale dimensions and
	// shift+right-click reports "No ship dimensions set" even after setDimensions() succeeded
	// server-side.
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		final CompoundNBT nbt = save(new CompoundNBT());
		debugCoreNbt("getUpdatePacket-out", nbt);
		return new SUpdateTileEntityPacket(worldPosition, 1, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		debugCoreNbt("onDataPacket-in", pkt.getTag());
		load(getBlockState(), pkt.getTag());
		debugCoreNbt("onDataPacket-applied", pkt.getTag());
		refreshRendererTracking();
	}

	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT nbt) {
		// Receive data on client (initial chunk load, and the destination core after a jump)
		debugCoreNbt("handleUpdateTag-in", nbt);
		load(state, nbt);
		debugCoreNbt("handleUpdateTag-applied", nbt);
		refreshRendererTracking();
	}

	@Override
	public void setRemoved() {
		debugCoreNbt("setRemoved", null);
		if (level != null && !level.isClientSide
		 && !(level.getBlockState(getBlockPos()).getBlock() instanceof ShipCoreBlock)) {
			cr0s.warpdrive.data.GlobalRegionRegistry.removeShip(this);
		} else {
			cr0s.warpdrive.data.GlobalRegionRegistry.unregisterShip(this);
		}
		// Drop the box when the core is broken or moved away by a jump
		if (level != null && level.isClientSide) {
			BoundingBoxRenderer.untrack(getBlockPos());
		} else if (shipState == STATE_COUNTDOWN) {
			stopCountdownHud();
		}
		super.setRemoved();
	}
}
