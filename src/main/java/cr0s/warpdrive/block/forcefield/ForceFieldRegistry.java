package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.api.IBeamFrequency;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Loaded force-field projectors and relays, grouped into 20-block relay networks by frequency. */
public final class ForceFieldRegistry {

	public static final int RELAY_RANGE = 20;
	private static final int RELAY_RANGE_SQUARED = RELAY_RANGE * RELAY_RANGE;
	private static final Map<ServerWorld, Map<Integer, Set<Node>>> REGISTRY =
		Collections.synchronizedMap(new WeakHashMap<>());

	private ForceFieldRegistry() {
	}

	public static void update(final AbstractForceFieldTileEntity tileEntity) {
		if (!(tileEntity.getLevel() instanceof ServerWorld)) {
			return;
		}
		final ServerWorld world = (ServerWorld) tileEntity.getLevel();
		remove(world, tileEntity.getBlockPos());
		if (!IBeamFrequency.isValid(tileEntity.getBeamFrequency())) {
			return;
		}
		final Map<Integer, Set<Node>> byFrequency = REGISTRY.computeIfAbsent(world, ignored -> new HashMap<>());
		byFrequency.computeIfAbsent(tileEntity.getBeamFrequency(), ignored -> new HashSet<>())
			.add(new Node(tileEntity.getBlockPos(), tileEntity instanceof ForceFieldRelayTileEntity));
	}

	public static void remove(final AbstractForceFieldTileEntity tileEntity) {
		if (tileEntity.getLevel() instanceof ServerWorld) {
			remove((ServerWorld) tileEntity.getLevel(), tileEntity.getBlockPos());
		}
	}

	private static void remove(final ServerWorld world, final BlockPos blockPos) {
		final Map<Integer, Set<Node>> byFrequency = REGISTRY.get(world);
		if (byFrequency == null) {
			return;
		}
		byFrequency.values().forEach(nodes -> nodes.removeIf(node -> node.blockPos.equals(blockPos)));
		byFrequency.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	public static Set<AbstractForceFieldTileEntity> getNetwork(final ServerWorld world,
	                                                           final int beamFrequency,
	                                                           final BlockPos origin) {
		final Map<Integer, Set<Node>> byFrequency = REGISTRY.get(world);
		if (byFrequency == null || !byFrequency.containsKey(beamFrequency)) {
			return singletonOrigin(world, origin, beamFrequency);
		}

		final Set<Node> nodes = byFrequency.get(beamFrequency);
		final Set<Node> unvisitedRelays = new HashSet<>();
		final Set<Node> nonRelays = new HashSet<>();
		final Set<Node> frontier = new HashSet<>();
		for (final Node node : new HashSet<>(nodes)) {
			if (node.relay) {
				if (node.blockPos.distSqr(origin) <= RELAY_RANGE_SQUARED) {
					frontier.add(node);
				} else {
					unvisitedRelays.add(node);
				}
			} else {
				nonRelays.add(node);
			}
		}
		if (frontier.isEmpty()) {
			return singletonOrigin(world, origin, beamFrequency);
		}

		final Set<Node> connectedRelays = new HashSet<>();
		while (!frontier.isEmpty()) {
			final Set<Node> next = new HashSet<>();
			for (final Node current : frontier) {
				if (!isValid(world, current, beamFrequency)) {
					nodes.remove(current);
					continue;
				}
				connectedRelays.add(current);
				for (final Node candidate : unvisitedRelays) {
					if (current.blockPos.distSqr(candidate.blockPos) <= RELAY_RANGE_SQUARED) {
						next.add(candidate);
					}
				}
			}
			unvisitedRelays.removeAll(next);
			frontier.clear();
			frontier.addAll(next);
		}

		final Set<AbstractForceFieldTileEntity> result = new HashSet<>();
		for (final Node relay : connectedRelays) {
			addIfValid(world, relay, beamFrequency, result, nodes);
			for (final Node projector : nonRelays) {
				if (relay.blockPos.distSqr(projector.blockPos) <= RELAY_RANGE_SQUARED) {
					addIfValid(world, projector, beamFrequency, result, nodes);
				}
			}
		}
		return result;
	}

	private static Set<AbstractForceFieldTileEntity> singletonOrigin(final ServerWorld world,
	                                                                 final BlockPos origin,
	                                                                 final int beamFrequency) {
		final Set<AbstractForceFieldTileEntity> result = new HashSet<>();
		final TileEntity tileEntity = world.getBlockEntity(origin);
		if (tileEntity instanceof AbstractForceFieldTileEntity
		 && ((AbstractForceFieldTileEntity) tileEntity).getBeamFrequency() == beamFrequency) {
			result.add((AbstractForceFieldTileEntity) tileEntity);
		}
		return result;
	}

	private static void addIfValid(final ServerWorld world, final Node node, final int frequency,
	                               final Set<AbstractForceFieldTileEntity> result,
	                               final Set<Node> registeredNodes) {
		if (!world.hasChunkAt(node.blockPos)) {
			return;
		}
		final TileEntity tileEntity = world.getBlockEntity(node.blockPos);
		if (tileEntity instanceof AbstractForceFieldTileEntity
		 && ((AbstractForceFieldTileEntity) tileEntity).getBeamFrequency() == frequency
		 && node.relay == (tileEntity instanceof ForceFieldRelayTileEntity)) {
			result.add((AbstractForceFieldTileEntity) tileEntity);
		} else {
			registeredNodes.remove(node);
		}
	}

	private static boolean isValid(final ServerWorld world, final Node node, final int frequency) {
		if (!world.hasChunkAt(node.blockPos)) {
			return false;
		}
		final TileEntity tileEntity = world.getBlockEntity(node.blockPos);
		return tileEntity instanceof AbstractForceFieldTileEntity
		    && ((AbstractForceFieldTileEntity) tileEntity).getBeamFrequency() == frequency
		    && node.relay == (tileEntity instanceof ForceFieldRelayTileEntity);
	}

	private static final class Node {
		private final BlockPos blockPos;
		private final boolean relay;

		private Node(final BlockPos blockPos, final boolean relay) {
			this.blockPos = blockPos.immutable();
			this.relay = relay;
		}

		@Override
		public boolean equals(final Object object) {
			if (this == object) return true;
			if (!(object instanceof Node)) return false;
			final Node node = (Node) object;
			return relay == node.relay && blockPos.equals(node.blockPos);
		}

		@Override
		public int hashCode() {
			return 31 * blockPos.hashCode() + (relay ? 1 : 0);
		}
	}
}
