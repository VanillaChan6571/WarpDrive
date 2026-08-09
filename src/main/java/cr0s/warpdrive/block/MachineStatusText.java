package cr0s.warpdrive.block;

import net.minecraft.util.text.Color;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
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

	public static IFormattableTextComponent name(final ITextComponent machineName) {
		return prefix(machineName);
	}

	public static IFormattableTextComponent status(final ITextComponent machineName,
	                                               final String state) {
		return status(machineName, new StringTextComponent(state));
	}

	public static IFormattableTextComponent status(final ITextComponent machineName,
	                                               final ITextComponent state) {
		return prefix(machineName).append(state.copy().withStyle(STYLE_CORRECT));
	}

	public static IFormattableTextComponent stateAndEnergy(final ITextComponent machineName,
	                                                        final String state,
	                                                        final int energyStored,
	                                                        final int maximumEnergyStored) {
		return stateAndEnergy(machineName, new StringTextComponent(state),
			energyStored, maximumEnergyStored);
	}

	public static IFormattableTextComponent stateAndEnergy(final ITextComponent machineName,
	                                                        final ITextComponent state,
	                                                        final int energyStored,
	                                                        final int maximumEnergyStored) {
		final IFormattableTextComponent status = status(machineName, state);
		appendCharge(status, energyStored, maximumEnergyStored);
		return status;
	}

	public static IFormattableTextComponent laser(final ITextComponent machineName,
	                                              final int beamFrequency,
	                                              @Nullable final Integer videoChannel,
	                                              final int energyStored,
	                                              final int maximumEnergyStored) {
		final IFormattableTextComponent status = prefix(machineName);
		appendBeamFrequency(status, beamFrequency);
		if (videoChannel != null) {
			appendVideoChannel(status, videoChannel);
		}
		if (maximumEnergyStored > 0) {
			appendCharge(status, energyStored, maximumEnergyStored);
		}
		return status;
	}

	public static IFormattableTextComponent virtualAssistant(final ITextComponent machineName,
	                                                         @Nullable final String lastCommand,
	                                                         final int energyStored,
	                                                         final int maximumEnergyStored) {
		final ITextComponent commandStatus = lastCommand == null || lastCommand.isEmpty()
			? new TranslationTextComponent("warpdrive.virtual_assistant.status_line.none")
			: new TranslationTextComponent("warpdrive.virtual_assistant.status_line.last_command",
				new StringTextComponent(lastCommand).withStyle(STYLE_VALUE));
		return stateAndEnergy(machineName, commandStatus, energyStored, maximumEnergyStored);
	}

	public static IFormattableTextComponent controlChannel(final ITextComponent machineName,
	                                                       final int controlChannel) {
		final String key;
		final Style style;
		if (controlChannel == -1) {
			key = "warpdrive.control_channel.status_line.undefined";
			style = STYLE_WARNING;
		} else if (controlChannel < 0) {
			key = "warpdrive.control_channel.status_line.invalid";
			style = STYLE_WARNING;
		} else {
			key = "warpdrive.control_channel.status_line.valid";
			style = STYLE_CORRECT;
		}
		return prefix(machineName).append(
			new TranslationTextComponent(key, value(controlChannel)).withStyle(style));
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

	public static IFormattableTextComponent miningLaser(final ITextComponent machineName,
	                                                    final String stateTranslationKey,
	                                                    final boolean insufficientEnergy,
	                                                    final int energyStored,
	                                                    final int maximumEnergyStored,
	                                                    final int pumpCount) {
		final IFormattableTextComponent status = prefix(machineName);
		status.append(new TranslationTextComponent(stateTranslationKey).withStyle(STYLE_CORRECT));
		if (insufficientEnergy) {
			status.append(new TranslationTextComponent(
				"warpdrive.mining_laser.status_line._insufficient_energy").withStyle(STYLE_WARNING));
		}
		appendCharge(status, energyStored, maximumEnergyStored);
		appendNewLine(status);
		status.append(new TranslationTextComponent("warpdrive.status.pumps",
			value(pumpCount), value(20)).withStyle(STYLE_NORMAL));
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
		appendNewLine(status);
		status.append(new TranslationTextComponent("warpdrive.status.charge",
			value(energyStored), value(maximumEnergyStored)).withStyle(STYLE_NORMAL));
	}

	private static void appendBeamFrequency(final IFormattableTextComponent status,
	                                        final int beamFrequency) {
		final String key;
		final Style style;
		if (beamFrequency == -1) {
			key = "warpdrive.beam_frequency.status_line.undefined";
			style = STYLE_WARNING;
		} else if (beamFrequency < 0) {
			key = "warpdrive.beam_frequency.status_line.invalid";
			style = STYLE_WARNING;
		} else {
			key = "warpdrive.beam_frequency.status_line.valid";
			style = STYLE_CORRECT;
		}
		status.append(new TranslationTextComponent(key, value(beamFrequency)).withStyle(style));
	}

	private static void appendVideoChannel(final IFormattableTextComponent status,
	                                       final int videoChannel) {
		appendNewLine(status);
		final String key;
		final Style style;
		if (videoChannel == -1) {
			key = "warpdrive.video_channel.status_line.undefined";
			style = STYLE_WARNING;
		} else if (videoChannel < 0) {
			key = "warpdrive.video_channel.status_line.invalid";
			style = STYLE_WARNING;
		} else {
			key = "warpdrive.video_channel.status_line.valid_self";
			style = STYLE_CORRECT;
		}
		status.append(new TranslationTextComponent(key, value(videoChannel)).withStyle(style));
	}

	private static void appendNewLine(final IFormattableTextComponent status) {
		status.append(new StringTextComponent("\n").withStyle(STYLE_NORMAL));
	}

	public static IFormattableTextComponent value(final int amount) {
		return new StringTextComponent(String.format(Locale.ROOT, "%,d", amount))
			.withStyle(STYLE_VALUE);
	}

	public static IFormattableTextComponent value(final String text) {
		return new StringTextComponent(text).withStyle(STYLE_VALUE);
	}

	private static Style color(final int rgb) {
		return Style.EMPTY.withColor(Color.fromRgb(rgb));
	}
}
