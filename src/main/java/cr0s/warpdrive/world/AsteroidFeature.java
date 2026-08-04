package cr0s.warpdrive.world;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;

import java.util.Random;

/**
 * Generates a field of small asteroids, in the style of the 1.12.2 "asteroids_field" structure
 * group: a loose cluster of rocks of mixed composition rather than one uniform lump.
 *
 * Each asteroid is a rough sphere of a base stone with a scattering of one ore type, so different
 * rocks in the same field are worth different amounts and are visually distinguishable.
 */
public class AsteroidFeature extends Feature<NoFeatureConfig> {

	/** Base material and the ore that salts it. Weight controls how often the type is chosen. */
	private static final AsteroidType[] TYPES = {
		new AsteroidType(Blocks.STONE, Blocks.IRON_ORE, 24, 0.06F),
		new AsteroidType(Blocks.ANDESITE, Blocks.COAL_ORE, 20, 0.09F),
		new AsteroidType(Blocks.DIORITE, Blocks.IRON_ORE, 14, 0.05F),
		new AsteroidType(Blocks.GRANITE, Blocks.GOLD_ORE, 12, 0.04F),
		new AsteroidType(Blocks.STONE, Blocks.REDSTONE_ORE, 10, 0.05F),
		new AsteroidType(Blocks.ANDESITE, Blocks.LAPIS_ORE, 8, 0.04F),
		new AsteroidType(Blocks.STONE, Blocks.DIAMOND_ORE, 4, 0.02F),
		new AsteroidType(Blocks.STONE, Blocks.EMERALD_ORE, 3, 0.02F),
		new AsteroidType(Blocks.OBSIDIAN, Blocks.ANCIENT_DEBRIS, 1, 0.01F)
	};

	private static final int TOTAL_WEIGHT;

	static {
		int total = 0;
		for (final AsteroidType type : TYPES) {
			total += type.weight;
		}
		TOTAL_WEIGHT = total;
	}

	private static final int FIELD_MIN = 3;
	private static final int FIELD_MAX = 14;

	/**
	 * Horizontal spread of a field, in blocks.
	 *
	 * This must stay small. Features run inside the (multithreaded) chunk generation pipeline and
	 * are handed a WorldGenRegion covering only the chunk being generated plus a small margin;
	 * writes outside it are refused and logged, so a wide spread silently loses asteroids. With a
	 * max radius of 6 this reaches 14 blocks from the origin, comfortably inside one chunk plus
	 * margin. Large sprawling fields still appear, because neighbouring chunks roll their own.
	 */
	private static final int FIELD_SPREAD = 8;
	private static final int ASTEROID_MIN_RADIUS = 2;
	private static final int ASTEROID_MAX_RADIUS = 6;
	private static final int MIN_Y = 24;
	private static final int MAX_Y = 210;

	public AsteroidFeature(final Codec<NoFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(final ISeedReader world, final ChunkGenerator generator,
	                     final Random random, final BlockPos origin, final NoFeatureConfig config) {
		final int count = FIELD_MIN + random.nextInt(FIELD_MAX - FIELD_MIN + 1);
		final int centreY = MIN_Y + random.nextInt(MAX_Y - MIN_Y);
		final BlockPos centre = new BlockPos(origin.getX(), centreY, origin.getZ());

		boolean placedAny = false;
		for (int index = 0; index < count; index++) {
			// Y may spread further than X/Z: the region restriction is horizontal only
			final BlockPos asteroidPos = centre.offset(
				random.nextInt(FIELD_SPREAD * 2) - FIELD_SPREAD,
				random.nextInt(FIELD_SPREAD * 4) - FIELD_SPREAD * 2,
				random.nextInt(FIELD_SPREAD * 2) - FIELD_SPREAD);

			if (asteroidPos.getY() < MIN_Y || asteroidPos.getY() > MAX_Y) {
				continue;
			}
			placedAny |= placeAsteroid(world, random, asteroidPos);
		}
		return placedAny;
	}

	private boolean placeAsteroid(final ISeedReader world, final Random random, final BlockPos centre) {
		final AsteroidType type = pickType(random);
		final int radius = ASTEROID_MIN_RADIUS + random.nextInt(ASTEROID_MAX_RADIUS - ASTEROID_MIN_RADIUS + 1);
		final BlockState base = type.base.defaultBlockState();
		final BlockState ore = type.ore.defaultBlockState();

		boolean placed = false;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					// Squash slightly on Y and jitter the edge so rocks look eroded, not spherical
					final double distance = Math.sqrt(dx * dx + (dy * dy) * 1.4D + dz * dz);
					if (distance > radius - 0.5D + random.nextDouble()) {
						continue;
					}
					final BlockPos pos = centre.offset(dx, dy, dz);
					if (!world.getBlockState(pos).isAir()) {
						continue;
					}
					world.setBlock(pos, random.nextFloat() < type.oreChance ? ore : base, 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	private static AsteroidType pickType(final Random random) {
		int roll = random.nextInt(TOTAL_WEIGHT);
		for (final AsteroidType type : TYPES) {
			roll -= type.weight;
			if (roll < 0) {
				return type;
			}
		}
		return TYPES[0];
	}

	private static final class AsteroidType {
		private final net.minecraft.block.Block base;
		private final net.minecraft.block.Block ore;
		private final int weight;
		private final float oreChance;

		private AsteroidType(final net.minecraft.block.Block base, final net.minecraft.block.Block ore,
		                     final int weight, final float oreChance) {
			this.base = base;
			this.ore = ore;
			this.weight = weight;
			this.oreChance = oreChance;
		}
	}
}
