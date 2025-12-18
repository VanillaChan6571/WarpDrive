package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatThermalDynamics implements IBlockTransformer {
	
	private static Class<?> classBlockTDBase;
	
	public static void register() {
		try {
			classBlockTDBase = Class.forName("cofh.thermaldynamics.block.BlockTDBase");
			
			WarpDriveConfig.registerBlockTransformer("ThermalDynamics", new CompatThermalDynamics());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockTDBase.isInstance(block);
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
	private static final int[] rotSide         = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private void rotateComponent(final NBTTagCompound nbtTileEntity, final byte rotationSteps, final String nameComponents) {
		if (nbtTileEntity.hasKey(nameComponents)) {
			final NBTTagList nbtOldComponents = nbtTileEntity.getTagList(nameComponents, Constants.NBT.TAG_COMPOUND);
			final NBTTagList nbtNewComponents = new NBTTagList();
			for (int index = 0; index < nbtOldComponents.tagCount(); index++) {
				final NBTTagCompound nbtOldComponent = nbtOldComponents.getCompoundTagAt(index);
				final NBTTagCompound nbtNewComponent = nbtOldComponent.copy();
				final int side = nbtOldComponent.getInteger("side");
				switch (rotationSteps) {
				case 1:
					nbtNewComponent.setInteger("side", rotSide[side]);
					break;
				case 2:
					nbtNewComponent.setInteger("side", rotSide[rotSide[side]]);
					break;
				case 3:
					nbtNewComponent.setInteger("side", rotSide[rotSide[rotSide[side]]]);
					break;
				default:
					// nbtNewComponent.setInteger("side", side);
					break;
				}
				nbtNewComponents.appendTag(nbtNewComponent);
			}
			nbtTileEntity.setTag(nameComponents, nbtNewComponents);
		}
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// Ducts attachments (servos)
		rotateComponent(nbtTileEntity, rotationSteps, "Attachments");
		
		// Ducts covers (facades)
		rotateComponent(nbtTileEntity, rotationSteps, "Covers");
		
		// Ducts connections
		if (nbtTileEntity.hasKey("Connections")) {
			final byte[] bytesOldConnections = nbtTileEntity.getByteArray("Connections");
			final byte[] bytesNewConnections = bytesOldConnections.clone();
			for (int sideOld = 0; sideOld < 6; sideOld++) {
				final byte byteConnection = bytesOldConnections[sideOld];
				switch (rotationSteps) {
				case 1:
					bytesNewConnections[rotSide[sideOld]] = byteConnection;
					break;
				case 2:
					bytesNewConnections[rotSide[rotSide[sideOld]]] = byteConnection;
					break;
				case 3:
					bytesNewConnections[rotSide[rotSide[rotSide[sideOld]]]] = byteConnection;
					break;
				default:
					// bytesNewConnections[sideOld] = byteConnection;
					break;
				}
			}
			nbtTileEntity.setByteArray("Connections", bytesNewConnections);
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
