package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.SideMode;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.util.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Subspace capacitor, ported from 1.12.2 TileEntityCapacitor.
 *
 * A buffer with per-face routing: each of the six faces is independently disabled, an input, or an
 * output, cycled with a wrench. Defaults are the 1.12.2 ones - bottom and top accept, the four
 * sides emit - which is what makes a stack of capacitors chain without configuration.
 *
 * Transfers are lossy. 1.12.2 charged the loss on the way out, so drawing X from the buffer
 * actually costs X / efficiency, with efficiency rising from 95% to 100% as superconductor upgrades
 * are fitted. The upgrade system is not ported yet, so this sits at the un-upgraded 95%.
 */
public class CapacitorTileEntity extends AbstractEnergyTileEntity {

	private static final String TAG_MODE_SIDE = "modeSide";

	/** 1.12.2 default: DOWN and UP accept, the four horizontals emit. */
	private static final SideMode[] MODE_DEFAULT = {
		SideMode.INPUT, SideMode.INPUT,
		SideMode.OUTPUT, SideMode.OUTPUT, SideMode.OUTPUT, SideMode.OUTPUT };

	/**
	 * Transfer efficiency with no upgrades fitted, 1.12.2 CAPACITOR_EFFICIENCY_PER_UPGRADE[0].
	 * Superconductor upgrades raised this to 0.98 then 1.0; see PORT_CHECKLIST.md.
	 */
	private static final double EFFICIENCY = 0.95D;

	private final SideMode[] modeSide = MODE_DEFAULT.clone();

	/**
	 * Cached rather than read from the block state: the tile is constructed before it is placed in
	 * the world, so getBlockState is not usable from the constructor.
	 */
	private CapacitorTier tier;

	public CapacitorTileEntity() {
		this(CapacitorTier.BASIC);
	}

	public CapacitorTileEntity(final CapacitorTier tier) {
		super(Registration.CAPACITOR_TILE.get());
		this.tier = tier;
	}

	public CapacitorTier getTier() {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof CapacitorBlock) {
			tier = ((CapacitorBlock) blockState.getBlock()).getTier();
		}
		return tier;
	}

	@Override
	public int getMaxEnergyStored() {
		return getTier().getMaxEnergyStored();
	}

	@Override
	protected int getMaxReceive() {
		return getTier().getTransferRate();
	}

	@Override
	protected int getMaxExtract() {
		return getTier().getTransferRate();
	}

	@Override
	protected boolean canReceiveFrom(@Nullable final Direction side) {
		return hasMode(side, SideMode.INPUT);
	}

	@Override
	protected boolean canExtractTo(@Nullable final Direction side) {
		return hasMode(side, SideMode.OUTPUT);
	}

	/** A null side means "any face", which 1.12.2 answered by scanning all six. */
	private boolean hasMode(@Nullable final Direction side, final SideMode mode) {
		if (side != null) {
			return modeSide[side.ordinal()] == mode;
		}
		for (final SideMode modeCurrent : modeSide) {
			if (modeCurrent == mode) {
				return true;
			}
		}
		return false;
	}

	// ===== energy, with transfer losses =====

	@Override
	public int getEnergyStored() {
		// a creative capacitor reads half full and never moves, exactly as in 1.12.2
		return getTier().isCreative() ? getMaxEnergyStored() / 2 : super.getEnergyStored();
	}

	@Override
	public boolean consumeEnergy(final int amount, final boolean simulate) {
		if (getTier().isCreative()) {
			return true;
		}
		// the loss is charged on withdrawal: taking `amount` out drains amount / efficiency
		final int amountWithLoss = (int) Math.round(amount / EFFICIENCY);
		return super.consumeEnergy(amountWithLoss, simulate);
	}

	@Override
	public int addEnergy(final int amount, final boolean simulate) {
		if (getTier().isCreative()) {
			return amount;   // bottomless sink, so a creative capacitor never backs up a network
		}
		return super.addEnergy(amount, simulate);
	}

	// ===== face routing =====

	public SideMode getMode(final Direction side) {
		return modeSide[side.ordinal()];
	}

	public void setMode(final Direction side, final SideMode mode) {
		modeSide[side.ordinal()] = mode;
		setChanged();
		// neighbours cache our handler, so it has to be dropped for the new routing to take effect
		invalidateEnergySides();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
			level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
		}
	}

	// ===== persistence =====

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		final byte[] bytes = tagCompound.getByteArray(TAG_MODE_SIDE);
		if (bytes.length != modeSide.length) {
			System.arraycopy(MODE_DEFAULT, 0, modeSide, 0, modeSide.length);
			return;
		}
		for (int index = 0; index < modeSide.length; index++) {
			modeSide[index] = SideMode.byIndex(bytes[index]);
		}
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		final byte[] bytes = new byte[modeSide.length];
		for (int index = 0; index < modeSide.length; index++) {
			bytes[index] = (byte) modeSide[index].getIndex();
		}
		tagCompound.putByteArray(TAG_MODE_SIDE, bytes);
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
}
