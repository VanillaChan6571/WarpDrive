package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;

public class TileEntityAcceleratorControlPoint extends TileEntityAbstractMachine implements IControlChannel {
	
	// persistent properties
	private int controlChannel = -1;
	
	// computed properties
	private static final int UPDATE_INTERVAL_TICKS = 20;
	private int updateTicks;
	
	public TileEntityAcceleratorControlPoint() {
		super();
		
		peripheralName = "warpdriveAcceleratorControlPoint";
		addMethods(new String[] {
			"state",
			"controlChannel"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = UPDATE_INTERVAL_TICKS;
			updateBlockState(null, BlockProperties.ACTIVE, (controlChannel != -1) && isEnabled);
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
	}
	
	@Override
	public int getControlChannel() {
		return controlChannel;
	}
	
	@Override
	public void setControlChannel(final int controlChannel) {
		if (this.controlChannel != controlChannel) {
			this.controlChannel = controlChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Accelerator control point controlChannel channel set to " + controlChannel);
			}
			// force update through main thread since CC & OC are running outside the main thread
			markDirty();
		}
	}
	
	private WarpDriveText getControlChannelStatus() {
		if (controlChannel == -1) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.control_channel.status_line.undefined");
		} else if ( !IControlChannel.isValid(controlChannel) ) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.control_channel.status_line.invalid",
			                         controlChannel);
		} else {
			return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.control_channel.status_line.valid",
			                         controlChannel);
		}
	}
	
	@Override
	public WarpDriveText getStatus() {
		return super.getStatus()
		       .append(getControlChannelStatus());
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		controlChannel = tagCompound.getInteger(CONTROL_CHANNEL_TAG);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger(CONTROL_CHANNEL_TAG, controlChannel);
		return tagCompound;
	}
	
	// Common OC/CC methods
	public Object[] controlChannel(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final int controlChannelRequested;
			try {
				controlChannelRequested = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on controlChannel(): Integer expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { controlChannel };
			}
			setControlChannel(controlChannelRequested);
		}
		return new Integer[] { controlChannel };
	}
	
	private Object[] state() {
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, controlChannel };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] controlChannel(final Context context, final Arguments arguments) {
		return controlChannel(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return state();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "controlChannel":
			return controlChannel(arguments);
			
		case "state":
			return state();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
