package cr0s.warpdrive.block.detection;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum RadarMode implements IStringSerializable {
	INACTIVE("inactive"),
	ACTIVE("active"),
	SCANNING("scanning");

	private final String name;
	RadarMode(final String name) { this.name = name; }
	@Nonnull @Override public String getSerializedName() { return name; }
}
