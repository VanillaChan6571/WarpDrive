package cr0s.warpdrive.api;

/**
 * A machine that can be tuned to a video channel, so a camera and a monitor can be paired.
 *
 * Ported verbatim from 1.12.2. Kept in the public API because third-party machines implemented it
 * to appear on WarpDrive's video network.
 */
public interface IVideoChannel {

	int VIDEO_CHANNEL_MIN = 0;
	int VIDEO_CHANNEL_MAX = 0xFFFFFFF;    // 268435455
	String VIDEO_CHANNEL_TAG = "videoChannel";

	static boolean isValid(final int videoChannel) {
		return videoChannel <= VIDEO_CHANNEL_MAX
		    && videoChannel >  VIDEO_CHANNEL_MIN;
	}

	/** @return the current video channel, or -1 when unset or invalid. */
	int getVideoChannel();

	void setVideoChannel(final int videoChannel);
}
