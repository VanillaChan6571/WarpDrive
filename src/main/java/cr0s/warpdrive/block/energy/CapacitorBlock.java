package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.MachineStatusText;
import cr0s.warpdrive.data.SideMode;
import cr0s.warpdrive.item.WrenchItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Subspace capacitor, ported from 1.12.2 BlockCapacitor.
 *
 * Wrenching a face cycles its routing - disabled, input, output - forwards, or backwards while
 * sneaking. 1.12.2 also recoloured each face to show its mode through unlisted blockstate
 * properties; that is a model change rather than behaviour and is not ported, so the face routing
 * is currently readable only from the chat feedback.
 */
public class CapacitorBlock extends Block {

	private final CapacitorTier tier;

	public CapacitorBlock(final CapacitorTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.harvestTool(ToolType.PICKAXE));
		this.tier = tier;
	}

	public CapacitorTier getTier() {
		return tier;
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new CapacitorTileEntity(tier);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		final ItemStack itemStackHeld = player.getItemInHand(hand);
		if (hand != Hand.MAIN_HAND) {
			return super.use(blockState, world, blockPos, player, hand, hit);
		}
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (itemStackHeld.isEmpty()) {
			if (player.isShiftKeyDown()) {
				return super.use(blockState, world, blockPos, player, hand, hit);
			}
			if (!(tileEntity instanceof CapacitorTileEntity)) {
				return super.use(blockState, world, blockPos, player, hand, hit);
			}
			if (!world.isClientSide) {
				final CapacitorTileEntity capacitor = (CapacitorTileEntity) tileEntity;
				player.displayClientMessage(MachineStatusText.energy(getName(),
					capacitor.getEnergyStored(), capacitor.getMaxEnergyStored()), false);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (!(itemStackHeld.getItem() instanceof WrenchItem)) {
			return super.use(blockState, world, blockPos, player, hand, hit);
		}
		if (world.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		if (!(tileEntity instanceof CapacitorTileEntity)) {
			return super.use(blockState, world, blockPos, player, hand, hit);
		}

		final CapacitorTileEntity capacitor = (CapacitorTileEntity) tileEntity;
		final Direction side = hit.getDirection();
		// sneaking steps backwards, so a mis-click is one click to undo rather than two
		final SideMode mode = player.isShiftKeyDown()
		                    ? capacitor.getMode(side).getPrevious()
		                    : capacitor.getMode(side).getNext();
		capacitor.setMode(side, mode);

		player.displayClientMessage(new TranslationTextComponent(
			"warpdrive.energy.side.changed_to_" + mode.getSerializedName(), side.getName()), true);
		return ActionResultType.CONSUME;
	}
}
