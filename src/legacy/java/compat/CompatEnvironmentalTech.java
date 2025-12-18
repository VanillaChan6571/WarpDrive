package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatEnvironmentalTech implements IBlockTransformer {
	
	private static Class<?> classETBlockSlave;
	
	public static void register() {
		try {
			classETBlockSlave = Class.forName("com.valkyrieofnight.et.m_multiblocks.block.ETBlockSlave");
			
			WarpDriveConfig.registerBlockTransformer("Environmental Tech", new CompatEnvironmentalTech());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classETBlockSlave.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	// (no rotation, only offset to controller)
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		if ( nbtTileEntity.hasKey("has_controller")
		  && nbtTileEntity.hasKey("cx")
		  && nbtTileEntity.hasKey("cy")
		  && nbtTileEntity.hasKey("cz") ) {
			final boolean hasController = nbtTileEntity.getBoolean("has_controller");
			if (hasController) {
				final BlockPos target = transformation.apply(nbtTileEntity.getInteger("cx"), nbtTileEntity.getInteger("cy"), nbtTileEntity.getInteger("cz"));
				nbtTileEntity.setInteger("cx", target.getX());
				nbtTileEntity.setInteger("cy", target.getY());
				nbtTileEntity.setInteger("cz", target.getZ());
			}
		}
		
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
