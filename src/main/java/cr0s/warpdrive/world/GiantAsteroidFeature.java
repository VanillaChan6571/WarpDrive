package cr0s.warpdrive.world;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;

import java.util.Random;

/**
 * The rare 1.12.2 asteroid-field event ({@code ratio="0.0016"}).
 *
 * Legacy generated the entire field synchronously: a radially distributed population of large
 * and small MetaOrb asteroids plus occasional coloured gas clouds. Here one deterministic field
 * is assigned to each 25x25-chunk cell, matching 1 / 625 = 0.0016 while avoiding random clumps.
 * Each invocation reconstructs that field and writes only the current chunk's slices.
 */
public final class GiantAsteroidFeature extends Feature<NoFeatureConfig> {

	private static final int GRID_SPACING = 25;
	private static final int MAX_FIELD_REACH_BLOCKS = 260;
	private static final long FIELD_SALT = 0x4649_454C_4453L; // "FIELDS"

	public GiantAsteroidFeature(final Codec<NoFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(final ISeedReader world, final ChunkGenerator generator,
	                     final Random ignored, final BlockPos origin, final NoFeatureConfig config) {
		final ChunkPos target = new ChunkPos(origin);
		final int baseRegionX = Math.floorDiv(target.x, GRID_SPACING);
		final int baseRegionZ = Math.floorDiv(target.z, GRID_SPACING);
		boolean placed = false;

		// A maximum-size field can cross one cell boundary, never two.
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				placed |= placeFieldSlice(world, target,
					baseRegionX + offsetX, baseRegionZ + offsetZ);
			}
		}
		return placed;
	}

	private static boolean placeFieldSlice(final ISeedReader world, final ChunkPos target,
	                                       final int regionX, final int regionZ) {
		final Random random = new Random(LegacyAsteroidGenerator.seed(
			world.getSeed(), regionX, regionZ, FIELD_SALT));
		final int sourceChunkX = regionX * GRID_SPACING + random.nextInt(GRID_SPACING);
		final int sourceChunkZ = regionZ * GRID_SPACING + random.nextInt(GRID_SPACING);
		final int fieldX = sourceChunkX * 16 + 5 - random.nextInt(10);
		final int fieldZ = sourceChunkZ * 16 + 5 - random.nextInt(10);
		final int requestedY = LegacyAsteroidGenerator.MIN_CENTRE_Y
			+ random.nextInt(LegacyAsteroidGenerator.MAX_CENTRE_Y
				- LegacyAsteroidGenerator.MIN_CENTRE_Y + 1);

		final int targetMinX = target.getMinBlockX();
		final int targetMinZ = target.getMinBlockZ();
		if (fieldX + MAX_FIELD_REACH_BLOCKS < targetMinX
		 || fieldX - MAX_FIELD_REACH_BLOCKS > targetMinX + 15
		 || fieldZ + MAX_FIELD_REACH_BLOCKS < targetMinZ
		 || fieldZ - MAX_FIELD_REACH_BLOCKS > targetMinZ + 15) {
			return false;
		}

		return generateLegacyFieldSlice(world, target, random, fieldX, requestedY, fieldZ);
	}

	private static boolean generateLegacyFieldSlice(final ISeedReader world, final ChunkPos target,
	                                                final Random random,
	                                                final int fieldX, final int requestedY,
	                                                final int fieldZ) {
		// AsteroidFieldInstance.generate, with the same order and ranges.
		final float surfacePerAsteroid = 80.0F + random.nextFloat() * 300.0F;
		final int maxDistance = 30 + random.nextInt(170);
		final int maxDistanceBig = Math.round(maxDistance * (0.6F + 0.2F * random.nextFloat()));
		final int maxDistanceSmall = Math.round(maxDistance * 1.1F);
		final float bigRatio = 0.3F + random.nextFloat() * 0.3F;
		final float surfaceBig = (float) (Math.PI * maxDistanceBig * maxDistanceBig);
		final float surfaceSmall = (float) (Math.PI * maxDistanceSmall * maxDistanceSmall);
		final int bigAsteroids = Math.round(bigRatio * surfaceBig / surfacePerAsteroid);
		final int smallAsteroids = Math.round((1.0F - bigRatio) * surfaceSmall / surfacePerAsteroid);
		final int clouds = Math.round(bigAsteroids / (10.0F + random.nextInt(10)));
		final int maxHeight = 70 + random.nextInt(50);
		final int fieldY = Math.min(LegacyAsteroidGenerator.MAX_BORDER_Y - maxHeight,
			Math.max(requestedY, LegacyAsteroidGenerator.MIN_BORDER_Y + maxHeight));

		boolean placed = false;
		for (int index = 0; index < bigAsteroids; index++) {
			final Position position = position(random, fieldX, fieldY, fieldZ,
				maxDistanceBig, maxHeight);
			placed |= LegacyAsteroidGenerator.placeRandomAsteroidSlice(
				world, target, random, position.x, position.y, position.z);
		}

		for (int index = 0; index < smallAsteroids; index++) {
			final Position position = position(random, fieldX, fieldY, fieldZ,
				maxDistanceSmall, maxHeight);
			// The old 1/400 branch generated a derelict ship or station. Those generators are not
			// ported yet, so preserve the empty slot rather than silently changing its probability.
			if (random.nextInt(400) == 1) {
				continue;
			}
			placed |= LegacyAsteroidGenerator.placeRandomAsteroidSlice(
				world, target, random, position.x, position.y, position.z);
		}

		for (int index = 0; index < clouds; index++) {
			final Position position = position(random, fieldX, fieldY, fieldZ,
				maxDistanceBig, maxHeight);
			// AsteroidFieldInstance intentionally discarded half of its computed cloud positions.
			if (!random.nextBoolean()) {
				continue;
			}
			placed |= LegacyAsteroidGenerator.placeRandomGasCloudSlice(
				world, target, random, position.x, position.y, position.z);
		}
		return placed;
	}

	private static Position position(final Random random,
	                                 final int fieldX, final int fieldY, final int fieldZ,
	                                 final int maxDistance, final int maxHeight) {
		final float binomial = binomialRandom(random);
		final double bearing = random.nextFloat() * 2.0D * Math.PI;
		final double yawn = random.nextFloat() * Math.PI;
		final float horizontalRange = Math.max(6.0F, binomial * maxDistance);
		final float verticalRange = Math.max(3.0F, binomial * maxHeight);
		return new Position(
			fieldX + (int) Math.round(horizontalRange * Math.cos(bearing)),
			fieldY + (int) Math.round(verticalRange * Math.cos(yawn)),
			fieldZ + (int) Math.round(horizontalRange * Math.sin(bearing)));
	}

	private static float binomialRandom(final Random random) {
		final float linear = random.nextFloat();
		return 1.25F - 0.625F / (0.5F + 2.0F * linear);
	}

	private static final class Position {
		private final int x;
		private final int y;
		private final int z;

		private Position(final int x, final int y, final int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
