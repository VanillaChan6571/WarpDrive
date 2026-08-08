package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fall damage in the WarpDrive dimensions, ported from 1.12.2 LivingHandler.onLivingFall.
 *
 * Vanilla computes fall damage from distance alone, which is wrong under reduced gravity: in space
 * you accelerate far more slowly, so a 30 block drop that vanilla treats as lethal is a gentle
 * drift in at walking pace. Without this, low gravity makes fall damage *worse* rather than better,
 * because you cover more distance before landing while never building up dangerous speed.
 *
 * So damage is driven by impact speed instead of distance:
 *
 *   - Below the impact speed a 3 block vanilla fall produces (-0.6517), no damage at all.
 *   - Otherwise the distance is remapped from the speed via the 1.12.2 curve, and vanilla's usual
 *     "distance - 3" arithmetic then applies to that remapped value.
 *
 * The curve is calibrated so it is a no-op at vanilla gravity - a 4 block overworld fall lands at
 * -0.717, which remaps to 3.22 and still yields exactly 1 damage. That is why 1.12.2 could apply it
 * everywhere. It is scoped to the WarpDrive dimensions here regardless, since those are the only
 * places gravity is altered, and leaving normal worlds strictly untouched is worth more than
 * matching where the original hooked in.
 *
 * LivingFallEvent fires from Entity.move() after collision resolution has already zeroed the
 * entity's vertical motion, so the speed has to be sampled each tick beforehand - the same reason
 * 1.12.2 kept its own entity_yMotion map.
 *
	 * NoFallDamage equipment is checked before the dimension-specific speed remap, matching the
	 * global 1.12.2 handler. The classification is a datapack item tag, so other 1.16 mods can add
	 * their boots or jetpacks without a hard dependency.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FallDamageHandler {

	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	/**
	 * Impact speed of a 3 block fall at vanilla gravity. Below this, 1.12.2 took no damage at all.
	 * For reference from the original: 3 blocks is -0.6517, 4 is -0.717, 5 is -0.844.
	 */
	private static final double MIN_DAMAGING_SPEED = -0.65170D;

	/** Speed-to-distance remap from 1.12.2: -6.582 + 4.148 * e^(1.200 * |motionY|). */
	private static final double REMAP_OFFSET = -6.582D;
	private static final double REMAP_SCALE = 4.148D;
	private static final double REMAP_EXPONENT = 1.200D;

	/**
	 * Last observed downward speed per entity id. Server thread only in practice, but concurrent
	 * because integrated server and client both dispatch living updates.
	 */
	private static final Map<Integer, Double> LAST_MOTION_Y = new ConcurrentHashMap<>();

	private FallDamageHandler() {
	}

	/** Sample vertical speed before the landing zeroes it. */
	@SubscribeEvent
	public static void onLivingUpdate(final LivingEvent.LivingUpdateEvent event) {
		final LivingEntity entity = event.getEntityLiving();
		if (entity == null || entity.level == null || entity.level.isClientSide) {
			return;
		}
		for (final EquipmentSlotType slot : EquipmentSlotType.values()) {
			final ItemStack equipment = entity.getItemBySlot(slot);
			if (equipment.getItem().is(WarpDriveTags.NO_FALL_DAMAGE)) {
				LAST_MOTION_Y.remove(entity.getId());
				event.setCanceled(true);
				return;
			}
		}
		if (!isReducedGravity(entity.level)) {
			// Drop any stale sample so a jump taken right after leaving the dimension is judged
			// by vanilla rules rather than a speed recorded in space
			LAST_MOTION_Y.remove(entity.getId());
			return;
		}

		final double motionY = entity.getDeltaMovement().y;
		if (motionY < 0.0D) {
			LAST_MOTION_Y.put(entity.getId(), motionY);
		}
	}

	/**
	 * Runs at HIGH priority so the decision is made before handlers that only want to see damage
	 * that is actually going to be dealt.
	 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingFall(final LivingFallEvent event) {
		final LivingEntity entity = event.getEntityLiving();
		if (entity == null || entity.level == null || entity.level.isClientSide) {
			return;
		}
		if (!isReducedGravity(entity.level)) {
			return;
		}

		final Double sampled = LAST_MOTION_Y.remove(entity.getId());
		// Fall back to current motion, which is normally already zero by this point and so reads as
		// "landed gently" - the safe direction to err in
		final double motionY = sampled != null ? sampled : entity.getDeltaMovement().y;

		// Drifted in too slowly to be hurt, however far the fall was
		if (motionY > MIN_DAMAGING_SPEED) {
			event.setCanceled(true);
			return;
		}

		// Vanilla's own "ignore the first 3 blocks" check, applied to the real distance first
		if (MathHelper.ceil(event.getDistance() - 3.0F) <= 0) {
			event.setCanceled(true);
			return;
		}

		event.setDistance((float) (REMAP_OFFSET
			+ REMAP_SCALE * Math.exp(REMAP_EXPONENT * Math.abs(motionY))));
	}

	/** Entities can die or unload mid-fall, so the sample map must not outlive them. */
	@SubscribeEvent
	public static void onEntityLeaveWorld(final EntityLeaveWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			LAST_MOTION_Y.remove(event.getEntity().getId());
		}
	}

	private static boolean isReducedGravity(final World world) {
		final String dimension = world.dimension().location().toString();
		return SPACE.equals(dimension) || HYPERSPACE.equals(dimension);
	}
}
