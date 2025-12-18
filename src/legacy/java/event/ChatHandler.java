package cr0s.warpdrive.event;

import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.GlobalRegionManager;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatHandler {
	
	@SubscribeEvent
	public void onServerChat(final ServerChatEvent event) {
		final boolean isCancelled = GlobalRegionManager.onChatReceived(event.getPlayer(), event.getMessage());
		if ( isCancelled
		  && WarpDriveConfig.VIRTUAL_ASSISTANT_HIDE_COMMANDS_IN_CHAT ) {
			event.setCanceled(true);
		}
	}
}
