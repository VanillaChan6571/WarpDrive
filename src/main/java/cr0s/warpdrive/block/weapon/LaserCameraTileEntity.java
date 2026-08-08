package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.data.CameraType;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.VideoChannelRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nonnull;

/** Laser cannon with the persisted video channel used by camera monitors and tuning tools. */
public class LaserCameraTileEntity extends LaserTileEntity implements IVideoChannel {

	private int videoChannel = -1;

	public LaserCameraTileEntity() {
		super(Registration.LASER_CAMERA_TILE.get());
	}

	@Override
	public int getVideoChannel() {
		return videoChannel;
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && level.isClientSide) {
			VideoChannelRegistry.update(level, getBlockPos(), videoChannel, CameraType.LASER);
		}
	}

	@Override
	public void setVideoChannel(final int requestedChannel) {
		if (!IVideoChannel.isValid(requestedChannel) || videoChannel == requestedChannel) return;
		videoChannel = requestedChannel;
		setChanged();
		if (level != null) {
			final BlockState blockState = getBlockState();
			level.sendBlockUpdated(getBlockPos(), blockState, blockState, 3);
		}
	}

	public Object[] setOrGetVideoChannel(final Integer requestedChannel) {
		if (requestedChannel != null) setVideoChannel(requestedChannel);
		return new Object[]{ videoChannel };
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		final int legacyAndCurrentChannel = tagCompound.getInt("cameraFrequency")
			+ tagCompound.getInt(IVideoChannel.VIDEO_CHANNEL_TAG);
		videoChannel = IVideoChannel.isValid(legacyAndCurrentChannel) ? legacyAndCurrentChannel : -1;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		if (IVideoChannel.isValid(videoChannel)) {
			tagCompound.putInt(IVideoChannel.VIDEO_CHANNEL_TAG, videoChannel);
		}
		return tagCompound;
	}

	@Override
	public void setRemoved() {
		if (level != null) VideoChannelRegistry.remove(level, getBlockPos());
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		if (level != null) VideoChannelRegistry.remove(level, getBlockPos());
		super.onChunkUnloaded();
	}
}
