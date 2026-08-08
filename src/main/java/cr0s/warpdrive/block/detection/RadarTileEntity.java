package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.CelestialCoordinates;
import cr0s.warpdrive.data.GlobalRegionRegistry;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Energy-backed delayed radar scan with the legacy ComputerCraft result contract. */
public class RadarTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	private static final int MAX_RADIUS = 10_000;
	private int radius;
	private boolean scanning;
	private int scanningRadius;
	private int scanningCountdown;
	private int computerConnections;
	@Nullable private List<GlobalRegionRegistry.RadarEcho> results;

	public RadarTileEntity() {
		super(Registration.RADAR_TILE.get());
	}

	@Override public int getMaxEnergyStored() { return 100_000_000; }
	@Override protected int getMaxReceive() { return 65_536; }
	@Override protected int getMaxExtract() { return 0; }

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		if (scanning && --scanningCountdown <= 0) {
			results = GlobalRegionRegistry.getRadarEchoes(
				(ServerWorld) level, worldPosition, scanningRadius);
			scanning = false;
			setChanged();
		}
		final RadarMode mode = scanning ? RadarMode.SCANNING
			: computerConnections > 0 ? RadarMode.ACTIVE : RadarMode.INACTIVE;
		final BlockState blockState = getBlockState();
		if (blockState.getValue(RadarBlock.MODE) != mode) {
			level.setBlock(worldPosition, blockState.setValue(RadarBlock.MODE, mode), 3);
		}
	}

	public void setComputerConnected(final boolean connected) {
		computerConnections = Math.max(0, computerConnections + (connected ? 1 : -1));
	}

	public Object[] getGlobalPosition() {
		final String dimension = level == null ? "unknown" : level.dimension().location().toString();
		if (level == null) {
			return new Object[]{ false, "???", worldPosition.getX(), worldPosition.getY(),
				worldPosition.getZ(), dimension };
		}
		final CelestialCoordinates.UniversalPosition position = CelestialCoordinates.toUniversal(
			level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		return position == null
			? new Object[]{ false, "???", worldPosition.getX(), worldPosition.getY(),
				worldPosition.getZ(), dimension }
			: new Object[]{ true, position.galaxyName, position.x, position.y,
				position.z, position.celestialName };
	}

	public Object[] radius(@Nullable final Integer requested) {
		if (requested != null && !scanning) {
			radius = Math.max(0, Math.min(MAX_RADIUS, requested));
			setChanged();
		}
		return new Object[]{ radius };
	}

	public int calculateEnergyRequired(final int scanRadius) {
		return (int) Math.round(Math.max(10_000.0D, 0.0001D * scanRadius * scanRadius * scanRadius));
	}

	public int calculateScanDuration(final int scanRadius) {
		return (int) Math.round(20.0D * Math.max(1.0D, 1.0D + 0.001D * scanRadius));
	}

	public Object[] getEnergyRequired() { return new Object[]{ true, calculateEnergyRequired(radius) }; }
	public Object[] getScanDuration() { return new Object[]{ calculateScanDuration(radius) / 20.0D }; }

	public Object[] start() {
		if (scanning) {
			return new Object[]{ false,
				String.format("Already scanning, %.3f seconds to go", scanningCountdown / 20.0F) };
		}
		results = null;
		if (radius <= 0 || radius > MAX_RADIUS) {
			radius = 0;
			return new Object[]{ false, "Invalid radius" };
		}
		if (!consumeEnergy(calculateEnergyRequired(radius), false)) {
			return new Object[]{ false, "Insufficient energy" };
		}
		scanningRadius = radius;
		scanningCountdown = calculateScanDuration(radius);
		scanning = true;
		setChanged();
		return new Object[]{ true,
			String.format("Scanning started, %.3f seconds to go", scanningCountdown / 20.0F) };
	}

	@Nullable
	public Object[] getResults() {
		if (results == null) return null;
		final Object[] converted = new Object[results.size()];
		for (int index = 0; index < results.size(); index++) {
			final GlobalRegionRegistry.RadarEcho echo = results.get(index);
			converted[index] = new Object[]{ echo.type, echo.name == null ? "" : echo.name,
				echo.x, echo.y, echo.z, echo.mass };
		}
		return converted;
	}

	public Object[] getResultsCount() { return new Object[]{ results == null ? -1 : results.size() }; }

	public Object[] getResult(final int index) {
		if (results != null && index >= 0 && index < results.size()) {
			final GlobalRegionRegistry.RadarEcho echo = results.get(index);
			return new Object[]{ true, echo.type, echo.name == null ? "" : echo.name,
				echo.x, echo.y, echo.z, echo.mass };
		}
		return new Object[]{ false, "!ERROR!", null, 0, 0, 0 };
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		radius = tag.getInt("radius");
		scanning = tag.getBoolean("isScanning");
		scanningRadius = tag.getInt("scanning_radius");
		scanningCountdown = tag.getInt("scanning_countdown");
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putInt("radius", radius);
		tag.putBoolean("isScanning", scanning);
		tag.putInt("scanning_radius", scanningRadius);
		tag.putInt("scanning_countdown", scanningCountdown);
		return tag;
	}
}
