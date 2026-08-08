package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Persisted and synchronized video-channel receiver. */
public class MonitorTileEntity extends TileEntity implements IVideoChannel {

	private int videoChannel = -1;

	public MonitorTileEntity() {
		super(Registration.MONITOR_TILE.get());
	}

	@Override public int getVideoChannel() { return videoChannel; }

	@Override
	public void setVideoChannel(final int requestedChannel) {
		if (!IVideoChannel.isValid(requestedChannel) || videoChannel == requestedChannel) return;
		videoChannel = requestedChannel;
		setChanged();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	public Object[] setOrGetVideoChannel(@Nullable final Integer requestedChannel) {
		if (requestedChannel != null) setVideoChannel(requestedChannel);
		return new Object[]{ videoChannel };
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		final int storedChannel = tagCompound.getInt("frequency")
		                        + tagCompound.getInt(IVideoChannel.VIDEO_CHANNEL_TAG);
		videoChannel = IVideoChannel.isValid(storedChannel) ? storedChannel : -1;
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

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getBlockPos(), 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager networkManager,
	                         final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}
}
