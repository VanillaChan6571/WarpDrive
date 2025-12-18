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

public class CompatBiblioCraft implements IBlockTransformer {
	
	private static Class<?> classBiblioBlock;
	
	public static void register() {
		try {
			classBiblioBlock = Class.forName("jds.bibliocraft.blocks.BiblioBlock");
			
			WarpDriveConfig.registerBlockTransformer("BiblioCraft", new CompatBiblioCraft());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBiblioBlock.isInstance(block);
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
	
	private static final int[]  rotAngle       = {  1,  2,  3,  0 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// tile entity rotations
		if (nbtTileEntity.hasKey("angle")) {
			final int angle = nbtTileEntity.getInteger("angle");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("angle", rotAngle[angle]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("angle", rotAngle[rotAngle[angle]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("angle", rotAngle[rotAngle[rotAngle[angle]]]);
				return metadata;
			default:
				return metadata;
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
