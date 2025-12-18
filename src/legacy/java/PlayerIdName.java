package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class PlayerIdName {
	
	private final UUID uuid;
	private String name;
	
	public PlayerIdName(@Nonnull final EntityPlayer entityPlayer) {
		this.uuid = entityPlayer.getUniqueID();
		this.name = entityPlayer.getName();
	}
	
	private PlayerIdName(@Nonnull final UUID uuid, @Nonnull final String name) {
		this.uuid = uuid;
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(@Nonnull final String name) {
		this.name = name;
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	/**
	 * Return null if NBT is invalid.
	 */
	@Nullable
	public static PlayerIdName loadFromNBT(final NBTTagCompound tagCompound) {
		if (tagCompound == null) {
			return null;
		}
		if ( !tagCompound.getKeySet().contains("UUID")
		  || !tagCompound.getKeySet().contains("name") ) {
			return null;
		}
		final UUID uuid = UUID.fromString(tagCompound.getString("UUID"));
		final String name = tagCompound.getString("name");
		return new PlayerIdName(uuid, name);
	}
	
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setString("UUID", uuid.toString());
		tagCompound.setString("name", name);
		return tagCompound;
	}
}
