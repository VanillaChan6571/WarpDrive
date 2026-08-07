package cr0s.warpdrive.world;

import java.util.Random;

/** Pure, deterministic planetary-system layout with no Minecraft registry dependencies. */
final class CelestialSystemLayout {

	static final int GRID_SPACING = 96;
	static final int GRID_SEPARATION = 32;
	private static final long GRID_SALT = 0x4345_4C45_5354_4941L;
	private static final int PLANET_RADIUS_MIN = 42;
	private static final int PLANET_RADIUS_MAX = 58;
	private static final int MOON_RADIUS_MIN = 13;
	private static final int MOON_RADIUS_MAX = 21;

	enum Theme {
		TERRAN,
		DESERT,
		ICE,
		VOLCANIC
	}

	static final class Body {
		final int x;
		final int y;
		final int z;
		final int radius;
		final Theme theme;
		final boolean moon;
		final long seed;

		private Body(final int x, final int y, final int z, final int radius,
		             final Theme theme, final boolean moon, final long seed) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = radius;
			this.theme = theme;
			this.moon = moon;
			this.seed = seed;
		}
	}

	private CelestialSystemLayout() {
	}

	static Body[] systemBodies(final long worldSeed, final int regionX, final int regionZ) {
		final long systemSeed = mix(worldSeed
			^ (long) regionX * 341873128712L
			^ (long) regionZ * 132897987541L
			^ GRID_SALT);
		final Random random = new Random(systemSeed);
		final int range = GRID_SPACING - GRID_SEPARATION;
		final int planetX = (regionX * GRID_SPACING + random.nextInt(range)) * 16 + 8;
		final int planetZ = (regionZ * GRID_SPACING + random.nextInt(range)) * 16 + 8;
		final int planetRadius = PLANET_RADIUS_MIN + random.nextInt(PLANET_RADIUS_MAX - PLANET_RADIUS_MIN + 1);
		final int planetY = planetRadius + 18
			+ random.nextInt(256 - 2 * (planetRadius + 18));
		final Theme planetTheme = Theme.values()[random.nextInt(Theme.values().length)];

		final int moonCount = 1 + random.nextInt(2);
		final Body[] bodies = new Body[moonCount + 1];
		bodies[0] = new Body(planetX, planetY, planetZ, planetRadius, planetTheme, false, systemSeed);
		final double baseAngle = random.nextDouble() * Math.PI * 2.0D;
		for (int index = 0; index < moonCount; index++) {
			final int moonRadius = MOON_RADIUS_MIN + random.nextInt(MOON_RADIUS_MAX - MOON_RADIUS_MIN + 1);
			final int orbit = planetRadius + moonRadius + 28 + index * 18;
			final double angle = baseAngle + index * Math.PI;
			final int moonY = clamp(24, 231, planetY + random.nextInt(31) - 15);
			final Theme moonTheme = random.nextBoolean() ? Theme.DESERT : Theme.VOLCANIC;
			bodies[index + 1] = new Body(
				planetX + (int) Math.round(Math.cos(angle) * orbit),
				moonY,
				planetZ + (int) Math.round(Math.sin(angle) * orbit),
				moonRadius, moonTheme, true, mix(systemSeed + index + 1L));
		}
		return bodies;
	}

	static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static int clamp(final int min, final int max, final int value) {
		return Math.max(min, Math.min(max, value));
	}
}
