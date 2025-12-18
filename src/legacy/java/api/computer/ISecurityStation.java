package cr0s.warpdrive.api.computer;

public interface ISecurityStation extends IMachine {
	
	Object[] getAttachedPlayers();
	Object[] removeAllAttachedPlayers();
	Object[] removeAttachedPlayer(final Object[] arguments);
}
