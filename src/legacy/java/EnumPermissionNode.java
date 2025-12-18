package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumPermissionNode implements IStringSerializable {
	
	NONE               ("none"),
	ENABLE             ("enable"),
	OPEN_GUI           ("open_gui"),
	MODIFY             ("modify"),
	SNEAK_THROUGH      ("sneak_through"),
	;
	
	public final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumPermissionNode> ID_MAP = new HashMap<>();
	
	static {
		length = EnumPermissionNode.values().length;
		for (final EnumPermissionNode permissionNode : values()) {
			ID_MAP.put(permissionNode.ordinal(), permissionNode);
		}
	}
	
	EnumPermissionNode(final String name) {
		this.name = name;
	}
	
	public static EnumPermissionNode get(final int id) {
		return ID_MAP.get(id);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
}
