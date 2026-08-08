package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.forcefield.ForceFieldProjectorTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveTags;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.network.BeamEffectPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Laser cannon, ported from 1.12.2 {@code TileEntityLaser}.
 *
 * The cannon is computer fired. It waits the original charge delay, drains up to ten adjacent
 * same-tier laser media, accepts same-frequency booster beams, then traces entities and blocks.
 * Frequency 1420 is the original non-destructive scanner mode. Destructive shots retain the
 * original energy attenuation, entity damage, hardness costs, absorption chance and explosions.
 */
public class LaserTileEntity extends AbstractLaserTileEntity
	implements ITickableTileEntity, IBeamFrequency {

	private static final int MAX_MEDIUMS_COUNT = 10;
	private static final int MAX_LASER_ENERGY = 3_400_000;
	private static final int FIRE_DELAY_TICKS = 5;
	private static final int SCAN_DELAY_TICKS = 1;
	private static final double BOOSTER_EFFICIENCY = 0.60D;
	private static final double ATTENUATION_PER_AIR_BLOCK = 0.000200D;
	private static final double ATTENUATION_PER_VOID_BLOCK = 0.000005D;
	private static final int RANGE_MAX = 500;

	private static final int ENTITY_HIT_FIRE_SECONDS = 20;
	private static final int ENTITY_HIT_ENERGY = 15_000;
	private static final int ENTITY_HIT_BASE_DAMAGE = 3;
	private static final int ENTITY_HIT_ENERGY_PER_DAMAGE = 30_000;
	private static final int ENTITY_HIT_MAX_DAMAGE = 100;
	private static final int ENTITY_EXPLOSION_THRESHOLD = 900_000;
	private static final float ENTITY_EXPLOSION_BASE_STRENGTH = 4.0F;
	private static final int ENTITY_EXPLOSION_ENERGY_PER_STRENGTH = 125_000;
	private static final float ENTITY_EXPLOSION_MAX_STRENGTH = 4.0F;

	private static final int BLOCK_HIT_ENERGY_MIN = 75_000;
	private static final int BLOCK_HIT_ENERGY_PER_HARDNESS = 150_000;
	private static final int BLOCK_HIT_ENERGY_MAX = 750_000;
	private static final double BLOCK_ABSORPTION_PER_HARDNESS = 0.01D;
	private static final double BLOCK_ABSORPTION_MAX = 0.80D;
	private static final float BLOCK_EXPLOSION_HARDNESS_THRESHOLD = 5.0F;
	private static final float BLOCK_EXPLOSION_BASE_STRENGTH = 8.0F;
	private static final int BLOCK_EXPLOSION_ENERGY_PER_STRENGTH = 125_000;
	private static final float BLOCK_EXPLOSION_MAX_STRENGTH = 50.0F;

	private static final String TAG_BEAM_FREQUENCY = IBeamFrequency.BEAM_FREQUENCY_TAG;

	private float yaw;
	private float pitch;
	private int beamFrequency = -1;
	private boolean emitting;
	private int delayTicks;
	private int energyFromOtherBeams;

	private ScanResultType scanResultType = ScanResultType.IDLE;
	@Nullable
	private BlockPos scanResultPosition;
	@Nullable
	private String scanResultBlockName;
	private int scanResultBlockMetadata;
	private float scanResultBlockResistance = -2.0F;

	private final Set<LaserEventListener> eventListeners = new HashSet<>();

	private enum ScanResultType {
		IDLE, BLOCK, NONE
	}

	public interface LaserEventListener {
		void queueEvent(String eventName, Object... arguments);
	}

	private static final class EntityHit {
		private final Entity entity;
		private final Vector3d location;
		private final double distance;

		private EntityHit(final Entity entity, final Vector3d location, final double distance) {
			this.entity = entity;
			this.location = location;
			this.distance = distance;
		}
	}

	public LaserTileEntity() {
		this(Registration.LASER_TILE.get());
	}

	protected LaserTileEntity(final TileEntityType<?> tileEntityType) {
		super(tileEntityType);
	}

	@Override
	protected Direction[] getValidLaserMediumDirections() {
		return Direction.values();
	}

	@Override
	protected int getMaxLaserMediumCount() {
		return MAX_MEDIUMS_COUNT;
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || !emitting || !IBeamFrequency.isValid(beamFrequency)) {
			return;
		}

		delayTicks++;
		final int delayRequired = beamFrequency == BEAM_FREQUENCY_SCANNING
		                        ? SCAN_DELAY_TICKS : FIRE_DELAY_TICKS;
		if (delayTicks <= delayRequired) {
			return;
		}

		delayTicks = 0;
		emitting = false;
		final int mediumEnergy = consumeLaserMediumEnergy(Integer.MAX_VALUE, false);
		final int beamEnergy = Math.min(MAX_LASER_ENERGY,
			mediumEnergy + MathHelper.floor(energyFromOtherBeams * BOOSTER_EFFICIENCY));
		energyFromOtherBeams = 0;
		emitBeam(beamEnergy);
		queueEvent("laserSend", beamFrequency, beamEnergy);
	}

	public Object[] initiateBeamEmission(final double yaw, final double pitch) {
		this.yaw = (float) yaw;
		this.pitch = (float) pitch;
		delayTicks = 0;
		emitting = true;
		setChanged();
		return new Object[]{ true };
	}

	public Object[] initiateBeamEmissionVector(final double deltaX, final double deltaY,
	                                           final double deltaZ) {
		// Preserve the legacy ComputerCraft coordinate conversion.
		final double x = -deltaX;
		final double y = -deltaY;
		final double z = deltaZ;
		final double horizontalDistance = Math.sqrt(x * x + z * z);
		return initiateBeamEmission(
			Math.atan2(x, z) * 180.0D / Math.PI,
			Math.atan2(y, horizontalDistance) * 180.0D / Math.PI);
	}

	private void addBeamEnergy(final int amount) {
		if (emitting && amount > 0) {
			energyFromOtherBeams = Math.min(MAX_LASER_ENERGY,
				energyFromOtherBeams + amount);
		}
	}

	private void emitBeam(final int beamEnergy) {
		if (!(level instanceof ServerWorld) || beamEnergy <= 0 || !IBeamFrequency.isValid(beamFrequency)) {
			return;
		}
		int energy = beamEnergy;
		final int beamLength = MathHelper.clamp(energy / 200, 0, RANGE_MAX);
		if (beamLength <= 0) {
			return;
		}

		final Vector3d direction = directionFromAngles(yaw, pitch);
		final Vector3d source = Vector3d.atCenterOf(getBlockPos()).add(direction);
		final Vector3d target = source.add(direction.scale(beamLength));
		playLaserSound(energy);

		if (beamFrequency == BEAM_FREQUENCY_SCANNING) {
			emitScanningBeam(source, target, energy);
			return;
		}

		final List<EntityHit> entityHits = rayTraceEntities(source, target);
		final Set<Entity> processedEntities = new HashSet<>();
		Vector3d hitPoint = target;
		Vector3d raySource = source;
		double distanceTravelled = 0.0D;

		for (int passedBlocks = 0; passedBlocks < beamLength && energy > 0; passedBlocks++) {
			final BlockRayTraceResult blockHit = rayTraceBlock(raySource, target);
			final double blockHitDistance = blockHit == null
			                              ? beamLength + 0.1D
			                              : blockHit.getLocation().distanceTo(source);

			for (final EntityHit entityHit : entityHits) {
				if (processedEntities.contains(entityHit.entity)) {
					continue;
				}
				if (entityHit.distance >= blockHitDistance) {
					break;
				}
				processedEntities.add(entityHit.entity);
				energy = MathHelper.floor(energy * getTransmittance(entityHit.distance - distanceTravelled));
				energy -= ENTITY_HIT_ENERGY;
				distanceTravelled = entityHit.distance;
				hitPoint = entityHit.location;
				if (energy <= 0) {
					break;
				}
				applyEntityHit(entityHit.entity, energy);
			}
			if (energy <= 0) {
				break;
			}

			if (blockHit == null || blockHitDistance >= beamLength) {
				hitPoint = target;
				break;
			}

			final BlockPos hitPos = blockHit.getBlockPos();
			final BlockState blockState = level.getBlockState(hitPos);
			final TileEntity hitTileEntity = level.getBlockEntity(hitPos);
			if (hitTileEntity instanceof ForceFieldTileEntity) {
				final ForceFieldTileEntity forceField = (ForceFieldTileEntity) hitTileEntity;
				if (forceField.getBeamFrequency() == beamFrequency) {
					// Matching frequencies are phase-transparent. Advance the trace origin beyond this
					// block so the next clip cannot select the same field face again.
					raySource = blockHit.getLocation().add(direction.scale(0.05D));
					hitPoint = blockHit.getLocation();
					continue;
				}
				final ForceFieldProjectorTileEntity projector = forceField.getProjector();
				if (projector != null) projector.absorbLaserEnergy(energy);
				hitPoint = blockHit.getLocation();
				break;
			}
			if (hitTileEntity instanceof LaserTileEntity
			 && ((LaserTileEntity) hitTileEntity).getBeamFrequency() == beamFrequency) {
				((LaserTileEntity) hitTileEntity).addBeamEnergy(energy);
				hitPoint = blockHit.getLocation();
				break;
			}

			final float hardness = blockState.getDestroySpeed(level, hitPos);
			if (!canBreakBlock(hitPos)) {
				hitPoint = blockHit.getLocation();
				break;
			}
			if (hardness < 0.0F) {
				final float strength = clamp((float) (BLOCK_EXPLOSION_BASE_STRENGTH
					+ energy / (double) BLOCK_EXPLOSION_ENERGY_PER_STRENGTH),
					0.0F, BLOCK_EXPLOSION_MAX_STRENGTH);
				level.explode(null, blockHit.getLocation().x, blockHit.getLocation().y,
					blockHit.getLocation().z, strength, true, Explosion.Mode.BREAK);
				hitPoint = blockHit.getLocation();
				break;
			}

			final int energyCost = MathHelper.clamp(
				Math.round(hardness * BLOCK_HIT_ENERGY_PER_HARDNESS),
				BLOCK_HIT_ENERGY_MIN, BLOCK_HIT_ENERGY_MAX);
			final double absorptionChance = clamp(
				hardness * BLOCK_ABSORPTION_PER_HARDNESS, 0.0D, BLOCK_ABSORPTION_MAX);
			energy = MathHelper.floor(energy * getTransmittance(blockHitDistance - distanceTravelled));
			do {
				energy -= energyCost;
				distanceTravelled = blockHitDistance;
				hitPoint = blockHit.getLocation();
			} while (energy > 0 && level.random.nextDouble() <= absorptionChance);
			if (energy <= 0) {
				break;
			}

			if (hardness >= BLOCK_EXPLOSION_HARDNESS_THRESHOLD) {
				final float strength = clamp((float) (BLOCK_EXPLOSION_BASE_STRENGTH
					+ energy / (double) BLOCK_EXPLOSION_ENERGY_PER_STRENGTH),
					0.0F, BLOCK_EXPLOSION_MAX_STRENGTH);
				level.explode(null, blockHit.getLocation().x, blockHit.getLocation().y,
					blockHit.getLocation().z, strength, true, Explosion.Mode.BREAK);
			} else {
				level.destroyBlock(hitPos, false, getLaserFakePlayer());
			}
		}

		sendBeamEffect(source.add(direction.scale(-0.5D)), hitPoint, energy, beamLength);
	}

	private void emitScanningBeam(final Vector3d source, final Vector3d target, final int energy) {
		final BlockRayTraceResult hit = rayTraceBlock(source, target);
		scanResultBlockName = null;
		scanResultBlockMetadata = 0;
		scanResultBlockResistance = -2.0F;
		final Vector3d hitPoint;
		if (hit == null) {
			scanResultType = ScanResultType.NONE;
			scanResultPosition = new BlockPos(target);
			hitPoint = target;
		} else {
			scanResultType = ScanResultType.BLOCK;
			scanResultPosition = hit.getBlockPos();
			final BlockState blockState = level.getBlockState(scanResultPosition);
			scanResultBlockName = blockState.getBlock().getDescriptionId();
			scanResultBlockResistance = blockState.getBlock().getExplosionResistance();
			hitPoint = hit.getLocation();
		}
		sendBeamEffect(source, hitPoint, energy, 200);
		queueEvent("laserScanning", scanResultType.name(),
			scanResultPosition.getX(), scanResultPosition.getY(), scanResultPosition.getZ(),
			scanResultBlockName, scanResultBlockMetadata, scanResultBlockResistance);
	}

	@Nullable
	private BlockRayTraceResult rayTraceBlock(final Vector3d source, final Vector3d target) {
		final BlockRayTraceResult result = level.clip(new RayTraceContext(source, target,
			RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, null));
		return result.getType() == RayTraceResult.Type.BLOCK ? result : null;
	}

	private List<EntityHit> rayTraceEntities(final Vector3d source, final Vector3d target) {
		final AxisAlignedBB scanBox = new AxisAlignedBB(source, target).inflate(2.0D);
		final List<EntityHit> hits = new ArrayList<>();
		for (final Entity entity : level.getEntities((Entity) null, scanBox,
			candidate -> candidate.isPickable() && isLaserTarget(candidate))) {
			final AxisAlignedBB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius());
			hitBox.clip(source, target).ifPresent(location ->
				hits.add(new EntityHit(entity, location, source.distanceTo(location))));
		}
		hits.sort(Comparator.comparingDouble(hit -> hit.distance));
		return hits;
	}

	private static boolean isLaserTarget(final Entity entity) {
		return entity instanceof LivingEntity
		    || entity.getType().is(WarpDriveTags.NON_LIVING_TARGETS);
	}

	private void applyEntityHit(final Entity entity, final int energy) {
		entity.setSecondsOnFire(ENTITY_HIT_FIRE_SECONDS);
		if (entity instanceof LivingEntity) {
			final float damage = clamp((float) (ENTITY_HIT_BASE_DAMAGE
				+ energy / (double) ENTITY_HIT_ENERGY_PER_DAMAGE), 0.0F, ENTITY_HIT_MAX_DAMAGE);
			entity.hurt(DamageSource.IN_FIRE, damage);
		} else {
			entity.kill();
		}

		if (energy > ENTITY_EXPLOSION_THRESHOLD) {
			final float strength = clamp((float) (ENTITY_EXPLOSION_BASE_STRENGTH
				+ energy / (double) ENTITY_EXPLOSION_ENERGY_PER_STRENGTH),
				0.0F, ENTITY_EXPLOSION_MAX_STRENGTH);
			level.explode(null, entity.getX(), entity.getY(), entity.getZ(),
				strength, true, Explosion.Mode.BREAK);
		}
	}

	private boolean canBreakBlock(final BlockPos blockPos) {
		if (!(level instanceof ServerWorld)) {
			return false;
		}
		final ServerPlayerEntity fakePlayer = getLaserFakePlayer();
		return ForgeHooks.onBlockBreakEvent(level, GameType.SURVIVAL, fakePlayer, blockPos) >= 0;
	}

	private ServerPlayerEntity getLaserFakePlayer() {
		return FakePlayerFactory.getMinecraft((ServerWorld) level);
	}

	private double getTransmittance(final double distance) {
		if (distance <= 0.0D) {
			return 1.0D;
		}
		// The current dimension model has atmosphere everywhere except simulated space/hyperspace.
		final double attenuation = ChunkHandler.isSimulated(level)
		                         ? ATTENUATION_PER_VOID_BLOCK : ATTENUATION_PER_AIR_BLOCK;
		return Math.exp(-attenuation * distance);
	}

	private static Vector3d directionFromAngles(final float yaw, final float pitch) {
		final float yawZ = MathHelper.cos(-yaw * ((float) Math.PI / 180.0F) - (float) Math.PI);
		final float yawX = MathHelper.sin(-yaw * ((float) Math.PI / 180.0F) - (float) Math.PI);
		final float pitchHorizontal = -MathHelper.cos(-pitch * ((float) Math.PI / 180.0F));
		final float pitchVertical = MathHelper.sin(-pitch * ((float) Math.PI / 180.0F));
		return new Vector3d(yawX * pitchHorizontal, pitchVertical, yawZ * pitchHorizontal).normalize();
	}

	private void playLaserSound(final int energy) {
		if (energy <= 500_000) {
			level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
				SoundCategory.HOSTILE, 1.0F, 1.0F);
		} else if (energy <= 1_000_000) {
			level.playSound(null, getBlockPos(), Registration.SOUND_LASER_MEDIUM.get(),
				SoundCategory.HOSTILE, 1.0F, 1.0F);
		} else {
			level.playSound(null, getBlockPos(), Registration.SOUND_LASER_HIGH.get(),
				SoundCategory.HOSTILE, 1.0F, 1.0F);
		}
	}

	private void sendBeamEffect(final Vector3d source, final Vector3d target,
	                            final int remainingEnergy, final int originalRange) {
		final Vector3f color = IBeamFrequency.getBeamColor(beamFrequency);
		WarpDriveNetwork.CHANNEL.send(
			PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
				getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),
				Math.max(96.0D, originalRange + 32.0D), level.dimension())),
			new BeamEffectPacket(source, target, color.x(), color.y(), color.z(), remainingEnergy, 50));
	}

	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}

	@Override
	public void setBeamFrequency(final int beamFrequency) {
		if (this.beamFrequency == beamFrequency || !IBeamFrequency.isValid(beamFrequency)) {
			return;
		}
		this.beamFrequency = beamFrequency;
		setChanged();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	public Object[] setOrGetBeamFrequency(@Nullable final Integer value) {
		if (value != null) {
			setBeamFrequency(value);
		}
		return new Object[]{ getBeamFrequency() };
	}

	public Object[] getEnergyRequired() {
		return new Object[]{ true, MAX_LASER_ENERGY };
	}

	public Object[] getEnergyStatus() {
		return new Object[]{ getLaserMediumEnergyStored(), getLaserMediumMaxStorage(), "FE" };
	}

	public Object[] laserMediumDirection() {
		final Direction direction = getLaserMediumDirection();
		return direction == null
		     ? new Object[]{ "NONE", 0, 0, 0 }
		     : new Object[]{ direction.getName(), direction.getStepX(), direction.getStepY(),
		                     direction.getStepZ() };
	}

	public Object[] laserMediumCount() {
		return new Object[]{ getLaserMediumCount() };
	}

	public Object[] getScanResult() {
		if (scanResultType == ScanResultType.IDLE || scanResultPosition == null) {
			return new Object[]{ ScanResultType.IDLE.name(), 0, 0, 0, null, 0, -1.0F };
		}
		final Object[] result = {
			scanResultType.name(), scanResultPosition.getX(), scanResultPosition.getY(),
			scanResultPosition.getZ(), scanResultBlockName, scanResultBlockMetadata,
			scanResultBlockResistance };
		scanResultType = ScanResultType.IDLE;
		scanResultPosition = null;
		scanResultBlockName = null;
		scanResultBlockMetadata = 0;
		scanResultBlockResistance = -2.0F;
		return result;
	}

	public void addEventListener(final LaserEventListener listener) {
		eventListeners.add(listener);
	}

	public void removeEventListener(final LaserEventListener listener) {
		eventListeners.remove(listener);
	}

	private void queueEvent(final String eventName, final Object... arguments) {
		for (final LaserEventListener listener : new ArrayList<>(eventListeners)) {
			try {
				listener.queueEvent(eventName, arguments);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.warn("Unable to deliver {} from laser at {}", eventName, getBlockPos(), exception);
			}
		}
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		final int storedFrequency = tagCompound.getInt(TAG_BEAM_FREQUENCY);
		beamFrequency = IBeamFrequency.isValid(storedFrequency) ? storedFrequency : -1;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		if (IBeamFrequency.isValid(beamFrequency)) {
			tagCompound.putInt(TAG_BEAM_FREQUENCY, beamFrequency);
		}
		return tagCompound;
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		return save(new CompoundNBT());
	}

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getBlockPos(), 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager networkManager, final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}

	@Override
	public void setRemoved() {
		eventListeners.clear();
		super.setRemoved();
	}

	private static float clamp(final float value, final float minimum, final float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static double clamp(final double value, final double minimum, final double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
