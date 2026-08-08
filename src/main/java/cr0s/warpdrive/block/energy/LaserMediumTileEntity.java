package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

/**
 * Input-only FE store feeding adjacent laser machines.
 *
 * This is the complete non-integration behaviour of 1.12.2 {@code TileEntityLaserMedium}: tiered
 * capacity, 4096-unit input rate, energy persistence, and a charge display refreshed once a
 * second. Output is deliberately unavailable through Forge Energy; lasers drain the buffer via
 * {@link #consumeEnergy(int, boolean)}, just as the old shared laser base called energy_consume.
 */
public class LaserMediumTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	private static final int MAX_RECEIVE = 4096;
	private static final int BLOCKSTATE_REFRESH_PERIOD_TICKS = 20;

	private LaserMediumTier tier;
	private int ticksUntilRefresh = BLOCKSTATE_REFRESH_PERIOD_TICKS;

	public LaserMediumTileEntity() {
		this(LaserMediumTier.BASIC);
	}

	public LaserMediumTileEntity(final LaserMediumTier tier) {
		super(Registration.LASER_MEDIUM_TILE.get());
		this.tier = tier;
	}

	public LaserMediumTier getTier() {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof LaserMediumBlock) {
			tier = ((LaserMediumBlock) blockState.getBlock()).getTier();
		}
		return tier;
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || --ticksUntilRefresh > 0) {
			return;
		}
		ticksUntilRefresh = BLOCKSTATE_REFRESH_PERIOD_TICKS;

		final BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof LaserMediumBlock)) {
			return;
		}
		final int chargeLevel = MathHelper.clamp(
			(int) Math.round(8.0D * getEnergyStored() / (double) getMaxEnergyStored()), 0, 7);
		if (blockState.getValue(LaserMediumBlock.LEVEL) != chargeLevel) {
			level.setBlock(getBlockPos(), blockState.setValue(LaserMediumBlock.LEVEL, chargeLevel), 3);
		}
	}

	@Override
	public int getMaxEnergyStored() {
		return getTier().getMaxEnergyStored();
	}

	@Override
	protected int getMaxReceive() {
		return MAX_RECEIVE;
	}

	@Override
	protected int getMaxExtract() {
		return 0;
	}

	@Override
	protected boolean canReceiveFrom(@Nullable final Direction side) {
		return true;
	}
}
