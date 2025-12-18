package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumShipScannerState implements IStringSerializable {
	
	IDLE          (0, "idle"),           // Ready for next command
	SCANNING      (1, "scanning"),       // Scanning a ship
	DEPLOYING     (2, "online");         // Deploying a ship
	
	private final int metadata;
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumShipScannerState> ID_MAP = new HashMap<>();
	
	static {
		length = EnumShipScannerState.values().length;
		for (final EnumShipScannerState shipScannerState : values()) {
			ID_MAP.put(shipScannerState.ordinal(), shipScannerState);
		}
	}
	
	EnumShipScannerState(final int metadata, final String name) {
		this.metadata = metadata;
		this.name = name;
	}
	
	public int getMetadata() {
		return metadata;
	}
	
	public static EnumShipScannerState get(final int id) {
		return ID_MAP.get(id);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
}
