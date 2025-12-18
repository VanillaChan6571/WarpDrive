package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageTargeting implements IMessage, IMessageHandler<MessageTargeting, IMessage> {
	
	private int x;
	private int y;
	private int z;
	private float yaw;
	private float pitch;

	@SuppressWarnings("unused")
	public MessageTargeting() {
		// required on receiving side
	}
	
	public MessageTargeting(final int x, final int y, final int z, final float yaw, final float pitch) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	@Override
	public void fromBytes(final ByteBuf buffer) {
		x = buffer.readInt();
		y = buffer.readInt();
		z = buffer.readInt();
		yaw = buffer.readFloat();
		pitch = buffer.readFloat();
	}

	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeInt(x);
		buffer.writeInt(y);
		buffer.writeInt(z);
		buffer.writeFloat(yaw);
		buffer.writeFloat(pitch);
	}
	
	private void handle(final World world) {
		final TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
		if (tileEntity instanceof TileEntityLaser) {
			final TileEntityLaser laser = (TileEntityLaser) tileEntity;
			laser.initiateBeamEmission(yaw, pitch);
		}
	}
	
	@Override
	public IMessage onMessage(final MessageTargeting targetingMessage, final MessageContext context) {
		if (WarpDriveConfig.LOGGING_TARGETING) {
			WarpDrive.logger.info(String.format("Received target packet: (%d %d %d) yaw: %.1f pitch: %.1f",
			                                    targetingMessage.x, targetingMessage.y, targetingMessage.z,
			                                    targetingMessage.yaw, targetingMessage.pitch));
		}
		
		targetingMessage.handle(context.getServerHandler().player.world);
        
		return null;	// no response
	}
}
