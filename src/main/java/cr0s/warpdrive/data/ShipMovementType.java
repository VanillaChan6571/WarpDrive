package cr0s.warpdrive.data;

import java.util.Locale;

/**
 * How far a ship may move in one jump, by the kind of movement it is making.
 *
 * Ported from 1.12.2 EnumShipMovementType and its cost table. The distances there are the first two
 * factors of a five-term formula; the remaining three are zero for every movement type that ships
 * actually use, so this keeps the linear part:
 *
 *   maximumDistance = ceil(base + perMass * mass)
 *
 * The dependence on mass is the interesting bit and easy to mistake for a typo: a bigger ship jumps
 * FURTHER, not less far. Range is something you build for.
 *
 * Hyperspace is the long-haul medium - twice the base range and five times the mass scaling of
 * normal space - while anything involving a planet is deliberately short, so leaving or approaching
 * a world is a series of deliberate hops rather than one leap.
 */
public enum ShipMovementType {

	//                    range A/B    energy A       B     C    D    E
	PLANET_MOVING      ( 50, 0.1D,          100D, 10D, 100D, 0D, 0D),
	PLANET_TAKEOFF     ( 50, 0.1D,       10_000D, 10D, 100D, 0D, 0D),
	PLANET_LANDING     ( 50, 0.1D,       10_000D, 10D, 100D, 0D, 0D),
	SPACE_MOVING       (100, 0.1D,        1_000D, 10D, 100D, 0D, 0D),
	HYPERSPACE_MOVING  (200, 0.5D,       10_000D,  1D,  10D, 0D, 0D),
	HYPERSPACE_ENTERING(100, 0.1D,   10_000_000D,  0D,   0D, 0D, 0D),
	HYPERSPACE_EXITING (100, 0.1D,   10_000_000D,  0D,   0D, 0D, 0D);

	/** Shortest meaningful jump. Anything less is a no-op that would still cost energy. */
	public static final int MINIMUM_DISTANCE_BLOCKS = 1;

	private final int baseDistance;
	private final double rangePerMass;
	private final double energyBase;
	private final double energyPerMass;
	private final double energyPerDistance;
	private final double energyLogMass;
	private final double energyExponentialScale;

	ShipMovementType(final int baseDistance, final double rangePerMass,
	                 final double energyBase, final double energyPerMass,
	                 final double energyPerDistance, final double energyLogMass,
	                 final double energyExponentialScale) {
		this.baseDistance = baseDistance;
		this.rangePerMass = rangePerMass;
		this.energyBase = energyBase;
		this.energyPerMass = energyPerMass;
		this.energyPerDistance = energyPerDistance;
		this.energyLogMass = energyLogMass;
		this.energyExponentialScale = energyExponentialScale;
	}

	/** Maximum jump distance in blocks for a ship of this mass. */
	public int maximumDistance(final int shipMass) {
		return (int) Math.ceil(baseDistance + rangePerMass * Math.max(0, shipMass));
	}

	/**
	 * Energy cost ported from 1.12.2 ShipMovementCosts. The five configurable legacy factors are
	 * kept as fields even though the stock movement types use zero for the exponential term; this
	 * prevents another "simplified" formula from silently changing game balance later.
	 */
	public int energyRequired(final int shipMass, final int distance) {
		final int mass = Math.max(0, shipMass);
		final int blocks = Math.max(0, distance);
		final double exponential = energyExponentialScale == 0.0D
			? 1.0D : Math.exp(blocks / energyExponentialScale);
		final double cost = energyBase
			+ energyPerMass * mass
			+ energyPerDistance * blocks
			+ energyLogMass * Math.log(Math.max(1.0D, mass)) * exponential;
		if (!Double.isFinite(cost) || cost >= Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return Math.max(0, (int) Math.ceil(cost));
	}

	/** Largest distance affordable with the supplied energy, capped to Minecraft's world width. */
	public int maximumDistanceForEnergy(final int shipMass, final int availableEnergy) {
		if (availableEnergy < energyRequired(shipMass, 0)) {
			return 0;
		}
		int low = 0;
		int high = 30_000_000;
		while (low < high) {
			final int middle = low + (high - low + 1) / 2;
			if (energyRequired(shipMass, middle) <= availableEnergy) {
				low = middle;
			} else {
				high = middle - 1;
			}
		}
		return low;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}
}
