package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
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
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Player-facing identity scanner; its protruding face defines the three-block scan volume. */
public class BiometricScannerBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public BiometricScannerBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
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
		return defaultBlockState().setValue(FACING,
			context.getNearestLookingDirection().getOpposite());
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new BiometricScannerTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || hit.getDirection() != blockState.getValue(FACING)) {
			return ActionResultType.PASS;
		}
		final ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty()) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof BiometricScannerTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final Object[] result = ((BiometricScannerTileEntity) tileEntity).startScanning(player);
			player.displayClientMessage(MachineStatusText.status(getName(),
				String.valueOf(result[1])), false);
		}
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
