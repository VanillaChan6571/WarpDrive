package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
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
 * uses Forge's ENTITY_GRAVITY attribute, which LivingEntity.travel() already consults.
 *
 * Values match the original: vanilla entity gravity is 0.08, space used 0.025 and hyperspace
 * 0.035, so the ratios below are 31% and 44% of normal.
 *
 * Not yet ported: 1.12.2 also distinguished "field" gravity near ship blocks from
 * SPACE_VOID_GRAVITY (0.001) in open void, and applied separate, lower values to dropped items.
 * The attribute only affects LivingEntity, so items still fall at vanilla speed.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GravityHandler {

	private static final UUID MODIFIER_ID = UUID.fromString("6f2b1c74-3ad8-4f5e-9f4a-2c1d5e7b9a30");
	private static final String MODIFIER_NAME = "warpdrive:dimension_gravity";

	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	/** Fractions of vanilla gravity (0.08), taken from 1.12.2 GravityManager. */
	private static final double SPACE_RATIO = 0.025D / 0.08D;        // 0.3125
	private static final double HYPERSPACE_RATIO = 0.035D / 0.08D;   // 0.4375

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

		final double ratio = gravityRatioFor(entity.level);
		final AttributeModifier existing = attribute.getModifier(MODIFIER_ID);

		if (ratio >= 1.0D) {
			// Back in a normal dimension - drop the modifier so gravity returns to vanilla
			if (existing != null) {
				attribute.removeModifier(MODIFIER_ID);
			}
			return;
		}

		// MULTIPLY_TOTAL sums the amounts and applies (1 + sum), so amount is ratio - 1
		final double amount = ratio - 1.0D;
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

	private static double gravityRatioFor(final World world) {
		final String dimension = world.dimension().location().toString();
		if (SPACE.equals(dimension)) {
			return SPACE_RATIO;
		}
		if (HYPERSPACE.equals(dimension)) {
			return HYPERSPACE_RATIO;
		}
		return 1.0D;
	}
}
