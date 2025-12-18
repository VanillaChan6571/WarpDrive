package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumTransporterState implements IStringSerializable {
	
	DISABLED      (0, "disabled"),    // disabled
	IDLE          (1, "idle"),        // enabling, waiting for lock
	ACQUIRING     (2, "acquiring"),   // acquiring lock
	ENERGIZING    (3, "energizing");  // transferring entities
	
	private final int metadata;
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumTransporterState> ID_MAP = new HashMap<>();
	
	static {
		length = EnumTransporterState.values().length;
		for (final EnumTransporterState transporterState : values()) {
			ID_MAP.put(transporterState.ordinal(), transporterState);
		}
	}
	
	EnumTransporterState(final int metadata, final String name) {
		this.metadata = metadata;
		this.name = name;
	}
	
	public int getMetadata() {
		return metadata;
	}
	
	public static EnumTransporterState get(final int id) {
		return ID_MAP.get(id);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
}
