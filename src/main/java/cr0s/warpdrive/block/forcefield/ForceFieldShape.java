package cr0s.warpdrive.block.forcefield;

import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** The seven legacy projector shapes, calculated without touching world state. */
public enum ForceFieldShape implements IStringSerializable {

	NONE,
	SPHERE,
	CYLINDER_H,
	CYLINDER_V,
	CUBE,
	PLANE,
	TUBE,
	TUNNEL;

	@Nonnull
	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static ForceFieldShape fromRegistrySuffix(final String suffix) {
		for (final ForceFieldShape shape : values()) {
			if (shape.getSerializedName().equals(suffix)) {
				return shape;
			}
		}
		return NONE;
	}

	/**
	 * Recreates EnumForceFieldShape#getVertexes. Values are projector-local coordinates; the
	 * projector applies its facing, optional rotation and translation afterwards.
	 */
	public Geometry calculate(final int minX, final int minY, final int minZ,
	                          final int maxX, final int maxY, final int maxZ,
	                          final float thickness, final boolean includeInterior) {
		final Set<BlockPos> perimeter = new HashSet<>();
		final Set<BlockPos> interior = new HashSet<>();
		final float halfThickness = thickness / 2.0F;
		float radius;
		float radiusInterior2;
		float radiusPerimeter2;

		switch (this) {
		case SPHERE:
			radius = maxY;
			radiusInterior2 = square(radius - halfThickness);
			radiusPerimeter2 = square(radius + halfThickness);
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					for (int z = minZ; z <= maxZ; z++) {
						classify(x, y, z, x * x + y * y + z * z,
							radiusInterior2, radiusPerimeter2, includeInterior, perimeter, interior);
					}
				}
			}
			break;

		case CYLINDER_H:
			radius = (maxY + maxZ) / 2.0F;
			radiusInterior2 = square(radius - halfThickness);
			radiusPerimeter2 = square(radius + halfThickness);
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					final int distance2 = y * y + z * z;
					for (int x = minX; x <= maxX; x++) {
						classify(x, y, z, distance2, radiusInterior2, radiusPerimeter2,
							includeInterior, perimeter, interior);
					}
				}
			}
			break;

		case CYLINDER_V:
			radius = (maxX + maxY) / 2.0F;
			radiusInterior2 = square(radius - halfThickness);
			radiusPerimeter2 = square(radius + halfThickness);
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					final int distance2 = x * x + y * y;
					for (int z = minZ; z <= maxZ; z++) {
						classify(x, y, z, distance2, radiusInterior2, radiusPerimeter2,
							includeInterior, perimeter, interior);
					}
				}
			}
			break;

		case TUBE:
			radius = (maxX + maxZ) / 2.0F;
			radiusInterior2 = square(radius - halfThickness);
			radiusPerimeter2 = square(radius + halfThickness);
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					final int distance2 = x * x + z * z;
					for (int y = minY; y <= maxY; y++) {
						classify(x, y, z, distance2, radiusInterior2, radiusPerimeter2,
							includeInterior, perimeter, interior);
					}
				}
			}
			break;

		case CUBE:
		case PLANE:
		case TUNNEL:
			for (int y = minY; y <= maxY; y++) {
				final boolean yFace = Math.abs(y - minY) <= halfThickness
				                   || Math.abs(y - maxY) <= halfThickness;
				for (int x = minX; x <= maxX; x++) {
					final boolean xFace = Math.abs(x - minX) <= halfThickness
					                   || Math.abs(x - maxX) <= halfThickness;
					for (int z = minZ; z <= maxZ; z++) {
						final boolean zFace = Math.abs(z - minZ) <= halfThickness
						                   || Math.abs(z - maxZ) <= halfThickness;
						final boolean isPerimeter = this == CUBE ? xFace || yFace || zFace
							: this == PLANE ? yFace : xFace || zFace;
						if (isPerimeter) {
							perimeter.add(new BlockPos(x, y, z));
						} else if (includeInterior) {
							interior.add(new BlockPos(x, y, z));
						}
					}
				}
			}
			break;

		case NONE:
		default:
			break;
		}
		return new Geometry(perimeter, interior);
	}

	private static float square(final float value) {
		return value * value;
	}

	private static void classify(final int x, final int y, final int z, final int distance2,
	                             final float interior2, final float perimeter2,
	                             final boolean includeInterior, final Set<BlockPos> perimeter,
	                             final Set<BlockPos> interior) {
		if (distance2 > perimeter2) {
			return;
		}
		final BlockPos blockPos = new BlockPos(x, y, z);
		if (distance2 >= interior2) {
			perimeter.add(blockPos);
		} else if (includeInterior) {
			interior.add(blockPos);
		}
	}

	public static final class Geometry {
		public final Set<BlockPos> perimeter;
		public final Set<BlockPos> interior;

		private Geometry(final Set<BlockPos> perimeter, final Set<BlockPos> interior) {
			this.perimeter = perimeter;
			this.interior = interior;
		}
	}
}
