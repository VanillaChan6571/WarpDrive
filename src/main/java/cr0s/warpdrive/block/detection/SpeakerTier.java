package cr0s.warpdrive.block.detection;

import java.util.Locale;

/** Speaker chat ranges from 1.12.2 {@code SPEAKER_RANGE_BLOCKS_BY_TIER}. */
public enum SpeakerTier {

	BASIC(16.0F),
	ADVANCED(32.0F),
	SUPERIOR(64.0F);

	private final float range;

	SpeakerTier(final float range) {
		this.range = range;
	}

	public float getRange() {
		return range;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
