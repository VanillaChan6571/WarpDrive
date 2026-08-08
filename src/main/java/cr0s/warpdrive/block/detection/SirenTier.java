package cr0s.warpdrive.block.detection;

import java.util.Locale;

/** Siren audible ranges from 1.12.2 {@code SIREN_RANGE_BLOCKS_BY_TIER}. */
public enum SirenTier {

	BASIC(32.0F),
	ADVANCED(64.0F),
	SUPERIOR(128.0F);

	private final float range;

	SirenTier(final float range) {
		this.range = range;
	}

	public float getRange() {
		return range;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
