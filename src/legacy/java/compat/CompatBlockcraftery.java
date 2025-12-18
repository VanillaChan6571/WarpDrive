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

public class CompatBlockcraftery implements IBlockTransformer {
	
	private static Class<?> classBlockCornerBase;
	private static Class<?> classBlockSlantBase;
	
	public static void register() {
		try {
			classBlockCornerBase = Class.forName("epicsquid.mysticallib.block.BlockCornerBase"); // 0-3 direction | 4 up => 0 1 2 3
			classBlockSlantBase = Class.forName("epicsquid.mysticallib.block.BlockSlantBase"); // 0-3 direction | 4 5 6 vertical
			
			WarpDriveConfig.registerBlockTransformer("Blockcraftery", new CompatBlockcraftery());
		} catch(final ClassNotFoundException | RuntimeException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockCornerBase.isInstance(block)
		    || classBlockSlantBase.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z, final Block block, final int blockMeta, final TileEntity tileEntity) {
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	//                                                   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] rotCorner           = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 }; // 0 1 2 3 / 4 5 6 7
	private static final byte[] rotSlope            = {  3,  2,  0,  1,  5,  6,  7,  4, 11, 10,  8,  9, 12, 13, 14, 15 }; // 0 3 1 2 / 4 5 6 7 / 8 11 9 10
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// corner
		if (classBlockCornerBase.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotCorner[metadata];
			case 2:
				return rotCorner[rotCorner[metadata]];
			case 3:
				return rotCorner[rotCorner[rotCorner[metadata]]];
			default:
				return metadata;
			}
		}
		
		// slope
		if (classBlockSlantBase.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotSlope[metadata];
			case 2:
				return rotSlope[rotSlope[metadata]];
			case 3:
				return rotSlope[rotSlope[rotSlope[metadata]]];
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
