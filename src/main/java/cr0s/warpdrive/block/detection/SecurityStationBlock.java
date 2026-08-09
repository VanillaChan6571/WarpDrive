package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Fingerprint-style player allowlist used by security-aware machines. */
public class SecurityStationBlock extends Block {

	public SecurityStationBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new SecurityStationTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty()) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof SecurityStationTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final SecurityStationTileEntity station = (SecurityStationTileEntity) tileEntity;
			final String message = player.isShiftKeyDown()
				? station.getStatus() : station.togglePlayer(player);
			player.displayClientMessage(MachineStatusText.status(getName(), message), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
