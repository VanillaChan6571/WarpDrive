package cr0s.warpdrive.world;

import cr0s.warpdrive.WarpDrive;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;

public class SpaceChunkProvider extends ChunkGeneratorOverworld {
	
	private final World world;
	private final Random rand;
	
	public SpaceChunkProvider(final World world, final long seed) {
		super(world, seed, false, null);
		
		rand = new Random(seed);
		this.world = world;
	}
	
	@Nonnull
	@Override
	public Chunk generateChunk(final int x, final int z) {
		rand.setSeed(x * 341873128712L + z * 132897987541L);
		
		final ChunkPrimer chunkprimer = new ChunkPrimer();
		// setBlocksInChunk(x, z, chunkprimer);
		
		final Chunk chunk = new Chunk(world, chunkprimer, x, z);
		
		final byte[] byteBiomes = chunk.getBiomeArray();
		for (int i = 0; i < byteBiomes.length; ++i) {
			byteBiomes[i] = (byte) Biome.getIdForBiome(WarpDrive.biomeSpace);
		}
		
		chunk.generateSkylightMap();
		return chunk;
	}
	
	@Override
	public void populate(final int x, final int z) {
		// super.populate(x, z);
	}
	
	@Override
	public boolean generateStructures(@Nonnull final Chunk chunk, final int x, final int z) {
		return false;
	}
	
	@Override
	public @Nonnull List<Biome.SpawnListEntry> getPossibleCreatures(@Nonnull final EnumCreatureType creatureType, @Nonnull final BlockPos blockPos) {
		return new ArrayList<>();
	}
	
	@Nullable
	@Override
	public BlockPos getNearestStructurePos(@Nonnull final World world, final String structureName, @Nonnull final BlockPos blockPos, boolean findUnexplored) {
		return null;
	}
	
	@Override
	public void recreateStructures(final Chunk chunk, final int x, final int z) {
		// no structure generation
	}
}