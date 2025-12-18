package cr0s.warpdrive.api.computer;

public interface IAbstractLaser extends IEnergyBase {
	
	Object[] getEnergyRequired();
	
	Object[] laserMediumDirection();
	
	Object[] laserMediumCount();
}
