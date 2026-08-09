package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.block.MachineStatusText;
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
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MiningLaserBlock extends Block {

	public static final EnumProperty<MiningLaserMode> MODE = EnumProperty.create("mode", MiningLaserMode.class);

	public MiningLaserBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(30.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(MODE, MiningLaserMode.INACTIVE));
	}

	@Override protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(MODE);
	}
	@Override public boolean hasTileEntity(final BlockState state) { return true; }
	@Nullable @Override public TileEntity createTileEntity(@Nonnull final BlockState state,
	                                                        @Nonnull final IBlockReader world) {
		return new MiningLaserTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState state, @Nonnull final World world,
	                            @Nonnull final BlockPos pos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof MiningLaserTileEntity)) return ActionResultType.PASS;
		final MiningLaserTileEntity miner = (MiningLaserTileEntity) tile;
		final ItemStack held = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && held.isEmpty() && miner.getPumpCount() > 0) {
			if (!world.isClientSide && miner.removePump() && !player.abilities.instabuild) {
				final ItemStack pump = new ItemStack(cr0s.warpdrive.data.Registration.COMPONENTS.get("pump").get());
				if (!player.inventory.add(pump)) popResource(world, pos, pump);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (held.getItem() == cr0s.warpdrive.data.Registration.COMPONENTS.get("pump").get()) {
			if (!world.isClientSide && miner.addPump() && !player.abilities.instabuild) held.shrink(1);
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (!held.isEmpty()) return ActionResultType.PASS;
		if (!world.isClientSide) {
			final int energyStored = miner.getLaserMediumEnergyStored();
			player.displayClientMessage(MachineStatusText.miningLaser(getName(),
				miner.getStatusTranslationKey(), miner.isInsufficientEnergy(energyStored),
				energyStored, miner.getLaserMediumMaxStorage(), miner.getPumpCount()), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}
}
