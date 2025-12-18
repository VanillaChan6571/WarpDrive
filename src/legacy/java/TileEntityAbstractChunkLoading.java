package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.event.ChunkLoadingHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import net.minecraftforge.common.ForgeChunkManager.Ticket;

public abstract class TileEntityAbstractChunkLoading extends TileEntityAbstractEnergyConsumer {
	
	// persistent properties
	protected ChunkPos chunkMin = null;
	protected ChunkPos chunkMax = null;
	
	// computed properties
	private Ticket ticket = null;
	private boolean isRefreshNeeded = true;
	protected boolean areChunksLoaded = false;
	
	TileEntityAbstractChunkLoading() {
		super();
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		if (world.isRemote) {
			return;
		}
		
		if ( chunkMin == null
		  || chunkMax == null ) {
			WarpDrive.logger.warn(String.format("%s No chunk coordinates defined, assuming current chunk", this));
			chunkMin = world.getChunk(pos).getPos();
			chunkMax = world.getChunk(pos).getPos();
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		if ( isRefreshNeeded
		  || shouldChunkLoad() != areChunksLoaded ) {
			refreshLoading(isRefreshNeeded);
			isRefreshNeeded = false;
		}
	}
	
	public abstract boolean shouldChunkLoad();
	
	public void refreshChunkLoading() {
		isRefreshNeeded = true;
	}
	
	private void refreshLoading(final boolean force) {
		final boolean shouldChunkLoad = shouldChunkLoad();
		if (shouldChunkLoad) {
			if (ticket == null) {
				chunkloading_giveTicket(ChunkLoadingHandler.forgeTicket_requestNormal(world, this));
			} else if (force) {
				ChunkLoadingHandler.forgeTicket_clearChunks(ticket);
			}
			
			if (!areChunksLoaded || force) {
				final int ticketSize = ticket.getMaxChunkListDepth();
				final ArrayList<ChunkPos> chunksToLoad = getChunksToLoad();
				if (chunksToLoad.size() > ticketSize) {
					WarpDrive.logger.error(String.format("Too many chunk requested for loading %s",
					                                     Commons.format(world, pos)));
					return;
				}
				
				for (final ChunkPos chunk : chunksToLoad) {
					ChunkLoadingHandler.forgeTicket_addChunks(ticket, chunk);
				}
				areChunksLoaded = true;
			}
			
		} else if (ticket != null) {
			ChunkLoadingHandler.forgeTicket_release(ticket);
			ticket = null;
			areChunksLoaded = false;
		}
	}
	
	public void chunkloading_giveTicket(final Ticket ticket) {
		if (this.ticket != null) {
			ChunkLoadingHandler.forgeTicket_release(this.ticket);
			this.ticket = null;
		}
		this.ticket = ticket;
	}
	
	public int chunkloading_getArea() {
		return (chunkMax.x - chunkMin.x + 1)
		     * (chunkMax.z - chunkMin.z + 1);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (chunkMin == null) {
			chunkMin = world.getChunk(pos).getPos();
		}
		
		if (chunkMax == null) {
			chunkMax = world.getChunk(pos).getPos();
		}
		
		tagCompound.setInteger("minChunkX", chunkMin.x);
		tagCompound.setInteger("minChunkZ", chunkMin.z);
		tagCompound.setInteger("maxChunkX", chunkMax.x);
		tagCompound.setInteger("maxChunkZ", chunkMax.z);
		return tagCompound;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		if (tagCompound.hasKey("minChunkX")) {
			final int xMin = tagCompound.getInteger("minChunkX");
			final int zMin = tagCompound.getInteger("minChunkZ");
			chunkMin = new ChunkPos(xMin, zMin);
			
			final int xMax = tagCompound.getInteger("maxChunkX");
			final int zMax = tagCompound.getInteger("maxChunkZ");
			chunkMax = new ChunkPos(xMax, zMax);
		}
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		if (ticket != null) {
			ChunkLoadingHandler.forgeTicket_release(ticket);
			ticket = null;
		}
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	public ArrayList<ChunkPos> getChunksToLoad() {
		if (!shouldChunkLoad()) {
			return null;
		}
		
		assert chunkMin.x <= chunkMax.x;
		assert chunkMin.z <= chunkMax.z;
		
		final int count = chunkloading_getArea();
		if (WarpDriveConfig.LOGGING_CHUNK_LOADING) {
			WarpDrive.logger.info(String.format("Collecting %d chunks to be loaded @ %s from %s to %s",
			                                    count,
			                                    Commons.format(world),
			                                    chunkMin, chunkMax));
		}
		final ArrayList<ChunkPos> chunkCoords = new ArrayList<>(count);
		
		for (int x = chunkMin.x; x <= chunkMax.x; x++) {
			for (int z = chunkMin.z; z <= chunkMax.z; z++) {
				chunkCoords.add(new ChunkPos(x, z));
			}
		}
		
		return chunkCoords;
	}
}
