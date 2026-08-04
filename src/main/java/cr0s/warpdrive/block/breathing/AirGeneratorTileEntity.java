package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.debug.DebugLog;
import cr0s.warpdrive.event.ChunkHandler;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Drives the air generator, ported from 1.12.2 TileEntityAirGeneratorTiered.
 *
 * Every interval it seeds an air source in the block it faces, at a pressure equal to the tier's
 * range. Propagation in AirSpreader then carries that outward, losing one per block, so the
 * generator itself does almost nothing - it just keeps re-asserting the source.
 *
 * Re-asserting matters rather than being wasteful: it is how the system recovers. If the source is
 * destroyed, or the volume depressurises, the next pass simply puts it back.
 *
 * Runs on Forge Energy, charged per push: more to establish a new air source than to refresh an
 * existing one. Out of power it does not simply stop - the air halves each interval and the source
 * is finally removed, so a failing reactor depressurises a ship gradually rather than killing
 * everyone the instant the lights go out.
 */
public class AirGeneratorTileEntity extends TileEntity implements ITickableTileEntity {

	/** Interval between air pushes, 1.12.2 BREATHING_AIR_GENERATION_TICKS. */
	private static final int AIR_GENERATION_TICKS = 40;

	/** Accepted per transfer, matching 1.12.2's generator input rate. */
	private static final int MAX_TRANSFER = 4096;

	private static final String TAG_ENERGY = "energy";

	private int tickUpdate;
	private int energyStored;

	public AirGeneratorTileEntity() {
		super(Registration.AIR_GENERATOR_TILE.get());
		// stagger generators so they do not all fire on the same tick
		this.tickUpdate = 0;
	}

	public Direction getFacing() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof AirGeneratorBlock
		     ? blockState.getValue(AirGeneratorBlock.FACING)
		     : Direction.NORTH;
	}

	private AirGeneratorTier getTier() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof AirGeneratorBlock
		     ? ((AirGeneratorBlock) blockState.getBlock()).getTier()
		     : AirGeneratorTier.BASIC;
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide) {
			return;
		}

		if (tickUpdate > 0) {
			tickUpdate--;
			return;
		}
		// spread the load: seed the next interval from the position so neighbouring generators
		// do not synchronise
		tickUpdate = AIR_GENERATION_TICKS;

		// Air generation is only meaningful where air can be lost
		if (!ChunkHandler.isSimulated(level)) {
			setActive(false);
			return;
		}

		setActive(releaseAir(getFacing()));
	}

	private boolean releaseAir(final Direction direction) {
		final BlockPos posDirection = getBlockPos().relative(direction);

		// Anything solid in front of the vent - cable, sign, machine - blocks it. 1.12.2 rejected
		// these explicitly rather than trying to push air through them.
		if (!level.getBlockState(posDirection).isAir()) {
			return false;
		}

		final ChunkData chunkData = ChunkHandler.getChunkData(level, posDirection.getX(), posDirection.getZ());
		if (chunkData == null) {
			// Chunk is not loaded. Assume it still works, so the generator does not flicker
			// inactive at the edge of loaded space.
			return true;
		}

		final StateAir stateAir = new StateAir(chunkData);
		try {
			stateAir.refresh(level, posDirection.getX(), posDirection.getY(), posDirection.getZ());
		} catch (final ExceptionChunkNotLoaded exception) {
			return true;
		}
		stateAir.updateBlockCache(level);

		if (!stateAir.isAir()) {
			return false;
		}

		// Refreshing an existing source is cheaper than establishing a new one, so a stable
		// pressurised ship costs far less than one that keeps losing its air
		final AirGeneratorTier tier = getTier();
		final int energyCost = stateAir.isAirSource()
		                     ? tier.getEnergyPerExistingAirBlock()
		                     : tier.getEnergyPerNewAirBlock();

		if (energyStored >= energyCost) {
			final short range = (short) (tier.getRange() - 1);
			stateAir.setAirSource(level, direction, range);
			energyStored -= energyCost;
			setChanged();
			return true;
		}

		// Out of power: let the air decay rather than vanish, so the crew get a warning
		if (stateAir.concentration > 4) {
			stateAir.setConcentration(level, (byte) (stateAir.concentration / 2));
		} else if (stateAir.concentration > 0) {
			stateAir.removeAirSource(level);
		}
		DebugLog.log("AIR", "generator at {} has {} of {} energy, needs {} - air decaying",
			getBlockPos(), energyStored, tier.getMaxEnergyStored(), energyCost);
		return false;
	}

	// ===== energy =====

	private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
		@Override
		public int receiveEnergy(final int maxReceive, final boolean simulate) {
			final int capacity = getTier().getMaxEnergyStored();
			final int received = Math.min(capacity - energyStored, Math.min(maxReceive, MAX_TRANSFER));
			if (!simulate && received > 0) {
				energyStored += received;
				setChanged();
			}
			return Math.max(0, received);
		}

		@Override
		public int extractEnergy(final int maxExtract, final boolean simulate) {
			return 0;   // consumer only
		}

		@Override
		public int getEnergyStored() {
			return energyStored;
		}

		@Override
		public int getMaxEnergyStored() {
			return getTier().getMaxEnergyStored();
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
	public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
		// Accepts from any face: the vent is one face and cabling the other five would be fiddly
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

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		energyStored = tagCompound.getInt(TAG_ENERGY);
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putInt(TAG_ENERGY, energyStored);
		return tagCompound;
	}

	private void setActive(final boolean isActive) {
		final BlockState blockState = getBlockState();
		if ( blockState.getBlock() instanceof AirGeneratorBlock
		  && blockState.getValue(AirGeneratorBlock.ACTIVE) != isActive ) {
			level.setBlock(getBlockPos(), blockState.setValue(AirGeneratorBlock.ACTIVE, isActive), 3);
		}
	}
}
