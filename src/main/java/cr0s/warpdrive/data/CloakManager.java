package cr0s.warpdrive.data;

import cr0s.warpdrive.network.CloakPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Runtime cloak registry. Areas exist only while their powered core is ticking. */
public final class CloakManager {

	private static final Map<Key, CloakedArea> AREAS = new HashMap<>();

	private CloakManager() { }

	public static void update(final ServerWorld world, final BlockPos core,
	                          final BlockPos min, final BlockPos max,
	                          final boolean transparent) {
		final Key key = new Key(world.dimension().location().toString(), core);
		CloakedArea area = AREAS.get(key);
		if (area != null && (!area.min.equals(min) || !area.max.equals(max)
		                   || area.transparent != transparent)) {
			remove(world, core, true);
			area = null;
		}
		if (area == null) {
			area = new CloakedArea(core, min, max, transparent);
			AREAS.put(key, area);
		}
		synchronizePlayers(world, area, true);
	}

	public static void remove(final ServerWorld world, final BlockPos core,
	                          final boolean reveal) {
		final CloakedArea area = AREAS.remove(
			new Key(world.dimension().location().toString(), core));
		if (area == null) return;
		for (final ServerPlayerEntity player : world.players()) {
			if (send(player, area, true) && reveal) revealToPlayer(world, player, area);
		}
	}

	public static void updatePlayer(final ServerPlayerEntity player) {
		final ServerWorld world = player.getLevel();
		for (final CloakedArea area : areasIn(world)) {
			final boolean inside = area.contains(player.blockPosition());
			final boolean wasInside = area.insidePlayers.contains(player.getUUID());
			if (inside == wasInside) continue;
			if (inside) {
				area.insidePlayers.add(player.getUUID());
				send(player, area, true);
				revealToPlayer(world, player, area);
			} else {
				area.insidePlayers.remove(player.getUUID());
				send(player, area, false);
			}
		}
	}

	public static void synchronizePlayer(final ServerPlayerEntity player) {
		final ServerWorld world = player.getLevel();
		for (final CloakedArea area : areasIn(world)) {
			final boolean inside = area.contains(player.blockPosition());
			if (inside) {
				area.insidePlayers.add(player.getUUID());
				send(player, area, true);
			} else {
				area.insidePlayers.remove(player.getUUID());
				send(player, area, false);
			}
		}
	}

	public static void synchronizeChunk(final ServerPlayerEntity player, final ChunkPos chunkPos) {
		for (final CloakedArea area : areasIn(player.getLevel())) {
			if (!area.insidePlayers.contains(player.getUUID()) && area.intersects(chunkPos)) {
				send(player, area, false);
			}
		}
	}

	public static void forgetPlayer(final UUID uuid) {
		for (final CloakedArea area : AREAS.values()) {
			area.insidePlayers.remove(uuid);
			area.maskedPlayers.remove(uuid);
		}
	}

	public static void unloadWorld(final ServerWorld world) {
		final String dimension = world.dimension().location().toString();
		AREAS.keySet().removeIf(key -> key.dimension.equals(dimension));
	}

	private static void synchronizePlayers(final ServerWorld world, final CloakedArea area,
	                                       final boolean refreshOutside) {
		for (final ServerPlayerEntity player : world.players()) {
			final boolean inside = area.contains(player.blockPosition());
			final boolean wasInside = area.insidePlayers.contains(player.getUUID());
			if (inside) {
				area.insidePlayers.add(player.getUUID());
				if (!wasInside) {
					send(player, area, true);
					revealToPlayer(world, player, area);
				}
			} else {
				area.insidePlayers.remove(player.getUUID());
				if (refreshOutside || wasInside) send(player, area, false);
			}
		}
	}

	private static List<CloakedArea> areasIn(final ServerWorld world) {
		final String dimension = world.dimension().location().toString();
		final List<CloakedArea> result = new ArrayList<>();
		for (final Map.Entry<Key, CloakedArea> entry : AREAS.entrySet()) {
			if (entry.getKey().dimension.equals(dimension)) result.add(entry.getValue());
		}
		return result;
	}

	private static boolean send(final ServerPlayerEntity player, final CloakedArea area,
	                            final boolean uncloaking) {
		if (WarpDriveNetwork.CHANNEL == null) return false;
		if (uncloaking) {
			if (!area.maskedPlayers.remove(player.getUUID())
			 && !area.contains(player.blockPosition()) && !area.isNear(player)) return false;
		} else {
			if (!area.isNear(player)) return false;
			area.maskedPlayers.add(player.getUUID());
		}
		WarpDriveNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
			new CloakPacket(area.core, area.min, area.max, area.transparent, uncloaking));
		return true;
	}

	private static void revealToPlayer(final ServerWorld world, final ServerPlayerEntity player,
	                                   final CloakedArea area) {
		if (!area.contains(player.blockPosition()) && !area.isNear(player)) return;
		final int minChunkX = area.min.getX() >> 4;
		final int maxChunkX = area.max.getX() >> 4;
		final int minChunkZ = area.min.getZ() >> 4;
		final int maxChunkZ = area.max.getZ() >> 4;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!world.hasChunk(chunkX, chunkZ)) continue;
				final Chunk chunk = world.getChunk(chunkX, chunkZ);
				player.connection.send(new SChunkDataPacket(chunk, 65_535));
			}
		}
		for (final Entity entity : world.getEntities((Entity) null, area.bounds,
			entity -> entity != player && !entity.getType().is(WarpDriveTags.NO_REVEAL))) {
			player.connection.send(entity.getAddEntityPacket());
		}
	}

	private static final class Key {
		private final String dimension;
		private final BlockPos core;

		private Key(final String dimension, final BlockPos core) {
			this.dimension = dimension;
			this.core = core.immutable();
		}

		@Override public boolean equals(final Object other) {
			if (this == other) return true;
			if (!(other instanceof Key)) return false;
			final Key key = (Key) other;
			return dimension.equals(key.dimension) && core.equals(key.core);
		}

		@Override public int hashCode() { return Objects.hash(dimension, core); }
	}

	private static final class CloakedArea {
		private final BlockPos core;
		private final BlockPos min;
		private final BlockPos max;
		private final boolean transparent;
		private final AxisAlignedBB bounds;
		private final Set<UUID> insidePlayers = new HashSet<>();
		private final Set<UUID> maskedPlayers = new HashSet<>();

		private CloakedArea(final BlockPos core, final BlockPos min, final BlockPos max,
		                    final boolean transparent) {
			this.core = core.immutable();
			this.min = min.immutable();
			this.max = max.immutable();
			this.transparent = transparent;
			bounds = new AxisAlignedBB(min.getX(), min.getY(), min.getZ(),
				max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D);
		}

		private boolean contains(final BlockPos position) {
			return position.getX() >= min.getX() && position.getX() <= max.getX()
				&& position.getY() >= min.getY() && position.getY() <= max.getY()
				&& position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
		}

		private boolean intersects(final ChunkPos chunk) {
			return chunk.x >= (min.getX() >> 4) && chunk.x <= (max.getX() >> 4)
				&& chunk.z >= (min.getZ() >> 4) && chunk.z <= (max.getZ() >> 4);
		}

		private boolean isNear(final ServerPlayerEntity player) {
			final double centerX = (min.getX() + max.getX() + 1.0D) * 0.5D;
			final double centerY = (min.getY() + max.getY() + 1.0D) * 0.5D;
			final double centerZ = (min.getZ() + max.getZ() + 1.0D) * 0.5D;
			return Math.abs(player.getX() - centerX) < 250.0D
				&& Math.abs(player.getY() - centerY) < 250.0D
				&& Math.abs(player.getZ() - centerZ) < 250.0D;
		}
	}
}
