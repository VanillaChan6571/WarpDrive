package cr0s.warpdrive.data;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import net.minecraftforge.common.DimensionManager;

public class GlobalPosition {
	
	public final int dimensionId;
	public final int x, y, z;
	private BlockPos cache_blockPos;
	
	public GlobalPosition(final int dimensionId, final int x, final int y, final int z) {
		this.dimensionId = dimensionId;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public GlobalPosition(final int dimensionId, @Nonnull final BlockPos blockPos) {
		this.dimensionId = dimensionId;
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
	}
	
	public GlobalPosition(@Nonnull final GlobalPosition globalPosition) {
		this(globalPosition.dimensionId, globalPosition.x, globalPosition.y, globalPosition.z);
		this.cache_blockPos = globalPosition.cache_blockPos;
	}
	
	public GlobalPosition(@Nonnull final TileEntity tileEntity) {
		this(tileEntity.getWorld().provider.getDimension(), tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
	}
	
	public GlobalPosition(@Nonnull final Entity entity) {
		this(entity.world.provider.getDimension(),
			(int) Math.floor(entity.posX),
			(int) Math.floor(entity.posY),
			(int) Math.floor(entity.posZ));
	}
	
	public WorldServer getWorldServerIfLoaded() {
		final WorldServer world = DimensionManager.getWorld(dimensionId);
		// skip unloaded worlds
		if (world == null) {
			return null;
		}
		
		boolean isLoaded = false;
		final ChunkProviderServer chunkProviderServer = world.getChunkProvider();
		try {
			final long i = ChunkPos.asLong(x >> 4, z >> 4);
			final Chunk chunk = chunkProviderServer.loadedChunks.get(i);
			if (chunk != null) {
				isLoaded = !chunk.unloadQueued;
			}
		} catch (final NoSuchFieldError exception) {
			isLoaded = chunkProviderServer.chunkExists(x >> 4, z >> 4);
		}
		// skip unloaded chunks
		if (!isLoaded) {
			return null;
		}
		return world;
	}
	
	public boolean isLoaded() {
		return getWorldServerIfLoaded() != null;
	}
	
	public CelestialObject getCelestialObject(final boolean isRemote) {
		return CelestialObjectManager.get(isRemote, dimensionId, x, z);
	}
	
	public Vector3 getUniversalCoordinates(final boolean isRemote) {
		final CelestialObject celestialObject = CelestialObjectManager.get(isRemote, dimensionId, x, z);
		return GlobalRegionManager.getUniversalCoordinates(celestialObject, x, y, z);
	}
	
	public BlockPos getBlockPos() {
		if ( cache_blockPos == null
		  || cache_blockPos.getX() != x
		  || cache_blockPos.getY() != y
		  || cache_blockPos.getZ() != z ) {
			cache_blockPos = new BlockPos(x, y, z);
		}
		return cache_blockPos;
	}
	
	public int distance2To(@Nonnull final TileEntity tileEntity) {
		if (tileEntity.getWorld().provider.getDimension() != dimensionId) {
			return Integer.MAX_VALUE;
		}
		final int newX = tileEntity.getPos().getX() - x;
		final int newY = tileEntity.getPos().getY() - y;
		final int newZ = tileEntity.getPos().getZ() - z;
		return newX * newX + newY * newY + newZ * newZ;
	}
	
	public double distance2To(@Nonnull final Entity entity) {
		if (entity.world.provider.getDimension() != dimensionId) {
			return Double.MAX_VALUE;
		}
		final double newX = entity.posX - x;
		final double newY = entity.posY - y;
		final double newZ = entity.posZ - z;
		return newX * newX + newY * newY + newZ * newZ;
	}
	
	public GlobalPosition(@Nonnull final NBTTagCompound tagCompound) {
		dimensionId = tagCompound.getInteger("dimensionId");
		x = tagCompound.getInteger("x");
		y = tagCompound.getInteger("y");
		z = tagCompound.getInteger("z");
	}
	
	public void writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setInteger("dimensionId", dimensionId);
		tagCompound.setInteger("x", x);
		tagCompound.setInteger("y", y);
		tagCompound.setInteger("z", z);
	}
	
	public boolean equals(@Nonnull final TileEntity tileEntity) {
		return dimensionId == tileEntity.getWorld().provider.getDimension()
			&& x == tileEntity.getPos().getX() && y == tileEntity.getPos().getY() && z == tileEntity.getPos().getZ();
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof GlobalPosition) {
			final GlobalPosition globalPosition = (GlobalPosition) object;
			return (dimensionId == globalPosition.dimensionId) && (x == globalPosition.x) && (y == globalPosition.y) && (z == globalPosition.z);
		} else if (object instanceof VectorI) {
			final VectorI vector = (VectorI) object;
			return (x == vector.x) && (y == vector.y) && (z == vector.z);
		} else if (object instanceof TileEntity) {
			final TileEntity tileEntity = (TileEntity) object;
			return (dimensionId == tileEntity.getWorld().provider.getDimension())
			    && (x == tileEntity.getPos().getX())
			    && (y == tileEntity.getPos().getY())
			    && (z == tileEntity.getPos().getZ());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return dimensionId << 24 + (x >> 10) << 12 + y << 10 + (z >> 10);
	}
	
	@Override
	public String toString() {
		return String.format("GlobalPosition{DIM%d (%d %d %d)}",
		                     dimensionId, x, y, z );
	}
}