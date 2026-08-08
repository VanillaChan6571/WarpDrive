package cr0s.warpdrive.event;

import cr0s.warpdrive.block.detection.VirtualAssistantTileEntity;
import net.minecraftforge.event.ServerChatEvent;

/** Routes server chat to loaded virtual assistants without requiring the unported global registry. */
public final class VirtualAssistantChatHandler {

	private VirtualAssistantChatHandler() { }

	public static void onServerChat(final ServerChatEvent event) {
		if (VirtualAssistantTileEntity.dispatchChat(event.getPlayer(), event.getMessage())) {
			event.setCanceled(true);
		}
	}
}
