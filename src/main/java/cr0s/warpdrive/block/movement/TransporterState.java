package cr0s.warpdrive.block.movement;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

/** The four persisted states and model variants of the legacy transporter core. */
public enum TransporterState implements IStringSerializable {
	DISABLED("disabled"),
	IDLE("idle"),
	ACQUIRING("acquiring"),
	ENERGIZING("energizing");

	private final String name;

	TransporterState(final String name) {
		this.name = name;
	}

	@Nonnull
	@Override
	public String getSerializedName() {
		return name;
	}

	public static TransporterState byName(final String name) {
		for (final TransporterState state : values()) {
			if (state.name.equals(name)) return state;
		}
		return DISABLED;
	}
}
