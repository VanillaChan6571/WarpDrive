package cr0s.warpdrive.block.detection;

import java.util.Locale;

/** Legacy listening range, FE buffer and configured running cost by assistant tier. */
public enum VirtualAssistantTier {

	BASIC(32.0F, 10_000, 10),
	ADVANCED(64.0F, 30_000, 40),
	SUPERIOR(128.0F, 100_000, 160);

	private final float range;
	private final int capacity;
	private final int energyPerTick;

	VirtualAssistantTier(final float range, final int capacity, final int energyPerTick) {
		this.range = range;
		this.capacity = capacity;
		this.energyPerTick = energyPerTick;
	}

	public float getRange() { return range; }
	public int getCapacity() { return capacity; }
	public int getEnergyPerTick() { return energyPerTick; }
	public int getLegacyIndex() { return ordinal() + 1; }
	public String getName() { return name().toLowerCase(Locale.ROOT); }
}
