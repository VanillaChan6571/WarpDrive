package cr0s.warpdrive.block.atomic;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;

/** Accelerator part whose running texture is controlled by the accelerator core. */
public class AcceleratorComponentBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public AcceleratorComponentBlock() {
		this(1);
	}

	protected AcceleratorComponentBlock(final int tierIndex) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(4.0F + tierIndex, (float) ((2 + 2 * tierIndex) * 5 / 3))
			.sound(SoundType.METAL).requiresCorrectToolForDrops());
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}
}
