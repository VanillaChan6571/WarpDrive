package cr0s.warpdrive.data;

import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.block.movement.TransporterCoreTileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Persistent registry of known multi-block regions.
 *
 * <p>Loaded providers are weakly referenced, while immutable snapshots live in Overworld saved
 * data. Radar can therefore find known ships in unloaded chunks and other dimensions without
 * force-loading either. The provider lifecycle refreshes those snapshots whenever a ship loads or
 * its assembly changes.</p>
 */
public final class GlobalRegionRegistry {

	private static final String DATA_NAME = "warpdrive_global_regions";
	private static final String TAG_REGIONS = "regions";
	private static final String TAG_TRANSPORTERS = "transporters";
	private static final Set<ShipCoreTileEntity> LIVE_SHIPS =
		Collections.newSetFromMap(new WeakHashMap<>());
	private static final Set<TransporterCoreTileEntity> LIVE_TRANSPORTERS =
		Collections.newSetFromMap(new WeakHashMap<>());

	private GlobalRegionRegistry() { }

	public static void registerShip(final ShipCoreTileEntity shipCore) {
		synchronized (LIVE_SHIPS) { LIVE_SHIPS.add(shipCore); }
		updateShip(shipCore);
	}

	/** Remove only the loaded provider; its last known snapshot remains radar-visible. */
	public static void unregisterShip(final ShipCoreTileEntity shipCore) {
		synchronized (LIVE_SHIPS) { LIVE_SHIPS.remove(shipCore); }
	}

	/** Remove a destroyed or moved-away provider from both the live and persistent registries. */
	public static void removeShip(final ShipCoreTileEntity shipCore) {
		unregisterShip(shipCore);
		if (!(shipCore.getLevel() instanceof ServerWorld)) return;
		final RegistryData data = data((ServerWorld) shipCore.getLevel());
		if (data.regions.remove(shipCore.getSignatureUUID()) != null) data.setDirty();
	}

	public static void updateShip(final ShipCoreTileEntity shipCore) {
		if (!(shipCore.getLevel() instanceof ServerWorld) || shipCore.isRemoved()) return;
		final ServerWorld world = (ServerWorld) shipCore.getLevel();
		final ShipRegion updated = new ShipRegion(shipCore);
		final RegistryData data = data(world);
		// A block replaced in-place must not leave the previous core identity behind.
		data.regions.values().removeIf(region -> !region.uuid.equals(updated.uuid)
			&& region.dimension.equals(updated.dimension) && region.corePos.equals(updated.corePos));
		data.regions.put(updated.uuid, updated);
		data.setDirty();
	}

	public static void registerTransporter(final TransporterCoreTileEntity transporter) {
		synchronized (LIVE_TRANSPORTERS) { LIVE_TRANSPORTERS.add(transporter); }
		updateTransporter(transporter);
	}

	/** Keep the last persistent signature, but forget the unloaded Java object. */
	public static void unregisterTransporter(final TransporterCoreTileEntity transporter) {
		synchronized (LIVE_TRANSPORTERS) { LIVE_TRANSPORTERS.remove(transporter); }
	}

	public static void removeTransporter(final TransporterCoreTileEntity transporter) {
		unregisterTransporter(transporter);
		if (!(transporter.getLevel() instanceof ServerWorld)) return;
		final RegistryData data = data((ServerWorld) transporter.getLevel());
		if (data.transporters.remove(transporter.getSignatureUUID()) != null) data.setDirty();
	}

	public static void updateTransporter(final TransporterCoreTileEntity transporter) {
		if (!(transporter.getLevel() instanceof ServerWorld) || transporter.isRemoved()) return;
		final TransporterLocation updated = new TransporterLocation(transporter);
		final RegistryData data = data((ServerWorld) transporter.getLevel());
		// Replacing a core in place must not leave an unreachable old signature behind.
		data.transporters.values().removeIf(location -> !location.uuid.equals(updated.uuid)
			&& location.dimension.equals(updated.dimension) && location.position.equals(updated.position));
		data.transporters.put(updated.uuid, updated);
		data.setDirty();
	}

	@Nullable
	public static TransporterLocation findTransporter(final ServerWorld context, final UUID uuid) {
		if (uuid == null) return null;
		// Refresh the loaded instance first, so a newly renamed core wins over its old snapshot.
		synchronized (LIVE_TRANSPORTERS) {
			for (final TransporterCoreTileEntity transporter : new ArrayList<>(LIVE_TRANSPORTERS)) {
				if (transporter.isRemoved() || !(transporter.getLevel() instanceof ServerWorld)) continue;
				if (uuid.equals(transporter.getSignatureUUID())) {
					updateTransporter(transporter);
					break;
				}
			}
		}
		return data(context).transporters.get(uuid);
	}

	/** Loaded ship cores whose configured regions contain the supplied block. */
	public static List<ShipCoreTileEntity> getContainingShips(final ServerWorld world,
	                                                          final BlockPos blockPos) {
		final ArrayList<ShipCoreTileEntity> result = new ArrayList<>();
		for (final ShipCoreTileEntity shipCore : liveSnapshot()) {
			if (shipCore.isRemoved() || shipCore.getLevel() != world) continue;
			final int[] bounds = shipCore.getShipBounds();
			if (contains(bounds, blockPos)) result.add(shipCore);
		}
		return result;
	}

	/** A player is crew only when every overlapping ship accepts them, matching the legacy rule. */
	public static boolean isCrewMember(final ServerWorld world, final BlockPos blockPos,
	                                   final PlayerEntity player) {
		final List<ShipCoreTileEntity> ships = getContainingShips(world, blockPos);
		if (ships.isEmpty()) return false;
		for (final ShipCoreTileEntity ship : ships) {
			if (!ship.isCrewMember(player)) return false;
		}
		return true;
	}

	/** Returns known radar echoes in canonical coordinates, across all three celestial layers. */
	public static List<RadarEcho> getRadarEchoes(final ServerWorld radarWorld,
	                                             final BlockPos radarPos,
	                                             final int radius) {
		// Refresh loaded providers first so name, bounds, mass and isolation are current.
		for (final ShipCoreTileEntity shipCore : liveSnapshot()) {
			if (!shipCore.isRemoved() && shipCore.getLevel() instanceof ServerWorld) {
				shipCore.refreshGlobalRegion();
			}
		}

		final CelestialCoordinates.UniversalPosition radar = CelestialCoordinates.toUniversal(
			radarWorld, radarPos.getX(), radarPos.getY(), radarPos.getZ());
		if (radar == null) return Collections.emptyList();
		final double radiusSquared = (double) radius * radius;
		final ArrayList<RadarEcho> echoes = new ArrayList<>();
		for (final ShipRegion region : new ArrayList<>(data(radarWorld).regions.values())) {
			if (!region.type.hasRadarEcho()) continue;
			final CelestialCoordinates.UniversalPosition position = region.universalPosition();
			if (position == null || radar.distanceSquaredTo(position) > radiusSquared) continue;
			if (region.isolationRate > 0.0D
			 && radarWorld.random.nextDouble() < region.isolationRate) continue;
			echoes.add(new RadarEcho(region.type.getName(), region.name,
				position.x, position.y, position.z, region.mass));
		}
		return echoes;
	}

	private static ArrayList<ShipCoreTileEntity> liveSnapshot() {
		synchronized (LIVE_SHIPS) { return new ArrayList<>(LIVE_SHIPS); }
	}

	private static boolean contains(final int[] bounds, final BlockPos blockPos) {
		return bounds[0] <= blockPos.getX() && blockPos.getX() <= bounds[3]
			&& bounds[1] <= blockPos.getY() && blockPos.getY() <= bounds[4]
			&& bounds[2] <= blockPos.getZ() && blockPos.getZ() <= bounds[5];
	}

	private static RegistryData data(final ServerWorld world) {
		final ServerWorld overworld = world.getServer().getLevel(World.OVERWORLD);
		final ServerWorld storageWorld = overworld == null ? world : overworld;
		return storageWorld.getDataStorage().computeIfAbsent(RegistryData::new, DATA_NAME);
	}

	private static final class RegistryData extends WorldSavedData {
		private final Map<UUID, ShipRegion> regions = new LinkedHashMap<>();
		private final Map<UUID, TransporterLocation> transporters = new LinkedHashMap<>();

		private RegistryData() { super(DATA_NAME); }

		@Override
		public void load(final CompoundNBT tag) {
			regions.clear();
			final ListNBT list = tag.getList(TAG_REGIONS, Constants.NBT.TAG_COMPOUND);
			for (int index = 0; index < list.size(); index++) {
				final ShipRegion region = new ShipRegion(list.getCompound(index));
				if (region.type != GlobalRegionType.UNDEFINED) regions.put(region.uuid, region);
			}
			transporters.clear();
			final ListNBT transporterList = tag.getList(TAG_TRANSPORTERS,
				Constants.NBT.TAG_COMPOUND);
			for (int index = 0; index < transporterList.size(); index++) {
				final TransporterLocation location =
					new TransporterLocation(transporterList.getCompound(index));
				transporters.put(location.uuid, location);
			}
		}

		@Override
		public CompoundNBT save(final CompoundNBT tag) {
			final ListNBT list = new ListNBT();
			for (final ShipRegion region : regions.values()) list.add(region.save());
			tag.put(TAG_REGIONS, list);
			final ListNBT transporterList = new ListNBT();
			for (final TransporterLocation location : transporters.values()) {
				transporterList.add(location.save());
			}
			tag.put(TAG_TRANSPORTERS, transporterList);
			return tag;
		}
	}

	/** Persistent, dimension-safe address used by transporter cores and beacons. */
	public static final class TransporterLocation {
		public final UUID uuid;
		public final ResourceLocation dimension;
		public final BlockPos position;
		public final String name;

		private TransporterLocation(final TransporterCoreTileEntity transporter) {
			uuid = transporter.getSignatureUUID();
			dimension = transporter.getLevel().dimension().location();
			position = transporter.getBlockPos().immutable();
			name = transporter.getSignatureName();
		}

		private TransporterLocation(final CompoundNBT tag) {
			uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : UUID.randomUUID();
			final ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("dimension"));
			dimension = parsed == null ? World.OVERWORLD.location() : parsed;
			position = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
			name = tag.getString("name");
		}

		private CompoundNBT save() {
			final CompoundNBT tag = new CompoundNBT();
			tag.putUUID("uuid", uuid);
			tag.putString("dimension", dimension.toString());
			tag.putInt("x", position.getX());
			tag.putInt("y", position.getY());
			tag.putInt("z", position.getZ());
			tag.putString("name", name == null ? "" : name);
			return tag;
		}
	}

	private static final class ShipRegion {
		private final GlobalRegionType type;
		private final UUID uuid;
		private final ResourceLocation dimension;
		private final BlockPos corePos;
		private final int[] bounds;
		private final String name;
		private final int mass;
		private final double isolationRate;

		private ShipRegion(final ShipCoreTileEntity shipCore) {
			type = GlobalRegionType.SHIP;
			uuid = shipCore.getSignatureUUID();
			dimension = shipCore.getLevel().dimension().location();
			corePos = shipCore.getBlockPos().immutable();
			bounds = shipCore.getShipBounds();
			name = shipCore.getName();
			mass = shipCore.getKnownShipMass();
			isolationRate = shipCore.getIsolationRate();
		}

		private ShipRegion(final CompoundNBT tag) {
			final GlobalRegionType loadedType = GlobalRegionType.byName(tag.getString("type"));
			type = loadedType == null ? GlobalRegionType.UNDEFINED : loadedType;
			uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : UUID.randomUUID();
			final ResourceLocation loadedDimension = ResourceLocation.tryParse(tag.getString("dimension"));
			dimension = loadedDimension == null ? World.OVERWORLD.location() : loadedDimension;
			corePos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
			bounds = new int[]{ tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"),
				tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ") };
			name = tag.getString("name");
			mass = Math.max(0, tag.getInt("mass"));
			isolationRate = Math.max(0.0D, Math.min(1.0D, tag.getDouble("isolationRate")));
		}

		@Nullable
		private CelestialCoordinates.UniversalPosition universalPosition() {
			return CelestialCoordinates.toUniversal(dimension,
				corePos.getX(), corePos.getY(), corePos.getZ());
		}

		private CompoundNBT save() {
			final CompoundNBT tag = new CompoundNBT();
			tag.putString("type", type.getName());
			tag.putUUID("uuid", uuid);
			tag.putString("dimension", dimension.toString());
			tag.putInt("x", corePos.getX());
			tag.putInt("y", corePos.getY());
			tag.putInt("z", corePos.getZ());
			tag.putInt("minX", bounds[0]);
			tag.putInt("minY", bounds[1]);
			tag.putInt("minZ", bounds[2]);
			tag.putInt("maxX", bounds[3]);
			tag.putInt("maxY", bounds[4]);
			tag.putInt("maxZ", bounds[5]);
			tag.putString("name", name == null ? "" : name);
			tag.putInt("mass", mass);
			tag.putDouble("isolationRate", isolationRate);
			return tag;
		}
	}

	public static final class RadarEcho {
		public final String type;
		public final String name;
		public final double x;
		public final double y;
		public final double z;
		public final int mass;

		private RadarEcho(final String type, final String name,
		                  final double x, final double y, final double z, final int mass) {
			this.type = type;
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.mass = mass;
		}
	}
}
