package cr0s.warpdrive.entity;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityLaserExploder extends Entity {
	
	// persistent properties
	// (none)
	
	// computed properties
	private int lastUpdateTicks = 0;
	private static final int UPDATE_TICKS_TIMEOUT = 20;
	
	public EntityLaserExploder(final World world) {
		super(world);
		
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(String.format("%s created in dimension %s",
			                                    this, Commons.format(world)));
		}
	}
	
	public EntityLaserExploder(final World world, final BlockPos blockPos) {
		this(world);
		
		setPosition(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D);
	}
	
	// override to skip the block bounding override on client side
	@SideOnly(Side.CLIENT)
	@Override
	public void setPositionAndRotation(final double x, final double y, final double z, final float yaw, final float pitch) {
	//	super.setPositionAndRotation(x, y, z, yaw, pitch);
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}
	
	@Override
	public boolean isEntityInvulnerable(@Nonnull final DamageSource source) {
		return true;
	}
	
	@Override
	public void onUpdate() {
		if (world.isRemote) {
			return;
		}
		
		lastUpdateTicks++;
		if (lastUpdateTicks > UPDATE_TICKS_TIMEOUT) {
			setDead();
		}
	}
	
	@Override
	protected void entityInit() {
		noClip = true;
	}
	
	@Override
	public float getEyeHeight() {
		return 2.0F;
	}
	
	@Override
	public void setDead() {
		super.setDead();
		if (WarpDriveConfig.LOGGING_WEAPON) {
			WarpDrive.logger.info(this + " dead");
		}
	}
	
	@Override
	protected void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		// not applicable
	}
	
	@Override
	protected void writeEntityToNBT(@Nonnull final NBTTagCompound tagCompound) {
		// not applicable
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		return super.writeToNBT(tagCompound);
	}
	
	// prevent saving entity to chunk
	@Override
	public boolean writeToNBTAtomically(@Nonnull final NBTTagCompound tagCompound) {
		return false;
	}
	
	// prevent saving entity to chunk
	@Override
	public boolean writeToNBTOptional(@Nonnull final NBTTagCompound tagCompound) {
		return false;
	}
	
	@Nonnull
	@Override
	public String toString() {
		return String.format("%s/%d %s",
		                     getClass().getSimpleName(),
		                     getEntityId(),
		                     Commons.format(world, posX, posY, posZ));
	}
}