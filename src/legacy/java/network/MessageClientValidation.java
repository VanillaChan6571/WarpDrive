package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.core.ClassTransformer;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class MessageClientValidation implements IMessage, IMessageHandler<MessageClientValidation, IMessage> {
	
	private String mapClass;
	
	@SuppressWarnings("unused")
	public MessageClientValidation() {
		// required on receiving side
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		final int size = buffer.readInt();
		mapClass = buffer.toString(buffer.readerIndex(), size, Charset.forName("UTF8"));
		buffer.readerIndex(buffer.readerIndex() + size);
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		final String mapClassFull = ClassTransformer.getClientValidation();
		final String mapClassTruncated = mapClassFull.substring(0, Math.min(32700, mapClassFull.length()));
		final byte[] bytesString = mapClassTruncated.getBytes(Charset.forName("UTF8"));
		buffer.writeInt(bytesString.length);
		buffer.writeBytes(bytesString);
	}
	
	private void handle(final String namePlayer) {
		try {
			if (new File("ClientValidation").exists()) {
				final String fileName = String.format("ClientValidation/%s.tsv", namePlayer);
				
				final File file = new File(fileName);
				if (!file.exists()) {
					//noinspection ResultOfMethodCallIgnored
					file.createNewFile();
				}
				
				final PrintWriter printWriter = new PrintWriter(new FileWriter(file));
				printWriter.println(mapClass);
				printWriter.close();
			}
		} catch (final IOException exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error("Exception while saving client validation to disk");
		}
	}
	
	@Override
	public IMessage onMessage(final MessageClientValidation targetingMessage, final MessageContext context) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("Received client validation packet from %s",
			                                    context.getServerHandler().player.getName()));
		}
		
		targetingMessage.handle(context.getServerHandler().player.getName());
        
		return null;	// no response
	}
}
