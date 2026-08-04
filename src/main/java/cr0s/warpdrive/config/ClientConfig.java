package cr0s.warpdrive.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side settings, written to config/warpdrive-client.toml in the game directory.
 *
 * HUD layout lives here rather than in a world or server config because it is a per-user display
 * preference: screen size, GUI scale and whatever other HUD mods are installed all differ per
 * player, so no single default can be right for everyone.
 */
public final class ClientConfig {

	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.BooleanValue AIR_TIMER_VISIBLE;
	public static final ForgeConfigSpec.IntValue AIR_TIMER_OFFSET_X;
	public static final ForgeConfigSpec.IntValue AIR_TIMER_OFFSET_Y;

	static {
		final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		builder.comment("Heads-up display").push("hud");

		AIR_TIMER_VISIBLE = builder
			.comment("Show the remaining air time above the air gauge.",
			         "The gauge itself is unaffected and stays visible either way.")
			.define("air_timer_visible", true);

		AIR_TIMER_OFFSET_X = builder
			.comment("Horizontal shift of the air timer, in GUI pixels. Positive moves it right.",
			         "By default the timer is centred just above the air gauge, which sits close to",
			         "where the held item's name appears when you change tools. Shift it if the two",
			         "overlap at your GUI scale.")
			.defineInRange("air_timer_offset_x", 0, -1000, 1000);

		AIR_TIMER_OFFSET_Y = builder
			.comment("Vertical shift of the air timer, in GUI pixels. Positive moves it down.")
			.defineInRange("air_timer_offset_y", 0, -1000, 1000);

		builder.pop();

		SPEC = builder.build();
	}

	private ClientConfig() {
	}
}
