package cr0s.warpdrive.block.atomic;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Energy/control endpoint for connected void-shell accelerator trajectories. */
public class AcceleratorCoreBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public AcceleratorCoreBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new AcceleratorCoreTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof AcceleratorCoreTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final AcceleratorCoreTileEntity core = (AcceleratorCoreTileEntity) tileEntity;
			if (!player.isShiftKeyDown()) core.enable(!core.isEnabled());
			player.displayClientMessage(new StringTextComponent(core.getStatus()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
