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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Asteroid fields, modelled on 1.12.2's structures-default.xml / fillerSets-default.xml.
 *
 * Two ideas from the original are what make asteroids look distinct from one another, and both
 * were missing from the first attempt here:
 *
 * 1. metaShell - an asteroid is not one sphere but several merged sub-spheres ("cores"), so the
 *    silhouette is lumpy and irregular. A "common-cluster" has 6-10 cores; a "rare-geode" has 1-2.
 *
 * 2. Filler sets - the block mix is chosen ONCE PER ASTEROID from a weighted list of sets, then
 *    individual blocks roll within that set. So a whole rock reads as sandstone-and-clay, or as
 *    cobblestone, rather than every rock being the same speckle of stone plus one ore.
 *
 * Sizes are scaled down from the original: features may only write within the chunk being
 * generated plus a small margin, so a 1.12.2 asteroid (core radius 4 + mantle up to 7) would be
 * clipped. Proportions are preserved.
 */
public class AsteroidFeature extends Feature<NoFeatureConfig> {

	// ===== Filler sets: weighted block mixes, chosen once per asteroid =====

	private static final class Filler {
		private final Block block;
		private final int weight;

		private Filler(final Block block, final int weight) {
			this.block = block;
			this.weight = weight;
		}
	}

	private static final class FillerSet {
		private final int weight;
		private final Filler[] blocks;
		private final int totalWeight;

		private FillerSet(final int weight, final Filler... blocks) {
			this.weight = weight;
			this.blocks = blocks;
			int total = 0;
			for (final Filler filler : blocks) {
				total += filler.weight;
			}
			this.totalWeight = total;
		}

		private BlockState pick(final Random random) {
			int roll = random.nextInt(totalWeight);
			for (final Filler filler : blocks) {
				roll -= filler.weight;
				if (roll < 0) {
					return filler.block.defaultBlockState();
				}
			}
			return blocks[0].block.defaultBlockState();
		}
	}

	/** overworld_common: mostly stone, with the cheap ores. */
	private static final FillerSet[] COMMON = {
		new FillerSet(20,
			new Filler(Blocks.STONE, 40), new Filler(Blocks.COAL_ORE, 6),
			new Filler(Blocks.IRON_ORE, 5), new Filler(Blocks.REDSTONE_ORE, 2)),
		new FillerSet(5,
			new Filler(Blocks.STONE, 25), new Filler(Blocks.COBBLESTONE, 5),
			new Filler(Blocks.COAL_ORE, 5), new Filler(Blocks.IRON_ORE, 4)),
		new FillerSet(1,
			new Filler(Blocks.COBBLESTONE, 40), new Filler(Blocks.COAL_ORE, 6),
			new Filler(Blocks.IRON_ORE, 4)),
		new FillerSet(2,
			new Filler(Blocks.SANDSTONE, 20), new Filler(Blocks.CLAY, 20),
			new Filler(Blocks.IRON_ORE, 3), new Filler(Blocks.COAL_ORE, 3))
	};

	/** overworld_uncommon: gold and lapis territory. */
	private static final FillerSet[] UNCOMMON = {
		new FillerSet(10,
			new Filler(Blocks.STONE, 40), new Filler(Blocks.GOLD_ORE, 5),
			new Filler(Blocks.LAPIS_ORE, 4), new Filler(Blocks.IRON_ORE, 3)),
		new FillerSet(4,
			new Filler(Blocks.ANDESITE, 30), new Filler(Blocks.GOLD_ORE, 6),
			new Filler(Blocks.REDSTONE_ORE, 5)),
		new FillerSet(2,
			new Filler(Blocks.DIORITE, 30), new Filler(Blocks.LAPIS_ORE, 8),
			new Filler(Blocks.GOLD_ORE, 3))
	};

	/**
	 * Themed sets. 1.12.2 had a "nether-small" asteroid variant importing a nether filler group;
	 * these extend that idea to desert and ice so a field can throw up something distinctive.
	 */
	private static final FillerSet[] NETHER = {
		new FillerSet(10,
			new Filler(Blocks.NETHERRACK, 55), new Filler(Blocks.NETHER_QUARTZ_ORE, 10),
			new Filler(Blocks.MAGMA_BLOCK, 8), new Filler(Blocks.OBSIDIAN, 6),
			new Filler(Blocks.NETHER_GOLD_ORE, 4), new Filler(Blocks.GLOWSTONE, 2)),
		new FillerSet(4,
			new Filler(Blocks.BLACKSTONE, 45), new Filler(Blocks.BASALT, 25),
			new Filler(Blocks.MAGMA_BLOCK, 10), new Filler(Blocks.NETHER_GOLD_ORE, 5),
			new Filler(Blocks.ANCIENT_DEBRIS, 1))
	};

	private static final FillerSet[] DESERT = {
		new FillerSet(10,
			new Filler(Blocks.SANDSTONE, 40), new Filler(Blocks.CLAY, 20),
			new Filler(Blocks.SAND, 15), new Filler(Blocks.SMOOTH_SANDSTONE, 10),
			new Filler(Blocks.GOLD_ORE, 4), new Filler(Blocks.IRON_ORE, 4)),
		new FillerSet(4,
			new Filler(Blocks.RED_SANDSTONE, 40), new Filler(Blocks.RED_SAND, 20),
			new Filler(Blocks.TERRACOTTA, 18), new Filler(Blocks.GOLD_ORE, 5))
	};

	private static final FillerSet[] ICE = {
		new FillerSet(10,
			new Filler(Blocks.PACKED_ICE, 45), new Filler(Blocks.SNOW_BLOCK, 15),
			new Filler(Blocks.ICE, 10), new Filler(Blocks.OBSIDIAN, 8),
			new Filler(Blocks.LAPIS_ORE, 5), new Filler(Blocks.DIAMOND_ORE, 2)),
		new FillerSet(3,
			new Filler(Blocks.BLUE_ICE, 40), new Filler(Blocks.PACKED_ICE, 30),
			new Filler(Blocks.OBSIDIAN, 12), new Filler(Blocks.DIAMOND_ORE, 3))
	};

	/** overworld_rare: the payoff. Mirrors the original's stone/obsidian split. */
	private static final FillerSet[] RARE = {
		new FillerSet(10,
			new Filler(Blocks.STONE, 100), new Filler(Blocks.IRON_ORE, 5),
			new Filler(Blocks.DIAMOND_ORE, 3), new Filler(Blocks.EMERALD_ORE, 2)),
		new FillerSet(2,
			new Filler(Blocks.OBSIDIAN, 100), new Filler(Blocks.DIAMOND_ORE, 5),
			new Filler(Blocks.ANCIENT_DEBRIS, 1))
	};

	// ===== Variants: weighted asteroid archetypes =====

	private static final class Variant {
		private final String name;
		private final int weight;
		private final int minCores;
		private final int maxCores;
		private final int coreRadius;
		private final int minMantle;
		private final int maxMantle;
		private final FillerSet[] fillers;

		private Variant(final String name, final int weight, final int minCores, final int maxCores,
		                final int coreRadius, final int minMantle, final int maxMantle,
		                final FillerSet[] fillers) {
			this.name = name;
			this.weight = weight;
			this.minCores = minCores;
			this.maxCores = maxCores;
			this.coreRadius = coreRadius;
			this.minMantle = minMantle;
			this.maxMantle = maxMantle;
			this.fillers = fillers;
		}
	}

	/** Weights carried over from structures-default.xml; sizes scaled to fit the write region. */
	private static final Variant[] VARIANTS = {
		new Variant("common-cluster", 100, 3, 5, 2, 1, 2, COMMON),
		new Variant("uncommon-medium", 100, 1, 3, 1, 1, 2, UNCOMMON),
		new Variant("common-geode", 30, 3, 6, 1, 2, 3, COMMON),
		new Variant("rare-geode", 10, 1, 2, 1, 1, 2, RARE),
		new Variant("stone-large", 10, 1, 2, 2, 2, 3, RARE),
		// Themed rocks: ~14% of the pool, so they read as a find rather than the norm
		new Variant("desert", 15, 2, 5, 2, 1, 2, DESERT),
		new Variant("nether", 12, 2, 4, 2, 1, 3, NETHER),
		new Variant("ice", 12, 2, 4, 2, 1, 3, ICE)
	};

	private static final int VARIANT_TOTAL_WEIGHT;

	static {
		int total = 0;
		for (final Variant variant : VARIANTS) {
			total += variant.weight;
		}
		VARIANT_TOTAL_WEIGHT = total;
	}

	// ===== Field placement =====

	/**
	 * Village-style grid placement. A per-chunk chance decorator clumps, because neighbouring
	 * chunks roll independently; this guarantees one field per region with a minimum separation.
	 * Field richness varies per region so some are a few loose rocks and others sprawl over
	 * several chunks.
	 */
	// Village spacing (32/8). Giant asteroids use their own, much rarer grid - see
	// GiantAsteroidFeature, which sits at Woodland Mansion frequency.
	private static final int GRID_SPACING = 32;
	private static final int GRID_SEPARATION = 8;
	private static final long GRID_SALT = 0x5741_5250L;   // "WARP"

	/** Chunks each richness class spans, and how many rocks the centre chunk gets. */
	// Heavily weighted towards small scattered fields: a big sprawl should be a landmark you
	// remember, not something you pass every other region.
	private static final int[] RICHNESS_WEIGHT = { 70, 20, 8, 2 };
	private static final int[] RICHNESS_CHUNK_RADIUS = { 0, 1, 1, 2 };
	private static final int[] RICHNESS_CORE_ROCKS = { 2, 3, 4, 6 };

	private static final int FIELD_SPREAD = 6;
	private static final int MIN_Y = 24;
	private static final int MAX_Y = 210;

	public AsteroidFeature(final Codec<NoFeatureConfig> codec) {
		super(codec);
	}

	private static final class FieldSlot {
		private final int rocks;
		private final long fieldSeed;

		private FieldSlot(final int rocks, final long fieldSeed) {
			this.rocks = rocks;
			this.fieldSeed = fieldSeed;
		}
	}

	/**
	 * Which field, if any, covers this chunk. Neighbouring regions are checked too, so a field
	 * centred near a region edge still spills correctly instead of being clipped.
	 */
	@Nullable
	private static FieldSlot fieldFor(final long seed, final int chunkX, final int chunkZ) {
		final int baseRegionX = Math.floorDiv(chunkX, GRID_SPACING);
		final int baseRegionZ = Math.floorDiv(chunkZ, GRID_SPACING);

		for (int offsetRegionX = -1; offsetRegionX <= 1; offsetRegionX++) {
			for (int offsetRegionZ = -1; offsetRegionZ <= 1; offsetRegionZ++) {
				final int regionX = baseRegionX + offsetRegionX;
				final int regionZ = baseRegionZ + offsetRegionZ;
				final long regionSeed = seed + regionX * 341873128712L + regionZ * 132897987541L + GRID_SALT;
				final Random regionRandom = new Random(regionSeed);

				final int range = GRID_SPACING - GRID_SEPARATION;
				final int centreX = regionX * GRID_SPACING + regionRandom.nextInt(range);
				final int centreZ = regionZ * GRID_SPACING + regionRandom.nextInt(range);
				final int richness = pickWeighted(regionRandom, RICHNESS_WEIGHT);

				final int distance = Math.max(Math.abs(chunkX - centreX), Math.abs(chunkZ - centreZ));
				if (distance > RICHNESS_CHUNK_RADIUS[richness]) {
					continue;
				}

				// Thin out towards the edge so a field fades rather than ending abruptly
				final int rocks = Math.max(1, RICHNESS_CORE_ROCKS[richness] - distance * 2);
				return new FieldSlot(rocks, regionSeed ^ ((long) chunkX << 32) ^ chunkZ);
			}
		}
		return null;
	}

	@Override
	public boolean place(final ISeedReader world, final ChunkGenerator generator,
	                     final Random random, final BlockPos origin, final NoFeatureConfig config) {
		final ChunkPos chunkPos = new ChunkPos(origin);
		final FieldSlot field = fieldFor(world.getSeed(), chunkPos.x, chunkPos.z);
		if (field == null) {
			return false;
		}

		final Random fieldRandom = new Random(field.fieldSeed);
		final int centreY = MIN_Y + fieldRandom.nextInt(MAX_Y - MIN_Y);

		boolean placedAny = false;
		for (int index = 0; index < field.rocks; index++) {
			final BlockPos asteroidPos = new BlockPos(
				origin.getX() + fieldRandom.nextInt(FIELD_SPREAD * 2) - FIELD_SPREAD,
				centreY + fieldRandom.nextInt(FIELD_SPREAD * 4) - FIELD_SPREAD * 2,
				origin.getZ() + fieldRandom.nextInt(FIELD_SPREAD * 2) - FIELD_SPREAD);

			if (asteroidPos.getY() < MIN_Y || asteroidPos.getY() > MAX_Y) {
				continue;
			}
			placedAny |= placeAsteroid(world, fieldRandom, asteroidPos);
		}
		return placedAny;
	}

	/** One asteroid: a metaShell of several cores, wrapped in a mantle of one filler set. */
	private boolean placeAsteroid(final ISeedReader world, final Random random, final BlockPos centre) {
		final Variant variant = pickVariant(random);
		final FillerSet fillerSet = pickFillerSet(random, variant.fillers);

		final int coreCount = variant.minCores + random.nextInt(variant.maxCores - variant.minCores + 1);
		final int mantle = variant.minMantle + random.nextInt(variant.maxMantle - variant.minMantle + 1);
		final int coreRadius = variant.coreRadius;

		// Scatter the cores so the merged silhouette is lumpy rather than spherical
		final int scatter = Math.max(1, coreRadius + coreCount / 2);
		final List<BlockPos> cores = new ArrayList<>(coreCount);
		for (int index = 0; index < coreCount; index++) {
			cores.add(centre.offset(
				random.nextInt(scatter * 2 + 1) - scatter,
				random.nextInt(Math.max(1, scatter)) - scatter / 2,
				random.nextInt(scatter * 2 + 1) - scatter));
		}

		final int reach = coreRadius + mantle;
		final int bound = scatter + reach;
		boolean placed = false;

		for (int dx = -bound; dx <= bound; dx++) {
			for (int dy = -bound; dy <= bound; dy++) {
				for (int dz = -bound; dz <= bound; dz++) {
					final BlockPos pos = centre.offset(dx, dy, dz);

					double nearest = Double.MAX_VALUE;
					for (final BlockPos core : cores) {
						final double distance = Math.sqrt(core.distSqr(pos));
						if (distance < nearest) {
							nearest = distance;
						}
					}

					// Jittered edge so the surface is eroded rather than smooth
					if (nearest > reach - 0.5D + random.nextDouble()) {
						continue;
					}
					if (!world.getBlockState(pos).isAir()) {
						continue;
					}
					world.setBlock(pos, fillerSet.pick(random), 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	private static Variant pickVariant(final Random random) {
		int roll = random.nextInt(VARIANT_TOTAL_WEIGHT);
		for (final Variant variant : VARIANTS) {
			roll -= variant.weight;
			if (roll < 0) {
				return variant;
			}
		}
		return VARIANTS[0];
	}

	private static FillerSet pickFillerSet(final Random random, final FillerSet[] sets) {
		int total = 0;
		for (final FillerSet set : sets) {
			total += set.weight;
		}
		int roll = random.nextInt(total);
		for (final FillerSet set : sets) {
			roll -= set.weight;
			if (roll < 0) {
				return set;
			}
		}
		return sets[0];
	}

	private static int pickWeighted(final Random random, final int[] weights) {
		int total = 0;
		for (final int weight : weights) {
			total += weight;
		}
		int roll = random.nextInt(total);
		for (int index = 0; index < weights.length; index++) {
			roll -= weights[index];
			if (roll < 0) {
				return index;
			}
		}
		return 0;
	}
}
