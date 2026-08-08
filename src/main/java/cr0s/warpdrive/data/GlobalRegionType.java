package cr0s.warpdrive.data;

import javax.annotation.Nullable;

/** Types persisted by the global-region registry, preserving the 1.12.2 names. */
public enum GlobalRegionType {
	UNDEFINED("-undefined-", true),
	SHIP("ship", true),
	JUMP_GATE("jump_gate", true),
	PLANET("planet", true),
	STAR("star", true),
	STRUCTURE("structure", true),
	WARP_ECHO("warp_echo", true),
	ACCELERATOR("accelerator", false),
	TRANSPORTER("transporter", false),
	VIRTUAL_ASSISTANT("virtual_assistant", false),
	REACTOR("reactor", false);

	private final String name;
	private final boolean radarEcho;

	GlobalRegionType(final String name, final boolean radarEcho) {
		this.name = name;
		this.radarEcho = radarEcho;
	}

	public String getName() { return name; }
	public boolean hasRadarEcho() { return radarEcho; }

	@Nullable
	public static GlobalRegionType byName(final String name) {
		for (final GlobalRegionType type : values()) {
			if (type.name.equals(name)) return type;
		}
		return null;
	}
}
