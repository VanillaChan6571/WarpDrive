package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.block.energy.LaserMediumTier;
import cr0s.warpdrive.block.energy.LaserMediumTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared laser-medium assembly logic, ported from 1.12.2 {@code TileEntityAbstractLaser}.
 *
 * A laser machine accepts one straight line of adjacent, same-tier media. Energy is drained across
 * that line as evenly as possible so the visible charge levels fall together instead of emptying
 * the nearest block first. Subclasses choose the valid directions and maximum line length.
 */
public abstract class AbstractLaserTileEntity extends TileEntity {

	@Nullable
	private Direction laserMediumDirection;
	@Nullable
	private LaserMediumTier laserMediumTier;
	private int laserMediumCount;
	private double laserMediumFactor;
	private int laserMediumEnergyStored;
	private int laserMediumMaxStorage;

	protected AbstractLaserTileEntity(final TileEntityType<?> tileEntityType) {
		super(tileEntityType);
	}

	protected abstract Direction[] getValidLaserMediumDirections();

	protected abstract int getMaxLaserMediumCount();

	/** Refresh the assembly cache without loading chunks. */
	protected boolean scanLaserMediums() {
		clearLaserMediumCache();
		if (level == null) {
			return false;
		}

		for (final Direction direction : getValidLaserMediumDirections()) {
			final BlockPos firstPos = getBlockPos().relative(direction);
			if (!level.hasChunkAt(firstPos)) {
				continue;
			}
			final TileEntity first = level.getBlockEntity(firstPos);
			if (!(first instanceof LaserMediumTileEntity)) {
				continue;
			}

			final LaserMediumTier tier = ((LaserMediumTileEntity) first).getTier();
			int energyStored = 0;
			int maxStorage = 0;
			int count = 0;
			for (int distance = 1; distance <= getMaxLaserMediumCount(); distance++) {
				final BlockPos mediumPos = getBlockPos().relative(direction, distance);
				if (!level.hasChunkAt(mediumPos)) {
					break;
				}
				final TileEntity tileEntity = level.getBlockEntity(mediumPos);
				if (!(tileEntity instanceof LaserMediumTileEntity)) {
					break;
				}
				final LaserMediumTileEntity medium = (LaserMediumTileEntity) tileEntity;
				if (medium.getTier() != tier) {
					break;
				}
				count++;
				energyStored += medium.getEnergyStored();
				maxStorage += medium.getMaxEnergyStored();
			}

			laserMediumDirection = direction;
			laserMediumTier = tier;
			laserMediumCount = count;
			laserMediumFactor = Math.max(1.0D, count * tier.getBeamFactor());
			laserMediumEnergyStored = energyStored;
			laserMediumMaxStorage = maxStorage;
			return count > 0;
		}
		return false;
	}

	private void clearLaserMediumCache() {
		laserMediumDirection = null;
		laserMediumTier = null;
		laserMediumCount = 0;
		laserMediumFactor = 0.0D;
		laserMediumEnergyStored = 0;
		laserMediumMaxStorage = 0;
	}

	private List<LaserMediumTileEntity> getLaserMediums() {
		if (level == null || laserMediumDirection == null || laserMediumTier == null) {
			return Collections.emptyList();
		}
		final List<LaserMediumTileEntity> mediums = new ArrayList<>();
		for (int distance = 1; distance <= getMaxLaserMediumCount(); distance++) {
			final BlockPos mediumPos = getBlockPos().relative(laserMediumDirection, distance);
			if (!level.hasChunkAt(mediumPos)) {
				break;
			}
			final TileEntity tileEntity = level.getBlockEntity(mediumPos);
			if (!(tileEntity instanceof LaserMediumTileEntity)) {
				break;
			}
			final LaserMediumTileEntity medium = (LaserMediumTileEntity) tileEntity;
			if (medium.getTier() != laserMediumTier) {
				break;
			}
			mediums.add(medium);
		}
		return mediums;
	}

	/**
	 * Drain up to {@code amount} FE from the current line, balancing the withdrawal across media.
	 */
	protected int consumeLaserMediumEnergy(final int amount, final boolean simulate) {
		if (amount <= 0 || !scanLaserMediums()) {
			return 0;
		}
		final List<LaserMediumTileEntity> mediums = getLaserMediums();
		if (mediums.isEmpty()) {
			clearLaserMediumCache();
			return 0;
		}

		int totalEnergy = 0;
		for (final LaserMediumTileEntity medium : mediums) {
			totalEnergy += medium.getEnergyStored();
		}
		final int requested = Math.min(amount, totalEnergy);
		if (simulate || requested <= 0) {
			return requested;
		}

		int average = requested / mediums.size();
		int leftOver = requested - average * mediums.size();
		for (final LaserMediumTileEntity medium : mediums) {
			if (medium.getEnergyStored() < average) {
				leftOver += average - medium.getEnergyStored();
			}
		}

		int consumed = 0;
		for (final LaserMediumTileEntity medium : mediums) {
			final int energyToConsume = Math.min(medium.getEnergyStored(), average + leftOver);
			leftOver -= Math.max(0, energyToConsume - average);
			if (medium.consumeEnergy(energyToConsume, false)) {
				consumed += energyToConsume;
			}
		}
		scanLaserMediums();
		return consumed;
	}

	@Nullable
	public Direction getLaserMediumDirection() {
		scanLaserMediums();
		return laserMediumDirection;
	}

	public int getLaserMediumCount() {
		scanLaserMediums();
		return laserMediumCount;
	}

	public double getLaserMediumFactor() {
		scanLaserMediums();
		return laserMediumFactor;
	}

	public int getLaserMediumEnergyStored() {
		scanLaserMediums();
		return laserMediumEnergyStored;
	}

	public int getLaserMediumMaxStorage() {
		scanLaserMediums();
		return laserMediumMaxStorage;
	}
}
