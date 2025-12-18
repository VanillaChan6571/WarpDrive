package cr0s.warpdrive.api.computer;

public interface IEnanReactorController extends IMultiBlockCoreOrController {
	
	Double[] getInstabilities();
	
	Double[] instabilityTarget(Object[] arguments);
	
	Object[] outputMode(Object[] arguments);
	
	Object[] stabilizerEnergy(Object[] arguments);
	
	Object[] state();
}
