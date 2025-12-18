package cr0s.warpdrive.api.computer;

public interface ILift extends IMachine {
	
	Object[] mode(Object[] arguments);
	
	Object[] state();
}
