package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumReactorOutputMode implements IStringSerializable {
	
	OFF        ("off"),
	UNLIMITED  ("unlimited"),
	ABOVE      ("above"),
	AT_RATE    ("at_rate");
	
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumReactorOutputMode> ID_MAP = new HashMap<>();
	
	static {
		length = EnumReactorOutputMode.values().length;
		for (final EnumReactorOutputMode enumReactorOutputMode : values()) {
			ID_MAP.put(enumReactorOutputMode.ordinal(), enumReactorOutputMode);
		}
	}
	
	EnumReactorOutputMode(final String name) {
		this.name = name;
	}
	
	public static EnumReactorOutputMode get(final int ordinal) {
		return ID_MAP.get(ordinal);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
	
	public static EnumReactorOutputMode byName(@Nonnull final String name) {
		for (final EnumReactorOutputMode enumReactorOutputMode : values()) {
			if (enumReactorOutputMode.getName().equalsIgnoreCase(name)) {
				return enumReactorOutputMode;
			}
		}
		return null;
	}
}
