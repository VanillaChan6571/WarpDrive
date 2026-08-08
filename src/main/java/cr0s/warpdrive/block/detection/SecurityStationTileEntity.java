package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persisted UUID/name allowlist with the legacy registration toggle and CC management API. */
public class SecurityStationTileEntity extends TileEntity {

	private static final String TAG_PLAYERS = "players";
	private static final String TAG_UUID = "uuid";
	private static final String TAG_NAME = "name";
	private final Map<UUID, String> players = new LinkedHashMap<>();
	private boolean enabled = true;

	public SecurityStationTileEntity() {
		super(Registration.SECURITY_STATION_TILE.get());
	}

	public String togglePlayer(final PlayerEntity player) {
		final UUID uuid = player.getUUID();
		if (players.remove(uuid) != null) {
			setChanged();
			return "Player unregistered. Registered players: " + getPlayersList();
		}
		player.hurt(DamageSource.GENERIC, 1.0F);
		players.put(uuid, player.getGameProfile().getName());
		setChanged();
		return "Player registered. Registered players: " + getPlayersList();
	}

	public boolean isAttachedPlayer(final PlayerEntity player) {
		return players.containsKey(player.getUUID());
	}

	public boolean isEnabled() { return enabled; }

	@Nullable
	public String getFirstOnlinePlayer() {
		if (!(level instanceof ServerWorld)) return null;
		for (final Map.Entry<UUID, String> entry : players.entrySet()) {
			final ServerPlayerEntity player = ((ServerWorld) level).getServer()
				.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				final String currentName = player.getGameProfile().getName();
				if (!currentName.equals(entry.getValue())) {
					entry.setValue(currentName);
					setChanged();
				}
				return currentName;
			}
		}
		return null;
	}

	public String getStatus() {
		return "Registered players: " + getPlayersList();
	}

	private String getPlayersList() {
		return players.isEmpty() ? "<nobody>" : String.join(", ", players.values());
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null && enabled != requested) {
			enabled = requested;
			setChanged();
		}
		return new Object[]{ enabled };
	}

	public Object[] getAttachedPlayers() {
		final String[] names = players.values().toArray(new String[0]);
		return new Object[]{ String.join(", ", names), names };
	}

	public Object[] removeAllAttachedPlayers() {
		final int count = players.size();
		if (count == 0) {
			return new Object[]{ true, "Nothing to do as there's already no attached players." };
		}
		players.clear();
		setChanged();
		return new Object[]{ true, String.format("Done, %d players have been removed.", count) };
	}

	public Object[] removeAttachedPlayer(final String name) {
		for (final Map.Entry<UUID, String> entry : new ArrayList<>(players.entrySet())) {
			if (name.equals(entry.getValue())) {
				players.remove(entry.getKey());
				setChanged();
				return new Object[]{ true, "Player removed successfully." };
			}
		}
		return new Object[]{ false, "No player found with that name." };
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		enabled = !tagCompound.contains("isEnabled") || tagCompound.getBoolean("isEnabled");
		players.clear();
		final ListNBT list = tagCompound.getList(TAG_PLAYERS, Constants.NBT.TAG_COMPOUND);
		for (int index = 0; index < list.size(); index++) {
			final CompoundNBT playerTag = list.getCompound(index);
			if (!playerTag.hasUUID(TAG_UUID)) continue;
			players.put(playerTag.getUUID(TAG_UUID), playerTag.getString(TAG_NAME));
		}
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putBoolean("isEnabled", enabled);
		final ListNBT list = new ListNBT();
		for (final Map.Entry<UUID, String> entry : players.entrySet()) {
			final CompoundNBT playerTag = new CompoundNBT();
			playerTag.putUUID(TAG_UUID, entry.getKey());
			playerTag.putString(TAG_NAME, entry.getValue());
			list.add(playerTag);
		}
		tagCompound.put(TAG_PLAYERS, list);
		return tagCompound;
	}
}
