package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.weapon.AbstractLaserTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.network.BeamEffectPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/** One-medium stabilization laser linked to a specific face of an assembled reactor. */
public class EnanReactorLaserTileEntity extends AbstractLaserTileEntity
	implements ITickableTileEntity {

	private static final String TAG_REACTOR_FACE = "reactorFace";
	private static final String TAG_STABILIZATION_REQUEST = "energyStabilizationRequest";
	private static final String TAG_NAME = "name";
	private static final int ASSEMBLY_SCAN_INTERVAL_TICKS = 100;

	private EnanReactorFace reactorFace = EnanReactorFace.UNKNOWN;
	private int stabilizationRequest;
	private String name = "";
	private String reactorSignatureName = "";
	@Nullable private WeakReference<EnanReactorCoreTileEntity> reactorReference;
	private boolean assemblyDirty = true;
	private boolean assemblyValid;
	private String assemblyStatus = "Assembly not scanned";
	private int assemblyScanTicks;

	public EnanReactorLaserTileEntity() {
		super(Registration.ENAN_REACTOR_LASER_TILE.get());
	}

	@Override protected Direction[] getValidLaserMediumDirections() {
		return new Direction[]{ Direction.UP, Direction.DOWN };
	}

	@Override protected int getMaxLaserMediumCount() { return 1; }

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		if (assemblyDirty || --assemblyScanTicks <= 0) {
			assemblyDirty = false;
			assemblyScanTicks = ASSEMBLY_SCAN_INTERVAL_TICKS;
			scanAssembly();
		}
		if (stabilizationRequest > 0) {
			final int requested = stabilizationRequest;
			stabilizationRequest = 0;
			doStabilize(requested);
			setChanged();
		}
	}

	public void markAssemblyDirty() { assemblyDirty = true; }

	private void scanAssembly() {
		final boolean hasMedium = scanLaserMediums();
		final EnanReactorCoreTileEntity core = getReactorCore();
		assemblyValid = hasMedium && core != null;
		if (!hasMedium) assemblyStatus = "Missing laser medium above or below";
		else if (core == null) assemblyStatus = "Missing Enantiomorphic reactor core";
		else assemblyStatus = "ok";
		refreshBlockState();
	}

	@Nonnull
	public EnanReactorFace getReactorFace() {
		return reactorFace == null ? EnanReactorFace.UNKNOWN : reactorFace;
	}

	public void setReactorFace(@Nonnull final EnanReactorFace requestedFace,
	                           @Nonnull final EnanReactorCoreTileEntity requestedCore) {
		final EnanReactorCoreTileEntity currentCore = getReactorCore();
		if (reactorFace != requestedFace && reactorFace != EnanReactorFace.UNKNOWN
		 && currentCore != null && currentCore != requestedCore) {
			WarpDrive.logger.warn("Ignoring reactor {} on face {} for laser {} already linked to {}",
				requestedCore.getBlockPos(), requestedFace.getName(), worldPosition,
				currentCore.getBlockPos());
			return;
		}

		reactorSignatureName = requestedCore.getSignatureName();
		if (currentCore == requestedCore && reactorFace == requestedFace) return;
		reactorFace = requestedFace;
		reactorReference = new WeakReference<>(requestedCore);
		assemblyDirty = true;
		setChanged();
		refreshBlockState();
	}

	public void clearReactor(final EnanReactorCoreTileEntity core) {
		final EnanReactorCoreTileEntity current = getReactorCore();
		if (current != null && current != core) return;
		reactorFace = EnanReactorFace.UNKNOWN;
		reactorReference = null;
		reactorSignatureName = "";
		assemblyDirty = true;
		setChanged();
		refreshBlockState();
	}

	@Nullable
	private EnanReactorCoreTileEntity getReactorCore() {
		if (reactorFace == null || reactorFace == EnanReactorFace.UNKNOWN || level == null) {
			return null;
		}
		EnanReactorCoreTileEntity core =
			reactorReference == null ? null : reactorReference.get();
		if (core != null && !core.isRemoved()) return core;

		final BlockPos corePosition = worldPosition.subtract(reactorFace.getOffset());
		if (!level.hasChunkAt(corePosition)) return null;
		final TileEntity tileEntity = level.getBlockEntity(corePosition);
		if (tileEntity instanceof EnanReactorCoreTileEntity
		 && ((EnanReactorCoreTileEntity) tileEntity).getTier() == reactorFace.getTier()) {
			core = (EnanReactorCoreTileEntity) tileEntity;
			reactorReference = new WeakReference<>(core);
			return core;
		}
		reactorFace = EnanReactorFace.UNKNOWN;
		reactorReference = null;
		reactorSignatureName = "";
		return null;
	}

	private void refreshBlockState() {
		if (level == null) return;
		final BlockState state = getBlockState();
		if (!(state.getBlock() instanceof EnanReactorLaserBlock)) return;
		final Direction facing = reactorFace == null || reactorFace.getLaserFacing() == null
			? Direction.DOWN : reactorFace.getLaserFacing();
		final boolean active = reactorFace != null && reactorFace != EnanReactorFace.UNKNOWN;
		if (state.getValue(EnanReactorLaserBlock.ACTIVE) != active
		 || state.getValue(EnanReactorLaserBlock.FACING) != facing) {
			level.setBlock(worldPosition, state.setValue(EnanReactorLaserBlock.ACTIVE, active)
				.setValue(EnanReactorLaserBlock.FACING, facing), 3);
		}
	}

	public int stabilize(final int energy) {
		if (energy <= 0 || !scanLaserMediums() || getReactorCore() == null) return 0;
		if (stabilizationRequest > 0) return -energy;
		stabilizationRequest = energy;
		setChanged();
		return energy;
	}

	private void doStabilize(final int energy) {
		if (energy <= 0 || !scanLaserMediums()) return;
		final EnanReactorCoreTileEntity core = getReactorCore();
		if (core == null || consumeLaserMediumEnergy(energy, true) < energy) return;
		if (consumeLaserMediumEnergy(energy, false) < energy) return;
		core.decreaseInstability(reactorFace, energy);
		sendBeam(core.getCenter(), energy);
	}

	private void sendBeam(final Vector3d target, final int energy) {
		if (level == null) return;
		final Vector3d source = Vector3d.atCenterOf(worldPosition);
		WarpDriveNetwork.CHANNEL.send(
			PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
				source.x, source.y, source.z, 100.0D, level.dimension())),
			new BeamEffectPacket(source, target, 0.1F, 0.2F, 1.0F, energy, 25));
	}

	public Object[] name(@Nullable final String value) {
		if (value != null) {
			name = value;
			setChanged();
		}
		return new Object[]{ name };
	}

	public Object[] getLocalPosition() {
		return new Object[]{ worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() };
	}

	public Object[] getAssemblyStatus() {
		return new Object[]{ assemblyValid, assemblyStatus };
	}

	public Object[] getEnergyRequired() {
		return new Object[]{ true, stabilizationRequest };
	}

	public Object[] getEnergyStatus() {
		return new Object[]{ getLaserMediumEnergyStored(), getLaserMediumMaxStorage(), "FE" };
	}

	public Object[] laserMediumDirection() {
		final Direction direction = getLaserMediumDirection();
		return direction == null
			? new Object[]{ "NONE", 0, 0, 0 }
			: new Object[]{ direction.getName(), direction.getStepX(), direction.getStepY(),
				direction.getStepZ() };
	}

	public Object[] laserMediumCount() { return new Object[]{ getLaserMediumCount() }; }

	public Object[] stabilizeComputer(@Nullable final Integer energy) {
		return new Object[]{ energy == null ? stabilizationRequest : stabilize(energy) };
	}

	public Object[] side() {
		if (reactorFace == null || reactorFace.getTier() == null) {
			return new Object[]{ null, null, null };
		}
		return new Object[]{ reactorFace.getInstabilityIndex(),
			reactorFace.getTier().getName(), reactorSignatureName };
	}

	public String getStatus() {
		return String.format("Reactor stabilization laser%s: assembly %s, %,d / %,d FE, face %s",
			name.isEmpty() ? "" : " '" + name + "'", assemblyStatus,
			getLaserMediumEnergyStored(), getLaserMediumMaxStorage(),
			reactorFace == null ? "unknown" : reactorFace.getName());
	}

	public void onLaserBroken() {
		final EnanReactorCoreTileEntity core = getReactorCore();
		if (core != null) core.markAssemblyDirty();
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		reactorFace = EnanReactorFace.byName(tag.getString(TAG_REACTOR_FACE));
		stabilizationRequest = tag.getInt(TAG_STABILIZATION_REQUEST);
		name = tag.getString(TAG_NAME);
		reactorReference = null;
		assemblyDirty = true;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		if (reactorFace != null && reactorFace != EnanReactorFace.UNKNOWN) {
			tag.putString(TAG_REACTOR_FACE, reactorFace.getName());
		}
		tag.putInt(TAG_STABILIZATION_REQUEST, stabilizationRequest);
		tag.putString(TAG_NAME, name);
		return tag;
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(worldPosition, 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager networkManager,
	                         final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}
}
