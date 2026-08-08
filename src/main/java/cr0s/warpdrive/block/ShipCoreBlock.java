package cr0s.warpdrive.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Ship Core Block - The main control block for WarpDrive ships
 *
 * Features:
 * - Acts as the center point for ship structure scanning
 * - Stores energy for warp jumps
 * - Interfaces with ComputerCraft for control
 * - Must be present on every ship
 */
public class ShipCoreBlock extends HorizontalBlock {

	/** Matches the two render states used by the 1.12.2 Ship Core model. */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	@Nullable private final ShipCoreTier tier;

	public ShipCoreBlock() {
		this(null);
	}

	public ShipCoreBlock(@Nullable final ShipCoreTier tier) {
		super(Properties.of(Material.METAL)
				.strength(5.0f, 6.0f)
				.requiresCorrectToolForDrops()
				.harvestTool(ToolType.PICKAXE)
				.harvestLevel(2)
		);
		this.tier = tier;
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			// ShipCoreTileEntity starts enabled, matching the 1.12.2 machine default. Keeping the
			// placement state consistent avoids a same-tick state mutation while its tile entity is
			// still in World's pendingBlockEntities queue.
			.setValue(ACTIVE, true));
	}

	/** Null identifies the permissive untiered compatibility core. */
	@Nullable
	public ShipCoreTier getTier() { return tier; }

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		return defaultBlockState()
			// FACING is the ship's bow direction: the arrow should point where the player is
			// looking, not back toward the player like a furnace front.
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(ACTIVE, true);
	}

	@Override
	public void setPlacedBy(@Nonnull final World world, @Nonnull final BlockPos pos,
	                        @Nonnull final BlockState state, @Nullable final LivingEntity placer,
	                        @Nonnull final ItemStack stack) {
		super.setPlacedBy(world, pos, state, placer, stack);
		final TileEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof ShipCoreTileEntity) {
			((ShipCoreTileEntity) tileEntity).setFacingFromBlock(state.getValue(FACING));
		}
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new ShipCoreTileEntity();
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos,
	                             PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		// Only process main hand to prevent double-toggling
		if (player.isShiftKeyDown() && hand == Hand.MAIN_HAND) {
			TileEntity te = world.getBlockEntity(pos);
			if (te instanceof ShipCoreTileEntity) {
				ShipCoreTileEntity shipCore = (ShipCoreTileEntity) te;
				shipCore.toggleBoundingBoxDisplay(player);
				return ActionResultType.SUCCESS;
			}
		}
		return ActionResultType.PASS;
	}

	@Override
	public BlockState rotate(@Nonnull final BlockState state, @Nonnull final Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(@Nonnull final BlockState state, @Nonnull final Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}
}
