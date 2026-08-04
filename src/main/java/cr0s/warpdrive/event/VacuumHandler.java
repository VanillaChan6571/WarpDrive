package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.item.WarpArmorItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Vacuum in the WarpDrive dimensions.
 *
 * Deliberately modelled on drowning rather than a bespoke timer: it drains the entity's normal air
 * supply, so the vanilla bubble HUD works for free and the behaviour is already familiar - bubbles
 * drain, then you start taking damage. A WarpDrive helmet of any tier supplies air and refills it.
 *
 * Creative and spectator players are exempt, as are entities without an air supply.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VacuumHandler {

	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	/** Air ticks lost per tick while unprotected. 1 matches vanilla drowning. */
	private static final int AIR_LOSS_PER_TICK = 1;
	/** Air ticks recovered per tick while wearing a helmet. */
	private static final int AIR_GAIN_PER_TICK = 4;
	/** Damage dealt each second once the air supply is exhausted. */
	private static final float SUFFOCATION_DAMAGE = 2.0F;

	/** Bypasses armour: a sealed suit is the defence, not plating. */
	public static final DamageSource VACUUM = new DamageSource("warpdrive.vacuum")
		.bypassArmor()
		.setMagic();

	private VacuumHandler() {
	}

	@SubscribeEvent
	public static void onLivingUpdate(final LivingEvent.LivingUpdateEvent event) {
		final LivingEntity entity = event.getEntityLiving();
		if (entity == null || entity.level == null || entity.level.isClientSide) {
			return;
		}
		if (!isVacuum(entity.level)) {
			return;
		}
		if (isExempt(entity)) {
			return;
		}

		if (WarpArmorItem.hasBreathingHelmet(entity)) {
			// Suit is sealed: top the air supply back up
			final int air = entity.getAirSupply();
			if (air < entity.getMaxAirSupply()) {
				entity.setAirSupply(Math.min(entity.getMaxAirSupply(), air + AIR_GAIN_PER_TICK));
			}
			return;
		}

		final int air = entity.getAirSupply() - AIR_LOSS_PER_TICK;
		if (air >= 0) {
			entity.setAirSupply(air);
			// Warn once, as the bubbles run out
			if (air == 40 && entity instanceof PlayerEntity) {
				((PlayerEntity) entity).displayClientMessage(new StringTextComponent(
					TextFormatting.RED + "⚠ No air supply - a WarpDrive helmet is required"), true);
			}
			return;
		}

		entity.setAirSupply(0);
		// Damage once a second rather than every tick
		if (entity.tickCount % 20 == 0) {
			entity.hurt(VACUUM, SUFFOCATION_DAMAGE);
		}
	}

	private static boolean isVacuum(final World world) {
		final String dimension = world.dimension().location().toString();
		return SPACE.equals(dimension) || HYPERSPACE.equals(dimension);
	}

	private static boolean isExempt(final LivingEntity entity) {
		if (entity instanceof PlayerEntity) {
			final PlayerEntity player = (PlayerEntity) entity;
			return player.isCreative() || player.isSpectator();
		}
		// Entities that do not breathe anyway (undead, water mobs, armour stands)
		return entity.getMaxAirSupply() <= 0;
	}
}
