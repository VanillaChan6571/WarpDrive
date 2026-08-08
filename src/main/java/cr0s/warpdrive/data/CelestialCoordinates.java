package cr0s.warpdrive.data;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Canonical coordinates shared by the Overworld, space and hyperspace.
 *
 * <p>The 1.12.2 celestial map obtained this transform by walking an XML parent tree. This port
 * deliberately gives every world its own dimension, so only two pieces of that transform remain:
 * the vertical layer and Minecraft's dimension coordinate scale. Keeping those rules here makes
 * radar, ship transitions and entity transitions agree exactly.</p>
 */
public final class CelestialCoordinates {

	public static final String GALAXY_NAME = "Hyperspace";
	private static final double WORLD_HEIGHT = 256.0D;

	private CelestialCoordinates() { }

	/** Resolve a local point into the common coordinate system, or null for an unrelated dimension. */
	@Nullable
	public static UniversalPosition toUniversal(final World world,
	                                            final double x, final double y, final double z) {
		return toUniversal(world.dimension().location(), x, y, z);
	}

	/** Pure overload used by tests and by persisted global-region entries. */
	@Nullable
	public static UniversalPosition toUniversal(final ResourceLocation dimension,
	                                            final double x, final double y, final double z) {
		final Layer layer = Layer.byDimension(dimension);
		if (layer == null) return null;
		return new UniversalPosition(
			GALAXY_NAME, layer.displayName,
			x * layer.coordinateScale,
			y + layer.verticalOffset,
			z * layer.coordinateScale);
	}

	/**
	 * Map one horizontal block coordinate between celestial layers.
	 *
	 * <p>Flooring matches vanilla portal coordinate conversion, especially for negative values.</p>
	 */
	public static int mapHorizontal(final World source, final World destination, final int coordinate) {
		return mapHorizontal(source.dimension().location(), destination.dimension().location(), coordinate);
	}

	public static int mapHorizontal(final ResourceLocation source, final ResourceLocation destination,
	                                final int coordinate) {
		return MathHelper.floor(mapHorizontal(source, destination, (double) coordinate));
	}

	public static double mapHorizontal(final World source, final World destination,
	                                   final double coordinate) {
		return mapHorizontal(source.dimension().location(), destination.dimension().location(), coordinate);
	}

	public static double mapHorizontal(final ResourceLocation source, final ResourceLocation destination,
	                                   final double coordinate) {
		final Layer sourceLayer = Layer.byDimension(source);
		final Layer destinationLayer = Layer.byDimension(destination);
		if (sourceLayer == null || destinationLayer == null) return coordinate;
		return coordinate * sourceLayer.coordinateScale / destinationLayer.coordinateScale;
	}

	public static double coordinateScale(final World world) {
		final Layer layer = Layer.byDimension(world.dimension().location());
		return layer == null ? 1.0D : layer.coordinateScale;
	}

	private enum Layer {
		OVERWORLD(new ResourceLocation(DimensionAltitude.OVERWORLD), "Overworld", 1.0D, 0.0D),
		SPACE(new ResourceLocation(DimensionAltitude.SPACE), "Space", 1.0D, WORLD_HEIGHT),
		HYPERSPACE(new ResourceLocation(DimensionAltitude.HYPERSPACE), "Hyperspace", 8.0D,
			2.0D * WORLD_HEIGHT);

		private final ResourceLocation dimension;
		private final String displayName;
		private final double coordinateScale;
		private final double verticalOffset;

		Layer(final ResourceLocation dimension, final String displayName,
		      final double coordinateScale, final double verticalOffset) {
			this.dimension = dimension;
			this.displayName = displayName;
			this.coordinateScale = coordinateScale;
			this.verticalOffset = verticalOffset;
		}

		@Nullable
		private static Layer byDimension(final ResourceLocation dimension) {
			for (final Layer layer : values()) {
				if (layer.dimension.equals(dimension)) return layer;
			}
			return null;
		}
	}

	public static final class UniversalPosition {
		public final String galaxyName;
		public final String celestialName;
		public final double x;
		public final double y;
		public final double z;

		private UniversalPosition(final String galaxyName, final String celestialName,
		                          final double x, final double y, final double z) {
			this.galaxyName = galaxyName;
			this.celestialName = celestialName;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public double distanceSquaredTo(final UniversalPosition other) {
			final double dx = other.x - x;
			final double dy = other.y - y;
			final double dz = other.z - z;
			return dx * dx + dy * dy + dz * dz;
		}
	}
}
