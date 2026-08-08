package cr0s.warpdrive.block.atomic;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Tunable accelerator control point; injectors use the same block behavior with a distinct tile. */
public class AcceleratorControlPointBlock extends AcceleratorComponentBlock {

	private final boolean injector;

	public AcceleratorControlPointBlock(final boolean injector) {
		super(1);
		this.injector = injector;
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return injector ? new ParticlesInjectorTileEntity() : new AcceleratorControlPointTileEntity();
	}
}
