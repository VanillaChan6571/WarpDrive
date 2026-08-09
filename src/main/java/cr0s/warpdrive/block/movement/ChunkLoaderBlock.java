package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.block.MachineStatusText;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Energy-backed dedicated chunk loader with the original component-upgrade interaction. */
public class ChunkLoaderBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private final ChunkLoaderTier tier;

	public ChunkLoaderBlock(final ChunkLoaderTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL).strength(5.0F, 10.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	public ChunkLoaderTier getTier() { return tier; }

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override public boolean hasTileEntity(final BlockState state) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                   @Nonnull final IBlockReader world) {
		return new ChunkLoaderTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos position, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(position);
		if (!(tileEntity instanceof ChunkLoaderTileEntity)) return ActionResultType.PASS;
		final ChunkLoaderTileEntity loader = (ChunkLoaderTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		final ChunkLoaderTileEntity.Upgrade upgrade = getUpgrade(held);
		final boolean isRedstoneTorch = held.getItem() == Blocks.REDSTONE_TORCH.asItem();

		if (player.isShiftKeyDown() && !held.isEmpty() && upgrade == null) {
			return ActionResultType.PASS;
		}
		if (!held.isEmpty() && upgrade == null && !isRedstoneTorch) {
			return ActionResultType.PASS;
		}

		if (!world.isClientSide) {
			String message;
			if (player.isShiftKeyDown()) {
				final ChunkLoaderTileEntity.Upgrade removed = loader.removeUpgrade(upgrade);
				if (removed == null) {
					message = "No matching chunk-loader upgrade is installed. ";
				} else {
					if (!player.abilities.instabuild) {
						final ItemStack returned = new ItemStack(getUpgradeItem(removed));
						if (!player.addItem(returned)) player.drop(returned, false);
					}
					message = "Chunk-loader upgrade removed. ";
				}
				message += loader.getStatus();
			} else if (upgrade != null) {
				if (loader.addUpgrade(upgrade)) {
					if (!player.abilities.instabuild) held.shrink(1);
					message = "Chunk-loader upgrade installed. " + loader.getStatus();
				} else {
					message = "That chunk-loader upgrade is already at its limit. " + loader.getStatus();
				}
			} else if (isRedstoneTorch) {
				loader.setEnabled(!loader.isEnabled());
				message = loader.getStatus();
			} else {
				message = loader.getStatus();
			}
			player.displayClientMessage(MachineStatusText.status(getName(), message), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Nullable
	private static ChunkLoaderTileEntity.Upgrade getUpgrade(final ItemStack itemStack) {
		if (itemStack.isEmpty()) return null;
		if (itemStack.getItem() == getUpgradeItem(ChunkLoaderTileEntity.Upgrade.EFFICIENCY)) {
			return ChunkLoaderTileEntity.Upgrade.EFFICIENCY;
		}
		if (itemStack.getItem() == getUpgradeItem(ChunkLoaderTileEntity.Upgrade.RANGE)) {
			return ChunkLoaderTileEntity.Upgrade.RANGE;
		}
		if (itemStack.getItem() == getUpgradeItem(ChunkLoaderTileEntity.Upgrade.COMPUTER_INTERFACE)) {
			return ChunkLoaderTileEntity.Upgrade.COMPUTER_INTERFACE;
		}
		return null;
	}

	private static net.minecraft.item.Item getUpgradeItem(final ChunkLoaderTileEntity.Upgrade upgrade) {
		switch (upgrade) {
		case EFFICIENCY: return Registration.COMPONENTS.get("superconductor").get();
		case RANGE: return Registration.COMPONENTS.get("emerald_crystal").get();
		case COMPUTER_INTERFACE: return Registration.COMPONENTS.get("computer_interface").get();
		default: throw new IllegalArgumentException("Unknown chunk-loader upgrade " + upgrade);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(final BlockState oldState, final World world, final BlockPos position,
	                     final BlockState newState, final boolean moving) {
		if (oldState.getBlock() != newState.getBlock()) {
			final TileEntity tileEntity = world.getBlockEntity(position);
			if (tileEntity instanceof ChunkLoaderTileEntity) {
				((ChunkLoaderTileEntity) tileEntity).onLoaderBroken();
			}
		}
		super.onRemove(oldState, world, position, newState, moving);
	}
}
