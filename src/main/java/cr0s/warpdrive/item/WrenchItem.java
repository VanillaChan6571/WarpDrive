package cr0s.warpdrive.item;

import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/** Rotates ordinary facing blocks without opening their GUI. */
public class WrenchItem extends Item {

	public WrenchItem() {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
	}

	@Nonnull
	@Override
	public ActionResultType useOn(@Nonnull final ItemUseContext context) {
		final World world = context.getLevel();
		final BlockPos blockPos = context.getClickedPos();
		final PlayerEntity player = context.getPlayer();
		if (player != null
		 && (!player.mayUseItemAt(blockPos, context.getClickedFace(), context.getItemInHand())
		  || !world.mayInteract(player, blockPos))) {
			return ActionResultType.FAIL;
		}
		final BlockState current = world.getBlockState(blockPos);
		final BlockState rotated = rotate(current, world, blockPos, context.getClickedFace());

		if (rotated.equals(current)) {
			return ActionResultType.PASS;
		}
		if (!world.isClientSide) {
			world.setBlock(blockPos, rotated, 3);
			final SoundType sound = current.getSoundType(world, blockPos, context.getPlayer());
			world.playSound(null, blockPos, sound.getPlaceSound(), SoundCategory.BLOCKS,
				(sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	private static BlockState rotate(final BlockState blockState, final World world,
	                                 final BlockPos blockPos, final Direction clickedFace) {
		if (blockState.hasProperty(BlockStateProperties.FACING)) {
			final Direction current = blockState.getValue(BlockStateProperties.FACING);
			final Direction candidate = rotateAround(current, clickedFace);
			return candidate == current ? blockState : blockState.setValue(BlockStateProperties.FACING, candidate);
		}
		if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return blockState.setValue(BlockStateProperties.HORIZONTAL_FACING,
				blockState.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
		}
		return blockState.rotate(world, blockPos, Rotation.CLOCKWISE_90);
	}

	/** Quarter turn around the face normal, matching the axis supplied to 1.12's rotateBlock hook. */
	private static Direction rotateAround(final Direction direction, final Direction axis) {
		if (direction.getAxis() == axis.getAxis()) {
			return direction;
		}
		// Cross product direction x axis. The sign of the clicked face naturally reverses the turn.
		final int x = direction.getStepY() * axis.getStepZ() - direction.getStepZ() * axis.getStepY();
		final int y = direction.getStepZ() * axis.getStepX() - direction.getStepX() * axis.getStepZ();
		final int z = direction.getStepX() * axis.getStepY() - direction.getStepY() * axis.getStepX();
		return Direction.fromNormal(x, y, z);
	}
}
