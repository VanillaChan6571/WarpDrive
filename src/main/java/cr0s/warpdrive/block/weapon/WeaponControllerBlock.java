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

/** Dedicated ComputerCraft host for the bundled weapon-control program. */
public class WeaponControllerBlock extends Block {

	public WeaponControllerBlock() {
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
		return new WeaponControllerTileEntity();
	}
}
