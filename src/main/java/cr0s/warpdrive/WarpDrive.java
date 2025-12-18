package cr0s.warpdrive;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WarpDrive Reimagined for Minecraft 1.16.5
 *
 * A clean, modern reimplementation of the WarpDrive mod focusing on core features:
 * - Ship structure scanning and warping
 * - Cross-dimension teleportation
 * - Space dimension with asteroids
 * - ComputerCraft integration for ship control
 * - Energy-based warp system
 */
@Mod(WarpDrive.MODID)
public class WarpDrive {

	public static final String MODID = "warpdrive";
	public static final String VERSION = "@version@";
	public static final Logger logger = LogManager.getLogger(MODID);

	public WarpDrive() {
		// Initialize deferred registration system (1.16.5 pattern)
		cr0s.warpdrive.data.Registration.init();

		// Register event handlers
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

		logger.info("WarpDrive Reimagined initialized!");
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		logger.info("WarpDrive common setup complete");
	}
}
