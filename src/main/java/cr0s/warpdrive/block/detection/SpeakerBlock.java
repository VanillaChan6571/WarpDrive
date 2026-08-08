package cr0s.warpdrive.block.detection;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Computer-controlled local chat speaker. */
public class SpeakerBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	private final SpeakerTier tier;

	public SpeakerBlock(final SpeakerTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.DOWN));
	}

	public SpeakerTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		Direction facing = context.getNearestLookingDirection().getOpposite();
		final PlayerEntity player = context.getPlayer();
		if (player != null && player.isShiftKeyDown()) {
			facing = facing.getOpposite();
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new SpeakerTileEntity();
	}
}
