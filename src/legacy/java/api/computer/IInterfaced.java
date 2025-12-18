package cr0s.warpdrive.api.computer;

public interface IInterfaced {
	
	// return true if it supports the interface
	boolean isInterfaceEnabled();
	
	// return true if it supports the interface
	Object[] isInterfaced();
	
	// return local block coordinates
	Object[] getLocalPosition();
	
	// return tier index and name
	Object[] getTier();
	
	// return upgradability and status
	Object[] getUpgrades();
	
	// return the mod version
	Integer[] getVersion();
}
