package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatPneumaticCraft implements IBlockTransformer {
	
	private static Class<?> classBlockPneumaticCraft;
	private static Method   methodBlockPneumaticCraft_isRotatable; // many blocks are rotatable, many are not => it's more efficient to read the property to differentiate them
	
	private static Class<?> classBlockPneumaticDoor;
	private static Class<?> classBlockPressureChamberWall;
	private static Class<?> classBlockPressureChamberValve;
	
	public static void register() {
		try {
			classBlockPneumaticCraft = Class.forName("me.desht.pneumaticcraft.common.block.BlockPneumaticCraft");
			methodBlockPneumaticCraft_isRotatable = classBlockPneumaticCraft.getMethod("isRotatable");
			
			classBlockPneumaticDoor = Class.forName("me.desht.pneumaticcraft.common.block.BlockPneumaticDoor");
			classBlockPressureChamberWall = Class.forName("me.desht.pneumaticcraft.common.block.BlockPressureChamberWall");
			classBlockPressureChamberValve = Class.forName("me.desht.pneumaticcraft.common.block.BlockPressureChamberValve");
			
			WarpDriveConfig.registerBlockTransformer("pneumaticcraft", new CompatPneumaticCraft());
		} catch(final ClassNotFoundException | NoSuchMethodException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockPneumaticCraft.isInstance(block);
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
	
	//                                                     0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] mrotFacing            = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 }; // Chamber interface & Omnidirectional hopper
	private static final byte[] mrotChamberWall       = {  0,  1,  3,  2,  4,  8,  5,  6,  7,  9, 10, 11, 12, 13, 14, 15 }; // Chamber wall
	private static final byte[] mrotChamberValve      = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 }; // Chamber valve
	private static final byte[] mrotPneumaticDoor     = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 }; // Pressure door
	private static final byte[] mrotTextRotation      = {  1,  2,  3,  0 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if ( rotationSteps == 0
		  && !nbtTileEntity.hasKey("valveX")
		  && !nbtTileEntity.hasKey("multiBlockX")) {
			return metadata;
		}
		
		// Aphorism signs
		// @todo the sign has no text after ship movement in single player until chunk is reloaded?
		if (nbtTileEntity.hasKey("textRot")) {
			if (metadata == 0 || metadata == 1) {// sign is horizontal, only the text needs to be rotated
				final int textRotation = nbtTileEntity.getInteger("textRot");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("textRot", mrotTextRotation[textRotation]);
					return metadata;
				case 2:
					nbtTileEntity.setInteger("textRot", mrotTextRotation[mrotTextRotation[textRotation]]);
					return metadata;
				case 3:
					nbtTileEntity.setInteger("textRot", mrotTextRotation[mrotTextRotation[mrotTextRotation[textRotation]]]);
					return metadata;
				default:
					return metadata;
				}
			} else {// sign is vertical, only the block itself is rotating
				switch (rotationSteps) {
				case 1:
					return mrotFacing[metadata];
				case 2:
					return mrotFacing[mrotFacing[metadata]];
				case 3:
					return mrotFacing[mrotFacing[mrotFacing[metadata]]];
				default:
					return metadata;
				}
			}
		}
		
		// Omnidirectional hoppers
		if (nbtTileEntity.hasKey("inputDir")) {
			final int inputDir = nbtTileEntity.getInteger("inputDir");
			final int outputDir = nbtTileEntity.getInteger("outputDir");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("inputDir", mrotFacing[inputDir]);
				nbtTileEntity.setInteger("outputDir", mrotFacing[outputDir]);
				return mrotFacing[metadata];
			case 2:
				nbtTileEntity.setInteger("inputDir", mrotFacing[mrotFacing[inputDir]]);
				nbtTileEntity.setInteger("outputDir", mrotFacing[mrotFacing[outputDir]]);
				return mrotFacing[mrotFacing[metadata]];
			case 3:
				nbtTileEntity.setInteger("inputDir", mrotFacing[mrotFacing[mrotFacing[inputDir]]]);
				nbtTileEntity.setInteger("outputDir", mrotFacing[mrotFacing[mrotFacing[outputDir]]]);
				return mrotFacing[mrotFacing[mrotFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Pneumatic door is facing + top/down on modulo (6 or 8 ?)
		if (classBlockPneumaticDoor.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotPneumaticDoor[metadata];
			case 2:
				return mrotPneumaticDoor[mrotPneumaticDoor[metadata]];
			case 3:
				return mrotPneumaticDoor[mrotPneumaticDoor[mrotPneumaticDoor[metadata]]];
			default:
				return metadata;
			}
		}
		
		// pressure chamber blocks (wall, glass, valve, interface)
		if (nbtTileEntity.hasKey("valveX")) {
			final BlockPos target = transformation.apply(
				nbtTileEntity.getInteger("valveX"),
				nbtTileEntity.getInteger("valveY"),
				nbtTileEntity.getInteger("valveZ"));
			nbtTileEntity.setInteger("valveX", target.getX());
			nbtTileEntity.setInteger("valveY", target.getY());
			nbtTileEntity.setInteger("valveZ", target.getZ());
			// use default metadata rotation
		}
		
		// pressure chamber valve
		if (nbtTileEntity.hasKey("multiBlockX")) {
			// multiBlockXYZ only makes sense when size is non null, even if they are part of the multiblock (yes, it's weird)
			final int multiBlockSize = nbtTileEntity.getInteger("multiBlockSize");
			if (multiBlockSize != 0) {
				final BlockPos sourceMin = new BlockPos(
						nbtTileEntity.getInteger("multiBlockX"),
						nbtTileEntity.getInteger("multiBlockY"),
						nbtTileEntity.getInteger("multiBlockZ"));
				final BlockPos sourceMax = new BlockPos(
						sourceMin.getX() + multiBlockSize - 1,
						sourceMin.getY() + multiBlockSize - 1,
						sourceMin.getZ() + multiBlockSize - 1);
				final BlockPos target1 = transformation.apply(sourceMin);
				final BlockPos target2 = transformation.apply(sourceMax);
				nbtTileEntity.setInteger("multiBlockX", Math.min(target1.getX(), target2.getX()));
				nbtTileEntity.setInteger("multiBlockY", Math.min(target1.getY(), target2.getY()));
				nbtTileEntity.setInteger("multiBlockZ", Math.min(target1.getZ(), target2.getZ()));
			}
			
			// Valves coordinates to each valves
			final NBTTagList tagListOld = nbtTileEntity.getTagList("Valves", 10);
			final NBTTagList tagListNew = new NBTTagList();
			for (int index = 0; index < tagListOld.tagCount(); index++) {
				final NBTTagCompound tagCompound = tagListOld.getCompoundTagAt(index);
				if (tagCompound != null) {
					final BlockPos coordinates = transformation.apply(
						tagCompound.getInteger("xCoord"),
						tagCompound.getInteger("yCoord"),
						tagCompound.getInteger("zCoord"));
					tagCompound.setInteger("xCoord", coordinates.getX());
					tagCompound.setInteger("yCoord", coordinates.getY());
					tagCompound.setInteger("zCoord", coordinates.getZ());
					tagListNew.appendTag(tagCompound);
				}
			}
			nbtTileEntity.setTag("Valves", tagListNew);
			// use default metadata rotation
		}
		
		// elevator base, pipe
		if (nbtTileEntity.hasKey("sideConnected0")) {
			final byte[] connectedOldSides = new byte[6];
			for (int side = 2; side < 6; side++) {
				connectedOldSides[side] = nbtTileEntity.getByte("sideConnected" + side);
			}
			for (int side = 2; side < 6; side++) {
			final byte connected = connectedOldSides[side];
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("sideConnected" + mrotFacing[side], connected);
					break;
				case 2:
					nbtTileEntity.setByte("sideConnected" + mrotFacing[mrotFacing[side]], connected);
					break;
				case 3:
					nbtTileEntity.setByte("sideConnected" + mrotFacing[mrotFacing[mrotFacing[side]]], connected);
					break;
				default:
					break;
				}
			}
			if (nbtTileEntity.hasKey("sideClosed0")) {
				final byte[] closedOldSides = new byte[6];
				for (int side = 2; side < 6; side++) {
					closedOldSides[side] = nbtTileEntity.getByte("sideClosed" + side);
				}
				for (int side = 2; side < 6; side++) {
					final byte connected = closedOldSides[side];
					switch (rotationSteps) {
					case 1:
						nbtTileEntity.setByte("sideClosed" + mrotFacing[side], connected);
						break;
					case 2:
						nbtTileEntity.setByte("sideClosed" + mrotFacing[mrotFacing[side]], connected);
						break;
					case 3:
						nbtTileEntity.setByte("sideClosed" + mrotFacing[mrotFacing[mrotFacing[side]]], connected);
						break;
					default:
						break;
					}
				}
			}
		}
		
		
		// Pressure chamber wall has its own logic: NONE,  CENTER,  XEDGE,  ZEDGE,  YEDGE,  XMIN_YMIN_ZMIN,  XMIN_YMIN_ZMAX,  XMIN_YMAX_ZMIN,  XMIN_YMAX_ZMAX
		if (classBlockPressureChamberWall.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotChamberWall[metadata];
			case 2:
				return mrotChamberWall[mrotChamberWall[metadata]];
			case 3:
				return mrotChamberWall[mrotChamberWall[mrotChamberWall[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockPressureChamberValve.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotChamberValve[metadata];
			case 2:
				return mrotChamberValve[mrotChamberValve[metadata]];
			case 3:
				return mrotChamberValve[mrotChamberValve[mrotChamberValve[metadata]]];
			default:
				return metadata;
			}
		}
		
		// all other tile entities we need to check the Rotatable state
		// this includes many blocks like security station, programmer, pneumatic dynamo, charging station, air cannon, elevator caller, air compressor, etc.
		final boolean isRotatable;
		try {
			final Object object = methodBlockPneumaticCraft_isRotatable.invoke(block);
			if (object instanceof Boolean) {
				isRotatable = (Boolean) object;
			} else {
				WarpDrive.logger.error(String.format("Block %s has invalid non-Boolean return value to isRotatable: %s",
				                                     block.getRegistryName(), object));
				return metadata;
			}
		} catch (final IllegalAccessException | InvocationTargetException exception) {
			exception.printStackTrace();
			return metadata;
		}
		WarpDrive.logger.info(String.format("Block %s isRotatable %s",
		                                    block.getRegistryName(), isRotatable));
		if (isRotatable) {
			switch (rotationSteps) {
			case 1:
				return mrotFacing[metadata];
			case 2:
				return mrotFacing[mrotFacing[metadata]];
			case 3:
				return mrotFacing[mrotFacing[mrotFacing[metadata]]];
			default:
				return metadata;
			}
		} else {
			return metadata;
		}
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
