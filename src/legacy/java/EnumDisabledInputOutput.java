package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumDisabledInputOutput implements IStringSerializable {
	
	DISABLED ("disabled",  0),
	INPUT    ("input"   ,  1),
	OUTPUT   ("output"  ,  2);
	
	private final String name;
	private final int index;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumDisabledInputOutput> ID_MAP = new HashMap<>();
	
	static {
		length = EnumDisabledInputOutput.values().length;
		for (final EnumDisabledInputOutput disabledInputOutput : values()) {
			ID_MAP.put(disabledInputOutput.index, disabledInputOutput);
		}
	}
	
	EnumDisabledInputOutput(final String name, final int index) {
		this.name = name;
		this.index = index;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public int getIndex() {
		return index;
	}
	
	public EnumDisabledInputOutput getNext() {
		return get((index + 1) % 3);
	}
	
	public EnumDisabledInputOutput getPrevious() {
		return get((index + 2) % 3);
	}
	
	public static EnumDisabledInputOutput get(final int index) {
		return ID_MAP.get(index);
	}
}
