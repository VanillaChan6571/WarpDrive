package cr0s.warpdrive.block.atomic;

import java.util.Locale;

/** The three legacy accelerator construction tiers. */
public enum AcceleratorTier {

	BASIC(1),
	ADVANCED(2),
	SUPERIOR(3);

	private final int index;

	AcceleratorTier(final int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
