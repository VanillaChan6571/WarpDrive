package cr0s.warpdrive.block.energy;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A charged laser medium. The eight block states are the original 0-7 charge indicator and use
 * the surviving animated side textures from 1.12.2.
 */
public class LaserMediumBlock extends Block {

	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);

	private final LaserMediumTier tier;

	public LaserMediumBlock(final LaserMediumTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any().setValue(LEVEL, 0));
	}

	public LaserMediumTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(LEVEL);
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new LaserMediumTileEntity(tier);
	}

}
