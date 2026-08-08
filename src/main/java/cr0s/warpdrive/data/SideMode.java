package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Per-face energy routing, from 1.12.2 EnumDisabledInputOutput.
 *
 * A wrench cycles a face through these. The indices are the 1.12.2 ones and are written to NBT, so
 * they must not be reordered.
 */
public enum SideMode implements IStringSerializable {

	DISABLED(0),
	INPUT(1),
	OUTPUT(2);

	private static final SideMode[] BY_INDEX = { DISABLED, INPUT, OUTPUT };

	private final int index;

	SideMode(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public static SideMode byIndex(final int index) {
		return index >= 0 && index < BY_INDEX.length ? BY_INDEX[index] : DISABLED;
	}

	/** Wrench without sneaking. */
	public SideMode getNext() {
		return BY_INDEX[(index + 1) % BY_INDEX.length];
	}

	/** Wrench while sneaking. */
	public SideMode getPrevious() {
		return BY_INDEX[(index + BY_INDEX.length - 1) % BY_INDEX.length];
	}

	@Nonnull
	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
