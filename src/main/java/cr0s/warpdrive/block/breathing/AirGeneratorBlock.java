package cr0s.warpdrive.block.breathing;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.IBlockReader;

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
