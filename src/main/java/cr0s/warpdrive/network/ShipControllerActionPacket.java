package cr0s.warpdrive.network;

import cr0s.warpdrive.block.ShipControllerTileEntity;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** GUI -> server actions for the native Ship Controller. */
public class ShipControllerActionPacket {

	public static final int APPLY = 0;
	public static final int JUMP = 1;
	public static final int ABORT = 2;
	public static final int TOGGLE_BOUNDS = 3;
	public static final int SCAN = 4;

	private final BlockPos pos;
	private final int action;
	private final int[] dimensions;
	private final int[] movement;
	private final int orientation;
	private final int rotation;
	private final String targetDimension;
	private final String shipName;

	public ShipControllerActionPacket(final BlockPos pos, final int action,
	                                  final int[] dimensions, final int[] movement,
	                                  final int orientation, final int rotation,
	                                  final String targetDimension, final String shipName) {
		this.pos = pos;
		this.action = action;
		this.dimensions = dimensions.clone();
		this.movement = movement.clone();
		this.orientation = orientation;
		this.rotation = rotation;
		this.targetDimension = targetDimension == null ? "" : targetDimension;
		this.shipName = shipName == null ? "" : shipName;
	}

	public static ShipControllerActionPacket simple(final BlockPos pos, final int action) {
		return new ShipControllerActionPacket(pos, action, new int[6], new int[3], 0, 0, "", "");
	}

	public static void encode(final ShipControllerActionPacket packet, final PacketBuffer buffer) {
		buffer.writeBlockPos(packet.pos);
		buffer.writeByte(packet.action);
		for (final int value : packet.dimensions) {
			buffer.writeVarInt(value);
		}
		for (final int value : packet.movement) {
			buffer.writeInt(value);
		}
		buffer.writeByte(packet.orientation);
		buffer.writeByte(packet.rotation);
		buffer.writeUtf(packet.targetDimension, 128);
		buffer.writeUtf(packet.shipName, 64);
	}

	public static ShipControllerActionPacket decode(final PacketBuffer buffer) {
		final BlockPos pos = buffer.readBlockPos();
		final int action = buffer.readUnsignedByte();
		final int[] dimensions = new int[6];
		for (int index = 0; index < dimensions.length; index++) {
			dimensions[index] = buffer.readVarInt();
		}
		final int[] movement = new int[3];
		for (int index = 0; index < movement.length; index++) {
			movement[index] = buffer.readInt();
		}
		return new ShipControllerActionPacket(pos, action, dimensions, movement,
			buffer.readUnsignedByte(), buffer.readUnsignedByte(),
			buffer.readUtf(128), buffer.readUtf(64));
	}

	public static void handle(final ShipControllerActionPacket packet,
	                          final Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			final ServerPlayerEntity player = context.get().getSender();
			if (player == null || player.distanceToSqr(packet.pos.getX() + 0.5,
				packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) > 64.0) {
				return;
			}
			final TileEntity tileEntity = player.level.getBlockEntity(packet.pos);
			if (!(tileEntity instanceof ShipControllerTileEntity)) {
				DebugLog.log("SHIP", "rejected controller action: no controller at {}", packet.pos);
				return;
			}

			final ShipControllerTileEntity controller = (ShipControllerTileEntity) tileEntity;
			switch (packet.action) {
				case APPLY:
					final String[] orientations = { "north", "east", "south", "west" };
					controller.applySettings(
						packet.dimensions[0], packet.dimensions[1], packet.dimensions[2],
						packet.dimensions[3], packet.dimensions[4], packet.dimensions[5],
						packet.movement[0], packet.movement[1], packet.movement[2],
						orientations[Math.floorMod(packet.orientation, orientations.length)],
						Math.floorMod(packet.rotation, 4), packet.targetDimension, packet.shipName);
					break;
				case JUMP:
					controller.startJump();
					break;
				case ABORT:
					controller.abortJump();
					break;
				case TOGGLE_BOUNDS:
					controller.toggleBounds(player);
					break;
				case SCAN:
					controller.scanShip();
					break;
				default:
					DebugLog.log("SHIP", "rejected unknown controller action {}", packet.action);
			}
		});
		context.get().setPacketHandled(true);
	}
}
