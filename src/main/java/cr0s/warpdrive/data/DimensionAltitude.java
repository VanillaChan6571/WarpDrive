package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * The vertical stack of dimensions, and where you arrive when you leave one through the top or the
 * bottom.
 *
 * <pre>
 *   hyperspace
 *       ^ up / down v
 *     space
 *       ^ up / down v
 *   overworld
 * </pre>
 *
 * Leaving through the ceiling puts you near the floor of the world above; leaving through the floor
 * puts you near the ceiling of the world below. That symmetry is the whole model - it means a
 * descent and the ascent that undoes it are geometrically consistent, so a ship that takes off and
 * immediately lands ends up roughly where it started.
 *
 * The 1.12.2 XML map packed several worlds into shared dimensions. This port intentionally uses
 * one full dimension per layer instead, so the relationship is fixed and global. Horizontal
 * coordinate scaling and universal positions live in {@link CelestialCoordinates}.
 */
public final class DimensionAltitude {

	public static final String OVERWORLD = "minecraft:overworld";
	public static final String SPACE = "warpdrive:space";
	public static final String HYPERSPACE = "warpdrive:hyperspace";

	public static final RegistryKey<World> SPACE_KEY = key("space");
	public static final RegistryKey<World> HYPERSPACE_KEY = key("hyperspace");

	/**
	 * Clearance kept from the boundary on arrival.
	 *
	 * Large enough that a ship never materialises clipped into the ceiling or floor it just came
	 * through, and that an entity has room to react before drifting back out.
	 */
	public static final int ENTRY_MARGIN = 10;

	private static RegistryKey<World> key(final String path) {
		return RegistryKey.create(Registry.DIMENSION_REGISTRY,
			new ResourceLocation(WarpDrive.MODID, path));
	}

	private DimensionAltitude() {
	}

	public static String idOf(final World world) {
		return world.dimension().location().toString();
	}

	/** The dimension reached by leaving this one through the ceiling, or null if there is none. */
	@Nullable
	public static RegistryKey<World> above(final World world) {
		switch (idOf(world)) {
			case OVERWORLD: return SPACE_KEY;
			case SPACE:     return HYPERSPACE_KEY;
			default:        return null;   // nothing above hyperspace
		}
	}

	/** The dimension reached by leaving this one through the floor, or null if there is none. */
	@Nullable
	public static RegistryKey<World> below(final World world) {
		switch (idOf(world)) {
			case HYPERSPACE: return SPACE_KEY;
			case SPACE:      return World.OVERWORLD;
			default:         return null;   // nothing below the overworld
		}
	}

	public static boolean isSpaceOrHyperspace(final World world) {
		final String id = idOf(world);
		return SPACE.equals(id) || HYPERSPACE.equals(id);
	}

	/**
	 * Lowest usable altitude in a world. Read from the world rather than hardcoded to 0, so a
	 * dimension with a different build range - or a future version that moves the floor - does not
	 * silently place arrivals out of bounds.
	 */
	public static int floorOf(final World world) {
		return 0;
	}

	/** Highest usable altitude in a world, from its actual build height rather than a fixed 255. */
	public static int ceilingOf(final World world) {
		return world.getMaxBuildHeight() - 1;
	}

	/** Arrival altitude for something entering through the floor, having risen out of the world below. */
	public static int entryAltitudeAscending(final World destination) {
		return floorOf(destination) + ENTRY_MARGIN;
	}

	/** Arrival altitude for something entering through the ceiling, having fallen out of the world above. */
	public static int entryAltitudeDescending(final World destination) {
		return ceilingOf(destination) - ENTRY_MARGIN;
	}
}
