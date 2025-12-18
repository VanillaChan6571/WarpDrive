package cr0s.warpdrive.damage;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public class DamageIrradiation extends DamageSource {

	public DamageIrradiation() {
		super("warpdrive.irradiation");
		
		setDamageBypassesArmor();
		setDamageIsAbsolute();
	}
	
	public void onWorldEffect(final World world, final Vector3 v3Position, final float strength) {
		// only search up to distance where damage applied is 0.5
		final double radius = Math.sqrt(2.0D * strength);
		final AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
			v3Position.x - radius, v3Position.y - radius, v3Position.z - radius,
			v3Position.x + radius, v3Position.y + radius, v3Position.z + radius);
		final List<EntityLivingBase> listEntityLivingBase = world.getEntitiesWithinAABB(EntityLivingBase.class, axisAlignedBB);
		if (listEntityLivingBase != null) {
			for (final EntityLivingBase entityLivingBase : listEntityLivingBase) {
				// cap damage below 1 m distance, since the entity is never really inside the source and damage tends to +INF
				final float distance = Math.min(1.0F, (float) Math.sqrt(v3Position.distanceTo_square(entityLivingBase)));
				onEntityEffect(strength / (distance * distance), world, v3Position, entityLivingBase);
			}
		}
	}
	
	public void onEntityEffect(final float strength, final World world, final Vector3 v3Source, final Entity entity) {
		if ( strength <= 0.0F
		  || !(entity instanceof EntityLivingBase)
		  || entity.isDead ) {
			return;
		}
		
		// common particle effects properties
		final Vector3 v3Entity = new Vector3(entity);
		final Vector3 v3Direction = new Vector3(entity).subtract(v3Source).normalize();
		final Vector3 v3From = v3Source.clone();
		v3From.translateFactor(v3Direction, 0.6D);
		v3Entity.translateFactor(v3Direction, -0.6D);
		
		final double speed = Math.abs(strength);
		final Vector3 v3Motion = v3Direction.clone().scale(speed); // new Vector3(entity.motionX, entity.motionY, entity.motionZ);
		if (WarpDrive.isDev && WarpDriveConfig.LOGGING_ACCELERATOR) {
			PacketHandler.sendBeamPacket(world, v3From, v3Entity,
			                             0.25F, 0.75F, 0.38F, 10, 0, 50);
			WarpDrive.logger.info(String.format("%s strength %.1f speed %.3f entity %s source %s direction %s motion %s entity %s",
			                                    this, strength, speed, v3Entity, v3Source, v3Direction, v3Motion, entity));
		}
		
		// apply damages and particle effects
		entity.attackEntityFrom(this, strength);
		
		// visual effect
		v3Direction.scale(0.20D);
		PacketHandler.sendSpawnParticlePacket(world, "mobSpell", (byte) Commons.clamp(3, 10, strength), v3Entity, v3Direction,
		                                      0.20F + 0.10F * world.rand.nextFloat(),
		                                      0.90F + 0.10F * world.rand.nextFloat(),
		                                      0.40F + 0.15F * world.rand.nextFloat(),
		                                      0.0F, 0.0F, 0.0F, 32);
	}
}
