package cr0s.warpdrive.world;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A rare giant asteroid: a layered geode system placed at roughly Woodland Mansion frequency, so
 * finding one is an event rather than routine.
 *
 * Spans at most 3x3 chunks. Each of those chunks generates only the slice of the structure that
 * falls inside its own 16x16 footprint - features may not write far outside the chunk being
 * generated, so building it from a single chunk would silently clip most of it away.
 *
 * Layering follows the shell model 1.12.2 used for its moons and stars: a rare-ore core, an
 * uncommon mantle, and a common crust, so digging inwards is progressively more rewarding.
 */
public class GiantAsteroidFeature extends Feature<NoFeatureConfig> {

	/** Woodland Mansion spacing, the rarest spacing-based structure in vanilla. */
	private static final int GRID_SPACING = 80;
	private static final int GRID_SEPARATION = 20;
	private static final long GRID_SALT = 0x4749_414EL;   // "GIAN"

	/** Chunk radius of 3 gives a 7x7 footprint, about 112 blocks across. */
	private static final int CHUNK_RADIUS = 3;

	/**
	 * This is a dense FIELD, not a big rock.
	 *
	 * An earlier version built a handful of large lobes, but spread them far enough apart that
	 * they never merged - so it rendered as three or four separate planets. What a landmark wants
	 * is a volume of space packed with many asteroids of varied size and material: something you
	 * fly into and mine through, rather than something you orbit.
	 *
	 * Every chunk in the 7x7 footprint contributes its own rocks, which is also what keeps each
	 * write inside the region a feature is allowed to touch.
	 */
	private static final int MIN_ROCKS_PER_CHUNK = 5;
	private static final int MAX_ROCKS_PER_CHUNK = 11;

	private static final int MIN_ROCK_RADIUS = 2;
	private static final int MAX_ROCK_RADIUS = 7;

	/** Occasionally a much larger rock, to break up the scale. */
	private static final float LARGE_ROCK_CHANCE = 0.14F;
	private static final int LARGE_ROCK_MIN_RADIUS = 8;
	private static final int LARGE_ROCK_MAX_RADIUS = 11;

	/** Vertical extent of the cluster, measured from its centre. */
	private static final int VERTICAL_SPREAD = 38;

	private static final int MIN_Y = 80;
	private static final int MAX_Y = 165;

	/**
	 * A whole cluster shares one theme, so a landmark reads as "the nether field" or "the ice
	 * field" rather than mixed rubble. Individual scattered asteroids pick their theme per rock -
	 * see AsteroidFeature - which keeps the two systems visually distinct.
	 */
	private static final class Theme {
		private final String name;
		private final int weight;
		private final Block[] core;
		private final int[] coreWeight;
		private final Block[] mantle;
		private final int[] mantleWeight;
		private final Block[] crust;
		private final int[] crustWeight;

		private Theme(final String name, final int weight,
		              final Block[] core, final int[] coreWeight,
		              final Block[] mantle, final int[] mantleWeight,
		              final Block[] crust, final int[] crustWeight) {
			this.name = name;
			this.weight = weight;
			this.core = core;
			this.coreWeight = coreWeight;
			this.mantle = mantle;
			this.mantleWeight = mantleWeight;
			this.crust = crust;
			this.crustWeight = crustWeight;
		}
	}

	private static final Theme[] THEMES = {
		new Theme("stone", 55,
			new Block[]{ Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.ANCIENT_DEBRIS, Blocks.OBSIDIAN },
			new int[]{ 6, 4, 2, 24 },
			new Block[]{ Blocks.GOLD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.STONE, Blocks.ANDESITE },
			new int[]{ 6, 5, 6, 40, 20 },
			new Block[]{ Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIORITE },
			new int[]{ 7, 8, 55, 12, 15 }),

		new Theme("nether", 16,
			new Block[]{ Blocks.ANCIENT_DEBRIS, Blocks.NETHER_GOLD_ORE, Blocks.GLOWSTONE, Blocks.OBSIDIAN },
			new int[]{ 4, 8, 6, 22 },
			new Block[]{ Blocks.NETHER_QUARTZ_ORE, Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, Blocks.BASALT },
			new int[]{ 10, 8, 45, 20 },
			new Block[]{ Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.MAGMA_BLOCK },
			new int[]{ 50, 25, 18, 6 }),

		new Theme("desert", 16,
			new Block[]{ Blocks.GOLD_ORE, Blocks.EMERALD_ORE, Blocks.SANDSTONE, Blocks.CLAY },
			new int[]{ 10, 3, 30, 20 },
			new Block[]{ Blocks.SANDSTONE, Blocks.CLAY, Blocks.SMOOTH_SANDSTONE, Blocks.GOLD_ORE },
			new int[]{ 45, 22, 15, 5 },
			new Block[]{ Blocks.SANDSTONE, Blocks.SAND, Blocks.RED_SANDSTONE, Blocks.TERRACOTTA },
			new int[]{ 45, 20, 20, 12 }),

		new Theme("ice", 13,
			new Block[]{ Blocks.DIAMOND_ORE, Blocks.LAPIS_ORE, Blocks.BLUE_ICE, Blocks.OBSIDIAN },
			new int[]{ 6, 8, 30, 15 },
			new Block[]{ Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.OBSIDIAN, Blocks.LAPIS_ORE },
			new int[]{ 50, 18, 10, 5 },
			new Block[]{ Blocks.PACKED_ICE, Blocks.ICE, Blocks.SNOW_BLOCK, Blocks.OBSIDIAN },
			new int[]{ 50, 15, 22, 6 })
	};

	public GiantAsteroidFeature(final Codec<NoFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(final ISeedReader world, final ChunkGenerator generator,
	                     final Random random, final BlockPos origin, final NoFeatureConfig config) {
		final ChunkPos chunkPos = new ChunkPos(origin);
		final int baseRegionX = Math.floorDiv(chunkPos.x, GRID_SPACING);
		final int baseRegionZ = Math.floorDiv(chunkPos.z, GRID_SPACING);

		// Check neighbouring regions too, so a structure centred near a region edge is not clipped
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				final int regionX = baseRegionX + offsetX;
				final int regionZ = baseRegionZ + offsetZ;
				final long regionSeed =
					world.getSeed() + regionX * 341873128712L + regionZ * 132897987541L + GRID_SALT;
				final Random regionRandom = new Random(regionSeed);

				final int range = GRID_SPACING - GRID_SEPARATION;
				final int centreChunkX = regionX * GRID_SPACING + regionRandom.nextInt(range);
				final int centreChunkZ = regionZ * GRID_SPACING + regionRandom.nextInt(range);

				if (Math.abs(chunkPos.x - centreChunkX) > CHUNK_RADIUS
				 || Math.abs(chunkPos.z - centreChunkZ) > CHUNK_RADIUS) {
					continue;
				}

				return buildSlice(world, regionRandom, chunkPos, centreChunkX, centreChunkZ);
			}
		}
		return false;
	}

	/**
	 * Fill this chunk's share of the cluster: a scatter of individual rocks, not a slice of one
	 * enormous body. Rocks stay inside this chunk (plus their own radius) so every write lands in
	 * the permitted region, and the density across the 7x7 footprint is what makes it a landmark.
	 */
	private boolean buildSlice(final ISeedReader world, final Random regionRandom,
	                           final ChunkPos chunkPos, final int centreChunkX, final int centreChunkZ) {
		final int centreY = MIN_Y + regionRandom.nextInt(MAX_Y - MIN_Y + 1);
		// Drawn from the region random, so every chunk of the cluster agrees on the theme
		final Theme theme = pickTheme(regionRandom);

		// Per chunk, but derived from the region seed so the whole cluster shares a centre height
		final Random random = new Random(
			world.getSeed() ^ ((long) chunkPos.x * 341873128712L) ^ ((long) chunkPos.z * 132897987541L) ^ GRID_SALT);

		// Thin towards the edge of the footprint so the cluster fades out rather than ending square
		final int distance = Math.max(Math.abs(chunkPos.x - centreChunkX), Math.abs(chunkPos.z - centreChunkZ));
		final int baseCount = MIN_ROCKS_PER_CHUNK
			+ random.nextInt(MAX_ROCKS_PER_CHUNK - MIN_ROCKS_PER_CHUNK + 1);
		final int rockCount = Math.max(1, baseCount - distance * 2);

		final int minX = chunkPos.getMinBlockX();
		final int minZ = chunkPos.getMinBlockZ();
		boolean placed = false;

		for (int index = 0; index < rockCount; index++) {
			final boolean large = random.nextFloat() < LARGE_ROCK_CHANCE;
			final int radius = large
				? LARGE_ROCK_MIN_RADIUS + random.nextInt(LARGE_ROCK_MAX_RADIUS - LARGE_ROCK_MIN_RADIUS + 1)
				: MIN_ROCK_RADIUS + random.nextInt(MAX_ROCK_RADIUS - MIN_ROCK_RADIUS + 1);

			// Denser near the middle of the cluster's vertical extent
			final int verticalOffset = (random.nextInt(VERTICAL_SPREAD * 2 + 1) - VERTICAL_SPREAD
				+ random.nextInt(VERTICAL_SPREAD * 2 + 1) - VERTICAL_SPREAD) / 2;

			final BlockPos rockCentre = new BlockPos(
				minX + random.nextInt(16),
				centreY + verticalOffset,
				minZ + random.nextInt(16));

			placed |= placeRock(world, random, rockCentre, radius, theme);
		}
		return placed;
	}

	/** One rock, layered so larger ones are worth digging into. */
	private boolean placeRock(final ISeedReader world, final Random random,
	                          final BlockPos centre, final int radius, final Theme theme) {
		boolean placed = false;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					final int y = centre.getY() + dy;
					if (y < 1 || y > 254) {
						continue;
					}
					final double depth = Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
					if (depth > 0.92D + random.nextDouble() * 0.14D) {
						continue;
					}

					final BlockPos pos = centre.offset(dx, dy, dz);
					if (!world.getBlockState(pos).isAir()) {
						continue;
					}

					final BlockState state;
					if (radius >= 6 && depth < 0.34D) {
						state = pick(random, theme.core, theme.coreWeight);
					} else if (depth < 0.72D) {
						state = pick(random, theme.mantle, theme.mantleWeight);
					} else {
						state = pick(random, theme.crust, theme.crustWeight);
					}
					world.setBlock(pos, state, 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	private static Theme pickTheme(final Random random) {
		int total = 0;
		for (final Theme theme : THEMES) {
			total += theme.weight;
		}
		int roll = random.nextInt(total);
		for (final Theme theme : THEMES) {
			roll -= theme.weight;
			if (roll < 0) {
				return theme;
			}
		}
		return THEMES[0];
	}

	private static BlockState pick(final Random random, final Block[] blocks, final int[] weights) {
		int total = 0;
		for (final int weight : weights) {
			total += weight;
		}
		int roll = random.nextInt(total);
		for (int index = 0; index < blocks.length; index++) {
			roll -= weights[index];
			if (roll < 0) {
				return blocks[index].defaultBlockState();
			}
		}
		return blocks[0].defaultBlockState();
	}
}
