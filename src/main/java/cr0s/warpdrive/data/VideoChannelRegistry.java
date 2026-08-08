package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.detection.CameraTileEntity;
import cr0s.warpdrive.block.weapon.LaserCameraTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-local registry of loaded camera endpoints.
 *
 * Entries are supplied by ticking camera tile entities and are never allowed to load a chunk.
 * This retains the old registry semantics while avoiding a server-wide singleton leaking state
 * between dimensions or integrated-server sides.
 */
public final class VideoChannelRegistry {

	private static final Map<Key, Endpoint> ENDPOINTS = new LinkedHashMap<>();

	private VideoChannelRegistry() { }

	public static void update(final World world, final BlockPos blockPos,
	                          final int videoChannel, final CameraType cameraType) {
		if (!world.isClientSide) return;
		final Key key = new Key(world.dimension().location().toString(), blockPos.asLong());
		if (!IVideoChannel.isValid(videoChannel)) {
			ENDPOINTS.remove(key);
			return;
		}
		ENDPOINTS.put(key, new Endpoint(key.dimension, blockPos.immutable(), videoChannel, cameraType));
	}

	public static void remove(final World world, final BlockPos blockPos) {
		if (!world.isClientSide) return;
		ENDPOINTS.remove(new Key(world.dimension().location().toString(), blockPos.asLong()));
	}

	@Nullable
	public static Endpoint find(final World world, final int videoChannel) {
		if (!world.isClientSide || !IVideoChannel.isValid(videoChannel)) return null;
		final String dimension = world.dimension().location().toString();
		final Iterator<Map.Entry<Key, Endpoint>> iterator = ENDPOINTS.entrySet().iterator();
		while (iterator.hasNext()) {
			final Map.Entry<Key, Endpoint> entry = iterator.next();
			if (!entry.getKey().dimension.equals(dimension)) continue;
			if (!isAlive(world, entry.getValue())) {
				iterator.remove();
				continue;
			}
			if (entry.getValue().videoChannel == videoChannel) return entry.getValue();
		}
		return null;
	}

	public static boolean isAlive(final World world, final Endpoint endpoint) {
		if (!world.isClientSide
		 || !endpoint.dimension.equals(world.dimension().location().toString())
		 || !world.hasChunkAt(endpoint.blockPos)) return false;
		final TileEntity tileEntity = world.getBlockEntity(endpoint.blockPos);
		if (!(tileEntity instanceof IVideoChannel)
		 || ((IVideoChannel) tileEntity).getVideoChannel() != endpoint.videoChannel) return false;
		return endpoint.cameraType == CameraType.SIMPLE
		     ? tileEntity instanceof CameraTileEntity
		     : tileEntity instanceof LaserCameraTileEntity;
	}

	public static void clear() {
		ENDPOINTS.clear();
	}

	public static final class Endpoint {
		private final String dimension;
		private final BlockPos blockPos;
		private final int videoChannel;
		private final CameraType cameraType;

		private Endpoint(final String dimension, final BlockPos blockPos, final int videoChannel,
		                 final CameraType cameraType) {
			this.dimension = dimension;
			this.blockPos = blockPos;
			this.videoChannel = videoChannel;
			this.cameraType = cameraType;
		}

		public BlockPos getBlockPos() { return blockPos; }
		public int getVideoChannel() { return videoChannel; }
		public CameraType getCameraType() { return cameraType; }
	}

	private static final class Key {
		private final String dimension;
		private final long position;

		private Key(final String dimension, final long position) {
			this.dimension = dimension;
			this.position = position;
		}

		@Override
		public boolean equals(final Object object) {
			if (this == object) return true;
			if (!(object instanceof Key)) return false;
			final Key key = (Key) object;
			return position == key.position && dimension.equals(key.dimension);
		}

		@Override
		public int hashCode() {
			return 31 * dimension.hashCode() + Long.hashCode(position);
		}
	}
}
