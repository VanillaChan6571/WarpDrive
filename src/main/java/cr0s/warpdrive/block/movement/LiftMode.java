package cr0s.warpdrive.block.movement;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

/** Operating and computer-control modes from 1.12.2 {@code EnumLiftMode}. */
public enum LiftMode implements IStringSerializable {

	INACTIVE("inactive"),
	UP("up"),
	DOWN("down"),
	REDSTONE("redstone");

	private final String name;

	LiftMode(final String name) {
		this.name = name;
	}

	public static LiftMode byIndex(final int index) {
		final LiftMode[] values = values();
		return index >= 0 && index < values.length ? values[index] : INACTIVE;
	}

	@Nonnull
	@Override
	public String getSerializedName() {
		return name;
	}
}
