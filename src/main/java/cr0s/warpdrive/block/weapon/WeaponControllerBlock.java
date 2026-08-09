package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Dedicated ComputerCraft host for the bundled weapon-control program. */
public class WeaponControllerBlock extends Block {

	public WeaponControllerBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(50.0F, 100.0F / 3.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops());
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new WeaponControllerTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || player.isShiftKeyDown()
		 || !player.getItemInHand(hand).isEmpty()
		 || !(world.getBlockEntity(blockPos) instanceof WeaponControllerTileEntity)) {
			return ActionResultType.PASS;
		}
		if (!world.isClientSide) {
			player.displayClientMessage(MachineStatusText.name(getName()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
