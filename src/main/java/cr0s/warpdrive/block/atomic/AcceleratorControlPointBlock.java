package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.BlockState;
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

/** Tunable accelerator control point; injectors use the same block behavior with a distinct tile. */
public class AcceleratorControlPointBlock extends AcceleratorComponentBlock {

	private final boolean injector;

	public AcceleratorControlPointBlock(final boolean injector) {
		super(1);
		this.injector = injector;
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return injector ? new ParticlesInjectorTileEntity() : new AcceleratorControlPointTileEntity();
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
		if (!(tileEntity instanceof AcceleratorControlPointTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			player.displayClientMessage(MachineStatusText.controlChannel(getName(),
				((AcceleratorControlPointTileEntity) tileEntity).getControlChannel()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
