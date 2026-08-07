package cr0s.warpdrive.container;

import cr0s.warpdrive.block.ShipControllerTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Slotless native control menu for the Ship Controller block. */
public class ShipControllerContainer extends Container {

	private final BlockPos pos;
	private final World world;

	public ShipControllerContainer(final int windowId, final PlayerInventory inventory,
	                               final PacketBuffer data) {
		this(windowId, inventory, data.readBlockPos());
	}

	public ShipControllerContainer(final int windowId, final PlayerInventory inventory,
	                               final BlockPos pos) {
		super(Registration.SHIP_CONTROLLER_CONTAINER.get(), windowId);
		this.pos = pos;
		this.world = inventory.player.level;
	}

	public BlockPos getPos() {
		return pos;
	}

	@Nullable
	public ShipControllerTileEntity getTileEntity() {
		if (world == null) {
			return null;
		}
		final TileEntity tileEntity = world.getBlockEntity(pos);
		return tileEntity instanceof ShipControllerTileEntity
			? (ShipControllerTileEntity) tileEntity : null;
	}

	@Override
	public boolean stillValid(final PlayerEntity player) {
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0
			&& player.level.getBlockState(pos).getBlock() == Registration.SHIP_CONTROLLER_BLOCK.get();
	}
}
