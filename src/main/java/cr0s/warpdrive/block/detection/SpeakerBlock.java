package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.MachineStatusText;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Computer-controlled local chat speaker. */
public class SpeakerBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	private final SpeakerTier tier;

	public SpeakerBlock(final SpeakerTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.DOWN));
	}

	public SpeakerTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		Direction facing = context.getNearestLookingDirection().getOpposite();
		final PlayerEntity player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			facing = facing.getOpposite();
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new SpeakerTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || player.isShiftKeyDown()
		 || !player.getItemInHand(hand).isEmpty()) {
			return ActionResultType.PASS;
		}
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof SpeakerTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			player.displayClientMessage(MachineStatusText.name(getName()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
