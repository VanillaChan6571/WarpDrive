package cr0s.warpdrive.world;

import java.util.Random;

/** Pure, deterministic planetary-system layout with no Minecraft registry dependencies. */
final class CelestialSystemLayout {

	// One system averages 2.5 bodies. A 45x45 grid therefore yields one body per ~810 chunks,
	// closely matching the legacy moon ratio of 0.00125 (one per 800 chunks).
	static final int GRID_SPACING = 45;
	static final int GRID_SEPARATION = 30;
	private static final long GRID_SALT = 0x4345_4C45_5354_4941L;

	/** Active vanilla profiles from 1.12.2 structures-default.xml, in inner-to-outer order. */
	enum Profile {
		OVERWORLD_BASE(2, ranges(4, 15, 3, 15, 0, 2, 2, 10, 1, 3)),
		OVERWORLD_RARE(2, ranges(1, 10, 3, 15, 1, 3, 2, 10, 1, 3)),
		OVERWORLD_EMPTY(-1, ranges(1, 1, 6, 29, 1, 1, 0, 2, 1, 5, 2, 3)),
		OVERWORLD_SHIP(-1, ranges(5, 16, 3, 17, 0, 2, 2, 10, 1, 3)),
		OVERWORLD_CORRUPTED(-1, ranges(5, 16, 6, 20, 10, 20, 2, 4)),
		NETHER(-1, ranges(1, 5, 1, 1, 6, 20, 10, 20, 2, 4)),
		NETHER_CORRUPTED(-1, ranges(1, 5, 1, 1, 6, 12, 8, 16, 2, 4)),
		END_HOLLOW(-1, ranges(5, 14, 1, 2, 10, 25, 2, 3)),
		END(-1, ranges(5, 15, 10, 25, 2, 3));

		private final int optionalFlowIndex;
		private final int[][] thicknessRanges;

		Profile(final int optionalFlowIndex, final int[][] thicknessRanges) {
			this.optionalFlowIndex = optionalFlowIndex;
			this.thicknessRanges = thicknessRanges;
		}

		private RolledProfile roll(final Random random, final boolean moon) {
			final int[] thicknesses = new int[thicknessRanges.length];
			final long[] materialSeeds = new long[thicknessRanges.length];
			int radius = 0;
			for (int index = 0; index < thicknessRanges.length; index++) {
				materialSeeds[index] = random.nextLong();
				// moon.flow has a fixed 35% chance to import its empty fillerSet.
				if (index == optionalFlowIndex && random.nextDouble() < 0.35D) {
					thicknesses[index] = 0;
				} else {
					thicknesses[index] = biasedRange(random,
						thicknessRanges[index][0], thicknessRanges[index][1], moon);
				}
				radius += thicknesses[index];
			}
			return new RolledProfile(this, thicknesses, materialSeeds, radius);
		}
	}

	private static final class RolledProfile {
		private final Profile profile;
		private final int[] shellThicknesses;
		private final long[] shellSeeds;
		private final int radius;

		private RolledProfile(final Profile profile, final int[] shellThicknesses,
		                      final long[] shellSeeds, final int radius) {
			this.profile = profile;
			this.shellThicknesses = shellThicknesses;
			this.shellSeeds = shellSeeds;
			this.radius = radius;
		}
	}

	static final class Body {
		final int x;
		final int y;
		final int z;
		final int radius;
		final Profile profile;
		final int[] shellThicknesses;
		final long[] shellSeeds;
		final boolean moon;
		final long seed;

		private Body(final int x, final int y, final int z, final RolledProfile rolled,
		             final boolean moon, final long seed) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.radius = rolled.radius;
			this.profile = rolled.profile;
			this.shellThicknesses = rolled.shellThicknesses;
			this.shellSeeds = rolled.shellSeeds;
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
		final long planetSeed = mix(systemSeed ^ 0x504C_414E_4554L);
		final RolledProfile planetProfile = rollProfile(planetSeed, false);
		final int planetY = planetProfile.radius + 18
			+ random.nextInt(256 - 2 * (planetProfile.radius + 18));

		final int moonCount = 1 + random.nextInt(2);
		final Body[] bodies = new Body[moonCount + 1];
		bodies[0] = new Body(planetX, planetY, planetZ, planetProfile, false, planetSeed);
		final double baseAngle = random.nextDouble() * Math.PI * 2.0D;
		for (int index = 0; index < moonCount; index++) {
			final long moonSeed = mix(systemSeed + index + 1L);
			final RolledProfile moonProfile = rollProfile(moonSeed, true);
			final int orbit = planetProfile.radius + moonProfile.radius + 28 + index * 18;
			final double angle = baseAngle + index * Math.PI;
			final int moonY = clamp(moonProfile.radius + 2, 253 - moonProfile.radius,
				planetY + random.nextInt(31) - 15);
			bodies[index + 1] = new Body(
				planetX + (int) Math.round(Math.cos(angle) * orbit),
				moonY,
				planetZ + (int) Math.round(Math.sin(angle) * orbit),
				moonProfile, true, moonSeed);
		}
		return bodies;
	}

	private static RolledProfile rollProfile(final long seed, final boolean moon) {
		final Random random = new Random(seed);
		return pickProfile(random).roll(random, moon);
	}

	/** Ratio entries retain fixed odds; the five Overworld profiles share the remainder by weight. */
	private static Profile pickProfile(final Random random) {
		final double value = random.nextDouble();
		if (value < 0.10D) return Profile.NETHER;
		if (value < 0.20D) return Profile.NETHER_CORRUPTED;
		if (value < 0.23D) return Profile.END_HOLLOW;
		if (value < 0.28D) return Profile.END;

		int roll = random.nextInt(170);
		if ((roll -= 100) < 0) return Profile.OVERWORLD_BASE;
		if ((roll -= 20) < 0) return Profile.OVERWORLD_RARE;
		if ((roll -= 20) < 0) return Profile.OVERWORLD_EMPTY;
		if ((roll -= 10) < 0) return Profile.OVERWORLD_SHIP;
		return Profile.OVERWORLD_CORRUPTED;
	}

	/** Central planets bias high and moons low, but every thickness remains inside its XML range. */
	private static int biasedRange(final Random random, final int min, final int max,
	                               final boolean moon) {
		if (min == max) {
			return min;
		}
		final int first = min + random.nextInt(max - min + 1);
		final int second = min + random.nextInt(max - min + 1);
		return moon ? Math.min(first, second) : Math.max(first, second);
	}

	private static int[][] ranges(final int... values) {
		final int[][] ranges = new int[values.length / 2][2];
		for (int index = 0; index < ranges.length; index++) {
			ranges[index][0] = values[index * 2];
			ranges[index][1] = values[index * 2 + 1];
		}
		return ranges;
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
