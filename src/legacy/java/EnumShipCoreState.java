package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumShipCoreState implements IStringSerializable {
	
	// DISCONNECTED  (0, "disconnected"),   // Not connected to controller
	IDLE          (1, "idle"),           // Ready for next command
	// SCANNING      (2, "scanning"),       // Ready for next command
	EXECUTE       (3, "execute"),        // Computing parameters
	WARMING_UP    (4, "warming_up");     // Warm up phase
	// COOLING_DOWN  (5, "cooling_down");   // Pending cool down
	
	private final int metadata;
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumShipCoreState> ID_MAP = new HashMap<>();
	
	static {
		length = EnumShipCoreState.values().length;
		for (final EnumShipCoreState shipCoreState : values()) {
			ID_MAP.put(shipCoreState.ordinal(), shipCoreState);
		}
	}
	
	EnumShipCoreState(final int metadata, final String name) {
		this.metadata = metadata;
		this.name = name;
	}
	
	public int getMetadata() {
		return metadata;
	}
	
	public static EnumShipCoreState get(final int id) {
		return ID_MAP.get(id);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
}
