package cr0s.warpdrive.data;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IStringSerializable;

public enum EnumTooltipCondition implements IStringSerializable {
	
	NEVER             ("never"            ),
	ON_SNEAK          ("on_sneak"         ),
	ADVANCED_TOOLTIPS ("advanced_tooltips"),
	CREATIVE_ONLY     ("creative_only"    ),
	ALWAYS            ("always"           );
	
	public final String name;
	
	EnumTooltipCondition(final String name) {
		this.name = name;
	}
	
	public boolean isEnabled(final boolean isSneaking, final boolean isCreativeMode) {
		switch(this) {
		case NEVER: return false;
		case ON_SNEAK: return isSneaking;
		case ADVANCED_TOOLTIPS: return Minecraft.getMinecraft().gameSettings.advancedItemTooltips;
		case CREATIVE_ONLY: return isCreativeMode;
		case ALWAYS: return true;
		default: return false;
		}
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
	
	public static String formatAllValues() {
		final StringBuilder result = new StringBuilder();
		for (final EnumTooltipCondition tooltipCondition : EnumTooltipCondition.values()) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append(tooltipCondition);
		}
		return result.toString();
	}
}
