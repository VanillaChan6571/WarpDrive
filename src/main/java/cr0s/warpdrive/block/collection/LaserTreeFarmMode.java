package cr0s.warpdrive.block.collection;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum LaserTreeFarmMode implements IStringSerializable {
	INACTIVE("inactive"),
	FARMING_LOW_POWER("farming_low_power"),
	FARMING_POWERED("farming_powered"),
	SCANNING_LOW_POWER("scanning_low_power"),
	SCANNING_POWERED("scanning_powered"),
	PLANTING_LOW_POWER("planting_low_power"),
	PLANTING_POWERED("planting_powered");

	private final String name;
	LaserTreeFarmMode(final String name) { this.name = name; }
	@Nonnull @Override public String getSerializedName() { return name; }
}
