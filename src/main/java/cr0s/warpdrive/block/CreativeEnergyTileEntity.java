package cr0s.warpdrive.block;

import cr0s.warpdrive.container.CreativeEnergyContainer;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Creative energy source. Emits a configurable number of FE per tick out of one chosen face, so a
 * survival-like generation rate can be simulated (solar panel, furnace generator, reactor, ...)
 * without the block silently topping anything up.
 *
 * This deliberately replaces the old creative auto-charge that lived in ShipCoreTileEntity.tick():
 * that one refilled the core to maximum every second, which made every energy cost invisible.
 */
public class CreativeEnergyTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {

	/** Upper bound for the typed value. The slider covers a smaller, more useful range. */
	public static final int MAX_RATE = 1_000_000;

	/** Named rates, roughly matching common survival generators. */
	public static final int[] PRESET_RATES = { 0, 5, 20, 120, 500, 2_000 };
	public static final String[] PRESET_NAMES = { "Off", "Solar", "Furnace", "Generator", "Reactor", "Fusion" };

	// Why nothing is flowing, so the GUI can say something more useful than "not accepting power"
	public static final int STATUS_OFF = 0;
	public static final int STATUS_NO_BLOCK = 1;
	public static final int STATUS_NO_CAPABILITY = 2;
	public static final int STATUS_RECEIVER_FULL = 3;
	public static final int STATUS_DELIVERING = 4;

	private int outputRate = 20;
	private Direction outputFace = Direction.DOWN;

	// Diagnostics: what actually left the block last tick, as opposed to what was configured
	private int lastPushed = 0;
	private int lastStatus = STATUS_NO_BLOCK;
	private int syncCooldown = 0;
	private boolean needsSync = false;

	public CreativeEnergyTileEntity() {
		super(Registration.CREATIVE_ENERGY_TILE.get());
	}

	// ===== Configuration =====

	public int getOutputRate() {
		return outputRate;
	}

	public Direction getOutputFace() {
		return outputFace;
	}

	public int getLastPushed() {
		return lastPushed;
	}

	public void configure(final int rate, final Direction face) {
		outputRate = Math.max(0, Math.min(MAX_RATE, rate));
		outputFace = face == null ? Direction.DOWN : face;
		energyHandler.invalidate();
		energyHandler = LazyOptional.of(this::createEnergyStorage);
		setChanged();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
			DebugLog.log("ENERGY", "creative source at {} configured: {} FE/t out of {}",
				getBlockPos(), outputRate, outputFace);
		}
	}

	// ===== Output =====

	@Override
	public void tick() {
		if (level == null || level.isClientSide) {
			return;
		}

		int pushed = 0;
		int status;

		if (outputRate <= 0) {
			status = STATUS_OFF;
		} else {
			final TileEntity neighbour = level.getBlockEntity(getBlockPos().relative(outputFace));
			if (neighbour == null) {
				status = STATUS_NO_BLOCK;
			} else {
				final IEnergyStorage storage =
					neighbour.getCapability(CapabilityEnergy.ENERGY, outputFace.getOpposite()).orElse(null);
				if (storage == null || !storage.canReceive()) {
					status = STATUS_NO_CAPABILITY;
				} else {
					pushed = storage.receiveEnergy(outputRate, false);
					// Accepting zero almost always means the buffer is already full, which is very
					// different from "nothing is connected" - worth saying out loud in the GUI.
					status = pushed > 0 ? STATUS_DELIVERING : STATUS_RECEIVER_FULL;
				}
			}
		}

		if (status != lastStatus) {
			DebugLog.log("ENERGY", "creative source at {} -> {}: status {} (rate={} pushed={})",
				getBlockPos(), outputFace, describeStatus(status), outputRate, pushed);
		}

		// Throttle the readout sync; the GUI does not need per-tick precision
		if (pushed != lastPushed || status != lastStatus) {
			lastPushed = pushed;
			lastStatus = status;
			needsSync = true;
		}
		if (needsSync && --syncCooldown <= 0) {
			syncCooldown = 10;
			needsSync = false;
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	public int getLastStatus() {
		return lastStatus;
	}

	public static String describeStatus(final int status) {
		switch (status) {
			case STATUS_OFF:            return "output is off";
			case STATUS_NO_BLOCK:       return "no block on that face";
			case STATUS_NO_CAPABILITY:  return "block there accepts no FE";
			case STATUS_RECEIVER_FULL:  return "receiver is full";
			case STATUS_DELIVERING:     return "delivering";
			default:                    return "unknown";
		}
	}

	// ===== Energy capability (so cables can pull as well as being pushed to) =====

	private LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(this::createEnergyStorage);

	private IEnergyStorage createEnergyStorage() {
		return new IEnergyStorage() {
			@Override
			public int receiveEnergy(final int maxReceive, final boolean simulate) {
				return 0;
			}

			@Override
			public int extractEnergy(final int maxExtract, final boolean simulate) {
				return Math.min(maxExtract, outputRate);
			}

			@Override
			public int getEnergyStored() {
				return Integer.MAX_VALUE;
			}

			@Override
			public int getMaxEnergyStored() {
				return Integer.MAX_VALUE;
			}

			@Override
			public boolean canExtract() {
				return outputRate > 0;
			}

			@Override
			public boolean canReceive() {
				return false;
			}
		};
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
		// Only the configured face provides power
		if (cap == CapabilityEnergy.ENERGY && (side == null || side == outputFace)) {
			return energyHandler.cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
	protected void invalidateCaps() {
		super.invalidateCaps();
		energyHandler.invalidate();
	}

	// ===== GUI =====

	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent("block.warpdrive.creative_energy");
	}

	@Nullable
	@Override
	public Container createMenu(final int windowId, @Nonnull final PlayerInventory inventory,
	                            @Nonnull final PlayerEntity player) {
		return new CreativeEnergyContainer(windowId, inventory, getBlockPos());
	}

	// ===== NBT =====

	@Override
	public void load(@Nonnull final BlockState state, @Nonnull final CompoundNBT nbt) {
		super.load(state, nbt);
		outputRate = Math.max(0, Math.min(MAX_RATE, nbt.getInt("OutputRate")));
		final int face = nbt.getInt("OutputFace");
		outputFace = face >= 0 && face < Direction.values().length ? Direction.from3DDataValue(face) : Direction.DOWN;
		lastPushed = nbt.getInt("LastPushed");
		lastStatus = nbt.getInt("LastStatus");
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT nbt) {
		super.save(nbt);
		nbt.putInt("OutputRate", outputRate);
		nbt.putInt("OutputFace", outputFace.get3DDataValue());
		nbt.putInt("LastPushed", lastPushed);
		nbt.putInt("LastStatus", lastStatus);
		return nbt;
	}

	// Same lesson as the Ship Core: without these the client never sees configuration changes
	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		return save(new CompoundNBT());
	}

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(worldPosition, 1, save(new CompoundNBT()));
	}

	@Override
	public void onDataPacket(final NetworkManager net, final SUpdateTileEntityPacket pkt) {
		load(getBlockState(), pkt.getTag());
	}

	@Override
	public void handleUpdateTag(final BlockState state, final CompoundNBT nbt) {
		load(state, nbt);
	}

	public String buildDebugReport() {
		final StringBuilder report = new StringBuilder();
		report.append("side       : ").append(level == null ? "no world" : (level.isClientSide ? "CLIENT" : "SERVER")).append('\n');
		report.append("pos        : ").append(getBlockPos()).append('\n');
		report.append("outputRate : ").append(outputRate).append(" FE/t\n");
		report.append("outputFace : ").append(outputFace).append('\n');
		report.append("lastPushed : ").append(lastPushed).append(" FE/t\n");
		if (level != null) {
			final BlockPos target = getBlockPos().relative(outputFace);
			report.append("target     : ").append(target).append(" = ")
				.append(level.getBlockState(target).getBlock().getRegistryName()).append('\n');
		}
		return report.toString();
	}
}
