package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CloakManager {
	
	private static final CopyOnWriteArraySet<CloakedArea> cloaks = new CopyOnWriteArraySet<>();
	private static final CopyOnWriteArraySet<CloakedArea> cloakToRefresh = new CopyOnWriteArraySet<>();
	
	public CloakManager() { }
	
	public void onChunkLoaded(final EntityPlayerMP player, final int chunkPosX, final int chunkPosZ) {
		for (final CloakedArea area : cloaks) {
			// skip other dimensions
			if (area.dimensionId != player.world.provider.getDimension()) {
				continue;
			}
			
			// force refresh if the chunk overlap the cloak
			if ( area.minX <= (chunkPosX << 4 + 15) && area.maxX >= (chunkPosX << 4)
			  && area.minZ <= (chunkPosZ << 4 + 15) && area.maxZ >= (chunkPosZ << 4) ) {
				PacketHandler.sendCloakPacket(player, area, false);
			}
		}
	}
	
	public void onPlayerJoinWorld(final EntityPlayerMP entityPlayerMP, final World world) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("CloakManager.onPlayerJoinWorld %s", entityPlayerMP));
		}
		for (final CloakedArea area : cloaks) {
			// skip other dimensions
			if (area.dimensionId != world.provider.getDimension()) {
				continue;
			}
			
			// force refresh if player is outside the cloak
			if ( area.minX > entityPlayerMP.posX || area.maxX < entityPlayerMP.posX
			  || area.minY > entityPlayerMP.posY || area.maxY < entityPlayerMP.posY
			  || area.minZ > entityPlayerMP.posZ || area.maxZ < entityPlayerMP.posZ ) {
				PacketHandler.sendCloakPacket(entityPlayerMP, area, false);
			}
		}
	}
	
	public boolean isAreaExists(final World world, final BlockPos blockPos) {
		return (getCloakedArea(world, blockPos) != null);
	}
	
	public CloakedArea updateCloakedArea(
			@Nonnull final World world, @Nonnull final BlockPos blockPosCore, final boolean isFullyTransparent,
			final int minX, final int minY, final int minZ,
			final int maxX, final int maxY, final int maxZ) {
		final CloakedArea cloakedAreaNew = new CloakedArea(world, world.provider.getDimension(), blockPosCore, isFullyTransparent, minX, minY, minZ, maxX, maxY, maxZ);
		
		// find existing one
		for (final CloakedArea cloakedArea : cloaks) {
			if ( cloakedArea.dimensionId == world.provider.getDimension()
			  && cloakedArea.blockPosCore.equals(blockPosCore) ) {
				cloaks.remove(cloakedArea);
				break;
			}
		}
		cloaks.add(cloakedAreaNew);
		if (world.isRemote) {
			cloakToRefresh.add(cloakedAreaNew);
		}
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("Cloak count is %s", cloaks.size()));
		}
		return cloakedAreaNew;
	}
	
	@SideOnly(Side.CLIENT)
	public void onClientTick() {
		@Nullable
		final EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null) {
			// skip without clearing the cache while client world is loading
			return;
		}
		final CloakedArea[] cloakedAreas = cloakToRefresh.toArray(new CloakedArea[0]);
		cloakToRefresh.clear();
		for (final CloakedArea cloakedArea : cloakedAreas) {
			cloakedArea.clientCloak(player);
		}
	}
	
	public void removeCloakedArea(final int dimensionId, final BlockPos blockPos) {
		for (final CloakedArea area : cloaks) {
			if ( area.dimensionId == dimensionId
			  && area.blockPosCore.equals(blockPos) ) {
				if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
					area.clientDecloak();
				} else {
					area.sendCloakPacketToPlayersEx(true); // send info about collapsing cloaking field
				}
				cloaks.remove(area);
				break;
			}
		}
	}
	
	public CloakedArea getCloakedArea(final World world, final BlockPos blockPos) {
		for (final CloakedArea area : cloaks) {
			if ( area.dimensionId == world.provider.getDimension()
			  && area.blockPosCore.equals(blockPos) ) {
				return area;
			}
		}
		
		return null;
	}
	
	public void updatePlayer(final EntityPlayerMP entityPlayerMP) {
		for (final CloakedArea area : cloaks) {
			area.updatePlayer(entityPlayerMP);
		}
	}
	
	@Nullable
	public static CloakedArea getContainingArea(@Nonnull final World world, @Nonnull final BlockPos blockPos1, @Nullable final BlockPos blockPos2) {
		for (final CloakedArea area : cloaks) {
			if ( area.dimensionId == world.provider.getDimension()
			  && area.isBlockWithinArea(blockPos1) ) {
				return blockPos2 == null || area.isBlockWithinArea(blockPos2) ? area : null;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unused") // Core mod
	@SideOnly(Side.CLIENT)
	public static boolean WorldClient_invalidateRegionAndSetBlock_setBlockState(final BlockPos blockPos, final IBlockState blockState, final int flags) {
		final World world = Minecraft.getMinecraft().world;
		assert world != null;
		
		if (blockState.getBlock() != Blocks.AIR) {
			for (final CloakedArea area : cloaks) {
				if (area.isBlockWithinArea(blockPos)) {
					if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info("CloakManager block is inside");
					}
					if (!area.isEntityWithinArea(Minecraft.getMinecraft().player)) {
						if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
							WarpDrive.logger.info("CloakManager player is outside");
						}
						return world.setBlockState(blockPos, area.blockStateFog, flags);
					}
				}
			}
		}
		return world.setBlockState(blockPos, blockState, flags);
	}
	
	@SuppressWarnings("unused") // Core mod
	@SideOnly(Side.CLIENT)
	public static void Chunk_read(final Chunk chunk) {
		final int chunkX_min = chunk.x * 16;
		final int chunkX_max = chunk.x * 16 + 15;
		final int chunkZ_min = chunk.z * 16;
		final int chunkZ_max = chunk.z * 16 + 15;
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("CloakManager Chunk_read (%d %d) %d cloak(s) from (%d %d) to (%d %d)",
			                                    chunk.x, chunk.z, cloaks.size(),
			                                    chunkX_min, chunkZ_min, chunkX_max, chunkZ_max));
		}
		
		for (final CloakedArea area : cloaks) {
			if ( area.minX <= chunkX_max && area.maxX >= chunkX_min
			  && area.minZ <= chunkZ_max && area.maxZ >= chunkZ_min ) {
				if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info("CloakManager chunk is inside");
				}
				if (!area.isEntityWithinArea(Minecraft.getMinecraft().player)) {
					if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info("CloakManager player is outside");
					}
					
					final int areaX_min = Math.max(chunkX_min, area.minX) & 15;
					final int areaX_max = Math.min(chunkX_max, area.maxX) & 15;
					final int areaZ_min = Math.max(chunkZ_min, area.minZ) & 15;
					final int areaZ_max = Math.min(chunkZ_max, area.maxZ) & 15;
					
					for (int x = areaX_min; x <= areaX_max; x++) {
						for (int z = areaZ_min; z <= areaZ_max; z++) {
							for (int y = area.maxY; y >= area.minY; y--) {
								if (chunk.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.AIR) {
									chunk.setBlockState(new BlockPos(x, y, z), area.blockStateFog);
								}
								
							}
						}
					}
				}
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void onClientChangingDimension() {
		cloaks.clear();
	}
}
