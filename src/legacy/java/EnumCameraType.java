package cr0s.warpdrive.data;

import cr0s.warpdrive.block.TileEntityLaser;
import cr0s.warpdrive.block.detection.TileEntityCamera;

import java.util.HashMap;

public enum EnumCameraType {
	
	SIMPLE_CAMERA   (TileEntityCamera.class),
	LASER_CAMERA    (TileEntityLaser.class);
	
	public final Class<?> clazz;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumCameraType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumCameraType.values().length;
		for (final EnumCameraType cameraType : values()) {
			ID_MAP.put(cameraType.ordinal(), cameraType);
		}
	}
	
	EnumCameraType(final Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public static EnumCameraType get(final int id) {
		return ID_MAP.get(id);
	}
}
