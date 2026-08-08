package cr0s.warpdrive.block.movement;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

/** Illuminated center/checker tiles of a 3x3 transporter pad. */
public class TransporterScannerBlock extends TransporterContainmentBlock {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public TransporterScannerBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL)
			.requiresCorrectToolForDrops().noOcclusion()
			.lightLevel(state -> state.getValue(ACTIVE) ? 13 : 0));
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	/**
	 * Validates the legacy alternating 3x3 scanner/containment pad and its two-block headroom.
	 * Returns the four containment positions, or {@code null} when the pad is invalid.
	 */
	@Nullable
	public Collection<BlockPos> getValidContainment(final World world, final BlockPos scannerPos) {
		final ArrayList<BlockPos> containment = new ArrayList<>(4);
		boolean scannerPosition = true;
		for (int x = scannerPos.getX() - 1; x <= scannerPos.getX() + 1; x++) {
			for (int z = scannerPos.getZ() - 1; z <= scannerPos.getZ() + 1; z++) {
				final BlockPos basePos = new BlockPos(x, scannerPos.getY(), z);
				final Block base = world.getBlockState(basePos).getBlock();
				if (!(base instanceof TransporterContainmentBlock)
				 || (scannerPosition && !(base instanceof TransporterScannerBlock))
				 || (!scannerPosition && base instanceof TransporterScannerBlock)) {
					return null;
				}
				scannerPosition = !scannerPosition;
				if (!world.isEmptyBlock(basePos.above()) || !world.isEmptyBlock(basePos.above(2))) {
					return null;
				}
				if (!(base instanceof TransporterScannerBlock)) containment.add(basePos);
			}
		}
		return containment;
	}
}
