package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Mod network channel for server-authoritative GUI actions and lightweight client HUD state.
 */
public final class WarpDriveNetwork {

	private static final String PROTOCOL_VERSION = "5";

	public static SimpleChannel CHANNEL;

	private WarpDriveNetwork() {
	}

	public static void init() {
		CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(WarpDrive.MODID, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals);

		int id = 0;
		CHANNEL.registerMessage(id++,
			SetCreativeEnergyPacket.class,
			SetCreativeEnergyPacket::encode,
			SetCreativeEnergyPacket::decode,
			SetCreativeEnergyPacket::handle);
		CHANNEL.registerMessage(id++,
			ShipControllerActionPacket.class,
			ShipControllerActionPacket::encode,
			ShipControllerActionPacket::decode,
			ShipControllerActionPacket::handle);
		CHANNEL.registerMessage(id++,
			ShipCountdownPacket.class,
			ShipCountdownPacket::encode,
			ShipCountdownPacket::decode,
			ShipCountdownPacket::handle);
		CHANNEL.registerMessage(id++,
			BeamEffectPacket.class,
			BeamEffectPacket::encode,
			BeamEffectPacket::decode,
			BeamEffectPacket::handle);
		CHANNEL.registerMessage(id++,
			CameraLaserFirePacket.class,
			CameraLaserFirePacket::encode,
			CameraLaserFirePacket::decode,
			CameraLaserFirePacket::handle);
		CHANNEL.registerMessage(id++,
			CloakPacket.class,
			CloakPacket::encode,
			CloakPacket::decode,
			CloakPacket::handle);

		WarpDrive.logger.info("Network channel registered ({} message types)", id);
	}
}
