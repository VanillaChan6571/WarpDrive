package cr0s.warpdrive.block.energy;

import javax.annotation.Nullable;
import java.util.Locale;

/** The four legacy reactor output policies. */
public enum EnanReactorOutputMode {
	OFF,
	UNLIMITED,
	ABOVE,
	AT_RATE;

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	@Nullable
	public static EnanReactorOutputMode byName(final String name) {
		if (name == null) return null;
		for (final EnanReactorOutputMode mode : values()) {
			if (mode.getName().equalsIgnoreCase(name)) return mode;
		}
		return null;
	}
}
