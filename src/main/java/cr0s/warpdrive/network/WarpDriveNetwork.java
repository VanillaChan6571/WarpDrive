package cr0s.warpdrive.network;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Mod network channel. Currently only carries GUI -> server configuration changes.
 */
public final class WarpDriveNetwork {

	private static final String PROTOCOL_VERSION = "1";

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

		WarpDrive.logger.info("Network channel registered ({} message types)", id);
	}
}
