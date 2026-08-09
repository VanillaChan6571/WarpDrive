package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Deploying, non-colliding transporter beacon with placed-block FE storage. */
public class TransporterBeaconBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty DEPLOYED = BooleanProperty.create("deployed");
	private static final VoxelShape SHAPE = box(6.5D, 0.0D, 6.5D, 9.5D, 10.5D, 9.5D);

	public TransporterBeaconBlock() {
		super(AbstractBlock.Properties.of(Material.METAL).strength(0.5F)
			.sound(SoundType.METAL).noOcclusion()
			.lightLevel(state -> state.getValue(ACTIVE) ? 6 : 0));
		registerDefaultState(getStateDefinition().any()
			.setValue(ACTIVE, false).setValue(DEPLOYED, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE, DEPLOYED);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(@Nonnull final BlockState state, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos pos, @Nonnull final ISelectionContext context) {
		return SHAPE;
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(@Nonnull final BlockState state,
	                                    @Nonnull final IBlockReader world,
	                                    @Nonnull final BlockPos pos,
	                                    @Nonnull final ISelectionContext context) {
		return VoxelShapes.empty();
	}

	@Override public boolean hasTileEntity(final BlockState state) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                   @Nonnull final IBlockReader world) {
		return new TransporterBeaconTileEntity();
	}

	@Override
	public void setPlacedBy(@Nonnull final World world, @Nonnull final BlockPos position,
	                       @Nonnull final BlockState state, @Nullable final LivingEntity placer,
	                       @Nonnull final ItemStack itemStack) {
		super.setPlacedBy(world, position, state, placer, itemStack);
		final TileEntity tileEntity = world.getBlockEntity(position);
		if (tileEntity instanceof TransporterBeaconTileEntity) {
			((TransporterBeaconTileEntity) tileEntity).initializeFromItem(itemStack);
		}
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos position, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
			return ActionResultType.PASS;
		}
		final TileEntity tileEntity = world.getBlockEntity(position);
		if (!(tileEntity instanceof TransporterBeaconTileEntity)) return ActionResultType.PASS;
		final TransporterBeaconTileEntity beacon = (TransporterBeaconTileEntity) tileEntity;
		if (!world.isClientSide) {
			if (player.isShiftKeyDown()) beacon.setEnabled(!beacon.isEnabled());
			player.displayClientMessage(MachineStatusText.status(getName(), beacon.getStatus()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(final BlockState oldState, final World world, final BlockPos position,
	                     final BlockState newState, final boolean moving) {
		if (oldState.getBlock() != newState.getBlock()) {
			final TileEntity tileEntity = world.getBlockEntity(position);
			if (tileEntity instanceof TransporterBeaconTileEntity) {
				((TransporterBeaconTileEntity) tileEntity).onBeaconBroken();
			}
		}
		super.onRemove(oldState, world, position, newState, moving);
	}
}
