package cr0s.warpdrive.client;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.network.CloakPacket;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client rendering view of server-authoritative cloak declarations. */
public final class ClientCloakManager {

	private static final Map<BlockPos, ClientArea> AREAS = new LinkedHashMap<>();

	private ClientCloakManager() { }

	public static void register() {
		MinecraftForge.EVENT_BUS.register(ClientCloakManager.class);
	}

	public static void handle(final CloakPacket packet) {
		final ClientWorld world = Minecraft.getInstance().level;
		if (world == null) return;
		final ClientArea previous = AREAS.get(packet.core);
		if (packet.uncloaking || isLocalPlayerInside(packet.min, packet.max)) {
			if (previous != null) {
				AREAS.remove(packet.core);
				markDirty(world, previous);
			}
			return;
		}
		final ClientArea area = new ClientArea(packet.min, packet.max, packet.transparent);
		AREAS.put(packet.core, area);
		if (!area.equals(previous)) markDirty(world, area);
		hideEntities(world, area);
	}

	/** Called by the client-only chunk mixin after the real state has been read. */
	public static BlockState maskBlockState(final BlockPos position, final BlockState original) {
		if (original.isAir()) return original;
		for (final ClientArea area : AREAS.values()) {
			if (!area.contains(position)) continue;
			return area.transparent
				? net.minecraft.block.Blocks.AIR.defaultBlockState()
				: Registration.GAS_BLOCKS.get("darkness").get().defaultBlockState();
		}
		return original;
	}

	private static boolean isLocalPlayerInside(final BlockPos min, final BlockPos max) {
		return Minecraft.getInstance().player != null
			&& contains(min, max, Minecraft.getInstance().player.blockPosition());
	}

	private static void hideEntities(final ClientWorld world, final ClientArea area) {
		final Entity localPlayer = Minecraft.getInstance().player;
		for (final Entity entity : new ArrayList<>(world.getEntities((Entity) null, area.bounds,
			candidate -> candidate != localPlayer))) {
			world.removeEntity(entity.getId());
		}
	}

	private static void markDirty(final ClientWorld world, final ClientArea area) {
		for (int sectionX = area.min.getX() >> 4; sectionX <= area.max.getX() >> 4; sectionX++) {
			for (int sectionY = Math.max(0, area.min.getY() >> 4);
			     sectionY <= Math.min(15, area.max.getY() >> 4); sectionY++) {
				for (int sectionZ = area.min.getZ() >> 4; sectionZ <= area.max.getZ() >> 4; sectionZ++) {
					world.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onChunkLoad(final ChunkEvent.Load event) {
		if (!(event.getWorld() instanceof ClientWorld)) return;
		final ClientWorld world = (ClientWorld) event.getWorld();
		final ChunkPos chunk = event.getChunk().getPos();
		for (final ClientArea area : AREAS.values()) {
			if (area.intersects(chunk)) markDirty(world, area);
		}
	}

	@SubscribeEvent
	public static void onWorldUnload(final WorldEvent.Unload event) {
		if (event.getWorld() instanceof ClientWorld) AREAS.clear();
	}

	private static boolean contains(final BlockPos min, final BlockPos max, final BlockPos position) {
		return position.getX() >= min.getX() && position.getX() <= max.getX()
			&& position.getY() >= min.getY() && position.getY() <= max.getY()
			&& position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
	}

	private static final class ClientArea {
		private final BlockPos min;
		private final BlockPos max;
		private final boolean transparent;
		private final AxisAlignedBB bounds;

		private ClientArea(final BlockPos min, final BlockPos max, final boolean transparent) {
			this.min = min.immutable();
			this.max = max.immutable();
			this.transparent = transparent;
			bounds = new AxisAlignedBB(min.getX(), min.getY(), min.getZ(),
				max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D);
		}

		private boolean contains(final BlockPos position) {
			return ClientCloakManager.contains(min, max, position);
		}

		private boolean intersects(final ChunkPos chunk) {
			return chunk.x >= (min.getX() >> 4) && chunk.x <= (max.getX() >> 4)
				&& chunk.z >= (min.getZ() >> 4) && chunk.z <= (max.getZ() >> 4);
		}

		@Override public boolean equals(final Object other) {
			if (this == other) return true;
			if (!(other instanceof ClientArea)) return false;
			final ClientArea area = (ClientArea) other;
			return transparent == area.transparent && min.equals(area.min) && max.equals(area.max);
		}

		@Override public int hashCode() {
			int result = min.hashCode();
			result = 31 * result + max.hashCode();
			return 31 * result + (transparent ? 1 : 0);
		}
	}
}
