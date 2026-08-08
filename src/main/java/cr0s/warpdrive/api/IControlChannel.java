package cr0s.warpdrive.api;

/**
 * A machine that can be tuned to a control channel, used to address it remotely.
 *
 * Ported verbatim from 1.12.2. Note the bounds differ from {@link IVideoChannel}: the minimum is
 * inclusive and the maximum exclusive, which is the original's asymmetry, not a transcription slip.
 */
public interface IControlChannel {

	int CONTROL_CHANNEL_MIN = 0;
	int CONTROL_CHANNEL_MAX = 0xFFFFFFF;    // 268435455
	String CONTROL_CHANNEL_TAG = "controlChannel";

	static boolean isValid(final int controlChannel) {
		return controlChannel <  CONTROL_CHANNEL_MAX
		    && controlChannel >= CONTROL_CHANNEL_MIN;
	}

	/** @return the current control channel, or -1 when unset or invalid. */
	int getControlChannel();

	void setControlChannel(final int controlChannel);
}
