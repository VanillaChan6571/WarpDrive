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

	PLANET_MOVING      ( 50, 0.1D),
	PLANET_TAKEOFF     ( 50, 0.1D),
	PLANET_LANDING     ( 50, 0.1D),
	SPACE_MOVING       (100, 0.1D),
	HYPERSPACE_MOVING  (200, 0.5D),
	HYPERSPACE_ENTERING(100, 0.1D),
	HYPERSPACE_EXITING (100, 0.1D);

	/** Shortest meaningful jump. Anything less is a no-op that would still cost energy. */
	public static final int MINIMUM_DISTANCE_BLOCKS = 1;

	private final int baseDistance;
	private final double perMass;

	ShipMovementType(final int baseDistance, final double perMass) {
		this.baseDistance = baseDistance;
		this.perMass = perMass;
	}

	/** Maximum jump distance in blocks for a ship of this mass. */
	public int maximumDistance(final int shipMass) {
		return (int) Math.ceil(baseDistance + perMass * Math.max(0, shipMass));
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT).replace('_', ' ');
	}
}
