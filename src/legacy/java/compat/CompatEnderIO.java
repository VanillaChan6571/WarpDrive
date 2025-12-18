package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatEnderIO implements IBlockTransformer {
	
	private static Class<?> classTileEntityEIO;
	
	public static void register() {
		try {
			classTileEntityEIO = Class.forName("crazypants.enderio.base.TileEntityEio");
			
			WarpDriveConfig.registerBlockTransformer("enderio", new CompatEnderIO());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classTileEntityEIO.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	/*
	As of EnderIO-1.12.2-5.1.52
	
	Non tile entities: anvil, door, bars, trap door, timed levers, antenna extender?
	
	enderio:tile_omni_reservoir
	enderio:tile_reservoir
	enderio:tile_wireless_charger
		metadata 0
	
	enderio:tile_enderman_skull
		float   yaw -315 to +0 / +90 per rotation step
	
	enderio:tile_tele_pad
		metadata    1 / 2 4 6 8 / 3 5 7 9
	
	enderio:tile_dialing_device
		int facing          3
		int dialerFacing    0 3 1 2 / 4 7 5 6 / 8 20 13 17 / 9 21 12 16 / 10 22 14 18 / 11 23 15 19
	
	enderio:tile_travel_anchor
	enderio:tile_lava_generator
	enderio:tile_soul_binder
	enderio:tile_combustion_generator_enhanced
	enderio:tile_alloy_smelter_furnace
	enderio:tile_alloy_smelter
	enderio:tile_alloy_smelter_enhanced
		int     facing      2 5 3 4
		int[3]  faceModes   3, 0, 0x004c0 0x1a000 0x00680 0x13000
							actually it's EnumMap<EnumFacing, IoMode> where IoMode can be NONE, PULL, PUSH, PUSH_PULL, DISABLED
							this is coded by sequence of 3 bits
	
	enderio:tile_cap_bank
		int[3]  faceModes           3, 0, 0x004c0 0x1a000 0x00680 0x13000
		int[3]  faceDisplayTypes    3, 0, 0x00080 0x10000 0x00400 0x02000 0b010010010010010010
	                                      0x000c0 0x00600 0x03000 0x18000 0b011011011011000000
	                                      0x00140 0x00a00 0x05000 0x28000 0b101101101101000000
	
	enderio:tile_conduit_bundle
		compound    conduits
			int         size
			compound    0 / 1 / 2 / etc.
				int[0 to 6] connections            0 / 1 / 2 5 3 4
				int[0 to 6] externalConnections    0 / 1 / 2 5 3 4
				byte[6]     conModes
				byte[6]     forcedConnections
				byte[6]     outputSignalColors
				byte[6]     signalColors
				byte[6]     signalStrengths
				tagCompound functionUpgrades.NORTH/SOUTH/EAST/WEST
				tagCompound inFilts.NORTH/SOUTH/EAST/WEST
				tagCompound inputFilterUpgrades.NORTH/SOUTH/EAST/WEST
				tagCompound outFilts.NORTH/SOUTH/EAST/WEST
				tagCompound outputFilterUpgrades.NORTH/SOUTH/EAST/WEST
				short       extRM.WEST          is it rsModes or extractionModes ?
				short       extSC.WEST          is it rsColors or extractionColors ?
				short       inSC.WEST           is it inputColors ?
				short       outSC.WEST          is it outputColors ?
				int         priority.WEST
				boolean     roundRobin.WEST
				boolean     selfFeed.WEST
	
	 */
	private static final Map<String, String> rotSideNames;
	static {
		final Map<String, String> map = new HashMap<>();
		map.put("EAST", "SOUTH");
		map.put("SOUTH", "WEST");
		map.put("WEST", "NORTH");
		map.put("NORTH", "EAST");
		rotSideNames = Collections.unmodifiableMap(map);
	}
	private static final String[] nameEnumMapEnumFacings = {
			"faceModes",
			"faceDisplayTypes"
	};
	private static final int[] rotFacing          = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	//                                                 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
	private static final int[] rotDialerFacing    = {  3,  2,  0,  1,  7,  6,  4,  5, 20, 21, 22, 23, 16, 17, 18, 19,  9,  8, 10, 11, 13, 12, 14, 15 };
	private static final int[] mrotTelePad        = {  0,  1,  4,  5,  6,  7,  8,  9,  2,  3, 10, 11, 12, 13, 14, 15 };
	
	private byte[] rotate_byteArray(final byte rotationSteps, @Nonnull final byte[] data) {
		final byte[] newData = data.clone();
		for (int index = 0; index < data.length; index++) {
			switch (rotationSteps) {
			case 1:
				newData[rotFacing[index]] = data[index];
				break;
			case 2:
				newData[rotFacing[rotFacing[index]]] = data[index];
				break;
			case 3:
				newData[rotFacing[rotFacing[rotFacing[index]]]] = data[index];
				break;
			default:
				break;
			}
		}
		return newData;
	}
	
	private int[] rotate_enumFacing(final byte rotationSteps, @Nonnull final int[] data) {
		// we only support 3 bits encoding
		if ( data.length != 3
		  || data[0] != 3
		  || data[1] != 0 ) {
			WarpDrive.logger.error(String.format("Invalid EnumFacing encoding [%s], please report to mod author",
			                                     Commons.formatHexadecimal(data) ));
			return data;
		}
		
		// extract into a byte array
		final int[] newData = data.clone();
		final byte[] bytes = new byte[6];
		int value = data[2];
		for (int index = 0; index < 6; index++) {
			bytes[index] = (byte) (value & 0b111);
			value = value >> 3;
		}
		
		// rotate
		final byte[] newBytes = rotate_byteArray(rotationSteps, bytes);
		
		// encode back
		int newValue = 0;
		for (int index = 5; index >= 0; index--) {
			newValue = (newValue << 3) | (newBytes[index] & 0b111);
		}
		newData[2] = newValue;
		
		return newData;
	}
	
	private NBTTagCompound rotate_conduit(final byte rotationSteps, final NBTTagCompound nbtConduit) {
		final NBTTagCompound nbtNewConduit = new NBTTagCompound();
		final Set<String> keys = nbtConduit.getKeySet();
		for (final String key : keys) {
			final NBTBase base = nbtConduit.getTag(key);
			switch(base.getId()) {
			case Constants.NBT.TAG_INT_ARRAY:	// "connections", "externalConnections"
				final int[] data = nbtConduit.getIntArray(key);
				final int[] newData = data.clone();
				for (int index = 0; index < data.length; index++) {
					switch (rotationSteps) {
					case 1:
						newData[index] = rotFacing[data[index]];
						break;
					case 2:
						newData[index] = rotFacing[rotFacing[data[index]]];
						break;
					case 3:
						newData[index] = rotFacing[rotFacing[rotFacing[data[index]]]];
						break;
					default:
						break;
					}
				}
				nbtNewConduit.setIntArray(key, newData);
				break;
				
			case Constants.NBT.TAG_BYTE_ARRAY:	// "conModes", "forcedConnections", "outputSignalColors", "signalColors", "signalStrengths"
				nbtNewConduit.setByteArray(key, rotate_byteArray(rotationSteps, nbtConduit.getByteArray(key)));
				break;
				
			default:
				final String[] parts = key.split("\\.");
				if (parts.length != 2 || !rotSideNames.containsKey(parts[1])) {
					nbtNewConduit.setTag(key, base);
				} else {
					switch (rotationSteps) {
					case 1:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(parts[1]), base);
						break;
					case 2:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(rotSideNames.get(parts[1])), base);
						break;
					case 3:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(rotSideNames.get(rotSideNames.get(parts[1]))), base);
						break;
					default:
						nbtNewConduit.setTag(key, base);
						break;
					}
				}
				break;
			}
		}
		return nbtNewConduit;
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		// Telepad are just metadata rotation
		if (nbtTileEntity.getString("id").equals("enderio:tile_tele_pad")) {
			switch (rotationSteps) {
			case 1:
				return mrotTelePad[metadata];
			case 2:
				return mrotTelePad[mrotTelePad[metadata]];
			case 3:
				return mrotTelePad[mrotTelePad[mrotTelePad[metadata]]];
			default:
				return metadata;
			}
		}
		
		// dialerFacing takes priority on facing, nothing else, so we do it in advance and return
		if (nbtTileEntity.hasKey("dialerFacing")) {
			final int dialerFacing = nbtTileEntity.getInteger("dialerFacing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("dialerFacing", rotDialerFacing[dialerFacing]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("dialerFacing", rotDialerFacing[rotDialerFacing[dialerFacing]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("dialerFacing", rotDialerFacing[rotDialerFacing[rotDialerFacing[dialerFacing]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// rotation is for pressure plates, nothing else, so we return
		if (nbtTileEntity.hasKey("rotation")) {
			final short rotation = nbtTileEntity.getShort("rotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("rotation", rotFacing[rotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("rotation", rotFacing[rotFacing[rotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("rotation", rotFacing[rotFacing[rotFacing[rotation]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// facing is common, sometime with more behind
		if (nbtTileEntity.hasKey("facing")) {
			final short facing = nbtTileEntity.getShort("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("facing", rotFacing[facing]);
				break;
			case 2:
				nbtTileEntity.setInteger("facing", rotFacing[rotFacing[facing]]);
				break;
			case 3:
				nbtTileEntity.setInteger("facing", rotFacing[rotFacing[rotFacing[facing]]]);
				break;
			default:
				break;
			}
		}
		
		// Faces
		for (final String nameEnumFacing : nameEnumMapEnumFacings) {
			if (nbtTileEntity.hasKey(nameEnumFacing)) {
				final int[] ints  = nbtTileEntity.getIntArray(nameEnumFacing);
				final int[] intsNew = rotate_enumFacing(rotationSteps, ints);
				nbtTileEntity.setIntArray(nameEnumFacing, intsNew);
			}
		}
		
		// Conduits
		if (nbtTileEntity.hasKey("conduits")) {
			final NBTTagCompound nbtConduits = nbtTileEntity.getCompoundTag("conduits");
			final int size = nbtConduits.getInteger("size");
			for (int index = 0; index < size; index++) {
				final String key = Integer.toString(index);
				final NBTTagCompound nbtConduit = nbtConduits.getCompoundTag(key);
				nbtConduits.setTag(key, rotate_conduit(rotationSteps, nbtConduit));
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
