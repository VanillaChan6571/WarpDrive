package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Receives player chat addressed to its configured name and exposes the command to computers. */
public class VirtualAssistantTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	private static final String TAG_ENABLED = "enabled";
	private static final String TAG_NAME = "name";
	private static final String TAG_LAST_COMMAND = "lastCommand";
	private static final Set<VirtualAssistantTileEntity> LOADED =
		Collections.newSetFromMap(new WeakHashMap<>());

	private boolean enabled = true;
	private boolean active;
	private String assistantName = "";
	private String lastCommand = "";
	private final Set<CommandListener> listeners =
		Collections.newSetFromMap(new WeakHashMap<>());

	public VirtualAssistantTileEntity() {
		super(Registration.VIRTUAL_ASSISTANT_TILE.get());
	}

	private VirtualAssistantTier getTier() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof VirtualAssistantBlock
			? ((VirtualAssistantBlock) blockState.getBlock()).getTier() : VirtualAssistantTier.BASIC;
	}

	@Override public int getMaxEnergyStored() { return getTier().getCapacity(); }
	@Override protected int getMaxReceive() { return 512; }
	@Override protected int getMaxExtract() { return 0; }

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		final int cost = getTier().getEnergyPerTick();
		active = enabled && consumeEnergy(cost, false);
		final BlockState blockState = getBlockState();
		if (blockState.getValue(VirtualAssistantBlock.ACTIVE) != active) {
			level.setBlock(worldPosition, blockState.setValue(VirtualAssistantBlock.ACTIVE, active), 3);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide) synchronized (LOADED) { LOADED.add(this); }
	}

	@Override
	public void onChunkUnloaded() {
		synchronized (LOADED) { LOADED.remove(this); }
		super.onChunkUnloaded();
	}

	@Override
	public void setRemoved() {
		synchronized (LOADED) { LOADED.remove(this); }
		listeners.clear();
		super.setRemoved();
	}

	/** Dispatches one chat message to every loaded assistant in the player's dimension. */
	public static boolean dispatchChat(final ServerPlayerEntity player, final String message) {
		final ArrayList<VirtualAssistantTileEntity> snapshot;
		synchronized (LOADED) { snapshot = new ArrayList<>(LOADED); }
		boolean consumed = false;
		for (final VirtualAssistantTileEntity assistant : snapshot) {
			if (!assistant.isRemoved() && assistant.level == player.level) {
				consumed |= assistant.onChatReceived(player, message);
			}
		}
		return consumed;
	}

	private boolean onChatReceived(final ServerPlayerEntity player, final String message) {
		if (!active || assistantName.length() < 3 || assistantName.contains("/")
		 || assistantName.contains("!") || level == null) return false;
		final float range = getTier().getRange();
		final AxisAlignedBB area = new AxisAlignedBB(
			worldPosition.getX() - range, worldPosition.getY() - range, worldPosition.getZ() - range,
			worldPosition.getX() + range + 1.0D, worldPosition.getY() + range + 1.0D,
			worldPosition.getZ() + range + 1.0D);
		if (!area.contains(player.position())
		 || !message.regionMatches(true, 0, assistantName, 0, assistantName.length())) return false;
		lastCommand = message.substring(assistantName.length()).trim();
		setChanged();
		for (final CommandListener listener : new ArrayList<>(listeners)) {
			listener.queueEvent("virtualAssistantCommand", lastCommand);
		}
		return true;
	}

	public String getAssistantName() { return assistantName; }

	public void setAssistantName(@Nullable final String name) {
		final String sanitized = name == null ? "" : name.replace("/", "")
			.replace(".", "").replace(":", "").replace('\\', '.');
		if (assistantName.equals(sanitized)) return;
		assistantName = sanitized;
		setChanged();
	}

	public boolean isEnabled() { return enabled; }
	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		setChanged();
	}

	public Object[] getLastCommand() {
		return lastCommand.isEmpty()
			? new Object[]{ false, "No command received." }
			: new Object[]{ true, lastCommand };
	}

	public Object[] pullLastCommand() {
		if (lastCommand.isEmpty()) return new Object[]{ false, "No command received." };
		final String command = lastCommand;
		lastCommand = "";
		setChanged();
		return new Object[]{ true, command };
	}

	public void addCommandListener(final CommandListener listener) { listeners.add(listener); }
	public void removeCommandListener(final CommandListener listener) { listeners.remove(listener); }

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		assistantName = tag.getString(TAG_NAME);
		lastCommand = tag.getString(TAG_LAST_COMMAND);
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putBoolean(TAG_ENABLED, enabled);
		tag.putString(TAG_NAME, assistantName);
		tag.putString(TAG_LAST_COMMAND, lastCommand);
		return tag;
	}

	public interface CommandListener {
		void queueEvent(String eventName, Object... arguments);
	}
}
