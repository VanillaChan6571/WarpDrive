package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ItemSlotRef;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Optional, reflection-only bridge to the Curios API, so an air tank worn in an accessory slot
 * counts exactly like one carried in the pack.
 *
 * Reflection rather than a compile-time dependency, matching {@link PatchouliCompat}: Curios has no
 * presence in the 1.12.2 source, so this is a new integration rather than a port, and it should not
 * make the mod fail to load when Curios is absent.
 *
 * The reflection stops at the API entry point. What comes back is a Forge
 * {@link IItemHandlerModifiable}, which is always on the classpath, so slot access itself is
 * ordinary typed code.
 */
public final class CuriosCompat {

	public static final String MOD_ID = "curios";

	private static Method getCuriosHelper;
	private static Method getEquippedCurios;
	private static boolean resolutionAttempted;
	private static boolean resolutionFailed;

	private CuriosCompat() {
	}

	public static boolean isLoaded() {
		return ModList.get().isLoaded(MOD_ID);
	}

	/**
	 * Append every Curios slot the player is wearing to {@code slots}.
	 *
	 * Silent no-op when Curios is missing, and after any failure it stops trying - a mismatched
	 * Curios version should cost one logged warning, not a reflective call per player per tick.
	 */
	public static void addWornSlots(final PlayerEntity player, final List<ItemSlotRef> slots) {
		if (!isLoaded() || !resolve()) {
			return;
		}
		try {
			final Object helper = getCuriosHelper.invoke(null);
			// getEquippedCurios returns Forge's LazyOptional, not java.util.Optional
			final Object lazy = getEquippedCurios.invoke(helper, player);
			if (!(lazy instanceof LazyOptional)) {
				return;
			}
			final Object handler = ((LazyOptional<?>) lazy).resolve().orElse(null);
			if (!(handler instanceof IItemHandlerModifiable)) {
				return;
			}
			final IItemHandlerModifiable itemHandler = (IItemHandlerModifiable) handler;
			for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
				slots.add(ItemSlotRef.ofHandler(itemHandler, slot));
			}
		} catch (final IllegalAccessException | InvocationTargetException exception) {
			resolutionFailed = true;
			WarpDrive.logger.error("Curios integration disabled after a call failure", exception);
		}
	}

	/**
	 * Ask Curios to open the "back" slot, so an air tank can be worn rather than filling a hotbar
	 * space. Sent during inter-mod enqueue; harmless when Curios is absent.
	 *
	 * Which items may go in the slot is decided by the item tag `curios:back`, so the tanks
	 * themselves need no capability - see data/curios/tags/items/back.json.
	 */
	public static void registerSlots() {
		if (!isLoaded()) {
			return;
		}
		try {
			final Class<?> classPreset = Class.forName("top.theillusivec4.curios.api.SlotTypePreset");
			final Object presetBack = Enum.valueOf(classPreset.asSubclass(Enum.class), "BACK");
			final Object builder = classPreset.getMethod("getMessageBuilder").invoke(presetBack);
			final Object message = builder.getClass().getMethod("build").invoke(builder);
			InterModComms.sendTo(MOD_ID, "register_type", () -> message);
		} catch (final ClassNotFoundException | NoSuchMethodException
		             | IllegalAccessException | InvocationTargetException exception) {
			WarpDrive.logger.warn("Unable to register the Curios back slot ({})", exception.toString());
		}
	}

	private static boolean resolve() {
		if (resolutionAttempted) {
			return !resolutionFailed;
		}
		resolutionAttempted = true;
		try {
			final Class<?> classCuriosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
			getCuriosHelper = classCuriosApi.getMethod("getCuriosHelper");
			final Class<?> classHelper = getCuriosHelper.getReturnType();
			// getEquippedCurios(LivingEntity) -> Optional<IItemHandlerModifiable>
			getEquippedCurios = classHelper.getMethod("getEquippedCurios",
				Class.forName("net.minecraft.entity.LivingEntity"));
		} catch (final ClassNotFoundException | NoSuchMethodException exception) {
			resolutionFailed = true;
			WarpDrive.logger.warn(
				"Curios is present but its API does not match what WarpDrive expects; "
				+ "air tanks in accessory slots will be ignored ({})", exception.toString());
		}
		return !resolutionFailed;
	}
}
