package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Reduced gravity in the WarpDrive dimensions.
 *
 * 1.12.2 did this with a CoreMod class transformer that rewrote Entity's hardcoded 0.08 fall
 * constant into a call to its own GravityManager. CoreMods were removed in 1.13+, so this instead
 * uses Forge's ENTITY_GRAVITY attribute, which LivingEntity.travel() already consults - the Forge
 * patch replaces the 0.08 local with the attribute's value, so writing the attribute is equivalent
 * to what the transformer did. The attribute is registered with a range of -8..8 and is syncable,
 * so negative gravity is representable and players' client-side movement prediction stays in step.
 *
 * The model is the one from GravityManager.getGravityForEntity, not a flat per-dimension constant:
 *
 *   - In a "gravi-field" - within 20 blocks above a substantial solid block, i.e. standing on or
 *     near a ship or asteroid - gravity is 0.025 in space, 0.035 in hyperspace.
 *   - In open void it drops to 0.001, which is what makes you drift rather than fall. Hyperspace
 *     adds a random jitter of +/-0.005 on top, so the value goes negative about half the time and
 *     you get pushed around instead of settling.
	 *   - Sneaking is the descent control in the void: 0.02 while wearing space-flight equipment,
	 *     0.005 without. Without this you have no way down once you are off the hull.
 *
 * Note the field/void split is what actually makes space feel like 1.12.2. A flat 0.025 everywhere
 * still falls 25x faster than the void value, so you sink instead of floating.
 *
 * Dropped items are handled too, but not through the attribute - that only exists on LivingEntity,
 * and Forge has no per-entity tick event for the rest. ItemEntityMixin substitutes the gravity and
 * drag constants inside ItemEntity.tick instead, which is where the 1.12.2 CoreMod rewrote them.
 * See getItemGravity / getItemDrag below.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GravityHandler {

	private static final UUID MODIFIER_ID = UUID.fromString("6f2b1c74-3ad8-4f5e-9f4a-2c1d5e7b9a30");
	private static final String MODIFIER_NAME = "warpdrive:dimension_gravity";

	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	/** Vanilla entity gravity, and the base the attribute multiplies. */
	private static final double VANILLA_GRAVITY = 0.08D;

	/** Absolute gravity values, taken verbatim from 1.12.2 GravityManager. */
	private static final double SPACE_FIELD_GRAVITY = 0.025D;
	private static final double HYPERSPACE_FIELD_GRAVITY = 0.035D;
	private static final double VOID_GRAVITY = 0.001D;
	private static final double HYPERSPACE_VOID_JITTER = 0.005D;
	private static final double VOID_SNEAK_IN_ARMOUR = 0.02D;
	private static final double VOID_SNEAK_UNPROTECTED = 0.005D;

	/** Sentinel for "leave vanilla gravity alone". */
	private static final double GRAVITY_VANILLA = Double.NaN;

	/** Vanilla item physics, the exact literals ItemEntityMixin replaces. */
	private static final double VANILLA_ITEM_GRAVITY = 0.04D;
	private static final double VANILLA_ITEM_DRAG = 0.98D;

	/** Item gravity and vertical drag, from 1.12.2 getItemGravity / getItemGravity2. */
	private static final double FIELD_ITEM_GRAVITY = 0.02D;
	private static final double FIELD_ITEM_DRAG = 0.60D;
	private static final double VOID_ITEM_GRAVITY = 0.001D;
	private static final double VOID_ITEM_DRAG = 0.001D;

	/** How far below the entity to look for something to stand near. */
	private static final int FIELD_CHECK_DISTANCE = 20;
	/** A block only anchors a field if it is close to a full cube - slabs and panels do not count. */
	private static final double FIELD_MIN_AVERAGE_EDGE = 0.90D;

	/**
	 * Hyperspace jitter is re-rolled on this period rather than every tick. Every tick would mean
	 * swapping the modifier 20 times a second, and since the attribute is syncable that is an
	 * attribute packet per player per tick. At 10 ticks the drift still reads as unsteady.
	 */
	private static final int JITTER_PERIOD_TICKS = 10;

	private GravityHandler() {
	}

	@SubscribeEvent
	public static void onLivingUpdate(final LivingEvent.LivingUpdateEvent event) {
		final LivingEntity entity = event.getEntityLiving();
		if (entity == null || entity.level == null || entity.level.isClientSide) {
			return;
		}

		final ModifiableAttributeInstance attribute = entity.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
		if (attribute == null) {
			return;
		}

		final double gravity = gravityFor(entity);
		final AttributeModifier existing = attribute.getModifier(MODIFIER_ID);

		if (Double.isNaN(gravity)) {
			// Back in a normal dimension - drop the modifier so gravity returns to vanilla
			if (existing != null) {
				attribute.removeModifier(MODIFIER_ID);
			}
			return;
		}

		// MULTIPLY_TOTAL sums the amounts and applies (1 + sum), so amount is ratio - 1
		final double amount = (gravity / VANILLA_GRAVITY) - 1.0D;
		if (existing != null) {
			if (Math.abs(existing.getAmount() - amount) < 1.0e-9D) {
				return;   // already correct, nothing to do
			}
			attribute.removeModifier(MODIFIER_ID);
		}

		// Transient, not permanent: this must never be written to the entity's saved NBT, or a
		// player logging out in space would keep low gravity everywhere.
		attribute.addTransientModifier(new AttributeModifier(
			MODIFIER_ID, MODIFIER_NAME, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/**
	 * Gravity for a dropped item, in blocks per tick squared, returned as a positive magnitude.
	 * Substituted into ItemEntity.tick by ItemEntityMixin.
	 *
	 * 1.12.2 quoted these as 0.039999999105930328 and 0.9800000190734863 - float-widened doubles
	 * captured from the old constants. 1.16.5 declares them as exact double literals, so the
	 * vanilla values below are the exact ones being replaced.
	 */
	public static double getItemGravity(final Entity item) {
		if (!isReducedGravity(item.level)) {
			return VANILLA_ITEM_GRAVITY;
		}
		return isInGraviField(item) ? FIELD_ITEM_GRAVITY : VOID_ITEM_GRAVITY;
	}

	/**
	 * Vertical drag for a dropped item, applied per tick.
	 *
	 * This is the value that actually makes items float. Terminal velocity is
	 * -gravity * drag / (1 - drag), so the void's 0.001 pair settles around a millionth of a block
	 * per tick - items hang - while the field's 0.02 / 0.60 settles near -0.03.
	 *
	 * Only the vertical component is affected; horizontal friction stays vanilla, as in 1.12.2.
	 */
	public static double getItemDrag(final Entity item) {
		if (!isReducedGravity(item.level)) {
			return VANILLA_ITEM_DRAG;
		}
		return isInGraviField(item) ? FIELD_ITEM_DRAG : VOID_ITEM_DRAG;
	}

	/** True in the dimensions where WarpDrive overrides gravity. */
	public static boolean isReducedGravity(final World world) {
		final String dimension = world.dimension().location().toString();
		return SPACE.equals(dimension) || HYPERSPACE.equals(dimension);
	}

	/** Absolute gravity for this entity, or {@link #GRAVITY_VANILLA} outside the WarpDrive dimensions. */
	private static double gravityFor(final LivingEntity entity) {
		final String dimension = entity.level.dimension().location().toString();
		final boolean inHyperspace = HYPERSPACE.equals(dimension);
		if (!inHyperspace && !SPACE.equals(dimension)) {
			return GRAVITY_VANILLA;
		}

		if (isInGraviField(entity)) {
			return inHyperspace ? HYPERSPACE_FIELD_GRAVITY : SPACE_FIELD_GRAVITY;
		}

		// Open void. Sneaking is the only way to make headway downwards.
		if (entity.isShiftKeyDown()) {
			return hasSpaceFlightEquipment(entity)
				? VOID_SNEAK_IN_ARMOUR : VOID_SNEAK_UNPROTECTED;
		}

		if (inHyperspace) {
			// Quantised so the modifier is only swapped once per period - see JITTER_PERIOD_TICKS
			final double roll = jitterFor(entity);
			return VOID_GRAVITY + (roll - 0.5D) * 2.0D * HYPERSPACE_VOID_JITTER;
		}

		return VOID_GRAVITY;
	}

	/**
	 * A stable-per-period pseudo-random roll in [0,1). Derived from the entity id and the tick
	 * bucket rather than world.random so that it only changes once per period and entities do not
	 * all drift in unison.
	 */
	private static double jitterFor(final LivingEntity entity) {
		final int bucket = entity.tickCount / JITTER_PERIOD_TICKS;
		int hash = entity.getId() * 31 + bucket;
		hash ^= hash >>> 15;
		hash *= 0x2c1b3c6d;
		hash ^= hash >>> 12;
		return (hash & 0x7fffffff) / (double) 0x80000000L;
	}

	/**
	 * True when there is a substantial solid block within {@link #FIELD_CHECK_DISTANCE} below the
	 * entity - the 1.12.2 stand-in for "close enough to a ship or asteroid to have a floor".
	 *
	 * Shared with item gravity rather than duplicated: if the two disagreed about where the field
	 * is, dropped items would hang in mid-air beside a player standing on solid ground.
	 */
	public static boolean isInGraviField(final Entity entity) {
		return isInGraviField(entity.level, entity.getX(), entity.getY(), entity.getZ());
	}

	public static boolean isInGraviField(final World world,
	                                     final double posX, final double posY, final double posZ) {
		final int x = MathHelper.floor(posX);
		final int y = MathHelper.floor(posY);
		final int z = MathHelper.floor(posZ);
		final int yMin = Math.max(0, y - FIELD_CHECK_DISTANCE);

		final BlockPos.Mutable blockPos = new BlockPos.Mutable(x, y, z);
		for (int scanY = y; scanY > yMin; scanY--) {
			blockPos.setY(scanY);
			final BlockState blockState = world.getBlockState(blockPos);
			final VoxelShape shape = blockState.getCollisionShape(world, blockPos);
			if (shape.isEmpty()) {
				continue;   // covers air and every other non-colliding block
			}
			if (shape.bounds().getSize() > FIELD_MIN_AVERAGE_EDGE) {
				return true;
			}
		}

		return false;
	}

	/**
	 * The 1.12.2 FlyInSpace Dictionary classification, now reloadable through an item tag.
	 */
	private static boolean hasSpaceFlightEquipment(final LivingEntity entity) {
		for (final ItemStack armour : entity.getArmorSlots()) {
			if (armour.getItem().is(WarpDriveTags.FLY_IN_SPACE)) {
				return true;
			}
		}
		return false;
	}
}
