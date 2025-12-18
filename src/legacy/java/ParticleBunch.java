package cr0s.warpdrive.data;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.atomic.BlockVoidShellPlain;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorCore;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.entity.EntityParticleBunch;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class ParticleBunch extends Vector3 {
	
	private static final double RADIATION_RADIUS_VOID_PIPE = 2.1D;
	private static final double RADIATION_RADIUS_FREE_FLIGHT = 3.1D;
	private static final int FREE_FLIGHT_MAXIMUM_RANGE = 64;
	private static final double FREE_FLIGHT_ENERGY_FACTOR_PER_TICK = 0.997;
	private static final double SPEED_STEPS = 0.999D;
	
	// persistent properties
	public int id;
	public double energy = TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MINIMUM[0];
	public EnumFacing directionCurrentMotion = null;
	public Vector3 vectorCurrentMotion = new Vector3(0.0D, 0.0D, 0.0D);
	public Vector3 vectorTurningPoint = null;
	private int tickFreeFlight = 0;
	private Vector3 vectorFreeFlightStart = null;
	private VectorI vLastBlock = null;
	
	// calculated properties
	private int entityId = -1;
	private TrajectoryPoint trajectoryPointCurrent = null;
	private boolean isDead = false;
	
	public ParticleBunch(final int x, final int y, final int z, final EnumFacing directionCurrentMotion, final Vector3 vectorCurrentMotion) {
		super(x + 0.5D, y + 0.5D, z + 0.5D);
		this.id = (int) System.nanoTime();
		this.directionCurrentMotion = directionCurrentMotion;
		this.vectorCurrentMotion = vectorCurrentMotion;
	}
	
	public ParticleBunch(final NBTTagCompound tagCompound) {
		super(0.0D, 0.0D, 0.0D);
		readFromNBT(tagCompound);
	}
	
	public boolean onUpdate(final World world, final Map<Integer, AcceleratorControlParameter> mapParameters, final AcceleratorSetup acceleratorSetup) {
		// clear dead entities
		if (entityId >= 0) {
			final Entity entity = world.getEntityByID(entityId);
			if (entity == null || entity.isDead) {
				entityId = -1;
			}
		}
		
		// compute speed
		double speed = getSpeedFromEnergy(energy);
		
		// update position, 1 block at a time
		boolean isChunkLoaded = (speed > 0.0D);
		while (!isDead && speed > 0.0D) {
			isChunkLoaded = isChunkLoaded && moveForward(world, mapParameters, acceleratorSetup, Math.min(SPEED_STEPS, speed));
			speed -= SPEED_STEPS;
		}
		
		// create and remove entity as needed
		Entity entity = null;
		if (entityId < 0 && isChunkLoaded && !isDead) {
			entity = new EntityParticleBunch(world, x, y, z);
			entityId = entity.getEntityId();
			world.spawnEntity(entity);
		} else if (entityId > 0) {
			entity = world.getEntityByID(entityId);
			if (entity == null) {
				entityId = -1;
			} else if (!isChunkLoaded || isDead) {
				entity.setDead();
				entity = null;
				entityId = -1;
			}
		}
		
		// update entity
		if (entity instanceof EntityParticleBunch) {
			final EntityParticleBunch entityParticleBunch = (EntityParticleBunch) entity;
			entityParticleBunch.onRefreshFromSimulation(energy, this, vectorTurningPoint);
			
			// apply radiation
			doIrradiation(world,
				tickFreeFlight > 0 ? RADIATION_RADIUS_FREE_FLIGHT : RADIATION_RADIUS_VOID_PIPE,
				tickFreeFlight > 0 ? 3.0F : 1.0F);
		}
		
		return !isDead;
	}
	
	public void onCollided(final World world, final TrajectoryPoint trajectoryPointCollider) {
		// ignore if there's no entity
		if (entityId < 0) {
			return;
		}
		
		// get entity
		final Entity entity = world.getEntityByID(entityId);
		if (entity == null) {
			return;
		}
		
		// update entity position to collider center
		if (entity instanceof EntityParticleBunch) {
			final EntityParticleBunch entityParticleBunch = (EntityParticleBunch) entity;
			x = trajectoryPointCollider.x + 0.5D;
			y = trajectoryPointCollider.y + 0.5D;
			z = trajectoryPointCollider.z + 0.5D;
			entityParticleBunch.onRefreshFromSimulation(energy, this, vectorTurningPoint);
		}
	}
	
	private boolean moveForward(final World world, final Map<Integer, AcceleratorControlParameter> mapParameters, final AcceleratorSetup acceleratorSetup, final double speed) {
		// get current position
		final VectorI vCurrentBlock = new VectorI((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
		final boolean isNewBlock = vLastBlock == null
		                        || vLastBlock.x != vCurrentBlock.x
		                        || vLastBlock.z != vCurrentBlock.z;
		if (trajectoryPointCurrent == null) {
			trajectoryPointCurrent = (acceleratorSetup == null) ? null : acceleratorSetup.getTrajectoryPoint(vCurrentBlock);
		}
		
		// get current tier
		final int tier = trajectoryPointCurrent == null ? 0 : trajectoryPointCurrent.getTier();
		
		// apply magnets only once per passage
		if ( tier > 0
		  && trajectoryPointCurrent.hasNoMissingVoidShells()
		  && isNewBlock
		  && energy >= TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MINIMUM[tier - 1]
		  && energy <= TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MAXIMUM[tier - 1] ) {
			// linear accelerate
			final int countMagnets = trajectoryPointCurrent.getMagnetsCount();
			final double energy_before = energy;
			energy *= 1.0D + countMagnets * TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_FACTOR_PER_MAGNET[tier - 1];
			energy = Math.min(energy, TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MAXIMUM[tier - 1]);
			if (WarpDrive.isDev && WarpDriveConfig.LOGGING_ACCELERATOR) {
				WarpDrive.logger.info(String.format(this + " accelerating by %d magnets energy %.5f -> %.5f at [%d %d %d]",
						countMagnets, energy_before, energy, vCurrentBlock.x, vCurrentBlock.y, vCurrentBlock.z));
			}
		}
		
		// check control point
		final int controlChannel = trajectoryPointCurrent == null ? -1 : trajectoryPointCurrent.controlChannel;
		final boolean enableControlPoint;
		if ( tier > 0
		  && controlChannel >= 0 ) {
			final AcceleratorControlParameter acceleratorControlParameter = mapParameters.get(controlChannel); 
			final double threshold = Math.max(0.01D, (acceleratorControlParameter  == null ? WarpDriveConfig.ACCELERATOR_THRESHOLD_DEFAULT : acceleratorControlParameter.threshold));
			enableControlPoint = energy > threshold
			                            * TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MAXIMUM[tier - 1];
			if (enableControlPoint && WarpDriveConfig.LOGGING_ACCELERATOR) {
				WarpDrive.logger.info(String.format(this + " control point enabled at [%d %d %d]",
					vCurrentBlock.x, vCurrentBlock.y, vCurrentBlock.z));
			}
		} else {
			enableControlPoint = false;
		}
		
		// get new orientation and turning point, as applicable
		EnumFacing directionNewMotion = null;
		Vector3 vectorNewMotion = null;
		Vector3 vectorNewTurningPoint = null;
		boolean isTurning = false;
		if (trajectoryPointCurrent != null) {
			// recover from free flight => boom
			if (tickFreeFlight != 0 || vectorFreeFlightStart != null) {
				doExplosion(world, "Re-entry");
				return false;
			}
			
			if (!trajectoryPointCurrent.isTransferPipe()) {
				if (enableControlPoint) {
					vectorNewMotion = trajectoryPointCurrent.getJunctionOut(directionCurrentMotion);
					if (vectorNewMotion != null) {
						directionNewMotion = directionCurrentMotion;
						if (WarpDriveConfig.LOGGING_ACCELERATOR) {
							WarpDrive.logger.info(String.format(this + " approaching tier %d transfer towards %s %s",
									tier, directionNewMotion, vectorNewMotion));
						}
					} else if (WarpDriveConfig.LOGGING_ACCELERATOR) {
							WarpDrive.logger.info(String.format(this + " ignoring output junction in other direction",
							        tier, directionNewMotion, vectorNewMotion));
					}
				}
				
				if ( !enableControlPoint
				  || vectorNewMotion == null ) {
					directionNewMotion = trajectoryPointCurrent.getTurnedDirection(directionCurrentMotion);
					if (directionNewMotion != null) {
						isTurning = energy >= TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MINIMUM[tier - 1];
						if (isTurning) {
							vectorNewMotion = new Vector3(directionNewMotion);
							if (WarpDriveConfig.LOGGING_ACCELERATOR) {
								WarpDrive.logger.info(String.format(this + " approaching tier %d turn towards %s %s",
								                                    tier, directionNewMotion, vectorNewMotion));
							}
						} else {
							// cancel turning
							directionNewMotion = null;
							vectorNewMotion = null;
							vectorNewTurningPoint = null;
						}
					}
				}
			
			/*
			} else {// in a transfer trajectory
				if (enableControlPoint) {
					directionNewMotion = trajectoryPointCurrent.getJunctionIn(vectorCurrentMotion);
					if (directionNewMotion != null) {
						vectorNewMotion = new Vector3(directionNewMotion);
					}
				}
			/**/
			}
			
			// when changing direction, compute the turning point
			if (vectorNewMotion != null) {
				// default to center of block
				vectorNewTurningPoint = new Vector3(vCurrentBlock.x + 0.5D, vCurrentBlock.y + 0.5D, vCurrentBlock.z + 0.5D);
				// adjust if it's not a straight 90 deg turn (i.e. it's a 45 deg turn)
				if ( Math.abs(vectorNewMotion.x) != 1.0D
				  && Math.abs(vectorNewMotion.z) != 1.0D ) {
					vectorNewTurningPoint.translateFactor(vectorCurrentMotion, -0.5D);
				}
			}
			
		} else {// (no trajectory point => free flight)
			final IBlockState blockStateCurrent = vCurrentBlock.getBlockState(world);
			final Block blockCurrent = blockStateCurrent.getBlock();
			if (!(blockCurrent instanceof BlockVoidShellPlain)) {
				if (vectorFreeFlightStart == null) {
					vectorFreeFlightStart = new Vector3(x, y, z);
				} else if (distanceTo_square(vectorFreeFlightStart) > FREE_FLIGHT_MAXIMUM_RANGE * FREE_FLIGHT_MAXIMUM_RANGE) {
					doExplosion(world, "Out of range");
					return false;
				}
				tickFreeFlight++;
				if (!blockCurrent.isAir(blockStateCurrent, world, vCurrentBlock.getBlockPos())) {
					if (blockStateCurrent.isOpaqueCube()) {
						doExplosion(world, "Opaque cube");
						return false;
					}
				}
				energy *= FREE_FLIGHT_ENERGY_FACTOR_PER_TICK;
			}
		}
		
		// move forward
		double speedAdjusted = speed;
		Vector3 vectorNewPosition = new Vector3(x + speedAdjusted * vectorCurrentMotion.x,
		                                        y + speedAdjusted * vectorCurrentMotion.y,
		                                        z + speedAdjusted * vectorCurrentMotion.z);
		if (isTurning) {
			Vector3 vectorOldPosition = new Vector3(x, y, z);
			
			// note: at this point both vectorNewMotion and vectorNewTurningPoint are defined
			// rollback if it's a new block so we recover the turning point in case we overshoot last time
			if (isNewBlock) {
				vectorOldPosition = new Vector3(x - vectorCurrentMotion.x, y - vectorCurrentMotion.y, z - vectorCurrentMotion.z);
				speedAdjusted += 1.0D;
			}
			
			// is the turning point ahead of us?
			final Vector3 vectorOffset = vectorNewTurningPoint.clone().subtract(vectorOldPosition);
			if (vectorOffset.x * vectorCurrentMotion.x >= 0.0D && vectorOffset.z * vectorCurrentMotion.z >= 0.0D) {
				// did we just pass the turning point?
				final Vector3 vectorAfter = vectorNewTurningPoint.clone().subtract(vectorNewPosition);
				if (vectorAfter.x * vectorCurrentMotion.x <= 0.0D && vectorAfter.z * vectorCurrentMotion.z <= 0.0D) {
					// adjust position after the turn
					final double distanceLeft = speedAdjusted - vectorOffset.getMagnitude();
					vectorNewPosition = vectorNewTurningPoint.clone().translateFactor(vectorNewMotion, distanceLeft);
					
					// adjust speed
					final double energy_before = energy;
					// final double energyMin = TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MINIMUM[tier - 1];
					// energy = energyMin + (energy - energyMin) * TileEntityAcceleratorCore.ACCELERATOR_PARTICLE_ENERGY_TURN_COEFFICIENTS[tier - 1];
					final double energyMin = TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MINIMUM[tier - 1];
					final double energyMax = TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_MAXIMUM[tier - 1];
					energy = Commons.clamp(energyMin, energyMax, energy * Commons.interpolate(
						new double[] { energyMin, energyMax },
						new double[] { TileEntityAcceleratorCore.PARTICLE_BUNCH_TURN_COEFFICIENTS_AT_MIN_ENERGY[tier - 1], TileEntityAcceleratorCore.PARTICLE_BUNCH_TURN_COEFFICIENTS_AT_MAX_ENERGY[tier - 1] },
						energy));
					if (WarpDriveConfig.LOGGING_ACCELERATOR) {
						WarpDrive.logger.info(String.format(this + " turning energy %.5f -> %.5f at [%d %d %d]",
							energy_before, energy, vCurrentBlock.x, vCurrentBlock.y, vCurrentBlock.z));
					}
					
				} else {
					// cancel turning
					directionNewMotion = null;
					vectorNewMotion = null;
					vectorNewTurningPoint = null;
				}
				
			} else {
				// cancel turning
				directionNewMotion = null;
				vectorNewMotion = null;
				vectorNewTurningPoint = null;
			}
		}
		
		// update properties
		vLastBlock = vCurrentBlock.clone();
		final VectorI vNewBlock = new VectorI((int) Math.floor(vectorNewPosition.x), (int) Math.floor(vectorNewPosition.y), (int) Math.floor(vectorNewPosition.z));
		if (!vNewBlock.equals(vCurrentBlock)) {
			trajectoryPointCurrent = null;
		}
		if (directionNewMotion != null) {
			directionCurrentMotion = directionNewMotion;
			vectorCurrentMotion = vectorNewMotion;
			vectorTurningPoint = vectorNewTurningPoint;
		} else {
			vectorTurningPoint = null;
		}
		x = vectorNewPosition.x;
		y = vectorNewPosition.y;
		z = vectorNewPosition.z;
		
		return Commons.isChunkLoaded(world, vCurrentBlock.x, vCurrentBlock.z)
		    && ( vCurrentBlock == vNewBlock
		      || Commons.isChunkLoaded(world, vNewBlock.x, vNewBlock.z) );
	}
	
	private static double getSpeedFromEnergy(final double energy) {
		return Commons.interpolate(
				TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_TO_SPEEDS_X,
				TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_TO_SPEEDS_Y,
				energy);
	}
	
	private void doIrradiation(@Nonnull final World world, final double radius, final float strength) {
		final AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
			x + 0.5D - radius, y + 0.5D - radius, z + 0.5D - radius,
			x + 0.5D + radius, y + 0.5D + radius, z + 0.5D + radius);
		final List<EntityLivingBase> listEntityLivingBase = world.getEntitiesWithinAABB(EntityLivingBase.class, axisAlignedBB);
		for (final EntityLivingBase entityLivingBase : listEntityLivingBase) {
			WarpDrive.damageIrradiation.onEntityEffect(strength, world, this, entityLivingBase);
		}
	}
	
	private void doExplosion(final World world, final String reason) {
		WarpDrive.logger.info(String.format("Particle bunch explosion due to %s %s",
		                                    reason, Commons.format(world, x, y, z) ));
		if (world instanceof WorldServer) {
			final double explosionStrength = Commons.interpolate(
					TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_TO_EXPLOSION_STRENGTH_X,
					TileEntityAcceleratorCore.PARTICLE_BUNCH_ENERGY_TO_EXPLOSION_STRENGTH_Y,
					energy); 
			final EntityPlayer entityPlayer = CommonProxy.getFakePlayer(null, (WorldServer) world, getBlockPos());
			world.newExplosion(entityPlayer, x, y, z, (float) explosionStrength, true, true);
			doIrradiation(world, explosionStrength, (float) explosionStrength);
			isDead = true;
		}
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		id = tagCompound.getInteger("id");
		try {
			directionCurrentMotion = EnumFacing.valueOf(tagCompound.getString("direction"));
		} catch (final Exception exception) {
			WarpDrive.logger.error(String.format("Invalid direction %s in ParticleBunch NBT %s", tagCompound.getString("direction"), tagCompound));
		}
		vectorCurrentMotion = Vector3.createFromNBT(tagCompound.getCompoundTag("vector"));
		energy = tagCompound.getDouble("energy");
		
		tickFreeFlight = tagCompound.getInteger("freeFlight_ticks");
		if (tagCompound.hasKey("freeFlightStart")) {
			vectorFreeFlightStart = Vector3.createFromNBT(tagCompound.getCompoundTag("freeFlightStart"));
		} else {
			vectorFreeFlightStart = null;
		}
	}
	
	@Override
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setInteger("id", id);
		tagCompound.setString("direction", directionCurrentMotion.name());
		tagCompound.setTag("vector", vectorCurrentMotion.writeToNBT(new NBTTagCompound()));
		tagCompound.setDouble("energy", energy);
		
		tagCompound.setInteger("freeFlight_ticks", tickFreeFlight);
		if (vectorFreeFlightStart != null) {
			tagCompound.setTag("freeFlightStart", vectorFreeFlightStart.writeToNBT(new NBTTagCompound()));
		}
		return tagCompound;
	}
	
	// Hash based collections need a stable hashcode, so we use a unique id instead
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof ParticleBunch) {
			final ParticleBunch particleBunch = (ParticleBunch) object;
			return id == particleBunch.id && x == particleBunch.x && y == particleBunch.y && z == particleBunch.z;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("%s/%d @ (%.2f %.2f %.2f) energy %.5f",
			getClass().getSimpleName(),
			entityId,
			x, y, z,
			energy);
	}
}
