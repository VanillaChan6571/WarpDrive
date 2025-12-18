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

public class CompatIronChest implements IBlockTransformer {
	
	private static Class<?> classBlockIronChest;
	private static Class<?> classBlockIronShulkerBox;
	
	public static void register() {
		try {
			classBlockIronChest = Class.forName("cpw.mods.ironchest.common.blocks.chest.BlockIronChest");
			classBlockIronShulkerBox = Class.forName("cpw.mods.ironchest.common.blocks.shulker.BlockIronShulkerBox");
			
			WarpDriveConfig.registerBlockTransformer("IronChests", new CompatIronChest());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockIronChest.isInstance(block)
		    || classBlockIronShulkerBox .isInstance(block);
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
	
	//                                              0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] rotFacing      = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// Iron chests and shulker boxes
		if (nbtTileEntity.hasKey("facing")) {
			final byte facing = nbtTileEntity.getByte("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("facing", rotFacing[facing]);
				break;
			case 2:
				nbtTileEntity.setByte("facing", rotFacing[rotFacing[facing]]);
				break;
			case 3:
				nbtTileEntity.setByte("facing", rotFacing[rotFacing[rotFacing[facing]]]);
				break;
			default:
				break;
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
