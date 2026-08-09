package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.block.MachineStatusText;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tiered, frequency-linked relay carrying one force-field upgrade. */
public class ForceFieldRelayBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final EnumProperty<ForceFieldUpgrade> UPGRADE =
		EnumProperty.create("upgrade", ForceFieldUpgrade.class);
	private static final VoxelShape SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);
	private final ForceFieldTier tier;

	public ForceFieldRelayBlock(final ForceFieldTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(tier.getHardness(), tier.getBlastResistance())
			.sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any()
			.setValue(FACING, Direction.NORTH).setValue(ACTIVE, false)
			.setValue(UPGRADE, ForceFieldUpgrade.NONE));
	}

	public ForceFieldTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE, UPGRADE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		return SHAPE;
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world) {
		return new ForceFieldRelayTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) {
			return ActionResultType.PASS;
		}
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof ForceFieldRelayTileEntity)) {
			return ActionResultType.PASS;
		}
		final ForceFieldRelayTileEntity relay = (ForceFieldRelayTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		final ForceFieldUpgrade heldUpgrade = getUpgrade(held);
		if (player.isShiftKeyDown()) {
			if (relay.getUpgrade() == ForceFieldUpgrade.NONE) {
				return ActionResultType.PASS;
			}
			if (!world.isClientSide) {
				giveOrDrop(player, blockPos, new ItemStack(
					Registration.FORCE_FIELD_UPGRADES.get(relay.getUpgrade().getSerializedName()).get()));
				relay.setUpgrade(ForceFieldUpgrade.NONE);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (heldUpgrade != ForceFieldUpgrade.NONE) {
			if (heldUpgrade.getRelayLimit() <= 0) {
				if (!world.isClientSide) {
					player.displayClientMessage(new TranslationTextComponent(
						"warpdrive.force_field.invalid_relay_upgrade"), true);
				}
				return ActionResultType.sidedSuccess(world.isClientSide);
			}
			if (!world.isClientSide) {
				if (relay.getUpgrade() != ForceFieldUpgrade.NONE) {
					giveOrDrop(player, blockPos, new ItemStack(
						Registration.FORCE_FIELD_UPGRADES.get(relay.getUpgrade().getSerializedName()).get()));
				}
				if (!player.abilities.instabuild) held.shrink(1);
				relay.setUpgrade(heldUpgrade);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (held.isEmpty()) {
			if (!world.isClientSide) {
				player.displayClientMessage(MachineStatusText.status(getName(),
					new TranslationTextComponent("warpdrive.force_field.relay.status",
						MachineStatusText.value(relay.getBeamFrequency()),
						MachineStatusText.value(relay.getUpgrade().getSerializedName()))), false);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		return ActionResultType.PASS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(final BlockState oldState, final World world, final BlockPos blockPos,
	                     final BlockState newState, final boolean isMoving) {
		if (oldState.getBlock() != newState.getBlock()) {
			final TileEntity tileEntity = world.getBlockEntity(blockPos);
			if (tileEntity instanceof AbstractForceFieldTileEntity) {
				ForceFieldRegistry.remove((AbstractForceFieldTileEntity) tileEntity);
			}
		}
		super.onRemove(oldState, world, blockPos, newState, isMoving);
	}

	private static ForceFieldUpgrade getUpgrade(final ItemStack itemStack) {
		final ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
		if (registryName == null || !registryName.getNamespace().equals("warpdrive")
		 || !registryName.getPath().startsWith("force_field_upgrade-")) {
			return ForceFieldUpgrade.NONE;
		}
		return ForceFieldUpgrade.fromRegistrySuffix(
			registryName.getPath().substring("force_field_upgrade-".length()));
	}

	static void giveOrDrop(final PlayerEntity player, final BlockPos blockPos, final ItemStack itemStack) {
		if (player.abilities.instabuild || itemStack.isEmpty()) return;
		if (!player.inventory.add(itemStack)) {
			popResource(player.level, blockPos, itemStack);
		}
	}
}
