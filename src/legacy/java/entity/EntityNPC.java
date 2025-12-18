package cr0s.warpdrive.entity;

import javax.annotation.Nonnull;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;

public class EntityNPC extends EntityLiving {
	
	private static final DataParameter<String> DATA_PARAMETER_TEXTURE = EntityDataManager.createKey(EntityNPC.class, DataSerializers.STRING);
	private static final DataParameter<Float> DATA_PARAMETER_SIZE_SCALE = EntityDataManager.createKey(EntityNPC.class, DataSerializers.FLOAT);
	
	
	public EntityNPC(@Nonnull final World world) {
		super(world);
		
		setCanPickUpLoot(false);
		setNoAI(true);
		setCustomNameTag("WarpDrive NPC");
		setAlwaysRenderNameTag(true);
	}
	
	@Override
	protected void entityInit() {
		super.entityInit();
		dataManager.register(DATA_PARAMETER_TEXTURE, "Fennec");
		dataManager.register(DATA_PARAMETER_SIZE_SCALE, 1.0F);
	}
	
	public void setTextureString(@Nonnull final String textureString) {
		dataManager.set(DATA_PARAMETER_TEXTURE, textureString);
	}
	
	public String getTextureString() {
		return dataManager.get(DATA_PARAMETER_TEXTURE);
	}
	
	@Override
	public float getRenderSizeModifier() {
		return getSizeScale();
	}
	
	public void setSizeScale(final float sizeScale) {
		dataManager.set(DATA_PARAMETER_SIZE_SCALE, sizeScale);
	}
	
	public float getSizeScale() {
		return dataManager.get(DATA_PARAMETER_SIZE_SCALE);
	}
	
	@Override
	public void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readEntityFromNBT(tagCompound);
		
		setTextureString(tagCompound.getString("texture"));
		setSizeScale(tagCompound.getFloat("sizeScale"));
	}
	
	@Override
	public void writeEntityToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeEntityToNBT(tagCompound);
		
		tagCompound.setString("texture", getTextureString());
		tagCompound.setFloat("sizeScale", getSizeScale());
	}
	
	// always save this entity, even when it's dead
	@Override
	public boolean writeToNBTAtomically(@Nonnull final NBTTagCompound tagCompound) {
		final String entityString = this.getEntityString();
		
		if (entityString != null) {
			tagCompound.setString("id", entityString);
			writeToNBT(tagCompound);
			return true;
		} else {
			return false;
		}
	}
	
	// always save this entity, even when it's dead
	@Override
	public boolean writeToNBTOptional(@Nonnull final NBTTagCompound tagCompound) {
		final String entityString = getEntityString();
		
		if (entityString != null && !isRiding()) {
			tagCompound.setString("id", entityString);
			writeToNBT(tagCompound);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
	}
	
	@Override
	protected boolean isMovementBlocked() {
		return true;
	}
}
