package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatSGCraft implements IBlockTransformer {
	
	private static Class<?> classBaseTileEntity;
	private static Class<?> classDHDBlock;
	private static Class<?> classSGBaseBlock;
	private static Class<?> classSGBaseTE;
	private static Method methodSGBaseTE_sgStateDescription;
	
	public static void register() {
		try {
			classBaseTileEntity = Class.forName("gcewing.sg.BaseTileEntity");
			classDHDBlock = Class.forName("gcewing.sg.block.DHDBlock");
			classSGBaseBlock = Class.forName("gcewing.sg.block.SGBaseBlock");
			classSGBaseTE = Class.forName("gcewing.sg.tileentity.SGBaseTE");
			methodSGBaseTE_sgStateDescription = classSGBaseTE.getMethod("sgStateDescription");
			
			WarpDriveConfig.registerBlockTransformer("SGCraft", new CompatSGCraft());
		} catch(final ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBaseTileEntity.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		if (classSGBaseTE.isInstance(tileEntity)) {
			try {
				final Object object = methodSGBaseTE_sgStateDescription.invoke(tileEntity);
				final String state = (String)object;
				if (!state.equalsIgnoreCase("Idle")) {
					reason.append(Commons.getStyleWarning(), "warpdrive.compat.guide.stargate_is_active", state);
					return false;
				}
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
				exception.printStackTrace();
			}
		}
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
	
	//                                                        0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] mrotSGBase               = {  3,  2,  0,  1,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  rotFacingDirectionOfBase = {  3,  0,  1,  2,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		// Link between stargate controller and DHD
		if (nbtTileEntity.hasKey("isLinkedToStargate")) {
			if ( nbtTileEntity.getBoolean("isLinkedToStargate")
			  && nbtTileEntity.hasKey("linkedX") && nbtTileEntity.hasKey("linkedY") && nbtTileEntity.hasKey("linkedZ") ) {
				if (transformation.isInside(nbtTileEntity.getInteger("linkedX"), nbtTileEntity.getInteger("linkedY"), nbtTileEntity.getInteger("linkedZ"))) {
					final BlockPos targetLink = transformation.apply(nbtTileEntity.getInteger("linkedX"), nbtTileEntity.getInteger("linkedY"), nbtTileEntity.getInteger("linkedZ"));
					nbtTileEntity.setInteger("linkedX", targetLink.getX());
					nbtTileEntity.setInteger("linkedY", targetLink.getY());
					nbtTileEntity.setInteger("linkedZ", targetLink.getZ());
				} else {
					nbtTileEntity.setBoolean("isLinkedToController", false);
					nbtTileEntity.setInteger("linkedX", 0);
					nbtTileEntity.setInteger("linkedY", 0);
					nbtTileEntity.setInteger("linkedZ", 0);
				}
			}
		}
		if (nbtTileEntity.hasKey("isLinkedToController")) {
			if ( nbtTileEntity.getBoolean("isLinkedToController")
			  && nbtTileEntity.hasKey("linkedX") && nbtTileEntity.hasKey("linkedY") && nbtTileEntity.hasKey("linkedZ") ) {
				if (transformation.isInside(nbtTileEntity.getInteger("linkedX"), nbtTileEntity.getInteger("linkedY"), nbtTileEntity.getInteger("linkedZ"))) {
					final BlockPos targetLink = transformation.apply(nbtTileEntity.getInteger("linkedX"), nbtTileEntity.getInteger("linkedY"), nbtTileEntity.getInteger("linkedZ"));
					nbtTileEntity.setInteger("linkedX", targetLink.getX());
					nbtTileEntity.setInteger("linkedY", targetLink.getY());
					nbtTileEntity.setInteger("linkedZ", targetLink.getZ());
				} else {
					nbtTileEntity.setBoolean("isLinkedToController", false);
					nbtTileEntity.setInteger("linkedX", 0);
					nbtTileEntity.setInteger("linkedY", 0);
					nbtTileEntity.setInteger("linkedZ", 0);
				}
			}
		}
		
		// Reference of ring blocks to the controller block
		if (nbtTileEntity.hasKey("isMerged")) {
			if ( nbtTileEntity.getBoolean("isMerged")
			  && nbtTileEntity.hasKey("baseX") && nbtTileEntity.hasKey("baseY") && nbtTileEntity.hasKey("baseZ")) {
				final BlockPos targetLink = transformation.apply(nbtTileEntity.getInteger("baseX"), nbtTileEntity.getInteger("baseY"), nbtTileEntity.getInteger("baseZ"));
				nbtTileEntity.setInteger("baseX", targetLink.getX());
				nbtTileEntity.setInteger("baseY", targetLink.getY());
				nbtTileEntity.setInteger("baseZ", targetLink.getZ());
			}
		}
		
		// Ring renderer orientation
		if (nbtTileEntity.hasKey("facingDirectionOfBase")) {
			final int facing = nbtTileEntity.getByte("facingDirectionOfBase");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("facingDirectionOfBase", rotFacingDirectionOfBase[facing]);
				break;
			case 2:
				nbtTileEntity.setInteger("facingDirectionOfBase", rotFacingDirectionOfBase[rotFacingDirectionOfBase[facing]]);
				break;
			case 3:
				nbtTileEntity.setInteger("facingDirectionOfBase", rotFacingDirectionOfBase[rotFacingDirectionOfBase[rotFacingDirectionOfBase[facing]]]);
				break;
			default:
				break;
			}
		}
		
		if ( classDHDBlock.isInstance(block)
		  || classSGBaseBlock.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrotSGBase[metadata];
			case 2:
				return mrotSGBase[mrotSGBase[metadata]];
			case 3:
				return mrotSGBase[mrotSGBase[mrotSGBase[metadata]]];
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
