package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Energy-backed transporter controller and its four legacy visual states. */
public class TransporterCoreBlock extends Block {

	public static final EnumProperty<TransporterState> STATE =
		EnumProperty.create("state", TransporterState.class);

	public TransporterCoreBlock() {
		super(AbstractBlock.Properties.of(Material.METAL).strength(3.5F, 10.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE).harvestLevel(2));
		registerDefaultState(getStateDefinition().any().setValue(STATE, TransporterState.DISABLED));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(STATE);
	}

	@Override
	public boolean hasTileEntity(final BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                   @Nonnull final IBlockReader world) {
		return new TransporterCoreTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof TransporterCoreTileEntity)) return ActionResultType.PASS;
		final TransporterCoreTileEntity core = (TransporterCoreTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		if (held.getItem() instanceof TransporterBeaconBlockItem) return ActionResultType.PASS;

		final TransporterCoreTileEntity.Upgrade upgrade = upgradeFor(held);
		if (upgrade != null) {
			if (!world.isClientSide) {
				if (player.isShiftKeyDown()) {
					if (core.removeUpgrade(upgrade) && !player.abilities.instabuild) {
						giveOrDrop(player, blockPos, new ItemStack(upgrade.getItem()));
					}
				} else if (core.addUpgrade(upgrade) && !player.abilities.instabuild) {
					held.shrink(1);
				}
				player.displayClientMessage(new StringTextComponent(core.getUpgradeStatus()), true);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}

		if (!held.isEmpty()) return ActionResultType.PASS;
		if (!world.isClientSide) {
			if (player.isShiftKeyDown()) core.setEnabled(!core.isEnabled());
			player.displayClientMessage(new StringTextComponent(core.getStatus()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Nullable
	private static TransporterCoreTileEntity.Upgrade upgradeFor(final ItemStack held) {
		if (held.isEmpty()) return null;
		for (final TransporterCoreTileEntity.Upgrade upgrade :
			TransporterCoreTileEntity.Upgrade.values()) {
			if (held.getItem() == upgrade.getItem()) return upgrade;
		}
		return null;
	}

	private static void giveOrDrop(final PlayerEntity player, final BlockPos blockPos,
	                               final ItemStack itemStack) {
		if (!player.inventory.add(itemStack)) popResource(player.level, blockPos, itemStack);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(final BlockState oldState, final World world, final BlockPos blockPos,
	                     final BlockState newState, final boolean moving) {
		if (oldState.getBlock() != newState.getBlock()) {
			final TileEntity tileEntity = world.getBlockEntity(blockPos);
			if (tileEntity instanceof TransporterCoreTileEntity) {
				((TransporterCoreTileEntity) tileEntity).onCoreBroken();
			}
		}
		super.onRemove(oldState, world, blockPos, newState, moving);
	}
}
