package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Replanting tree/crop farm with legacy medium-scaled area, delays and jungle-log rubber tapping. */
public class LaserTreeFarmTileEntity extends AbstractMinerTileEntity implements ITickableTileEntity {

	private static final int MAX_MEDIUMS = 5;
	private static final int TOTAL_MAX_RADIUS = 13;
	private static final int WARMUP_TICKS = 40;
	private static final int SCAN_TICKS = 40;
	private static final int PLANT_TICKS = 2;
	private static final int HARVEST_LOG_TICKS = 4;
	private static final int HARVEST_LEAF_TICKS = 2;

	private State state = State.IDLE;
	private int requestedRadiusX = TOTAL_MAX_RADIUS;
	private int requestedRadiusZ = TOTAL_MAX_RADIUS;
	private boolean breakLeaves = true;
	private boolean tapTrees = true;
	private int actualRadiusX;
	private int actualRadiusZ;
	private int maxDistance;
	private int scanEnergy;
	private int logEnergy = 1;
	private int leafEnergy = 1;
	private int taskTicks;
	private boolean powered;
	private int totalHarvested;
	private final List<BlockPos> soils = new ArrayList<>();
	private final List<BlockPos> targets = new ArrayList<>();
	private int soilIndex;
	private int targetIndex;

	private enum State { IDLE, WARMING_UP, SCANNING, PLANTING, HARVESTING }

	public LaserTreeFarmTileEntity() {
		super(Registration.LASER_TREE_FARM_TILE.get());
	}

	@Override protected int getMaxLaserMediumCount() { return MAX_MEDIUMS; }

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		updateParameters();
		if (!enabled) {
			reset();
			return;
		}
		if (--taskTicks > 0) return;

		switch (state) {
		case IDLE:
			totalHarvested = 0;
			state = State.WARMING_UP;
			taskTicks = 0;
			break;
		case WARMING_UP:
			setMode(powered ? LaserTreeFarmMode.SCANNING_POWERED : LaserTreeFarmMode.SCANNING_LOW_POWER);
			if (!areaLoaded() || isJammed()) {
				taskTicks = WARMUP_TICKS;
				break;
			}
			powered = consumeExactly(scanEnergy);
			if (!powered) {
				taskTicks = WARMUP_TICKS;
				break;
			}
			state = State.SCANNING;
			taskTicks = SCAN_TICKS;
			setMode(LaserTreeFarmMode.SCANNING_POWERED);
			sendScanOutline();
			break;
		case SCANNING:
			scanArea();
			if (!soils.isEmpty() && hasAdjacentPlantable()) {
				state = State.PLANTING;
				taskTicks = PLANT_TICKS;
				setMode(LaserTreeFarmMode.PLANTING_POWERED);
			} else if (!targets.isEmpty()) {
				state = State.HARVESTING;
				taskTicks = HARVEST_LOG_TICKS;
				setMode(LaserTreeFarmMode.FARMING_POWERED);
			} else {
				state = State.WARMING_UP;
				taskTicks = WARMUP_TICKS;
				level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
					SoundCategory.BLOCKS, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
			}
			break;
		case PLANTING:
			if (soilIndex >= soils.size()) {
				state = State.HARVESTING;
				taskTicks = HARVEST_LOG_TICKS;
				break;
			}
			final PlantResult result = plantOn(soils.get(soilIndex));
			if (result == PlantResult.RETRY) {
				powered = false;
				taskTicks = WARMUP_TICKS;
				setMode(LaserTreeFarmMode.PLANTING_LOW_POWER);
			} else {
				soilIndex++;
				taskTicks = PLANT_TICKS;
				setMode(LaserTreeFarmMode.PLANTING_POWERED);
			}
			break;
		case HARVESTING:
			if (targetIndex >= targets.size()) {
				state = State.WARMING_UP;
				taskTicks = 0;
				break;
			}
			final BlockPos target = targets.get(targetIndex++);
			final BlockState blockState = level.getBlockState(target);
			if (!isHarvestable(blockState, target)) break;
			if (!canBreakBlock(target)) break;
			final boolean log = blockState.is(BlockTags.LOGS);
			if (tapTrees && (blockState.is(Blocks.JUNGLE_LOG) || blockState.is(Blocks.JUNGLE_WOOD))) {
				if (!consumeExactly(2)) {
					targetIndex--;
					powered = false;
					taskTicks = WARMUP_TICKS;
					setMode(LaserTreeFarmMode.FARMING_LOW_POWER);
					break;
				}
				if (tapJungleLog(target)) totalHarvested++;
				taskTicks = 6;
			} else {
				final int energy = log ? logEnergy : leafEnergy;
				if (!consumeExactly(energy)) {
					targetIndex--;
					powered = false;
					taskTicks = WARMUP_TICKS;
					setMode(LaserTreeFarmMode.FARMING_LOW_POWER);
					break;
				}
				powered = true;
				sendBeam(Vector3d.atCenterOf(target), 0.2F, 0.7F, 0.4F);
				level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
					SoundCategory.BLOCKS, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
				if (harvestBlockUnchecked(target, blockState)) totalHarvested++;
				taskTicks = log || isMatureCrop(blockState) ? HARVEST_LOG_TICKS : HARVEST_LEAF_TICKS;
			}
			setMode(powered ? LaserTreeFarmMode.FARMING_POWERED : LaserTreeFarmMode.FARMING_LOW_POWER);
			break;
		default:
			reset();
		}
	}

	private void updateParameters() {
		final int maxRadius = CollectorTuning.treeFarmRadius(getLaserMediumFactor());
		actualRadiusX = Math.min(requestedRadiusX, maxRadius);
		actualRadiusZ = Math.min(requestedRadiusZ, maxRadius);
		maxDistance = CollectorTuning.treeFarmDistance(getLaserMediumFactor());
		scanEnergy = CollectorTuning.treeFarmScanEnergy(actualRadiusX, actualRadiusZ);
		logEnergy = silkTouch ? 2 : 1;
		leafEnergy = silkTouch ? 2 : 1;
	}

	private boolean areaLoaded() {
		for (final int x : new int[]{ getBlockPos().getX() - actualRadiusX,
		                                getBlockPos().getX() + actualRadiusX }) {
			for (final int z : new int[]{ getBlockPos().getZ() - actualRadiusZ,
			                                getBlockPos().getZ() + actualRadiusZ }) {
				if (!level.hasChunkAt(new BlockPos(x, getBlockPos().getY(), z))) return false;
			}
		}
		return true;
	}

	private boolean isJammed() {
		final BlockPos above = getBlockPos().above();
		final BlockState stateAbove = level.getBlockState(above);
		return !stateAbove.isAir(level, above)
		    && !stateAbove.is(BlockTags.LOGS) && !stateAbove.is(BlockTags.LEAVES);
	}

	private void scanArea() {
		soils.clear();
		targets.clear();
		soilIndex = 0;
		targetIndex = 0;
		final int minY = getBlockPos().getY();
		final int maxY = Math.min(level.getMaxBuildHeight() - 1, minY + maxDistance);
		for (int x = getBlockPos().getX() - actualRadiusX; x <= getBlockPos().getX() + actualRadiusX; x++) {
			for (int z = getBlockPos().getZ() - actualRadiusZ; z <= getBlockPos().getZ() + actualRadiusZ; z++) {
				for (int y = minY; y <= Math.min(minY + 8, maxY); y++) {
					final BlockPos soil = new BlockPos(x, y, z);
					if (isPotentialSoil(level.getBlockState(soil))
					 && level.getBlockState(soil.above()).getMaterial().isReplaceable()) soils.add(soil);
				}
				for (int y = minY + 1; y <= maxY; y++) {
					final BlockPos target = new BlockPos(x, y, z);
					if (isHarvestable(level.getBlockState(target), target)) targets.add(target);
				}
			}
		}
		targets.sort(this::compareTargets);
	}

	private int compareTargets(final BlockPos first, final BlockPos second) {
		final boolean firstCentral = first.getX() == getBlockPos().getX()
			&& first.getZ() == getBlockPos().getZ();
		final boolean secondCentral = second.getX() == getBlockPos().getX()
			&& second.getZ() == getBlockPos().getZ();
		if (firstCentral && secondCentral) return Integer.compare(first.getY(), second.getY());
		if (firstCentral) return -1;
		if (secondCentral) return 1;
		if (first.getY() != second.getY()) return Integer.compare(second.getY(), first.getY());
		final boolean firstLeaf = level.getBlockState(first).is(BlockTags.LEAVES);
		final boolean secondLeaf = level.getBlockState(second).is(BlockTags.LEAVES);
		if (firstLeaf != secondLeaf) return firstLeaf ? -1 : 1;
		return Integer.compare(first.distManhattan(getBlockPos()), second.distManhattan(getBlockPos()));
	}

	private boolean isHarvestable(final BlockState blockState, final BlockPos pos) {
		if (blockState.isAir(level, pos)) return false;
		if (blockState.is(BlockTags.LOGS)) return true;
		if (breakLeaves && blockState.is(BlockTags.LEAVES)) return true;
		if (isMatureCrop(blockState)) return true;
		final Block block = blockState.getBlock();
		return (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooBlock)
		    && level.getBlockState(pos.below()).getBlock() == block;
	}

	private static boolean isMatureCrop(final BlockState blockState) {
		final Block block = blockState.getBlock();
		if (block instanceof CropsBlock) return ((CropsBlock) block).isMaxAge(blockState);
		if (block instanceof NetherWartBlock) return blockState.getValue(NetherWartBlock.AGE) >= 3;
		if (block instanceof CocoaBlock) return blockState.getValue(CocoaBlock.AGE) >= 2;
		return block instanceof SweetBerryBushBlock && blockState.getValue(SweetBerryBushBlock.AGE) >= 3;
	}

	private static boolean isPotentialSoil(final BlockState blockState) {
		return blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.DIRT)
		    || blockState.is(Blocks.COARSE_DIRT) || blockState.is(Blocks.PODZOL)
		    || blockState.is(Blocks.FARMLAND) || blockState.is(Blocks.SOUL_SAND)
		    || blockState.is(Blocks.SAND) || blockState.is(Blocks.RED_SAND)
		    || blockState.is(Blocks.MYCELIUM);
	}

	private PlantResult plantOn(final BlockPos soil) {
		final BlockPos plantPos = soil.above();
		if (!level.getBlockState(plantPos).getMaterial().isReplaceable()) return PlantResult.CONTINUE;
		for (final Direction direction : Direction.values()) {
			final TileEntity tile = level.getBlockEntity(getBlockPos().relative(direction));
			if (tile == null) continue;
			final LazyOptional<IItemHandler> capability = tile.getCapability(
				CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
			if (!capability.isPresent()) continue;
			final IItemHandler inventory = capability.orElseThrow(
				() -> new IllegalStateException("Present item capability had no value"));
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				final ItemStack simulated = inventory.extractItem(slot, 1, true);
				if (simulated.isEmpty() || !(simulated.getItem() instanceof BlockItem)) continue;
				final BlockState plantState = ((BlockItem) simulated.getItem()).getBlock().defaultBlockState();
				if (!isPlantableState(plantState)) continue;
				if (!canPlaceBlock(plantPos, plantState)) continue;
				if (!consumeExactly(1)) return PlantResult.RETRY;
				final ItemStack extracted = inventory.extractItem(slot, 1, false);
				if (extracted.isEmpty()) return PlantResult.CONTINUE;
				level.setBlock(plantPos, plantState, 3);
				sendBeam(Vector3d.atCenterOf(plantPos), 0.2F, 0.7F, 0.4F);
				level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
					SoundCategory.BLOCKS, 1.0F, 0.35F + level.random.nextFloat() * 0.3F);
				powered = true;
				return PlantResult.CONTINUE;
			}
		}
		return PlantResult.CONTINUE;
	}

	private boolean hasAdjacentPlantable() {
		for (final Direction direction : Direction.values()) {
			final TileEntity tile = level.getBlockEntity(getBlockPos().relative(direction));
			if (tile == null) continue;
			final LazyOptional<IItemHandler> capability = tile.getCapability(
				CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
			if (!capability.isPresent()) continue;
			final IItemHandler inventory = capability.orElseThrow(
				() -> new IllegalStateException("Present item capability had no value"));
			for (int slot = 0; slot < inventory.getSlots(); slot++) {
				final ItemStack itemStack = inventory.getStackInSlot(slot);
				if (itemStack.getItem() instanceof BlockItem
				 && isPlantableState(((BlockItem) itemStack.getItem()).getBlock().defaultBlockState())) return true;
			}
		}
		return false;
	}

	private static boolean isPlantableState(final BlockState blockState) {
		final Block block = blockState.getBlock();
		return block instanceof BushBlock || block instanceof SugarCaneBlock
		    || block instanceof CactusBlock || block instanceof BambooBlock
		    || block instanceof CocoaBlock;
	}

	private boolean tapJungleLog(final BlockPos target) {
		final ItemStack rubber = new ItemStack(Registration.COMPONENTS.get("raw_rubber").get());
		if (outputDrops(java.util.Collections.singletonList(rubber))) setEnabled(false);
		level.removeBlock(target, false);
		sendBeam(Vector3d.atCenterOf(target), 0.8F, 0.8F, 0.2F);
		level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
			SoundCategory.BLOCKS, 1.0F, 0.35F + level.random.nextFloat() * 0.3F);
		return true;
	}

	private void sendScanOutline() {
		final double y = getBlockPos().getY() + 1.0D;
		sendBeam(new Vector3d(getBlockPos().getX() - actualRadiusX, y,
			getBlockPos().getZ() - actualRadiusZ), 0.3F, 0.0F, 1.0F);
		sendBeam(new Vector3d(getBlockPos().getX() + actualRadiusX + 1, y,
			getBlockPos().getZ() + actualRadiusZ + 1), 0.3F, 0.0F, 1.0F);
	}

	private void setMode(final LaserTreeFarmMode mode) {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof LaserTreeFarmBlock
		 && blockState.getValue(LaserTreeFarmBlock.MODE) != mode) {
			level.setBlock(getBlockPos(), blockState.setValue(LaserTreeFarmBlock.MODE, mode), 3);
		}
	}

	private void reset() {
		state = State.IDLE;
		taskTicks = 0;
		soils.clear();
		targets.clear();
		soilIndex = 0;
		targetIndex = 0;
		powered = false;
		setMode(LaserTreeFarmMode.INACTIVE);
	}

	public String getStatusText() {
		if (!enabled || state == State.IDLE) return "idle";
		if (state == State.WARMING_UP) return "warming up";
		if (state == State.SCANNING) return "scanning";
		if (state == State.PLANTING) return powered ? "planting" : "planting - insufficient energy";
		return powered ? "harvesting" : "harvesting - insufficient energy";
	}

	public int getTotalHarvested() { return totalHarvested; }
	public Object[] state() {
		return new Object[]{ getStatusText(), state != State.IDLE, getLaserMediumEnergyStored(),
			totalHarvested, state == State.IDLE ? 0 : targetIndex,
			state == State.IDLE ? 0 : targets.size() };
	}
	public Object[] getEnergyRequired() {
		return new Object[]{ true, scanEnergy, 1, 2, logEnergy, leafEnergy, 1, "FE" };
	}
	public Object[] radius(@Nullable final Integer x, @Nullable final Integer z) {
		if (x != null) {
			requestedRadiusX = MathHelper.clamp(Math.abs(x), 1, TOTAL_MAX_RADIUS);
			requestedRadiusZ = MathHelper.clamp(Math.abs(z == null ? x : z), 1, TOTAL_MAX_RADIUS);
			setChanged();
		}
		return new Object[]{ requestedRadiusX, requestedRadiusZ };
	}
	public Object[] breakLeaves(@Nullable final Boolean requested) {
		if (requested != null) { breakLeaves = requested; setChanged(); }
		return new Object[]{ breakLeaves };
	}
	public Object[] tapTrees(@Nullable final Boolean requested) {
		if (requested != null) { tapTrees = requested; setChanged(); }
		return new Object[]{ tapTrees };
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		requestedRadiusX = tagCompound.contains("radiusX")
			? MathHelper.clamp(tagCompound.getInt("radiusX"), 1, TOTAL_MAX_RADIUS) : TOTAL_MAX_RADIUS;
		requestedRadiusZ = tagCompound.contains("radiusZ")
			? MathHelper.clamp(tagCompound.getInt("radiusZ"), 1, TOTAL_MAX_RADIUS) : TOTAL_MAX_RADIUS;
		breakLeaves = !tagCompound.contains("breakLeaves") || tagCompound.getBoolean("breakLeaves");
		tapTrees = !tagCompound.contains("tapTrees") || tagCompound.getBoolean("tapTrees");
		state = State.IDLE;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putInt("radiusX", requestedRadiusX);
		tagCompound.putInt("radiusZ", requestedRadiusZ);
		tagCompound.putBoolean("breakLeaves", breakLeaves);
		tagCompound.putBoolean("tapTrees", tapTrees);
		return tagCompound;
	}

	private enum PlantResult { CONTINUE, RETRY }
}
