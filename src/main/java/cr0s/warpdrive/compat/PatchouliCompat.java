package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional, reflection-only bridge to Patchouli's 1.16 API. */
public final class PatchouliCompat {

	public static final String MOD_ID = "patchouli";
	public static final ResourceLocation MANUAL_ID =
		new ResourceLocation(WarpDrive.MODID, "warpdrive_manual");

	private static Method openBookGui;
	private static Object patchouliApi;
	private static boolean resolutionAttempted;

	private PatchouliCompat() {
	}

	public static boolean isLoaded() {
		return ModList.get().isLoaded(MOD_ID);
	}

	/**
	 * Opens the manual through Patchouli without making Patchouli a hard class-loading dependency.
	 * The 1.16 API sends the GUI-opening packet from the logical server.
	 */
	public static boolean openManual(final ServerPlayerEntity player) {
		if (!isLoaded() || !resolveApi()) return false;
		try {
			openBookGui.invoke(patchouliApi, player, MANUAL_ID);
			return true;
		} catch (final IllegalAccessException | InvocationTargetException exception) {
			WarpDrive.logger.error("Patchouli failed to open manual {}", MANUAL_ID, exception);
			return false;
		}
	}

	private static synchronized boolean resolveApi() {
		if (resolutionAttempted) return openBookGui != null;
		resolutionAttempted = true;
		try {
			final Class<?> patchouliApiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
			final Class<?> patchouliApiInterface =
				Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI");
			patchouliApi = patchouliApiClass.getMethod("get").invoke(null);
			openBookGui = patchouliApiInterface.getMethod("openBookGUI",
				ServerPlayerEntity.class, ResourceLocation.class);
			return true;
		} catch (final ReflectiveOperationException | LinkageError exception) {
			WarpDrive.logger.error("Patchouli is loaded but its 1.16 book API is unavailable", exception);
			return false;
		}
	}
}
