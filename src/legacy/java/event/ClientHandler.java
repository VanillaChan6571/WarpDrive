package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

public class ClientHandler {
	
	@SubscribeEvent
	public void onClientTick(final ClientTickEvent event) {
		if (event.side != Side.CLIENT || event.phase != Phase.END) {
			return;
		}
		
		WarpDrive.cloaks.onClientTick();
	}
}
