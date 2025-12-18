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

public class CompatStorageDrawers implements IBlockTransformer {
	
	private static Class<?> classBlockController;    // controller = metadata rotation              com.jaquadro.minecraft.storagedrawers.block.BlockController
	private static Class<?> classBlockDrawers;       // basic/custom/compacting drawers = byte Dir  com.jaquadro.minecraft.storagedrawers.block.BlockDrawers
	private static Class<?> classBlockFramingTable;  // framing table = metadata rotation + side    com.jaquadro.minecraft.storagedrawers.block.BlockFramingTable
	
	public static void register() {
		try {
			classBlockController = Class.forName("com.jaquadro.minecraft.storagedrawers.block.BlockController");
			classBlockDrawers = Class.forName("com.jaquadro.minecraft.storagedrawers.block.BlockDrawers");
			classBlockFramingTable = Class.forName("com.jaquadro.minecraft.storagedrawers.block.BlockFramingTable");
			
			WarpDriveConfig.registerBlockTransformer("StorageDrawers", new CompatStorageDrawers());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockController.isInstance(block)
		    || classBlockDrawers.isInstance(block)
		    || classBlockFramingTable.isInstance(block);
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
	
	//                                               0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] rotFacing       = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] rotFramingTable = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// controller
		if (classBlockController.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotFacing[metadata];
			case 2:
				return rotFacing[rotFacing[metadata]];
			case 3:
				return rotFacing[rotFacing[rotFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// basic/custom/compacting drawers
		if (nbtTileEntity.hasKey("Dir")) {
			final byte facing = nbtTileEntity.getByte("Dir");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("Dir", rotFacing[facing]);
				break;
			case 2:
				nbtTileEntity.setByte("Dir", rotFacing[rotFacing[facing]]);
				break;
			case 3:
				nbtTileEntity.setByte("Dir", rotFacing[rotFacing[rotFacing[facing]]]);
				break;
			default:
				break;
			}
			return metadata;
		}
		
		// framing table
		if (classBlockFramingTable.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotFramingTable[metadata];
			case 2:
				return rotFramingTable[rotFramingTable[metadata]];
			case 3:
				return rotFramingTable[rotFramingTable[rotFramingTable[metadata]]];
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
