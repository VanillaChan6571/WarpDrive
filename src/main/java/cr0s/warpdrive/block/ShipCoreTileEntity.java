package cr0s.warpdrive.block;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.debug.DebugLog;
import cr0s.warpdrive.render.BoundingBoxRenderer;
import cr0s.warpdrive.ship.ShipScanner;
import cr0s.warpdrive.ship.WarpEngine;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.Capabilities;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Ship Core TileEntity - Handles ship logic and ComputerCraft integration
 *
 * Implements:
 * - IEnergyStorage for Forge Energy
 * - Provides IPeripheral via capability for CC:Tweaked
 */
public class ShipCoreTileEntity extends TileEntity implements ITickableTileEntity {

	// Energy storage
	private int energyStored = 0;
	private static final int MAX_ENERGY = 10_000_000; // 10M FE
	private static final int MAX_TRANSFER = 10_000;    // 10K FE/t

	// Ship dimensions (classic WarpDrive 1.12.2 style), relative to `facing`
	private int dimFront = 0;
	private int dimBack = 0;
	private int dimLeft = 0;
	private int dimRight = 0;
	private int dimUp = 0;
	private int dimDown = 0;
	private String shipName = "Unnamed Ship";

	// Which way the ship's bow points. SOUTH reproduces the original hard-coded mapping exactly
	// (front=+Z, back=-Z, right=+X, left=-X), so existing ships keep their current bounds.
	private Direction facing = Direction.SOUTH;

	// Yaw applied on jump: 0..3 quarter turns clockwise
	private int rotationSteps = 0;

	// Ship command state, mirroring the 1.12.2 controller's model
	private String command = "MANUAL";
	private boolean enabled = false;
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
	private int cooldownRemaining = 0;
	private int jumpDelayTicks = JUMP_DELAY_NORMAL_TICKS;
	/** Delay actually in use for the current countdown (may be escalated for dimension/ship size). */
	private int activeJumpDelayTicks = JUMP_DELAY_NORMAL_TICKS;
	private boolean warpSoundPlayed = false;

	/** Client-side only: last state we logged, to avoid a SYNC line every second. */
	private String lastLoggedSyncSignature = "";
	private int cooldownTicks = 200;    // 10s
	private final Set<Long> forcedChunks = new HashSet<>();
	/** Which world `forcedChunks` belongs to - not necessarily this tile entity's world. */
	private RegistryKey<World> forcedChunksWorld = null;

	// Movement (relative, not absolute destination)
	private int moveX = 0;
	private int moveY = 0;
	private int moveZ = 0;

	// Whether the bounding boxes are displayed. Server-authoritative and stored in NBT so the
	// setting survives a jump; the client renders straight off this + the fields above, which
	// means movement edits show up immediately without re-toggling.
	private boolean showBoundingBox = false;

	public ShipCoreTileEntity() {
		super(Registration.SHIP_CORE_TILE.get());
	}

	// Ticking is back (it was removed with the creative auto-charge) because the jump countdown and
	// post-jump cooldown genuinely need per-tick work. Power still comes from real energy blocks.
	@Override
	public void tick() {
		if (level == null || level.isClientSide) {
			return;
		}

		if (shipState == STATE_COUNTDOWN) {
			countdownRemaining--;

			// Start the charge-up so it ends as the ship moves
			if (!warpSoundPlayed && countdownRemaining <= warpSoundLengthTicks(activeJumpDelayTicks)) {
				warpSoundPlayed = true;
				// 1.12.2 used volume 4.0 - deliberately above 1.0 to widen the audible radius
				playShipSound(selectWarpSound(activeJumpDelayTicks), 4.0F, 1.0F);
			}

			// Action bar countdown for anyone aboard. Refreshed twice a second: often enough to
			// tick down smoothly, and well inside the ~3s the action bar stays visible.
			if (countdownRemaining > 0 && countdownRemaining % 10 == 0) {
				final double seconds = countdownRemaining / 20.0;
				final TextFormatting colour = seconds <= 3.0 ? TextFormatting.RED
					: seconds <= 5.0 ? TextFormatting.GOLD : TextFormatting.YELLOW;
				broadcastToOnboard(new StringTextComponent(
					colour + String.format("⚠ WARP IN %.1fs", seconds)));
			}

			if (countdownRemaining <= 0) {
				countdownRemaining = 0;
				shipState = STATE_IDLE;
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
	public final Object[] setDimensions(int front, int back, int left, int right, int up, int down) throws LuaException {
		// Validate dimensions
		if (front < 0 || back < 0 || left < 0 || right < 0 || up < 0 || down < 0) {
			return new Object[]{ false, "All dimensions must be >= 0" };
		}

		// Set dimensions
		dimFront = front;
		dimBack = back;
		dimLeft = left;
		dimRight = right;
		dimUp = up;
		dimDown = down;
		setChanged();

		// Sync to client
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}

		// Calculate total volume
		int volume = (front + back + 1) * (left + right + 1) * (up + down + 1);

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

	@LuaFunction
	public final int getEnergyRequired() {
		return calculateEnergyRequired();
	}

	/** Legacy: local shipMass, shipVolume = ship.getShipSize() - mass is the solid block count. */
	@LuaFunction
	public final Object[] getShipSize() {
		return new Object[]{ getShipMass(), getShipVolume() };
	}

	/** Number of non-air blocks inside the envelope. */
	public int getShipMass() {
		if (level == null || getShipVolume() <= 1) {
			return 0;
		}
		final int[] b = getShipBounds();
		int mass = 0;
		for (int y = b[1]; y <= b[4]; y++) {
			for (int x = b[0]; x <= b[3]; x++) {
				for (int z = b[2]; z <= b[5]; z++) {
					if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
						mass++;
					}
				}
			}
		}
		return mass;
	}

	@LuaFunction
	public final Object[] addEnergy(int amount) throws LuaException {
		if (amount <= 0) {
			return new Object[]{ false, "Amount must be positive" };
		}

		int added = Math.min(MAX_ENERGY - energyStored, amount);
		energyStored += added;
		setChanged();

		WarpDrive.logger.info("Added {} FE to ship core (now: {} FE)", added, energyStored);

		return new Object[]{ true, String.format("Added %,d FE (Total: %,d FE)", added, energyStored) };
	}

	@LuaFunction
	public final Object[] setEnergy(int amount) throws LuaException {
		if (amount < 0 || amount > MAX_ENERGY) {
			return new Object[]{ false, String.format("Amount must be 0-%,d", MAX_ENERGY) };
		}

		energyStored = amount;
		setChanged();

		WarpDrive.logger.info("Set ship core energy to {} FE", energyStored);

		return new Object[]{ true, String.format("Energy set to %,d FE", energyStored) };
	}

	private int calculateEnergyRequired() {
		if (level == null) {
			return 0;
		}

		// Calculate ship volume from dimensions
		int volume = (dimFront + dimBack + 1) * (dimLeft + dimRight + 1) * (dimUp + dimDown + 1);

		// Calculate movement distance
		double distance = Math.sqrt(moveX*moveX + moveY*moveY + moveZ*moveZ);

		// Energy formula: volume * 100 + distance * 10
		int baseCost = volume * 100;
		int distanceCost = (int)(distance * 10);

		return baseCost + distanceCost;
	}

	private int getShipVolume() {
		return (dimFront + dimBack + 1) * (dimLeft + dimRight + 1) * (dimUp + dimDown + 1);
	}

	public boolean isBoundingBoxShown() {
		return showBoundingBox;
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
	public final Object[] setOrientation(final String direction) throws LuaException {
		final Direction parsed = Direction.byName(direction == null ? "" : direction.toLowerCase());
		if (parsed == null || parsed.getAxis().isVertical()) {
			return new Object[]{ false, "Orientation must be north, south, east or west" };
		}
		facing = parsed;
		setChanged();
		syncToClient();
		return new Object[]{ true, "Bow now faces " + facing };
	}

	/** Legacy: ship.rotationSteps() to read, ship.rotationSteps(n) to set. 0..3 quarter turns. */
	@LuaFunction
	public final Object[] rotationSteps(final Optional<Integer> steps) throws LuaException {
		if (steps.isPresent()) {
			rotationSteps = ((steps.get() % 4) + 4) % 4;
			setChanged();
			syncToClient();
		}
		return new Object[]{ rotationSteps };
	}

	/** Legacy: local energyStored, energyMax, energyUnits = ship.getEnergyStatus() */
	@LuaFunction
	public final Object[] getEnergyStatus() {
		return new Object[]{ energyStored, MAX_ENERGY, "FE" };
	}

	/** Legacy: local success, maxJumpDistance = ship.getMaxJumpDistance() */
	@LuaFunction
	public final Object[] getMaxJumpDistance() {
		final int volume = getShipVolume();
		if (volume <= 1) {
			return new Object[]{ false, 0, 0, "dimensions not set" };
		}
		final int byType = getMaxJumpDistanceByType();
		final int byEnergy = getMaxJumpDistanceByEnergy();
		final int effective = Math.min(byType, byEnergy);
		// Naming the binding constraint is the point: "48 blocks" is not actionable, but "48,
		// limited by energy" tells the pilot to charge rather than to rebuild the ship
		final String limitedBy = byEnergy < byType ? "energy" : getMovementType().getName();

		return new Object[]{
			effective >= ShipMovementType.MINIMUM_DISTANCE_BLOCKS,
			effective,
			ShipMovementType.MINIMUM_DISTANCE_BLOCKS,
			limitedBy };
	}

	/** Range ceiling imposed by the kind of movement and the ship's mass. */
	public int getMaxJumpDistanceByType() {
		return getMovementType().maximumDistance(getShipMass());
	}

	/** Range ceiling imposed by stored energy - the inverse of cost = volume * 100 + distance * 10. */
	public int getMaxJumpDistanceByEnergy() {
		final int volume = getShipVolume();
		if (volume <= 1) {
			return 0;
		}
		return Math.max(0, (energyStored - volume * 100) / 10);
	}

	/** Whichever ceiling binds first. */
	public int getEffectiveMaxJumpDistance() {
		return Math.min(getMaxJumpDistanceByType(), getMaxJumpDistanceByEnergy());
	}

	/** Legacy: isValid, message = ship.getAssemblyStatus() */
	@LuaFunction
	public final Object[] getAssemblyStatus() {
		if (getShipVolume() <= 1) {
			return new Object[]{ false, "Dimensions not set" };
		}
		if (level == null) {
			return new Object[]{ false, "No world" };
		}
		final int[] b = getShipBounds();
		if (b[1] < 0 || b[4] > 255) {
			return new Object[]{ false, "Ship extends outside the world height" };
		}
		int solid = 0;
		for (int y = b[1]; y <= b[4]; y++) {
			for (int x = b[0]; x <= b[3]; x++) {
				for (int z = b[2]; z <= b[5]; z++) {
					if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
						solid++;
					}
				}
			}
		}
		return new Object[]{ true, String.format("Valid - %d blocks in a %d block envelope", solid, getShipVolume()) };
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
		return new Object[]{ true };
	}

	/**
	 * Legacy: ship.isInSpace() / ship.isInHyperspace().
	 *
	 * TODO(Phase 4): neither the Space nor the Hyperspace dimension has been implemented yet.
	 * These compare against the intended dimension ids, so they answer false today and start
	 * answering correctly the moment the dimensions are registered - no call site needs changing.
	 * Blocked work: dimension registration, void chunk generator, asteroid feature/placement,
	 * vacuum handling, and cross-dimension jumps in WarpEngine (which currently moves blocks
	 * within a single World only).
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
	public final Object[] command(final String newCommand, final Optional<Boolean> doEnable) throws LuaException {
		if (newCommand != null && !newCommand.isEmpty()) {
			command = newCommand.toUpperCase();
		}
		if (doEnable.isPresent()) {
			enabled = doEnable.get();
		}
		DebugLog.log("SHIP", "command set to {} (enabled={})", command, enabled);
		setChanged();
		return new Object[]{ command, enabled };
	}

	/** Legacy: ship.enable(true) starts the currently selected command. */
	@LuaFunction
	public final Object[] enable(final boolean value) {
		enabled = value;
		setChanged();
		DebugLog.log("SHIP", "enable({}) with command={}", value, command);
		if (value && "MANUAL".equals(command)) {
			return jump();
		}
		if (value) {
			return new Object[]{ false, "Command '" + command + "' is not implemented yet" };
		}
		return new Object[]{ true, "Disabled" };
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

	/** Legacy: front, right, up = ship.dim_positive() */
	@LuaFunction
	public final Object[] dim_positive() {
		return new Object[]{ dimFront, dimRight, dimUp };
	}

	/** Legacy: back, left, down = ship.dim_negative() */
	@LuaFunction
	public final Object[] dim_negative() {
		return new Object[]{ dimBack, dimLeft, dimDown };
	}

	/** Legacy: ship.movement() to read, ship.movement(x, y, z) to set. */
	@LuaFunction
	public final Object[] movement(final Optional<Integer> x, final Optional<Integer> y, final Optional<Integer> z)
		throws LuaException {
		if (x.isPresent() && y.isPresent() && z.isPresent()) {
			return setMovement(x.get(), y.get(), z.get());
		}
		return new Object[]{ moveX, moveY, moveZ };
	}

	/** Legacy: ship.name() to read, ship.name("x") to set. */
	@LuaFunction
	public final Object[] name(final Optional<String> newName) throws LuaException {
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
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	// ===== Debug reporting =====

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
		report.append("energyStored  : ").append(energyStored).append(" / ").append(MAX_ENERGY).append('\n');
		report.append("energyNeeded  : ").append(calculateEnergyRequired()).append('\n');
		report.append("showBoundBox  : ").append(showBoundingBox).append('\n');
		report.append("peripheralCap : ").append(peripheralCap == null ? "not resolved" : "present").append('\n');
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
		extend(lo, hi, facing.getCounterClockWise(), dimRight);
		extend(lo, hi, facing.getClockWise(), dimLeft);

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
		double[] b = getShipBoxBounds();
		return new double[]{
			b[0] + moveX, b[1] + moveY, b[2] + moveZ,
			b[3] + moveX, b[4] + moveY, b[5] + moveZ
		};
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
	public final Object[] setMovement(int dx, int dy, int dz) throws LuaException {
		// Set the requested movement first: the movement type - and therefore the range - depends on
		// where this jump would end up, so takeoff and landing can only be recognised once it is known
		moveX = dx;
		moveY = dy;
		moveZ = dz;

		final ShipMovementType movementType = getMovementType();
		final int byType = getMaxJumpDistanceByType();
		final int byEnergy = getMaxJumpDistanceByEnergy();
		final int maximum = Math.min(byType, byEnergy);
		final String limitedBy = byEnergy < byType ? "energy" : movementType.getName();
		final double requested = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);

		String note = "";
		if (requested > maximum) {
			// Scale the whole vector rather than clipping each axis, so the heading is preserved and
			// the ship still travels the way the pilot pointed it
			final double scale = maximum / requested;
			moveX = (int) Math.round(dx * scale);
			moveY = (int) Math.round(dy * scale);
			moveZ = (int) Math.round(dz * scale);
			note = String.format(" (clamped from %d, limited by %s)",
				(int) Math.round(requested), limitedBy);
		} else if (requested < ShipMovementType.MINIMUM_DISTANCE_BLOCKS) {
			return new Object[]{ false, String.format(
				"Movement too small: at least %d block required, up to %d (%s)",
				ShipMovementType.MINIMUM_DISTANCE_BLOCKS, maximum, limitedBy) };
		}

		setChanged();

		// Sync to client so the destination bounding box can be drawn
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}

		WarpDrive.logger.info("Movement set to: {}, {}, {} ({}, max {})",
			moveX, moveY, moveZ, movementType.getName(), maximum);

		return new Object[]{ true, String.format("Movement set: %d, %d, %d%s - %s, range %d to %d blocks",
			moveX, moveY, moveZ, note, movementType.getName(),
			ShipMovementType.MINIMUM_DISTANCE_BLOCKS, maximum), maximum, limitedBy };
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

	@LuaFunction
	public final Object[] setName(String name) throws LuaException {
		if (name == null || name.trim().isEmpty()) {
			return new Object[]{ false, "Name cannot be empty" };
		}

		shipName = name.trim();
		setChanged();

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

		// Check if dimensions are set
		int volume = getShipVolume();
		if (volume <= 1) {
			return new Object[]{ false, "Ship dimensions not set! Use setDimensions() first." };
		}

		int required = calculateEnergyRequired();
		if (energyStored < required) {
			return new Object[]{
				false,
				String.format("Insufficient energy: %d/%d FE", energyStored, required)
			};
		}

		if (shipState == STATE_COOLDOWN) {
			return new Object[]{ false, String.format("Drive cooling down: %.1fs remaining", cooldownRemaining / 20.0) };
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
		if (shipState == STATE_COUNTDOWN) {
			return new Object[]{ false, String.format("Jump already counting down: %.1fs", countdownRemaining / 20.0) };
		}

		// Force-load the destination before counting down. Placing blocks into an unloaded chunk
		// silently does nothing, which is why the 1.12.2 version pre-loaded the target area first.
		final int chunks = forceDestinationChunks(true);

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
		final int volume = getShipVolume();
		final int required = calculateEnergyRequired();

		if (energyStored < required) {
			DebugLog.log("JUMP", "countdown finished but energy dropped below requirement ({} < {})",
				energyStored, required);
			forceDestinationChunks(false);
			beginCooldown();
			return;
		}

		CompletableFuture<Object[]> future = new CompletableFuture<>();
		{
			try {
				DebugLog.log("JUMP", "countdown complete, executing on thread: {}", Thread.currentThread().getName());

				// Calculate absolute destination from relative movement
				int destX = getBlockPos().getX() + moveX;
				int destY = getBlockPos().getY() + moveY;
				int destZ = getBlockPos().getZ() + moveZ;

				DebugLog.log("JUMP", "Warp jump initiated: {} moving by {}, {}, {} to {}, {}, {}",
					shipName, moveX, moveY, moveZ, destX, destY, destZ);
				DebugLog.log("JUMP", "energyStored={} required={} volume={}",
					energyStored, required, volume);

				// Build ship scan from dimensions
				ShipScanner.ShipScanResult dimensionScan = buildShipFromDimensions();

				// Execute warp using WarpEngine
				int energyAfterJump = Math.max(0, energyStored - required);

				DebugLog.log("JUMP", "About to call WarpEngine.executeWarp with energyAfterJump={}", energyAfterJump);
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
					future.complete(new Object[]{ false, "Unknown destination dimension: " + targetDimension });
					return;
				}
				WarpEngine.WarpResult result = WarpEngine.executeWarp(level, destWorld, dimensionScan, destX, destY, destZ, energyAfterJump, rotationSteps);
				DebugLog.log("JUMP", "WarpEngine.executeWarp returned: success={} message='{}'",
					result.success, result.message);

				if (result.success) {
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
						destCore.beginCooldown();
						destCore.setChanged();

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

					future.complete(new Object[]{
						true,
						String.format("%s (Used %,d FE)", result.message, required)
					});
				} else {
					WarpDrive.logger.warn("Warp failed: {}", result.message);
					// Collision or out-of-bounds: the original mod had a sound for exactly this
					playShipSound(Registration.SOUND_COLLISION.get(), 1.0F, 1.0F);
					broadcastToOnboard(new StringTextComponent(
						TextFormatting.RED + "✖ WARP FAILED: " + result.message));
					future.complete(new Object[]{
						false,
						result.message
					});
				}
			} catch (Exception e) {
				WarpDrive.logger.error("Exception during jump", e);
				DebugLog.log("JUMP", "EXCEPTION during jump: {}", e);
				future.completeExceptionally(e);
			}
		}

		// Release the force-load: the destination is occupied now, so normal chunk rules apply
		forceDestinationChunks(false);

		// If the origin core still exists (jump failed), cool down here instead
		if (shipState != STATE_COOLDOWN) {
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
		final int destX = core.getX() + moveX;
		final int destZ = core.getZ() + moveZ;
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
		final int destX = core.getX() + moveX;
		final int destZ = core.getZ() + moveZ;

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

	private ShipScanner.ShipScanResult buildShipFromDimensions() {
		// Build a rectangular ship based on dimensions (shares the bounds math with the renderer,
		// so what you see highlighted is exactly what gets moved)
		BlockPos corePos = getBlockPos();
		final int[] bounds = getShipBounds();
		int minX = bounds[0];
		int minY = bounds[1];
		int minZ = bounds[2];
		int maxX = bounds[3];
		int maxY = bounds[4];
		int maxZ = bounds[5];

		List<ShipScanner.ShipBlock> blocks = new java.util.ArrayList<>();

		// Scan all blocks in the rectangular region
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
					net.minecraft.block.BlockState state = level.getBlockState(pos);

					// Skip air blocks
					if (!state.isAir()) {
						TileEntity te = level.getBlockEntity(pos);
						blocks.add(new ShipScanner.ShipBlock(pos, state, te));
					}
				}
			}
		}

		return new ShipScanner.ShipScanResult(
			true,
			String.format("Scanned %d blocks", blocks.size()),
			blocks,
			minX, maxX,
			minY, maxY,
			minZ, maxZ,
			corePos
		);
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

	// ===== CC:Tweaked Peripheral Capability =====

	private LazyOptional<IPeripheral> peripheralCap;

	// ===== Energy Capability =====

	private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = Math.min(MAX_ENERGY - energyStored, Math.min(maxReceive, MAX_TRANSFER));
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
			return MAX_ENERGY;
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

		// CC:Tweaked peripheral capability
		if (cap == Capabilities.CAPABILITY_PERIPHERAL) {
			if (peripheralCap == null && level != null) {
				IPeripheral peripheral = GenericPeripheralProvider.getPeripheral(level, worldPosition, side, invalidate -> {
					if (peripheralCap != null) {
						peripheralCap.invalidate();
						peripheralCap = null;
					}
				});
				if (peripheral != null) {
					peripheralCap = LazyOptional.of(() -> peripheral);
				}
			}
			return peripheralCap == null ? LazyOptional.empty() : peripheralCap.cast();
		}

		return super.getCapability(cap, side);
	}

	@Override
	protected void invalidateCaps() {
		super.invalidateCaps();
		energyHandler.invalidate();
		if (peripheralCap != null) {
			peripheralCap.invalidate();
			peripheralCap = null;
		}
	}

	// ===== NBT Serialization =====

	@Override
	public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
		super.load(state, nbt);
		energyStored = nbt.getInt("Energy");
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
		moveX = nbt.getInt("MoveX");
		moveY = nbt.getInt("MoveY");
		moveZ = nbt.getInt("MoveZ");
		showBoundingBox = nbt.getBoolean("ShowBoundingBox");
		// Ships saved before facing existed used the fixed +Z mapping, which is SOUTH
		final int facingIndex = nbt.contains("Facing") ? nbt.getInt("Facing") : Direction.SOUTH.get3DDataValue();
		facing = Direction.from3DDataValue(facingIndex);
		if (facing.getAxis().isVertical()) {
			facing = Direction.SOUTH;
		}
		rotationSteps = ((nbt.getInt("RotationSteps") % 4) + 4) % 4;
		command = nbt.contains("Command") ? nbt.getString("Command") : "MANUAL";
		enabled = nbt.getBoolean("Enabled");
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
		nbt.putInt("MoveX", moveX);
		nbt.putInt("MoveY", moveY);
		nbt.putInt("MoveZ", moveZ);
		nbt.putBoolean("ShowBoundingBox", showBoundingBox);
		nbt.putInt("Facing", facing.get3DDataValue());
		nbt.putInt("RotationSteps", rotationSteps);
		nbt.putString("Command", command);
		nbt.putBoolean("Enabled", enabled);
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
		return save(new CompoundNBT());
	}

	// getUpdateTag/handleUpdateTag only cover the initial chunk load. Without these two,
	// level.sendBlockUpdated(...) sends nothing, so the client keeps stale dimensions and
	// shift+right-click reports "No ship dimensions set" even after setDimensions() succeeded
	// server-side.
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(worldPosition, 1, save(new CompoundNBT()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		load(getBlockState(), pkt.getTag());
		refreshRendererTracking();
	}

	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT nbt) {
		// Receive data on client (initial chunk load, and the destination core after a jump)
		load(state, nbt);
		refreshRendererTracking();
	}

	@Override
	public void setRemoved() {
		// Drop the box when the core is broken or moved away by a jump
		if (level != null && level.isClientSide) {
			BoundingBoxRenderer.untrack(getBlockPos());
		}
		super.setRemoved();
	}
}
