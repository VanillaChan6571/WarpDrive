package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExtendedProperties {
	
	public static final String IDENTIFIER = WarpDrive.MODID;
	
	private EntityLivingBase entityLivingBase;
	
	private GlobalPosition globalPositionHome;
	
	private static final byte UPDATE_FLAG_ALL    = 0x7F;
	private static final byte UPDATE_FLAG_HOME   = 0x01;
	private byte updateFlags;
	
	private static final int SYNC_DELAY_TICKS = 200; // 10 seconds
	private int ticksToSync;
	
	public ExtendedProperties() {
	}
	
	public static ExtendedProperties For(final EntityLivingBase entityLivingBase) {
		return null; // (ExtendedProperties) entityLivingBase.getExtendedProperties(IDENTIFIER);
	}
	
	public void saveNBTData(final NBTTagCompound tagCompound) {
		if (globalPositionHome != null) {
			final NBTTagCompound nbtHome = new NBTTagCompound();
			globalPositionHome.writeToNBT(nbtHome);
			tagCompound.setTag("home", nbtHome);
		}
	}
	
	public void loadNBTData(final NBTTagCompound tagCompound) {
		if (tagCompound.hasKey("home")) {
			final NBTTagCompound nbtHome = tagCompound.getCompoundTag("home");
			globalPositionHome = new GlobalPosition(nbtHome);
		}
	}
	
	public void init(final Entity entity, final World world) {
		if ( world == null
		  || !(entity instanceof EntityLivingBase)
		  || entity.world == null ) {
			WarpDrive.logger.error(String.format("Invalid parameters to ExtendedProperty.init(%s, %s)",
				entity, world));
			return;
		}
		
		entityLivingBase = (EntityLivingBase) entity;
		
		globalPositionHome = null;
		
		updateFlags = UPDATE_FLAG_ALL;
		ticksToSync = world.rand.nextInt(SYNC_DELAY_TICKS);
	}
	
	
	// home
	
	public void setHome(final int dimensionId, final int x, final int y, final int z) {
		setHome(new GlobalPosition(dimensionId, x, y, z));
	}
	
	public void setHome(final GlobalPosition globalPosition) {
		globalPositionHome = globalPosition;
		setUpdateFlag(UPDATE_FLAG_HOME);
	}
	
	public GlobalPosition getHome() {
		return globalPositionHome;
	}
	
	
	// synchronization
	
	private void setUpdateFlag(final int flag) {
		updateFlags |= flag;
	}
	
	public void requestFullSyncWithDelay(final int delay) {
		setUpdateFlag(UPDATE_FLAG_ALL);
		ticksToSync = delay;
	}
	
	public void requestFullSync() {
		requestFullSyncWithDelay(0);
	}
	
	public byte[] getUpdateData() {
		final byte updateFlags_save = updateFlags;
		updateFlags = 0;
		
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
		
		try {
			dataOutputStream.writeInt(entityLivingBase.getEntityId());
			dataOutputStream.writeByte(updateFlags_save);
			
			if ((updateFlags_save & UPDATE_FLAG_HOME) != 0) {
				dataOutputStream.writeBoolean(globalPositionHome != null);
				if (globalPositionHome != null) {
					dataOutputStream.writeInt(globalPositionHome.dimensionId);
					dataOutputStream.writeInt(globalPositionHome.x);
					dataOutputStream.writeInt(globalPositionHome.y);
					dataOutputStream.writeInt(globalPositionHome.z);
				}
			}
			
			return byteArrayOutputStream.toByteArray();
		} catch (final IOException exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Exception while saving extended properties for entity %s",
			                                     entityLivingBase));
		}
		return null;
	}
	
	public boolean handleDataPacket(final byte[] data) {
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
		final DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
		
		try {
			final int entityId = dataInputStream.readInt();
			if (entityId != entityLivingBase.getEntityId()) {
				return false;
			}
			
			final int updateFlags_read = dataInputStream.readByte();
			
			if ((updateFlags_read & UPDATE_FLAG_HOME) != 0) {
				final boolean isHomeSet = dataInputStream.readBoolean();
				if (isHomeSet) {
					final int dimensionId = dataInputStream.readInt();
					final int x = dataInputStream.readInt();
					final int y = dataInputStream.readInt();
					final int z = dataInputStream.readInt();
					globalPositionHome = new GlobalPosition(dimensionId, x, y, z);
				} else {
					globalPositionHome = null;
				}
			}
			
			return true;
		} catch (final IOException exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Exception while reading extended properties for entity %s of %d bytes",
			                                     entityLivingBase, data.length));
			return false;
		}
	}
	
	public void handleSynchronization() {
		if (entityLivingBase.world.isRemote) {
			return;
		}
		
		ticksToSync--;
		if (ticksToSync <= 0) {
			ticksToSync = SYNC_DELAY_TICKS;
			if (updateFlags != 0) {
				final byte[] data = this.getUpdateData();
				// @TODO PacketHandler.sendExtendedProperties(entity, data);
			}
		}
	}
	
	@Override
	public String toString() {
		try {
			return hashCode() + " " + entityLivingBase;
		} catch (final Exception exception) {
			return hashCode() + " (error)";
		}
	}
}
