package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IForceFieldShape;
import cr0s.warpdrive.config.WarpDriveConfig;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public enum EnumForceFieldShape implements IStringSerializable, IForceFieldShape {
	
	NONE               ("none"),
	SPHERE             ("sphere"),
	CYLINDER_H         ("cylinder_h"),
	CYLINDER_V         ("cylinder_v"),
	CUBE               ("cube"),
	PLANE              ("plane"),
	TUBE               ("tube"),
	TUNNEL             ("tunnel");
	
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumForceFieldShape> ID_MAP = new HashMap<>();
	
	static {
		length = EnumForceFieldShape.values().length;
		for (final EnumForceFieldShape forceFieldShape : values()) {
			ID_MAP.put(forceFieldShape.ordinal(), forceFieldShape);
		}
	}
	
	EnumForceFieldShape(final String name) {
		this.name = name;
	}
	
	public static EnumForceFieldShape get(final int damage) {
		return ID_MAP.get(damage);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Map<VectorI, Boolean> getVertexes(final ForceFieldSetup forceFieldSetup) {
		final VectorI vScale = forceFieldSetup.vMax.clone().translateBack(forceFieldSetup.vMin);
		final boolean isFusionOrInverted = forceFieldSetup.hasFusion || forceFieldSetup.isInverted;
		final float thickness = Commons.clamp(1.0F, 2.0F, forceFieldSetup.thickness);
		final int sizeEstimation;
		if (!isFusionOrInverted) {// surface only
			// plane surface           is r^2
			// sphere surface          is 4*PI*r^2
			// open cylinder surface   is 2*PI*r * 2*r
			// cube surface            is 4*6*r^2
			// in practice, 'r' is adjusted to account for overlaps and rounding
			final int maxRadius = (int) Math.ceil(Math.max(vScale.x, Math.max(vScale.y, vScale.z)) / 2.0F);
			switch(this) {
			case SPHERE:
				sizeEstimation = (int) Math.ceil(thickness * 4 * Math.PI * maxRadius * (maxRadius + 1));
				break;
			
			case CYLINDER_H:
			case CYLINDER_V:
			case TUBE:
				sizeEstimation = (int) Math.ceil(thickness * 4 * Math.PI * (maxRadius + 2) * (maxRadius + 2));
				break;
			
			case CUBE:
				sizeEstimation = (int) Math.ceil((int) thickness * 6 * (2 * maxRadius) * (2 * maxRadius + 1));
				break;
			
			case TUNNEL:
				sizeEstimation = (int) Math.ceil((int) thickness * 4 * (2 * maxRadius) * (2 * maxRadius + 1));
				break;
			
			case PLANE:
				sizeEstimation = (int) Math.ceil((int) thickness * 2 * (2 * maxRadius + 1) * (2 * maxRadius + 1));
				break;
			
			default:
				sizeEstimation = 8;
				WarpDrive.logger.error(String.format("Invalid object %s for shape %s with size %s. Please report this to the mod author",
				                                     this,
				                                     name,
				                                     vScale));
				break;
			}
		} else {
			sizeEstimation = (vScale.x + 1) * (vScale.y + 1) * (vScale.z + 1);
		}
		final Map<VectorI, Boolean> mapVertexes = new HashMap<>(sizeEstimation);
		
		final float radius;
		final float halfThickness = forceFieldSetup.thickness / 2.0F;
		final float radiusInterior2;
		final float radiusPerimeter2;
		final VectorI vCenter;
		boolean isPerimeter;
		switch(this) {
		case SPHERE:
			radius = forceFieldSetup.vMax.y;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				final int y2 = (y - vCenter.y) * (y - vCenter.y);
				for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
					final int x2 = (x - vCenter.x) * (x - vCenter.x);
					for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
						final int z2 = (z - vCenter.z) * (z - vCenter.z);
						if (x2 + y2 + z2 <= radiusPerimeter2) {
							isPerimeter = x2 + y2 + z2 >= radiusInterior2;
							if (isPerimeter || isFusionOrInverted) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case CYLINDER_H:
			radius = (forceFieldSetup.vMax.y + forceFieldSetup.vMax.z) / 2.0F;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				final int y2 = (y - vCenter.y) * (y - vCenter.y);
				for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
					final int z2 = (z - vCenter.z) * (z - vCenter.z);
					if (y2 + z2 <= radiusPerimeter2) {
						isPerimeter = y2 + z2 >= radiusInterior2;
						if (isPerimeter || isFusionOrInverted) {
							for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case CYLINDER_V:
			radius = (forceFieldSetup.vMax.x + forceFieldSetup.vMax.y) / 2.0F;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
				final int x2 = (x - vCenter.x) * (x - vCenter.x);
				for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
					final int y2 = (y - vCenter.y) * (y - vCenter.y);
					if (x2 + y2 <= radiusPerimeter2) {
						isPerimeter = x2 + y2 >= radiusInterior2;
						if (isPerimeter || isFusionOrInverted) {
							for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case TUBE:
			radius =(forceFieldSetup.vMax.x + forceFieldSetup.vMax.z) / 2.0F;
			radiusInterior2 = (radius - halfThickness) * (radius - halfThickness);
			radiusPerimeter2 = (radius + halfThickness) * (radius + halfThickness);
			vCenter = new VectorI(0, 0, 0);
			for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
				final int x2 = (x - vCenter.x) * (x - vCenter.x);
				for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
					final int z2 = (z - vCenter.z) * (z - vCenter.z);
					if (x2 + z2 <= radiusPerimeter2) {
						isPerimeter = x2 + z2 >= radiusInterior2;
						if (isPerimeter || isFusionOrInverted) {
							for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
								mapVertexes.put(new VectorI(x, y, z), isPerimeter);
							}
						}
					}
				}
			}
			break;
		
		case CUBE:
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				final boolean yFace = Math.abs(y - forceFieldSetup.vMin.y) <= halfThickness
				                   || Math.abs(y - forceFieldSetup.vMax.y) <= halfThickness;
				for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
					final boolean xFace = Math.abs(x - forceFieldSetup.vMin.x) <= halfThickness
					                   || Math.abs(x - forceFieldSetup.vMax.x) <= halfThickness;
					for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
						final boolean zFace = Math.abs(z - forceFieldSetup.vMin.z) <= halfThickness
						                   || Math.abs(z - forceFieldSetup.vMax.z) <= halfThickness;
						isPerimeter = xFace || yFace || zFace;
						if (isPerimeter || isFusionOrInverted) {
							mapVertexes.put(new VectorI(x, y, z), isPerimeter);
						}
					}
				}
			}
			break;
		
		case PLANE:
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				isPerimeter = Math.abs(y - forceFieldSetup.vMin.y) <= halfThickness
				           || Math.abs(y - forceFieldSetup.vMax.y) <= halfThickness;
				if (isPerimeter || isFusionOrInverted) {
					for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
						for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
							mapVertexes.put(new VectorI(x, y, z), isPerimeter);
						}
					}
				}
			}
			break;
		
		case TUNNEL:
			for (int y = forceFieldSetup.vMin.y; y <= forceFieldSetup.vMax.y; y++) {
				for (int x = forceFieldSetup.vMin.x; x <= forceFieldSetup.vMax.x; x++) {
					final boolean xFace = Math.abs(x - forceFieldSetup.vMin.x) <= halfThickness
					                   || Math.abs(x - forceFieldSetup.vMax.x) <= halfThickness;
					for (int z = forceFieldSetup.vMin.z; z <= forceFieldSetup.vMax.z; z++) {
						isPerimeter = xFace
						           || Math.abs(z - forceFieldSetup.vMin.z) <= halfThickness
						           || Math.abs(z - forceFieldSetup.vMax.z) <= halfThickness;
						if (isPerimeter || isFusionOrInverted) {
							mapVertexes.put(new VectorI(x, y, z), isPerimeter);
						}
					}
				}
			}
			break;
		
		default:
			break;
			
		}
		
		if (mapVertexes.size() > sizeEstimation) {
			WarpDrive.logger.warn(String.format("Underestimated memory allocation lag %d > %d for shape %s with size %s, isFusionOrInverted %s, thickness %.2f. Please report this to the mod author",
			                                    mapVertexes.size(), sizeEstimation,
			                                    name,
			                                    vScale,
			                                    isFusionOrInverted,
			                                    forceFieldSetup.thickness));
		} else if (WarpDriveConfig.LOGGING_PROFILING_MEMORY_ALLOCATION) {
			if (mapVertexes.size() * 1.25 < sizeEstimation) {
				WarpDrive.logger.warn(String.format("Overestimated memory allocation %d < %d for shape %s with size %s, isFusionOrInverted %s, thickness %.2f. Please report this to the mod author",
				                                    mapVertexes.size(), sizeEstimation,
				                                    name,
				                                    vScale,
				                                    isFusionOrInverted,
				                                    forceFieldSetup.thickness));
			} else {
				WarpDrive.logger.warn(String.format("Memory allocation is good: %d vs %d for shape %s with size %s, isFusionOrInverted %s, thickness %.2f. Please report this to the mod author",
				                                    mapVertexes.size(), sizeEstimation,
				                                    name,
				                                    vScale,
				                                    isFusionOrInverted,
				                                    forceFieldSetup.thickness));
			}
		}
		
		return mapVertexes;
	}
}
