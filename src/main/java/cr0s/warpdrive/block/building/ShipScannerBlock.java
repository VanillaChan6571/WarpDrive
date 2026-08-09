package cr0s.warpdrive.block.building;

import cr0s.warpdrive.block.MachineStatusText;
import cr0s.warpdrive.item.ShipTokenItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
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

/** Tiered ship schematic scanner/builder. */
public class ShipScannerBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private final ShipScannerTier tier;

	public ShipScannerBlock(final ShipScannerTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F + tier.ordinal(), 10.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE));
		this.tier = tier;
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	public ShipScannerTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new ShipScannerTileEntity();
	}

	@Override
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof ShipScannerTileEntity)) return ActionResultType.PASS;
		if (world.isClientSide) return ActionResultType.SUCCESS;

		final ShipScannerTileEntity scanner = (ShipScannerTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && held.getItem() instanceof ShipTokenItem) {
			scanner.bindToken((ServerPlayerEntity) player, held);
		} else if (player.isShiftKeyDown() && held.isEmpty()) {
			scanner.startScan((ServerPlayerEntity) player);
		} else if (held.isEmpty() || held.getItem() instanceof ShipTokenItem) {
			player.displayClientMessage(MachineStatusText.status(getName(), scanner.getStatusText()), false);
		} else {
			return ActionResultType.PASS;
		}
		return ActionResultType.CONSUME;
	}
}
