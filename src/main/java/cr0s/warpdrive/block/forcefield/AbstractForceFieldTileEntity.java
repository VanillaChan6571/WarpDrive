package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;

/** Shared frequency, enable state, registry lifecycle and persistence for projectors and relays. */
public abstract class AbstractForceFieldTileEntity extends AbstractEnergyTileEntity
	implements ITickableTileEntity, IBeamFrequency {

	private static final String TAG_ENABLED = "enabled";
	private static final String TAG_CONNECTED = "isConnected";
	private int beamFrequency = -1;
	private boolean connected;
	private boolean enabled = true;
	private int registryTicks;

	protected AbstractForceFieldTileEntity(final TileEntityType<?> tileEntityType) {
		super(tileEntityType);
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || --registryTicks > 0) {
			return;
		}
		registryTicks = 20;
		final boolean nextConnected = IBeamFrequency.isValid(beamFrequency);
		if (connected != nextConnected) {
			connected = nextConnected;
			setChanged();
		}
		ForceFieldRegistry.update(this);
	}

	@Override
	public int getBeamFrequency() {
		return beamFrequency;
	}

	@Override
	public void setBeamFrequency(final int requestedFrequency) {
		if (!IBeamFrequency.isValid(requestedFrequency) || beamFrequency == requestedFrequency) {
			return;
		}
		ForceFieldRegistry.remove(this);
		beamFrequency = requestedFrequency;
		connected = true;
		registryTicks = 0;
		setChanged();
		ForceFieldRegistry.update(this);
		onConfigurationChanged();
	}

	protected void onConfigurationChanged() {
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Object[] enable(final Boolean requested) {
		if (requested != null && enabled != requested) {
			enabled = requested;
			setChanged();
			onConfigurationChanged();
		}
		return new Object[]{ enabled };
	}

	public Object[] setOrGetBeamFrequency(final Integer requested) {
		if (requested != null) {
			setBeamFrequency(requested);
		}
		return new Object[]{ beamFrequency };
	}

	@Override
	public void setRemoved() {
		ForceFieldRegistry.remove(this);
		super.setRemoved();
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		beamFrequency = tagCompound.contains(IBeamFrequency.BEAM_FREQUENCY_TAG)
			? tagCompound.getInt(IBeamFrequency.BEAM_FREQUENCY_TAG) : -1;
		connected = tagCompound.getBoolean(TAG_CONNECTED) && IBeamFrequency.isValid(beamFrequency);
		enabled = !tagCompound.contains(TAG_ENABLED) || tagCompound.getBoolean(TAG_ENABLED);
		registryTicks = 0;
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putInt(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		tagCompound.putBoolean(TAG_CONNECTED, connected);
		tagCompound.putBoolean(TAG_ENABLED, enabled);
		return tagCompound;
	}
}
