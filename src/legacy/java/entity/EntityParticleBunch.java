package cr0s.warpdrive.entity;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityParticleBunch extends Entity {
	
	private static final int ACCELERATION_SOUND_UPDATE_TICKS = 10;
	
	private static final double[] PARTICLE_BUNCH_ENERGY_TO_X       = { 0.1D, 1.0D, 10.0D, 100.0D };
	private static final double[] PARTICLE_BUNCH_ENERGY_TO_SOUND_Y = { 0.0D, 1.0D,  2.0D,   3.0D };
	private static final SoundEvent[] PARTICLE_BUNCH_SOUNDS = { SoundEvents.ACCELERATING_LOW, SoundEvents.ACCELERATING_MEDIUM, SoundEvents.ACCELERATING_HIGH };
	
	// persistent properties
	private static final DataParameter<Float> DATA_PARAMETER_ENERGY = EntityDataManager.createKey(EntityParticleBunch.class, DataSerializers.FLOAT);
	public Vector3 vectorNextPosition = new Vector3(0.0D, 0.0D, 0.0D);
	public Vector3 vectorTurningPoint = null;
	
	// computed properties
	private int lastUpdateTicks = 0;
	private static final int UPDATE_TICKS_TIMEOUT = 20;
	private int soundTicks;
	
	public EntityParticleBunch(final World world) {
		super(world);
		
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(String.format("%s created in dimension %s",
			                                    this, Commons.format(world)));
		}
	}
	
	public EntityParticleBunch(final World world, final double x, final double y, final double z) {
		super(world);
		
		this.posX = x + 0.5D;
		this.posY = y + 0.5D;
		this.posZ = z + 0.5D;
		
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(this + " created");
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public int getBrightnessForRender() {
		final int lightFromSky = 0;
		final int lightFromBlock = 15;
		return lightFromSky << 20 | lightFromBlock << 4;
	}
	
	@Override
	public boolean isEntityInvulnerable(@Nonnull final DamageSource source) {
		return true;
	}
	
	public void onRefreshFromSimulation(final double newEnergy, final Vector3 vectorNewPosition, final Vector3 vectorNewTurningPoint) {
		setPosition(vectorNextPosition.x, vectorNextPosition.y, vectorNextPosition.z);
		setEnergy((float) newEnergy);
		vectorNextPosition = vectorNewPosition;
		vectorTurningPoint = vectorNewTurningPoint;
		lastUpdateTicks = 0;
	}
	
	public float getEnergy() {
		return this.dataManager.get(DATA_PARAMETER_ENERGY);
	}
	
	public void setEnergy(final float energy) {
		this.dataManager.set(DATA_PARAMETER_ENERGY, energy);
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
		
		// apply sound effects
		soundTicks--;
		if (soundTicks < 0) {
			final double factor = Commons.interpolate(PARTICLE_BUNCH_ENERGY_TO_X, PARTICLE_BUNCH_ENERGY_TO_SOUND_Y, getEnergy());
			final int indexSound = (int) Math.floor(factor);
			final SoundEvent soundEvent = PARTICLE_BUNCH_SOUNDS[ Commons.clamp(0, PARTICLE_BUNCH_SOUNDS.length - 1, indexSound) ];
			final float pitch = 0.6F + 0.4F * (float) (factor - indexSound);
			
			soundTicks = (int) Math.floor(ACCELERATION_SOUND_UPDATE_TICKS * pitch);
			world.playSound(null, posX, posY, posZ, soundEvent, SoundCategory.HOSTILE, 1.0F, pitch);
		}
	}
	
	@Override
	protected void entityInit() {
		dataManager.register(DATA_PARAMETER_ENERGY, 0.0F);
		
		// entity size is used by vanilla to define render distance, so we force to a high value and adjust in render itself
		setSize(2.0F, 2.0F);
		noClip = true;
		soundTicks = 0;
	}
	
	@Override
	public float getEyeHeight() {
		return 2.0F;
	}
	
	@Override
	public void setDead() {
		super.setDead();
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(this + " dead");
		}
	}
	
	@Override
	protected void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		// energy = tagCompound.getInteger("energy");
		vectorNextPosition = Vector3.createFromNBT(tagCompound.getCompoundTag("nextPosition"));
		if (tagCompound.hasKey("turningPoint")) {
			vectorTurningPoint = Vector3.createFromNBT(tagCompound.getCompoundTag("turningPoint"));
		}
	}
	
	@Override
	protected void writeEntityToNBT(@Nonnull final NBTTagCompound tagCompound) {
		// tagCompound.setDouble("energy", energy);
		tagCompound.setTag("nextPosition", vectorNextPosition.writeToNBT(new NBTTagCompound()));
		if (vectorTurningPoint != null) {
			tagCompound.setTag("turningPoint", vectorTurningPoint.writeToNBT(new NBTTagCompound()));
		}
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