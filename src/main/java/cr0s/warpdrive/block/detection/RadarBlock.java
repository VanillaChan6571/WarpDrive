package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.block.MachineStatusText;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RadarBlock extends Block {

	public static final EnumProperty<RadarMode> MODE = EnumProperty.create("mode", RadarMode.class);

	public RadarBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(MODE, RadarMode.INACTIVE));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(MODE);
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }
	@Nullable @Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new RadarTileEntity();
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
		if (!(tileEntity instanceof RadarTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final RadarTileEntity radar = (RadarTileEntity) tileEntity;
			player.displayClientMessage(MachineStatusText.stateAndEnergy(getName(),
				new TranslationTextComponent("warpdrive.status.state."
					+ blockState.getValue(MODE).getSerializedName()),
				radar.getEnergyStored(), radar.getMaxEnergyStored()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
