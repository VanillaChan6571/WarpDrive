package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
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
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Six-way environmental probe with the legacy plate-sized collision shapes. */
public class EnvironmentalSensorBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private static final VoxelShape DOWN = box(2.0D, 11.0D, 2.0D, 14.0D, 16.0D, 14.0D);
	private static final VoxelShape UP = box(2.0D, 0.0D, 2.0D, 14.0D, 5.0D, 14.0D);
	private static final VoxelShape NORTH = box(2.0D, 2.0D, 11.0D, 14.0D, 14.0D, 16.0D);
	private static final VoxelShape SOUTH = box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 5.0D);
	private static final VoxelShape WEST = box(11.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D);
	private static final VoxelShape EAST = box(0.0D, 2.0D, 2.0D, 5.0D, 14.0D, 14.0D);

	public EnvironmentalSensorBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL)
			.requiresCorrectToolForDrops().noOcclusion());
		registerDefaultState(getStateDefinition().any()
			.setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		Direction facing = context.getNearestLookingDirection().getOpposite();
		final PlayerEntity player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) facing = facing.getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(@Nonnull final BlockState blockState,
	                           @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos,
	                           @Nonnull final ISelectionContext context) {
		switch (blockState.getValue(FACING)) {
		case DOWN: return DOWN;
		case UP: return UP;
		case SOUTH: return SOUTH;
		case WEST: return WEST;
		case EAST: return EAST;
		case NORTH:
		default: return NORTH;
		}
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new EnvironmentalSensorTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || player.isShiftKeyDown()
		 || !player.getItemInHand(hand).isEmpty()
		 || !(world.getBlockEntity(blockPos) instanceof EnvironmentalSensorTileEntity)) {
			return ActionResultType.PASS;
		}
		if (!world.isClientSide) player.displayClientMessage(MachineStatusText.name(getName()), false);
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState rotate(final BlockState blockState, final Rotation rotation) {
		return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(final BlockState blockState, final Mirror mirror) {
		return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
	}
}
