package cr0s.warpdrive.block.forcefield;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.Locale;

/** Upgrade compatibility and coefficients copied from EnumForceFieldUpgrade. */
public enum ForceFieldUpgrade implements IStringSerializable {

	NONE         (0, 0, 0.0F,     0.0F,  0.000F, 0.000F, 0.000F, 0.000F,    0.0F, 0.000F,  0.000F,  0.0F),
	ATTRACTION   (0, 1, 1.0F,     4.0F,  0.000F, 0.000F, 0.000F, 0.000F,   50.0F, 0.150F,  0.000F,  8.0F),
	BREAKING     (0, 1, 1.0F,    25.0F,  0.400F, 0.500F, 0.020F, 0.150F,  700.0F, 0.080F,  4.000F,  0.0F),
	CAMOUFLAGE   (0, 1, 1.0F,     3.0F,  0.600F, 0.850F, 0.700F, 0.950F, 1000.0F, 3.000F,  7.000F,  0.0F),
	COOLING      (3, 1, 30.0F,  300.0F,  0.000F, 0.000F, 0.900F, 0.900F,  150.0F, 0.060F,  1.500F, 40.0F),
	FUSION       (1, 1, 1.0F,     1.0F,  0.000F, 0.000F, 0.000F, 0.000F, 1000.0F, 0.040F,  0.150F,  0.0F),
	HEATING      (3, 1, 100.0F,10000.0F,  0.000F, 0.000F, 0.900F, 0.900F,  150.0F, 0.300F,  3.000F, 25.0F),
	INVERSION    (1, 0, 1.0F,     1.0F,  1.250F, 1.250F, 0.000F, 0.000F, 1500.0F, 0.150F,  0.150F, 20.0F),
	ITEM_PORT    (0, 1, 1.0F,    10.0F,  0.000F, 0.000F, 0.950F, 0.900F,   50.0F, 0.120F,  0.500F,  2.0F),
	PUMPING      (0, 1, 2500.0F,50000.0F,  0.800F, 1.000F, 0.400F, 1.000F,  800.0F, 0.150F,  4.500F,  0.0F),
	RANGE        (4, 1, 8.0F,   128.0F,  1.150F, 0.450F, 1.150F, 0.450F,   10.0F, 0.300F,  0.750F, 12.0F),
	REPULSION    (0, 1, 1.0F,     4.0F,  0.000F, 0.000F, 0.000F, 0.000F,   50.0F, 0.150F,  0.000F,  5.0F),
	ROTATION     (1, 0, 1.0F,     1.0F,  0.000F, 0.000F, 0.000F, 0.000F,  100.0F, 0.000F,  0.000F,  0.0F),
	SHOCK        (0, 1, 1.0F,    10.0F,  0.800F, 0.800F, 0.800F, 0.800F,  300.0F, 0.600F,  4.000F, 30.0F),
	SILENCER     (1, 0, 1.0F,     1.0F,  0.000F, 0.000F, 0.000F, 0.000F,    0.0F, 0.120F,  0.620F,  0.0F),
	SPEED        (4, 1, 1.0F,    20.0F,  1.250F, 6.000F, 1.200F, 5.000F,  200.0F, 0.135F,  1.250F, 15.0F),
	STABILIZATION(0, 1, 1.0F,     9.0F,  0.250F, 0.850F, 0.025F, 0.450F,  400.0F, 0.050F, 73.600F,  0.0F),
	THICKNESS    (5, 1, 0.2F,     1.0F,  0.800F, 1.600F, 0.900F, 1.500F,  100.0F, 0.400F,  2.200F,  5.0F),
	TRANSLATION  (1, 0, 1.0F,     1.0F,  0.000F, 0.000F, 0.000F, 0.000F,  100.0F, 0.000F,  0.000F,  0.0F);

	private final int projectorLimit;
	private final int relayLimit;
	private final float baseValue;
	private final float valueCap;
	private final float scanOffset;
	private final float scanSlope;
	private final float placeOffset;
	private final float placeSlope;
	private final float startupCost;
	private final float scanCost;
	private final float placeCost;
	private final float entityCost;

	ForceFieldUpgrade(final int projectorLimit, final int relayLimit,
	                  final float baseValue, final float valueCap,
	                  final float scanMinimum, final float scanMaximum,
	                  final float placeMinimum, final float placeMaximum,
	                  final float startupCost, final float scanCost,
	                  final float placeCost, final float entityCost) {
		this.projectorLimit = projectorLimit;
		this.relayLimit = relayLimit;
		this.baseValue = baseValue;
		this.valueCap = valueCap;
		scanSlope = valueCap == baseValue ? 0.0F : (scanMaximum - scanMinimum) / (valueCap - baseValue);
		scanOffset = scanMinimum - scanSlope * baseValue;
		placeSlope = valueCap == baseValue ? 0.0F : (placeMaximum - placeMinimum) / (valueCap - baseValue);
		placeOffset = placeMinimum - placeSlope * baseValue;
		final float divisor = baseValue == 0.0F ? 1.0F : baseValue;
		this.startupCost = startupCost / divisor;
		this.scanCost = scanCost / divisor;
		this.placeCost = placeCost / divisor;
		this.entityCost = entityCost / divisor;
	}

	public int getProjectorLimit() { return projectorLimit; }
	public int getRelayLimit() { return relayLimit; }
	public float getBaseValue() { return baseValue; }
	public float scale(final float value) { return Math.min(valueCap, value); }
	public float getScanFactor(final float value) { return scanOffset + scanSlope * value; }
	public float getPlaceFactor(final float value) { return placeOffset + placeSlope * value; }
	public float getStartupCost(final float value) { return startupCost * value; }
	public float getScanCost(final float value) { return scanCost * value; }
	public float getPlaceCost(final float value) { return placeCost * value; }
	public float getEntityCost(final float value) { return entityCost * value; }

	@Nonnull
	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static ForceFieldUpgrade fromRegistrySuffix(final String suffix) {
		for (final ForceFieldUpgrade upgrade : values()) {
			if (upgrade.getSerializedName().equals(suffix)) {
				return upgrade;
			}
		}
		return NONE;
	}
}
