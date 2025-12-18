package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.block.breathing.BlockAirFlow;
import cr0s.warpdrive.block.breathing.BlockAirSource;
import cr0s.warpdrive.block.detection.TileEntityCloakingCore;
import cr0s.warpdrive.block.hull.BlockHullSlab;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.ChunkData;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatWarpDrive implements IBlockTransformer {
	
	public static void register() {
		WarpDriveConfig.registerBlockTransformer("WarpDrive", new CompatWarpDrive());
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return block instanceof BlockAbstractBase
		    || block instanceof BlockAbstractContainer;
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		if ( tileEntity instanceof TileEntityCloakingCore
		  && ((TileEntityCloakingCore) tileEntity).getIsEnabled() ) {
			reason.append(Commons.getStyleWarning(), "warpdrive.ship.guide.cloaking_field_overlapping");
			return false;
		}
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity) {
		if (block instanceof BlockAirFlow || block instanceof BlockAirSource) {
			final ChunkData chunkData = ChunkHandler.getChunkData(world, x, y, z);
			if (chunkData == null) {
				// chunk isn't loaded, skip it
				return null;
			}
			final int dataAir = chunkData.getDataAir(x, y, z);
			if (dataAir == StateAir.AIR_DEFAULT) {
				return null;
			}
			final NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setInteger("dataAir", dataAir);
			return tagCompound;
		}
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		if ( block instanceof BlockAirFlow
		  || block instanceof BlockAirSource ) {
			final ChunkData chunkData = ChunkHandler.getChunkData(world, x, y, z);
			if (chunkData == null) {
				// chunk isn't loaded, skip it
				return;
			}
			chunkData.setDataAir(x, y, z, StateAir.AIR_DEFAULT);
		}
		if (tileEntity instanceof IGlobalRegionProvider) {
			GlobalRegionManager.removeFromRegistry((IGlobalRegionProvider) tileEntity);
		}
	}
	
	//                                                       0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final short[] mrotDirection          = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] mrotHorizontalSpinning = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 12, 13, 14, 15 };
	private static final short[] mrotHullSlab           = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 15, 14 };
	// cloaking core will refresh itself
	
	private byte[] rotate_byteArray(final byte rotationSteps, final byte[] data) {
		final byte[] newData = data.clone();
		for (int index = 0; index < data.length; index++) {
			switch (rotationSteps) {
			case 1:
				newData[mrotDirection[index]] = data[index];
				break;
			case 2:
				newData[mrotDirection[mrotDirection[index]]] = data[index];
				break;
			case 3:
				newData[mrotDirection[mrotDirection[mrotDirection[index]]]] = data[index];
				break;
			default:
				break;
			}
		}
		return newData;
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		// Hull slabs
		if (block instanceof BlockHullSlab) {
			switch (rotationSteps) {
			case 1:
				return mrotHullSlab[metadata];
			case 2:
				return mrotHullSlab[mrotHullSlab[metadata]];
			case 3:
				return mrotHullSlab[mrotHullSlab[mrotHullSlab[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (nbtTileEntity != null) {
			// subspace capacitor sides
			if (nbtTileEntity.hasKey("modeSide")) {
				nbtTileEntity.setByteArray("modeSide", rotate_byteArray(rotationSteps, nbtTileEntity.getByteArray("modeSide")));
			}
			
			// reactor stabilization laser
			if ( nbtTileEntity.hasKey("reactorFace")
			  && rotationSteps != 0 ) {
				final String reactorFaceOriginal = nbtTileEntity.getString("reactorFace");
				final Pattern patternFacing = Pattern.compile("(laser\\.[a-z]+\\.)([a-z]+)([+-]*)");
				final Matcher matcher = patternFacing.matcher(reactorFaceOriginal);
				if (!matcher.matches()) {
					throw new RuntimeException(String.format("Failed to parse reactor facing %s: unrecognized format",
					                                         reactorFaceOriginal));
				}
				
				final String prefix = matcher.group(1);
				final String nameFacing = matcher.group(2);
				final String suffix = matcher.group(3);
				
				EnumFacing enumFacing = EnumFacing.byName(nameFacing);
				if (enumFacing == null) {
					throw new RuntimeException(String.format("Failed to parse reactor facing %s: unrecognized facing %s",
					                                         reactorFaceOriginal, nameFacing));
				}
				switch (rotationSteps) {
				case 1:
					enumFacing = enumFacing.rotateY();
					break;
				case 2:
					enumFacing = enumFacing.rotateY().rotateY();
					break;
				case 3:
					enumFacing = enumFacing.rotateY().rotateY().rotateY();
					break;
				default:
					assert false;
					break;
				}
				
				final String reactorFaceUpdated = prefix + enumFacing.name().toLowerCase() + suffix; 
				nbtTileEntity.setString("reactorFace", reactorFaceUpdated);
			}
		}
		
		// Rotating blocks
		final IBlockState blockState = block.getStateFromMeta(metadata);
		if (blockState.getProperties().containsKey(BlockProperties.HORIZONTAL_SPINNING)) {
			switch (rotationSteps) {
			case 1:
				return mrotHorizontalSpinning[metadata];
			case 2:
				return mrotHorizontalSpinning[mrotHorizontalSpinning[metadata]];
			case 3:
				return mrotHorizontalSpinning[mrotHorizontalSpinning[mrotHorizontalSpinning[metadata]]];
			default:
				return metadata;
			}			
		}
		
		if ( blockState.getProperties().containsKey(BlockProperties.FACING)
		  || blockState.getProperties().containsKey(BlockProperties.FACING_HORIZONTAL) ) {
			switch (rotationSteps) {
			case 1:
				return mrotDirection[metadata & 0x7] | (metadata & 0x8);
			case 2:
				return mrotDirection[mrotDirection[metadata & 0x7]] | (metadata & 0x8);
			case 3:
				return mrotDirection[mrotDirection[mrotDirection[metadata & 0x7]]] | (metadata & 0x8);
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
		if (nbtBase == null) {
			return;
		}
		if (!(nbtBase instanceof NBTTagCompound)) {
			return;
		}
		if (((NBTTagCompound) nbtBase).hasKey("dataAir")) {
			final byte rotationSteps = transformation.getRotationSteps();
			final int dataAir = ((NBTTagCompound) nbtBase).getInteger("dataAir");
			final ChunkData chunkData = ChunkHandler.getChunkData(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
			if (chunkData == null) {
				// chunk isn't loaded, skip it
				return;
			}
			final int dataAirRotated = StateAir.rotate(dataAir, rotationSteps);
			chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAirRotated);
		}
	}
}
