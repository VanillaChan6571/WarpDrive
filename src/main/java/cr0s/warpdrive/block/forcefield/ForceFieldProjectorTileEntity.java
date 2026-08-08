package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.SoundType;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Energy-backed projector with asynchronous legacy-shape calculation and incremental placement.
 * Relay upgrades are aggregated through {@link ForceFieldRegistry}; all world mutation stays on
 * the server thread.
 */
public class ForceFieldProjectorTileEntity extends AbstractForceFieldTileEntity {

	private static final String TAG_DOUBLE_SIDED = "isDoubleSided";
	private static final String TAG_SHAPE = "shape";
	private static final String TAG_ACTIVE = "isOn";
	private static final String TAG_UPGRADES = "upgrades";
	private static final int PROJECTION_UPDATE_TICKS = 8;
	private static final int SOUND_UPDATE_TICKS = 60;
	private static final int COOLDOWN_TICKS = 300;
	private static final int MAX_RECEIVE = 4096;
	private static final float MAX_SCAN_SPEED = 10_000.0F;
	private static final float MAX_PLACE_SPEED = 4_000.0F;
	private static final double LASER_ENERGY_PER_PROJECTOR_DAMAGE =
		10.0D * (3_400_000 - 750_000) / 150_000.0D;

	private ForceFieldShape shape = ForceFieldShape.NONE;
	private boolean doubleSided;
	private boolean active;
	private float minX = -1.0F;
	private float minY = -1.0F;
	private float minZ = -1.0F;
	private float maxX = 1.0F;
	private float maxY = 1.0F;
	private float maxZ = 1.0F;
	private float translationX;
	private float translationY;
	private float translationZ;
	private float rotationYaw;
	private float rotationPitch;
	private float rotationRoll;
	private final EnumMap<ForceFieldUpgrade, Integer> upgrades =
		new EnumMap<>(ForceFieldUpgrade.class);

	private int projectionTicks;
	private int soundTicks;
	private int cooldownTicks;
	private double consumptionRemainder;
	private ProjectionSettings currentSettings;
	private CompletableFuture<ProjectionResult> calculation;
	private ProjectionSettings calculationSettings;
	private Set<BlockPos> forceField = Collections.emptySet();
	private Set<BlockPos> interior = Collections.emptySet();
	private final Set<BlockPos> placedFields = new HashSet<>();
	private Iterator<BlockPos> fieldIterator;
	private float scanCarry;
	private float placeCarry;
	private final Set<UUID> interactedEntities = new HashSet<>();

	public ForceFieldProjectorTileEntity() {
		super(Registration.FORCE_FIELD_PROJECTOR_TILE.get());
	}

	public ForceFieldTier getTier() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof ForceFieldProjectorBlock
			? ((ForceFieldProjectorBlock) blockState.getBlock()).getTier() : ForceFieldTier.BASIC;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide) return;
		if (cooldownTicks > 0) cooldownTicks--;
		settleEntityInteractions();
		finishCalculationIfReady();
		if (--projectionTicks > 0) return;
		projectionTicks = PROJECTION_UPDATE_TICKS;

		if (!isEnabled() || !isConnected() || shape == ForceFieldShape.NONE) {
			deactivate();
			return;
		}
		final ProjectionSettings settings = buildSettings();
		if (!settings.equals(currentSettings)) {
			deactivate();
			currentSettings = settings;
			startCalculation(settings);
			return;
		}
		if (calculation != null || forceField.isEmpty()) return;

		if (!consumeFractional(getBatchEnergyRequired(settings), true)) {
			deactivate();
			return;
		}
		if (!active) {
			if (cooldownTicks > 0) return;
			if (!consumeFractional(settings.startupEnergyCost, false)) return;
			setActive(true);
		}
		project(settings);
		if (active && --soundTicks <= 0) {
			soundTicks = SOUND_UPDATE_TICKS;
			if (getUpgradeCount(ForceFieldUpgrade.SILENCER) == 0) {
				level.playSound(null, getBlockPos(), Registration.SOUND_PROJECTING.get(),
					SoundCategory.BLOCKS, 1.0F, 0.85F + 0.15F * level.random.nextFloat());
			}
		}
	}

	private void startCalculation(final ProjectionSettings settings) {
		calculationSettings = settings;
		calculation = CompletableFuture.supplyAsync(() -> calculate(settings));
	}

	private void finishCalculationIfReady() {
		if (calculation == null || !calculation.isDone()) return;
		final ProjectionResult result;
		try {
			result = calculation.join();
		} catch (final RuntimeException exception) {
			calculation = null;
			calculationSettings = null;
			return;
		}
		calculation = null;
		if (calculationSettings != null && calculationSettings.equals(currentSettings)) {
			forceField = result.forceField;
			interior = result.interior;
			fieldIterator = null;
		}
		calculationSettings = null;
	}

	private ProjectionResult calculate(final ProjectionSettings settings) {
		final boolean needInterior = settings.inverted || settings.fusion;
		final ForceFieldShape.Geometry geometry = settings.shape.calculate(
			settings.minX, settings.minY, settings.minZ,
			settings.maxX, settings.maxY, settings.maxZ,
			settings.thickness, needInterior);
		final Set<BlockPos> worldPerimeter = transform(geometry.perimeter, settings);
		final Set<BlockPos> worldInterior = transform(geometry.interior, settings);
		return new ProjectionResult(settings.inverted ? new HashSet<>(worldInterior) : worldPerimeter,
			worldInterior);
	}

	private Set<BlockPos> transform(final Set<BlockPos> localPositions,
	                                final ProjectionSettings settings) {
		final Set<BlockPos> transformed = new HashSet<>(Math.max(16, localPositions.size()));
		for (final BlockPos local : localPositions) {
			if (!settings.doubleSided && local.getY() < 0) continue;
			final BlockPos rotated = rotate(local, settings.yaw, settings.pitch, settings.roll);
			final BlockPos worldPos = settings.origin.offset(rotated)
				.offset(settings.translationX, settings.translationY, settings.translationZ);
			if (worldPos.getY() >= 0 && worldPos.getY() < settings.worldHeight) {
				transformed.add(worldPos.immutable());
			}
		}
		return transformed;
	}

	private static BlockPos rotate(final BlockPos position, final float yaw,
	                               final float pitch, final float roll) {
		final double yawRadians = Math.toRadians(yaw);
		final double pitchRadians = Math.toRadians(pitch);
		final double rollRadians = Math.toRadians(roll);
		final double yawCos = Math.cos(yawRadians);
		final double yawSin = Math.sin(yawRadians);
		final double pitchCos = Math.cos(pitchRadians);
		final double pitchSin = Math.sin(pitchRadians);
		final double rollCos = Math.cos(rollRadians);
		final double rollSin = Math.sin(rollRadians);
		final double oldX = position.getX();
		final double oldY = position.getY();
		final double oldZ = position.getZ();
		final int x = (int) Math.round(oldX * yawCos * pitchCos
			+ oldZ * (yawCos * pitchSin * rollSin - yawSin * rollCos)
			+ oldY * (yawCos * pitchSin * rollCos + yawSin * rollSin));
		final int z = (int) Math.round(oldX * yawSin * pitchCos
			+ oldZ * (yawSin * pitchSin * rollSin + yawCos * rollCos)
			+ oldY * (yawSin * pitchSin * rollCos - yawCos * rollSin));
		final int y = (int) Math.round(-oldX * pitchSin + oldZ * pitchCos * rollSin
			+ oldY * pitchCos * rollCos);
		return new BlockPos(x, y, z);
	}

	private ProjectionSettings buildSettings() {
		final EnumMap<ForceFieldUpgrade, Float> values = new EnumMap<>(ForceFieldUpgrade.class);
		final List<BlockPos> itemPorts = new ArrayList<>();
		final List<BlockPos> pumpingPorts = new ArrayList<>();
		BlockState camouflage = null;
		BlockPos camouflageRelay = null;
		for (final Map.Entry<ForceFieldUpgrade, Integer> entry : upgrades.entrySet()) {
			final float value = entry.getKey().getBaseValue() * entry.getValue()
				* (1.0F + 0.50F * getTier().ordinal());
			values.merge(entry.getKey(), value, Float::sum);
		}
		if (level instanceof ServerWorld) {
			for (final AbstractForceFieldTileEntity tileEntity : ForceFieldRegistry.getNetwork(
				(ServerWorld) level, getBeamFrequency(), getBlockPos())) {
				if (tileEntity instanceof ForceFieldRelayTileEntity) {
					final ForceFieldRelayTileEntity relay = (ForceFieldRelayTileEntity) tileEntity;
					if (relay.getUpgrade() != ForceFieldUpgrade.NONE && relay.isEnabled()) {
						values.merge(relay.getUpgrade(), relay.getUpgradeValue(), Float::sum);
						if (relay.getUpgrade() == ForceFieldUpgrade.ITEM_PORT) {
							itemPorts.add(relay.getBlockPos().immutable());
						} else if (relay.getUpgrade() == ForceFieldUpgrade.PUMPING) {
							pumpingPorts.add(relay.getBlockPos().immutable());
						} else if (relay.getUpgrade() == ForceFieldUpgrade.CAMOUFLAGE) {
							final BlockState candidate = level.getBlockState(relay.getBlockPos().above());
							if (isValidCamouflage(candidate) && (camouflageRelay == null
							 || relay.getBlockPos().compareTo(camouflageRelay) < 0)) {
								camouflageRelay = relay.getBlockPos();
								camouflage = candidate;
							}
						}
					}
				}
			}
		}

		float scanSpeed = 100.0F * (doubleSided ? 2.1F : 1.0F);
		float placeSpeed = 20.0F * (doubleSided ? 2.1F : 1.0F);
		double startupCost = 60.0D + 20.0D * getTier().ordinal();
		double scanCost = 0.4D + 0.4D * getTier().ordinal();
		double placeCost = 3.0D + 3.0D * getTier().ordinal();
		double entityCost = 2.0D;
		if (doubleSided) {
			startupCost += 20.0D * getTier().ordinal();
			scanCost *= 0.45D;
			placeCost *= 0.45D;
			entityCost *= 0.45D;
		}
		for (final Map.Entry<ForceFieldUpgrade, Float> entry : values.entrySet()) {
			final ForceFieldUpgrade upgrade = entry.getKey();
			final float value = upgrade.scale(entry.getValue());
			entry.setValue(value);
			final float scanFactor = upgrade.getScanFactor(value);
			final float placeFactor = upgrade.getPlaceFactor(value);
			if (scanFactor > 0.0F) scanSpeed *= scanFactor;
			if (placeFactor > 0.0F) placeSpeed *= placeFactor;
			startupCost += upgrade.getStartupCost(value);
			scanCost += upgrade.getScanCost(value);
			placeCost += upgrade.getPlaceCost(value);
			entityCost += upgrade.getEntityCost(value);
		}

		double range = values.getOrDefault(ForceFieldUpgrade.RANGE, 0.0F);
		final boolean hasRange = range > 0.0D;
		if (!hasRange) range = 8.0D;
		final boolean inverted = values.getOrDefault(ForceFieldUpgrade.INVERSION, 0.0F) > 0.0F;
		final boolean fusion = values.getOrDefault(ForceFieldUpgrade.FUSION, 0.0F) > 0.0F;
		itemPorts.sort(BlockPos::compareTo);
		pumpingPorts.sort(BlockPos::compareTo);
		if (inverted || fusion) range = Math.min(64.0D, range);
		final int rangeMinX = (int) Math.round((hasRange ? minX : -1.0F) * range);
		final int rangeMinY = (int) Math.round((hasRange ? minY : -1.0F) * range);
		final int rangeMinZ = (int) Math.round((hasRange ? minZ : -1.0F) * range);
		final int rangeMaxX = (int) Math.round((hasRange ? maxX : 1.0F) * range);
		final int rangeMaxY = (int) Math.round((hasRange ? maxY : 1.0F) * range);
		final int rangeMaxZ = (int) Math.round((hasRange ? maxZ : 1.0F) * range);
		final boolean hasTranslation = getUpgradeCount(ForceFieldUpgrade.TRANSLATION) > 0;
		final boolean hasRotation = getUpgradeCount(ForceFieldUpgrade.ROTATION) > 0;
		final float[] facingRotation = getFacingRotation();

		return new ProjectionSettings(getBlockPos(), shape, doubleSided,
			rangeMinX, rangeMinY, rangeMinZ, rangeMaxX, rangeMaxY, rangeMaxZ,
			(int) Math.round((hasTranslation ? translationX : 0.0F) * range),
			(int) Math.round((hasTranslation ? translationY : 0.0F) * range),
			(int) Math.round((hasTranslation ? translationZ : 0.0F) * range),
			facingRotation[0] + (hasRotation ? rotationYaw : 0.0F),
			facingRotation[1] + (hasRotation ? rotationPitch : 0.0F),
			                         hasRotation ? rotationRoll : 0.0F,
			1.0F + values.getOrDefault(ForceFieldUpgrade.THICKNESS, 0.0F),
			inverted, fusion, Math.min(MAX_SCAN_SPEED, scanSpeed),
			Math.min(MAX_PLACE_SPEED, placeSpeed), startupCost, scanCost, placeCost,
			entityCost, values, camouflage, itemPorts, pumpingPorts, level.getMaxBuildHeight());
	}

	private static boolean isValidCamouflage(@Nullable final BlockState blockState) {
		return blockState != null && !blockState.isAir()
		    && !blockState.is(WarpDriveTags.NO_CAMOUFLAGE)
		    && !(blockState.getBlock() instanceof ForceFieldBlock)
		    && blockState.getRenderShape() == BlockRenderType.MODEL
		    && blockState.getFluidState().isEmpty()
		    && !blockState.hasTileEntity();
	}

	private float[] getFacingRotation() {
		final BlockState blockState = getBlockState();
		final Direction facing = blockState.getBlock() instanceof ForceFieldProjectorBlock
			? blockState.getValue(ForceFieldProjectorBlock.FACING) : Direction.UP;
		switch (facing) {
		case DOWN:  return new float[]{ 0.0F, 180.0F };
		case NORTH: return new float[]{ 90.0F, -90.0F };
		case SOUTH: return new float[]{ 270.0F, -90.0F };
		case WEST:  return new float[]{ 0.0F, -90.0F };
		case EAST:  return new float[]{ 180.0F, -90.0F };
		case UP:
		default:    return new float[]{ 0.0F, 0.0F };
		}
	}

	private void project(final ProjectionSettings settings) {
		final float scanBudgetFloat = Math.min(forceField.size(),
			settings.scanSpeed * PROJECTION_UPDATE_TICKS / 20.0F + scanCarry);
		final int scanBudget = (int) Math.floor(scanBudgetFloat);
		scanCarry = scanBudgetFloat - scanBudget;
		final float placeBudgetFloat = Math.min(forceField.size(),
			settings.placeSpeed * PROJECTION_UPDATE_TICKS / 20.0F + placeCarry);
		final int placeBudget = (int) Math.floor(placeBudgetFloat);
		placeCarry = placeBudgetFloat - placeBudget;
		int scanned = 0;
		int placed = 0;
		while (scanned < scanBudget && placed < placeBudget) {
			if (!consumeFractional(Math.max(settings.scanEnergyCost, settings.placeEnergyCost), true)) {
				deactivate();
				return;
			}
			if (fieldIterator == null || !fieldIterator.hasNext()) fieldIterator = forceField.iterator();
			if (!fieldIterator.hasNext()) return;
			final BlockPos target = fieldIterator.next();
			scanned++;
			if (!level.hasChunkAt(target) || target.equals(getBlockPos())) continue;
			if (settings.fusion && isInsideAnotherProjector(target)) {
				consumeFractional(settings.scanEnergyCost, false);
				removeOwnedField(target);
				continue;
			}

			final BlockState targetState = level.getBlockState(target);
			if (targetState.getBlock() instanceof ForceFieldBlock) {
				final TileEntity tileEntity = level.getBlockEntity(target);
				final boolean owned = tileEntity instanceof ForceFieldTileEntity
					&& ((ForceFieldTileEntity) tileEntity).isOwnedBy(this);
				if (owned && ((ForceFieldBlock) targetState.getBlock()).getTier() == getTier()
				 && Objects.equals(((ForceFieldTileEntity) tileEntity).getCamouflage(),
					settings.camouflage)) {
					placedFields.add(target.immutable());
				} else if (owned) {
					level.removeBlock(target, false);
					placedFields.remove(target);
				}
				consumeFractional(settings.scanEnergyCost, false);
				continue;
			}

			final FluidState fluidState = targetState.getFluidState();
			final float pumping = settings.upgrades.getOrDefault(ForceFieldUpgrade.PUMPING, 0.0F);
			if (!fluidState.isEmpty()) {
				final int viscosity = fluidState.getType().getAttributes().getViscosity(level, target);
				if (pumping <= 0.0F || viscosity > pumping
				 || isFluidMutationCancelled(target, targetState, settings)) {
					consumeFractional(settings.scanEnergyCost, false);
					continue;
				}
				if (!consumeFractional(settings.placeEnergyCost, false)) {
					deactivate();
					return;
				}
				pumpFluid(target, fluidState, settings);
				placed++;
				continue;
			}

			if (!targetState.getMaterial().isReplaceable()) {
				final float breaking = settings.upgrades.getOrDefault(ForceFieldUpgrade.BREAKING, 0.0F);
				final float hardness = targetState.getDestroySpeed(level, target);
				if (breaking <= 0.0F || hardness < 0.0F || hardness > breaking
				 || isBreakCancelled(target, targetState)) {
					consumeFractional(settings.scanEnergyCost, false);
					removeOwnedField(target);
					continue;
				}
				if (!consumeFractional(settings.placeEnergyCost, false)) {
					deactivate();
					return;
				}
				breakBlock(target, targetState, settings);
				placed++;
				continue;
			}

			final float acceleration = settings.upgrades.getOrDefault(ForceFieldUpgrade.ATTRACTION, 0.0F)
				- settings.upgrades.getOrDefault(ForceFieldUpgrade.REPULSION, 0.0F);
			final boolean stabilize = settings.upgrades.getOrDefault(
				ForceFieldUpgrade.STABILIZATION, 0.0F) > 0.0F && acceleration < 1.0F;
			if (stabilize) {
				if (!stabilizeBlock(target, settings)) return;
				if (!consumeFractional(settings.placeEnergyCost, false)) {
					deactivate();
					return;
				}
				placed++;
				continue;
			}

			// Inverted projectors operate on their interior through pumping, breaking and
			// stabilization; they do not fill the volume with collision blocks.
			if (settings.inverted || isPlacementCancelled(target)) {
				consumeFractional(settings.scanEnergyCost, false);
				continue;
			}
			if (!consumeFractional(settings.placeEnergyCost, false)) {
				deactivate();
				return;
			}
			placeField(target, settings);
			placed++;
		}
	}

	private void placeField(final BlockPos target, final ProjectionSettings settings) {
		final ForceFieldBlock fieldBlock = (ForceFieldBlock)
			Registration.FORCE_FIELD_BLOCKS.get(getTier().getName()).get();
		final int color = MathHelper.clamp(getBeamFrequency() * 16 / 65_000, 0, 15);
		level.setBlock(target, fieldBlock.defaultBlockState()
			.setValue(ForceFieldBlock.FREQUENCY, color)
			.setValue(ForceFieldBlock.CAMOUFLAGED, settings.camouflage != null), 2);
		final TileEntity tileEntity = level.getBlockEntity(target);
		if (tileEntity instanceof ForceFieldTileEntity) {
			((ForceFieldTileEntity) tileEntity).setProjector(this, settings.camouflage);
			placedFields.add(target.immutable());
		}
	}

	private boolean isFluidMutationCancelled(final BlockPos target, final BlockState targetState,
	                                         final ProjectionSettings settings) {
		return (settings.inverted
		    || settings.upgrades.getOrDefault(ForceFieldUpgrade.BREAKING, 0.0F) > 0.0F)
		     ? isBreakCancelled(target, targetState) : isPlacementCancelled(target);
	}

	private boolean isBreakCancelled(final BlockPos target, final BlockState targetState) {
		if (!(level instanceof ServerWorld)) return true;
		final BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, target, targetState,
			FakePlayerFactory.getMinecraft((ServerWorld) level));
		return MinecraftForge.EVENT_BUS.post(event);
	}

	private void pumpFluid(final BlockPos target, final FluidState fluidState,
	                       final ProjectionSettings settings) {
		if (fluidState.isSource()) {
			FluidStack remaining = new FluidStack(fluidState.getType(), 1000);
			for (final IFluidHandler handler : getFluidHandlers(settings.pumpingPorts)) {
				if (remaining.isEmpty()) break;
				final int filled = handler.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
				if (filled > 0) remaining.shrink(filled);
			}
		}
		level.setBlock(target, Blocks.AIR.defaultBlockState(), 2);
		if (!settings.inverted
		 && settings.upgrades.getOrDefault(ForceFieldUpgrade.BREAKING, 0.0F) <= 0.0F) {
			placeField(target, settings);
		}
	}

	private void breakBlock(final BlockPos target, final BlockState targetState,
	                        final ProjectionSettings settings) {
		final TileEntity tileEntity = level.getBlockEntity(target);
		final List<ItemStack> drops = Block.getDrops(targetState, (ServerWorld) level, target,
			tileEntity, FakePlayerFactory.getMinecraft((ServerWorld) level), ItemStack.EMPTY);
		final float acceleration = settings.upgrades.getOrDefault(ForceFieldUpgrade.ATTRACTION, 0.0F)
			- settings.upgrades.getOrDefault(ForceFieldUpgrade.REPULSION, 0.0F);
		final boolean collect = settings.upgrades.getOrDefault(ForceFieldUpgrade.ITEM_PORT, 0.0F) > 0.0F
			&& acceleration > 1.0F;
		final List<IItemHandler> handlers = collect
			? getItemHandlers(settings.itemPorts) : Collections.emptyList();
		for (final ItemStack drop : drops) {
			ItemStack remaining = drop.copy();
			for (final IItemHandler handler : handlers) {
				if (remaining.isEmpty()) break;
				remaining = ItemHandlerHelper.insertItemStacked(handler, remaining, false);
			}
			if (!remaining.isEmpty()) Block.popResource(level, target.above(), remaining);
		}
		level.levelEvent(2001, target, Block.getId(targetState));
		level.removeBlock(target, false);
	}

	private boolean stabilizeBlock(final BlockPos target, final ProjectionSettings settings) {
		for (final IItemHandler handler : getItemHandlers(settings.itemPorts)) {
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				final ItemStack itemStack = handler.getStackInSlot(slot);
				if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem)) continue;
				final BlockState placement = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
				if (placement.getBlock() instanceof ForceFieldBlock || !placement.canSurvive(level, target)) continue;
				if (isPlacementCancelled(target)) return false;
				if (handler.extractItem(slot, 1, true).isEmpty()) continue;
				if (handler.extractItem(slot, 1, false).isEmpty()) continue;
				level.setBlock(target, placement, 3);
				final SoundType sound = placement.getSoundType(level, target, null);
				level.playSound(null, target, sound.getPlaceSound(), SoundCategory.BLOCKS,
					(sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
				return true;
			}
		}
		// Matching the old projector, an empty/blocked material port pauses this batch so it
		// does not race across the volume while waiting for construction blocks.
		return false;
	}

	private List<IItemHandler> getItemHandlers(final List<BlockPos> ports) {
		final Set<IItemHandler> unique = Collections.newSetFromMap(new IdentityHashMap<>());
		for (final BlockPos port : ports) {
			for (final Direction direction : Direction.values()) {
				final BlockPos adjacent = port.relative(direction);
				if (!level.hasChunkAt(adjacent)) continue;
				final TileEntity tileEntity = level.getBlockEntity(adjacent);
				if (tileEntity != null) tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
					direction.getOpposite()).ifPresent(unique::add);
			}
		}
		return new ArrayList<>(unique);
	}

	private List<IFluidHandler> getFluidHandlers(final List<BlockPos> ports) {
		final Set<IFluidHandler> unique = Collections.newSetFromMap(new IdentityHashMap<>());
		for (final BlockPos port : ports) {
			for (final Direction direction : Direction.values()) {
				final BlockPos adjacent = port.relative(direction);
				if (!level.hasChunkAt(adjacent)) continue;
				final TileEntity tileEntity = level.getBlockEntity(adjacent);
				if (tileEntity != null) tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
					direction.getOpposite()).ifPresent(unique::add);
			}
		}
		return new ArrayList<>(unique);
	}

	private boolean isPlacementCancelled(final BlockPos target) {
		if (!(level instanceof ServerWorld)) return true;
		final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, target);
		return ForgeEventFactory.onBlockPlace(FakePlayerFactory.getMinecraft((ServerWorld) level),
			snapshot, Direction.UP);
	}

	private boolean isInsideAnotherProjector(final BlockPos target) {
		if (!(level instanceof ServerWorld)) return false;
		for (final AbstractForceFieldTileEntity tileEntity : ForceFieldRegistry.getNetwork(
			(ServerWorld) level, getBeamFrequency(), getBlockPos())) {
			if (tileEntity instanceof ForceFieldProjectorTileEntity && tileEntity != this
			 && ((ForceFieldProjectorTileEntity) tileEntity).isActive()
			 && ((ForceFieldProjectorTileEntity) tileEntity).isPartOfInterior(target)) {
				return true;
			}
		}
		return false;
	}

	private boolean consumeFractional(final double amount, final boolean simulate) {
		if (amount <= 0.0D) return true;
		final double total = amount + consumptionRemainder;
		final int whole = (int) Math.floor(total);
		if (!consumeEnergy(whole, simulate)) return false;
		if (!simulate) consumptionRemainder = total - whole;
		return true;
	}

	private double getBatchEnergyRequired(final ProjectionSettings settings) {
		return active
			? settings.scanEnergyCost * settings.scanSpeed * PROJECTION_UPDATE_TICKS / 20.0D
			: settings.startupEnergyCost
				+ settings.placeEnergyCost * settings.placeSpeed * PROJECTION_UPDATE_TICKS / 20.0D;
	}

	private void removeOwnedField(final BlockPos blockPos) {
		if (!placedFields.contains(blockPos) || level == null || !level.hasChunkAt(blockPos)) return;
		final TileEntity tileEntity = level.getBlockEntity(blockPos);
		if (tileEntity instanceof ForceFieldTileEntity
		 && ((ForceFieldTileEntity) tileEntity).isOwnedBy(this)) {
			level.removeBlock(blockPos, false);
		}
		placedFields.remove(blockPos);
	}

	private void deactivate() {
		if (active) cooldownTicks = COOLDOWN_TICKS;
		setActive(false);
		for (final BlockPos blockPos : new ArrayList<>(placedFields)) removeOwnedField(blockPos);
		placedFields.clear();
	}

	public void destroyForceField() {
		deactivate();
		ForceFieldRegistry.remove(this);
	}

	private void setActive(final boolean nextActive) {
		if (active == nextActive) return;
		active = nextActive;
		setChanged();
		final BlockState blockState = getBlockState();
		if (level != null && blockState.getBlock() instanceof ForceFieldProjectorBlock
		 && blockState.getValue(ForceFieldProjectorBlock.ACTIVE) != active) {
			level.setBlock(getBlockPos(), blockState.setValue(ForceFieldProjectorBlock.ACTIVE, active), 3);
		}
	}

	private void settleEntityInteractions() {
		final int count = interactedEntities.size();
		if (count > 0 && currentSettings != null) {
			final double energyCost = currentSettings.entityEnergyCost * 5.0D
				* Math.exp(-Math.log(5.0D) / count);
			consumeFractional(energyCost, false);
		}
		interactedEntities.clear();
	}

	public void onEntityContact(final BlockPos collisionPos, final Entity entity) {
		if (!active || currentSettings == null || !interactedEntities.add(entity.getUUID())) return;
		final float attraction = currentSettings.upgrades.getOrDefault(ForceFieldUpgrade.ATTRACTION, 0.0F);
		final float repulsion = currentSettings.upgrades.getOrDefault(ForceFieldUpgrade.REPULSION, 0.0F);
		final float acceleration = attraction - repulsion;
		final int entityLevel = entity instanceof PlayerEntity ? 4
			: entity instanceof MobEntity ? 3 : entity instanceof LivingEntity ? 2 : 1;
		if (Math.abs(acceleration) >= entityLevel) {
			Vector3d direction = Vector3d.atCenterOf(getBlockPos())
				.vectorTo(entity.position()).normalize();
			if (acceleration > 0.0F) direction = direction.scale(-1.0D);
			final double speed = Math.abs(acceleration) / entityLevel * 0.16D;
			entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(speed)));
			entity.fallDistance = 0.0F;
		}
		if (entity instanceof LivingEntity) {
			final LivingEntity living = (LivingEntity) entity;
			final float shock = currentSettings.upgrades.getOrDefault(ForceFieldUpgrade.SHOCK, 0.0F);
			if (shock > 0.0F) living.hurt(DamageSource.LIGHTNING_BOLT, shock);
			final float temperature = 300.0F
				+ currentSettings.upgrades.getOrDefault(ForceFieldUpgrade.HEATING, 0.0F)
				- currentSettings.upgrades.getOrDefault(ForceFieldUpgrade.COOLING, 0.0F);
			if (temperature > 305.0F) {
				living.setSecondsOnFire(1);
				living.hurt(DamageSource.IN_FIRE, (temperature - 300.0F) / 100.0F);
			} else if (temperature < 295.0F) {
				living.hurt(DamageSource.MAGIC, (300.0F - temperature) / 10.0F);
			}
		}
	}

	public void absorbLaserEnergy(final int laserEnergy) {
		if (laserEnergy <= 0 || currentSettings == null) return;
		final int cost = Math.max(1, (int) Math.round(
			laserEnergy / LASER_ENERGY_PER_PROJECTOR_DAMAGE + currentSettings.entityEnergyCost * 5.0D));
		consumeEnergy(Math.min(cost, getEnergyStored()), false);
		if (getEnergyStored() <= 0) deactivate();
	}

	public boolean absorbExplosionDamage(final double damageLevel) {
		if (damageLevel <= 0.0D || currentSettings == null) return getEnergyStored() > 0;
		consumeFractional(damageLevel / 1000.0D
			+ currentSettings.entityEnergyCost * 0.1D, false);
		return getEnergyStored() > 0;
	}

	public boolean isActive() { return active; }
	public boolean isDoubleSided() { return doubleSided; }
	public void setDoubleSided(final boolean value) {
		if (doubleSided == value) return;
		doubleSided = value;
		setChanged();
		onConfigurationChanged();
	}
	public ForceFieldShape getShape() { return shape; }

	public void setShape(final ForceFieldShape requestedShape) {
		shape = requestedShape == null ? ForceFieldShape.NONE : requestedShape;
		setChanged();
		final BlockState blockState = getBlockState();
		if (level != null && blockState.getBlock() instanceof ForceFieldProjectorBlock
		 && blockState.getValue(ForceFieldProjectorBlock.SHAPE) != shape) {
			level.setBlock(getBlockPos(), blockState.setValue(ForceFieldProjectorBlock.SHAPE, shape), 3);
		}
		onConfigurationChanged();
	}

	public int getUpgradeCount(final ForceFieldUpgrade upgrade) {
		return upgrades.getOrDefault(upgrade, 0);
	}

	public boolean addUpgrade(final ForceFieldUpgrade upgrade) {
		final int count = getUpgradeCount(upgrade);
		if (upgrade.getProjectorLimit() <= count) return false;
		upgrades.put(upgrade, count + 1);
		setChanged();
		onConfigurationChanged();
		return true;
	}

	public boolean removeUpgrade(final ForceFieldUpgrade upgrade) {
		final int count = getUpgradeCount(upgrade);
		if (count <= 0) return false;
		if (count == 1) upgrades.remove(upgrade); else upgrades.put(upgrade, count - 1);
		setChanged();
		onConfigurationChanged();
		return true;
	}

	@Nullable
	public ForceFieldUpgrade getAnyUpgrade() {
		for (final Map.Entry<ForceFieldUpgrade, Integer> entry : upgrades.entrySet()) {
			if (entry.getValue() > 0) return entry.getKey();
		}
		return null;
	}

	public boolean isPartOfForceField(final BlockPos blockPos) { return forceField.contains(blockPos); }
	public boolean isPartOfInterior(final BlockPos blockPos) { return interior.contains(blockPos); }

	@Override
	protected void onConfigurationChanged() {
		if (level != null && !level.isClientSide) {
			deactivate();
			currentSettings = null;
			forceField = Collections.emptySet();
			interior = Collections.emptySet();
			fieldIterator = null;
			projectionTicks = 0;
		}
	}

	public Object[] state() {
		final String status = !isEnabled() ? "disabled" : !isConnected() ? "not connected"
			: shape == ForceFieldShape.NONE ? "missing shape" : active ? "active" : "offline";
		return new Object[]{ status, isEnabled(), isConnected(), active,
			shape.getSerializedName(), getEnergyStored() };
	}

	public Object[] getEnergyRequired() {
		return currentSettings == null ? new Object[]{ false, 0 }
			: new Object[]{ true, (int) Math.ceil(getBatchEnergyRequired(currentSettings)) };
	}

	public Object[] getEnergyStatus() {
		return new Object[]{ getEnergyStored(), getMaxEnergyStored(), "FE" };
	}

	public Object[] min(@Nullable final Double x, @Nullable final Double y, @Nullable final Double z) {
		if (x != null && y != null && z != null) {
			minX = MathHelper.clamp(x.floatValue(), -1.0F, 0.0F);
			minY = MathHelper.clamp(y.floatValue(), -1.0F, 0.0F);
			minZ = MathHelper.clamp(z.floatValue(), -1.0F, 0.0F);
			setChanged(); onConfigurationChanged();
		}
		return new Object[]{ (double) minX, (double) minY, (double) minZ };
	}

	public Object[] max(@Nullable final Double x, @Nullable final Double y, @Nullable final Double z) {
		if (x != null && y != null && z != null) {
			maxX = MathHelper.clamp(x.floatValue(), 0.0F, 1.0F);
			maxY = MathHelper.clamp(y.floatValue(), 0.0F, 1.0F);
			maxZ = MathHelper.clamp(z.floatValue(), 0.0F, 1.0F);
			setChanged(); onConfigurationChanged();
		}
		return new Object[]{ (double) maxX, (double) maxY, (double) maxZ };
	}

	public Object[] translation(@Nullable final Double x, @Nullable final Double y, @Nullable final Double z) {
		if (x != null && y != null && z != null) {
			translationX = MathHelper.clamp(x.floatValue(), -1.0F, 1.0F);
			translationY = MathHelper.clamp(y.floatValue(), -1.0F, 1.0F);
			translationZ = MathHelper.clamp(z.floatValue(), -1.0F, 1.0F);
			setChanged(); onConfigurationChanged();
		}
		return new Object[]{ (double) translationX, (double) translationY, (double) translationZ };
	}

	public Object[] rotation(@Nullable final Double yaw, @Nullable final Double pitch, @Nullable final Double roll) {
		if (yaw != null) {
			rotationYaw = MathHelper.clamp(yaw.floatValue(), -45.0F, 45.0F);
			rotationPitch = pitch == null ? 0.0F : MathHelper.clamp(pitch.floatValue(), -45.0F, 45.0F);
			rotationRoll = roll == null ? 0.0F : normalizeAngle(roll.floatValue());
			setChanged(); onConfigurationChanged();
		}
		return new Object[]{ (double) rotationYaw, (double) rotationPitch, (double) rotationRoll };
	}

	private static float normalizeAngle(final float angle) {
		return (angle + 540.0F) % 360.0F - 180.0F;
	}

	@Override
	public int getMaxEnergyStored() { return getTier().getMaxEnergyStored(); }
	@Override
	protected int getMaxReceive() { return MAX_RECEIVE; }
	@Override
	protected int getMaxExtract() { return 0; }
	@Override
	protected boolean canReceiveFrom(@Nullable final Direction side) { return true; }

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		doubleSided = tagCompound.getBoolean(TAG_DOUBLE_SIDED);
		shape = ForceFieldShape.fromRegistrySuffix(tagCompound.getString(TAG_SHAPE));
		active = tagCompound.getBoolean(TAG_ACTIVE);
		minX = tagCompound.contains("minX") ? tagCompound.getFloat("minX") : -1.0F;
		minY = tagCompound.contains("minY") ? tagCompound.getFloat("minY") : -1.0F;
		minZ = tagCompound.contains("minZ") ? tagCompound.getFloat("minZ") : -1.0F;
		maxX = tagCompound.contains("maxX") ? tagCompound.getFloat("maxX") : 1.0F;
		maxY = tagCompound.contains("maxY") ? tagCompound.getFloat("maxY") : 1.0F;
		maxZ = tagCompound.contains("maxZ") ? tagCompound.getFloat("maxZ") : 1.0F;
		translationX = tagCompound.getFloat("translationX");
		translationY = tagCompound.getFloat("translationY");
		translationZ = tagCompound.getFloat("translationZ");
		rotationYaw = tagCompound.getFloat("rotationYaw");
		rotationPitch = tagCompound.getFloat("rotationPitch");
		rotationRoll = tagCompound.getFloat("rotationRoll");
		upgrades.clear();
		final CompoundNBT upgradeTag = tagCompound.getCompound(TAG_UPGRADES);
		for (final ForceFieldUpgrade upgrade : ForceFieldUpgrade.values()) {
			final int count = upgradeTag.getInt(upgrade.getSerializedName());
			if (count > 0) upgrades.put(upgrade, Math.min(count, upgrade.getProjectorLimit()));
		}
		active = false;
		currentSettings = null;
		forceField = Collections.emptySet();
		interior = Collections.emptySet();
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putBoolean(TAG_DOUBLE_SIDED, doubleSided);
		tagCompound.putString(TAG_SHAPE, shape.getSerializedName());
		tagCompound.putBoolean(TAG_ACTIVE, active);
		tagCompound.putFloat("minX", minX); tagCompound.putFloat("minY", minY); tagCompound.putFloat("minZ", minZ);
		tagCompound.putFloat("maxX", maxX); tagCompound.putFloat("maxY", maxY); tagCompound.putFloat("maxZ", maxZ);
		tagCompound.putFloat("translationX", translationX);
		tagCompound.putFloat("translationY", translationY);
		tagCompound.putFloat("translationZ", translationZ);
		tagCompound.putFloat("rotationYaw", rotationYaw);
		tagCompound.putFloat("rotationPitch", rotationPitch);
		tagCompound.putFloat("rotationRoll", rotationRoll);
		final CompoundNBT upgradeTag = new CompoundNBT();
		upgrades.forEach((upgrade, count) -> upgradeTag.putInt(upgrade.getSerializedName(), count));
		tagCompound.put(TAG_UPGRADES, upgradeTag);
		return tagCompound;
	}

	private static final class ProjectionResult {
		private final Set<BlockPos> forceField;
		private final Set<BlockPos> interior;
		private ProjectionResult(final Set<BlockPos> forceField, final Set<BlockPos> interior) {
			this.forceField = forceField;
			this.interior = interior;
		}
	}

	private static final class ProjectionSettings {
		private final BlockPos origin;
		private final ForceFieldShape shape;
		private final boolean doubleSided;
		private final int minX, minY, minZ, maxX, maxY, maxZ;
		private final int translationX, translationY, translationZ;
		private final float yaw, pitch, roll, thickness;
		private final boolean inverted, fusion;
		private final float scanSpeed, placeSpeed;
		private final double startupEnergyCost, scanEnergyCost, placeEnergyCost, entityEnergyCost;
		private final EnumMap<ForceFieldUpgrade, Float> upgrades;
		@Nullable private final BlockState camouflage;
		private final List<BlockPos> itemPorts;
		private final List<BlockPos> pumpingPorts;
		private final int worldHeight;

		private ProjectionSettings(final BlockPos origin, final ForceFieldShape shape,
		                           final boolean doubleSided,
		                           final int minX, final int minY, final int minZ,
		                           final int maxX, final int maxY, final int maxZ,
		                           final int translationX, final int translationY, final int translationZ,
		                           final float yaw, final float pitch, final float roll,
		                           final float thickness, final boolean inverted, final boolean fusion,
		                           final float scanSpeed, final float placeSpeed,
		                           final double startupEnergyCost, final double scanEnergyCost,
		                           final double placeEnergyCost, final double entityEnergyCost,
		                           final EnumMap<ForceFieldUpgrade, Float> upgrades,
		                           @Nullable final BlockState camouflage,
		                           final List<BlockPos> itemPorts,
		                           final List<BlockPos> pumpingPorts,
		                           final int worldHeight) {
			this.origin = origin.immutable(); this.shape = shape; this.doubleSided = doubleSided;
			this.minX = minX; this.minY = minY; this.minZ = minZ;
			this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
			this.translationX = translationX; this.translationY = translationY; this.translationZ = translationZ;
			this.yaw = yaw; this.pitch = pitch; this.roll = roll; this.thickness = thickness;
			this.inverted = inverted; this.fusion = fusion;
			this.scanSpeed = scanSpeed; this.placeSpeed = placeSpeed;
			this.startupEnergyCost = startupEnergyCost; this.scanEnergyCost = scanEnergyCost;
			this.placeEnergyCost = placeEnergyCost; this.entityEnergyCost = entityEnergyCost;
			this.upgrades = new EnumMap<>(upgrades);
			this.camouflage = camouflage;
			this.itemPorts = new ArrayList<>(itemPorts);
			this.pumpingPorts = new ArrayList<>(pumpingPorts);
			this.worldHeight = worldHeight;
		}

		@Override
		public boolean equals(final Object object) {
			if (this == object) return true;
			if (!(object instanceof ProjectionSettings)) return false;
			final ProjectionSettings that = (ProjectionSettings) object;
			return doubleSided == that.doubleSided && minX == that.minX && minY == that.minY
				&& minZ == that.minZ && maxX == that.maxX && maxY == that.maxY && maxZ == that.maxZ
				&& translationX == that.translationX && translationY == that.translationY
				&& translationZ == that.translationZ && Float.compare(yaw, that.yaw) == 0
				&& Float.compare(pitch, that.pitch) == 0 && Float.compare(roll, that.roll) == 0
				&& Float.compare(thickness, that.thickness) == 0 && inverted == that.inverted
				&& fusion == that.fusion && Float.compare(scanSpeed, that.scanSpeed) == 0
				&& Float.compare(placeSpeed, that.placeSpeed) == 0
				&& Double.compare(startupEnergyCost, that.startupEnergyCost) == 0
				&& Double.compare(scanEnergyCost, that.scanEnergyCost) == 0
				&& Double.compare(placeEnergyCost, that.placeEnergyCost) == 0
				&& Double.compare(entityEnergyCost, that.entityEnergyCost) == 0
				&& worldHeight == that.worldHeight && origin.equals(that.origin)
				&& shape == that.shape && upgrades.equals(that.upgrades)
				&& Objects.equals(camouflage, that.camouflage)
				&& itemPorts.equals(that.itemPorts) && pumpingPorts.equals(that.pumpingPorts);
		}

		@Override
		public int hashCode() {
			return Objects.hash(origin, shape, doubleSided, minX, minY, minZ, maxX, maxY, maxZ,
				translationX, translationY, translationZ, yaw, pitch, roll, thickness, inverted,
				fusion, scanSpeed, placeSpeed, startupEnergyCost, scanEnergyCost, placeEnergyCost,
				entityEnergyCost, upgrades, camouflage, itemPorts, pumpingPorts, worldHeight);
		}
	}
}
