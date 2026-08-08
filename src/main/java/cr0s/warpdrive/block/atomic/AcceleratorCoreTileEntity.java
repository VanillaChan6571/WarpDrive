package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.ParticleType;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.item.ElectromagneticCellItem;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loaded-trajectory accelerator simulation.
 *
 * It keeps the original thermal and per-magnet energy equations, injector cadence, bunch energy
 * bands, collider thresholds and particle-cell output contract. The old sub-block motion loop is
 * represented as bunch energy advancing through the scanned trajectory once per server tick; that
 * keeps the machine deterministic without reintroducing the 1.12 CoreMod-era position cache.
 */
public class AcceleratorCoreTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	private static final double AMBIENT_TEMPERATURE = 300.0D;
	private static final double AMBIENT_WARMING_RATE = 0.01D;
	private static final double TEMPERATURE_TOLERANCE = 0.05D;
	private static final double[] TARGET_TEMPERATURES = { 270.0D, 200.0D, 7.0D };
	private static final double[] ENERGY_MINIMUM = { 0.1D, 0.8D, 8.0D };
	private static final double[] ENERGY_MAXIMUM = { 1.0D, 10.0D, 100.0D };
	private static final double[] ENERGY_FACTOR_PER_MAGNET = { 0.024570D, 0.0060324D, 0.0005625D };
	private static final int MAX_PARTICLE_BUNCHES = 20;
	private static final int COOLDOWN_TICKS = 300;

	private AcceleratorAssembly.Result assembly = new AcceleratorAssembly.Result();
	private final List<ParticleBunch> bunches = new ArrayList<>();
	private final Map<Integer, ControlParameter> parameters = new LinkedHashMap<>();
	private double temperature = AMBIENT_TEMPERATURE;
	private boolean enabled;
	private boolean powered;
	private boolean running;
	private int scanTimer = 1;
	private int cooldown;
	private int injectionPeriod = 60;
	private int injectionTimer;
	private int injectorIndex;
	private int energyRequired;

	public AcceleratorCoreTileEntity() {
		super(Registration.ACCELERATOR_CORE_TILE.get());
	}

	@Override public int getMaxEnergyStored() { return 100_000_000; }
	@Override protected int getMaxReceive() { return 65_536; }
	@Override protected int getMaxExtract() { return 0; }

	@Override
	public void tick() {
		if (!(level instanceof ServerWorld)) return;
		final ServerWorld world = (ServerWorld) level;
		if (cooldown > 0) cooldown--;
		if (--scanTimer <= 0) {
			scanTimer = 100;
			rescan(world);
		}

		final boolean needsCooling = temperature > getTargetTemperature() + TEMPERATURE_TOLERANCE;
		energyRequired = calculateCurrentEnergyRequired(needsCooling);
		powered = energyRequired > 0 && consumeEnergy(energyRequired, true);
		final boolean shouldRun = enabled && assembly.valid && cooldown <= 0 && powered;
		if (shouldRun) {
			consumeEnergy(energyRequired, false);
			if (!running) setRunning(world, true, needsCooling);
			else updateComponents(world, true, needsCooling);
			if (needsCooling) {
				temperature = updateTemperature(temperature, getCoolingRate(), getTargetTemperature());
			} else {
				temperature = updateTemperature(temperature, AMBIENT_WARMING_RATE,
					getTargetTemperature());
				inject(world);
				simulateBunches(world);
			}
		} else {
			if (running) {
				setRunning(world, false, false);
				cooldown = COOLDOWN_TICKS;
			}
			temperature = updateTemperature(temperature, AMBIENT_WARMING_RATE,
				AMBIENT_TEMPERATURE);
		}
		setChanged();
	}

	private void rescan(final ServerWorld world) {
		updateComponents(world, false, false);
		assembly = AcceleratorAssembly.scan(world, worldPosition);
		updateComponents(world, running && assembly.valid,
			temperature > getTargetTemperature() + TEMPERATURE_TOLERANCE);
		if (!assembly.valid && running) setRunning(world, false, false);
	}

	private void setRunning(final ServerWorld world, final boolean active,
	                        final boolean cooling) {
		running = active;
		updateComponents(world, active, cooling);
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof AcceleratorCoreBlock
		 && blockState.getValue(AcceleratorCoreBlock.ACTIVE) != active) {
			world.setBlock(worldPosition, blockState.setValue(AcceleratorCoreBlock.ACTIVE, active), 3);
		}
	}

	private void updateComponents(final ServerWorld world, final boolean active,
	                              final boolean cooling) {
		for (final BlockPos position : assembly.chillers) {
			if (!world.hasChunkAt(position)) continue;
			final BlockState blockState = world.getBlockState(position);
			if (blockState.hasProperty(AcceleratorComponentBlock.ACTIVE)
			 && blockState.getValue(AcceleratorComponentBlock.ACTIVE) != active && cooling) {
				world.setBlock(position, blockState.setValue(AcceleratorComponentBlock.ACTIVE, true), 3);
			} else if (blockState.hasProperty(AcceleratorComponentBlock.ACTIVE)
			        && blockState.getValue(AcceleratorComponentBlock.ACTIVE) && (!active || !cooling)) {
				world.setBlock(position, blockState.setValue(AcceleratorComponentBlock.ACTIVE, false), 3);
			}
		}
		for (final BlockPos position : assembly.colliders) {
			if (!world.hasChunkAt(position)) continue;
			final BlockState blockState = world.getBlockState(position);
			if (blockState.hasProperty(AcceleratorComponentBlock.ACTIVE)
			 && blockState.getValue(AcceleratorComponentBlock.ACTIVE) != active) {
				world.setBlock(position, blockState.setValue(AcceleratorComponentBlock.ACTIVE, active), 3);
			}
		}
		for (final BlockPos position : assembly.controlPoints) {
			final TileEntity tileEntity = world.getBlockEntity(position);
			if (tileEntity instanceof AcceleratorControlPointTileEntity) {
				((AcceleratorControlPointTileEntity) tileEntity).setCoreActive(active);
			}
		}
	}

	private void inject(final ServerWorld world) {
		if (--injectionTimer > 0 || bunches.size() >= MAX_PARTICLE_BUNCHES
		 || assembly.injectors.isEmpty()) return;
		injectionTimer = injectionPeriod;
		final List<BlockPos> injectors = new ArrayList<>(assembly.injectors);
		if (injectorIndex >= injectors.size()) injectorIndex = 0;
		final BlockPos injector = injectors.get(injectorIndex++);
		final TileEntity tileEntity = world.getBlockEntity(injector);
		if (!(tileEntity instanceof ParticlesInjectorTileEntity)) return;
		final ParticlesInjectorTileEntity control = (ParticlesInjectorTileEntity) tileEntity;
		if (!control.isEnabled() || !isChannelEnabled(control.getControlChannel())) return;
		if (!consumeInjectionItem(world, injector)) return;
		bunches.add(new ParticleBunch(ENERGY_MINIMUM[Math.max(0, assembly.highestTier - 1)]));
		world.sendParticles(ParticleTypes.END_ROD, injector.getX() + 0.5D,
			injector.getY() + 0.5D, injector.getZ() + 0.5D,
			12, 0.35D, 0.35D, 0.35D, 0.05D);
	}

	private static boolean consumeInjectionItem(final ServerWorld world, final BlockPos injector) {
		for (final Direction direction : Direction.values()) {
			final TileEntity tileEntity = world.getBlockEntity(injector.relative(direction));
			if (tileEntity == null) continue;
			final LazyOptional<IItemHandler> capability = tileEntity.getCapability(
				CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
			final IItemHandler handler = capability.orElse(null);
			if (handler == null) continue;
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				final ItemStack simulated = handler.extractItem(slot, 1, true);
				if (!simulated.isEmpty() && simulated.getItem() instanceof BlockItem) {
					handler.extractItem(slot, 1, false);
					return true;
				}
			}
		}
		return false;
	}

	private void simulateBunches(final ServerWorld world) {
		if (bunches.isEmpty()) return;
		double factor = 0.0D;
		for (int tier = 0; tier < assembly.magnetCounts.length; tier++) {
			factor += assembly.magnetCounts[tier] * ENERGY_FACTOR_PER_MAGNET[tier];
		}
		factor /= Math.max(1, assembly.shells.size());
		for (final ParticleBunch bunch : bunches) {
			bunch.energy = Math.min(200.0D, bunch.energy * (1.0D + factor));
			bunch.age++;
		}
		if (assembly.colliders.isEmpty()) return;
		final BlockPos collider = assembly.colliders.iterator().next();
		final double threshold = getColliderThreshold(world, collider)
			* ENERGY_MAXIMUM[Math.max(0, assembly.highestTier - 1)];
		ParticleBunch first = null;
		ParticleBunch second = null;
		for (final ParticleBunch bunch : bunches) {
			if (bunch.energy >= threshold) { first = bunch; break; }
			if (first == null) first = bunch;
			else if (first.energy + bunch.energy >= threshold) { second = bunch; break; }
		}
		if (first == null || first.energy < threshold && second == null) return;
		final double totalEnergy = first.energy + (second == null ? 0.0D : second.energy);
		bunches.remove(first);
		if (second != null) bunches.remove(second);
		collide(world, collider, totalEnergy);
	}

	private double getColliderThreshold(final ServerWorld world, final BlockPos collider) {
		for (final BlockPos controlPos : assembly.controlPoints) {
			if (controlPos.distSqr(collider) > 16.0D) continue;
			final TileEntity tileEntity = world.getBlockEntity(controlPos);
			if (!(tileEntity instanceof AcceleratorControlPointTileEntity)
			 || tileEntity instanceof ParticlesInjectorTileEntity) continue;
			final int channel = ((AcceleratorControlPointTileEntity) tileEntity).getControlChannel();
			final ControlParameter parameter = parameters.get(channel);
			return parameter == null ? 0.95D
				: parameter.enabled ? parameter.threshold : Double.POSITIVE_INFINITY;
		}
		return 0.95D;
	}

	private void collide(final ServerWorld world, final BlockPos collider, final double energy) {
		final ParticleType particle = particleForEnergy(energy);
		final int tier = Math.max(1, assembly.highestTier);
		final double bandStart = particleBandStart(particle);
		final double bandWidth = particleBandWidth(particle);
		final double extra = Math.max(0.0D, Math.min(1.0D, (energy - bandStart) / bandWidth));
		final int quantity = Math.max(1, (int) Math.round(
			(10.0D + 5.0D * world.random.nextGaussian()) * (1.0D - 0.2D * tier)
				* (0.5D + 2.0D * extra)));
		final int left = storeParticles(world, collider, particle, quantity);
		world.playSound(null, collider, Registration.SOUND_COLLISION.get(), SoundCategory.BLOCKS,
			0.5F + (float) extra, 0.85F + 0.5F * (float) extra);
		if (left > 0) {
			world.explode(null, collider.getX() + 0.5D, collider.getY() + 0.5D,
				collider.getZ() + 0.5D, 3.0F + tier, true, Explosion.Mode.DESTROY);
		} else {
			world.sendParticles(ParticleTypes.EXPLOSION, collider.getX() + 0.5D,
				collider.getY() + 0.5D, collider.getZ() + 0.5D,
				5, 1.0D, 1.0D, 1.0D, 0.05D);
		}
	}

	static ParticleType particleForEnergy(final double energy) {
		if (energy >= 150.0D) return ParticleType.STRANGE_MATTER;
		if (energy >= 50.0D) return ParticleType.ANTIMATTER;
		if (energy >= 5.0D) return ParticleType.PROTON;
		return ParticleType.ION;
	}

	private static double particleBandStart(final ParticleType particle) {
		switch (particle) {
		case STRANGE_MATTER: return 150.0D;
		case ANTIMATTER: return 50.0D;
		case PROTON: return 5.0D;
		case ION:
		default: return 0.4D;
		}
	}

	private static double particleBandWidth(final ParticleType particle) {
		switch (particle) {
		case STRANGE_MATTER: return 50.0D;
		case ANTIMATTER: return 100.0D;
		case PROTON: return 45.0D;
		case ION:
		default: return 4.6D;
		}
	}

	private int storeParticles(final ServerWorld world, final BlockPos collider,
	                           final ParticleType particle, final int quantity) {
		int left = quantity;
		final Collection<BlockPos> anchors = new ArrayList<>(assembly.controlPoints);
		anchors.add(collider);
		for (final BlockPos anchor : anchors) {
			for (final Direction direction : Direction.values()) {
				final TileEntity tileEntity = world.getBlockEntity(anchor.relative(direction));
				if (tileEntity == null) continue;
				final IItemHandler handler = tileEntity.getCapability(
					CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()).orElse(null);
				if (!(handler instanceof IItemHandlerModifiable)) continue;
				left = fillHandler((IItemHandlerModifiable) handler, particle, left);
				if (left <= 0) return 0;
			}
		}
		return left;
	}

	private static int fillHandler(final IItemHandlerModifiable handler,
	                              final ParticleType particle, final int requested) {
		int left = requested;
		// Existing compatible cells first.
		for (int slot = 0; slot < handler.getSlots() && left > 0; slot++) {
			final ItemStack current = handler.getStackInSlot(slot);
			if (!(current.getItem() instanceof ElectromagneticCellItem)) continue;
			final ElectromagneticCellItem cell = (ElectromagneticCellItem) current.getItem();
			if (cell.getParticleType() != particle) continue;
			final ItemStack updated = current.copy();
			left -= cell.fill(updated, particle, left, true);
			handler.setStackInSlot(slot, updated);
		}
		// Convert flattened empty-cell ids into the matching particle id.
		for (int slot = 0; slot < handler.getSlots() && left > 0; slot++) {
			final ItemStack current = handler.getStackInSlot(slot);
			if (!(current.getItem() instanceof ElectromagneticCellItem)) continue;
			final ElectromagneticCellItem cell = (ElectromagneticCellItem) current.getItem();
			if (cell.getParticleType() != null) continue;
			final int transfer = Math.min(left, cell.getCapacity());
			handler.setStackInSlot(slot,
				ElectromagneticCellItem.create(cell.getTier(), particle, transfer));
			left -= transfer;
		}
		return left;
	}

	private boolean isChannelEnabled(final int channel) {
		final ControlParameter parameter = parameters.get(channel);
		return parameter == null || parameter.enabled;
	}

	private int calculateCurrentEnergyRequired(final boolean needsCooling) {
		if (!assembly.valid) return 0;
		double cost = needsCooling
			? assembly.chillerCounts[0] * 10.0D + assembly.chillerCounts[1] * 20.0D
				+ assembly.chillerCounts[2] * 40.0D
			: getSustainCost();
		cost += (assembly.magnetCounts[0] + 2.0D * assembly.magnetCounts[1]
			+ 3.0D * assembly.magnetCounts[2]) * (0.1D + bunches.size());
		return Math.max(1, (int) Math.round(cost));
	}

	private double getSustainCost() {
		switch (getTemperatureTier()) {
		case 3: return assembly.chillerCounts[0] * 0.25D + assembly.chillerCounts[1]
			+ assembly.chillerCounts[2] * 4.0D;
		case 2: return assembly.chillerCounts[0] * 0.5D + assembly.chillerCounts[1] * 2.0D
			+ assembly.chillerCounts[2] * 3.0D;
		case 1:
		default: return assembly.chillerCounts[0] + assembly.chillerCounts[1] * 2.0D
			+ assembly.chillerCounts[2] * 2.0D;
		}
	}

	private double getCoolingRate() {
		final int magnets = Math.max(1, assembly.getMagnetCount());
		final double factor = 10.0D / magnets;
		switch (getTemperatureTier()) {
		case 3: return assembly.chillerCounts[2] * factor;
		case 2: return (assembly.chillerCounts[1] + assembly.chillerCounts[2] * 0.75D) * factor;
		case 1:
		default: return (assembly.chillerCounts[0] + assembly.chillerCounts[1] * 0.75D
			+ assembly.chillerCounts[2] * 0.5D) * factor;
		}
	}

	private int getTemperatureTier() {
		if (temperature <= TARGET_TEMPERATURES[1]) return 3;
		if (temperature <= TARGET_TEMPERATURES[0]) return 2;
		return 1;
	}

	private double getTargetTemperature() {
		return TARGET_TEMPERATURES[Math.max(0, assembly.highestTier - 1)];
	}

	private static double updateTemperature(final double actual, final double rate,
	                                        final double target) {
		final double delta = target - actual;
		if (Math.abs(delta) < TEMPERATURE_TOLERANCE) return target;
		return actual + rate * Math.signum(delta) * Math.sqrt(Math.abs(delta) / AMBIENT_TEMPERATURE);
	}

	public boolean isEnabled() { return enabled; }
	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null && enabled != requested) {
			enabled = requested;
			setChanged();
		}
		return new Object[]{ enabled };
	}

	public String getStatus() {
		return String.format("%s: %s, %.2f K / %.2f K, %d bunches, %d FE",
			running ? "Running" : enabled ? "Enabled" : "Disabled", assembly.status,
			temperature, getTargetTemperature(), bunches.size(), getEnergyStored());
	}

	public Object[] state() {
		return new Object[]{ getStatus(), enabled, powered, getEnergyStored(),
			temperature, getTargetTemperature() };
	}

	public Object[] getEnergyRequired() {
		final int magnets = assembly.magnetCounts[0] + 2 * assembly.magnetCounts[1]
			+ 3 * assembly.magnetCounts[2];
		final int cooling = Math.max(0, (int) Math.round(assembly.chillerCounts[0] * 10.0D
			+ assembly.chillerCounts[1] * 20.0D + assembly.chillerCounts[2] * 40.0D
			+ magnets * 0.1D));
		final int sustain = Math.max(0, (int) Math.round(getSustainCost() + magnets * 0.1D));
		final int single = Math.max(0, (int) Math.round(getSustainCost() + magnets * 1.1D));
		final int maximum = Math.max(0, (int) Math.round(
			getSustainCost() + magnets * (0.1D + MAX_PARTICLE_BUNCHES)));
		return new Object[]{ cooling, sustain, single, maximum };
	}

	public Object[] getControlPoints() {
		if (!(level instanceof ServerWorld)) return new Object[0];
		return assembly.describeControlPoints((ServerWorld) level).toArray();
	}

	public Object[] getControlPointsCount() {
		return new Object[]{ assembly.valid ? assembly.controlPoints.size() : -1 };
	}

	public Object[] getControlPoint(final int index) {
		final Object[] points = getControlPoints();
		if (index < 0 || index >= points.length) return new Object[]{ false, "Index out of range" };
		final Object[] point = (Object[]) points[index];
		return new Object[]{ true, point[0], point[1], point[2], point[3], point[4], point[5], point[6] };
	}

	public Object[] getParameters() {
		final Object[] result = new Object[parameters.size()];
		int index = 0;
		for (final ControlParameter parameter : parameters.values()) result[index++] = parameter.toArray();
		return result;
	}

	public Object[] getParametersControlChannels() { return parameters.keySet().toArray(); }

	public Object[] parameter(final int channel, @Nullable final Boolean enabledRequested,
	                          @Nullable final Double thresholdRequested,
	                          @Nullable final String descriptionRequested) {
		final int clampedChannel = Math.max(0, Math.min(65_535, channel));
		final ControlParameter parameter = parameters.computeIfAbsent(clampedChannel,
			ControlParameter::new);
		if (enabledRequested != null) parameter.enabled = enabledRequested;
		if (thresholdRequested != null) parameter.threshold = Math.max(0.0D,
			Math.min(2.0D, thresholdRequested));
		if (descriptionRequested != null) parameter.description = descriptionRequested;
		setChanged();
		return new Object[]{ true, parameter.channel, parameter.enabled,
			parameter.threshold, parameter.description };
	}

	public Object[] injectionPeriod(@Nullable final Double seconds) {
		if (seconds != null) {
			injectionPeriod = Math.max(1, Math.min(18_000, (int) Math.round(seconds * 20.0D)));
			setChanged();
		}
		return new Object[]{ injectionPeriod / 20.0D };
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerWorld) updateComponents((ServerWorld) level, false, false);
		running = false;
		super.setRemoved();
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		enabled = tag.getBoolean("isEnabled");
		temperature = tag.contains("temperature") ? tag.getDouble("temperature") : AMBIENT_TEMPERATURE;
		injectionPeriod = Math.max(1, tag.getInt("injectionPeriod"));
		bunches.clear();
		final ListNBT bunchList = tag.getList("particleBunches", Constants.NBT.TAG_DOUBLE);
		for (int index = 0; index < bunchList.size(); index++) {
			bunches.add(new ParticleBunch(bunchList.getDouble(index)));
		}
		parameters.clear();
		final ListNBT list = tag.getList("parameters", Constants.NBT.TAG_COMPOUND);
		for (int index = 0; index < list.size(); index++) {
			final ControlParameter parameter = new ControlParameter(list.getCompound(index));
			parameters.put(parameter.channel, parameter);
		}
		scanTimer = 1;
		running = false;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putBoolean("isEnabled", enabled);
		tag.putDouble("temperature", temperature);
		tag.putInt("injectionPeriod", injectionPeriod);
		final ListNBT bunchList = new ListNBT();
		for (final ParticleBunch bunch : bunches) bunchList.add(DoubleNBT.valueOf(bunch.energy));
		tag.put("particleBunches", bunchList);
		final ListNBT list = new ListNBT();
		for (final ControlParameter parameter : parameters.values()) list.add(parameter.save());
		tag.put("parameters", list);
		return tag;
	}

	private static final class ParticleBunch {
		private double energy;
		private int age;
		private ParticleBunch(final double energy) { this.energy = energy; }
	}

	private static final class ControlParameter {
		private final int channel;
		private boolean enabled = true;
		private double threshold = 0.95D;
		private String description = "-";

		private ControlParameter(final int channel) { this.channel = channel; }
		private ControlParameter(final CompoundNBT tag) {
			channel = tag.getInt("controlChannel");
			enabled = !tag.contains("isEnabled") || tag.getBoolean("isEnabled");
			threshold = tag.contains("threshold") ? tag.getDouble("threshold") : 0.95D;
			description = tag.getString("description");
		}
		private CompoundNBT save() {
			final CompoundNBT tag = new CompoundNBT();
			tag.putInt("controlChannel", channel);
			tag.putBoolean("isEnabled", enabled);
			tag.putDouble("threshold", threshold);
			tag.putString("description", description);
			return tag;
		}
		private Object[] toArray() { return new Object[]{ channel, enabled, threshold, description }; }
	}
}
