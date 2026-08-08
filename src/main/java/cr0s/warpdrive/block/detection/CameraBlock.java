package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Directional video camera and optional optical sensor. */
public class CameraBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public CameraBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
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

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new CameraTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public ActionResultType use(@Nonnull final BlockState blockState, @Nonnull final World world,
	                            @Nonnull final BlockPos blockPos, @Nonnull final PlayerEntity player,
	                            @Nonnull final Hand hand, @Nonnull final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (!(tileEntity instanceof CameraTileEntity)) return ActionResultType.PASS;
		final CameraTileEntity camera = (CameraTileEntity) tileEntity;
		final ItemStack held = player.getItemInHand(hand);
		final boolean recognitionCrystal = held.getItem() == Registration.COMPONENTS.get("diamond_crystal").get();

		if (player.isShiftKeyDown() && held.isEmpty() && camera.getRecognitionUpgrades() > 0) {
			if (!world.isClientSide && camera.removeRecognitionUpgrade() && !player.abilities.instabuild) {
				final ItemStack crystal = new ItemStack(Registration.COMPONENTS.get("diamond_crystal").get());
				if (!player.inventory.add(crystal)) popResource(world, blockPos, crystal);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (recognitionCrystal) {
			if (!world.isClientSide && camera.addRecognitionUpgrade() && !player.abilities.instabuild) {
				held.shrink(1);
			}
			return ActionResultType.sidedSuccess(world.isClientSide);
		}
		if (!held.isEmpty()) return ActionResultType.PASS;
		if (!world.isClientSide) {
			player.displayClientMessage(new TranslationTextComponent("warpdrive.camera.status",
				camera.getVideoChannel(), camera.getRecognitionUpgrades(),
				camera.getRecognitionUpgrades() * CameraTileEntity.RANGE_PER_UPGRADE,
				camera.getResultsCount()[0]), false);
		}
		return ActionResultType.sidedSuccess(world.isClientSide);
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
