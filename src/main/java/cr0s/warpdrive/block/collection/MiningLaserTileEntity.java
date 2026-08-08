package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.data.DimensionAltitude;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Downward layer quarry driven by up to three adjacent same-tier laser media. */
public class MiningLaserTileEntity extends AbstractMinerTileEntity implements ITickableTileEntity {

	private static final int MAX_MEDIUMS = 3;
	private static final int WARMUP_TICKS = 20;
	private static final int SCAN_TICKS = 20;
	private static final int MINE_TICKS = 3;
	private static final int SCAN_ENERGY_ATMOSPHERE = 30_000;
	private static final int SCAN_ENERGY_VOID = 20_000;
	private static final ITag.INamedTag<net.minecraft.block.Block> ORES =
		BlockTags.createOptional(new ResourceLocation("forge", "ores"));

	private State state = State.IDLE;
	private int layerOffset = 1;
	private boolean mineAllBlocks = true;
	private int currentLayer;
	private int taskTicks;
	private int updateTicks;
	private int radius;
	private int scanEnergy = SCAN_ENERGY_ATMOSPHERE;
	private int mineEnergy = 2_500;
	private int pumpCount;
	private boolean powered;
	private final List<BlockPos> targets = new ArrayList<>();
	private int targetIndex;

	private enum State { IDLE, WARMING_UP, START_SCANNING, SCANNING, MINING }

	public MiningLaserTileEntity() {
		super(Registration.MINING_LASER_TILE.get());
	}

	@Override protected int getMaxLaserMediumCount() { return MAX_MEDIUMS; }

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		if (!enabled) {
			if (state != State.IDLE) reset();
			return;
		}
		if (--updateTicks <= 0) {
			updateTicks = 20;
			updateParameters();
		}
		if (--taskTicks > 0) return;

		switch (state) {
		case IDLE:
			currentLayer = getBlockPos().getY() - layerOffset - 1;
			state = State.WARMING_UP;
			taskTicks = WARMUP_TICKS;
			setMode(MiningLaserMode.SCANNING_LOW_POWER);
			break;
		case WARMING_UP:
			state = State.START_SCANNING;
			taskTicks = 0;
			break;
		case START_SCANNING:
			powered = hasEnergy(scanEnergy);
			if (!powered) {
				taskTicks = WARMUP_TICKS;
				setMode(MiningLaserMode.SCANNING_LOW_POWER);
				break;
			}
			state = State.SCANNING;
			taskTicks = SCAN_TICKS;
			setMode(MiningLaserMode.SCANNING_POWERED);
			sendScanOutline();
			break;
		case SCANNING:
			if (currentLayer <= 0) {
				setEnabled(false);
				reset();
				break;
			}
			powered = consumeExactly(scanEnergy);
			if (!powered) {
				taskTicks = WARMUP_TICKS;
				setMode(MiningLaserMode.SCANNING_LOW_POWER);
				break;
			}
			scanLayer();
			if (!enabled) break;
			if (targets.isEmpty()) {
				level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
					SoundCategory.BLOCKS, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
				currentLayer--;
				taskTicks = SCAN_TICKS;
				state = State.START_SCANNING;
			} else {
				state = State.MINING;
				taskTicks = MINE_TICKS;
				setMode(MiningLaserMode.MINING_POWERED);
			}
			break;
		case MINING:
			taskTicks = MINE_TICKS;
			if (targetIndex >= targets.size()) {
				scanLayer();
				if (targets.isEmpty()) currentLayer--;
				state = State.START_SCANNING;
				taskTicks = SCAN_TICKS;
				setMode(MiningLaserMode.SCANNING_POWERED);
				break;
			}
			powered = consumeExactly(mineEnergy);
			if (!powered) {
				setMode(MiningLaserMode.MINING_LOW_POWER);
				break;
			}
			setMode(MiningLaserMode.MINING_POWERED);
			final BlockPos target = targets.get(targetIndex++);
			final BlockState blockState = level.getBlockState(target);
			if (!canDig(blockState, target)) break;
			sendBeam(Vector3d.atCenterOf(target), 1.0F, 1.0F, 0.0F);
			level.playSound(null, getBlockPos(), Registration.SOUND_LASER_LOW.get(),
				SoundCategory.BLOCKS, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
			harvestBlock(target, blockState);
			break;
		default:
			reset();
		}
	}

	private void updateParameters() {
		radius = CollectorTuning.miningRadius(getLaserMediumFactor());
		final boolean atmosphere = !DimensionAltitude.isSpaceOrHyperspace(level);
		scanEnergy = atmosphere ? SCAN_ENERGY_ATMOSPHERE : SCAN_ENERGY_VOID;
		mineEnergy = CollectorTuning.miningEnergy(atmosphere, mineAllBlocks, silkTouch);
	}

	private void scanLayer() {
		targets.clear();
		targetIndex = 0;
		for (int y = getBlockPos().getY() - 1; y > currentLayer; y--) {
			final BlockPos obstruction = new BlockPos(getBlockPos().getX(), y, getBlockPos().getZ());
			if (level.getBlockState(obstruction).is(WarpDriveTags.MINING_STOP)) {
				setEnabled(false);
				reset();
				return;
			}
		}
		addIfMineable(getBlockPos().getX(), getBlockPos().getZ());
		for (int ring = 1; ring <= radius; ring++) {
			final int minX = getBlockPos().getX() - ring;
			final int maxX = getBlockPos().getX() + ring;
			final int minZ = getBlockPos().getZ() - ring;
			final int maxZ = getBlockPos().getZ() + ring;
			for (int x = minX; x <= maxX; x++) addIfMineable(x, minZ);
			for (int z = minZ + 1; z <= maxZ; z++) addIfMineable(maxX, z);
			for (int x = maxX - 1; x >= minX; x--) addIfMineable(x, maxZ);
			for (int z = maxZ - 1; z > minZ; z--) addIfMineable(minX, z);
		}
	}

	private void addIfMineable(final int x, final int z) {
		final BlockPos target = new BlockPos(x, currentLayer, z);
		if (!level.hasChunkAt(target)) return;
		final BlockState stateAtTarget = level.getBlockState(target);
		if (canDig(stateAtTarget, target)
		 && (mineAllBlocks || stateAtTarget.is(ORES) || stateAtTarget.is(WarpDriveTags.MINING))) {
			targets.add(target);
		}
	}

	private boolean canDig(final BlockState blockState, final BlockPos target) {
		if (blockState.isAir(level, target) || blockState.is(WarpDriveTags.MINING_SKIP)) return false;
		if (blockState.is(WarpDriveTags.MINING_STOP)) {
			setEnabled(false);
			reset();
			return false;
		}
		final FluidState fluidState = blockState.getFluidState();
		if (!fluidState.isEmpty()) {
			return pumpCount > 0 && fluidState.getType().getAttributes().getViscosity(level, target)
				<= pumpCount * 2_500;
		}
		final float hardness = blockState.getDestroySpeed(level, target);
		return hardness >= 0.0F && blockState.getBlock().getExplosionResistance()
			<= Blocks.OBSIDIAN.getExplosionResistance();
	}

	private void sendScanOutline() {
		final double y = currentLayer + 1.0D;
		sendBeam(new Vector3d(getBlockPos().getX() - radius, y, getBlockPos().getZ() - radius),
			0.3F, 0.0F, 1.0F);
		sendBeam(new Vector3d(getBlockPos().getX() + radius + 1, y, getBlockPos().getZ() + radius + 1),
			0.3F, 0.0F, 1.0F);
	}

	private void setMode(final MiningLaserMode mode) {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof MiningLaserBlock
		 && blockState.getValue(MiningLaserBlock.MODE) != mode) {
			level.setBlock(getBlockPos(), blockState.setValue(MiningLaserBlock.MODE, mode), 3);
		}
	}

	private void reset() {
		state = State.IDLE;
		taskTicks = 0;
		targets.clear();
		targetIndex = 0;
		powered = false;
		setMode(MiningLaserMode.INACTIVE);
	}

	public String getStatusText() {
		if (!enabled || state == State.IDLE) return "idle";
		if (state == State.WARMING_UP) return "warming up";
		if (state == State.MINING) return powered ? "mining" : "mining - insufficient energy";
		return powered ? "scanning" : "scanning - insufficient energy";
	}

	public Object[] state() {
		return new Object[]{ getStatusText(), state != State.IDLE, getLaserMediumEnergyStored(),
			currentLayer, state == State.IDLE ? 0 : targetIndex, state == State.IDLE ? 0 : targets.size() };
	}

	public Object[] getEnergyRequired() { return new Object[]{ true, scanEnergy, mineEnergy, "FE" }; }

	public Object[] offset(@Nullable final Integer requested) {
		if (requested != null) {
			layerOffset = Math.min(256, Math.abs(requested));
			setChanged();
		}
		return new Object[]{ layerOffset };
	}

	public Object[] onlyOres(@Nullable final Boolean requested) {
		if (requested != null) {
			mineAllBlocks = !requested;
			updateParameters();
			setChanged();
		}
		return new Object[]{ !mineAllBlocks };
	}

	public int getPumpCount() { return pumpCount; }
	public boolean addPump() {
		if (pumpCount >= 20) return false;
		pumpCount++;
		setChanged();
		return true;
	}
	public boolean removePump() {
		if (pumpCount <= 0) return false;
		pumpCount--;
		setChanged();
		return true;
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		layerOffset = tagCompound.contains("layerOffset")
			? MathHelper.clamp(tagCompound.getInt("layerOffset"), 0, 256) : 1;
		mineAllBlocks = !tagCompound.contains("mineAllBlocks") || tagCompound.getBoolean("mineAllBlocks");
		currentLayer = tagCompound.getInt("currentLayer");
		pumpCount = MathHelper.clamp(tagCompound.getInt("pumpCount"), 0, 20);
		state = State.IDLE;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putInt("layerOffset", layerOffset);
		tagCompound.putBoolean("mineAllBlocks", mineAllBlocks);
		tagCompound.putInt("currentLayer", currentLayer);
		tagCompound.putInt("pumpCount", pumpCount);
		return tagCompound;
	}
}
