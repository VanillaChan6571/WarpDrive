package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Five-second exclusive player scan with persisted last successful identity. */
public class BiometricScannerTileEntity extends TileEntity implements ITickableTileEntity {

	public static final int RANGE_BLOCKS = 3;
	public static final int DURATION_TICKS = 100;
	private static final String TAG_ENABLED = "isEnabled";
	private static final String TAG_PLAYER = "uuidLastPlayer";
	private static final String TAG_PLAYER_NAME = "nameLastPlayer";

	private final Set<ScanListener> listeners = new HashSet<>();
	private boolean enabled = true;
	@Nullable private UUID lastPlayer;
	private String lastPlayerName = "";
	private int scanningTicks = -1;
	private int stateUpdateTicks;

	public BiometricScannerTileEntity() {
		super(Registration.BIOMETRIC_SCANNER_TILE.get());
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		if (--stateUpdateTicks <= 0) {
			stateUpdateTicks = 20;
			final BlockState blockState = getBlockState();
			if (blockState.hasProperty(BiometricScannerBlock.ACTIVE)
			 && blockState.getValue(BiometricScannerBlock.ACTIVE) != enabled) {
				level.setBlock(getBlockPos(), blockState.setValue(
					BiometricScannerBlock.ACTIVE, enabled), 3);
			}
		}
		if (!enabled || scanningTicks < 0) return;

		scanningTicks--;
		final AxisAlignedBB scanArea = getScanArea();
		final List<ServerPlayerEntity> players = level.getEntitiesOfClass(
			ServerPlayerEntity.class, scanArea,
			player -> player.isAlive() && !player.isSpectator());
		boolean present = false;
		boolean jammed = false;
		for (final ServerPlayerEntity player : players) {
			if (player.getUUID().equals(lastPlayer)) {
				present = true;
			} else {
				jammed = true;
				spawnParticles(player.position(), 1.0F, 0.0F, 0.0F, 24, 0.6D);
			}
		}
		if (!present || jammed) {
			spawnParticles(Vector3d.atCenterOf(getBlockPos()), 1.0F, 0.0F, 0.0F, 24, 0.5D);
			scanningTicks = -1;
			lastPlayer = null;
			lastPlayerName = "";
			setChanged();
			queueEvent("biometricScanAborted");
			return;
		}
		if (scanningTicks < 0) {
			spawnParticles(scanArea.getCenter(), 0.3F, 1.0F, 0.3F, 48, RANGE_BLOCKS / 2.0D);
			queueEvent("biometricScanDone", lastPlayer.toString(), lastPlayerName);
			level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_PLING,
				SoundCategory.BLOCKS, 1.0F, 1.0F);
			setChanged();
		} else if (scanningTicks == DURATION_TICKS - 1) {
			spawnParticles(scanArea.getCenter(), 0.3F, 0.0F, 1.0F, 64, RANGE_BLOCKS / 2.0D);
		}
	}

	private AxisAlignedBB getScanArea() {
		final BlockState blockState = getBlockState();
		final Direction facing = blockState.hasProperty(BiometricScannerBlock.FACING)
			? blockState.getValue(BiometricScannerBlock.FACING) : Direction.NORTH;
		final double radius = RANGE_BLOCKS / 2.0D;
		final Vector3d center = Vector3d.atCenterOf(getBlockPos()).add(
			facing.getStepX() * (radius + 0.5D),
			facing.getStepY() * (radius + 0.5D),
			facing.getStepZ() * (radius + 0.5D));
		return new AxisAlignedBB(center.x - radius, center.y - radius, center.z - radius,
			center.x + radius, center.y + radius, center.z + radius);
	}

	private void spawnParticles(final Vector3d center, final float red, final float green,
	                            final float blue, final int count, final double spread) {
		if (!(level instanceof ServerWorld)) return;
		((ServerWorld) level).sendParticles(new RedstoneParticleData(red, green, blue, 1.0F),
			center.x, center.y, center.z, count, spread, spread, spread, 0.0D);
	}

	public Object[] startScanning(final PlayerEntity player) {
		if (!enabled) return new Object[]{ false, "Sensor is disabled." };
		if (scanningTicks >= 0) return new Object[]{ false,
			String.format("Scan already in progress, %d s to go...",
				(int) Math.ceil(scanningTicks / 20.0D)) };
		lastPlayer = player.getUUID();
		lastPlayerName = player.getGameProfile().getName();
		scanningTicks = DURATION_TICKS;
		return new Object[]{ true, "Scanning started, please stay still..." };
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null && enabled != requested) {
			enabled = requested;
			stateUpdateTicks = 0;
			setChanged();
		}
		return new Object[]{ enabled };
	}

	public Object[] getScanResults() {
		if (scanningTicks >= 0) return new Object[]{ false, "Scan is in progress..." };
		if (lastPlayer == null) return new Object[]{ false, "No results available." };
		return new Object[]{ true, lastPlayer.toString(), lastPlayerName };
	}

	public void addListener(final ScanListener listener) { listeners.add(listener); }
	public void removeListener(final ScanListener listener) { listeners.remove(listener); }

	private void queueEvent(final String eventName, final Object... arguments) {
		for (final ScanListener listener : new HashSet<>(listeners)) {
			listener.queueEvent(eventName, arguments);
		}
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		enabled = !tagCompound.contains(TAG_ENABLED) || tagCompound.getBoolean(TAG_ENABLED);
		lastPlayer = tagCompound.hasUUID(TAG_PLAYER) ? tagCompound.getUUID(TAG_PLAYER) : null;
		lastPlayerName = lastPlayer == null ? "" : tagCompound.getString(TAG_PLAYER_NAME);
		scanningTicks = -1;
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putBoolean(TAG_ENABLED, enabled);
		if (scanningTicks < 0 && lastPlayer != null) {
			tagCompound.putUUID(TAG_PLAYER, lastPlayer);
			tagCompound.putString(TAG_PLAYER_NAME, lastPlayerName);
		} else {
			tagCompound.remove(TAG_PLAYER);
			tagCompound.remove(TAG_PLAYER_NAME);
		}
		return tagCompound;
	}

	public interface ScanListener {
		void queueEvent(String eventName, Object... arguments);
	}
}
