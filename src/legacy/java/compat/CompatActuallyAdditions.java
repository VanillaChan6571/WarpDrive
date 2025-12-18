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

public class CompatActuallyAdditions implements IBlockTransformer {
	
	private static Class<?> classBlockInputter;
	
	public static void register() {
		try {
			classBlockInputter = Class.forName("de.ellpeck.actuallyadditions.mod.blocks.BlockInputter");
			
			WarpDriveConfig.registerBlockTransformer("ActuallyAdditions", new CompatActuallyAdditions());
		} catch(final ClassNotFoundException | RuntimeException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockInputter.isInstance(block);
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
	
	/*
	inputter & advanced inputter
		SideToPull -1 / 0 / 1 / 2 3 4 5
		SideToPut  -1 / 0 / 1 / 2 3 4 5
	 */
	
	//                                                   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] rotInputter         = {  0,  1,  3,  4,  5,  2 }; // -1 / 0 / 1 / 2 3 4 5
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// inputter
		if (nbtTileEntity.hasKey("SideToPull")) {
			final int side = nbtTileEntity.getInteger("SideToPull");
			if (side > 1) {
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("SideToPull", rotInputter[side]);
					break;
				case 2:
					nbtTileEntity.setInteger("SideToPull", rotInputter[rotInputter[side]]);
					break;
				case 3:
					nbtTileEntity.setInteger("SideToPull", rotInputter[rotInputter[rotInputter[side]]]);
					break;
				default:
					break;
				}
			}
		}
		if (nbtTileEntity.hasKey("SideToPut")) {
			final int side = nbtTileEntity.getInteger("SideToPut");
			if (side > 1) {
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("SideToPut", rotInputter[side]);
					break;
				case 2:
					nbtTileEntity.setInteger("SideToPut", rotInputter[rotInputter[side]]);
					break;
				case 3:
					nbtTileEntity.setInteger("SideToPut", rotInputter[rotInputter[rotInputter[side]]]);
					break;
				default:
					break;
				}
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
