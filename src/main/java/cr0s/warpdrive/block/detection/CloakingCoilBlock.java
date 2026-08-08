package cr0s.warpdrive.block.detection;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Stateful passive coil whose link role and animation are driven by the cloaking core. */
public class CloakingCoilBlock extends Block {

	public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty OUTER = BooleanProperty.create("outer");
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	public CloakingCoilBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any()
			.setValue(CONNECTED, false).setValue(ACTIVE, false)
			.setValue(OUTER, false).setValue(FACING, Direction.DOWN));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(CONNECTED, ACTIVE, OUTER, FACING);
	}

	/** Updates a coil without replacing it, preserving the legacy core-to-coil contract. */
	public static void setCoilState(final World world, final BlockPos blockPos,
	                                final boolean connected, final boolean active,
	                                final boolean outer, final Direction facing) {
		final BlockState current = world.getBlockState(blockPos);
		if (!(current.getBlock() instanceof CloakingCoilBlock)) return;
		final BlockState updated = current
			.setValue(CONNECTED, connected).setValue(ACTIVE, active).setValue(OUTER, outer)
			.setValue(FACING, facing == null ? current.getValue(FACING) : facing);
		if (updated != current) world.setBlock(blockPos, updated, 3);
	}
}
