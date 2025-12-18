package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.render.EntityFXBeam;
import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageBeamEffect implements IMessage, IMessageHandler<MessageBeamEffect, IMessage> {
	
	private Vector3 source;
	private Vector3 target;
	private float red;
	private float green;
	private float blue;
	private int age;

	@SuppressWarnings("unused")
	public MessageBeamEffect() {
		// required on receiving side
	}
	
	public MessageBeamEffect(final Vector3 source, final Vector3 target, final float red, final float green, final float blue, final int age) {
		this.source = source;
		this.target = target;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.age = age;
	}
	
	@Override
	public void fromBytes(final ByteBuf buffer) {
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		source = new Vector3(x, y, z);

		x = buffer.readDouble();
		y = buffer.readDouble();
		z = buffer.readDouble();
		target = new Vector3(x, y, z);

		red = buffer.readFloat();
		green = buffer.readFloat();
		blue = buffer.readFloat();
		age = buffer.readShort();
	}
	
	@Override
	public void toBytes(final ByteBuf buffer) {
		buffer.writeDouble(source.x);
		buffer.writeDouble(source.y);
		buffer.writeDouble(source.z);
		buffer.writeDouble(target.x);
		buffer.writeDouble(target.y);
		buffer.writeDouble(target.z);
		buffer.writeFloat(red);
		buffer.writeFloat(green);
		buffer.writeFloat(blue);
		buffer.writeShort(Math.min(32767, age));
	}
	
	@SideOnly(Side.CLIENT)
	private void handle(final World world) {
		FMLClientHandler.instance().getClient().effectRenderer.addEffect(new EntityFXBeam(world, source.clone(), target.clone(), red, green, blue, age));
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageBeamEffect beamEffectMessage, final MessageContext context) {
		// skip in case player just logged in
		if (Minecraft.getMinecraft().world == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring beam packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info(String.format("Received beam packet from %s to %s as RGB %.3f %.3f %.3f age %d",
			                                    beamEffectMessage.source, beamEffectMessage.target,
			                                    beamEffectMessage.red, beamEffectMessage.green, beamEffectMessage.blue,
			                                    beamEffectMessage.age));
		}
		
        beamEffectMessage.handle(Minecraft.getMinecraft().world);
        
		return null;	// no response
	}
}
