package cr0s.warpdrive.api.computer;

public interface ITransporterBeacon extends IEnergyConsumer {
	
	Boolean[] isActive(final Object[] arguments);
	
	boolean isActive();
	
	void energizeDone();
}
