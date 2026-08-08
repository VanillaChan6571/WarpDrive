package cr0s.warpdrive.block;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shared Forge Energy buffer for WarpDrive machines, standing in for 1.12.2's
 * TileEntityAbstractEnergy [917].
 *
 * The original bridged four energy APIs at once - Forge Energy, RF, IC2 EU and GregTech - which is
 * most of why it was that large. On 1.16.5 only Forge Energy exists, so what is left is the part
 * that actually varies between machines: capacity, transfer rates, and which faces accept or emit.
 *
 * Energy is a plain int in FE. 1.12.2 stored "internal units" on an EU scale and converted at the
 * boundary; this port maps internal units to FE 1:1, so the 1.12.2 numbers carry over unchanged.
 *
 * Capabilities are handed out per face, because a machine may accept on one side and emit on
 * another. Subclasses that let players re-route faces must call {@link #invalidateEnergySides} so
 * neighbours re-query rather than holding a stale handler.
 */
public abstract class AbstractEnergyTileEntity extends TileEntity {

	protected static final String TAG_ENERGY = "energy";

	/** Indexed by Direction.ordinal(), plus one slot for the null (side-agnostic) handler. */
	private final LazyOptional<?>[] energyHandlers = new LazyOptional<?>[Direction.values().length + 1];

	protected int energyStored;

	protected AbstractEnergyTileEntity(final TileEntityType<?> tileEntityType) {
		super(tileEntityType);
	}

	// ===== contract =====

	/** Buffer size in FE. */
	public abstract int getMaxEnergyStored();

	/** Most FE this machine accepts per transfer. */
	protected abstract int getMaxReceive();

	/** Most FE this machine emits per transfer. 0 for a pure consumer. */
	protected abstract int getMaxExtract();

	/** @param side the face being queried, or null for a side-agnostic query. */
	protected boolean canReceiveFrom(@Nullable final Direction side) {
		return getMaxReceive() > 0;
	}

	protected boolean canExtractTo(@Nullable final Direction side) {
		return getMaxExtract() > 0;
	}

	// ===== buffer =====

	public int getEnergyStored() {
		return energyStored;
	}

	/**
	 * Spend from the buffer.
	 *
	 * @return true when the buffer covers the cost; energy is only deducted when not simulating.
	 */
	public boolean consumeEnergy(final int amount, final boolean simulate) {
		if (amount <= 0) {
			return true;
		}
		if (energyStored < amount) {
			return false;
		}
		if (!simulate) {
			energyStored -= amount;
			setChanged();
		}
		return true;
	}

	/** @return how much was actually accepted, respecting only the buffer size. */
	public int addEnergy(final int amount, final boolean simulate) {
		final int accepted = Math.min(Math.max(0, amount), getMaxEnergyStored() - energyStored);
		if (!simulate && accepted > 0) {
			energyStored += accepted;
			setChanged();
		}
		return accepted;
	}

	public void setEnergy(final int amount) {
		energyStored = Math.max(0, Math.min(getMaxEnergyStored(), amount));
		setChanged();
	}

	// ===== capability =====

	/** Drop the cached per-face handlers, so neighbours re-query after a routing change. */
	protected void invalidateEnergySides() {
		for (int index = 0; index < energyHandlers.length; index++) {
			if (energyHandlers[index] != null) {
				energyHandlers[index].invalidate();
				energyHandlers[index] = null;
			}
		}
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
	                                         @Nullable final Direction side) {
		if (capability != CapabilityEnergy.ENERGY) {
			return super.getCapability(capability, side);
		}
		final int index = side == null ? energyHandlers.length - 1 : side.ordinal();
		if (energyHandlers[index] == null) {
			energyHandlers[index] = LazyOptional.of(() -> new SidedEnergyStorage(side));
		}
		return (LazyOptional<T>) energyHandlers[index];
	}

	@Override
	protected void invalidateCaps() {
		super.invalidateCaps();
		invalidateEnergySides();
	}

	private final class SidedEnergyStorage implements IEnergyStorage {

		@Nullable
		private final Direction side;

		private SidedEnergyStorage(@Nullable final Direction side) {
			this.side = side;
		}

		@Override
		public int receiveEnergy(final int maxReceive, final boolean simulate) {
			if (!canReceiveFrom(side)) {
				return 0;
			}
			return addEnergy(Math.min(maxReceive, getMaxReceive()), simulate);
		}

		@Override
		public int extractEnergy(final int maxExtract, final boolean simulate) {
			if (!canExtractTo(side)) {
				return 0;
			}
			final int extracted = Math.min(Math.min(maxExtract, getMaxExtract()), getEnergyStored());
			if (extracted <= 0) {
				return 0;
			}
			// through consumeEnergy so a subclass with unusual accounting - a creative buffer, a
			// machine with transfer losses - stays in control of what leaving actually costs
			if (!consumeEnergy(extracted, simulate)) {
				return 0;
			}
			return extracted;
		}

		@Override
		public int getEnergyStored() {
			return AbstractEnergyTileEntity.this.getEnergyStored();
		}

		@Override
		public int getMaxEnergyStored() {
			return AbstractEnergyTileEntity.this.getMaxEnergyStored();
		}

		@Override
		public boolean canExtract() {
			return canExtractTo(side);
		}

		@Override
		public boolean canReceive() {
			return canReceiveFrom(side);
		}
	}

	// ===== persistence =====

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
}
