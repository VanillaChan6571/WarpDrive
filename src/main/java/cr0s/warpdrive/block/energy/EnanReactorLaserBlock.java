package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Stabilization laser; its facing is assigned by the reactor assembly rather than placement. */
public class EnanReactorLaserBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public EnanReactorLaserBlock() {
		super(AbstractBlock.Properties.of(Material.METAL).strength(3.5F, 100.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE).harvestLevel(2));
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false)
			.setValue(FACING, Direction.DOWN));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE, FACING);
	}

	@Override public boolean hasTileEntity(final BlockState state) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                   @Nonnull final IBlockReader world) {
		return new EnanReactorLaserTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || player.isShiftKeyDown()
		 || !player.getItemInHand(hand).isEmpty()) {
			return ActionResultType.PASS;
		}
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof EnanReactorLaserTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			player.displayClientMessage(MachineStatusText.status(getName(),
				((EnanReactorLaserTileEntity) tileEntity).getStatus()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final Block block,
	                            @Nonnull final BlockPos fromPos, final boolean moving) {
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof EnanReactorLaserTileEntity) {
			((EnanReactorLaserTileEntity) tileEntity).markAssemblyDirty();
		}
		super.neighborChanged(state, world, blockPos, block, fromPos, moving);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(final BlockState oldState, final World world, final BlockPos blockPos,
	                     final BlockState newState, final boolean moving) {
		if (oldState.getBlock() != newState.getBlock()) {
			final TileEntity tileEntity = world.getBlockEntity(blockPos);
			if (tileEntity instanceof EnanReactorLaserTileEntity) {
				((EnanReactorLaserTileEntity) tileEntity).onLaserBroken();
			}
		}
		super.onRemove(oldState, world, blockPos, newState, moving);
	}
}
