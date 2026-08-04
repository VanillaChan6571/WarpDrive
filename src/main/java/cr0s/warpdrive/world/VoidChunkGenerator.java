package cr0s.warpdrive.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * Empty-world generator for the WarpDrive dimensions.
 *
 * This exists because `minecraft:flat` with zero layers, while it does produce a void, did not run
 * biome decoration - the asteroid feature was correctly registered and correctly attached to the
 * space biome (the log confirmed both) but never placed a block.
 *
 * Generating nothing is the entire job: fillFromNoise and buildSurfaceAndBedrock are deliberately
 * empty. applyBiomeDecoration is inherited from ChunkGenerator unchanged, which is the point - it
 * runs the biome's configured features, so asteroids generate.
 */
public class VoidChunkGenerator extends ChunkGenerator {

	public static final Codec<VoidChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			BiomeProvider.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
		).apply(instance, VoidChunkGenerator::new));

	/**
	 * An all-air column. Implemented inline rather than using vanilla's helper, whose name differs
	 * between mapping sets.
	 */
	private static final IBlockReader EMPTY_COLUMN = new IBlockReader() {
		@Nullable
		@Override
		public TileEntity getBlockEntity(final BlockPos pos) {
			return null;
		}

		@Override
		public BlockState getBlockState(final BlockPos pos) {
			return Blocks.AIR.defaultBlockState();
		}

		@Override
		public FluidState getFluidState(final BlockPos pos) {
			return Fluids.EMPTY.defaultFluidState();
		}
	};

	public VoidChunkGenerator(final BiomeProvider biomeSource) {
		super(biomeSource, new DimensionStructuresSettings(false));
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public ChunkGenerator withSeed(final long seed) {
		// Nothing here depends on the seed; features derive their own randomness
		return this;
	}

	@Override
	public void buildSurfaceAndBedrock(final WorldGenRegion region, final IChunk chunk) {
		// Intentionally empty: no terrain, no bedrock floor
	}

	@Override
	public void fillFromNoise(final IWorld world, final StructureManager structures, final IChunk chunk) {
		// Intentionally empty: the void is the terrain
	}

	@Override
	public int getBaseHeight(final int x, final int z, final Heightmap.Type type) {
		return 0;
	}

	@Override
	public IBlockReader getBaseColumn(final int x, final int z) {
		return EMPTY_COLUMN;
	}
}
