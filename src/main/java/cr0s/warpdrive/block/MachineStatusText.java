package cr0s.warpdrive.block;

import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Locale;

/** Colored, multiline machine status text replacing the shared 1.12 status builder. */
public final class MachineStatusText {

	static final int COLOR_HEADER = 0xFFAA00;
	static final int COLOR_CORRECT = 0x55FF55;
	static final int COLOR_NORMAL = 0xFFFFFF;
	static final int COLOR_VALUE = 0xFFFF55;
	static final int COLOR_WARNING = 0xFF5555;

	private static final Style STYLE_HEADER = color(COLOR_HEADER);
	private static final Style STYLE_CORRECT = color(COLOR_CORRECT);
	private static final Style STYLE_NORMAL = color(COLOR_NORMAL);
	private static final Style STYLE_VALUE = color(COLOR_VALUE);
	private static final Style STYLE_WARNING = color(COLOR_WARNING);

	private MachineStatusText() { }

	public static IFormattableTextComponent energy(final ITextComponent machineName,
	                                               final int energyStored,
	                                               final int maximumEnergyStored) {
		final IFormattableTextComponent status = prefix(machineName);
		appendCharge(status, energyStored, maximumEnergyStored);
		return status;
	}

	public static IFormattableTextComponent laserTreeFarm(final ITextComponent machineName,
	                                                      final String stateTranslationKey,
	                                                      final boolean insufficientEnergy,
	                                                      final int energyStored,
	                                                      final int maximumEnergyStored,
	                                                      final int harvested) {
		final IFormattableTextComponent status = prefix(machineName);
		status.append(new TranslationTextComponent(stateTranslationKey).withStyle(STYLE_CORRECT));
		if (insufficientEnergy) {
			status.append(new TranslationTextComponent(
				"warpdrive.laser_tree_farm.status_line._insufficient_energy").withStyle(STYLE_WARNING));
		}
		appendCharge(status, energyStored, maximumEnergyStored);
		status.append(new StringTextComponent("\n").withStyle(STYLE_NORMAL));
		status.append(new TranslationTextComponent("warpdrive.status.harvested",
			value(harvested)).withStyle(STYLE_NORMAL));
		return status;
	}

	private static IFormattableTextComponent prefix(final ITextComponent machineName) {
		return new StringTextComponent("")
			.append(machineName.copy().withStyle(STYLE_HEADER))
			.append(new StringTextComponent(": ").withStyle(STYLE_NORMAL));
	}

	private static void appendCharge(final IFormattableTextComponent status,
	                                 final int energyStored,
	                                 final int maximumEnergyStored) {
		status.append(new StringTextComponent("\n").withStyle(STYLE_NORMAL));
		status.append(new TranslationTextComponent("warpdrive.status.charge",
			value(energyStored), value(maximumEnergyStored)).withStyle(STYLE_NORMAL));
	}

	private static IFormattableTextComponent value(final int amount) {
		return new StringTextComponent(String.format(Locale.ROOT, "%,d", Math.max(0, amount)))
			.withStyle(STYLE_VALUE);
	}

	private static Style color(final int rgb) {
		return Style.EMPTY.withColor(Color.fromRgb(rgb));
	}
}
