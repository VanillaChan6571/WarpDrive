package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.api.IAirContainerItem;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Air generator, ported from 1.12.2 BlockAirGeneratorTiered.
 *
 * Pushes breathable air out of the face it points at. FACING is set from the direction the placer
 * is looking, so it can vent along any axis - ceilings and floors included, which matters for
 * compact ship layouts.
 */
public class AirGeneratorBlock extends Block {

	/** All six facings - generators vent along any axis, ceilings and floors included. */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/** Cost of refilling one air container, 1.12.2 BREATHING_ENERGY_PER_CANISTER. */
	private static final int ENERGY_PER_CANISTER = 200;

	private final AirGeneratorTier tier;

	public AirGeneratorBlock(final AirGeneratorTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.0F, 10.0F)
			.requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any()
			.setValue(FACING, Direction.NORTH)
			.setValue(ACTIVE, false));
	}

	public AirGeneratorTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		// vent away from the player, so it blows into the room being built
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
	}

	/**
	 * Right-click with an air tank or canister to refill it, as in 1.12.2
	 * BlockAirGeneratorTiered.onBlockActivated.
	 *
	 * Any tier of generator refills any container - the tier governs how far it can pressurise a
	 * ship, not what it can fill. The cost comes out of the generator's own buffer, so a refill
	 * competes with keeping the air up.
	 */
	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		// off-hand clicks fall through, so the main hand always decides the interaction
		final ItemStack itemStackHeld = player.getItemInHand(hand);
		if ( hand != Hand.MAIN_HAND
		  || !(itemStackHeld.getItem() instanceof IAirContainerItem) ) {
			return super.use(blockState, world, blockPos, player, hand, hit);
		}

		final IAirContainerItem airContainer = (IAirContainerItem) itemStackHeld.getItem();
		if (!airContainer.canContainAir(itemStackHeld)) {
			// already full: leave the click alone rather than charging for nothing
			return super.use(blockState, world, blockPos, player, hand, hit);
		}

		if (world.isClientSide) {
			// the client cannot see the generator's buffer, so it assumes success and swings
			return ActionResultType.SUCCESS;
		}

		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof AirGeneratorTileEntity)) {
			return super.use(blockState, world, blockPos, player, hand, hit);
		}
		final AirGeneratorTileEntity airGenerator = (AirGeneratorTileEntity) tileEntity;
		if (!airGenerator.consumeEnergy(ENERGY_PER_CANISTER, true)) {
			return ActionResultType.CONSUME;
		}

		final ItemStack itemStackSingle = itemStackHeld.copy();
		itemStackSingle.setCount(1);
		final ItemStack itemStackRefilled = airContainer.getFullAirContainer(itemStackSingle);
		if (itemStackRefilled == null || itemStackRefilled.isEmpty()) {
			return super.use(blockState, world, blockPos, player, hand, hit);
		}

		airGenerator.consumeEnergy(ENERGY_PER_CANISTER, false);
		itemStackHeld.shrink(1);
		// 1.12.2 pushed the refilled tank into the first free slot. Returning it to the hand instead
		// keeps it where the player was holding it, which for a stack-of-one tank is the same slot
		// nine times out of ten anyway.
		if (itemStackHeld.isEmpty()) {
			player.setItemInHand(hand, itemStackRefilled);
		} else if (!player.inventory.add(itemStackRefilled)) {
			player.drop(itemStackRefilled, false);
		}
		player.inventoryMenu.broadcastChanges();
		return ActionResultType.CONSUME;
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world) {
		return new AirGeneratorTileEntity();
	}
}
