package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tiered enantiomorphic reactor core with the original 4x4 visual telemetry states. */
public class EnanReactorCoreBlock extends Block {

	public static final IntegerProperty ENERGY = IntegerProperty.create("energy", 0, 3);
	public static final IntegerProperty STABILITY = IntegerProperty.create("stability", 0, 3);

	private final EnanReactorTier tier;

	public EnanReactorCoreBlock(final EnanReactorTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL).strength(3.5F, 10.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE).harvestLevel(2));
		this.tier = tier;
		registerDefaultState(getStateDefinition().any().setValue(ENERGY, 0).setValue(STABILITY, 0));
	}

	public EnanReactorTier getTier() { return tier; }

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ENERGY, STABILITY);
	}

	@Override public boolean hasTileEntity(final BlockState state) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                   @Nonnull final IBlockReader world) {
		return new EnanReactorCoreTileEntity(tier);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty()) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof EnanReactorCoreTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final EnanReactorCoreTileEntity core = (EnanReactorCoreTileEntity) tileEntity;
			if (player.isShiftKeyDown()) core.enable(!core.isEnabled());
			player.displayClientMessage(MachineStatusText.status(getName(), core.getStatus()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final Block block,
	                            @Nonnull final BlockPos fromPos, final boolean moving) {
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof EnanReactorCoreTileEntity) {
			((EnanReactorCoreTileEntity) tileEntity).markAssemblyDirty();
		}
		super.neighborChanged(state, world, blockPos, block, fromPos, moving);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(final BlockState oldState, final World world, final BlockPos blockPos,
	                     final BlockState newState, final boolean moving) {
		if (oldState.getBlock() != newState.getBlock()) {
			final TileEntity tileEntity = world.getBlockEntity(blockPos);
			if (tileEntity instanceof EnanReactorCoreTileEntity) {
				((EnanReactorCoreTileEntity) tileEntity).onCoreBroken();
			}
		}
		super.onRemove(oldState, world, blockPos, newState, moving);
	}
}
