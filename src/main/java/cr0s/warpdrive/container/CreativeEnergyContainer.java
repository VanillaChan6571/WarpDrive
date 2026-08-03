package cr0s.warpdrive.container;

import cr0s.warpdrive.block.CreativeEnergyTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Slotless container for the creative energy source.
 *
 * There are no data slots on purpose: vanilla container data is synced as a short, which cannot
 * carry an FE rate up to 1,000,000. The tile entity syncs itself through getUpdatePacket instead
 * and the screen reads the values straight off the client-side tile entity.
 */
public class CreativeEnergyContainer extends Container {

	private final BlockPos pos;
	private final World world;

	public CreativeEnergyContainer(final int windowId, final PlayerInventory inventory, final PacketBuffer data) {
		this(windowId, inventory, data.readBlockPos());
	}

	public CreativeEnergyContainer(final int windowId, final PlayerInventory inventory, final BlockPos pos) {
		super(Registration.CREATIVE_ENERGY_CONTAINER.get(), windowId);
		this.pos = pos;
		this.world = inventory.player.level;
	}

	public BlockPos getPos() {
		return pos;
	}

	@Nullable
	public CreativeEnergyTileEntity getTileEntity() {
		if (world == null) {
			return null;
		}
		final TileEntity tileEntity = world.getBlockEntity(pos);
		return tileEntity instanceof CreativeEnergyTileEntity ? (CreativeEnergyTileEntity) tileEntity : null;
	}

	@Override
	public boolean stillValid(final PlayerEntity player) {
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}
}
