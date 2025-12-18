package cr0s.warpdrive.world;

import cr0s.warpdrive.WarpDrive;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityStarCore extends Entity {
	
	public int xCoord;
	public int yCoord;
	public int zCoord;
	
	private int radius;
	
	private static final int KILL_RADIUS = 60;
	private static final int BURN_RADIUS = 200;
	//private final int ROCKET_INTERCEPT_RADIUS = 100; //disabled
	private boolean isLogged = false;
	
	private static final int ENTITY_ACTION_INTERVAL = 10; // ticks
	
	private int ticks = 0;
	
	public EntityStarCore(final World world) {
		super(world);
	}
	
	public EntityStarCore(final World world, final int x, final int y, final int z, final int radius) {
		super(world);
		
		this.xCoord = x;
		this.posX = x;
		this.yCoord = y;
		this.posY = y;
		this.zCoord = z;
		this.posZ = z;
		this.radius = radius;
	}
	
	private void actionToEntitiesNearStar() {
		final int xMax, yMax, zMax;
		final int xMin, yMin, zMin;
		final int MAX_RANGE = radius + KILL_RADIUS + BURN_RADIUS;// + ROCKET_INTERCEPT_RADIUS;
		final int KILL_RANGESQ = (radius + KILL_RADIUS) * (radius + KILL_RADIUS);
		final int BURN_RANGESQ = (radius + KILL_RADIUS + BURN_RADIUS) * (radius + KILL_RADIUS + BURN_RADIUS);
		xMin = xCoord - MAX_RANGE;
		xMax = xCoord + MAX_RANGE;
		
		zMin = zCoord - MAX_RANGE;
		zMax = zCoord + MAX_RANGE;
		
		yMin = yCoord - MAX_RANGE;
		yMax = yCoord + MAX_RANGE;
		final AxisAlignedBB aabb = new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax);
		final List list = world.getEntitiesWithinAABBExcludingEntity(this, aabb);
		
		if (!isLogged) {
			isLogged = true;
			WarpDrive.logger.info(this + " Capture range " + MAX_RANGE + " X " + xMin + " to " + xMax + " Y " + yMin + " to " + yMax + " Z " + zMin + " to " + zMax);
		}
		for (final Object object : list) {
			if (!(object instanceof Entity)) {
				continue;
			}
			
			if (object instanceof EntityLivingBase) {
				final EntityLivingBase entityLivingBase = (EntityLivingBase) object;
				
				//System.out.println("Found: " + entity.getEntityName() + " distance: " + entity.getDistanceToEntity(this));
				
				// creative bypass
				if (entityLivingBase.isEntityInvulnerable(WarpDrive.damageWarm)) {
					continue;
				}
				if (entityLivingBase instanceof EntityPlayer) {
					final EntityPlayer entityPlayer = (EntityPlayer) entityLivingBase;
					if (entityPlayer.capabilities.disableDamage) {
						continue;
					}
				}
				
				final double distanceSq = entityLivingBase.getDistanceSq(this);
				if (distanceSq <= KILL_RANGESQ) {
					// 100% kill, ignores any protection
					entityLivingBase.attackEntityFrom(DamageSource.ON_FIRE, 9000);
					entityLivingBase.attackEntityFrom(DamageSource.GENERIC, 9000);
					if (!entityLivingBase.isDead) {
						WarpDrive.logger.warn(String.format("Forcing entity death due to star proximity: %s", entityLivingBase));
						entityLivingBase.setDead();
					}
				} else if (distanceSq <= BURN_RANGESQ) {
					// burn entity
					if (!entityLivingBase.isImmuneToFire()) {
						entityLivingBase.setFire(3);
					}
					entityLivingBase.attackEntityFrom(DamageSource.ON_FIRE, 1);
				}
			}/* else { // Intercept ICBM rocket and kill

				   Entity entity = (Entity) o;
				   if (entity.getDistanceToEntity(this) <= (this.radius + ROCKET_INTERCEPT_RADIUS)) {
				       System.out.println("[SC] Intercepted entity: " + entity.getEntityName());
				       world.removeEntity(entity);
				   }
				}*/
		}
	}
	
	@Override
	public void onUpdate() {
		if (world.isRemote) {
			return;
		}
		
		if (++ticks > ENTITY_ACTION_INTERVAL) {
			ticks = 0;
			actionToEntitiesNearStar();
		}
	}
	
	@Override
	protected void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		xCoord = tagCompound.getInteger("x");
		yCoord = tagCompound.getInteger("y");
		zCoord = tagCompound.getInteger("z");
		radius = tagCompound.getInteger("radius");
	}
	
	@Override
	protected void entityInit() {
		noClip = true;
	}
	
	// override to skip the block bounding override on client side
	@Override
	public void setPositionAndRotation(final double x, final double y, final double z, final float yaw, final float pitch) {
		//	super.setPositionAndRotation(x, y, z, yaw, pitch);
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}
	
	@Override
	protected void writeEntityToNBT(final NBTTagCompound tagCompound) {
		tagCompound.setInteger("x", xCoord);
		tagCompound.setInteger("y", yCoord);
		tagCompound.setInteger("z", zCoord);
		tagCompound.setInteger("radius", radius);
	}
	
	@Override
	public boolean shouldRenderInPass(final int pass) {
		return false;
	}
}