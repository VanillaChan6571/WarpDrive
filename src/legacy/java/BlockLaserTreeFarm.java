package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.block.BlockAbstractContainer;

import cr0s.warpdrive.data.EnumLaserTreeFarmMode;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockLaserTreeFarm extends BlockAbstractContainer {
	
	public static final PropertyEnum<EnumLaserTreeFarmMode> MODE = PropertyEnum.create("mode", EnumLaserTreeFarmMode.class);
	
	public BlockLaserTreeFarm(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.collection.laser_tree_farm");

		setDefaultState(getDefaultState()
				                .withProperty(MODE, EnumLaserTreeFarmMode.INACTIVE)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, MODE);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				.withProperty(MODE, EnumLaserTreeFarmMode.get(metadata));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(MODE).ordinal();
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityLaserTreeFarm();
	}
}