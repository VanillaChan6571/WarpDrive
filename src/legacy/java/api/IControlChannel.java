package cr0s.warpdrive.api;

public interface IControlChannel {
	
	int CONTROL_CHANNEL_MIN = 0;
	int CONTROL_CHANNEL_MAX = 0xFFFFFFF;    // 268435455
	String CONTROL_CHANNEL_TAG = "controlChannel";
	
	static boolean isValid(final int controlChannel) {
		return controlChannel <  CONTROL_CHANNEL_MAX
		    && controlChannel >= CONTROL_CHANNEL_MIN;
	}
	
	// get control channel, return -1 if invalid 
	int getControlChannel();
	
	// set control channel
	void setControlChannel(final int controlChannel);
}
