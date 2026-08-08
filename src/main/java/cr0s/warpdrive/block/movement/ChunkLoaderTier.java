package cr0s.warpdrive.block.movement;

import java.util.Locale;

/** The three legacy registry/art tiers; chunk-loading balance is intentionally shared. */
public enum ChunkLoaderTier {
	BASIC(1),
	ADVANCED(2),
	SUPERIOR(3);

	private final int index;

	ChunkLoaderTier(final int index) {
		this.index = index;
	}

	public int getIndex() { return index; }
	public String getName() { return name().toLowerCase(Locale.ROOT); }
}
