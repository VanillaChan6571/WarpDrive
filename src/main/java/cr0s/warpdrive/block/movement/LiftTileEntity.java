package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.network.BeamEffectPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Laser lift, ported from 1.12.2 {@code TileEntityLift}.
 *
 * With no redstone it pulls living entities from the shaft to its top. A powered lift sends them
 * down to the first floor below. Computer mode can force either direction or return control to
 * redstone. The original optional security upgrade (ship-crew-only transport) remains unavailable
 * until machine upgrade slots and the global ship-region registry are ported.
 */
public class LiftTileEntity extends AbstractEnergyTileEntity implements ITickableTileEntity {

	private static final String TAG_MODE = "mode";
	private static final String TAG_COMPUTER_MODE = "computerMode";
	private static final String TAG_ENABLED = "enabled";

	private static final double GRAB_RADIUS = 0.4D;
	private static final int MAX_ENERGY_STORED = 900;
	private static final int MAX_RECEIVE = 1024;
	private static final int ENERGY_PER_ENTITY = 150;
	private static final int UPDATE_INTERVAL_TICKS = 10;
	private static final int ENTITY_COOLDOWN_TICKS = 40;

	private LiftMode mode = LiftMode.INACTIVE;
	private LiftMode computerMode = LiftMode.REDSTONE;
	private int updateTicks;
	private boolean active;
	private boolean valid;
	private boolean enabled = true;
	private int firstUncoveredY;

	public LiftTileEntity() {
		super(Registration.LIFT_TILE.get());
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || --updateTicks >= 0) {
			return;
		}
		updateTicks = UPDATE_INTERVAL_TICKS;

		mode = computerMode == LiftMode.DOWN
		    || (computerMode == LiftMode.REDSTONE && level.hasNeighborSignal(getBlockPos()))
		     ? LiftMode.DOWN : LiftMode.UP;
		valid = isPassable(getBlockPos().getY() + 1)
		     && isPassable(getBlockPos().getY() + 2)
		     && isPassable(getBlockPos().getY() - 1)
		     && isPassable(getBlockPos().getY() - 2);
		active = enabled && valid;

		if (getEnergyStored() < ENERGY_PER_ENTITY || !active) {
			mode = LiftMode.INACTIVE;
			setBlockMode(mode);
			return;
		}
		setBlockMode(mode);

		firstUncoveredY = findFloorTop();
		if (getBlockPos().getY() - firstUncoveredY < 2) {
			return;
		}

		final Vector3d liftCenter = new Vector3d(
			getBlockPos().getX() + 0.5D, getBlockPos().getY(), getBlockPos().getZ() + 0.5D);
		final Vector3d floorCenter = new Vector3d(
			getBlockPos().getX() + 0.5D, firstUncoveredY, getBlockPos().getZ() + 0.5D);
		sendBeam(mode == LiftMode.UP ? floorCenter : liftCenter,
			mode == LiftMode.UP ? liftCenter : floorCenter,
			mode == LiftMode.UP ? 0.0F : 0.0F,
			mode == LiftMode.UP ? 1.0F : 0.0F,
			mode == LiftMode.DOWN ? 1.0F : 0.0F);

		if (moveEntities()) {
			updateTicks = ENTITY_COOLDOWN_TICKS;
		}
	}

	private boolean isPassable(final int y) {
		final BlockPos blockPos = new BlockPos(getBlockPos().getX(), y, getBlockPos().getZ());
		if (!level.hasChunkAt(blockPos)) {
			return false;
		}
		final BlockState blockState = level.getBlockState(blockPos);
		return blockState.isAir() || blockState.getCollisionShape(level, blockPos).isEmpty();
	}

	private int findFloorTop() {
		for (int y = getBlockPos().getY() - 2; y > 0; y--) {
			if (!isPassable(y)) {
				return y + 1;
			}
		}
		return 1;
	}

	private boolean moveEntities() {
		final double xMin = getBlockPos().getX() + 0.5D - GRAB_RADIUS;
		final double xMax = getBlockPos().getX() + 0.5D + GRAB_RADIUS;
		final double zMin = getBlockPos().getZ() + 0.5D - GRAB_RADIUS;
		final double zMax = getBlockPos().getZ() + 0.5D + GRAB_RADIUS;
		final AxisAlignedBB grabBox = mode == LiftMode.UP
			? new AxisAlignedBB(xMin, firstUncoveredY, zMin,
			                     xMax, getBlockPos().getY(), zMax)
			: new AxisAlignedBB(xMin, Math.min(firstUncoveredY + 4.0D, getBlockPos().getY()), zMin,
			                     xMax, getBlockPos().getY() + 2.0D, zMax);
		final List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, grabBox,
			LivingEntity::isAlive);
		if (entities.isEmpty()) {
			return false;
		}

		boolean moved = false;
		for (final LivingEntity entity : entities) {
			if (!consumeEnergy(ENERGY_PER_ENTITY, true)) {
				break;
			}
			final double targetY = mode == LiftMode.UP ? getBlockPos().getY() + 1.0D : firstUncoveredY;
			entity.teleportTo(getBlockPos().getX() + 0.5D, targetY, getBlockPos().getZ() + 0.5D);
			consumeEnergy(ENERGY_PER_ENTITY, false);
			moved = true;

			final Vector3d liftCenter = new Vector3d(
				getBlockPos().getX() + 0.5D, getBlockPos().getY(), getBlockPos().getZ() + 0.5D);
			final Vector3d floorCenter = new Vector3d(
				getBlockPos().getX() + 0.5D, firstUncoveredY, getBlockPos().getZ() + 0.5D);
			sendBeam(mode == LiftMode.UP ? floorCenter : liftCenter,
				mode == LiftMode.UP ? liftCenter : floorCenter, 1.0F, 1.0F, 0.0F);
		}
		if (moved) {
			level.playSound(null, getBlockPos(), Registration.SOUND_LASER_HIGH.get(),
				SoundCategory.AMBIENT, 1.0F, 1.0F);
		}
		return moved;
	}

	private void sendBeam(final Vector3d source, final Vector3d target,
	                      final float red, final float green, final float blue) {
		WarpDriveNetwork.CHANNEL.send(
			PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
				getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),
				128.0D, level.dimension())),
			new BeamEffectPacket(source, target, red, green, blue, 0, 40));
	}

	private void setBlockMode(final LiftMode newMode) {
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof LiftBlock
		 && blockState.getValue(LiftBlock.MODE) != newMode) {
			level.setBlock(getBlockPos(), blockState.setValue(LiftBlock.MODE, newMode), 3);
		}
	}

	public Object[] setOrGetMode(@Nullable final String requestedMode) {
		if (requestedMode != null) {
			switch (requestedMode.toLowerCase(Locale.ROOT)) {
				case "up":
					computerMode = LiftMode.UP;
					break;
				case "down":
					computerMode = LiftMode.DOWN;
					break;
				default:
					computerMode = LiftMode.REDSTONE;
			}
			setChanged();
			updateTicks = 0;
		}
		return new Object[]{ computerMode.getSerializedName() };
	}

	public Object[] state() {
		final String status = !enabled ? "disabled"
		                    : !valid ? "invalid"
		                    : getEnergyStored() < ENERGY_PER_ENTITY ? "insufficient energy"
		                    : active ? "active" : "inactive";
		return new Object[]{ status, active, getEnergyStored(), valid, enabled,
		                     computerMode.getSerializedName() };
	}

	public Object[] getEnergyRequired() {
		return new Object[]{ true, ENERGY_PER_ENTITY };
	}

	public Object[] getEnergyStatus() {
		return new Object[]{ getEnergyStored(), getMaxEnergyStored(), "FE" };
	}

	public Object[] enable(@Nullable final Boolean value) {
		if (value != null) {
			enabled = value;
			setChanged();
			updateTicks = 0;
		}
		return new Object[]{ enabled };
	}

	@Override
	public int getMaxEnergyStored() {
		return MAX_ENERGY_STORED;
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

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		mode = LiftMode.byIndex(tagCompound.getByte(TAG_MODE));
		computerMode = tagCompound.contains(TAG_COMPUTER_MODE)
		             ? LiftMode.byIndex(tagCompound.getByte(TAG_COMPUTER_MODE))
		             : LiftMode.REDSTONE;
		enabled = !tagCompound.contains(TAG_ENABLED) || tagCompound.getBoolean(TAG_ENABLED);
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putByte(TAG_MODE, (byte) mode.ordinal());
		tagCompound.putByte(TAG_COMPUTER_MODE, (byte) computerMode.ordinal());
		tagCompound.putBoolean(TAG_ENABLED, enabled);
		return tagCompound;
	}
}
