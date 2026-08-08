package cr0s.warpdrive.block.collection;

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

public class LaserTreeFarmBlock extends Block {

	public static final EnumProperty<LaserTreeFarmMode> MODE =
		EnumProperty.create("mode", LaserTreeFarmMode.class);

	public LaserTreeFarmBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(30.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(MODE, LaserTreeFarmMode.INACTIVE));
	}

	@Override protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(MODE);
	}
	@Override public boolean hasTileEntity(final BlockState state) { return true; }
	@Nullable @Override public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                                        @Nonnull final IBlockReader world) {
		return new LaserTreeFarmTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos pos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) return ActionResultType.PASS;
		final TileEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof LaserTreeFarmTileEntity)) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final LaserTreeFarmTileEntity farm = (LaserTreeFarmTileEntity) tile;
			player.displayClientMessage(new TranslationTextComponent("warpdrive.laser_tree_farm.status",
				farm.getStatusText(), farm.getLaserMediumEnergyStored(), farm.getTotalHarvested()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
