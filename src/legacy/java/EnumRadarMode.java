package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumRadarMode implements IStringSerializable {
	
	INACTIVE			("inactive"),
	ACTIVE				("active"),
	SCANNING			("scanning");
	
	private final String unlocalizedName;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumRadarMode> ID_MAP = new HashMap<>();
	
	static {
		length = EnumRadarMode.values().length;
		for (final EnumRadarMode radarMode : values()) {
			ID_MAP.put(radarMode.ordinal(), radarMode);
		}
	}
	
	EnumRadarMode(final String unlocalizedName) {
		this.unlocalizedName = unlocalizedName;
	}
	
	public static EnumRadarMode get(final int damage) {
		return ID_MAP.get(damage);
	}

	public String getTranslationKey() {
		return unlocalizedName;
	}
	
	@Nonnull
	@Override
	public String getName() { return unlocalizedName; }
}
