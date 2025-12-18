package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

public class AcceleratorControlParameter {
	
	// persistent properties
	public int controlChannel;  // final
	public boolean isEnabled = true;
	public double threshold = WarpDriveConfig.ACCELERATOR_THRESHOLD_DEFAULT;
	public String description = "-";
	
	public AcceleratorControlParameter(final int controlChannel) {
		this.controlChannel = controlChannel;
	}
	
	public AcceleratorControlParameter(@Nonnull final NBTTagCompound tagCompound) {
		readFromNBT(tagCompound);
	}
	
	private void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		controlChannel = tagCompound.getInteger(IControlChannel.CONTROL_CHANNEL_TAG);
		isEnabled = !tagCompound.hasKey("isEnabled") || tagCompound.getBoolean("isEnabled");
		threshold = tagCompound.getDouble("threshold");
		description = tagCompound.getString("description");
	}
	
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setInteger(IControlChannel.CONTROL_CHANNEL_TAG, controlChannel);
		tagCompound.setBoolean("isEnabled", isEnabled);
		tagCompound.setDouble("threshold", threshold);
		tagCompound.setString("description", description);
		return tagCompound;
	}
	
	// Hash based collections need a stable hashcode, so we use a unique id instead
	@Override
	public int hashCode() {
		return controlChannel;
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof AcceleratorControlParameter) {
			final AcceleratorControlParameter acceleratorControlParameter = (AcceleratorControlParameter) object;
			return controlChannel == acceleratorControlParameter.controlChannel
			    && isEnabled == acceleratorControlParameter.isEnabled
			    && threshold == acceleratorControlParameter.threshold
			    && description.equals(acceleratorControlParameter.description);
		}
		
		return false;
	}
	
	
	@Override
	public String toString() {
		return String.format("%s/%d isEnabled %s threshold %.3f '%s'",
			getClass().getSimpleName(),
			controlChannel,
			isEnabled,
			threshold,
			description);
	}
}
