package cr0s.warpdrive.block.collection;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum MiningLaserMode implements IStringSerializable {
	INACTIVE("inactive"),
	SCANNING_LOW_POWER("scanning_low_power"),
	SCANNING_POWERED("scanning_powered"),
	MINING_LOW_POWER("mining_low_power"),
	MINING_POWERED("mining_powered");

	private final String name;
	MiningLaserMode(final String name) { this.name = name; }
	@Nonnull @Override public String getSerializedName() { return name; }
}
