package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.api.computer.IEnanReactorController;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Collections;

import net.minecraftforge.fml.common.Optional;

public class TileEntityEnanReactorController extends TileEntityAbstractEnergyCoreOrController implements IEnanReactorController {
	
	// persistent properties
	// (none)
	
	// computed properties
	// (none)
	
	// @TODO implement reactor controller
	private WeakReference<TileEntityEnanReactorCore> tileEntityEnanReactorCoreWeakReference = null;
	
	public TileEntityEnanReactorController() {
		super();
		
		peripheralName = "warpdriveEnanReactorController";
		addMethods(new String[] {
				"getInstabilities",
				"instabilityTarget",
				"outputMode",
				"stabilizerEnergy",
				"state"
		});
		CC_scripts = Collections.singletonList("startup");
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { false, "No energy consumption" };
	}
	
	@Override
	public Object[] getLocalPosition() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.getLocalPosition();
	}
	
	@Override
	public Object[] getAssemblyStatus() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { false, "No core detected" };
		}
		return tileEntityEnanReactorCore.getAssemblyStatus();
	}
	
	@Override
	public String[] name(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return super.name(null); // return current local values
		}
		return tileEntityEnanReactorCore.name(arguments);
	}
	
	@Override
	public Double[] getInstabilities() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Double[] { -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D,
			                      -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D, -1.0D };
		}
		return tileEntityEnanReactorCore.getInstabilities();
	}
	
	@Override
	public Double[] instabilityTarget(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Double[] { -1.0D };
		}
		return tileEntityEnanReactorCore.instabilityTarget(arguments);
	}
	
	@Override
	public Object[] outputMode(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { "???", -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.outputMode(arguments);
	}
	
	@Override
	public Object[] stabilizerEnergy(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.stabilizerEnergy(arguments);
	}
	
	@Override
	public Object[] state() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return new Object[] { -1, "Core not found" };
		}
		return tileEntityEnanReactorCore.state();
	}
	
	@Override
	public Object[] energyDisplayUnits(final Object[] arguments) {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.energyDisplayUnits(arguments);
	}
	
	@Override
	public Object[] getEnergyStatus() {
		final TileEntityEnanReactorCore tileEntityEnanReactorCore = tileEntityEnanReactorCoreWeakReference == null ? null : tileEntityEnanReactorCoreWeakReference.get();
		if (tileEntityEnanReactorCore == null) {
			return null;
		}
		return tileEntityEnanReactorCore.getEnergyStatus();
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getInstabilities(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getInstabilities();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] instabilityTarget(final Context context, final Arguments arguments) {
		return instabilityTarget(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] outputMode(final Context context, final Arguments arguments) {
		return outputMode(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] stabilizerEnergy(final Context context, final Arguments arguments) {
		return stabilizerEnergy(OC_convertArgumentsAndLogCall(context, arguments));
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
		case "getInstabilities":
			return getInstabilities();
			
		case "instabilityTarget":
			return instabilityTarget(arguments);
			
		case "outputMode":
			return outputMode(arguments);
			
		case "stabilizerEnergy":
			return stabilizerEnergy(arguments);
			
		case "state":
			return state();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
