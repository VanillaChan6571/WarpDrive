package cr0s.warpdrive.network;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageVideoChannel implements IMessage, IMessageHandler<MessageVideoChannel, IMessage> {
	private BlockPos blockPos;
	private int videoChannel;

	@SuppressWarnings("unused")
	public MessageVideoChannel() {
		// required on receiving side
	}
	
	public MessageVideoChannel(final BlockPos blockPos, final int videoChannel) {
		this.blockPos = blockPos;
		this.videoChannel = videoChannel;
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		blockPos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
		videoChannel = buffer.readInt();
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeInt(blockPos.getX());
		buffer.writeInt(blockPos.getY());
		buffer.writeInt(blockPos.getZ());
		buffer.writeInt(videoChannel);
	}
	
	@SideOnly(Side.CLIENT)
	private void handle(final World world) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity != null) {
			if (tileEntity instanceof IVideoChannel) {
				((IVideoChannel) tileEntity).setVideoChannel(videoChannel);
			} else {
				WarpDrive.logger.error(String.format("Received video channel packet: invalid tile entity %s",
				                                     Commons.format(world, blockPos)));
			}
		} else {
			WarpDrive.logger.error(String.format("Received video channel packet: no tile entity %s",
			                                     Commons.format(world, blockPos)));
		}
 	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageVideoChannel videoChannelMessage, final MessageContext context) {
		// skip in case player just logged in
		final World world = Minecraft.getMinecraft().world;
		if (world == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring video channel packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
			WarpDrive.logger.info(String.format("Received video channel packet %s videoChannel %d",
			                                    Commons.format(world, videoChannelMessage.blockPos), videoChannelMessage.videoChannel));
		}
		
		videoChannelMessage.handle(world);
		
		return null;	// no response
	}
}
