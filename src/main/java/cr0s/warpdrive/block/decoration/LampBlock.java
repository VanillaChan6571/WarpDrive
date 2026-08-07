package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.block.SimpleBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The three attachable 1.12 lamps, sharing facing, toggle and support behaviour. */
public class LampBlock extends SimpleBlock {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

	public enum Style {
		BUBBLE(new VoxelShape[]{
			box(0.0D, 4.8D, 0.0D, 16.0D, 16.0D, 16.0D),
			box(0.0D, 0.0D, 0.0D, 16.0D, 11.2D, 16.0D),
			box(0.0D, 0.0D, 4.8D, 16.0D, 16.0D, 16.0D),
			box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 11.2D),
			box(4.8D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
			box(0.0D, 0.0D, 0.0D, 11.2D, 16.0D, 16.0D) }),
		FLAT(new VoxelShape[]{
			box(0.0D, 13.44D, 0.0D, 16.0D, 16.0D, 16.0D),
			box(0.0D, 0.0D, 0.0D, 16.0D, 2.56D, 16.0D),
			box(0.0D, 0.0D, 13.44D, 16.0D, 16.0D, 16.0D),
			box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.56D),
			box(13.44D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
			box(0.0D, 0.0D, 0.0D, 2.56D, 16.0D, 16.0D) }),
		LONG(new VoxelShape[]{
			box(0.0D, 12.8D, 5.12D, 16.0D, 16.0D, 10.88D),
			box(0.0D, 0.0D, 5.12D, 16.0D, 3.2D, 10.88D),
			box(0.0D, 5.12D, 12.8D, 16.0D, 10.88D, 16.0D),
			box(0.0D, 5.12D, 0.0D, 16.0D, 10.88D, 3.2D),
			box(12.8D, 5.12D, 0.0D, 16.0D, 10.88D, 16.0D),
			box(0.0D, 5.12D, 0.0D, 3.2D, 10.88D, 16.0D) });

		private final VoxelShape[] shapes;

		Style(final VoxelShape[] shapes) {
			this.shapes = shapes;
		}

		private VoxelShape shape(final Direction direction) {
			return shapes[direction.ordinal()];
		}
	}

	private final Style style;

	public LampBlock(final Style style) {
		super(AbstractBlock.Properties.of(Material.DECORATION)
			// Default 1.12 hull configuration for the basic-tier lamp casing.
			.strength(25.0F, 100.0F)
			.sound(SoundType.METAL)
			.noCollission()
			.noOcclusion()
			.lightLevel(state -> state.getValue(ACTIVE) ? 14 : 0));
		this.style = style;
		registerDefaultState(getStateDefinition().any()
			.setValue(FACING, Direction.DOWN)
			.setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(@Nonnull final BlockItemUseContext context) {
		for (final Direction direction : context.getNearestLookingDirections()) {
			final BlockState candidate = defaultBlockState().setValue(FACING, direction);
			if (candidate.canSurvive(context.getLevel(), context.getClickedPos())) {
				return candidate;
			}
		}
		return null;
	}

	@Override
	public boolean canSurvive(@Nonnull final BlockState blockState, @Nonnull final IWorldReader world,
	                          @Nonnull final BlockPos blockPos) {
		final Direction facing = blockState.getValue(FACING);
		final BlockPos supportPos = blockPos.relative(facing.getOpposite());
		return world.getBlockState(supportPos).isFaceSturdy(world, supportPos, facing);
	}

	@Nonnull
	@Override
	public BlockState updateShape(@Nonnull final BlockState blockState, @Nonnull final Direction direction,
	                              @Nonnull final BlockState neighbourState, @Nonnull final IWorld world,
	                              @Nonnull final BlockPos blockPos, @Nonnull final BlockPos neighbourPos) {
		if (direction != blockState.getValue(FACING).getOpposite()
		 || blockState.canSurvive(world, blockPos)) {
			return blockState;
		}
		// The 1.12 lamps reattached to another sturdy face before dropping.
		for (final Direction candidateFacing : Direction.values()) {
			final BlockState candidate = blockState.setValue(FACING, candidateFacing);
			if (candidate.canSurvive(world, blockPos)) {
				return candidate;
			}
		}
		return Blocks.AIR.defaultBlockState();
	}

	@Nonnull
	@Override
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || player.isShiftKeyDown()) {
			return ActionResultType.PASS;
		}
		if (!world.isClientSide) {
			world.setBlock(blockPos, blockState.cycle(ACTIVE), 3);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Nonnull
	@Override
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		return style.shape(blockState.getValue(FACING));
	}

	@Nonnull
	@Override
	public BlockState rotate(@Nonnull final BlockState blockState, @Nonnull final Rotation rotation) {
		return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
	}

	@Nonnull
	@Override
	public BlockState mirror(@Nonnull final BlockState blockState, @Nonnull final Mirror mirror) {
		return rotate(blockState, mirror.getRotation(blockState.getValue(FACING)));
	}
}
