package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IAirContainerItem;
import cr0s.warpdrive.api.IBreathingHelmet;
import cr0s.warpdrive.damage.WarpDamageSources;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Breathing and asphyxiation in the WarpDrive dimensions, ported from 1.12.2 BreathingManager.
 *
 * This deliberately does NOT use the vanilla air supply. The first cut of vacuum did, and it could
 * never work: LivingEntity.baseTick tops air back up by min(air + 4, max) every tick an entity is
 * out of water, and Forge fires LivingUpdateEvent before that runs, so any drain was handed straight
 * back. More to the point, 1.12.2 never used vanilla air either - it kept its own per-entity tick
 * counters and drew its own gauge, which is what this restores.
 *
 * The model, unchanged from the original:
 *
 *   - Entities get a 300 tick grace period on their first breath in vacuum, so stepping through an
 *     airlock does not immediately start the clock.
 *   - Players then breathe from air tanks in their inventory, one breath at a time, each lasting
 *     300 ticks. The most-depleted tank is drained first so partial tanks get finished off.
 *   - Breathing at all requires a full four-piece armour set plus a working breathing helmet.
 *     Other living entities need only the helmet.
 *   - With no air left, asphyxia deals 2 damage every 20 ticks, bypassing armour.
 *
 * Not ported yet, all blocked on the air-block subsystem (AirSpreader / StateAir / ChunkHandler)
 * rather than on effort - until those land there are no breathable interiors, so a suit is required
 * everywhere in space:
 *   - air blocks and sealed rooms
 *   - IC2 compressed air cells
 *   - refilling tanks by electrolysing ice with a superior chestplate
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BreathingManager {

	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	/** Grace period granted on the first breath in vacuum, and when leaving breathable air. */
	private static final int AIR_FIRST_BREATH_TICKS = 300;
	/** Interval between asphyxia hits once out of air. */
	private static final int AIR_DROWN_TICKS = 20;
	/** Damage per asphyxia hit. */
	private static final float ASPHYXIA_DAMAGE = 2.0F;

	/**
	 * Environmental air remaining, in ticks. Without air blocks this only ever carries the initial
	 * grace period, but it is kept separate from the tank counter exactly as in 1.12.2 so air
	 * blocks can be slotted in later without reworking the state machine.
	 */
	private static final Map<UUID, Integer> ENTITY_AIR_BLOCK = new ConcurrentHashMap<>();
	/** Ticks left on the breath a player is currently drawing from a tank. */
	private static final Map<UUID, Integer> PLAYER_AIR_TANK = new ConcurrentHashMap<>();

	private BreathingManager() {
	}

	@SubscribeEvent
	public static void onLivingUpdate(final LivingEvent.LivingUpdateEvent event) {
		final LivingEntity entity = event.getEntityLiving();
		if (entity == null || entity.level == null || entity.level.isClientSide) {
			return;
		}

		final UUID uuid = entity.getUUID();
		if (!isVacuum(entity.level)) {
			// Back in breathable air: forget everything so re-entry grants a fresh grace period
			ENTITY_AIR_BLOCK.remove(uuid);
			PLAYER_AIR_TANK.remove(uuid);
			return;
		}
		if (isExempt(entity)) {
			return;
		}

		// No air blocks ported yet, so everywhere in these dimensions counts as vacuum
		final Integer environmental = ENTITY_AIR_BLOCK.get(uuid);

		// First tick in vacuum - grace period before anything starts draining
		if (environmental == null) {
			ENTITY_AIR_BLOCK.put(uuid, AIR_FIRST_BREATH_TICKS);
			return;
		}

		// Still coasting on the grace period. Players hand over to their tanks as soon as it runs
		// out; other entities simply run it down.
		if (environmental > 0) {
			if (entity instanceof ServerPlayerEntity) {
				ENTITY_AIR_BLOCK.put(uuid, 0);
				PLAYER_AIR_TANK.put(uuid, AIR_FIRST_BREATH_TICKS);
			} else {
				ENTITY_AIR_BLOCK.put(uuid, environmental - 1);
			}
			return;
		}

		final boolean hasValidSetup = hasValidSetup(entity);

		if (entity instanceof ServerPlayerEntity) {
			tickPlayer((ServerPlayerEntity) entity, uuid, hasValidSetup);
			return;
		}

		if (hasValidSetup) {
			// Let it live, re-checking periodically in case the helmet is destroyed in combat
			ENTITY_AIR_BLOCK.put(uuid, AIR_FIRST_BREATH_TICKS);
		} else {
			ENTITY_AIR_BLOCK.put(uuid, 0);
			entity.hurt(WarpDamageSources.ASPHYXIA, ASPHYXIA_DAMAGE);
		}
	}

	private static void tickPlayer(final ServerPlayerEntity player, final UUID uuid,
	                               final boolean hasValidSetup) {
		Integer ticksLeft = PLAYER_AIR_TANK.get(uuid);
		boolean isBreathing = hasValidSetup;

		if (hasValidSetup) {
			if (ticksLeft == null) {
				PLAYER_AIR_TANK.put(uuid, AIR_FIRST_BREATH_TICKS);
			} else if (ticksLeft <= 1) {
				// Current breath is spent - draw the next one
				final int ticksAir = consumeAir(player);
				if (ticksAir > 0) {
					// No message: the gauge already shows the reserve dropping, and at one breath
					// every 300 ticks an action bar notice would just be noise
					PLAYER_AIR_TANK.put(uuid, ticksAir);
				} else {
					// Suit is fine, but every tank is empty
					isBreathing = false;
				}
			} else {
				PLAYER_AIR_TANK.put(uuid, ticksLeft - 1);
			}
		}

		if (isBreathing) {
			return;
		}

		ticksLeft = PLAYER_AIR_TANK.get(uuid);
		if (ticksLeft == null) {
			PLAYER_AIR_TANK.put(uuid, AIR_FIRST_BREATH_TICKS);
		} else if (ticksLeft <= 1) {
			PLAYER_AIR_TANK.put(uuid, AIR_DROWN_TICKS);
			player.hurt(WarpDamageSources.ASPHYXIA, ASPHYXIA_DAMAGE);
			// No message here: warning the player is the client overlay's job, which raises the
			// same splash alarm 1.12.2 used rather than writing to the action bar
		} else {
			PLAYER_AIR_TANK.put(uuid, ticksLeft - 1);
		}
	}

	/**
	 * Draw one breath from the player's inventory, returning how long it lasts, or 0 if there is
	 * nothing left to breathe.
	 *
	 * Picks the most-depleted tank first, as 1.12.2 did, so partially used tanks get finished
	 * rather than leaving a spread of half-empty ones.
	 */
	private static int consumeAir(final ServerPlayerEntity player) {
		final NonNullList<ItemStack> inventory = player.inventory.items;

		int slotFound = -1;
		float lowestFillRatio = Float.MAX_VALUE;

		for (int slot = 0; slot < inventory.size(); slot++) {
			final ItemStack itemStack = inventory.get(slot);
			if (itemStack.isEmpty() || !(itemStack.getItem() instanceof IAirContainerItem)) {
				continue;
			}
			final IAirContainerItem container = (IAirContainerItem) itemStack.getItem();
			final int available = container.getCurrentAirStorage(itemStack);
			if (available <= 0) {
				continue;
			}
			final int capacity = container.getMaxAirStorage(itemStack);
			final float fillRatio = capacity > 0 ? available / (float) capacity : 0.0F;
			if (fillRatio < lowestFillRatio) {
				lowestFillRatio = fillRatio;
				slotFound = slot;
			}
		}

		if (slotFound < 0) {
			return 0;
		}

		final ItemStack itemStack = inventory.get(slotFound);
		final IAirContainerItem container = (IAirContainerItem) itemStack.getItem();
		final ItemStack consumed = container.consumeAir(itemStack);
		if (consumed != itemStack) {
			inventory.set(slotFound, consumed);
		}
		// Push the new damage value out so the tank's fill level updates on the client
		player.inventoryMenu.broadcastChanges();

		return container.getAirTicksPerConsumption(consumed);
	}

	/**
	 * Whether this entity is equipped to breathe at all.
	 *
	 * Players need the full four-piece set plus a working breathing helmet; other living entities
	 * need only the helmet. Straight from 1.12.2 - the full-set requirement is why a helmet alone
	 * no longer keeps you alive.
	 */
	public static boolean hasValidSetup(@Nonnull final LivingEntity entity) {
		final ItemStack helmet = entity.getItemBySlot(EquipmentSlotType.HEAD);
		if (helmet.isEmpty()) {
			return false;
		}

		if (entity instanceof PlayerEntity) {
			if ( entity.getItemBySlot(EquipmentSlotType.CHEST).isEmpty()
			  || entity.getItemBySlot(EquipmentSlotType.LEGS).isEmpty()
			  || entity.getItemBySlot(EquipmentSlotType.FEET).isEmpty() ) {
				return false;
			}
		}

		final Item itemHelmet = helmet.getItem();
		return itemHelmet instanceof IBreathingHelmet
		    && ((IBreathingHelmet) itemHelmet).canBreath(entity);
	}

	/**
	 * Air still sitting in the player's tanks, in ticks.
	 *
	 * This is what is left to *draw*, so it excludes the breath already being consumed - that one
	 * was deducted from its tank when it was taken.
	 *
	 * Safe to call client-side: a player's own inventory is synced.
	 */
	public static int getStoredAirTicks(@Nonnull final PlayerEntity player) {
		int storedTicks = 0;
		for (final ItemStack itemStack : player.inventory.items) {
			if (itemStack.isEmpty() || !(itemStack.getItem() instanceof IAirContainerItem)) {
				continue;
			}
			final IAirContainerItem container = (IAirContainerItem) itemStack.getItem();
			storedTicks += container.getCurrentAirStorage(itemStack)
			             * container.getAirTicksPerConsumption(itemStack);
		}
		return storedTicks;
	}

	/** Total air the player could carry with every tank full, in ticks. */
	public static int getAirCapacityTicks(@Nonnull final PlayerEntity player) {
		int capacityTicks = 0;
		for (final ItemStack itemStack : player.inventory.items) {
			if (itemStack.isEmpty() || !(itemStack.getItem() instanceof IAirContainerItem)) {
				continue;
			}
			final IAirContainerItem container = (IAirContainerItem) itemStack.getItem();
			capacityTicks += container.getMaxAirStorage(itemStack)
			               * container.getAirTicksPerConsumption(itemStack);
		}
		return capacityTicks;
	}

	/** Fraction of total air capacity still stored across the player's tanks, for the HUD gauge. */
	public static float getAirReserveRatio(@Nonnull final PlayerEntity player) {
		final int capacityTicks = getAirCapacityTicks(player);
		return capacityTicks > 0 ? getStoredAirTicks(player) / (float) capacityTicks : 0.0F;
	}

	public static boolean isVacuum(final World world) {
		final String dimension = world.dimension().location().toString();
		return SPACE.equals(dimension) || HYPERSPACE.equals(dimension);
	}

	/**
	 * Stand-in for 1.12.2's Dictionary.isLivingWithoutAir, which was a configurable list. Creative
	 * and spectator players are exempt too, as they were before.
	 */
	private static boolean isExempt(final LivingEntity entity) {
		if (entity instanceof PlayerEntity) {
			final PlayerEntity player = (PlayerEntity) entity;
			return player.isCreative() || player.isSpectator();
		}
		return entity instanceof ArmorStandEntity
		    || entity.getMobType() == CreatureAttribute.UNDEAD
		    || entity.canBreatheUnderwater();
	}

	@SubscribeEvent
	public static void onLivingDeath(final LivingDeathEvent event) {
		forget(event.getEntityLiving());
	}

	@SubscribeEvent
	public static void onEntityLeaveWorld(final EntityLeaveWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			forget((LivingEntity) event.getEntity());
		}
	}

	private static void forget(final LivingEntity entity) {
		ENTITY_AIR_BLOCK.remove(entity.getUUID());
		PLAYER_AIR_TANK.remove(entity.getUUID());
	}
}
