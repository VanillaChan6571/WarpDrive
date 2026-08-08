package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.GlobalRegionRegistry;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/** Placed beacon: consumes 10 FE/t and asks its signed transporter to lock onto it. */
public class TransporterBeaconTileEntity extends AbstractEnergyTileEntity
	implements ITickableTileEntity {

	private static final String TAG_ENABLED = "enabled";
	private static final String TAG_DEPLOYING = "tickDeploying";
	private static final String TAG_TICKET_UUID = "ticketUuid";
	private UUID transporterUuid;
	private String transporterName = "";
	private boolean enabled = true;
	private boolean active;
	private int deployingTicks;
	private String transporterStatus = "";
	private UUID ticketUuid = UUID.randomUUID();
	@Nullable private RegistryKey<World> forcedDimension;
	@Nullable private ChunkPos forcedChunk;

	public TransporterBeaconTileEntity() {
		super(Registration.TRANSPORTER_BEACON_TILE.get());
	}

	@Override public int getMaxEnergyStored() { return TransporterBeaconBlockItem.MAX_ENERGY; }
	@Override protected int getMaxReceive() { return 1024; }
	@Override protected int getMaxExtract() { return 0; }
	@Override protected boolean canReceiveFrom(@Nullable final Direction side) {
		return side == null || side == Direction.DOWN;
	}

	@Override
	public void tick() {
		if (!(level instanceof ServerWorld)) return;
		final ServerWorld world = (ServerWorld) level;
		final boolean deployed = deployingTicks > 20;
		if (!deployed) deployingTicks++;

		boolean nextActive = false;
		if (enabled && getEnergyStored() >= TransporterBeaconBlockItem.ENERGY_PER_TICK) {
			if (transporterUuid == null) {
				// The legacy unlinked beacon deliberately burns power while reporting disconnected.
				consumeEnergy(TransporterBeaconBlockItem.ENERGY_PER_TICK, false);
			} else if (pingTransporter(world)) {
				consumeEnergy(TransporterBeaconBlockItem.ENERGY_PER_TICK, false);
				nextActive = true;
			}
		}
		if (!enabled) releaseTicket();
		active = nextActive;
		updateBlockState(world, deployed);
		setChanged();
	}

	private boolean pingTransporter(final ServerWorld context) {
		final GlobalRegionRegistry.TransporterLocation location =
			GlobalRegionRegistry.findTransporter(context, transporterUuid);
		if (location == null || context.getServer() == null) {
			transporterStatus = "Unknown transporter signature";
			releaseTicket();
			return false;
		}
		final RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY,
			location.dimension);
		final ServerWorld targetWorld = context.getServer().getLevel(dimension);
		if (targetWorld == null) {
			transporterStatus = "Transporter dimension is unavailable";
			releaseTicket();
			return false;
		}
		final ChunkPos chunk = new ChunkPos(location.position);
		ensureTicket(targetWorld, dimension, chunk);
		final TileEntity tileEntity = targetWorld.getBlockEntity(location.position);
		if (!(tileEntity instanceof TransporterCoreTileEntity)) {
			transporterStatus = "Transporter core is unavailable";
			return false;
		}
		final TransporterCoreTileEntity core = (TransporterCoreTileEntity) tileEntity;
		final boolean accepted = core.updateBeacon(this, transporterUuid);
		transporterStatus = core.getStateDescription();
		return accepted;
	}

	private void ensureTicket(final ServerWorld world, final RegistryKey<World> dimension,
	                          final ChunkPos chunk) {
		if (dimension.equals(forcedDimension) && chunk.equals(forcedChunk)) return;
		releaseTicket();
		if (ForgeChunkManager.forceChunk(world, WarpDrive.MODID, ticketUuid,
			chunk.x, chunk.z, true, true)) {
			forcedDimension = dimension;
			forcedChunk = chunk;
		}
	}

	private void releaseTicket() {
		if (forcedDimension == null || forcedChunk == null || level == null
		 || level.getServer() == null) return;
		final ServerWorld world = level.getServer().getLevel(forcedDimension);
		if (world != null) ForgeChunkManager.forceChunk(world, WarpDrive.MODID, ticketUuid,
			forcedChunk.x, forcedChunk.z, false, true);
		forcedDimension = null;
		forcedChunk = null;
	}

	private void updateBlockState(final ServerWorld world, final boolean deployed) {
		final BlockState state = getBlockState();
		if (!(state.getBlock() instanceof TransporterBeaconBlock)) return;
		if (state.getValue(TransporterBeaconBlock.ACTIVE) != active
		 || state.getValue(TransporterBeaconBlock.DEPLOYED) != deployed) {
			world.setBlock(worldPosition, state.setValue(TransporterBeaconBlock.ACTIVE, active)
				.setValue(TransporterBeaconBlock.DEPLOYED, deployed), 3);
		}
	}

	public void initializeFromItem(final ItemStack itemStack) {
		setEnergy(TransporterBeaconBlockItem.getEnergy(itemStack));
		transporterUuid = TransporterBeaconBlockItem.getSignature(itemStack);
		transporterName = TransporterBeaconBlockItem.getSignatureName(itemStack);
		setChanged();
	}

	public boolean isEnabled() { return enabled; }
	public boolean isActive() { return active; }

	public void setEnabled(final boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;
		if (enabled) deployingTicks = 0;
		else {
			active = false;
			releaseTicket();
		}
		transporterStatus = "";
		setChanged();
	}

	public void energizeDone() {
		setEnabled(false);
	}

	public String getStatus() {
		final String link = transporterUuid == null ? "unlinked"
			: "linked to " + transporterName + " (" + transporterUuid + ")";
		return String.format("Transporter beacon %s, %s, %d / %d FE%s",
			enabled ? active ? "active" : "enabled" : "disabled", link,
			getEnergyStored(), getMaxEnergyStored(),
			transporterStatus.isEmpty() ? "" : ": " + transporterStatus);
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null) setEnabled(requested);
		return new Object[]{ enabled };
	}

	public Object[] isActiveComputer() { return new Object[]{ active }; }
	public Object[] getEnergyRequired() {
		return new Object[]{ true, TransporterBeaconBlockItem.ENERGY_PER_TICK };
	}

	public void onBeaconBroken() {
		active = false;
		releaseTicket();
	}

	@Override
	public void onChunkUnloaded() {
		releaseTicket();
		super.onChunkUnloaded();
	}

	@Override
	public void setRemoved() {
		releaseTicket();
		super.setRemoved();
	}

	@Override
	public void load(@Nonnull final BlockState state, @Nonnull final CompoundNBT tag) {
		super.load(state, tag);
		enabled = !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED);
		deployingTicks = tag.getInt(TAG_DEPLOYING);
		transporterUuid = tag.hasUUID(TransporterBeaconBlockItem.TAG_SIGNATURE)
			? tag.getUUID(TransporterBeaconBlockItem.TAG_SIGNATURE) : null;
		transporterName = tag.getString(TransporterBeaconBlockItem.TAG_NAME);
		ticketUuid = tag.hasUUID(TAG_TICKET_UUID) ? tag.getUUID(TAG_TICKET_UUID) : UUID.randomUUID();
		active = false;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putBoolean(TAG_ENABLED, enabled);
		tag.putInt(TAG_DEPLOYING, deployingTicks);
		if (transporterUuid != null) tag.putUUID(TransporterBeaconBlockItem.TAG_SIGNATURE,
			transporterUuid);
		tag.putString(TransporterBeaconBlockItem.TAG_NAME, transporterName);
		tag.putUUID(TAG_TICKET_UUID, ticketUuid);
		return tag;
	}
}
