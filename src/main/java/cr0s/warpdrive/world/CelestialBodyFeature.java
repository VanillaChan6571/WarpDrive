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
 * Mineable planetary systems using 1.12.2's layered Orb profiles.
 *
 * The system layout remains native to this port. Each chunk deterministically reconstructs the
 * same planet and moons, then writes only its own X/Z slice. Body radii, shells, gas interiors,
 * and material sets come from the legacy moon structures rather than simplified depth bands.
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

		// The largest legacy body system is still much smaller than a grid cell.
		for (int offsetX = -1; offsetX <= 1; offsetX++) {
			for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
				for (final CelestialSystemLayout.Body body :
				     CelestialSystemLayout.systemBodies(world.getSeed(),
					     regionX + offsetX, regionZ + offsetZ)) {
					placed |= LegacyAsteroidGenerator.placeCelestialBodySlice(world, chunk, body);
				}
			}
		}
		return placed;
	}
}
