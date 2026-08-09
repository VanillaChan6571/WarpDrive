package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.block.MachineStatusText;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.item.CatalogVariantBlockItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
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

/** Tiered half/full projector with shape and upgrade mounting interactions. */
public class ForceFieldProjectorBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty DOUBLE_SIDED = BooleanProperty.create("is_double_sided");
	public static final EnumProperty<ForceFieldShape> SHAPE =
		EnumProperty.create("shape", ForceFieldShape.class);
	private static final VoxelShape SHAPE_Y = box(0.0D, 4.32D, 0.0D, 16.0D, 11.68D, 16.0D);
	private static final VoxelShape SHAPE_Z = box(0.0D, 0.0D, 4.32D, 16.0D, 16.0D, 11.68D);
	private static final VoxelShape SHAPE_X = box(4.32D, 0.0D, 0.0D, 11.68D, 16.0D, 16.0D);
	private final ForceFieldTier tier;

	public ForceFieldProjectorBlock(final ForceFieldTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(tier.getHardness(), tier.getBlastResistance())
			.sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any()
			.setValue(FACING, Direction.DOWN).setValue(ACTIVE, false)
			.setValue(DOUBLE_SIDED, false).setValue(SHAPE, ForceFieldShape.NONE));
	}

	public ForceFieldTier getTier() { return tier; }

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE, DOUBLE_SIDED, SHAPE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace())
			.setValue(DOUBLE_SIDED, CatalogVariantBlockItem.getVariant(context.getItemInHand()) == 1);
	}

	@Override
	public void setPlacedBy(final World world, final BlockPos blockPos, final BlockState blockState,
	                       @Nullable final LivingEntity placer, final ItemStack itemStack) {
		super.setPlacedBy(world, blockPos, blockState, placer, itemStack);
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof ForceFieldProjectorTileEntity) {
			((ForceFieldProjectorTileEntity) tileEntity).setDoubleSided(blockState.getValue(DOUBLE_SIDED));
		}
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		switch (blockState.getValue(FACING).getAxis()) {
		case X: return SHAPE_X;
		case Z: return SHAPE_Z;
		case Y:
		default: return SHAPE_Y;
		}
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world) {
		return new ForceFieldProjectorTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof ForceFieldProjectorTileEntity)) return ActionResultType.PASS;
		final ForceFieldProjectorTileEntity projector = (ForceFieldProjectorTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		final ForceFieldShape heldShape = getShapeItem(held);
		final ForceFieldUpgrade heldUpgrade = getUpgradeItem(held);
		final boolean shapeFace = hit.getDirection() == blockState.getValue(FACING)
			|| blockState.getValue(DOUBLE_SIDED)
			&& hit.getDirection() == blockState.getValue(FACING).getOpposite();

		if (player.isShiftKeyDown()) {
			if (heldUpgrade != ForceFieldUpgrade.NONE && projector.getUpgradeCount(heldUpgrade) > 0) {
				if (!world.isClientSide) {
					projector.removeUpgrade(heldUpgrade);
					giveUpgrade(player, blockPos, heldUpgrade);
				}
				return ActionResultType.sidedSuccess(world.isClientSide);
			}
			if (shapeFace && projector.getShape() != ForceFieldShape.NONE
			 && (held.isEmpty() || heldShape != ForceFieldShape.NONE)) {
				if (!world.isClientSide) {
					giveShape(player, blockPos, projector.getShape(), projector.isDoubleSided() ? 2 : 1);
					projector.setShape(ForceFieldShape.NONE);
				}
				return ActionResultType.sidedSuccess(world.isClientSide);
			}
			if (held.isEmpty()) {
				final ForceFieldUpgrade mounted = projector.getAnyUpgrade();
				if (mounted != null) {
					if (!world.isClientSide) {
						projector.removeUpgrade(mounted);
						giveUpgrade(player, blockPos, mounted);
					}
					return ActionResultType.sidedSuccess(world.isClientSide);
				}
			}
			return ActionResultType.PASS;
		}

		if (heldShape != ForceFieldShape.NONE) {
			if (!shapeFace) {
				if (!world.isClientSide) player.displayClientMessage(new TranslationTextComponent(
					"warpdrive.force_field.wrong_shape_side"), true);
				return ActionResultType.sidedSuccess(world.isClientSide);
			}
			final int required = projector.isDoubleSided() ? 2 : 1;
			if (!player.abilities.instabuild && held.getCount() < required) {
				if (!world.isClientSide) player.displayClientMessage(new TranslationTextComponent(
					"warpdrive.force_field.not_enough_shapes", required), true);
				return ActionResultType.sidedSuccess(world.isClientSide);
			}
			if (!world.isClientSide) {
				if (projector.getShape() != ForceFieldShape.NONE) {
					giveShape(player, blockPos, projector.getShape(), required);
				}
				if (!player.abilities.instabuild) held.shrink(required);
				projector.setShape(heldShape);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}

		if (heldUpgrade != ForceFieldUpgrade.NONE) {
			if (heldUpgrade.getProjectorLimit() <= 0
			 || projector.getUpgradeCount(heldUpgrade) >= heldUpgrade.getProjectorLimit()) {
				if (!world.isClientSide) player.displayClientMessage(new TranslationTextComponent(
					"warpdrive.force_field.invalid_projector_upgrade"), true);
				return ActionResultType.sidedSuccess(world.isClientSide);
			}
			if (!world.isClientSide && projector.addUpgrade(heldUpgrade)
			 && !player.abilities.instabuild) held.shrink(1);
			return ActionResultType.sidedSuccess(world.isClientSide);
		}

		if (held.isEmpty()) {
			if (!world.isClientSide) player.displayClientMessage(MachineStatusText.status(getName(),
				new TranslationTextComponent("warpdrive.force_field.projector.status",
					MachineStatusText.value(projector.getBeamFrequency()),
					MachineStatusText.value(projector.getShape().getSerializedName()),
					MachineStatusText.value(projector.getEnergyStored()),
					MachineStatusText.value(projector.getMaxEnergyStored()))), false);
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
			if (tileEntity instanceof ForceFieldProjectorTileEntity) {
				((ForceFieldProjectorTileEntity) tileEntity).destroyForceField();
			}
		}
		super.onRemove(oldState, world, blockPos, newState, isMoving);
	}

	private static ForceFieldShape getShapeItem(final ItemStack itemStack) {
		final ResourceLocation name = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
		if (name == null || !name.getNamespace().equals("warpdrive")
		 || !name.getPath().startsWith("force_field_shape-")) return ForceFieldShape.NONE;
		return ForceFieldShape.fromRegistrySuffix(name.getPath().substring("force_field_shape-".length()));
	}

	private static ForceFieldUpgrade getUpgradeItem(final ItemStack itemStack) {
		final ResourceLocation name = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
		if (name == null || !name.getNamespace().equals("warpdrive")
		 || !name.getPath().startsWith("force_field_upgrade-")) return ForceFieldUpgrade.NONE;
		return ForceFieldUpgrade.fromRegistrySuffix(name.getPath().substring("force_field_upgrade-".length()));
	}

	private static void giveShape(final PlayerEntity player, final BlockPos blockPos,
	                             final ForceFieldShape shape, final int count) {
		final ItemStack itemStack = new ItemStack(
			Registration.FORCE_FIELD_SHAPES.get(shape.getSerializedName()).get(), count);
		ForceFieldRelayBlock.giveOrDrop(player, blockPos, itemStack);
	}

	private static void giveUpgrade(final PlayerEntity player, final BlockPos blockPos,
	                               final ForceFieldUpgrade upgrade) {
		ForceFieldRelayBlock.giveOrDrop(player, blockPos, new ItemStack(
			Registration.FORCE_FIELD_UPGRADES.get(upgrade.getSerializedName()).get()));
	}
}
