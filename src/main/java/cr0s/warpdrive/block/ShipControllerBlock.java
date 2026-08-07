package cr0s.warpdrive.block;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Native, no-computer-required flight console for an adjacent Ship Core.
 *
 * The connection is deliberately physical instead of an absolute saved coordinate. Both blocks
 * therefore remain connected after a jump without needing a mod-specific NBT coordinate rewrite.
 */
public class ShipControllerBlock extends HorizontalBlock {

	public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

	public ShipControllerBlock() {
		super(Properties.of(Material.METAL)
			.strength(3.5F, 8.0F)
			.sound(net.minecraft.block.SoundType.METAL)
			.requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE)
			.harvestLevel(1));
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(CONNECTED, false));
	}

	@Override
	public boolean hasTileEntity(final BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
		return new ShipControllerTileEntity();
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(CONNECTED, hasAdjacentCore(context.getLevel(), context.getClickedPos()));
	}

	@Override
	public void setPlacedBy(@Nonnull final World world, @Nonnull final BlockPos pos,
	                        @Nonnull final BlockState state, @Nullable final LivingEntity placer,
	                        @Nonnull final ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		if (!world.isClientSide) {
			world.setBlock(pos, state.setValue(CONNECTED, hasAdjacentCore(world, pos)), 3);
		}
	}

	@Override
	public BlockState updateShape(@Nonnull final BlockState state, @Nonnull final Direction direction,
	                              @Nonnull final BlockState neighbourState, @Nonnull final IWorld world,
	                              @Nonnull final BlockPos pos, @Nonnull final BlockPos neighbourPos) {
		return state.setValue(CONNECTED, hasAdjacentCore(world, pos));
	}

	@Override
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos pos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) {
			return ActionResultType.PASS;
		}
		if (world.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		final TileEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof ShipControllerTileEntity && player instanceof ServerPlayerEntity) {
			final ShipControllerTileEntity controller = (ShipControllerTileEntity) tileEntity;
			controller.refreshAndSync();
			NetworkHooks.openGui((ServerPlayerEntity) player, controller,
				buffer -> buffer.writeBlockPos(pos));
			return ActionResultType.CONSUME;
		}
		return ActionResultType.PASS;
	}

	private static boolean hasAdjacentCore(final IBlockReader world, final BlockPos pos) {
		for (final Direction direction : Direction.values()) {
			if (world.getBlockState(pos.relative(direction)).getBlock() == Registration.SHIP_CORE_BLOCK.get()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public BlockState rotate(@Nonnull final BlockState state, @Nonnull final Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(@Nonnull final BlockState state, @Nonnull final Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, CONNECTED);
	}
}
