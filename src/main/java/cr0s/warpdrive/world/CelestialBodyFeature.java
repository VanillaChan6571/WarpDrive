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

import java.util.Random;

/**
 * Rare, mineable planetary systems in space.
 *
 * This deliberately uses normal 1.16.5 feature generation rather than StructureBuilder. Every
 * chunk derives the same system from the world seed and writes only its own 16x16 vertical slice.
 * Consequently generation order cannot clip a body, no neighbouring chunks are force-loaded, and
 * there is no enormous whole-planet block list retained on the heap. StructureBuilder remains the
 * right tool for bodies created at runtime by a command or event, outside the worldgen pipeline.
 */
public final class CelestialBodyFeature extends Feature<NoFeatureConfig> {

	public CelestialBodyFeature(final Codec<NoFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(final ISeedReader world, final ChunkGenerator generator,
	                     final Random ignored, final BlockPos origin, final NoFeatureConfig config) {
		final ChunkPos chunk = new ChunkPos(origin);
		final int regionX = Math.floorDiv(chunk.x, CelestialSystemLayout.GRID_SPACING);
		final int regionZ = Math.floorDiv(chunk.z, CelestialSystemLayout.GRID_SPACING);
		boolean placed = false;

		// A system is far smaller than a grid cell, so the current and eight adjacent cells are
		// sufficient even when its planet was selected close to a cell boundary.
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				placed |= placeSystemSlice(world, chunk, regionX + offsetX, regionZ + offsetZ);
			}
		}
		return placed;
	}

	private static boolean placeSystemSlice(final ISeedReader world, final ChunkPos chunk,
	                                        final int regionX, final int regionZ) {
		boolean placed = false;
		for (final CelestialSystemLayout.Body body :
		     CelestialSystemLayout.systemBodies(world.getSeed(), regionX, regionZ)) {
			placed |= placeBodySlice(world, chunk, body);
		}
		return placed;
	}

	/** Writes only the requested chunk's X/Z columns. */
	private static boolean placeBodySlice(final ISeedReader world, final ChunkPos chunk,
	                                      final CelestialSystemLayout.Body body) {
		final int minX = chunk.getMinBlockX();
		final int minZ = chunk.getMinBlockZ();
		final int maxX = minX + 15;
		final int maxZ = minZ + 15;
		final int horizontalReach = body.radius + 2;
		if (body.x + horizontalReach < minX
		 || body.x - horizontalReach > maxX
		 || body.z + horizontalReach < minZ
		 || body.z - horizontalReach > maxZ) {
			return false;
		}

		boolean placed = false;
		final BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = minX; x <= maxX; x++) {
			final int dx = x - body.x;
			for (int z = minZ; z <= maxZ; z++) {
				final int dz = z - body.z;
				final int horizontalSquared = dx * dx + dz * dz;
				if (horizontalSquared > horizontalReach * horizontalReach) {
					continue;
				}
				final int minY = Math.max(1, body.y - body.radius - 2);
				final int maxY = Math.min(254, body.y + body.radius + 2);
				for (int y = minY; y <= maxY; y++) {
					final int dy = y - body.y;
					final double distance = Math.sqrt(horizontalSquared + dy * dy);
					// Coordinate hashing gives a stable rough surface without sharing mutable Random state
					// between chunks or depending on the order in which they generate.
					final double roughness = (unitHash(body.seed, x, y, z) - 0.5D)
						* (body.moon ? 2.8D : 3.6D);
					if (distance > body.radius + roughness) {
						continue;
					}

					pos.set(x, y, z);
					if (!world.getBlockState(pos).isAir()) {
						continue;
					}
					final double depth = distance / body.radius;
					world.setBlock(pos, material(body, depth, x, y, z), 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	private static BlockState material(final CelestialSystemLayout.Body body, final double depth,
	                                   final int x, final int y, final int z) {
		final double roll = unitHash(body.seed ^ 0x4D41_5445_5249_414CL, x, y, z);
		if (depth < 0.30D) {
			if (roll < 0.025D) return Blocks.DIAMOND_ORE.defaultBlockState();
			if (roll < 0.070D) return Blocks.GOLD_ORE.defaultBlockState();
			if (roll < 0.16D) return Blocks.IRON_ORE.defaultBlockState();
			return body.moon ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
		}
		if (depth < 0.82D) {
			if (roll < 0.035D) return Blocks.REDSTONE_ORE.defaultBlockState();
			if (roll < 0.075D) return Blocks.LAPIS_ORE.defaultBlockState();
			switch (body.theme) {
				case DESERT:   return (roll < 0.55D ? Blocks.SANDSTONE : Blocks.TERRACOTTA).defaultBlockState();
				case ICE:      return (roll < 0.70D ? Blocks.PACKED_ICE : Blocks.BLUE_ICE).defaultBlockState();
				case VOLCANIC: return (roll < 0.60D ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState();
				default:       return (roll < 0.70D ? Blocks.STONE : Blocks.ANDESITE).defaultBlockState();
			}
		}

		// Surface materials are all non-falling blocks; sand/gravel would turn a newly generated
		// spherical body into a server-tick storm before WarpDrive's zero-g suppression catches up.
		switch (body.theme) {
			case TERRAN:
				return (roll < 0.62D ? Blocks.DIRT : roll < 0.82D ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE)
					.defaultBlockState();
			case DESERT:
				return (roll < 0.68D ? Blocks.SMOOTH_SANDSTONE : Blocks.RED_SANDSTONE).defaultBlockState();
			case ICE:
				return (roll < 0.78D ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK).defaultBlockState();
			default:
				return (roll < 0.58D ? Blocks.BLACKSTONE : roll < 0.88D ? Blocks.BASALT : Blocks.MAGMA_BLOCK)
					.defaultBlockState();
		}
	}

	private static double unitHash(final long seed, final int x, final int y, final int z) {
		final long value = mix(seed ^ (long) x * 0x632BE59BD9B4E019L
			^ (long) y * 0x9E3779B97F4A7C15L ^ (long) z * 0x94D049BB133111EBL);
		return (value >>> 11) * 0x1.0p-53;
	}

	private static long mix(final long value) {
		return CelestialSystemLayout.mix(value);
	}
}
