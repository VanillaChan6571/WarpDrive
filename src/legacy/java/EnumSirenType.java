package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumSirenType implements IStringSerializable {
	
	INDUSTRIAL("industrial", 0),
	RAID      ("raid"      , 4);
	
	private final String name;
	private final int index;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumSirenType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumSirenType.values().length;
		for (final EnumSirenType sirenType : values()) {
			ID_MAP.put(sirenType.index, sirenType);
		}
	}
	
	EnumSirenType(final String name, final int index) {
		this.name = name;
		this.index = index;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static EnumSirenType get(final int index) {
		return ID_MAP.get(index);
	}
	
	public int getIndex() {
		return index;
	}
}
