package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ISecurityStation;
import cr0s.warpdrive.data.PlayerIdName;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Optional;

public class TileEntitySecurityStation extends TileEntityAbstractMachine implements ISecurityStation {
	
	public static final TileEntitySecurityStation DUMMY = new TileEntitySecurityStation(true);
	
	// persistent properties
	private final CopyOnWriteArraySet<PlayerIdName> playerIdNames = new CopyOnWriteArraySet<>();
	
	// computer properties
	private final boolean isDummy;
	
	private TileEntitySecurityStation(final boolean isDummy) {
		super();
		
		this.isDummy = isDummy;
		peripheralName = "warpdriveSecurityStation";
		addMethods(new String[] {
				"getAttachedPlayers",
				"removeAllAttachedPlayers",
				"removeAttachedPlayer"
		});
	}
	
	public TileEntitySecurityStation() {
		this(false);
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		final NBTTagList tagListPlayers = tagCompound.getTagList("players", Constants.NBT.TAG_COMPOUND);
		final ArrayList<PlayerIdName> playerIdNames = new ArrayList<>(tagListPlayers.tagCount());
		for (int index = 0; index < tagListPlayers.tagCount(); index++) {
			final NBTTagCompound tagCompoundEntry = tagListPlayers.getCompoundTagAt(index);
			final PlayerIdName playerIdName = PlayerIdName.loadFromNBT(tagCompoundEntry);
			if (playerIdName == null) {
				WarpDrive.logger.warn(String.format("Skipping invalid PlayerIdName in %s: %s", this, tagCompoundEntry.toString()));
				continue;
			}
			playerIdNames.add(playerIdName);
		}
		this.playerIdNames.clear();
		this.playerIdNames.addAll(playerIdNames);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		final NBTTagList tagListPlayers = new NBTTagList();
		for (final PlayerIdName playerIdName : playerIdNames) {
			tagListPlayers.appendTag(playerIdName.writeToNBT(new NBTTagCompound()));
		}
		tagCompound.setTag("players", tagListPlayers);
		
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("players");
		
		return tagCompound;
	}
	
	@Override
	public WarpDriveText getStatus() {
		return super.getStatus()
		            .append(null, "warpdrive.security_station.guide.registered_players",
		                    getAttachedPlayersList());
	}
	
	public WarpDriveText attachPlayer(final EntityPlayer entityPlayer) {
		if (isDummy) {
			return new WarpDriveText(Commons.getStyleDisabled(), "-dummy-");
		}
		for (final PlayerIdName playerIdName : playerIdNames) {
			if (entityPlayer.getUniqueID().equals(playerIdName.getUUID())) {
				playerIdNames.remove(playerIdName);
				final WarpDriveText text = Commons.getChatPrefix(getBlockType());
				text.appendSibling(new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.security_station.guide.player_unregistered",
				                                     getAttachedPlayersList()));
				markDirty();
				return text;
			}
		}
		
		entityPlayer.attackEntityFrom(DamageSource.GENERIC, 1);
		playerIdNames.add(new PlayerIdName(entityPlayer));
		final WarpDriveText text = Commons.getChatPrefix(getBlockType());
		text.appendSibling(new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.security_station.guide.player_registered",
		                                     getAttachedPlayersList()));
		markDirty();
		return text;
	}
	
	public boolean isAttachedPlayer(final EntityPlayer entityPlayer) {
		if (isDummy) {
			return false;
		}
		
		for (final PlayerIdName playerIdName : playerIdNames) {
			if (entityPlayer.getUniqueID().equals(playerIdName.getUUID())) {
				return true;
			}
		}
		
		return false;
	}
	
	protected String getAttachedPlayersList() {
		if (isDummy) {
			return "<everyone>";
		}
		
		if (playerIdNames.isEmpty()) {
			return "<nobody>";
		}
		
		final StringBuilder stringBuilderList = new StringBuilder();
		
		int index = 0;
		for (final PlayerIdName playerIdName : playerIdNames) {
			final String namePlayer = playerIdName.getName();
			if (index > 0) {
				stringBuilderList.append(", ");
			}
			stringBuilderList.append(namePlayer);
			index++;
		}
		
		return stringBuilderList.toString();
	}
	
	public String getFirstOnlinePlayer() {
		if (playerIdNames.isEmpty()) {// no crew defined
			return null;
		}
		
		for (final PlayerIdName playerIdName : playerIdNames) {
			final EntityPlayer entityPlayer = Commons.getOnlinePlayerByUUID(playerIdName.getUUID());
			if (entityPlayer != null) {// crew member is online
				playerIdName.setName(entityPlayer.getName());
				return playerIdName.getName();
			}
		}
		
		// all cleared
		return null;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getAttachedPlayers() {
		final StringBuilder stringBuilderList = new StringBuilder();
		final String[] namePlayers = new String[playerIdNames.size()];
		
		if (!playerIdNames.isEmpty()) {
			int index = 0;
			for (final PlayerIdName playerIdName : playerIdNames) {
				final String namePlayer = playerIdName.getName();
				if (index > 0) {
					stringBuilderList.append(", ");
				}
				stringBuilderList.append(namePlayer);
				namePlayers[index] = namePlayer;
				index++;
			}
		}
		
		return new Object[] { stringBuilderList.toString(), namePlayers };
	}
	
	@Override
	public Object[] removeAllAttachedPlayers() {
		final int count = playerIdNames.size();
		if (count == 0) {
			return new Object[] { true, "Nothing to do as there's already no attached players." };
		}
		
		playerIdNames.clear();
		return new Object[] { true, String.format("Done, %d players have been removed.", count) };
	}
	
	@Override
	public Object[] removeAttachedPlayer(@Nonnull final Object[] arguments) {
		if (arguments.length != 1 || !(arguments[0] instanceof String)) {
			return new Object[] { false, "Invalid argument, expecting exactly one player name as string." };
		}
		
		final String nameToRemove = (String) arguments[0];
		for (final PlayerIdName playerIdName : playerIdNames) {
			if (nameToRemove.equals(playerIdName.getName())) {
				playerIdNames.remove(playerIdName);
				return new Object[] { true, "Player removed successfully." };
			}
		}
		
		return new Object[] { false, "No player found with that name." };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getAttachedPlayers(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getAttachedPlayers();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] removeAllAttachedPlayers(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return removeAllAttachedPlayers();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] removeAttachedPlayer(final Context context, final Arguments arguments) {
		return removeAttachedPlayer(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getAttachedPlayers":
			return getAttachedPlayers();
			
		case "removeAllAttachedPlayers":
			return removeAllAttachedPlayers();
			
		case "removeAttachedPlayer":
			return removeAttachedPlayer(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
