package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumStructureGroup implements IStringSerializable {
	
	STARS            ("star"           , true),
	MOONS            ("moon"           , true),
	GAS_CLOUDS       ("gascloud"       , true),
	ASTEROIDS        ("asteroid"       , true),
	ASTEROIDS_FIELDS ("asteroids_field", true),
	;
	
	private final String name;
	private final boolean isRequired;
	
	// cached values
	public static final int length;
	private static final HashMap<String, EnumStructureGroup> ID_MAP = new HashMap<>();
	
	static {
		length = EnumStructureGroup.values().length;
		for (final EnumStructureGroup enumFrameType : values()) {
			ID_MAP.put(enumFrameType.getName(), enumFrameType);
		}
	}
	
	EnumStructureGroup(final String name, final boolean isRequired) {
		this.name = name;
		this.isRequired = isRequired;
	}
	
	public static EnumStructureGroup byName(final String name) {
		return ID_MAP.get(name);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
	
	public boolean isRequired() {
		return isRequired;
	}
}
