package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.io.IOException;
import java.lang.ref.WeakReference;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class MovingEntity {
	
	public static final MovingEntity INVALID = new MovingEntity(null);
	
	private final WeakReference<Entity> weakEntity;
	private final WeakReference<World> weakWorld;
	private final int entityId;
	public final Vector3 v3OriginalPosition;
	
	public MovingEntity(final Entity entity) {
		if (entity == null) {
			weakEntity = new WeakReference<>(null);
			weakWorld = new WeakReference<>(null);
			entityId = -1;
			v3OriginalPosition = new Vector3(0, -100, 0);
		} else {
			weakEntity = new WeakReference<>(entity);
			weakWorld = new WeakReference<>(entity.world);
			entityId = entity.getEntityId();
			v3OriginalPosition = new Vector3(entity);
		}
	}
	
	public Entity getEntity() {
		if (entityId < 0) {
			return null;
		}
		
		final Entity entity = weakEntity.get();
		if (entity != null) {
			return entity;
		}
		
		// try to recover
		final World world = weakWorld.get();
		if (world == null) {
			return null;
		}
		return world.getEntityByID(entityId);
	}
	
	public boolean isUnlimited() {
		final Entity entity = getEntity();
		if (!(entity instanceof EntityPlayer)) {
			return false;
		}
		
		final String playerName = entity.getName();
		for (final String unlimitedName : WarpDriveConfig.SHIP_MASS_UNLIMITED_PLAYER_NAMES) {
			if (unlimitedName.equals(playerName)) {
				return true;
			}
		}
		
		return false;
	}
	
	public double getDistanceMoved_square() {
		final Entity entity = getEntity();
		if (entity == null) {// dead or disconnected
			return Double.MAX_VALUE;
		}
		if (entity.world != weakWorld.get()) {// moved to another dimension
			return Double.MAX_VALUE;
		}
		return v3OriginalPosition.distanceTo_square(entity);
	}
	
	public float getMassFactor() {
		final Entity entity = getEntity();
		if (entity == null) {
			return 0.0F;
		}
		
		final NBTTagCompound tagCompound = new NBTTagCompound();
		entity.writeToNBT(tagCompound);
		int mass;
		try {
			final DataOutputLength dataOutputLength = new DataOutputLength();
			CompressedStreamTools.write(tagCompound, dataOutputLength);
			if (WarpDrive.isDev && WarpDriveConfig.LOGGING_TRANSPORTER) {
				WarpDrive.logger.info(String.format("Entity %s estimated mass is %d",
				                                    entity, dataOutputLength.getLength()));
			}
			mass = dataOutputLength.getLength();
		} catch (final IOException exception) {
			mass = (int) Math.sqrt(tagCompound.toString().length());
			WarpDrive.logger.error(String.format("Unable to estimate mass for entity %s, defaulting to %d",
			                                     entity, mass));
		}
		
		// average player data size is 7.5 times smaller (gz compression)
		return Commons.clamp(0.25F, 4.0F, mass / 80000.0F);
	}
}
