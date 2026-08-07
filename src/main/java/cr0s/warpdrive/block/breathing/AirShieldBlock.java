package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.block.AbstractOmnipanelBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;

/**
 * A barrier that stops air but not people, ported from 1.12.2 BlockAirShield.
 *
 * The whole point is the asymmetry: no collision box at all, so anyone walks straight through,
 * while the air simulation classifies it as a full sealer. That makes an airlock or a hangar mouth
 * possible without a door - the bay stays pressurised while crew and ships pass through freely.
 *
 * Because sealing is decided from collision shape everywhere else, this block has to be named
 * explicitly in AirClassifier. A shape-derived answer would call it "leaks in every direction",
 * which is exactly backwards.
 *
 * Panel geometry comes from AbstractOmnipanelBlock, so shields join into flush sheets with each
 * other, with hull glass, and with any solid face.
 *
 * Deviation: 1.12.2 offered sixteen dye colours as metadata variants. This is a single colour.
 */
public class AirShieldBlock extends AbstractOmnipanelBlock {

	public AirShieldBlock() {
		super(AbstractBlock.Properties.of(Material.WOOL)
			.strength(0.5F)
			.noOcclusion()
			.noCollission()
			// Both are predicates in 1.16.5 rather than overridable methods. Suffocation would be
			// fatal for a block you are meant to walk through, and view blocking would black the
			// screen out as your head passed the plane.
			.isSuffocating((blockState, world, blockPos) -> false)
			.isViewBlocking((blockState, world, blockPos) -> false));
	}

	/** Nothing collides with it - that is the feature, not an oversight. */
	@Nonnull
	@Override
	public VoxelShape getCollisionShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                                    @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		return VoxelShapes.empty();
	}
}
