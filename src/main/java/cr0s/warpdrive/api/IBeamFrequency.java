package cr0s.warpdrive.api;

import net.minecraft.util.math.vector.Vector3f;

/**
 * A machine whose beam is tuned to a frequency - lasers, force field projectors, transporters.
 *
 * Ported verbatim from 1.12.2, including the colour curve: frequency maps to a visible spectrum so
 * two machines on the same frequency also draw the same colour, which is how players read a
 * network at a glance.
 */
public interface IBeamFrequency {

	/** Reserved frequency used by scanning beams. */
	int BEAM_FREQUENCY_SCANNING = 1420;
	int BEAM_FREQUENCY_MIN = 0;
	int BEAM_FREQUENCY_MAX = 65000;
	String BEAM_FREQUENCY_TAG = "beamFrequency";

	static boolean isValid(final int beamFrequency) {
		return beamFrequency <= BEAM_FREQUENCY_MAX
		    && beamFrequency >  BEAM_FREQUENCY_MIN;
	}

	/** @return the current beam frequency, or -1 when unset or invalid. */
	int getBeamFrequency();

	void setBeamFrequency(final int beamFrequency);

	/**
	 * Beam colour for a frequency: red through violet, then a banded "rainbow" above 60000.
	 * 1.12.2 returned its own Vector3; this returns Vector3f, which is what 1.16.5 rendering wants.
	 */
	static Vector3f getBeamColor(final int beamFrequency) {
		final float r;
		final float g;
		final float b;
		if (beamFrequency <= BEAM_FREQUENCY_MIN) {          // invalid frequency
			r = 1.0F;
			g = 0.0F;
			b = 0.0F;
		} else if (beamFrequency <= 10000) {                // red
			r = 1.0F;
			g = 0.0F;
			b = 0.5F * beamFrequency / 10000F;
		} else if (beamFrequency <= 20000) {                // orange
			r = 1.0F;
			g = (beamFrequency - 10000F) / 10000F;
			b = 0.5F - 0.5F * (beamFrequency - 10000F) / 10000F;
		} else if (beamFrequency <= 30000) {                // yellow
			r = 1.0F - (beamFrequency - 20000F) / 10000F;
			g = 1.0F;
			b = 0.0F;
		} else if (beamFrequency <= 40000) {                // green
			r = 0.0F;
			g = 1.0F - (beamFrequency - 30000F) / 10000F;
			b = (beamFrequency - 30000F) / 10000F;
		} else if (beamFrequency <= 50000) {                // blue
			r = 0.5F * (beamFrequency - 40000F) / 10000F;
			g = 0.0F;
			b = 1.0F - 0.5F * (beamFrequency - 40000F) / 10000F;
		} else if (beamFrequency <= 60000) {                // violet
			r = 0.5F + 0.5F * (beamFrequency - 50000F) / 10000F;
			g = 0.0F;
			b = 0.5F - 0.5F * (beamFrequency - 50000F) / 10000F;
		} else if (beamFrequency <= BEAM_FREQUENCY_MAX) {   // rainbow
			final int component = Math.round(4096F * (beamFrequency - 60000F)
			                               / (BEAM_FREQUENCY_MAX - 60000F));
			r = 1.0F - 0.5F * (component & 0xF);
			g = 0.5F + 0.5F * (component >> 4 & 0xF);
			b = 0.5F + 0.5F * (component >> 8 & 0xF);
		} else {                                            // invalid frequency
			r = 1.0F;
			g = 0.0F;
			b = 0.0F;
		}
		return new Vector3f(r, g, b);
	}
}
