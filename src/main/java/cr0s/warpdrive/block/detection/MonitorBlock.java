package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.client.CameraViewController;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
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
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tunable monitor which opens a loaded camera endpoint on its front face. */
public class MonitorBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public MonitorBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING);
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
		return new MonitorTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()
		 || hit.getDirection() != blockState.getValue(FACING)) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof MonitorTileEntity)) return ActionResultType.PASS;
		final MonitorTileEntity monitor = (MonitorTileEntity) tileEntity;

		if (world.isClientSide && !player.isShiftKeyDown()) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientAccess.open(blockPos, monitor.getVideoChannel()));
		} else if (!world.isClientSide && player.isShiftKeyDown()) {
			player.displayClientMessage(new TranslationTextComponent("warpdrive.monitor.status",
				monitor.getVideoChannel()), false);
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

	@OnlyIn(Dist.CLIENT)
	private static final class ClientAccess {
		private static void open(final BlockPos monitorPos, final int videoChannel) {
			CameraViewController.open(monitorPos, videoChannel);
		}
	}
}
