package cr0s.warpdrive.block.weapon;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Combined laser cannon and video camera. */
public class LaserCameraBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public LaserCameraBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(50.0F, 100.0F / 3.0F)
			.sound(SoundType.METAL).noOcclusion()
			.isSuffocating((state, world, pos) -> false)
			.isViewBlocking((state, world, pos) -> false)
			.requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context) {
		return defaultBlockState().setValue(FACING,
			context.getNearestLookingDirection().getOpposite());
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new LaserCameraTileEntity();
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState rotate(final BlockState blockState, final Rotation rotation) {
		return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(final BlockState blockState, final Mirror mirror) {
		return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
	}
}
