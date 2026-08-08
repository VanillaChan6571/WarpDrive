package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.CloakManager;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.network.BeamEffectPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Energy-backed cloaking core using the original six inner/six outer coil geometry.
 *
 * The field is deliberately reconstructed after load: a persisted "active" bit could otherwise
 * leave clients masked after the machine has lost coils or power while its chunk was absent.
 */
public class CloakingCoreTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	public static final int MAX_FIELD_RADIUS = 63;
	private static final int INNER_DISTANCE = 2;
	private static final int MIN_OUTER_DISTANCE = 3;
	private static final int MAX_OUTER_DISTANCE_EXCLUSIVE = MAX_FIELD_RADIUS - 5;
	private static final int CAPTURE_MARGIN = 5;
	private static final int SCAN_BUDGET = 1_000;
	private static final int RESCAN_INTERVAL = 120 * 20;
	private static final int TIER_ONE_REFRESH = 60;
	private static final int TIER_TWO_REFRESH = 30;
	private static final int TIER_ONE_ENERGY_PER_BLOCK = 32;
	private static final int TIER_TWO_ENERGY_PER_BLOCK = 128;

	private final BlockPos[] innerCoils = new BlockPos[Direction.values().length];
	private final BlockPos[] outerCoils = new BlockPos[Direction.values().length];
	private boolean enabled;
	private boolean assemblyValid;
	private boolean active;
	private boolean volumeDefined;
	private boolean scannedAsTierTwo;
	private int diamondCrystals;
	private int fieldVolume;
	private int assemblyTimer;
	private int refreshTimer;
	private int rescanTimer;
	private int beamTimer;
	private BlockPos min = BlockPos.ZERO;
	private BlockPos max = BlockPos.ZERO;
	@Nullable private CloakVolumeScanner volumeScanner;

	public CloakingCoreTileEntity() {
		super(Registration.CLOAKING_CORE_TILE.get());
	}

	@Override public int getMaxEnergyStored() { return 500_000_000; }
	@Override protected int getMaxReceive() { return 16_384; }
	@Override protected int getMaxExtract() { return 0; }

	@Override
	public void tick() {
		if (!(level instanceof ServerWorld)) return;
		final ServerWorld serverWorld = (ServerWorld) level;
		if (--assemblyTimer <= 0) {
			assemblyTimer = 20;
			inspectAssembly(serverWorld);
		}
		if (volumeScanner != null && volumeScanner.scan(serverWorld, SCAN_BUDGET)) {
			fieldVolume = volumeScanner.getNonAirBlocks();
			volumeDefined = true;
			volumeScanner = null;
			rescanTimer = RESCAN_INTERVAL;
			setChanged();
		} else if (volumeScanner == null && assemblyValid && --rescanTimer <= 0) {
			startVolumeScan();
		}

		if (--refreshTimer <= 0) {
			refreshTimer = getRefreshInterval();
			refreshCloak(serverWorld);
		}
		if (enabled && assemblyValid && --beamTimer <= 0) {
			beamTimer = 100;
			sendCoilBeams();
		}
	}

	private void inspectAssembly(final ServerWorld world) {
		final BlockPos[] foundInner = new BlockPos[Direction.values().length];
		final BlockPos[] foundOuter = new BlockPos[Direction.values().length];
		boolean valid = true;
		for (final Direction direction : Direction.values()) {
			final int index = direction.ordinal();
			final BlockPos inner = worldPosition.relative(direction, INNER_DISTANCE);
			if (world.hasChunkAt(inner)
			 && world.getBlockState(inner).getBlock() instanceof CloakingCoilBlock) {
				foundInner[index] = inner.immutable();
			} else {
				valid = false;
			}
			for (int distance = MIN_OUTER_DISTANCE;
			     distance < MAX_OUTER_DISTANCE_EXCLUSIVE; distance++) {
				final BlockPos candidate = worldPosition.relative(direction, distance);
				if (world.hasChunkAt(candidate)
				 && world.getBlockState(candidate).getBlock() instanceof CloakingCoilBlock) {
					foundOuter[index] = candidate.immutable();
					break;
				}
			}
			if (foundOuter[index] == null) valid = false;
		}

		BlockPos foundMin = worldPosition;
		BlockPos foundMax = worldPosition;
		if (valid) {
			foundMin = new BlockPos(
				foundOuter[Direction.WEST.ordinal()].getX() - CAPTURE_MARGIN,
				Math.max(0, foundOuter[Direction.DOWN.ordinal()].getY() - CAPTURE_MARGIN),
				foundOuter[Direction.NORTH.ordinal()].getZ() - CAPTURE_MARGIN);
			foundMax = new BlockPos(
				foundOuter[Direction.EAST.ordinal()].getX() + CAPTURE_MARGIN,
				Math.min(world.getMaxBuildHeight() - 1,
					foundOuter[Direction.UP.ordinal()].getY() + CAPTURE_MARGIN),
				foundOuter[Direction.SOUTH.ordinal()].getZ() + CAPTURE_MARGIN);
		}

		final boolean changed = assemblyValid != valid
			|| !Arrays.equals(innerCoils, foundInner) || !Arrays.equals(outerCoils, foundOuter)
			|| !min.equals(foundMin) || !max.equals(foundMax)
			|| valid && scannedAsTierTwo != isTierTwo();
		if (!changed) return;
		disconnectCoils(world);
		System.arraycopy(foundInner, 0, innerCoils, 0, innerCoils.length);
		System.arraycopy(foundOuter, 0, outerCoils, 0, outerCoils.length);
		assemblyValid = valid;
		min = foundMin;
		max = foundMax;
		fieldVolume = 0;
		volumeDefined = false;
		scannedAsTierTwo = isTierTwo();
		volumeScanner = valid ? new CloakVolumeScanner(min, max, scannedAsTierTwo) : null;
		rescanTimer = RESCAN_INTERVAL;
		updateCoils(world);
		setChanged();
		if (!valid && active) deactivate(world, true);
	}

	private void startVolumeScan() {
		volumeScanner = new CloakVolumeScanner(min, max, scannedAsTierTwo);
	}

	private void refreshCloak(final ServerWorld world) {
		final int energyRequired = calculateEnergyRequired();
		final boolean shouldBeActive = enabled && assemblyValid && volumeDefined
			&& fieldVolume > 13 && consumeEnergy(energyRequired, true);
		if (!shouldBeActive) {
			if (active) deactivate(world, true);
			return;
		}
		consumeEnergy(energyRequired, false);
		if (!active) {
			active = true;
			beamTimer = 1;
			world.playSound(null, worldPosition, Registration.SOUND_CLOAK.get(),
				SoundCategory.BLOCKS, 1.0F, 1.0F);
			updateVisualState(world);
		}
		CloakManager.update(world, worldPosition, min, max, isTierTwo());
	}

	private void deactivate(final ServerWorld world, final boolean reveal) {
		active = false;
		CloakManager.remove(world, worldPosition, reveal);
		world.playSound(null, worldPosition, Registration.SOUND_DECLOAK.get(),
			SoundCategory.BLOCKS, 1.0F, 1.0F);
		updateVisualState(world);
	}

	private void updateVisualState(final ServerWorld world) {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof CloakingCoreBlock
		 && blockState.getValue(CloakingCoreBlock.ACTIVE) != active) {
			world.setBlock(worldPosition, blockState.setValue(CloakingCoreBlock.ACTIVE, active), 3);
		}
		updateCoils(world);
	}

	private void updateCoils(final ServerWorld world) {
		for (final Direction direction : Direction.values()) {
			final int index = direction.ordinal();
			if (innerCoils[index] != null && world.hasChunkAt(innerCoils[index])) {
				CloakingCoilBlock.setCoilState(world, innerCoils[index], assemblyValid, active,
					false, Direction.DOWN);
			}
			if (outerCoils[index] != null && world.hasChunkAt(outerCoils[index])) {
				CloakingCoilBlock.setCoilState(world, outerCoils[index], assemblyValid, active,
					true, direction);
			}
		}
	}

	private void disconnectCoils(final ServerWorld world) {
		for (final Direction direction : Direction.values()) {
			final int index = direction.ordinal();
			if (innerCoils[index] != null && world.hasChunkAt(innerCoils[index])) {
				CloakingCoilBlock.setCoilState(world, innerCoils[index], false, false,
					false, Direction.DOWN);
			}
			if (outerCoils[index] != null && world.hasChunkAt(outerCoils[index])) {
				CloakingCoilBlock.setCoilState(world, outerCoils[index], false, false,
					true, Direction.DOWN);
			}
		}
	}

	private void sendCoilBeams() {
		if (level == null || WarpDriveNetwork.CHANNEL == null) return;
		final float phase = (level.getGameTime() % 200L) / 200.0F;
		for (final Direction direction : Direction.values()) {
			final BlockPos inner = innerCoils[direction.ordinal()];
			final BlockPos outer = outerCoils[direction.ordinal()];
			if (inner == null || outer == null) continue;
			sendBeam(inner, outer, phase, 1.0F - phase, 0.8F);
		}
		for (int first = 0; first < innerCoils.length; first++) {
			for (int second = first + 1; second < innerCoils.length; second++) {
				if (Direction.values()[first].getOpposite() == Direction.values()[second]) continue;
				if (innerCoils[first] != null && innerCoils[second] != null) {
					sendBeam(innerCoils[first], innerCoils[second],
						(first + 1.0F) / innerCoils.length, (second + 1.0F) / innerCoils.length,
						1.0F - phase);
				}
			}
		}
	}

	private void sendBeam(final BlockPos source, final BlockPos target,
	                      final float red, final float green, final float blue) {
		WarpDriveNetwork.CHANNEL.send(
			PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
				worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
				160.0D, level.dimension())),
			new BeamEffectPacket(Vector3d.atCenterOf(source), Vector3d.atCenterOf(target),
				red, green, blue, 0));
	}

	private int getRefreshInterval() { return isTierTwo() ? TIER_TWO_REFRESH : TIER_ONE_REFRESH; }
	private int getEnergyPerBlock() {
		return isTierTwo() ? TIER_TWO_ENERGY_PER_BLOCK : TIER_ONE_ENERGY_PER_BLOCK;
	}

	private int calculateEnergyRequired() {
		return (int) Math.min(Integer.MAX_VALUE, (long) fieldVolume * getEnergyPerBlock());
	}

	public boolean isTierTwo() { return diamondCrystals >= 6; }
	public boolean isEnabled() { return enabled; }
	public int getDiamondCrystalCount() { return diamondCrystals; }

	public boolean addDiamondCrystal() {
		if (diamondCrystals >= 6) return false;
		diamondCrystals++;
		refreshTimer = 1;
		setChanged();
		return true;
	}

	public boolean removeDiamondCrystal() {
		if (diamondCrystals <= 0) return false;
		diamondCrystals--;
		refreshTimer = 1;
		setChanged();
		return true;
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null && enabled != requested) {
			enabled = requested;
			refreshTimer = 1;
			setChanged();
		}
		return new Object[]{ enabled };
	}

	public Object[] state() { return new Object[]{ enabled, active, getStatus() }; }
	public Object[] getEnergyRequired() { return new Object[]{ true, calculateEnergyRequired() }; }
	public Object[] getAssemblyStatus() {
		return new Object[]{ assemblyValid, assemblyValid ? "Assembly is valid" : "Invalid coil assembly" };
	}

	public String getStatus() {
		final String assembly = assemblyValid
			? (volumeScanner == null ? fieldVolume + " blocks" : "scanning field")
			: "invalid coil assembly";
		return String.format("%s, tier %d (%d/6 crystals), %s, %d FE",
			enabled ? (active ? "active" : "enabled") : "disabled",
			isTierTwo() ? 2 : 1, diamondCrystals, assembly, getEnergyStored());
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerWorld) {
			final ServerWorld world = (ServerWorld) level;
			CloakManager.remove(world, worldPosition, true);
			disconnectCoils(world);
		}
		active = false;
		super.setRemoved();
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		enabled = tag.getBoolean("isEnabled");
		diamondCrystals = Math.max(0, Math.min(6, tag.getInt("diamondCrystals")));
		fieldVolume = Math.max(0, tag.getInt("fieldVolume"));
		active = false;
		assemblyTimer = 1;
		refreshTimer = 1;
		rescanTimer = 1;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putBoolean("isEnabled", enabled);
		tag.putInt("diamondCrystals", diamondCrystals);
		tag.putInt("fieldVolume", fieldVolume);
		return tag;
	}
}
