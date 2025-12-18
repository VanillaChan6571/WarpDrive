package cr0s.warpdrive.event;

import com.google.common.collect.ImmutableSet;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractChunkLoading;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

public class ChunkLoadingHandler implements LoadingCallback {
	
	public final static ChunkLoadingHandler INSTANCE = new ChunkLoadingHandler();
	
	/* event catchers */
	@Override
	public void ticketsLoaded(final List<Ticket> tickets, final World world) {
		for (final Ticket ticket : tickets) {
			final NBTTagCompound tagCompound = ticket.getModData();
			if ( !tagCompound.hasKey("posX")
			  || !tagCompound.hasKey("posY")
			  || !tagCompound.hasKey("posZ") ) {
				WarpDrive.logger.error(String.format("Unable to resume chunkloading: incomplete or corrupted NBT data %s",
				                                     tagCompound));
				ForgeChunkManager.releaseTicket(ticket);
				continue;
			}
			
			final int x = tagCompound.getInteger("posX");
			final int y = tagCompound.getInteger("posY");
			final int z = tagCompound.getInteger("posZ");
			final TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
			if (!(tileEntity instanceof TileEntityAbstractChunkLoading)) {
				WarpDrive.logger.error(String.format("Unable to resume chunkloading %s: invalid tile entity %s",
				                                     Commons.format(world, x, y, z),
				                                     tileEntity == null ? "-null-" : tileEntity));
				ForgeChunkManager.releaseTicket(ticket);
				continue;
			}
			
			final TileEntityAbstractChunkLoading tileEntityAbstractChunkLoading = (TileEntityAbstractChunkLoading) tileEntity;
			final boolean shouldChunkLoad = tileEntityAbstractChunkLoading.shouldChunkLoad();
			if (!shouldChunkLoad) {
				WarpDrive.logger.warn(String.format("Unable to resume chunkloading %s: chunk loader is disabled or out of power %s",
				                                    Commons.format(world, x, y, z),
				                                    tileEntity));
				ForgeChunkManager.releaseTicket(ticket);
				continue;
			}
			
			WarpDrive.logger.info(String.format("Resuming chunkloading of %s", tileEntity));
			tileEntityAbstractChunkLoading.chunkloading_giveTicket(ticket);
			tileEntityAbstractChunkLoading.refreshChunkLoading();
		}
	}
	
	/* Forge wrappers */
	public static Ticket forgeTicket_requestNormal(final World world, final TileEntity tileEntity) {
		if (ForgeChunkManager.ticketCountAvailableFor(WarpDrive.instance, world) <= 0) {
			WarpDrive.logger.error(String.format("No ChunkLoader tickets available for %s",
			                                     Commons.format(world)));
			return null;
		}
		
		final Ticket ticket = ForgeChunkManager.requestTicket(WarpDrive.instance, world, Type.NORMAL);
		if (ticket == null) {
			WarpDrive.logger.error(String.format("Failed to register ChunkLoader Ticket for %s",
			                                     Commons.format(world)));
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_CHUNK_LOADING) {
			WarpDrive.logger.info(String.format("Forcing chunk loading %s",
			                                    Commons.format(ticket.world, tileEntity.getPos())));
		}
		
		final NBTTagCompound tagCompound = ticket.getModData();
		tagCompound.setString("id", tileEntity.getClass().getSimpleName());
		tagCompound.setInteger("posX", tileEntity.getPos().getX());
		tagCompound.setInteger("posY", tileEntity.getPos().getY());
		tagCompound.setInteger("posZ", tileEntity.getPos().getZ());
		
		return ticket;
	}
	
	public static void forgeTicket_release(final Ticket ticket) {
		if (ticket == null) {
			return;
		}
		
		forgeTicket_clearChunks(ticket);
		
		ForgeChunkManager.releaseTicket(ticket);
	}
	
	public static void forgeTicket_addChunks(final Ticket ticket, final ChunkPos chunk) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("Forcing chunk loading @ %s %s",
			                                    Commons.format(ticket.world),
			                                    chunk));
		}
		ForgeChunkManager.forceChunk(ticket, chunk);
	}
	
	public static void forgeTicket_clearChunks(final Ticket ticket) {
		if (ticket == null) {
			return;
		}
		
		if (WarpDriveConfig.LOGGING_CHUNK_LOADING) {
			final NBTTagCompound tagCompound = ticket.getModData();
			final int x = tagCompound.getInteger("posX");
			final int y = tagCompound.getInteger("posY");
			final int z = tagCompound.getInteger("posZ");
			WarpDrive.logger.info(String.format("Releasing chunk loading %s",
			                                    Commons.format(ticket.world, x, y, z)));
		}
		
		final ImmutableSet<ChunkPos> chunks = ticket.getChunkList();
		for (final ChunkPos chunk : chunks) {
			if (WarpDrive.isDev) {
				WarpDrive.logger.info(String.format("Releasing chunk loading @ %s %s",
				                                    Commons.format(ticket.world),
				                                    chunk));
			}
			ForgeChunkManager.unforceChunk(ticket, chunk);
		}
	}
}