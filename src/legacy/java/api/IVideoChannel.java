package cr0s.warpdrive.api;

public interface IVideoChannel {
	
	int VIDEO_CHANNEL_MIN = 0;
	int VIDEO_CHANNEL_MAX = 0xFFFFFFF;    // 268435455
	String VIDEO_CHANNEL_TAG = "videoChannel";
	
	static boolean isValid(final int videoChannel) {
		return videoChannel <= VIDEO_CHANNEL_MAX
		    && videoChannel >  VIDEO_CHANNEL_MIN;
	}
	
	// get video channel, return -1 if invalid 
	int getVideoChannel();
	
	// set video channel
	void setVideoChannel(final int videoChannel);
}
