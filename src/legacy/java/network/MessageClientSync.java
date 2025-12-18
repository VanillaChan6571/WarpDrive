package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageClientSync implements IMessage, IMessageHandler<MessageClientSync, IMessage> {
	
	private NBTTagCompound tagCompound;
	
	@SuppressWarnings("unused")
	public MessageClientSync() {
		// required on receiving side
	}
	
	public MessageClientSync(final EntityPlayerMP entityPlayerMP, final CelestialObject celestialObject) {
		tagCompound = new NBTTagCompound();
		tagCompound.setTag("celestialObjects"     , CelestialObjectManager.writeClientSync(entityPlayerMP, celestialObject));
		tagCompound.setTag("items_breathingHelmet", Dictionary.writeItemsToNBT(Dictionary.ITEMS_BREATHING_HELMET));
		tagCompound.setTag("items_flyInSpace"     , Dictionary.writeItemsToNBT(Dictionary.ITEMS_FLYINSPACE));
		tagCompound.setTag("items_noFallDamage"   , Dictionary.writeItemsToNBT(Dictionary.ITEMS_NOFALLDAMAGE));
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		tagCompound = ByteBufUtils.readTag(buffer);
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		ByteBufUtils.writeTag(buffer, tagCompound);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageClientSync messageClientSync, final MessageContext context) {
		// skip in case player just logged in
		// if (Minecraft.getMinecraft().world == null) {
		// 	WarpDrive.logger.error("WorldObj is null, ignoring client synchronization packet");
		// 	return null;
		// }
		
		if (WarpDriveConfig.LOGGING_CLIENT_SYNCHRONIZATION) {
			WarpDrive.logger.info(String.format("Received client synchronization packet: %s",
			                                    messageClientSync.tagCompound));
		}
		
		try {
			CelestialObjectManager.readClientSync(messageClientSync.tagCompound.getTagList("celestialObjects", NBT.TAG_COMPOUND));
			Dictionary.ITEMS_BREATHING_HELMET = Dictionary.readItemsFromNBT(messageClientSync.tagCompound.getTagList("items_breathingHelmet", NBT.TAG_STRING));
			Dictionary.ITEMS_FLYINSPACE       = Dictionary.readItemsFromNBT(messageClientSync.tagCompound.getTagList("items_flyInSpace"     , NBT.TAG_STRING));
			Dictionary.ITEMS_NOFALLDAMAGE     = Dictionary.readItemsFromNBT(messageClientSync.tagCompound.getTagList("items_noFallDamage"   , NBT.TAG_STRING));
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Fails to parse client synchronization packet %s",
			                                     messageClientSync.tagCompound ));
		}
		
		return new MessageClientValidation();
	}
}
