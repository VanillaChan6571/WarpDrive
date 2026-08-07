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
 * The ordinary asteroid stream from the 1.12.2 space definition ({@code ratio="0.0067"}).
 *
 * A source asteroid is selected deterministically for each chunk. Every generated chunk checks
 * the nearby source chunks and writes only its own slice, so full-sized legacy MetaOrbs can cross
 * chunk boundaries without clipping or force-loading adjacent chunks.
 */
public final class AsteroidFeature extends Feature<NoFeatureConfig> {

	private static final double ASTEROID_RATIO = 0.0067D;
	private static final int SOURCE_CHUNK_REACH = 2;
	private static final long ASTEROID_SALT = 0x4153_5445_524F_4944L; // "ASTEROID"

	public AsteroidFeature(final Codec<NoFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(final ISeedReader world, final ChunkGenerator generator,
	                     final Random ignored, final BlockPos origin, final NoFeatureConfig config) {
		final ChunkPos target = new ChunkPos(origin);
		boolean placed = false;
		for (int offsetX = -SOURCE_CHUNK_REACH; offsetX <= SOURCE_CHUNK_REACH; offsetX++) {
			for (int offsetZ = -SOURCE_CHUNK_REACH; offsetZ <= SOURCE_CHUNK_REACH; offsetZ++) {
				placed |= placeSourceSlice(world, target, target.x + offsetX, target.z + offsetZ);
			}
		}
		return placed;
	}

	private static boolean placeSourceSlice(final ISeedReader world, final ChunkPos target,
	                                        final int sourceChunkX, final int sourceChunkZ) {
		final Random random = new Random(LegacyAsteroidGenerator.seed(
			world.getSeed(), sourceChunkX, sourceChunkZ, ASTEROID_SALT));
		if (random.nextDouble() >= ASTEROID_RATIO) {
			return false;
		}

		// CommonWorldGenerator used 5 - nextInt(10), retained here including its edge bias.
		final int x = sourceChunkX * 16 + 5 - random.nextInt(10);
		final int z = sourceChunkZ * 16 + 5 - random.nextInt(10);
		final int y = LegacyAsteroidGenerator.MIN_CENTRE_Y
			+ random.nextInt(LegacyAsteroidGenerator.MAX_CENTRE_Y
				- LegacyAsteroidGenerator.MIN_CENTRE_Y + 1);
		return LegacyAsteroidGenerator.placeRandomAsteroidSlice(world, target, random, x, y, z);
	}
}
