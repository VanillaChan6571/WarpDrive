package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Controller for the twelve-coil legacy cloaking multiblock. */
public class CloakingCoreBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public CloakingCoreBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(4.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new CloakingCoreTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof CloakingCoreTileEntity)) return ActionResultType.PASS;
		final CloakingCoreTileEntity core = (CloakingCoreTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty()
		 && held.getItem() != Registration.COMPONENTS.get("diamond_crystal").get()) {
			return ActionResultType.PASS;
		}
		if (!world.isClientSide) {
			final String message;
			if (!held.isEmpty()) {
				if (core.addDiamondCrystal()) {
					if (!player.abilities.instabuild) held.shrink(1);
					message = "Cloaking upgrade installed. " + core.getStatus();
				} else {
					message = "All six cloaking upgrades are already installed. " + core.getStatus();
				}
			} else if (player.isShiftKeyDown() && core.removeDiamondCrystal()) {
				player.addItem(new ItemStack(Registration.COMPONENTS.get("diamond_crystal").get()));
				message = "Cloaking upgrade removed. " + core.getStatus();
			} else if (player.isShiftKeyDown()) {
				message = core.getStatus();
			} else {
				core.enable(!core.isEnabled());
				message = core.getStatus();
			}
			player.displayClientMessage(new StringTextComponent(message), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
	}

	@Override
	public void playerWillDestroy(@Nonnull final World world, @Nonnull final BlockPos blockPos,
	                              @Nonnull final BlockState blockState,
	                              @Nonnull final PlayerEntity player) {
		// Ship movement removes and recreates blocks without a player. Restricting refunds to an
		// actual survival break prevents copied upgrade drops during a warp.
		if (!world.isClientSide && !player.abilities.instabuild) {
			final TileEntity tileEntity = world.getBlockEntity(blockPos);
			if (tileEntity instanceof CloakingCoreTileEntity) {
				final int count = ((CloakingCoreTileEntity) tileEntity).getDiamondCrystalCount();
				if (count > 0) popResource(world, blockPos,
					new ItemStack(Registration.COMPONENTS.get("diamond_crystal").get(), count));
			}
		}
		super.playerWillDestroy(world, blockPos, blockState, player);
	}
}
