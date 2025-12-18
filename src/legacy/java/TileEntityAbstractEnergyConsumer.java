package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.computer.IEnergyConsumer;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.common.Optional;

public abstract class TileEntityAbstractEnergyConsumer extends TileEntityAbstractEnergy implements IEnergyConsumer {
	
	// persistent properties
	// (none)
	
	// computed properties
	// (none)
	
	public TileEntityAbstractEnergyConsumer() {
		super();
		
		addMethods(new String[] {
				"getEnergyRequired",
				});
	}
	
	// Common OC/CC methods
	@Override
	public abstract Object[] getEnergyRequired();
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getEnergyRequired(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getEnergyRequired();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getEnergyRequired":
			return getEnergyRequired();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s",
		                     getClass().getSimpleName(),
		                     name,
		                     Commons.format(world, pos));
	}
}
