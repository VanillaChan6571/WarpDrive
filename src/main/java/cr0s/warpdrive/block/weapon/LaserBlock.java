package cr0s.warpdrive.block.weapon;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Ship-mounted laser cannon. Configuration and firing live on its tile entity. */
public class LaserBlock extends Block {

	public LaserBlock() {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(50.0F, 100.0F / 3.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops());
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new LaserTileEntity();
	}
}
