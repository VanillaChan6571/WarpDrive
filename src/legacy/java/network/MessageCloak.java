package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CloakedArea;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageCloak implements IMessage, IMessageHandler<MessageCloak, IMessage> {
	
	private int coreX;
	private int coreY;
	private int coreZ;
	private int minX;
	private int minY;
	private int minZ;
	private int maxX;
	private int maxY;
	private int maxZ;
	private boolean isFullyTransparent;
	private boolean isUncloaking;

	@SuppressWarnings("unused")
	public MessageCloak() {
		// required on receiving side
	}
	
	public MessageCloak(final CloakedArea area, final boolean isUncloaking) {
		this.coreX = area.blockPosCore.getX();
		this.coreY = area.blockPosCore.getY();
		this.coreZ = area.blockPosCore.getZ();
		this.minX = area.minX;
		this.minY = area.minY;
		this.minZ = area.minZ;
		this.maxX = area.maxX;
		this.maxY = area.maxY;
		this.maxZ = area.maxZ;
		this.isFullyTransparent = area.isFullyTransparent;
		this.isUncloaking = isUncloaking;
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		coreX = buffer.readInt();
		coreY = buffer.readInt();
		coreZ = buffer.readInt();
		minX = buffer.readInt();
		minY = buffer.readInt();
		minZ = buffer.readInt();
		maxX = buffer.readInt();
		maxY = buffer.readInt();
		maxZ = buffer.readInt();
		isFullyTransparent = buffer.readBoolean();
		isUncloaking = buffer.readBoolean();
	}

	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeInt(coreX);
		buffer.writeInt(coreY);
		buffer.writeInt(coreZ);
		buffer.writeInt(minX);
		buffer.writeInt(minY);
		buffer.writeInt(minZ);
		buffer.writeInt(maxX);
		buffer.writeInt(maxY);
		buffer.writeInt(maxZ);
		buffer.writeBoolean(isFullyTransparent);
		buffer.writeBoolean(isUncloaking);
	}
	
	@SideOnly(Side.CLIENT)
	private void handle(final EntityPlayerSP player) {
		if (isUncloaking) {
			// reveal the area
			WarpDrive.cloaks.removeCloakedArea(player.world.provider.getDimension(), new BlockPos(coreX, coreY, coreZ));
		} else { 
			// Hide the area
			WarpDrive.cloaks.updateCloakedArea(player.world, new BlockPos(coreX, coreY, coreZ), isFullyTransparent,
			                                   minX, minY, minZ, maxX, maxY, maxZ);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageCloak cloakMessage, final MessageContext context) {
		// skip in case player just logged in
		if (Minecraft.getMinecraft().world == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring cloak packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("Received cloak packet: %s area (%d %d %d) -> (%d %d %d) tier %d",
			                                    ((cloakMessage.isUncloaking) ? "UNCLOAKING" : "cloaking"),
			                                    cloakMessage.minX, cloakMessage.minY, cloakMessage.minZ,
			                                    cloakMessage.maxX, cloakMessage.maxY, cloakMessage.maxZ, cloakMessage.isFullyTransparent ? 2 : 1));
		}
		
		final EntityPlayerSP player = Minecraft.getMinecraft().player;
		assert player != null;
		if ( cloakMessage.minX <= player.posX && (cloakMessage.maxX + 1) > player.posX
		  && cloakMessage.minY <= player.posY && (cloakMessage.maxY + 1) > player.posY
		  && cloakMessage.minZ <= player.posZ && (cloakMessage.maxZ + 1) > player.posZ) {
			return null;
		}
		cloakMessage.handle(player);
		
		return null;	// no response
	}
}
