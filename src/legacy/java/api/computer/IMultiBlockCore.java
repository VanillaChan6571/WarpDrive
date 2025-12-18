package cr0s.warpdrive.api.computer;

public interface IMultiBlockCore extends IMultiBlockCoreOrController {
	
	boolean refreshLink(final IMultiBlockCoreOrController multiblockController);
	
	void removeLink(final IMultiBlockCoreOrController multiblockController);
}
