package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.block.weapon.AbstractLaserTileEntity;
import cr0s.warpdrive.network.BeamEffectPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Shared medium power, protected harvesting and adjacent-inventory output for laser collectors. */
public abstract class AbstractMinerTileEntity extends AbstractLaserTileEntity {

	private static final String TAG_ENABLED = "enabled";
	private static final String TAG_SILK_TOUCH = "enableSilktouch";
	protected boolean enabled = true;
	protected boolean silkTouch;

	protected AbstractMinerTileEntity(final TileEntityType<?> tileEntityType) {
		super(tileEntityType);
	}

	@Override
	protected Direction[] getValidLaserMediumDirections() {
		return Direction.values();
	}

	protected boolean consumeExactly(final int amount) {
		if (amount <= 0) return true;
		if (consumeLaserMediumEnergy(amount, true) < amount) return false;
		return consumeLaserMediumEnergy(amount, false) == amount;
	}

	protected boolean hasEnergy(final int amount) {
		return amount <= 0 || consumeLaserMediumEnergy(amount, true) >= amount;
	}

	protected boolean harvestBlock(final BlockPos targetPos, final BlockState expectedState) {
		if (!(level instanceof ServerWorld) || expectedState.isAir(level, targetPos)) return false;
		if (!canBreakBlock(targetPos)) return false;
		return harvestBlockUnchecked(targetPos, expectedState);
	}

	protected boolean harvestBlockUnchecked(final BlockPos targetPos, final BlockState expectedState) {
		if (!(level instanceof ServerWorld) || expectedState.isAir(level, targetPos)) return false;
		final ServerWorld serverWorld = (ServerWorld) level;
		final ServerPlayerEntity fakePlayer = FakePlayerFactory.getMinecraft(serverWorld);
		fakePlayer.setPos(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);

		final ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		if (silkTouch) tool.enchant(Enchantments.SILK_TOUCH, 1);
		final TileEntity targetTile = serverWorld.getBlockEntity(targetPos);
		final List<ItemStack> drops = new ArrayList<>(Block.getDrops(
			expectedState, serverWorld, targetPos, targetTile, fakePlayer, tool));
		serverWorld.levelEvent(2001, targetPos, Block.getId(expectedState));
		serverWorld.removeBlock(targetPos, false);
		if (outputDrops(drops)) setEnabled(false);
		return true;
	}

	protected boolean canBreakBlock(final BlockPos targetPos) {
		if (!(level instanceof ServerWorld)) return false;
		final ServerWorld serverWorld = (ServerWorld) level;
		final ServerPlayerEntity fakePlayer = FakePlayerFactory.getMinecraft(serverWorld);
		fakePlayer.setPos(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
		if (ForgeHooks.onBlockBreakEvent(serverWorld, GameType.SURVIVAL, fakePlayer, targetPos) != -1) {
			return true;
		}
		setEnabled(false);
		return false;
	}

	protected boolean canPlaceBlock(final BlockPos targetPos, final BlockState blockState) {
		if (!(level instanceof ServerWorld)) return false;
		final ServerWorld serverWorld = (ServerWorld) level;
		final ServerPlayerEntity fakePlayer = FakePlayerFactory.getMinecraft(serverWorld);
		fakePlayer.setPos(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
		final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, targetPos);
		return blockState.canSurvive(level, targetPos)
		    && !ForgeEventFactory.onBlockPlace(fakePlayer, snapshot, Direction.UP);
	}

	/** @return true when one or more stacks overflowed and were ejected above the machine. */
	protected boolean outputDrops(final List<ItemStack> drops) {
		if (level == null || drops == null || drops.isEmpty()) return false;
		boolean overflow = false;
		for (final ItemStack drop : drops) {
			if (drop.isEmpty()) continue;
			ItemStack remaining = drop.copy();
			for (final Direction direction : Direction.values()) {
				final TileEntity adjacent = level.getBlockEntity(getBlockPos().relative(direction));
				if (adjacent == null) continue;
				final LazyOptional<IItemHandler> capability = adjacent.getCapability(
					CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
				if (capability.isPresent()) {
					remaining = ItemHandlerHelper.insertItemStacked(capability.orElseThrow(
						() -> new IllegalStateException("Present item capability had no value")), remaining, false);
					if (remaining.isEmpty()) break;
				}
			}
			if (!remaining.isEmpty()) {
				overflow = true;
				level.addFreshEntity(new ItemEntity(level,
					getBlockPos().getX() + 0.5D, getBlockPos().getY() + 1.0D,
					getBlockPos().getZ() + 0.5D, remaining));
			}
		}
		return overflow;
	}

	protected void sendBeam(final Vector3d target, final float red, final float green, final float blue) {
		if (level == null || level.isClientSide) return;
		final Vector3d source = Vector3d.atCenterOf(getBlockPos());
		WarpDriveNetwork.CHANNEL.send(
			PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
				getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),
				128.0D, level.dimension())),
			new BeamEffectPacket(source, target, red, green, blue, 0));
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean nextEnabled) {
		if (enabled == nextEnabled) return;
		enabled = nextEnabled;
		setChanged();
	}

	public Object[] enable(final Boolean requested) {
		if (requested != null) setEnabled(requested);
		return new Object[]{ enabled };
	}

	public Object[] silktouch(final Boolean requested) {
		if (requested != null && silkTouch != requested) {
			silkTouch = requested;
			setChanged();
		}
		return new Object[]{ silkTouch };
	}

	public Object[] laserMediumDirection() {
		final Direction direction = getLaserMediumDirection();
		return new Object[]{ direction == null ? "none" : direction.getName() };
	}

	public Object[] laserMediumCount() {
		return new Object[]{ getLaserMediumCount() };
	}

	public Object[] getEnergyStatus() {
		return new Object[]{ getLaserMediumEnergyStored(), getLaserMediumMaxStorage(), "FE" };
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		enabled = !tagCompound.contains(TAG_ENABLED) || tagCompound.getBoolean(TAG_ENABLED);
		silkTouch = tagCompound.getBoolean(TAG_SILK_TOUCH);
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putBoolean(TAG_ENABLED, enabled);
		tagCompound.putBoolean(TAG_SILK_TOUCH, silkTouch);
		return tagCompound;
	}
}
