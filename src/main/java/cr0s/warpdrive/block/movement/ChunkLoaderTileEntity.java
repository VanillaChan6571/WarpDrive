package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** The dedicated loader's FE accounting, persisted controls, upgrades, and Forge tickets. */
public class ChunkLoaderTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	public static final int MAX_ENERGY = 1_000_000;
	private static final int MAX_RECEIVE = 1_024;

	private static final String TAG_ENABLED = "isEnabled";
	private static final String TAG_POWERED = "isPowered";
	private static final String TAG_NAME = "name";
	private static final String TAG_NEGATIVE_X = "radiusXneg";
	private static final String TAG_POSITIVE_X = "radiusXpos";
	private static final String TAG_NEGATIVE_Z = "radiusZneg";
	private static final String TAG_POSITIVE_Z = "radiusZpos";
	private static final String TAG_UPGRADES = "upgrades";
	private static final String TAG_EFFICIENCY = "chunk_loader.efficiency";
	private static final String TAG_RANGE = "chunk_loader.range";
	private static final String TAG_COMPUTER_INTERFACE = "base.computer_interface";
	private static final String TAG_TICKETS_ACTIVE = "chunkTicketsActive";
	private static final String TAG_TICKET_NEGATIVE_X = "ticketRadiusXneg";
	private static final String TAG_TICKET_POSITIVE_X = "ticketRadiusXpos";
	private static final String TAG_TICKET_NEGATIVE_Z = "ticketRadiusZneg";
	private static final String TAG_TICKET_POSITIVE_Z = "ticketRadiusZpos";

	public enum Upgrade {
		EFFICIENCY,
		RANGE,
		COMPUTER_INTERFACE
	}

	private ChunkLoaderBounds bounds = ChunkLoaderBounds.centered(0);
	private ChunkLoaderBounds ticketBounds = ChunkLoaderBounds.centered(0);
	private final Set<ChunkPos> forcedChunks = new HashSet<>();
	private String name = "";
	private boolean enabled = true;
	private boolean powered;
	private boolean active;
	private boolean ticketsActive;
	private boolean persistedTicketsNeedClear;
	private boolean ticketsDirty = true;
	private int efficiencyUpgrades;
	private int rangeUpgrades;
	private int computerInterfaceUpgrades;

	public ChunkLoaderTileEntity() {
		super(Registration.CHUNK_LOADER_TILE.get());
	}

	@Override public int getMaxEnergyStored() { return MAX_ENERGY; }
	@Override protected int getMaxReceive() { return MAX_RECEIVE; }
	@Override protected int getMaxExtract() { return 0; }
	@Override protected boolean canReceiveFrom(@Nullable final Direction side) { return true; }

	@Override
	public void tick() {
		if (!(level instanceof ServerWorld)) return;
		final ServerWorld world = (ServerWorld) level;
		final int energyRequired = calculateEnergyRequired();
		final boolean nextPowered = consumeEnergy(energyRequired, !enabled);
		final boolean shouldLoad = enabled && nextPowered;

		if (shouldLoad) {
			synchroniseTickets(world);
		} else {
			releaseTickets(world);
		}

		final boolean nextActive = shouldLoad && ticketsActive;
		if (powered != nextPowered || active != nextActive) {
			powered = nextPowered;
			active = nextActive;
			setChanged();
		}
		updateBlockState(world);
	}

	private void synchroniseTickets(final ServerWorld world) {
		if (persistedTicketsNeedClear) {
			clearPersistedTickets(world);
			persistedTicketsNeedClear = false;
			forcedChunks.clear();
			ticketsDirty = true;
		}
		if (!ticketsDirty) return;

		final Set<ChunkPos> desired = getDesiredChunks();
		final Iterator<ChunkPos> iterator = forcedChunks.iterator();
		while (iterator.hasNext()) {
			final ChunkPos chunk = iterator.next();
			if (!desired.contains(chunk)) {
				setTicket(world, chunk, false);
				iterator.remove();
			}
		}
		for (final ChunkPos chunk : desired) {
			if (forcedChunks.add(chunk)) setTicket(world, chunk, true);
		}
		ticketBounds = bounds;
		ticketsActive = !forcedChunks.isEmpty();
		ticketsDirty = false;
		setChanged();
	}

	private Set<ChunkPos> getDesiredChunks() {
		final ChunkPos self = new ChunkPos(worldPosition);
		final Set<ChunkPos> desired = new HashSet<>(bounds.area());
		for (int x = bounds.getNegativeX(); x <= bounds.getPositiveX(); x++) {
			for (int z = bounds.getNegativeZ(); z <= bounds.getPositiveZ(); z++) {
				desired.add(new ChunkPos(self.x + x, self.z + z));
			}
		}
		return desired;
	}

	private void releaseTickets(final ServerWorld world) {
		if (persistedTicketsNeedClear) {
			clearPersistedTickets(world);
			persistedTicketsNeedClear = false;
		}
		if (!forcedChunks.isEmpty()) {
			for (final ChunkPos chunk : forcedChunks) setTicket(world, chunk, false);
			forcedChunks.clear();
		}
		if (ticketsActive) setChanged();
		ticketsActive = false;
		ticketsDirty = true;
	}

	/** Rebuild tickets recorded by a previous tile instance before applying the current bounds. */
	private void clearPersistedTickets(final ServerWorld world) {
		final ChunkPos self = new ChunkPos(worldPosition);
		for (int x = ticketBounds.getNegativeX(); x <= ticketBounds.getPositiveX(); x++) {
			for (int z = ticketBounds.getNegativeZ(); z <= ticketBounds.getPositiveZ(); z++) {
				setTicket(world, new ChunkPos(self.x + x, self.z + z), false);
			}
		}
	}

	private void setTicket(final ServerWorld world, final ChunkPos chunk, final boolean add) {
		// false also means "already in that state", so duplicate restoration is intentionally benign.
		ForgeChunkManager.forceChunk(world, WarpDrive.MODID, worldPosition,
			chunk.x, chunk.z, add, true);
	}

	private void updateBlockState(final ServerWorld world) {
		final BlockState state = getBlockState();
		if (state.getBlock() instanceof ChunkLoaderBlock
		 && state.getValue(ChunkLoaderBlock.ACTIVE) != active) {
			world.setBlock(worldPosition, state.setValue(ChunkLoaderBlock.ACTIVE, active), 3);
		}
	}

	public int calculateEnergyRequired() {
		return bounds.energyRequired(efficiencyUpgrades);
	}

	public boolean setBounds(final int negativeX, final int positiveX,
	                         final int negativeZ, final int positiveZ) {
		final ChunkLoaderBounds requested = ChunkLoaderBounds.fromRequested(
			negativeX, positiveX, negativeZ, positiveZ);
		if (requested.fits(rangeUpgrades)) {
			return replaceBounds(requested);
		}
		if (!bounds.fits(rangeUpgrades)) {
			replaceBounds(ChunkLoaderBounds.centered(rangeUpgrades));
		}
		return false;
	}

	private boolean replaceBounds(final ChunkLoaderBounds requested) {
		if (bounds.equals(requested)) return true;
		bounds = requested;
		ticketsDirty = true;
		setChanged();
		return true;
	}

	public int[] getBounds() {
		return new int[]{ bounds.getNegativeX(), bounds.getPositiveX(),
			bounds.getNegativeZ(), bounds.getPositiveZ() };
	}

	public boolean addUpgrade(final Upgrade upgrade) {
		switch (upgrade) {
		case EFFICIENCY:
			if (efficiencyUpgrades >= ChunkLoaderBounds.MAX_EFFICIENCY_UPGRADES) return false;
			efficiencyUpgrades++;
			break;
		case RANGE:
			if (rangeUpgrades >= ChunkLoaderBounds.MAX_RANGE_UPGRADES) return false;
			rangeUpgrades++;
			replaceBounds(ChunkLoaderBounds.centered(rangeUpgrades));
			break;
		case COMPUTER_INTERFACE:
			if (computerInterfaceUpgrades >= 1) return false;
			computerInterfaceUpgrades++;
			break;
		default:
			return false;
		}
		setChanged();
		return true;
	}

	/** With no requested type, preserve a deterministic equivalent of legacy empty-hand removal. */
	@Nullable
	public Upgrade removeUpgrade(@Nullable final Upgrade requested) {
		final Upgrade upgrade = requested == null ? firstInstalledUpgrade() : requested;
		if (upgrade == null) return null;
		switch (upgrade) {
		case EFFICIENCY:
			if (efficiencyUpgrades <= 0) return null;
			efficiencyUpgrades--;
			break;
		case RANGE:
			if (rangeUpgrades <= 0) return null;
			rangeUpgrades--;
			// The old code could retain an oversized rectangle after removal. Re-centering prevents
			// a removed component from continuing to grant loading capacity.
			replaceBounds(ChunkLoaderBounds.centered(rangeUpgrades));
			break;
		case COMPUTER_INTERFACE:
			if (computerInterfaceUpgrades <= 0) return null;
			computerInterfaceUpgrades--;
			break;
		default:
			return null;
		}
		setChanged();
		return upgrade;
	}

	@Nullable
	private Upgrade firstInstalledUpgrade() {
		if (efficiencyUpgrades > 0) return Upgrade.EFFICIENCY;
		if (rangeUpgrades > 0) return Upgrade.RANGE;
		if (computerInterfaceUpgrades > 0) return Upgrade.COMPUTER_INTERFACE;
		return null;
	}

	public int getUpgradeCount(final Upgrade upgrade) {
		switch (upgrade) {
		case EFFICIENCY: return efficiencyUpgrades;
		case RANGE: return rangeUpgrades;
		case COMPUTER_INTERFACE: return computerInterfaceUpgrades;
		default: return 0;
		}
	}

	public boolean hasComputerInterface() { return computerInterfaceUpgrades > 0; }
	public boolean isEnabled() { return enabled; }
	public boolean isPowered() { return powered; }
	public boolean isActive() { return active; }

	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (!enabled) {
			active = false;
			if (level instanceof ServerWorld) releaseTickets((ServerWorld) level);
		}
		setChanged();
	}

	public String getStatus() {
		return String.format("Chunk loader %s%s, %d chunk%s, bounds [%d, %d, %d, %d], "
			+ "%d FE/t, %d / %d FE; upgrades: efficiency %d/%d, range %d/%d, interface %d/1",
			enabled ? active ? "active" : "enabled" : "disabled",
			enabled && !powered ? " (insufficient energy)" : "",
			bounds.area(), bounds.area() == 1 ? "" : "s",
			bounds.getNegativeX(), bounds.getPositiveX(),
			bounds.getNegativeZ(), bounds.getPositiveZ(), calculateEnergyRequired(),
			getEnergyStored(), getMaxEnergyStored(), efficiencyUpgrades,
			ChunkLoaderBounds.MAX_EFFICIENCY_UPGRADES, rangeUpgrades,
			ChunkLoaderBounds.MAX_RANGE_UPGRADES, computerInterfaceUpgrades);
	}

	public Object[] isInterfacedComputer() {
		return hasComputerInterface()
			? new Object[]{ true, "Computer interface installed." }
			: new Object[]{ false, "Missing Computer interface upgrade." };
	}

	public Object[] name(@Nullable final String requested) {
		if (requested != null) {
			name = requested.replaceAll("[^A-Za-z0-9._-]", "_");
			if (name.length() > 64) name = name.substring(0, 64);
			setChanged();
		}
		return new Object[]{ name };
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null) setEnabled(requested);
		return new Object[]{ enabled };
	}

	public Object[] boundsComputer(@Nullable final int[] requested) {
		if (requested != null) setBounds(requested[0], requested[1], requested[2], requested[3]);
		final int[] current = getBounds();
		return new Object[]{ current[0], current[1], current[2], current[3] };
	}

	public Object[] radiusComputer(@Nullable final Integer requested) {
		if (requested != null) setBounds(requested, requested, requested, requested);
		return boundsComputer(null);
	}

	public Object[] getEnergyRequiredComputer() {
		return new Object[]{ true, calculateEnergyRequired() };
	}

	public Object[] getEnergyStatusComputer() {
		return new Object[]{ getEnergyStored(), getMaxEnergyStored(), "FE" };
	}

	public Object[] getLocalPositionComputer() {
		return new Object[]{ worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() };
	}

	public Object[] getTierComputer() {
		final BlockState state = getBlockState();
		final ChunkLoaderTier tier = state.getBlock() instanceof ChunkLoaderBlock
			? ((ChunkLoaderBlock) state.getBlock()).getTier() : ChunkLoaderTier.BASIC;
		return new Object[]{ tier.getIndex(), tier.getName() };
	}

	public Object[] getUpgradesComputer() {
		return new Object[]{ true, String.format("efficiency %d/%d, range %d/%d, interface %d/1",
			efficiencyUpgrades, ChunkLoaderBounds.MAX_EFFICIENCY_UPGRADES,
			rangeUpgrades, ChunkLoaderBounds.MAX_RANGE_UPGRADES, computerInterfaceUpgrades) };
	}

	public Object[] stateComputer() {
		return new Object[]{ enabled, active, getStatus() };
	}

	public void onLoaderBroken() {
		active = false;
		if (level instanceof ServerWorld) releaseTickets((ServerWorld) level);
	}

	@Override
	public void load(@Nonnull final BlockState state, @Nonnull final CompoundNBT tag) {
		super.load(state, tag);
		enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		powered = tag.getBoolean(TAG_POWERED);
		name = tag.getString(TAG_NAME);
		final CompoundNBT upgrades = tag.getCompound(TAG_UPGRADES);
		efficiencyUpgrades = Math.max(0, Math.min(ChunkLoaderBounds.MAX_EFFICIENCY_UPGRADES,
			upgrades.getInt(TAG_EFFICIENCY)));
		rangeUpgrades = Math.max(0, Math.min(ChunkLoaderBounds.MAX_RANGE_UPGRADES,
			upgrades.getInt(TAG_RANGE)));
		computerInterfaceUpgrades = Math.max(0, Math.min(1,
			upgrades.getInt(TAG_COMPUTER_INTERFACE)));
		bounds = ChunkLoaderBounds.fromRequested(tag.getInt(TAG_NEGATIVE_X),
			tag.getInt(TAG_POSITIVE_X), tag.getInt(TAG_NEGATIVE_Z), tag.getInt(TAG_POSITIVE_Z));
		if (!bounds.fits(rangeUpgrades)) bounds = ChunkLoaderBounds.centered(rangeUpgrades);
		ticketsActive = tag.getBoolean(TAG_TICKETS_ACTIVE);
		ticketBounds = tag.contains(TAG_TICKET_NEGATIVE_X)
			? ChunkLoaderBounds.fromRequested(tag.getInt(TAG_TICKET_NEGATIVE_X),
				tag.getInt(TAG_TICKET_POSITIVE_X), tag.getInt(TAG_TICKET_NEGATIVE_Z),
				tag.getInt(TAG_TICKET_POSITIVE_Z))
			: bounds;
		persistedTicketsNeedClear = ticketsActive;
		forcedChunks.clear();
		ticketsDirty = true;
		active = false;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putBoolean(TAG_ENABLED, enabled);
		tag.putBoolean(TAG_POWERED, powered);
		if (!name.isEmpty()) tag.putString(TAG_NAME, name);
		tag.putInt(TAG_NEGATIVE_X, bounds.getNegativeX());
		tag.putInt(TAG_POSITIVE_X, bounds.getPositiveX());
		tag.putInt(TAG_NEGATIVE_Z, bounds.getNegativeZ());
		tag.putInt(TAG_POSITIVE_Z, bounds.getPositiveZ());
		final CompoundNBT upgrades = new CompoundNBT();
		if (efficiencyUpgrades > 0) upgrades.putInt(TAG_EFFICIENCY, efficiencyUpgrades);
		if (rangeUpgrades > 0) upgrades.putInt(TAG_RANGE, rangeUpgrades);
		if (computerInterfaceUpgrades > 0) {
			upgrades.putInt(TAG_COMPUTER_INTERFACE, computerInterfaceUpgrades);
		}
		if (!upgrades.isEmpty()) tag.put(TAG_UPGRADES, upgrades);
		tag.putBoolean(TAG_TICKETS_ACTIVE, ticketsActive || persistedTicketsNeedClear);
		if (ticketsActive || persistedTicketsNeedClear) {
			tag.putInt(TAG_TICKET_NEGATIVE_X, ticketBounds.getNegativeX());
			tag.putInt(TAG_TICKET_POSITIVE_X, ticketBounds.getPositiveX());
			tag.putInt(TAG_TICKET_NEGATIVE_Z, ticketBounds.getNegativeZ());
			tag.putInt(TAG_TICKET_POSITIVE_Z, ticketBounds.getPositiveZ());
		}
		return tag;
	}
}
