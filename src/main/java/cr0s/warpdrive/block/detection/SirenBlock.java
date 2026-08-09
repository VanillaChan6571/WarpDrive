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
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Redstone-controlled industrial or military siren. */
public class SirenBlock extends Block {

	public static final EnumProperty<SirenOrientation> ORIENTATION =
		EnumProperty.create("orientation", SirenOrientation.class);
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");

	private static final VoxelShape INDUSTRIAL_X = box(0.0D, 3.0D, 3.0D, 16.0D, 13.0D, 13.0D);
	private static final VoxelShape INDUSTRIAL_Z = box(3.0D, 3.0D, 0.0D, 13.0D, 13.0D, 16.0D);
	private static final VoxelShape MILITARY_Y = box(0.0D, 5.0D, 0.0D, 16.0D, 11.0D, 16.0D);
	private static final VoxelShape MILITARY_NORTH = box(0.0D, 5.0D, 7.0D, 16.0D, 11.0D, 13.0D);
	private static final VoxelShape MILITARY_SOUTH = box(0.0D, 5.0D, 3.0D, 16.0D, 11.0D, 9.0D);
	private static final VoxelShape MILITARY_WEST = box(7.0D, 5.0D, 0.0D, 13.0D, 11.0D, 16.0D);
	private static final VoxelShape MILITARY_EAST = box(3.0D, 5.0D, 0.0D, 9.0D, 11.0D, 16.0D);

	private final SirenStyle style;
	private final SirenTier tier;

	public SirenBlock(final SirenStyle style, final SirenTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F)
			.sound(SoundType.METAL)
			.noOcclusion()
			.requiresCorrectToolForDrops());
		this.style = style;
		this.tier = tier;
		registerDefaultState(getStateDefinition().any()
			.setValue(ORIENTATION, SirenOrientation.NORTH)
			.setValue(POWERED, false));
	}

	public SirenStyle getStyle() {
		return style;
	}

	public SirenTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ORIENTATION, POWERED);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		final Direction facing = context.getClickedFace();
		// The 1.12 helper's yaw mapping was opposite vanilla's modern getHorizontalDirection().
		final SirenOrientation orientation = SirenOrientation.of(
			facing, context.getHorizontalDirection().getOpposite());
		return defaultBlockState()
			.setValue(ORIENTATION, orientation)
			.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final Block neighborBlock,
	                            @Nonnull final BlockPos neighborPos, final boolean isMoving) {
		if (!world.isClientSide) {
			final boolean powered = world.hasNeighborSignal(blockPos);
			if (blockState.getValue(POWERED) != powered) {
				world.setBlock(blockPos, blockState.setValue(POWERED, powered), 3);
			}
		}
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		final SirenOrientation orientation = blockState.getValue(ORIENTATION);
		if (style == SirenStyle.INDUSTRIAL) {
			return orientation.getSpinning().getAxis() == Direction.Axis.X ? INDUSTRIAL_X : INDUSTRIAL_Z;
		}
		switch (orientation.getFacing()) {
			case UP:
			case DOWN:
				return MILITARY_Y;
			case SOUTH:
				return MILITARY_SOUTH;
			case WEST:
				return MILITARY_WEST;
			case EAST:
				return MILITARY_EAST;
			case NORTH:
			default:
				return MILITARY_NORTH;
		}
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new SirenTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || player.isShiftKeyDown()
		 || !player.getItemInHand(hand).isEmpty()
		 || !(world.getBlockEntity(blockPos) instanceof SirenTileEntity)) {
			return ActionResultType.PASS;
		}
		if (!world.isClientSide) {
			player.displayClientMessage(MachineStatusText.name(getName()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
