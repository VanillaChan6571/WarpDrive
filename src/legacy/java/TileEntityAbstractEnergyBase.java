package cr0s.warpdrive.block;

import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.IEnergyBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.fml.common.Optional;


public abstract class TileEntityAbstractEnergyBase extends TileEntityAbstractMachine implements IEnergyBase {
	
	// persistent properties
	private String energy_displayUnits = null;
	
	public TileEntityAbstractEnergyBase() {
		super();
		
		addMethods(new String[] {
				"energyDisplayUnits",
				"getEnergyStatus"
		});
	}
	
	// Forge overrides
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		if (tagCompound.hasKey(EnergyWrapper.TAG_DISPLAY_UNITS)) {
			energy_displayUnits = tagCompound.getString(EnergyWrapper.TAG_DISPLAY_UNITS);
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (energy_displayUnits != null) {
			tagCompound.setString(EnergyWrapper.TAG_DISPLAY_UNITS, energy_displayUnits);
		}
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		return tagCompound;
	}
	
	public String energy_getDisplayUnits() {
		return energy_displayUnits == null ? WarpDriveConfig.ENERGY_DISPLAY_UNITS : energy_displayUnits;
	}
	
	// Methods to override
	protected abstract WarpDriveText getEnergyStatusText();
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textEnergyStatus = getEnergyStatusText();
		if (textEnergyStatus.getUnformattedText().isEmpty()) {
			return super.getStatus();
		} else {
			return super.getStatus().append(textEnergyStatus);
		}
	}
	
	// Common OC/CC methods
	@Override
	public Object[] energyDisplayUnits(final Object[] arguments) {
		if (arguments.length == 1) {
			final Object value = arguments[0];
			if (!(value instanceof String)
			    || (((String) value).isEmpty())
			    || value.equals("-")) {
				energy_displayUnits = null;
			} else {
				energy_displayUnits = (String) value;
			}
		}
		return new Object[] {
				energy_getDisplayUnits(),
				energy_displayUnits == null };
	}
	
	@Override
	public abstract Object[] getEnergyStatus();
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] energyDisplayUnits(final Context context, final Arguments arguments) {
		return energyDisplayUnits(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getEnergyStatus(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getEnergyStatus();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "energyDisplayUnits":
			return energyDisplayUnits(arguments);
		
		case "getEnergyStatus":
			return getEnergyStatus();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}