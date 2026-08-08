package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import javax.annotation.Nonnull;

/** Persisted enable/control-channel state shared by accelerator control points and injectors. */
public class AcceleratorControlPointTileEntity extends TileEntity
	implements ITickableTileEntity, IControlChannel {

	private static final String TAG_ENABLED = "enabled";
	private int controlChannel = -1;
	private boolean enabled = true;
	private boolean coreActive;

	public AcceleratorControlPointTileEntity() {
		this(Registration.ACCELERATOR_CONTROL_POINT_TILE.get());
	}

	protected AcceleratorControlPointTileEntity(final TileEntityType<?> type) {
		super(type);
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		final BlockState blockState = getBlockState();
		if (blockState.hasProperty(AcceleratorComponentBlock.ACTIVE)) {
			final boolean active = coreActive && enabled && controlChannel != -1;
			if (blockState.getValue(AcceleratorComponentBlock.ACTIVE) != active) {
				level.setBlock(worldPosition,
					blockState.setValue(AcceleratorComponentBlock.ACTIVE, active), 3);
			}
		}
	}

	@Override
	public int getControlChannel() {
		return controlChannel;
	}

	@Override
	public void setControlChannel(final int controlChannel) {
		if (this.controlChannel == controlChannel) return;
		this.controlChannel = controlChannel;
		setChanged();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		setChanged();
	}

	public void setCoreActive(final boolean coreActive) {
		this.coreActive = coreActive;
	}

	public Object[] state() {
		return new Object[]{ enabled ? "enabled" : "disabled", enabled, controlChannel };
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		controlChannel = tag.contains(CONTROL_CHANNEL_TAG) ? tag.getInt(CONTROL_CHANNEL_TAG) : -1;
		enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putInt(CONTROL_CHANNEL_TAG, controlChannel);
		tag.putBoolean(TAG_ENABLED, enabled);
		return tag;
	}
}
