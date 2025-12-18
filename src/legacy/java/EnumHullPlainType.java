package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumHullPlainType implements IStringSerializable {
	
	PLAIN               ("plain"),
	TILED               ("tiled"),
	;
	
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumHullPlainType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumHullPlainType.values().length;
		for (final EnumHullPlainType hullPlainType : values()) {
			ID_MAP.put(hullPlainType.ordinal(), hullPlainType);
		}
	}
	
	EnumHullPlainType(final String name) {
		this.name = name;
	}
	
	public static EnumHullPlainType get(final int index) {
		return ID_MAP.get(index);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
}
