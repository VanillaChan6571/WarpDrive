package cr0s.warpdrive.event;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.network.PacketHandler;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
*
* @author LemADEC
*/
public class WorldHandler {
	
	//TODO: register as event receiver
	public void onChunkLoaded(final ChunkWatchEvent event) {
		final Chunk chunk = event.getChunkInstance();
		assert chunk != null;
		
		// Check chunk for locating in cloaked areas
		WarpDrive.logger.info(String.format("onChunkLoaded %d %d", chunk.x, chunk.z));
		WarpDrive.cloaks.onChunkLoaded(event.getPlayer(), chunk.x, chunk.z);
		
		/*
		List<Chunk> list = new ArrayList<Chunk>();
		list.add(c);
		
		// Send obscured chunk
		WarpDrive.logger.info(String.format("[Cloak] Sending to player %s obscured chunk at (%d %d)",
		                                    p, chunk.x, chunk.z));
		((EntityPlayerMP)p).connection.sendPacketToPlayer(new Packet56MapChunks(list));
		*/
	}
	
	// Server side
	@SubscribeEvent
	public void onEntityJoinWorld(final EntityJoinWorldEvent event){
		if (event.getWorld().isRemote) {
			return;
		}
		// WarpDrive.logger.info(String.format("onEntityJoinWorld %s", event.entity));
		if (event.getEntity() instanceof EntityLivingBase) {
			final EntityLivingBase entityLivingBase = (EntityLivingBase) event.getEntity();
			final int x = MathHelper.floor(entityLivingBase.posX);
			final int y = MathHelper.floor(entityLivingBase.posY);
			final int z = MathHelper.floor(entityLivingBase.posZ);
			final CelestialObject celestialObject = CelestialObjectManager.get(event.getWorld(), x, z);
			
			if (entityLivingBase instanceof EntityPlayerMP) {
				WarpDrive.cloaks.onPlayerJoinWorld((EntityPlayerMP) entityLivingBase, event.getWorld());
				PacketHandler.sendClientSync((EntityPlayerMP) entityLivingBase, celestialObject);
				
			} else {
				if (celestialObject == null) {
					// unregistered dimension => exit
					return;
				}
				if (entityLivingBase.ticksExisted > 5) {
					// just changing dimension
					return;
				}
				if (!celestialObject.hasAtmosphere()) {
					final boolean canJoin = BreathingManager.onLivingJoinEvent(entityLivingBase, x, y, z);
					if (!canJoin) {
						event.setCanceled(true);
					}
				}
				if (!celestialObject.isInsideBorder(entityLivingBase.posX, entityLivingBase.posZ)) {
					event.setCanceled(true);
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerChangedDimension(final PlayerChangedDimensionEvent event) {
		WarpDrive.logger.info(String.format("onPlayerChangedDimension %s %d -> %d (%.1f %.1f %.1f)",
		                                    event.player.getName(), event.fromDim, event.toDim,
		                                    event.player.posX, event.player.posY, event.player.posZ ));
		WarpDrive.cloaks.onPlayerJoinWorld((EntityPlayerMP) event.player, ((EntityPlayerMP) event.player).world);
	}
	
	// Client side
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientConnectedToServer(final ClientConnectedToServerEvent event) {
		// WarpDrive.logger.info(String.format("onClientConnectedToServer connectionType %s isLocal %s", event.connectionType, event.isLocal));
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onWorldUnload(final WorldEvent.Unload event) {
		// WarpDrive.logger.info(String.format("onWorldUnload world %s", Commons.format(event.getWorld()));
		WarpDrive.cloaks.onClientChangingDimension();
	}
	
	@SubscribeEvent
	public void onServerTick(final ServerTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END) {
			return;
		}
		
		AbstractSequencer.updateTick();
		LivingHandler.updateTick();
	}
	
	// BreakEvent = entity is breaking a block (no ancestor)
	// EntityPlaceEvent = entity is EntityFallingBlock
	// HarvestDropsEvent = collecting drops
	// NeighborNotifyEvent = neighbours update, snow placed/removed by environment, WorldEdit (can't be cancelled)
	// PlaceEvent (EntityPlaceEvent) = player is (re)placing a block
	// PortalSpawnEvent = nether portal is opening (fire placed inside an obsidian frame)
	@SuppressWarnings("ConstantConditions")
	@SubscribeEvent
	public void onBlockEvent(final BlockEvent blockEvent) {
		if ( WarpDriveConfig.isGregtechLoaded
		  && blockEvent.getWorld().getWorldInfo().getWorldName().equals("DummyServer") ) {
			return;
		}
		
		final Entity entity;
		final IBlockState blockStateBefore;
		final IBlockState blockStatePlaced;
		if (blockEvent instanceof BlockEvent.EntityPlaceEvent) {
			final BlockEvent.EntityPlaceEvent entityPlaceEvent = (BlockEvent.EntityPlaceEvent) blockEvent; 
			entity = entityPlaceEvent.getEntity();
			if (entity instanceof EntityPlayer) {
				blockStateBefore = entityPlaceEvent.getBlockSnapshot().getReplacedBlock();
				blockStatePlaced = entityPlaceEvent.getPlacedBlock();
			} else {
				blockStateBefore = entityPlaceEvent.getPlacedAgainst();
				blockStatePlaced = entityPlaceEvent.getPlacedBlock();
			}
		} else if (blockEvent instanceof BlockEvent.BreakEvent) {
			entity = ((BlockEvent.BreakEvent) blockEvent).getPlayer();
			blockStateBefore = blockEvent.getWorld().getBlockState(blockEvent.getPos());
			blockStatePlaced = blockEvent.getState();
		} else {
			entity = null;
			blockStateBefore = blockEvent.getWorld().getBlockState(blockEvent.getPos());
			blockStatePlaced = blockEvent.getState();
		}
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_BREAK_PLACE) {
			if (blockStateBefore != blockStatePlaced) {
				WarpDrive.logger.info(String.format("onBlockEvent %s %s -> %s %s by %s",
				                                    blockEvent.getClass().getSimpleName(),
				                                    blockStateBefore,
				                                    blockStatePlaced,
				                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
				                                    entity ));
			} else {
				WarpDrive.logger.info(String.format("onBlockEvent %s %s %s by %s",
				                                    blockEvent.getClass().getSimpleName(),
				                                    blockStatePlaced,
				                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
				                                    entity ));
			}
		}
		boolean isAllowed = true;
		if ( blockEvent instanceof BlockEvent.MultiPlaceEvent
		  || blockEvent instanceof BlockEvent.EntityMultiPlaceEvent ) {
			final List<BlockSnapshot> listBlockSnapshots = blockEvent instanceof BlockEvent.MultiPlaceEvent
			                                             ? ((BlockEvent.MultiPlaceEvent) blockEvent).getReplacedBlockSnapshots()
			                                             : ((BlockEvent.EntityMultiPlaceEvent) blockEvent).getReplacedBlockSnapshots();
			for (final BlockSnapshot blockSnapshot : listBlockSnapshots) {
				final IBlockState blockStateCurrent = blockSnapshot.getCurrentBlock();
				isAllowed = isAllowed && GlobalRegionManager.onBlockUpdating(entity, blockEvent.getWorld(), blockSnapshot.getPos(), blockStateCurrent);
				if (blockStateCurrent != blockSnapshot.getReplacedBlock()) {
					isAllowed = isAllowed && GlobalRegionManager.onBlockUpdating(entity, blockEvent.getWorld(), blockSnapshot.getPos(), blockSnapshot.getReplacedBlock());
				}
			}
		} else if (blockEvent instanceof BlockEvent.PortalSpawnEvent) {
			isAllowed = isAllowed && CelestialObjectManager.onOpeningNetherPortal(blockEvent.getWorld(), blockEvent.getPos());
		} else {
			isAllowed = isAllowed && GlobalRegionManager.onBlockUpdating(entity, blockEvent.getWorld(), blockEvent.getPos(), blockEvent.getState());
		}
		if ( blockEvent instanceof BlockEvent.BreakEvent
		  && entity instanceof EntityPlayer ) {
			isAllowed = isAllowed && PlayerHandler.checkMaintenanceAndCrew(blockEvent, (EntityPlayer) entity, blockEvent.getPos(), blockStateBefore);
		}
		if (!isAllowed) {
			if (blockEvent.isCancelable()) {
				blockEvent.setCanceled(true);
			} else if (blockEvent instanceof BlockEvent.HarvestDropsEvent) {
				if (Commons.throttleMe("WorldHandler.onBlockEvent")) {
					WarpDrive.logger.info(String.format("Skipping HarvestDropsEvent %s %s %s by %s",
					                                    blockEvent.getClass().getSimpleName(),
					                                    blockStatePlaced,
					                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
					                                    entity ));
				}
			} else {
				try {
					blockEvent.getWorld().setBlockToAir(blockEvent.getPos());
				} catch (final Exception exception) {
					if (Commons.throttleMe("WorldHandler.onBlockEvent")) {
						exception.printStackTrace();
						WarpDrive.logger.info(String.format("Exception with %s %s %s by %s",
						                                    blockEvent.getClass().getSimpleName(),
						                                    blockStatePlaced,
						                                    Commons.format(blockEvent.getWorld(), blockEvent.getPos()),
						                                    entity ));
					}
				}
			}
			return;
		}
		
		if ( blockEvent instanceof BlockEvent.MultiPlaceEvent
		  || blockEvent instanceof BlockEvent.EntityMultiPlaceEvent ) {
			final List<BlockSnapshot> listBlockSnapshots = blockEvent instanceof BlockEvent.MultiPlaceEvent
			                                             ? ((BlockEvent.MultiPlaceEvent) blockEvent).getReplacedBlockSnapshots()
			                                             : ((BlockEvent.EntityMultiPlaceEvent) blockEvent).getReplacedBlockSnapshots();
			for (final BlockSnapshot blockSnapshot : listBlockSnapshots) {
				ChunkHandler.onBlockUpdated(blockEvent.getWorld(), blockSnapshot.getPos());
			}
		} else {
			ChunkHandler.onBlockUpdated(blockEvent.getWorld(), blockEvent.getPos());
		}
	}
}
