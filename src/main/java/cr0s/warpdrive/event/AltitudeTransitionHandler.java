package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialCoordinates;
import cr0s.warpdrive.data.DimensionAltitude;
import cr0s.warpdrive.debug.DebugLog;
import cr0s.warpdrive.item.WarpArmorItem;
import cr0s.warpdrive.item.WarpArmorMaterial;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Falling out of the bottom of space, ported from 1.12.2 LivingHandler.
 *
 * You can never fall out of space. Drop below the world and one of two things happens: you descend
 * to the celestial object below, or - if there is nothing beneath you - you wrap around and come
 * back in from the top. Space is vertically toroidal, which is why the original never needed void
 * damage here.
 *
 * Without this, falling in space is simply fatal, which is both un-1.12.2 and unrecoverable.
 *
 * The old XML map needed an orbit lookup because several worlds shared one dimension. In the
 * current one-world-per-layer design the child covers the complete layer, while universal
 * coordinates preserve the 8:1 hyperspace scale during transfer.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltitudeTransitionHandler {

	/** Below this, the entity is considered to have fallen out of the world. */
	private static final double FALL_THRESHOLD = -10.0D;

	/** Where a wrap-around re-enters from. Above build height, so you fall back into the field. */
	private static final double WRAP_ALTITUDE = 260.0D;

	/**
	 * Entry altitude when descending from space to a planet. Deliberately far above build height:
	 * re-entry is meant to be a long fall, not a step through a door.
	 */
	private static final double REENTRY_ALTITUDE = 1500.0D;

	/** Entry altitude when descending from hyperspace into space. */
	private static final double DESCENT_ALTITUDE = 261.0D;

	/** Burn duration on hitting an atmosphere unprotected. */
	private static final int ATMOSPHERIC_ENTRY_FIRE_SECONDS = 30;

	/** Fall tolerance granted on arrival, so the transition itself does not injure. */
	private static final float ARRIVAL_FALL_TOLERANCE = -5.0F;

	private AltitudeTransitionHandler() {
	}

	@SubscribeEvent
	public static void onLivingUpdate(final LivingEvent.LivingUpdateEvent event) {
		final LivingEntity entity = event.getEntityLiving();
		if (entity == null || entity.level == null || entity.level.isClientSide) {
			return;
		}
		if (entity.getY() >= FALL_THRESHOLD) {
			return;
		}

		if (!DimensionAltitude.isSpaceOrHyperspace(entity.level)) {
			return;
		}
		final String dimension = DimensionAltitude.idOf(entity.level);
		final boolean fromSpace = DimensionAltitude.SPACE.equals(dimension);

		final ServerWorld destination = resolveDestination(entity);
		if (destination == null) {
			// Nothing below: come back in from the top rather than dying
			wrapAround(entity);
			return;
		}

		final double x = CelestialCoordinates.mapHorizontal(entity.level, destination, entity.getX());
		final double z = CelestialCoordinates.mapHorizontal(entity.level, destination, entity.getZ());
		final double y = fromSpace ? REENTRY_ALTITUDE : DESCENT_ALTITUDE;

		entity.fallDistance = ARRIVAL_FALL_TOLERANCE;
		DebugLog.log("TRANSITION", "{} fell out of {}, descending to {} at {} {} {}",
			entity.getName().getString(), dimension,
			destination.dimension().location(), (int) x, (int) y, (int) z);

		transfer(entity, destination, x, y, z);

		// Space has no atmosphere to slow you; arriving in one at that speed burns
		if (fromSpace) {
			applyAtmosphericEntry(entity);
		}
	}

	@Nullable
	private static ServerWorld resolveDestination(final LivingEntity entity) {
		final MinecraftServer server = entity.level.getServer();
		final RegistryKey<World> below = DimensionAltitude.below(entity.level);
		if (server == null || below == null) {
			return null;
		}
		return server.getLevel(below);
	}

	/** Re-enter from the top of the same world, preserving horizontal position and momentum. */
	private static void wrapAround(final LivingEntity entity) {
		teleportWithin(entity, entity.getX(), WRAP_ALTITUDE, entity.getZ());
		DebugLog.log("TRANSITION", "{} wrapped around to y={}",
			entity.getName().getString(), (int) WRAP_ALTITUDE);
	}

	private static void teleportWithin(final Entity entity, final double x, final double y, final double z) {
		if (entity instanceof ServerPlayerEntity) {
			// A bare setPos leaves the client believing it is still falling
			((ServerPlayerEntity) entity).connection.teleport(x, y, z, entity.yRot, entity.xRot);
		} else {
			entity.teleportTo(x, y, z);
		}
	}

	/** Same approach as WarpEngine uses for ship passengers. */
	private static void transfer(final Entity entity, final ServerWorld destination,
	                             final double x, final double y, final double z) {
		if (entity instanceof ServerPlayerEntity) {
			((ServerPlayerEntity) entity).teleportTo(destination, x, y, z, entity.yRot, entity.xRot);
			return;
		}

		final Entity moved = entity.changeDimension(destination, new ITeleporter() {
			@Override
			public Entity placeEntity(final Entity entityToPlace, final ServerWorld currentWorld,
			                          final ServerWorld target, final float yaw,
			                          final Function<Boolean, Entity> repositionEntity) {
				final Entity placed = repositionEntity.apply(false);   // no portal
				if (placed != null) {
					placed.teleportTo(x, y, z);
				}
				return placed;
			}
		});
		if (moved == null) {
			WarpDrive.logger.warn("Failed to descend entity {} out of space",
				entity.getName().getString());
		}
	}

	/**
	 * Set the entity alight unless it is shielded, from 1.12.2.
	 *
	 * A player needs the full four pieces above basic tier; anything else needs only one. Basic
	 * armour does not protect - it is a pressure suit, not a heat shield.
	 */
	private static void applyAtmosphericEntry(final LivingEntity entity) {
		int countReentryArmor = 0;
		for (final ItemStack itemStack : entity.getArmorSlots()) {
			if ( itemStack.getItem() instanceof WarpArmorItem
			  && ((WarpArmorItem) itemStack.getItem()).getWarpMaterial() != WarpArmorMaterial.BASIC ) {
				countReentryArmor++;
			}
		}

		if ( countReentryArmor == 4
		  || (!(entity instanceof PlayerEntity) && countReentryArmor >= 1) ) {
			return;
		}
		entity.setSecondsOnFire(ATMOSPHERIC_ENTRY_FIRE_SECONDS);
	}
}
