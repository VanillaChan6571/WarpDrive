package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.decoration.BlockGas;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;
import cr0s.warpdrive.render.EntityFXBeam;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CloakedArea {
	
	public int dimensionId;
	public BlockPos blockPosCore;
	public int minX, minY, minZ;
	public int maxX, maxY, maxZ;
	private CopyOnWriteArraySet<UUID> playersInArea;
	public boolean isFullyTransparent;
	public IBlockState blockStateFog;
	
	public CloakedArea(final World world,
	                   final int dimensionId, @Nonnull final BlockPos blockPosCore, final boolean isFullyTransparent,
	                   final int minX, final int minY, final int minZ,
	                   final int maxX, final int maxY, final int maxZ) {
		this.dimensionId = dimensionId;
		this.blockPosCore = blockPosCore;
		this.isFullyTransparent = isFullyTransparent;
		
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		
		this.playersInArea = new CopyOnWriteArraySet<>();
		
		if (world != null) {
			try {
				// Add all players currently inside the field
				final List<EntityPlayer> list = world.getEntitiesWithinAABB(EntityPlayerMP.class, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
				for (final EntityPlayer player : list) {
					addPlayer(player.getUniqueID());
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}
		
		if (!isFullyTransparent) {
			blockStateFog = WarpDrive.blockGas.getDefaultState().withProperty(BlockGas.COLOR, EnumGasColor.DARKNESS);
		} else {
			blockStateFog = Blocks.AIR.getDefaultState();
		}
	}
	
	public boolean isPlayerListedInArea(final UUID uuidPlayer) {
		return playersInArea.contains(uuidPlayer);
	}
	
	private void removePlayer(final UUID uuidPlayer) {
		playersInArea.remove(uuidPlayer);
	}
	
	private void addPlayer(final UUID uuidPlayer) {
		playersInArea.add(uuidPlayer);
	}
	
	public boolean isEntityWithinArea(final EntityLivingBase entity) {
		return (minX <= entity.posX && (maxX + 1) > entity.posX
			 && minY <= (entity.posY + entity.height) && (maxY + 1) > entity.posY
			 && minZ <= entity.posZ && (maxZ + 1) > entity.posZ);
	}
	
	public boolean isBlockWithinArea(final BlockPos blockPos) {
		return (minX <= blockPos.getX() && (maxX + 1) > blockPos.getX()
			 && minY <= blockPos.getY() && (maxY + 1) > blockPos.getY()
			 && minZ <= blockPos.getZ() && (maxZ + 1) > blockPos.getZ());
	}
	
	// Sending only if field changes: sets up or collapsing
	public void sendCloakPacketToPlayersEx(final boolean isUncloaking) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			WarpDrive.logger.info(String.format("sendCloakPacketToPlayersEx isUncloaking %s", isUncloaking));
		}
		final int RADIUS = 250;
		
		final double midX = minX + (Math.abs(maxX - minX) / 2.0D);
		final double midY = minY + (Math.abs(maxY - minY) / 2.0D);
		final double midZ = minZ + (Math.abs(maxZ - minZ) / 2.0D);
		
		final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		for (final EntityPlayerMP entityPlayerMP : server.getPlayerList().getPlayers()) {
			if (entityPlayerMP.dimension == dimensionId) {
				final double dX = midX - entityPlayerMP.posX;
				final double dY = midY - entityPlayerMP.posY;
				final double dZ = midZ - entityPlayerMP.posZ;
				
				if (Math.abs(dX) < RADIUS && Math.abs(dY) < RADIUS && Math.abs(dZ) < RADIUS) {
					if (isUncloaking) {
						PacketHandler.sendCloakPacket(entityPlayerMP, this, true);
						revealChunksToPlayer(entityPlayerMP);
						revealEntitiesToPlayer(entityPlayerMP);
					} else if (!isEntityWithinArea(entityPlayerMP)) {
						PacketHandler.sendCloakPacket(entityPlayerMP, this, false);
					}
				}
			}
		}
	}
	
	public void updatePlayer(final EntityPlayerMP EntityPlayerMP) {
		if (isEntityWithinArea(EntityPlayerMP)) {
			if (!isPlayerListedInArea(EntityPlayerMP.getUniqueID())) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s Player %s has entered",
					                                    this, EntityPlayerMP.getName()));
				}
				addPlayer(EntityPlayerMP.getUniqueID());
				revealChunksToPlayer(EntityPlayerMP);
				revealEntitiesToPlayer(EntityPlayerMP);
				PacketHandler.sendCloakPacket(EntityPlayerMP, this, false);
			}
		} else {
			if (isPlayerListedInArea(EntityPlayerMP.getUniqueID())) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s Player %s has left",
					                                    this, EntityPlayerMP.getName()));
				}
				removePlayer(EntityPlayerMP.getUniqueID());
				final Packet<?> packetToSend = PacketHandler.getPacketForThisEntity(EntityPlayerMP);
				if (packetToSend != null) {
					FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList()
					                .sendToAllNearExcept(
							                EntityPlayerMP,
							                EntityPlayerMP.posX, EntityPlayerMP.posY, EntityPlayerMP.posZ,
							                100,
							                EntityPlayerMP.world.provider.getDimension(),
							                packetToSend);
				}
				PacketHandler.sendCloakPacket(EntityPlayerMP, this, false);
			}
		}
	}
	
	public void revealChunksToPlayer(final EntityPlayer player) {
		if (WarpDriveConfig.LOGGING_CLOAKING) {
			 WarpDrive.logger.info(String.format("%s Revealing cloaked blocks to player %s",
			                                     this, player.getName()));
		}
		final int minY_clamped = Math.max(0, minY);
		final int maxY_clamped = Math.min(255, maxY);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY_clamped; y <= maxY_clamped; y++) {
					BlockPos blockPos = new BlockPos(x, y, z);
					IBlockState blockState = player.world.getBlockState(blockPos);
					if (blockState.getBlock() != Blocks.AIR) {
						player.world.notifyBlockUpdate(blockPos, blockState, blockState, 3);
						
						JumpBlock.refreshBlockStateOnClient(player.world, new BlockPos(x, y, z));
					}
				}
			}
		}
		
		/*
		final ArrayList<Chunk> chunksToSend = new ArrayList<Chunk>();
		
		for (int x = minX >> 4; x <= maxX >> 4; x++) {
			for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
				chunksToSend.add(p.world.getChunk(x, z));
			}
		}
		
		//System.out.println("[Cloak] Sending " + chunksToSend.size() + " chunks to player " + p.username);
		((EntityPlayerMP) p).connection.sendPacketToPlayer(new Packet56MapChunks(chunksToSend));
		
		//System.out.println("[Cloak] Sending decloak packet to player " + p.username);
		area.sendCloakPacketToPlayer(p, true);
		// decloak = true
		
		/**/
	}
	
	public void revealEntitiesToPlayer(final EntityPlayerMP entityPlayerMP) {
		final List<Entity> list = entityPlayerMP.world.getEntitiesWithinAABBExcludingEntity(entityPlayerMP, new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
		
		for (final Entity entity : list) {
			PacketHandler.revealEntityToPlayer(entity, entityPlayerMP);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void clientCloak(@Nonnull final EntityPlayerSP player) {
		assert Commons.isSafeThread();
		
		// Hide the blocks within area
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Refreshing cloaked blocks..."); }
		final World world = player.getEntityWorld();
		final int minY_clamped = Math.max(0, minY);
		final int maxY_clamped = Math.min(255, maxY);
		for (final MutableBlockPos mutableBlockPos : MutableBlockPos.getAllInBoxMutable(minX, minY_clamped, minZ, maxX, maxY_clamped, maxZ)) {
			final IBlockState blockState = world.getBlockState(mutableBlockPos);
			if (blockState.getBlock() != Blocks.AIR) {
				world.setBlockState(mutableBlockPos, blockStateFog, 4);
			}
		}
		
		// Hide any entities inside area
		if (WarpDriveConfig.LOGGING_CLOAKING) { WarpDrive.logger.info("Refreshing cloaked entities..."); }
		final AxisAlignedBB aabb = new AxisAlignedBB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(player, aabb);
		for (final Entity entity : list) {
			world.removeEntity(entity);
			((WorldClient) world).removeEntityFromWorld(entity.getEntityId());
		}
	}
	
	@SideOnly(Side.CLIENT)
	public void clientDecloak() {
		final World world = Minecraft.getMinecraft().world;
		assert world != null;
		world.markBlockRangeForRenderUpdate(
			minX - 1, Math.max(  0, minY - 1), minZ - 1,
			maxX + 1, Math.min(255, maxY + 1), maxZ + 1);
		
		// Make some graphics
		final int numLasers = 80 + world.rand.nextInt(50);
		
		final double centerX = (minX + maxX) / 2.0D;
		final double centerY = (minY + maxY) / 2.0D;
		final double centerZ = (minZ + maxZ) / 2.0D;
		final double radiusX = (maxX - minX) / 2.0D + 5.0D;
		final double radiusY = (maxY - minY) / 2.0D + 5.0D;
		final double radiusZ = (maxZ - minZ) / 2.0D + 5.0D;
		
		for (int i = 0; i < numLasers; i++) {
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(new EntityFXBeam(world,
				new Vector3(
					centerX + radiusX * world.rand.nextGaussian(),
					centerY + radiusY * world.rand.nextGaussian(),
					centerZ + radiusZ * world.rand.nextGaussian()),
				new Vector3(
					centerX + radiusX * world.rand.nextGaussian(),
					centerY + radiusY * world.rand.nextGaussian(),
					centerZ + radiusZ * world.rand.nextGaussian()),
				world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat(),
				60 + world.rand.nextInt(60)));
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			blockPosCore.getX(), blockPosCore.getY(), blockPosCore.getZ(),
			minX, minY, minZ,
			maxX, maxY, maxZ);
	}
}
