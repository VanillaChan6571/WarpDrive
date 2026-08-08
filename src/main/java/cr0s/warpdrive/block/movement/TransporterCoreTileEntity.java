package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldBlock;
import cr0s.warpdrive.block.forcefield.ForceFieldTileEntity;
import cr0s.warpdrive.damage.WarpDamageSources;
import cr0s.warpdrive.data.CelestialCoordinates;
import cr0s.warpdrive.data.DimensionAltitude;
import cr0s.warpdrive.data.GlobalRegionRegistry;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
 * Faithful 1.16 transporter-room state machine.
 *
 * <p>The port retains the legacy 3x3 pad discovery, range/focus equations, lock convergence,
 * bidirectional slot allocation, movement tolerance, warm-up/cooldown, failure damage, beacon
 * handshake, shield-frequency jamming, signatures and computer contract. Dimension IDs and the
 * old ticket API are replaced by registry keys, canonical celestial coordinates and Forge 1.16
 * block tickets.</p>
 */
public class TransporterCoreTileEntity extends AbstractEnergyTileEntity
	implements ITickableTileEntity, IBeamFrequency {

	private static final int BASE_CAPACITY = 1_000_000;
	private static final int STORAGE_BONUS = 500_000;
	private static final int SCANNER_RANGE_XZ = 8;
	private static final int SCANNER_RANGE_BELOW = 3;
	private static final int SCANNER_RANGE_ABOVE = 1;
	private static final int RANGE_BASE = 256;
	private static final int RANGE_BONUS = 64;
	private static final double LOCK_DECAY = Math.pow(0.01D, 1.0D / 300.0D);
	private static final double STRENGTH_WILDERNESS = 0.25D;
	private static final double STRENGTH_BEACON = 0.50D;
	private static final double STRENGTH_TRANSPORTER = 1.00D;
	private static final double STRENGTH_ENERGY_BONUS = 0.50D;
	private static final double STRENGTH_UPGRADE = 0.15D;
	private static final double SPEED_WILDERNESS = 0.25D;
	private static final double SPEED_BEACON = 0.75D;
	private static final double SPEED_TRANSPORTER = 1.00D;
	private static final double SPEED_UPGRADE = 0.25D;
	private static final int LOCK_OPTIMAL_TICKS = 100;
	private static final int JAMMED_COOLDOWN = 40;
	private static final int CHARGING_TICKS = 60;
	private static final int ENERGIZING_COOLDOWN = 200;
	private static final double LOCK_LOST = 0.5D;
	private static final double SUCCESS_LOCK_BONUS = 0.20D;
	private static final double MAX_ENERGY_FACTOR = 10.0D;
	private static final double MOVEMENT_TOLERANCE_SQUARED = 1.0D;
	private static final int ENTITY_GRAB_RADIUS = 2;
	private static final int FOCUS_RADIUS = 2;

	private static final String TAG_ENABLED = "enabled";
	private static final String TAG_LOCK = "isLockRequested";
	private static final String TAG_ENERGIZE = "isEnergizeRequested";
	private static final String TAG_TARGET_TYPE = "remoteType";
	private static final String TAG_TARGET_X = "remoteX";
	private static final String TAG_TARGET_Y = "remoteY";
	private static final String TAG_TARGET_Z = "remoteZ";
	private static final String TAG_TARGET_UUID = "remoteUuid";
	private static final String TAG_TARGET_PLAYER = "remotePlayer";
	private static final String TAG_FACTOR = "energyFactor";
	private static final String TAG_LOCK_STRENGTH = "lockStrengthActual";
	private static final String TAG_COOLDOWN = "tickCooldown";
	private static final String TAG_ENERGIZING_TICKS = "tickEnergizing";
	private static final String TAG_STATE = "state";
	private static final String TAG_SIGNATURE = "signatureUuid";
	private static final String TAG_NAME = "name";
	private static final String TAG_SCANNERS = "scanners";
	private static final String TAG_CONTAINMENTS = "containments";
	private static final String TAG_UPGRADE_ENERGY = "upgradeEnergy";
	private static final String TAG_UPGRADE_FOCUS = "upgradeFocus";
	private static final String TAG_UPGRADE_RANGE = "upgradeRange";

	private final List<BlockPos> localScanners = new ArrayList<>();
	private final List<BlockPos> localContainments = new ArrayList<>();
	private final Map<Integer, CapturedEntity> movingLocal = new HashMap<>();
	private final Map<Integer, CapturedEntity> movingRemote = new HashMap<>();
	private final Set<TicketRef> chunkTickets = new HashSet<>();
	private final Set<TransporterEventListener> eventListeners = new CopyOnWriteArraySet<>();
	private int beamFrequency = -1;
	private boolean enabled = true;
	private boolean lockRequested;
	private boolean energizeRequested;
	private TargetType targetType = TargetType.NONE;
	private BlockPos targetCoordinates;
	private UUID targetUuid;
	private String targetPlayer = "";
	private double energyFactor = 1.0D;
	private double lockStrength;
	private int cooldown;
	private int energizingTicks;
	private TransporterState state = TransporterState.DISABLED;
	private UUID signatureUuid = UUID.randomUUID();
	private String signatureName = "Transporter";
	private int energyUpgrades;
	private int focusUpgrades;
	private int rangeUpgrades;

	private int scanTimer = 1;
	private int parameterTimer = 1;
	private int pulseTimer;
	private boolean parametersDirty = true;
	private boolean connected;
	private boolean powered = true;
	private boolean jammed;
	private String jamReason = "";
	private double acquiringCost;
	private double energizingCost;
	private double optimalLock = -1.0D;
	private double lockSpeed;
	@Nullable private RemoteTarget remoteTarget;
	@Nullable private List<BlockPos> remoteScanners;
	@Nullable private BeaconTarget beaconTarget;
	private int beaconTimeout;
	private boolean broken;

	public TransporterCoreTileEntity() {
		super(Registration.TRANSPORTER_CORE_TILE.get());
	}

	@Override public int getMaxEnergyStored() { return BASE_CAPACITY + STORAGE_BONUS * energyUpgrades; }
	@Override protected int getMaxReceive() { return 4096; }
	@Override protected int getMaxExtract() { return 0; }
	@Override protected boolean canReceiveFrom(@Nullable final Direction side) {
		return side == null || side != Direction.UP;
	}

	@Override
	public void tick() {
		if (!(level instanceof ServerWorld)) return;
		final ServerWorld world = (ServerWorld) level;
		connected = IBeamFrequency.isValid(beamFrequency);
		if (cooldown > 0) cooldown--;
		if (beaconTimeout > 0 && --beaconTimeout == 0) clearBeaconTarget();
		if (--scanTimer <= 0) {
			scanTimer = 100;
			scanAssembly(world);
		}
		if (parametersDirty || --parameterTimer <= 0) {
			parameterTimer = 10;
			parametersDirty = false;
			updateParameters(world);
		}

		if (!enabled || !connected) state = TransporterState.DISABLED;
		final int required = getEnergyRequired(state);
		powered = required <= 0 || consumeEnergy(required, true);
		if (required > 0 && !powered) {
			jammed = true;
			jamReason = "Insufficient energy for operation";
			state = TransporterState.IDLE;
			cooldown = Math.max(cooldown, JAMMED_COOLDOWN);
		} else if (required > 0) {
			consumeEnergy(required, false);
		}

		if (powered && (state == TransporterState.ACQUIRING
		 || state == TransporterState.ENERGIZING)) {
			final double overshoot = 0.01D;
			lockStrength = Math.min(optimalLock,
				lockStrength + lockSpeed * (optimalLock - lockStrength + overshoot));
		} else {
			lockStrength = Math.max(0.0D, lockStrength * LOCK_DECAY);
		}

		runStateMachine(world);
		updateAppearance(world);
		spawnFeedback(world);
		if (--pulseTimer <= 0) {
			pulseTimer = 20;
			if (lockStrength > 0.01D) fireEvent("transporterPulse", lockStrength);
		}
		setChanged();
	}

	private void runStateMachine(final ServerWorld world) {
		switch (state) {
		case DISABLED:
			releaseChunks();
			lockRequested = false;
			energizeRequested = false;
			lockStrength = 0.0D;
			if (connected && enabled) state = TransporterState.IDLE;
			break;
		case IDLE:
			if (lockRequested && cooldown == 0) {
				parametersDirty = true;
				state = TransporterState.ACQUIRING;
			} else {
				releaseChunks();
			}
			break;
		case ACQUIRING:
			if (!lockRequested) {
				state = TransporterState.IDLE;
			} else if (jammed) {
				cooldown += JAMMED_COOLDOWN;
				state = TransporterState.IDLE;
			} else if (cooldown == 0) {
				if (beaconTarget != null && !energizeRequested && lockStrength >= 0.85D) {
					energizeRequested = true;
				}
				if (energizeRequested) {
					parametersDirty = true;
					energizingTicks = CHARGING_TICKS;
					state = TransporterState.ENERGIZING;
				}
			}
			break;
		case ENERGIZING:
			if (!lockRequested) {
				state = TransporterState.IDLE;
			} else if (!energizeRequested) {
				state = TransporterState.ACQUIRING;
			} else if (jammed) {
				cooldown += JAMMED_COOLDOWN;
				state = TransporterState.IDLE;
			} else if (cooldown <= 0) {
				energize(world);
			}
			break;
		default:
			state = TransporterState.DISABLED;
		}
	}

	private void scanAssembly(final ServerWorld world) {
		for (final BlockPos scanner : localScanners) setScannerActive(world, scanner, false);
		final ArrayList<BlockPos> scanners = new ArrayList<>();
		final LinkedHashSet<BlockPos> containments = new LinkedHashSet<>();
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int x = worldPosition.getX() - SCANNER_RANGE_XZ;
		     x <= worldPosition.getX() + SCANNER_RANGE_XZ; x++) {
			for (int y = Math.max(0, worldPosition.getY() - SCANNER_RANGE_BELOW);
			     y <= Math.min(world.getMaxBuildHeight() - 1,
				     worldPosition.getY() + SCANNER_RANGE_ABOVE); y++) {
				for (int z = worldPosition.getZ() - SCANNER_RANGE_XZ;
				     z <= worldPosition.getZ() + SCANNER_RANGE_XZ; z++) {
					mutable.set(x, y, z);
					if (!world.hasChunkAt(mutable)) continue;
					final BlockState scannerState = world.getBlockState(mutable);
					if (!(scannerState.getBlock() instanceof TransporterScannerBlock)) continue;
					final Collection<BlockPos> valid = ((TransporterScannerBlock)
						scannerState.getBlock()).getValidContainment(world, mutable);
					if (valid == null || valid.isEmpty()) {
						setScannerActive(world, mutable, false);
						world.sendParticles(ParticleTypes.SMOKE, x + 0.5D, y + 1.5D, z + 0.5D,
							8, 0.3D, 0.4D, 0.3D, 0.02D);
					} else {
						scanners.add(mutable.immutable());
						containments.addAll(valid);
						setScannerActive(world, mutable, true);
					}
				}
			}
		}
		localScanners.clear();
		localScanners.addAll(scanners);
		localContainments.clear();
		localContainments.addAll(containments);
		movingLocal.clear();
		movingRemote.clear();
		parametersDirty = true;
		GlobalRegionRegistry.updateTransporter(this);
	}

	private static void setScannerActive(final ServerWorld world, final BlockPos position,
	                                     final boolean active) {
		if (!world.hasChunkAt(position)) return;
		final BlockState state = world.getBlockState(position);
		if (state.hasProperty(TransporterScannerBlock.ACTIVE)
		 && state.getValue(TransporterScannerBlock.ACTIVE) != active) {
			world.setBlock(position, state.setValue(TransporterScannerBlock.ACTIVE, active), 3);
		}
	}

	private void updateParameters(final ServerWorld world) {
		jammed = false;
		jamReason = "";
		if (state != TransporterState.ENERGIZING) {
			movingLocal.clear();
			movingRemote.clear();
		}
		if (!connected) {
			setJammed("Beam frequency not set");
			return;
		}

		final RemoteTarget nextTarget = resolveTarget(world);
		if (!Objects.equals(remoteTarget, nextTarget)) {
			remoteTarget = nextTarget;
			lockStrength = 0.0D;
			movingLocal.clear();
			movingRemote.clear();
			if (state == TransporterState.ENERGIZING) state = TransporterState.ACQUIRING;
		}
		if (remoteTarget == null) {
			setJammed(jamReason.isEmpty() ? "No remote location defined" : jamReason);
			return;
		}

		if (!remoteTarget.world.dimension().equals(world.dimension())
		 && (DimensionAltitude.HYPERSPACE.equals(DimensionAltitude.idOf(world))
		  || DimensionAltitude.HYPERSPACE.equals(DimensionAltitude.idOf(remoteTarget.world)))) {
			setJammed("Blocked by warp field barrier; exit hyperspace first");
			return;
		}

		if (state == TransporterState.ACQUIRING || state == TransporterState.ENERGIZING) {
			forceChunks(world, remoteTarget);
		}
		final CelestialCoordinates.UniversalPosition localUniversal =
			CelestialCoordinates.toUniversal(world, worldPosition.getX(), worldPosition.getY(),
				worldPosition.getZ());
		final CelestialCoordinates.UniversalPosition remoteUniversal =
			CelestialCoordinates.toUniversal(remoteTarget.world, remoteTarget.position.getX(),
				remoteTarget.position.getY(), remoteTarget.position.getZ());
		if (localUniversal == null || remoteUniversal == null) {
			setJammed("Unknown celestial object");
			return;
		}
		final int range = (int) Math.ceil(Math.sqrt(localUniversal.distanceSquaredTo(remoteUniversal)));
		final FocusValues localFocus = getFocus(world, worldPosition, 0);
		final FocusValues remoteFocus = getFocus(remoteTarget.world, remoteTarget.position, FOCUS_RADIUS);
		final double focusBoost = (energyFactor - 1.0D) / (MAX_ENERGY_FACTOR - 1.0D)
			* STRENGTH_ENERGY_BONUS;
		optimalLock = (localFocus.strength + remoteFocus.strength) / 2.0D + focusBoost;
		lockSpeed = (localFocus.speed + remoteFocus.speed) / 2.0D / LOCK_OPTIMAL_TICKS;
		remoteScanners = remoteFocus.scanners;
		final int maximumRange = RANGE_BASE
			+ RANGE_BONUS * localFocus.rangeUpgrades
			+ RANGE_BONUS * remoteFocus.rangeUpgrades;
		final EntityValues entities = updateEntities(remoteTarget.world);
		acquiringCost = calculateAcquiringEnergy(range,
			localScanners.isEmpty() ? 1 : localScanners.size());
		energizingCost = calculateEnergizingEnergy(range, entities.mass);

		if (range < 16) {
			setJammed("Remote location is too close");
			return;
		}
		if (range > maximumRange) {
			setJammed(String.format("Out of range: %d > %d m", range, maximumRange));
			return;
		}
		if (isTrajectoryJammed(world, remoteTarget.world, worldPosition,
			remoteTarget.position, localUniversal.y, remoteUniversal.y, beamFrequency)) {
			setJammed("Blocked by force field or unbreakable block");
		}
	}

	@Nullable
	private RemoteTarget resolveTarget(final ServerWorld world) {
		if (beaconTarget != null && beaconTimeout > 0) {
			final ServerWorld targetWorld = world.getServer() == null ? null
				: world.getServer().getLevel(beaconTarget.dimension);
			if (targetWorld != null) return new RemoteTarget(targetWorld, beaconTarget.position);
			jamReason = "Beacon dimension is unavailable";
			return null;
		}
		switch (targetType) {
		case COORDINATES:
			return resolveCoordinates(world);
		case UUID:
			final GlobalRegionRegistry.TransporterLocation location =
				GlobalRegionRegistry.findTransporter(world, targetUuid);
			if (location == null) {
				jamReason = "Unknown transporter signature";
				return null;
			}
			final ServerWorld targetWorld = getWorld(world, location.dimension);
			if (targetWorld == null) {
				jamReason = "Transporter dimension is unavailable";
				return null;
			}
			return new RemoteTarget(targetWorld, location.position);
		case PLAYER:
			if (world.getServer() == null) return null;
			final ServerPlayerEntity player = world.getServer().getPlayerList()
				.getPlayerByName(targetPlayer);
			if (player == null) {
				jamReason = "No player by that name";
				return null;
			}
			if (!hasActiveBeacon(player)) {
				jamReason = "Player has no powered transporter beacon in hand";
				return null;
			}
			return new RemoteTarget(player.getLevel(), player.blockPosition());
		case NONE:
		default:
			jamReason = "No remote location defined";
			return null;
		}
	}

	@Nullable
	private RemoteTarget resolveCoordinates(final ServerWorld source) {
		if (targetCoordinates == null || source.getServer() == null) return null;
		RegistryKey<World> dimension = source.dimension();
		int x = targetCoordinates.getX();
		int y = targetCoordinates.getY();
		int z = targetCoordinates.getZ();
		if (y < 0) {
			dimension = DimensionAltitude.below(source);
			if (dimension == null) {
				jamReason = "No lower celestial layer";
				return null;
			}
			final ServerWorld destination = source.getServer().getLevel(dimension);
			if (destination == null) return null;
			x = CelestialCoordinates.mapHorizontal(source, destination, x);
			z = CelestialCoordinates.mapHorizontal(source, destination, z);
			y = Math.floorMod(y, destination.getMaxBuildHeight());
		} else if (y > source.getMaxBuildHeight()) {
			dimension = DimensionAltitude.above(source);
			if (dimension == null) {
				jamReason = "No higher celestial layer";
				return null;
			}
			final ServerWorld destination = source.getServer().getLevel(dimension);
			if (destination == null) return null;
			x = CelestialCoordinates.mapHorizontal(source, destination, x);
			z = CelestialCoordinates.mapHorizontal(source, destination, z);
			y = Math.floorMod(y, destination.getMaxBuildHeight());
		}
		final ServerWorld destination = source.getServer().getLevel(dimension);
		return destination == null ? null : new RemoteTarget(destination, new BlockPos(x, y, z));
	}

	private static boolean hasActiveBeacon(final ServerPlayerEntity player) {
		for (final ItemStack held : new ItemStack[]{ player.getMainHandItem(), player.getOffhandItem() }) {
			if (held.getItem() instanceof TransporterBeaconBlockItem
			 && TransporterBeaconBlockItem.isActive(held)) return true;
		}
		return false;
	}

	@Nullable
	private static ServerWorld getWorld(final ServerWorld context, final ResourceLocation dimension) {
		return context.getServer() == null ? null : context.getServer().getLevel(
			RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension));
	}

	private static FocusValues getFocus(final ServerWorld world, final BlockPos center,
	                                    final int radius) {
		int beacons = 0;
		int transporters = 0;
		int range = 0;
		int focus = 0;
		List<BlockPos> scanners = null;
		final BlockPos.Mutable mutable = new BlockPos.Mutable();
		for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
			for (int y = Math.max(0, center.getY() - radius);
			     y <= Math.min(world.getMaxBuildHeight() - 1, center.getY() + radius); y++) {
				for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
					mutable.set(x, y, z);
					if (!world.hasChunkAt(mutable)) continue;
					final TileEntity tileEntity = world.getBlockEntity(mutable);
					if (tileEntity instanceof TransporterBeaconTileEntity
					 && ((TransporterBeaconTileEntity) tileEntity).isActive()) {
						beacons++;
					} else if (tileEntity instanceof TransporterCoreTileEntity) {
						final TransporterCoreTileEntity core = (TransporterCoreTileEntity) tileEntity;
						if (core.enabled && core.connected) {
							transporters++;
							scanners = new ArrayList<>(core.localScanners);
							range += core.rangeUpgrades;
							focus += core.focusUpgrades;
						}
					}
				}
			}
		}
		if (transporters == 1) {
			return new FocusValues(scanners, range,
				SPEED_TRANSPORTER + focus * STRENGTH_UPGRADE,
				STRENGTH_TRANSPORTER + focus * SPEED_UPGRADE);
		}
		if (beacons > 0) return new FocusValues(null, 0, SPEED_BEACON, STRENGTH_BEACON);
		return new FocusValues(null, 0, SPEED_WILDERNESS, STRENGTH_WILDERNESS);
	}

	static int calculateAcquiringEnergy(final int range, final int scannerCount) {
		return Math.max(0, (int) Math.ceil(20.0D + 3.0D * scannerCount
			* Math.sqrt(10.0D + Math.max(0, range))));
	}

	static int calculateEnergizingEnergy(final int range, final double mass) {
		return Math.max(0, (int) Math.ceil(10_000.0D + 1500.0D * Math.max(0.0D, mass)
			* Math.sqrt(10.0D + Math.max(0, range))));
	}

	private int getEnergyRequired(final TransporterState requestedState) {
		switch (requestedState) {
		case ACQUIRING: return (int) Math.ceil(acquiringCost * energyFactor);
		case ENERGIZING: return (int) Math.ceil(energizingCost * energyFactor / CHARGING_TICKS);
		default: return 0;
		}
	}

	private void energize(final ServerWorld world) {
		if (remoteTarget == null) {
			setJammed("Remote location is unavailable");
			return;
		}
		final EntityValues entities = updateEntities(remoteTarget.world);
		if (energizingTicks == CHARGING_TICKS) fireEvent("transporterEnergizing", entities.count);
		if (entities.count == 0) {
			energizeRequested = false;
			cooldown += ENERGIZING_COOLDOWN;
			state = TransporterState.ACQUIRING;
			return;
		}
		if (energizingTicks > 0) {
			energizingTicks--;
			return;
		}

		if (remoteScanners == null || remoteScanners.isEmpty()) {
			transportEntities(movingLocal, remoteTarget.world,
				Collections.singletonList(remoteTarget.position));
		} else {
			transportEntities(movingLocal, remoteTarget.world, remoteScanners);
		}
		if (!localScanners.isEmpty()) transportEntities(movingRemote, world, localScanners);
		movingLocal.clear();
		movingRemote.clear();
		energizeRequested = false;
		parametersDirty = true;
		cooldown += ENERGIZING_COOLDOWN;
		lockStrength = Math.max(0.0D, lockStrength - LOCK_LOST);
		state = TransporterState.ACQUIRING;
		notifyBeaconDone(world);
	}

	private void transportEntities(final Map<Integer, CapturedEntity> entities,
	                               final ServerWorld destination,
	                               final List<BlockPos> destinations) {
		for (final Map.Entry<Integer, CapturedEntity> entry : entities.entrySet()) {
			final CapturedEntity captured = entry.getValue();
			if (captured == null || captured.invalid) continue;
			final BlockPos target = destinations.size() == 1 ? destinations.get(0)
				: entry.getKey() < destinations.size() ? destinations.get(entry.getKey()) : null;
			if (target == null) continue;
			final Entity entity = captured.resolve(level.getServer());
			if (entity == null) continue;
			final String name = entity.getName().getString();
			if (lockStrength < 1.0D && destination.random.nextDouble() > lockStrength) {
				applyTeleportationDamage(false, entity, lockStrength);
				fireEvent("transporterFailure", name);
				continue;
			}
			final Entity moved = transfer(entity, destination, target.getX() + 0.5D,
				target.getY() + 0.99D, target.getZ() + 0.5D);
			if (moved != null) {
				applyTeleportationDamage(false, moved, lockStrength);
				fireEvent("transporterSuccess", name);
				destination.sendParticles(ParticleTypes.PORTAL, target.getX() + 0.5D,
					target.getY() + 1.0D, target.getZ() + 0.5D, 40,
					0.4D, 0.8D, 0.4D, 0.2D);
			}
		}
	}

	@Nullable
	private static Entity transfer(final Entity entity, final ServerWorld destination,
	                               final double x, final double y, final double z) {
		entity.stopRiding();
		if (entity.level == destination) {
			if (entity instanceof ServerPlayerEntity) {
				((ServerPlayerEntity) entity).connection.teleport(x, y, z, entity.yRot, entity.xRot);
			} else {
				entity.teleportTo(x, y, z);
			}
			return entity;
		}
		if (entity instanceof ServerPlayerEntity) {
			((ServerPlayerEntity) entity).teleportTo(destination, x, y, z, entity.yRot, entity.xRot);
			return entity;
		}
		return entity.changeDimension(destination, new ITeleporter() {
			@Override
			public Entity placeEntity(final Entity original, final ServerWorld current,
			                          final ServerWorld target, final float yaw,
			                          final Function<Boolean, Entity> reposition) {
				final Entity placed = reposition.apply(false);
				if (placed != null) placed.teleportTo(x, y, z);
				return placed;
			}
		});
	}

	private EntityValues updateEntities(final ServerWorld worldRemote) {
		final int localCount = localScanners.size();
		final int count = remoteScanners == null ? localCount
			: Math.min(localCount, remoteScanners.size());
		if (state != TransporterState.ACQUIRING && state != TransporterState.ENERGIZING) {
			return new EntityValues(count, 2.0D * count);
		}
		final EntityValues local = updateOnScanners((ServerWorld) level, localScanners,
			count, movingLocal);
		final EntityValues remote = remoteScanners == null
			? updateInArea(worldRemote, remoteTarget.position, count, movingRemote)
			: updateOnScanners(worldRemote, remoteScanners, count, movingRemote);
		return new EntityValues(local.count + remote.count, local.mass + remote.mass);
	}

	private static EntityValues updateOnScanners(final ServerWorld world,
	                                             final List<BlockPos> scanners, final int count,
	                                             final Map<Integer, CapturedEntity> moving) {
		final Set<UUID> allocated = new HashSet<>();
		int entityCount = 0;
		double mass = 0.0D;
		for (int index = 0; index < count; index++) {
			CapturedEntity captured = moving.get(index);
			if (captured != null && captured.invalid) continue;
			if (captured != null) {
				final Entity entity = captured.resolve(world.getServer());
				if (entity == null || !entity.isAlive()) {
					captured = null;
				} else if (captured.distanceSquared(entity) > MOVEMENT_TOLERANCE_SQUARED) {
					final double strength = Math.sqrt(captured.distanceSquared(entity)) / 2.0D;
					applyTeleportationDamage(true, entity, strength);
					captured = CapturedEntity.invalid();
				} else {
					allocated.add(entity.getUUID());
				}
			}
			if (captured == null && index < scanners.size()) {
				final Entity candidate = candidateOnScanner(world, scanners.get(index), allocated);
				if (candidate != null) {
					captured = new CapturedEntity(candidate);
					allocated.add(candidate.getUUID());
				}
			}
			if (captured == null) captured = CapturedEntity.invalid();
			moving.put(index, captured);
			if (!captured.invalid) {
				entityCount++;
				mass += captured.mass(world.getServer());
			}
		}
		return new EntityValues(entityCount, mass);
	}

	private static EntityValues updateInArea(final ServerWorld world, final BlockPos center,
	                                         final int count,
	                                         final Map<Integer, CapturedEntity> moving) {
		final LinkedHashSet<Entity> candidates = candidatesInArea(world, center);
		int entityCount = 0;
		double mass = 0.0D;
		for (int index = 0; index < count; index++) {
			CapturedEntity captured = moving.get(index);
			if (captured != null && captured.invalid) continue;
			if (captured != null) {
				final Entity entity = captured.resolve(world.getServer());
				if (entity == null || !entity.isAlive()) {
					captured = null;
				} else if (!candidates.remove(entity)) {
					captured = CapturedEntity.invalid();
				} else if (captured.distanceSquared(entity) > MOVEMENT_TOLERANCE_SQUARED) {
					final double strength = Math.sqrt(captured.distanceSquared(entity)) / 2.0D;
					applyTeleportationDamage(true, entity, strength);
					captured = CapturedEntity.invalid();
				}
			}
			if (captured == null && !candidates.isEmpty()) {
				final Iterator<Entity> iterator = candidates.iterator();
				captured = new CapturedEntity(iterator.next());
				iterator.remove();
			}
			if (captured == null) captured = CapturedEntity.invalid();
			moving.put(index, captured);
			if (!captured.invalid) {
				entityCount++;
				mass += captured.mass(world.getServer());
			}
		}
		return new EntityValues(entityCount, mass);
	}

	@Nullable
	private static Entity candidateOnScanner(final ServerWorld world, final BlockPos scanner,
	                                         final Set<UUID> allocated) {
		final AxisAlignedBB bounds = new AxisAlignedBB(scanner.getX() - 0.05D,
			scanner.getY() - 1.0D, scanner.getZ() - 0.05D,
			scanner.getX() + 1.05D, scanner.getY() + 2.0D, scanner.getZ() + 1.05D);
		Entity result = null;
		int count = 0;
		for (final Entity entity : world.getEntities((Entity) null, bounds,
			TransporterCoreTileEntity::canTransport)) {
			if (allocated.contains(entity.getUUID())) continue;
			result = entity;
			count++;
		}
		if (count > 1) {
			world.sendParticles(ParticleTypes.SMOKE, scanner.getX() + 0.5D,
				scanner.getY() + 1.5D, scanner.getZ() + 0.5D, 12,
				0.35D, 0.5D, 0.35D, 0.03D);
			return null;
		}
		return result;
	}

	private static LinkedHashSet<Entity> candidatesInArea(final ServerWorld world,
	                                                      final BlockPos center) {
		final AxisAlignedBB bounds = new AxisAlignedBB(center.getX() - ENTITY_GRAB_RADIUS,
			center.getY() - 1.0D, center.getZ() - ENTITY_GRAB_RADIUS,
			center.getX() + ENTITY_GRAB_RADIUS + 1.0D, center.getY() + 2.0D,
			center.getZ() + ENTITY_GRAB_RADIUS + 1.0D);
		return new LinkedHashSet<>(world.getEntities((Entity) null, bounds,
			TransporterCoreTileEntity::canTransport));
	}

	private static boolean canTransport(final Entity entity) {
		return entity != null && entity.isAlive() && !entity.isSpectator();
	}

	private static void applyTeleportationDamage(final boolean before, final Entity entity,
	                                             final double strength) {
		if (!entity.isAlive() || entity.isInvulnerableTo(WarpDamageSources.TELEPORTATION)) return;
		final double adjusted = before ? strength : strength + Math.random() * SUCCESS_LOCK_BONUS;
		if (adjusted > 0.95D) return;
		final double normalized = (0.95D - adjusted) / (0.95D - 0.65D);
		final double maximum = before ? 5.0D : 100.0D;
		final float damage = (float) MathHelper.clamp(maximum * normalized, 1.0D, 1000.0D);
		if (entity instanceof LivingEntity) {
			entity.hurt(WarpDamageSources.TELEPORTATION, damage);
			if (!(entity instanceof PlayerEntity) || !((PlayerEntity) entity).isCreative()) {
				((LivingEntity) entity).addEffect(new EffectInstance(
					before ? Effects.CONFUSION : Effects.POISON, before ? 20 : 100));
			}
		} else if (adjusted > 0.10D) {
			final BlockPos position = entity.blockPosition();
			if (entity.level.isEmptyBlock(position)) entity.level.setBlock(position,
				Blocks.LAVA.defaultBlockState(), 2);
		}
	}

	private static boolean isTrajectoryJammed(final ServerWorld sourceWorld,
	                                          final ServerWorld destinationWorld,
	                                          final BlockPos source, final BlockPos destination,
	                                          final double sourceUniversalY,
	                                          final double destinationUniversalY,
	                                          final int frequency) {
		if (sourceWorld == destinationWorld) {
			return isLineJammed(sourceWorld, source, destination, frequency);
		}
		if (sourceUniversalY > destinationUniversalY) {
			return isLineJammed(sourceWorld, source,
				new BlockPos(source.getX(), 0, source.getZ()), frequency)
			 || isLineJammed(destinationWorld,
				new BlockPos(destination.getX(), destinationWorld.getMaxBuildHeight() - 1,
					destination.getZ()), destination, frequency);
		}
		return isLineJammed(sourceWorld, source,
				new BlockPos(source.getX(), sourceWorld.getMaxBuildHeight() - 1, source.getZ()), frequency)
			 || isLineJammed(destinationWorld,
				new BlockPos(destination.getX(), 0, destination.getZ()), destination, frequency);
	}

	private static boolean isLineJammed(final ServerWorld world, final BlockPos source,
	                                    final BlockPos destination, final int frequency) {
		final Vector3d path = Vector3d.atCenterOf(destination).subtract(Vector3d.atCenterOf(source));
		final int steps = Math.max(1, (int) Math.ceil(3.0D * path.length()));
		final Vector3d delta = path.scale(1.0D / steps);
		Vector3d current = Vector3d.atCenterOf(source);
		BlockPos previous = source;
		for (int step = 0; step < steps; step++) {
			current = current.add(delta);
			final BlockPos position = new BlockPos(Math.round(current.x), Math.round(current.y),
				Math.round(current.z));
			if (position.equals(previous)) continue;
			previous = position;
			// Legacy World#getBlockState synchronously obtained each path chunk. Preserve that
			// obstruction guarantee here instead of treating an unloaded intermediate chunk as an
			// invisible wall; endpoint/setup chunks remain ticketed only while a lock is active.
			if (!world.hasChunkAt(position)) world.getChunkAt(position);
			final BlockState state = world.getBlockState(position);
			if (state.getBlock() instanceof ForceFieldBlock) {
				final TileEntity tileEntity = world.getBlockEntity(position);
				if (!(tileEntity instanceof ForceFieldTileEntity)
				 || ((ForceFieldTileEntity) tileEntity).getBeamFrequency() != frequency) return true;
				continue;
			}
			if (state.is(WarpDriveTags.NO_BLINK)
			 || state.getDestroySpeed(world, position) < 0.0F) return true;
		}
		return false;
	}

	private void forceChunks(final ServerWorld localWorld, final RemoteTarget target) {
		final Set<TicketRef> desired = new HashSet<>();
		addScannerAreaTickets(desired, localWorld.dimension(), worldPosition);
		for (final BlockPos scanner : localScanners) {
			desired.add(new TicketRef(localWorld.dimension(), new ChunkPos(scanner)));
		}
		// A remote core may have pad centres eight blocks away and across a chunk boundary. Load the
		// same setup window before asking it for focus/scanner data, matching the legacy ticket area.
		addScannerAreaTickets(desired, target.world.dimension(), target.position);
		for (final TicketRef ticket : new HashSet<>(chunkTickets)) {
			if (!desired.contains(ticket)) removeTicket(ticket);
		}
		for (final TicketRef ticket : desired) {
			if (chunkTickets.contains(ticket)) continue;
			final ServerWorld ticketWorld = level.getServer().getLevel(ticket.dimension);
			if (ticketWorld != null && ForgeChunkManager.forceChunk(ticketWorld, WarpDrive.MODID,
				signatureUuid, ticket.chunk.x, ticket.chunk.z, true, true)) {
				chunkTickets.add(ticket);
			}
		}
	}

	private static void addScannerAreaTickets(final Set<TicketRef> tickets,
	                                          final RegistryKey<World> dimension,
	                                          final BlockPos center) {
		for (final int x : new int[]{ center.getX() - SCANNER_RANGE_XZ,
			center.getX() + SCANNER_RANGE_XZ }) {
			for (final int z : new int[]{ center.getZ() - SCANNER_RANGE_XZ,
				center.getZ() + SCANNER_RANGE_XZ }) {
				tickets.add(new TicketRef(dimension, new ChunkPos(new BlockPos(x, center.getY(), z))));
			}
		}
		tickets.add(new TicketRef(dimension, new ChunkPos(center)));
	}

	private void releaseChunks() {
		for (final TicketRef ticket : new HashSet<>(chunkTickets)) removeTicket(ticket);
	}

	private void removeTicket(final TicketRef ticket) {
		if (level != null && level.getServer() != null) {
			final ServerWorld world = level.getServer().getLevel(ticket.dimension);
			if (world != null) ForgeChunkManager.forceChunk(world, WarpDrive.MODID, signatureUuid,
				ticket.chunk.x, ticket.chunk.z, false, true);
		}
		chunkTickets.remove(ticket);
	}

	private void spawnFeedback(final ServerWorld world) {
		if (jammed && lockRequested && world.getGameTime() % 10L == 0L) {
			world.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5D,
				worldPosition.getY() + 0.7D, worldPosition.getZ() + 0.5D,
				8, 0.4D, 0.4D, 0.4D, 0.03D);
		}
		if ((lockStrength > 0.01D || state == TransporterState.ENERGIZING)
		 && world.getGameTime() % 5L == 0L) {
			for (final BlockPos scanner : localScanners) {
				world.sendParticles(ParticleTypes.PORTAL, scanner.getX() + 0.5D,
					scanner.getY() + 1.0D, scanner.getZ() + 0.5D,
					Math.max(1, (int) Math.ceil(lockStrength * 4.0D)),
					0.35D, 0.65D, 0.35D, 0.05D);
			}
		}
	}

	private void updateAppearance(final ServerWorld world) {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof TransporterCoreBlock
		 && blockState.getValue(TransporterCoreBlock.STATE) != state) {
			world.setBlock(worldPosition, blockState.setValue(TransporterCoreBlock.STATE, state), 3);
		}
	}

	private void setJammed(final String reason) {
		jammed = true;
		jamReason = reason;
	}

	public boolean updateBeacon(final TransporterBeaconTileEntity beacon, final UUID requestedUuid) {
		if (beacon == null || requestedUuid == null || !signatureUuid.equals(requestedUuid)
		 || !(beacon.getLevel() instanceof ServerWorld)) return false;
		final BeaconTarget next = new BeaconTarget(beacon.getLevel().dimension(),
			beacon.getBlockPos());
		if (beaconTarget != null && !beaconTarget.equals(next)) {
			if (beaconTarget.dimension.equals(next.dimension)
			 && beaconTarget.position.distSqr(next.position) <= FOCUS_RADIUS * FOCUS_RADIUS) {
				beaconTimeout = 40;
				return true;
			}
			if (!isBeaconTargetActive()) clearBeaconTarget();
		}
		if (beaconTarget != null && !beaconTarget.equals(next)) {
			setJammed("Conflicting beacon requests received");
			cooldown = Math.max(cooldown, JAMMED_COOLDOWN);
			return false;
		}
		if (beaconTarget == null) {
			beaconTarget = next;
			energyFactor = Math.max(4.0D, energyFactor);
			lockStrength = 0.0D;
			movingLocal.clear();
			movingRemote.clear();
			parametersDirty = true;
		}
		beaconTimeout = 40;
		lockRequested = true;
		return true;
	}

	private boolean isBeaconTargetActive() {
		if (beaconTarget == null || level == null || level.getServer() == null) return false;
		final ServerWorld world = level.getServer().getLevel(beaconTarget.dimension);
		if (world == null || !world.hasChunkAt(beaconTarget.position)) return false;
		final TileEntity tileEntity = world.getBlockEntity(beaconTarget.position);
		return tileEntity instanceof TransporterBeaconTileEntity
			&& ((TransporterBeaconTileEntity) tileEntity).isActive();
	}

	private void clearBeaconTarget() {
		beaconTarget = null;
		lockRequested = false;
		energizeRequested = false;
		parametersDirty = true;
	}

	private void notifyBeaconDone(final ServerWorld context) {
		if (beaconTarget == null || context.getServer() == null) return;
		final ServerWorld world = context.getServer().getLevel(beaconTarget.dimension);
		if (world != null && world.hasChunkAt(beaconTarget.position)) {
			final TileEntity tileEntity = world.getBlockEntity(beaconTarget.position);
			if (tileEntity instanceof TransporterBeaconTileEntity) {
				((TransporterBeaconTileEntity) tileEntity).energizeDone();
			}
		}
		clearBeaconTarget();
	}

	@Override public int getBeamFrequency() { return beamFrequency; }
	@Override public void setBeamFrequency(final int frequency) {
		if (!IBeamFrequency.isValid(frequency) || beamFrequency == frequency) return;
		beamFrequency = frequency;
		parametersDirty = true;
		setChanged();
	}

	public UUID getSignatureUUID() { return signatureUuid; }
	public String getSignatureName() { return signatureName; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		parametersDirty = true;
		setChanged();
	}

	public void setRemoteUuid(final UUID uuid) {
		targetType = uuid == null ? TargetType.NONE : TargetType.UUID;
		targetUuid = uuid;
		targetCoordinates = null;
		targetPlayer = "";
		parametersDirty = true;
		setChanged();
	}

	public void setRemoteCoordinates(final int x, final int y, final int z) {
		targetType = TargetType.COORDINATES;
		targetCoordinates = new BlockPos(x, y, z);
		targetUuid = null;
		targetPlayer = "";
		parametersDirty = true;
		setChanged();
	}

	public void setRemotePlayer(final String name) {
		targetType = name == null || name.isEmpty() ? TargetType.NONE : TargetType.PLAYER;
		targetPlayer = name == null ? "" : name;
		targetCoordinates = null;
		targetUuid = null;
		parametersDirty = true;
		setChanged();
	}

	public String getStateDescription() {
		return jammed ? jamReason : cooldown > 0
			? String.format("Cooling down %d s", Math.round(cooldown / 20.0D))
			: state.getSerializedName();
	}

	public String getStatus() {
		return String.format("Transporter %s: %s; frequency %d; %d scanner%s; lock %.1f%%; "
			+ "%d / %d FE; upgrades %s", enabled ? "enabled" : "disabled",
			getStateDescription(), beamFrequency, localScanners.size(),
			localScanners.size() == 1 ? "" : "s", lockStrength * 100.0D,
			getEnergyStored(), getMaxEnergyStored(), getUpgradeStatus());
	}

	public String getUpgradeStatus() {
		return String.format("storage %d/%d, focus %d/%d, range %d/%d",
			energyUpgrades, Upgrade.ENERGY.limit, focusUpgrades, Upgrade.FOCUS.limit,
			rangeUpgrades, Upgrade.RANGE.limit);
	}

	public boolean addUpgrade(final Upgrade upgrade) {
		final int count = getUpgradeCount(upgrade);
		if (count >= upgrade.limit) return false;
		setUpgradeCount(upgrade, count + 1);
		return true;
	}

	public boolean removeUpgrade(final Upgrade upgrade) {
		final int count = getUpgradeCount(upgrade);
		if (count <= 0) return false;
		setUpgradeCount(upgrade, count - 1);
		return true;
	}

	private int getUpgradeCount(final Upgrade upgrade) {
		switch (upgrade) {
		case ENERGY: return energyUpgrades;
		case FOCUS: return focusUpgrades;
		case RANGE: return rangeUpgrades;
		default: return 0;
		}
	}

	private void setUpgradeCount(final Upgrade upgrade, final int count) {
		switch (upgrade) {
		case ENERGY:
			energyUpgrades = MathHelper.clamp(count, 0, upgrade.limit);
			if (energyStored > getMaxEnergyStored()) energyStored = getMaxEnergyStored();
			break;
		case FOCUS: focusUpgrades = MathHelper.clamp(count, 0, upgrade.limit); break;
		case RANGE: rangeUpgrades = MathHelper.clamp(count, 0, upgrade.limit); break;
		}
		parametersDirty = true;
		setChanged();
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null) setEnabled(requested);
		return new Object[]{ enabled };
	}

	public Object[] name(@Nullable final String requested) {
		if (requested != null && !requested.isEmpty() && !requested.equals(signatureName)) {
			releaseChunks();
			signatureName = requested;
			signatureUuid = UUID.randomUUID();
			GlobalRegionRegistry.updateTransporter(this);
			setChanged();
		}
		return new Object[]{ signatureName, signatureUuid.toString() };
	}

	public Object[] beamFrequency(@Nullable final Integer requested) {
		if (requested != null) setBeamFrequency(requested);
		return new Object[]{ beamFrequency };
	}

	public Object[] state() {
		return new Object[]{ getStatus(), getStateDescription(), connected, enabled, jammed,
			getEnergyStored(), lockStrength };
	}

	public Object[] remoteLocation() {
		if (targetType == TargetType.COORDINATES && targetCoordinates != null) {
			return new Object[]{ targetCoordinates.getX(), targetCoordinates.getY(),
				targetCoordinates.getZ() };
		}
		if (targetType == TargetType.UUID && targetUuid != null) {
			return new Object[]{ targetUuid.toString() };
		}
		if (targetType == TargetType.PLAYER) return new Object[]{ targetPlayer };
		return new Object[]{ null };
	}

	public Object[] lock(@Nullable final Boolean requested) {
		if (requested != null) {
			lockRequested = requested;
			parametersDirty = true;
			setChanged();
		}
		return new Object[]{ lockRequested };
	}

	public Object[] energyFactor(@Nullable final Double requested) {
		if (requested != null && Double.isFinite(requested)) {
			energyFactor = MathHelper.clamp(requested, 1.0D, MAX_ENERGY_FACTOR);
			parametersDirty = true;
			setChanged();
		}
		return new Object[]{ energyFactor };
	}

	public Object[] getLockStrength() { return new Object[]{ lockStrength }; }
	public Object[] getEnergyRequired() {
		return new Object[]{ true, getEnergyRequired(TransporterState.ACQUIRING),
			getEnergyRequired(TransporterState.ENERGIZING) };
	}
	public Object[] energize(@Nullable final Boolean requested) {
		if (requested != null) {
			energizeRequested = requested;
			parametersDirty = true;
			setChanged();
		}
		return new Object[]{ energizeRequested };
	}
	public Object[] getLocalPosition() {
		return new Object[]{ worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() };
	}

	public void addEventListener(final TransporterEventListener listener) {
		eventListeners.add(listener);
	}
	public void removeEventListener(final TransporterEventListener listener) {
		eventListeners.remove(listener);
	}
	private void fireEvent(final String event, final Object... arguments) {
		for (final TransporterEventListener listener : eventListeners) {
			listener.queueEvent(event, arguments);
		}
	}

	public void onCoreBroken() {
		broken = true;
		if (level instanceof ServerWorld) {
			for (final BlockPos scanner : localScanners) setScannerActive((ServerWorld) level,
				scanner, false);
			GlobalRegionRegistry.removeTransporter(this);
		}
		releaseChunks();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide) {
			GlobalRegionRegistry.registerTransporter(this);
			scanTimer = 1;
			parametersDirty = true;
		}
	}

	@Override
	public void onChunkUnloaded() {
		releaseChunks();
		GlobalRegionRegistry.unregisterTransporter(this);
		super.onChunkUnloaded();
	}

	@Override
	public void setRemoved() {
		releaseChunks();
		if (broken) GlobalRegionRegistry.removeTransporter(this);
		else GlobalRegionRegistry.unregisterTransporter(this);
		super.setRemoved();
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		beamFrequency = tag.contains(IBeamFrequency.BEAM_FREQUENCY_TAG)
			? tag.getInt(IBeamFrequency.BEAM_FREQUENCY_TAG) : -1;
		enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		lockRequested = tag.getBoolean(TAG_LOCK);
		energizeRequested = tag.getBoolean(TAG_ENERGIZE);
		targetType = TargetType.byName(tag.getString(TAG_TARGET_TYPE));
		if (tag.contains(TAG_TARGET_X)) targetCoordinates = new BlockPos(tag.getInt(TAG_TARGET_X),
			tag.getInt(TAG_TARGET_Y), tag.getInt(TAG_TARGET_Z));
		targetUuid = tag.hasUUID(TAG_TARGET_UUID) ? tag.getUUID(TAG_TARGET_UUID) : null;
		targetPlayer = tag.getString(TAG_TARGET_PLAYER);
		energyFactor = MathHelper.clamp(tag.contains(TAG_FACTOR) ? tag.getDouble(TAG_FACTOR) : 1.0D,
			1.0D, MAX_ENERGY_FACTOR);
		lockStrength = Math.max(0.0D, tag.getDouble(TAG_LOCK_STRENGTH));
		cooldown = Math.max(0, tag.getInt(TAG_COOLDOWN));
		energizingTicks = MathHelper.clamp(tag.getInt(TAG_ENERGIZING_TICKS), 0, CHARGING_TICKS);
		state = TransporterState.byName(tag.getString(TAG_STATE));
		signatureUuid = tag.hasUUID(TAG_SIGNATURE) ? tag.getUUID(TAG_SIGNATURE) : UUID.randomUUID();
		signatureName = tag.getString(TAG_NAME);
		if (signatureName.isEmpty()) signatureName = "Transporter";
		energyUpgrades = MathHelper.clamp(tag.getInt(TAG_UPGRADE_ENERGY), 0, Upgrade.ENERGY.limit);
		focusUpgrades = MathHelper.clamp(tag.getInt(TAG_UPGRADE_FOCUS), 0, Upgrade.FOCUS.limit);
		rangeUpgrades = MathHelper.clamp(tag.getInt(TAG_UPGRADE_RANGE), 0, Upgrade.RANGE.limit);
		localScanners.clear();
		for (final long value : tag.getLongArray(TAG_SCANNERS)) localScanners.add(BlockPos.of(value));
		localContainments.clear();
		for (final long value : tag.getLongArray(TAG_CONTAINMENTS)) {
			localContainments.add(BlockPos.of(value));
		}
		parametersDirty = true;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putInt(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		tag.putBoolean(TAG_ENABLED, enabled);
		tag.putBoolean(TAG_LOCK, lockRequested);
		tag.putBoolean(TAG_ENERGIZE, energizeRequested);
		tag.putString(TAG_TARGET_TYPE, targetType.name);
		if (targetCoordinates != null) {
			tag.putInt(TAG_TARGET_X, targetCoordinates.getX());
			tag.putInt(TAG_TARGET_Y, targetCoordinates.getY());
			tag.putInt(TAG_TARGET_Z, targetCoordinates.getZ());
		}
		if (targetUuid != null) tag.putUUID(TAG_TARGET_UUID, targetUuid);
		tag.putString(TAG_TARGET_PLAYER, targetPlayer);
		tag.putDouble(TAG_FACTOR, energyFactor);
		tag.putDouble(TAG_LOCK_STRENGTH, lockStrength);
		tag.putInt(TAG_COOLDOWN, cooldown);
		tag.putInt(TAG_ENERGIZING_TICKS, energizingTicks);
		tag.putString(TAG_STATE, state.getSerializedName());
		tag.putUUID(TAG_SIGNATURE, signatureUuid);
		tag.putString(TAG_NAME, signatureName);
		tag.putInt(TAG_UPGRADE_ENERGY, energyUpgrades);
		tag.putInt(TAG_UPGRADE_FOCUS, focusUpgrades);
		tag.putInt(TAG_UPGRADE_RANGE, rangeUpgrades);
		tag.putLongArray(TAG_SCANNERS, localScanners.stream().mapToLong(BlockPos::asLong).toArray());
		tag.putLongArray(TAG_CONTAINMENTS,
			localContainments.stream().mapToLong(BlockPos::asLong).toArray());
		return tag;
	}

	public enum Upgrade {
		ENERGY("capacitive_crystal", 8), FOCUS("emerald_crystal", 2), RANGE("ender_coil", 8);
		private final String component;
		private final int limit;
		Upgrade(final String component, final int limit) { this.component = component; this.limit = limit; }
		public Item getItem() { return Registration.COMPONENTS.get(component).get(); }
	}

	public interface TransporterEventListener {
		void queueEvent(String event, Object... arguments);
	}

	private enum TargetType {
		NONE("none"), COORDINATES("coordinates"), UUID("uuid"), PLAYER("player");
		private final String name;
		TargetType(final String name) { this.name = name; }
		private static TargetType byName(final String name) {
			for (final TargetType value : values()) if (value.name.equals(name)) return value;
			return NONE;
		}
	}

	private static final class FocusValues {
		@Nullable private final List<BlockPos> scanners;
		private final int rangeUpgrades;
		private final double speed;
		private final double strength;
		private FocusValues(@Nullable final List<BlockPos> scanners, final int rangeUpgrades,
		                    final double speed, final double strength) {
			this.scanners = scanners;
			this.rangeUpgrades = rangeUpgrades;
			this.speed = speed;
			this.strength = strength;
		}
	}

	private static final class EntityValues {
		private final int count;
		private final double mass;
		private EntityValues(final int count, final double mass) {
			this.count = count;
			this.mass = mass;
		}
	}

	private static final class RemoteTarget {
		private final ServerWorld world;
		private final BlockPos position;
		private RemoteTarget(final ServerWorld world, final BlockPos position) {
			this.world = world;
			this.position = position.immutable();
		}
		@Override public boolean equals(final Object other) {
			return other instanceof RemoteTarget
				&& world.dimension().equals(((RemoteTarget) other).world.dimension())
				&& position.equals(((RemoteTarget) other).position);
		}
		@Override public int hashCode() { return Objects.hash(world.dimension(), position); }
	}

	private static final class BeaconTarget {
		private final RegistryKey<World> dimension;
		private final BlockPos position;
		private BeaconTarget(final RegistryKey<World> dimension, final BlockPos position) {
			this.dimension = dimension;
			this.position = position.immutable();
		}
		@Override public boolean equals(final Object other) {
			return other instanceof BeaconTarget
				&& dimension.equals(((BeaconTarget) other).dimension)
				&& position.equals(((BeaconTarget) other).position);
		}
		@Override public int hashCode() { return Objects.hash(dimension, position); }
	}

	private static final class TicketRef {
		private final RegistryKey<World> dimension;
		private final ChunkPos chunk;
		private TicketRef(final RegistryKey<World> dimension, final ChunkPos chunk) {
			this.dimension = dimension;
			this.chunk = chunk;
		}
		@Override public boolean equals(final Object other) {
			return other instanceof TicketRef && dimension.equals(((TicketRef) other).dimension)
				&& chunk.equals(((TicketRef) other).chunk);
		}
		@Override public int hashCode() { return Objects.hash(dimension, chunk); }
	}

	private static final class CapturedEntity {
		private final boolean invalid;
		@Nullable private final UUID uuid;
		@Nullable private final RegistryKey<World> dimension;
		private final Vector3d originalPosition;
		private CapturedEntity(final Entity entity) {
			invalid = false;
			uuid = entity.getUUID();
			dimension = entity.level.dimension();
			originalPosition = entity.position();
		}
		private CapturedEntity() {
			invalid = true;
			uuid = null;
			dimension = null;
			originalPosition = Vector3d.ZERO;
		}
		private static CapturedEntity invalid() { return new CapturedEntity(); }
		@Nullable private Entity resolve(@Nullable final net.minecraft.server.MinecraftServer server) {
			if (invalid || uuid == null || dimension == null || server == null) return null;
			final ServerWorld world = server.getLevel(dimension);
			return world == null ? null : world.getEntity(uuid);
		}
		private double distanceSquared(final Entity entity) {
			return entity.level.dimension().equals(dimension)
				? entity.position().distanceToSqr(originalPosition) : Double.POSITIVE_INFINITY;
		}
		private double mass(@Nullable final net.minecraft.server.MinecraftServer server) {
			final Entity entity = resolve(server);
			if (entity == null) return 0.0D;
			final CompoundNBT tag = new CompoundNBT();
			if (!entity.save(tag)) return 0.25D;
			try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
				CompressedStreamTools.writeCompressed(tag, output);
				return MathHelper.clamp(output.size() / 80_000.0D, 0.25D, 4.0D);
			} catch (final IOException exception) {
				return MathHelper.clamp(Math.sqrt(tag.toString().length()) / 300.0D,
					0.25D, 4.0D);
			}
		}
	}
}
