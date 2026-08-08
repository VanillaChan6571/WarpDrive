package cr0s.warpdrive.event;

import cr0s.warpdrive.data.CloakManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Keeps per-player cloak visibility aligned with movement and chunk watching. */
public final class CloakEventHandler {

	private CloakEventHandler() { }

	public static void register() { MinecraftForge.EVENT_BUS.register(CloakEventHandler.class); }

	@SubscribeEvent
	public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide
		 || event.player.tickCount % 10 != 0 || !(event.player instanceof ServerPlayerEntity)) return;
		CloakManager.updatePlayer((ServerPlayerEntity) event.player);
	}

	@SubscribeEvent
	public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getPlayer() instanceof ServerPlayerEntity) {
			CloakManager.synchronizePlayer((ServerPlayerEntity) event.getPlayer());
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getPlayer() instanceof ServerPlayerEntity) {
			CloakManager.synchronizePlayer((ServerPlayerEntity) event.getPlayer());
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
		CloakManager.forgetPlayer(event.getPlayer().getUUID());
	}

	@SubscribeEvent
	public static void onChunkWatch(final ChunkWatchEvent.Watch event) {
		CloakManager.synchronizeChunk(event.getPlayer(), event.getPos());
	}

	@SubscribeEvent
	public static void onWorldUnload(final WorldEvent.Unload event) {
		if (event.getWorld() instanceof ServerWorld) {
			CloakManager.unloadWorld((ServerWorld) event.getWorld());
		}
	}
}
