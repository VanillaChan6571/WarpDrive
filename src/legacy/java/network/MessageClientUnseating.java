package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;

import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import io.netty.buffer.ByteBuf;

public class MessageClientUnseating implements IMessage, IMessageHandler<MessageClientUnseating, IMessage> {
		
	@SuppressWarnings("unused")
	public MessageClientUnseating() {
		// required on receiving side
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		// no operation
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		// no operation
	}
	
	private void handle(final EntityPlayerMP entityPlayerMP) {
		entityPlayerMP.getServerWorld().addScheduledTask(entityPlayerMP::dismountRidingEntity);
	}
	
	@Override
	public IMessage onMessage(final MessageClientUnseating targetingMessage, final MessageContext context) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("Received client unseating packet from %s",
			                                    context.getServerHandler().player.getName() ));
		}
		
		targetingMessage.handle(context.getServerHandler().player);
        
		return null;	// no response
	}
}
