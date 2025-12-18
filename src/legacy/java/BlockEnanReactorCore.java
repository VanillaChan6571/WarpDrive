package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.render.TileEntityEnanReactorCoreRenderer;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockEnanReactorCore extends BlockAbstractContainer {
	
	public static final PropertyInteger ENERGY = PropertyInteger.create("energy", 0, 3);
	public static final PropertyInteger INSTABILITY = PropertyInteger.create("stability", 0, 3);
	
	public BlockEnanReactorCore(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.energy.enan_reactor_core." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(ENERGY, 0)
				                .withProperty(INSTABILITY, 0)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, ENERGY, INSTABILITY);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(ENERGY, metadata & 0x3)
				       .withProperty(INSTABILITY, metadata >> 2);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(ENERGY) + (blockState.getValue(INSTABILITY) << 2);
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityEnanReactorCore();
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
		
		if (enumTier != EnumTier.BASIC) {
			// Bind our TESR to our tile entity
			ClientRegistry.bindTileEntitySpecialRenderer(TileEntityEnanReactorCore.class, new TileEntityEnanReactorCoreRenderer());
		}
	}
}