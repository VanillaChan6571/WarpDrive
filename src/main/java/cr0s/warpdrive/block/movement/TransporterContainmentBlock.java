package cr0s.warpdrive.block.movement;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;

/** Half-height containment floor used by a transporter scanner pad. */
public class TransporterContainmentBlock extends Block {

	protected static final VoxelShape HALF_HEIGHT = box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

	public TransporterContainmentBlock() {
		this(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL)
			.requiresCorrectToolForDrops().noOcclusion());
	}

	protected TransporterContainmentBlock(final AbstractBlock.Properties properties) {
		super(properties);
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                           @Nonnull final BlockPos blockPos,
	                           @Nonnull final ISelectionContext context) {
		return HALF_HEIGHT;
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(@Nonnull final BlockState blockState,
	                                    @Nonnull final IBlockReader world,
	                                    @Nonnull final BlockPos blockPos,
	                                    @Nonnull final ISelectionContext context) {
		return HALF_HEIGHT;
	}
}
