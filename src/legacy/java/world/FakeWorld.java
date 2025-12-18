package cr0s.warpdrive.world;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;

public class FakeWorld extends World {
	
	private IBlockState blockState;
	private TileEntity tileEntity;
	
	public FakeWorld(final IBlockState blockState, final boolean isRemote) {
		super(null, new WorldInfo(new NBTTagCompound()), new FakeWorldProvider(), null, isRemote);
		this.blockState = blockState;
	}
	
	@Nullable
	@Override
	protected IChunkProvider createChunkProvider() {
		return null;
	}
	
	@Nonnull
	@Override
	public Biome getBiomeForCoordsBody(@Nonnull final BlockPos blockPos) {
		return Biomes.PLAINS;
	}
	
	@Override
	protected boolean isChunkLoaded(final int x, final int z, final boolean allowEmpty) {
		return true;
	}
	
	@Nonnull
	@Override
	public Chunk getChunk(final int chunkX, final int chunkZ) {
		return new FakeChunk(this, 0, 0);
	}
	
	@Nonnull
	@Override
	public IBlockState getBlockState(@Nonnull final BlockPos blockPos) {
		if (blockPos == BlockPos.ORIGIN) {
			return blockState;
		}
		return Blocks.AIR.getDefaultState();
	}
	
	@Override
	public boolean setBlockState(@Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		if (blockPos == BlockPos.ORIGIN) {
			this.blockState = blockState;
			tileEntity = null;
		}
		return true;
	}
	
	@Nullable
	@Override
	public TileEntity getTileEntity(@Nonnull final BlockPos blockPos) {
		if ( blockPos == BlockPos.ORIGIN
		  && blockState.getBlock().hasTileEntity(blockState) ) {
			if (tileEntity == null) {
				tileEntity = blockState.getBlock().createTileEntity(this, blockState);
				if (tileEntity != null) {
					tileEntity.setPos(blockPos);
					tileEntity.setWorld(this);
					tileEntity.validate();
				}
			}
			return tileEntity;
		}
		return null;
	}
	
	@Override
	public long getTotalWorldTime() {
		return System.currentTimeMillis();
	}
	
	@Override
	public boolean canSeeSky(@Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@Override
	public int getLight(@Nonnull final BlockPos blockPos, final boolean checkNeighbors) {
		return 0;
	}
	
	@Override
	public int getHeight(final int x, final int z) {
		return getSeaLevel() + 1;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int getChunksLowestHorizon(final int x, final int z) {
		return getSeaLevel() + 1;
	}
	
	@Override
	public int getLightFor(@Nonnull final EnumSkyBlock type, @Nonnull final BlockPos blockPos) {
		return type.defaultLightValue;
	}
	
	@Nonnull
	@Override
	public BlockPos getPrecipitationHeight(@Nonnull final BlockPos blockPos) {
		return blockPos;
	}
	
	@Nonnull
	@Override
	public BlockPos getTopSolidOrLiquidBlock(@Nonnull final BlockPos blockPos) {
		return BlockPos.ORIGIN;
	}
	
	@Override
	public int getBlockLightOpacity(@Nonnull final BlockPos blockPos) {
		return blockState.getLightOpacity(this, blockPos);
	}
}
