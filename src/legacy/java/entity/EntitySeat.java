package cr0s.warpdrive.entity;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntitySeat extends Entity {
	
	private BlockPos blockPos;
	
	public EntitySeat(@Nonnull final World world) {
		super(world);
		
		setSize(0.25F, 0.25F);
	}
	
	@Override
	protected void entityInit() {
		// no operation, abstract parent
	}
	
	@Override
	public double getMountedYOffset() {
		return -0.25;
	}
	
	@Nonnull
	@Override
	public BlockPos getPosition() {
		if ( blockPos == null
		  || blockPos.getX() != (int) posX
		  || blockPos.getY() != (int) posY
		  || blockPos.getZ() != (int) posZ ) {
			if (blockPos != null) {
				WarpDrive.logger.error(String.format("EntitySeat has moved unexpectedly from %s to (%.3f %.3f %.3f): %s",
				                                     blockPos, posX, posY, posZ, this ));
			}
			blockPos = new BlockPos(posX, posY, posZ);
		}
		return blockPos;
	}
	
	@Override
	public void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		// no operation, abstract parent
	}
	
	@Override
	public void writeEntityToNBT(@Nonnull final NBTTagCompound tagCompound) {
		// no operation, abstract parent
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		final BlockPos blockPos = getPosition();
		
		if (!world.isAirBlock(blockPos)) {
			setDead();
			return;
		}
		
		// remove when no longer mounted or passenger is too far away
		final List<Entity> passengers = getPassengers();
		if (passengers.isEmpty()) {
			setDead();
		}
		for (final Entity entityPassenger : passengers) {
			if ( entityPassenger.isSneaking()
			  || entityPassenger.getDistanceSq(this) >= 1.0D ) {
				setDead();
			}
		}
	}
	
	@Override
	public void setDead() {
		super.setDead();
		
		// enforce server synchronization
		if (world.isRemote) {
			PacketHandler.sendUnseating();
		}
	}
	
	// ensure we remain static
	@Override
	public boolean canBeAttackedWithItem() {
		return false;
	}
	
	@Override
	public void applyEntityCollision(@Nonnull final Entity entity) {
		// no collision
	}
	
	@Override
	public void addVelocity(final double x, final double y, final double z) {
		// no motion
	}
	
	// ensure no rendering happens
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRender3d(final double x, final double y, final double z) {
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(final double distance) {
		return false;
	}
}
