package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Full server simulation for the tiered enantiomorphic reactor.
 *
 * <p>The equations, assembly coordinates, boot hold, stabilization anti-spam, output policies and
 * destructive failure are direct ports of the 1.12.2 machine. Forge Energy replaces the old
 * multi-API energy wrapper at the boundary; the reactor still generates and accounts in FE.</p>
 */
public class EnanReactorCoreTileEntity extends AbstractEnergyTileEntity
	implements ITickableTileEntity {

	private static final String TAG_ENABLED = "enabled";
	private static final String TAG_OUTPUT_MODE = "outputMode";
	private static final String TAG_OUTPUT_THRESHOLD = "outputThreshold";
	private static final String TAG_INSTABILITY_TARGET = "instabilityTarget";
	private static final String TAG_STABILIZER_ENERGY = "stabilizerEnergy";
	private static final String TAG_INSTABILITY = "instability";
	private static final String TAG_SIGNATURE = "signatureUuid";
	private static final String TAG_NAME = "name";

	private static final int UPDATE_INTERVAL_TICKS = 5;
	private static final int ASSEMBLY_SCAN_INTERVAL_TICKS = 100;
	private static final int FREEZE_INTERVAL_TICKS = 40;
	private static final double INSTABILITY_MIN = 0.004D;
	private static final double INSTABILITY_MAX = 0.060D;
	private static final double MAX_LASER_ENERGY = 200_000.0D;
	private static final double MAX_LASER_EFFECT = INSTABILITY_MAX * 20.0D / 0.33D;
	private static final Set<EnanReactorCoreTileEntity> LIVE_REACTORS =
		java.util.Collections.newSetFromMap(new WeakHashMap<>());

	private EnanReactorTier tier;
	private EnanReactorOutputMode outputMode = EnanReactorOutputMode.OFF;
	private int outputThreshold;
	private double instabilityTarget = 50.0D;
	private int stabilizerEnergy = 10_000;
	private boolean enabled;
	private UUID signatureUuid = UUID.randomUUID();
	private String name = "";
	private final double[] instabilities =
		new double[EnanReactorFace.MAX_INSTABILITIES];

	private boolean hold = true;
	private boolean assemblyDirty = true;
	private boolean assemblyValid;
	private String assemblyStatus = "Assembly not scanned";
	private int assemblyScanTicks;
	private int updateTicks;
	private float lasersReceived;
	private int lastGenerationRate;
	private int releasedThisTick;
	private long releasedThisCycle;
	private long energyReleasedLastCycle;
	private boolean broken;

	@SuppressWarnings("unchecked")
	private final WeakReference<EnanReactorLaserTileEntity>[] laserReferences =
		(WeakReference<EnanReactorLaserTileEntity>[]) new WeakReference<?>[
			EnanReactorFace.MAX_INSTABILITIES];
	private final Set<ReactorEventListener> eventListeners = new HashSet<>();

	public interface ReactorEventListener {
		void queueEvent(String eventName, Object... arguments);
	}

	public EnanReactorCoreTileEntity() {
		this(EnanReactorTier.BASIC);
	}

	public EnanReactorCoreTileEntity(final EnanReactorTier tier) {
		super(Registration.ENAN_REACTOR_CORE_TILE.get());
		this.tier = tier;
	}

	public EnanReactorTier getTier() {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof EnanReactorCoreBlock) {
			tier = ((EnanReactorCoreBlock) blockState.getBlock()).getTier();
		}
		return tier;
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || broken) return;

		if (assemblyDirty || --assemblyScanTicks <= 0) {
			assemblyDirty = false;
			assemblyScanTicks = ASSEMBLY_SCAN_INTERVAL_TICKS;
			scanAssembly();
		}

		releasedThisTick = 0;
		lasersReceived = Math.max(0.0F, lasersReceived - 0.05F);
		if (--updateTicks > 0) return;
		updateTicks = UPDATE_INTERVAL_TICKS;
		energyReleasedLastCycle = releasedThisCycle;
		releasedThisCycle = 0;
		refreshBlockState();

		if (!hold) {
			if (shouldExplode()) {
				explode();
				return;
			}
			increaseInstability();
			generateOrDecayEnergy();
			runControlLoop();
		}
		queueEvent("reactorPulse", lastGenerationRate);
	}

	public void markAssemblyDirty() {
		assemblyDirty = true;
	}

	/** Prompt exact-footprint rescans after Forge place/break events without force-loading chunks. */
	public static void notifyBlockChanged(final World world, final BlockPos changedPosition) {
		if (world == null || world.isClientSide || changedPosition == null) return;
		final List<EnanReactorCoreTileEntity> snapshot;
		synchronized (LIVE_REACTORS) {
			snapshot = new ArrayList<>(LIVE_REACTORS);
		}
		for (final EnanReactorCoreTileEntity core : snapshot) {
			if (core.isRemoved() || core.level != world) continue;
			final BlockPos relative = changedPosition.subtract(core.worldPosition);
			for (final EnanReactorFace face : EnanReactorFace.getFaces(core.getTier())) {
				if (face.getOffset().equals(relative)) {
					core.markAssemblyDirty();
					break;
				}
			}
		}
	}

	private void scanAssembly() {
		if (level == null) return;
		boolean valid = true;
		final List<String> issues = new ArrayList<>();
		for (final EnanReactorFace face : EnanReactorFace.getFaces(getTier())) {
			final BlockPos target = worldPosition.offset(face.getOffset());
			if (!level.hasChunkAt(target)) {
				valid = false;
				hold = true;
				issues.add("Unloaded reactor position " + format(target));
				continue;
			}
			if (face.isLaser()) {
				final TileEntity tileEntity = level.getBlockEntity(target);
				if (tileEntity instanceof EnanReactorLaserTileEntity) {
					final EnanReactorLaserTileEntity laser =
						(EnanReactorLaserTileEntity) tileEntity;
					laser.setReactorFace(face, this);
					laserReferences[face.getInstabilityIndex()] = new WeakReference<>(laser);
				} else {
					valid = false;
					issues.add("Missing stabilization laser at " + format(target));
					laserReferences[face.getInstabilityIndex()] = null;
				}
			} else if (!level.getBlockState(target).isAir()) {
				valid = false;
				issues.add("Non-air block at " + format(target));
			}
		}
		assemblyValid = valid;
		assemblyStatus = issues.isEmpty() ? "ok" : String.join("; ", issues);
		setChanged();
	}

	private void increaseInstability() {
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			final int index = face.getInstabilityIndex();
			if (energyStored > 2_000) {
				instabilities[index] += calculateInstabilityIncrease(
					level.random.nextDouble(), energyStored, getMaxEnergyStored());
			} else {
				instabilities[index] = Math.max(0.0D,
					instabilities[index] - calculateNaturalStabilization(instabilities[index]));
			}
		}
	}

	private void generateOrDecayEnergy() {
		if (enabled) {
			final int generated = calculateGeneration(getTier(), energyStored, instabilities);
			energyStored = (int) Math.min((long) getMaxEnergyStored(),
				(long) energyStored + generated);
			lastGenerationRate = generated / UPDATE_INTERVAL_TICKS;
		} else {
			final int decayed = calculateDecay(getTier(), energyStored, instabilities);
			energyStored = Math.max(0, energyStored - decayed);
			lastGenerationRate = 0;
		}
		setChanged();
	}

	private void runControlLoop() {
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			if (instabilities[face.getInstabilityIndex()] <= instabilityTarget) continue;
			final EnanReactorLaserTileEntity laser = getLaser(face);
			if (laser != null && laser.stabilize(stabilizerEnergy) == -stabilizerEnergy) {
				hold = true;
			}
		}
		if (!hold) return;

		updateTicks = Math.max(updateTicks, FREEZE_INTERVAL_TICKS);
		markAssemblyDirty();
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			final int index = face.getInstabilityIndex();
			if (instabilities[index] > instabilityTarget) {
				instabilities[index] = instabilityTarget;
			}
		}
	}

	@Nullable
	private EnanReactorLaserTileEntity getLaser(final EnanReactorFace face) {
		final int index = face.getInstabilityIndex();
		final WeakReference<EnanReactorLaserTileEntity> reference = laserReferences[index];
		final EnanReactorLaserTileEntity cached = reference == null ? null : reference.get();
		if (cached != null && !cached.isRemoved() && cached.getReactorFace() == face) return cached;
		if (level == null) return null;
		final BlockPos target = worldPosition.offset(face.getOffset());
		if (!level.hasChunkAt(target)) return null;
		final TileEntity tileEntity = level.getBlockEntity(target);
		if (!(tileEntity instanceof EnanReactorLaserTileEntity)) return null;
		final EnanReactorLaserTileEntity laser = (EnanReactorLaserTileEntity) tileEntity;
		laser.setReactorFace(face, this);
		laserReferences[index] = new WeakReference<>(laser);
		return laser;
	}

	public void decreaseInstability(final EnanReactorFace face, final int energy) {
		if (level == null || face == null || face.getInstabilityIndex() < 0 || energy <= 1) return;
		lasersReceived = Math.min(10.0F,
			lasersReceived + 1.0F / getTier().getMaxLasersPerSecond());
		double noSpamFactor = 1.0D;
		if (lasersReceived > 1.0F) {
			noSpamFactor = 0.5D;
			final Direction facing = face.getLaserFacing();
			final BlockPos source = facing == null
				? worldPosition.offset(face.getOffset())
				: worldPosition.offset(face.getOffset()).relative(facing.getOpposite());
			level.explode(null, source.getX() + 0.5D, source.getY() + 0.5D,
				source.getZ() + 0.5D, 1.0F, false, Explosion.Mode.NONE);
		}
		final int index = face.getInstabilityIndex();
		final double removed = calculateLaserEffect(energy, level.random.nextDouble(), noSpamFactor);
		instabilities[index] = Math.max(0.0D, instabilities[index] - removed);
		setChanged();
	}

	private boolean shouldExplode() {
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			if (instabilities[face.getInstabilityIndex()] >= 100.0D) {
				return level.random.nextInt(4) == 2;
			}
		}
		return false;
	}

	private void explode() {
		if (level == null) return;
		enabled = false;
		queueEvent("reactorDeactivation");
		final double normalizedEnergy = energyStored / (double) getMaxEnergyStored();
		final double energyFactor = Math.pow(normalizedEnergy, 0.125D);
		final int radius = (int) Math.round(getTier().getExplosionRadius() * energyFactor);
		final double removalChance = getTier().getExplosionRemovalChance() * energyFactor;
		if (radius > 1) {
			final float resistanceThreshold = Blocks.OBSIDIAN.getExplosionResistance();
			for (int x = worldPosition.getX() - radius;
			     x <= worldPosition.getX() + radius; x++) {
				for (int y = worldPosition.getY() - radius;
				     y <= worldPosition.getY() + radius; y++) {
					for (int z = worldPosition.getZ() - radius;
					     z <= worldPosition.getZ() + radius; z++) {
						final BlockPos target = new BlockPos(x, y, z);
						if (target.equals(worldPosition) || level.random.nextDouble() >= removalChance) {
							continue;
						}
						final BlockState state = level.getBlockState(target);
						if (state.getBlock().getExplosionResistance() >= resistanceThreshold) {
							level.removeBlock(target, false);
						}
					}
				}
			}
		}

		final Vector3d center = getCenter();
		level.removeBlock(worldPosition, false);
		final float minimum = getTier().getExplosionStrengthMinimum();
		final int range = Math.max(1, (int) Math.ceil(
			getTier().getExplosionStrengthMaximum() - minimum));
		for (int index = 0; index < getTier().getExplosionCount(); index++) {
			level.explode(null,
				center.x + level.random.nextInt(3) - 1.5D,
				center.y + level.random.nextInt(3) - 0.5D,
				center.z + level.random.nextInt(3) - 1.5D,
				minimum + level.random.nextInt(range), true, Explosion.Mode.DESTROY);
		}
	}

	private void refreshBlockState() {
		if (level == null) return;
		final BlockState state = getBlockState();
		if (!(state.getBlock() instanceof EnanReactorCoreBlock)) return;
		final int energyLevel = MathHelper.clamp((int) Math.round(
			4.0D * energyStored / getMaxEnergyStored()), 0, 3);
		final int instabilityLevel = MathHelper.clamp((int) Math.round(
			getMaximumInstability() / 25.0D), 0, 3);
		if (state.getValue(EnanReactorCoreBlock.ENERGY) != energyLevel
		 || state.getValue(EnanReactorCoreBlock.STABILITY) != instabilityLevel) {
			level.setBlock(worldPosition,
				state.setValue(EnanReactorCoreBlock.ENERGY, energyLevel)
					.setValue(EnanReactorCoreBlock.STABILITY, instabilityLevel), 3);
		}
	}

	private double getMaximumInstability() {
		double maximum = 0.0D;
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			maximum = Math.max(maximum, instabilities[face.getInstabilityIndex()]);
		}
		return maximum;
	}

	public Vector3d getCenter() {
		return Vector3d.atCenterOf(worldPosition).add(0.0D, getTier().getCenterYOffset(), 0.0D);
	}

	public void onCoreBroken() {
		if (broken) return;
		broken = true;
		if (level != null && !level.isClientSide) {
			for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
				final EnanReactorLaserTileEntity laser = getLaser(face);
				if (laser != null) laser.clearReactor(this);
			}
		}
	}

	@Override public int getMaxEnergyStored() { return getTier().getMaxEnergyStored(); }
	@Override protected int getMaxReceive() { return 0; }
	@Override protected int getMaxExtract() { return getPotentialOutput(); }
	@Override protected boolean canReceiveFrom(@Nullable final Direction side) { return false; }

	@Override
	protected boolean canExtractTo(@Nullable final Direction side) {
		if (getPotentialOutput() <= 0) return false;
		if (getTier() == EnanReactorTier.BASIC) {
			return side == null || side == Direction.UP || side == Direction.DOWN;
		}
		return side == null || side != Direction.UP;
	}

	@Override
	public boolean consumeEnergy(final int amount, final boolean simulate) {
		if (!super.consumeEnergy(amount, simulate)) return false;
		if (!simulate && amount > 0) {
			releasedThisTick += amount;
			releasedThisCycle += amount;
		}
		return true;
	}

	public int getPotentialOutput() {
		return calculatePotentialOutput(outputMode, energyStored, lastGenerationRate,
			releasedThisTick, outputThreshold, hold);
	}

	public boolean isEnabled() { return enabled; }

	public Object[] enable(@Nullable final Boolean value) {
		if (value != null && enabled != value) {
			enabled = value;
			setChanged();
			queueEvent(enabled ? "reactorActivation" : "reactorDeactivation");
		}
		return new Object[]{ enabled };
	}

	public Object[] name(@Nullable final String value) {
		if (value != null) {
			name = value;
			setChanged();
		}
		return new Object[]{ name };
	}

	public UUID getSignatureUuid() { return signatureUuid; }
	public String getSignatureName() { return name; }

	public Object[] getLocalPosition() {
		return new Object[]{ worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() };
	}

	public Object[] getAssemblyStatus() {
		return new Object[]{ assemblyValid, assemblyStatus };
	}

	public Object[] getEnergyRequired() {
		return new Object[]{ false, "No energy consumption" };
	}

	public Object[] getEnergyStatus() {
		return new Object[]{ energyStored, getMaxEnergyStored(), "FE", 0,
			energyReleasedLastCycle / UPDATE_INTERVAL_TICKS };
	}

	public Object[] getInstabilities() {
		hold = false;
		final Object[] result = new Object[EnanReactorFace.getLasers(getTier()).size()];
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			result[face.getInstabilityIndex()] = instabilities[face.getInstabilityIndex()];
		}
		return result;
	}

	public Object[] instabilityTarget(@Nullable final Double value) {
		if (value != null && Double.isFinite(value)) {
			instabilityTarget = MathHelper.clamp(value, 0.0D, 100.0D);
			setChanged();
		}
		return new Object[]{ instabilityTarget };
	}

	public Object[] outputMode(@Nullable final String modeName, @Nullable final Integer threshold) {
		if (modeName != null && threshold != null) {
			final EnanReactorOutputMode requested = EnanReactorOutputMode.byName(modeName);
			if (requested != null) {
				outputMode = requested;
				outputThreshold = threshold;
				setChanged();
			}
		}
		return new Object[]{ outputMode.getName(), outputThreshold };
	}

	public Object[] stabilizerEnergy(@Nullable final Integer value) {
		if (value != null) {
			stabilizerEnergy = Math.max(0, value);
			setChanged();
		}
		return new Object[]{ stabilizerEnergy };
	}

	public Object[] state() {
		return new Object[]{ getStatus(), enabled, energyStored,
			outputMode.getName(), outputThreshold };
	}

	public String getStatus() {
		return String.format("Enantiomorphic reactor%s: %s, assembly %s, %,d / %,d FE, "
			+ "stability %.1f%%, output %s %,d%s",
			name.isEmpty() ? "" : " '" + name + "'",
			enabled ? "enabled" : "disabled", assemblyStatus, energyStored,
			getMaxEnergyStored(), 100.0D - getMaximumInstability(), outputMode.getName(),
			outputThreshold, hold ? ", simulation on hold" : "");
	}

	public void addEventListener(final ReactorEventListener listener) {
		eventListeners.add(listener);
	}

	public void removeEventListener(final ReactorEventListener listener) {
		eventListeners.remove(listener);
	}

	private void queueEvent(final String eventName, final Object... arguments) {
		for (final ReactorEventListener listener : new ArrayList<>(eventListeners)) {
			try {
				listener.queueEvent(eventName, arguments);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.warn("Unable to deliver {} from reactor at {}",
					eventName, worldPosition, exception);
			}
		}
	}

	public static double calculateStabilityOffset(final EnanReactorTier tier,
	                                              final double[] instabilityValues) {
		double stabilityOffset = 0.5D;
		for (final EnanReactorFace face : EnanReactorFace.getLasers(tier)) {
			stabilityOffset *= Math.max(0.01D,
				instabilityValues[face.getInstabilityIndex()] / 100.0D);
		}
		return stabilityOffset;
	}

	public static int calculateGeneration(final EnanReactorTier tier, final int stored,
	                                      final double[] instabilityValues) {
		final double stabilityOffset = calculateStabilityOffset(tier, instabilityValues);
		final int range = tier.getGenerationMaximum() - tier.getGenerationMinimum();
		return (int) Math.ceil(UPDATE_INTERVAL_TICKS * (0.5D + stabilityOffset)
			* (tier.getGenerationMinimum() + range
			* Math.pow(stored / (double) tier.getMaxEnergyStored(), 0.6D)));
	}

	public static int calculateDecay(final EnanReactorTier tier, final int stored,
	                                 final double[] instabilityValues) {
		final double stabilityOffset = calculateStabilityOffset(tier, instabilityValues);
		return (int) (UPDATE_INTERVAL_TICKS * (1.0D - stabilityOffset)
			* (tier.getGenerationMinimum() + stored * 0.01D));
	}

	public static double calculateInstabilityIncrease(final double randomValue, final int stored,
	                                                  final int maximum) {
		return UPDATE_INTERVAL_TICKS * Math.max(INSTABILITY_MIN,
			INSTABILITY_MAX * Math.pow(randomValue * stored / maximum, 0.1D));
	}

	public static double calculateNaturalStabilization(final double instability) {
		return UPDATE_INTERVAL_TICKS * Math.max(INSTABILITY_MIN, instability * 0.02D);
	}

	public static double calculateLaserEffect(final int energy, final double randomValue,
	                                         final double noSpamFactor) {
		final double normalized = Math.min(1.0D, Math.max(0.0D, energy / MAX_LASER_ENERGY));
		final double baseEffect = 0.5D + 0.5D * Math.cos(
			Math.PI * Math.log10(0.1D + 0.9D * normalized));
		final double randomVariation = 0.8D + 0.4D * randomValue;
		return MAX_LASER_EFFECT * baseEffect * randomVariation * noSpamFactor;
	}

	public static int calculatePotentialOutput(final EnanReactorOutputMode mode,
	                                           final int stored, final int generationRate,
	                                           final int releasedThisTick, final int threshold,
	                                           final boolean hold) {
		if (hold) return 0;
		final int capacity = Math.max(0, 2 * generationRate - releasedThisTick);
		switch (mode) {
		case UNLIMITED:
			return Math.min(Math.max(0, stored), capacity);
		case ABOVE:
			return Math.min(Math.max(0, generationRate - threshold), capacity);
		case AT_RATE:
			return Math.min(Math.max(0, stored),
				Math.min(Math.max(0, threshold - releasedThisTick), capacity));
		case OFF:
		default:
			return 0;
		}
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		if (tag.contains(TAG_ENABLED)) enabled = tag.getBoolean(TAG_ENABLED);
		final EnanReactorOutputMode loadedMode =
			EnanReactorOutputMode.byName(tag.getString(TAG_OUTPUT_MODE));
		outputMode = loadedMode == null ? EnanReactorOutputMode.OFF : loadedMode;
		if (tag.contains(TAG_OUTPUT_THRESHOLD)) outputThreshold = tag.getInt(TAG_OUTPUT_THRESHOLD);
		if (tag.contains(TAG_INSTABILITY_TARGET)) {
			instabilityTarget = tag.getDouble(TAG_INSTABILITY_TARGET);
		}
		if (tag.contains(TAG_STABILIZER_ENERGY)) stabilizerEnergy = tag.getInt(TAG_STABILIZER_ENERGY);
		if (tag.hasUUID(TAG_SIGNATURE)) signatureUuid = tag.getUUID(TAG_SIGNATURE);
		name = tag.getString(TAG_NAME);
		final CompoundNBT instabilityTag = tag.getCompound(TAG_INSTABILITY);
		for (final EnanReactorTier candidate : EnanReactorTier.values()) {
			for (final EnanReactorFace face : EnanReactorFace.getLasers(candidate)) {
				if (instabilityTag.contains(face.getName())) {
					instabilities[face.getInstabilityIndex()] =
						instabilityTag.getDouble(face.getName());
				}
			}
		}
		hold = true;
		assemblyDirty = true;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putBoolean(TAG_ENABLED, enabled);
		tag.putString(TAG_OUTPUT_MODE, outputMode.getName());
		tag.putInt(TAG_OUTPUT_THRESHOLD, outputThreshold);
		tag.putDouble(TAG_INSTABILITY_TARGET, instabilityTarget);
		tag.putInt(TAG_STABILIZER_ENERGY, stabilizerEnergy);
		tag.putUUID(TAG_SIGNATURE, signatureUuid);
		tag.putString(TAG_NAME, name);
		final CompoundNBT instabilityTag = new CompoundNBT();
		for (final EnanReactorFace face : EnanReactorFace.getLasers(getTier())) {
			instabilityTag.putDouble(face.getName(),
				instabilities[face.getInstabilityIndex()]);
		}
		tag.put(TAG_INSTABILITY, instabilityTag);
		return tag;
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		return save(new CompoundNBT());
	}

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(worldPosition, 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager networkManager,
	                         final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide) {
			synchronized (LIVE_REACTORS) { LIVE_REACTORS.add(this); }
		}
	}

	@Override
	public void setRemoved() {
		synchronized (LIVE_REACTORS) { LIVE_REACTORS.remove(this); }
		eventListeners.clear();
		super.setRemoved();
	}

	private static String format(final BlockPos position) {
		return String.format("(%d, %d, %d)", position.getX(), position.getY(), position.getZ());
	}
}
