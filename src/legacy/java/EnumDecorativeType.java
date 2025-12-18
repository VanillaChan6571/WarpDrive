package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumDecorativeType implements IStringSerializable {
	
	PLAIN               ("plain"                , 0),
	GRATED              ("grated"               , 1),
	GLASS               ("glass"                , 2),
	STRIPES_BLACK_DOWN  ("stripes_black_down"   , 3),
	STRIPES_BLACK_UP    ("stripes_black_up"     , 4),
	STRIPES_YELLOW_DOWN ("stripes_yellow_down"  , 5),
	STRIPES_YELLOW_UP   ("stripes_yellow_up"    , 6),
	;
	
	private final String name;
	private final int metadata;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumDecorativeType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumDecorativeType.values().length;
		for (final EnumDecorativeType enumDecorativeType : values()) {
			ID_MAP.put(enumDecorativeType.getMetadata(), enumDecorativeType);
		}
	}
	
	EnumDecorativeType(final String name, final int metadata) {
		this.name = name;
		this.metadata = metadata;
	}
	
	public static EnumDecorativeType byMetadata(final int metadata) {
		return ID_MAP.get(metadata);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
	
	public int getMetadata() {
		return metadata;
	}
}
