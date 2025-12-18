package cr0s.warpdrive.api.computer;

public interface IMachine extends IInterfaced {
	
	String[] name(final Object[] arguments);
	
	boolean getIsEnabled();
	
	Object[] enable(final Object[] arguments);
	
	Object[] getAssemblyStatus();
	
	boolean isAssemblyValid();
}
