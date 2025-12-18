package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants.NBT;

public class CompatOpenComputers implements IBlockTransformer {
	
	// common base class
	private static Class<?> classBlockSimpleBlock;
	
	// block with metadata rotation to consider
	private static Class<?> classBlockDiskDrive;
	private static Class<?> classBlockKeyboard;
	private static Class<?> classBlockRaid;
	private static Class<?> classBlockCase;
	private static Class<?> classBlockCharger;
	private static Class<?> classBlockMicrocontroller;
	private static Class<?> classBlockRack;
	private static Class<?> classBlockScreen;
	
	public static void register() {
		try {
			classBlockSimpleBlock = Class.forName("li.cil.oc.common.block.SimpleBlock");
			
			classBlockDiskDrive = Class.forName("li.cil.oc.common.block.DiskDrive");
			classBlockKeyboard = Class.forName("li.cil.oc.common.block.Keyboard");
			classBlockRaid = Class.forName("li.cil.oc.common.block.Raid");
			classBlockCase = Class.forName("li.cil.oc.common.block.Case");
			classBlockCharger = Class.forName("li.cil.oc.common.block.Charger");
			classBlockMicrocontroller = Class.forName("li.cil.oc.common.block.Microcontroller");
			classBlockRack = Class.forName("li.cil.oc.common.block.Rack");
			classBlockScreen = Class.forName("li.cil.oc.common.block.Screen");
			
			WarpDriveConfig.registerBlockTransformer("opencomputers", new CompatOpenComputers());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockSimpleBlock.isInstance(block);
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
	
	/*
	As of OpenComputers-MC1.12.2-1.7.2.103
	
	Base block class is li.cil.oc.common.block.SimpleBlock
	
	Simple blocks that don't rotate
	li.cil.oc.common.block.ChameliumBlock (no tile entity, no rotation)
	li.cil.oc.common.block.FakeEndstone (no tile entity, no rotation)
	li.cil.oc.common.block.Assembler   oc:assembler
	li.cil.oc.common.block.Cable  oc:cable
	li.cil.oc.common.block.Capacitor   oc:capacitor
	li.cil.oc.common.block.CarpetedCapacitor   oc:carpetedcapacitor
	li.cil.oc.common.block.Disassembler    oc:disassembler
	li.cil.oc.common.block.Geolyzer    oc:geolyzer
	li.cil.oc.common.block.MotionSensor    oc:motionsensor
	li.cil.oc.common.block.PowerConverter  oc:powerconverter
	li.cil.oc.common.block.Printer   oc:printer
	li.cil.oc.common.block.Transposer  oc:transposer
	
	Simple blocks that do rotate
	li.cil.oc.common.block.Adapter oc:adapter
		oc:adapter.blocks List<Compound>(6) ?
	    oc:openSides byte ?
	li.cil.oc.common.block.DiskDrive   oc:diskdrive
		metadata 0 1 2 3
	li.cil.oc.common.block.Hologram    oc:hologram
		dimension   int      (don't change it or use externalData)
		chunkX/Z    int      (don't change it or use externalData)
		oc:yaw  int 2 5 3 4
		oc:pitch    int 2
		oc:offsetX/Y/Z double
		oc:rotationX/Y/Z    double
	li.cil.oc.common.block.Keyboard    oc:keyboard
		metadata 0 1 2 3 / 4 5 6 7 / 8 9 10 11
	li.cil.oc.common.block.PowerDistributor    oc:powerdistributor
		oc:connector    List<Compound>(6)
			address, buffer
	li.cil.oc.common.block.Raid    oc:raid
		metadata    0 1 2 3
	li.cil.oc.common.block.Relay   oc:relay
		oc:componentNodes   List(6)
			address, visibility, others?
		oc:plugs    List(6)
			address, buffer
	li.cil.oc.common.block.RobotAfterimage ?
	
	Derived blocks from li.cil.oc.common.block.RedstoneAware that do rotate
	li.cil.oc.common.block.Case    oc:case
		oc:rs.bundledInput   List<Int[16]>(6)
		oc:rs.bundledOutput  List<Int[16]>(6)
		oc:rs.rednetInput    List<Int[16]>(6)
		oc:rs.input          int[6]
		oc:rs.output         int[6]
		metadata    0 2 4 6 / 1 3 5 7
	li.cil.oc.common.block.Charger oc:charger
		oc:rs.input          List<Int[16]>(6)
		oc:rs.output         List<Int[16]>(6)
		metadata 0 1 2 3
		oc:rotationSpeedX/Y/Z   double
	li.cil.oc.common.block.Microcontroller oc:microcontroller
		oc:rs.bundledInput   List<Int[16]>(6)
		oc:rs.bundledOutput  List<Int[16]>(6)
		oc:rs.rednetInput    List<Int[16]>(6)
		oc:rs.input          int[6]
		oc:rs.output         int[6]
		metadata    0 1 2 3
	li.cil.oc.common.block.NetSplitter oc:netsplitter
	    oc:rs.input          int[6] 0 0 0 0 0 0
	    oc:rs.output         int[6] 0 0 0 0 0 0
	li.cil.oc.common.block.Print   @TODO to be evaluated
	li.cil.oc.common.block.Rack    oc:rack
		oc:plugs    List(6)
			address, buffer
		oc:rs.bundledInput   List<Int[16]>(6)
		oc:rs.bundledOutput  List<Int[16]>(6)
		oc:rs.rednetInput    List<Int[16]>(6)
		oc:rs.input          int[6]
		oc:rs.output         int[6]
		metadata    0 1 2 3
	li.cil.oc.common.block.RobotProxy oc:robot
		oc:yaw  int 2 5 3 4
		oc:pitch  int   2
		oc:rs.bundledInput   List<Int[16]>(6)
		oc:rs.bundledOutput  List<Int[16]>(6)
		oc:rs.rednetInput    List<Int[16]>(6)
		oc:rs.input          int[6]
		oc:rs.output         int[6]
	li.cil.oc.common.block.Screen  oc:screen
		dimension   int      (don't change it or use externalData)
		chunkX/Z    int      (don't change it or use externalData)
		metadata 0 1 2 3 / 4 5 6 7 / 8 9 10 11
	li.cil.oc.common.block.Waypoint    oc:waypoint
		oc:rs.input          int[6]
		oc:rs.output         int[6]
	*/
	
	//                                               0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[]  rotFacing       = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotHorizontal  = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotFull        = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotCase        = {  2,  3,  4,  5,  6,  7,  0,  1,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Nonnull
	private NBTTagList rotate_list(final byte rotationSteps, @Nonnull final NBTTagList listOldValues) {
		final NBTTagList listNewValues = new NBTTagList();
		// nota: we clone the list first so all indexes are already defined
		for (int index = 0; index < listOldValues.tagCount(); index++) {
			listNewValues.appendTag(listOldValues.get(index));
		}
		// do rotate
		for (int index = 0; index < listOldValues.tagCount(); index++) {
			final NBTBase nbtValue = listOldValues.get(index);
			switch (rotationSteps) {
			case 1:
				listNewValues.set(rotFacing[index], nbtValue);
				break;
			case 2:
				listNewValues.set(rotFacing[rotFacing[index]], nbtValue);
				break;
			case 3:
				listNewValues.set(rotFacing[rotFacing[rotFacing[index]]], nbtValue);
				break;
			default:
				listNewValues.set(index, nbtValue);
				break;
			}
		}
		return listNewValues;
	}
	
	@Nonnull
	private int[] rotate_intArray(final byte rotationSteps, @Nonnull final int[] intOldValues) {
		final int[] intNewValues = intOldValues.clone();
		for (int index = 0; index < intOldValues.length; index++) {
			switch (rotationSteps) {
			case 1:
				intNewValues[rotFacing[index]] = intOldValues[index];
				break;
			case 2:
				intNewValues[rotFacing[rotFacing[index]]] = intOldValues[index];
				break;
			case 3:
				intNewValues[rotFacing[rotFacing[rotFacing[index]]]] = intOldValues[index];
				break;
			default:
				break;
			}
		}
		return intNewValues;
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// *** NBT data transformations
		if (nbtTileEntity != null) {
			// adapter blocks @TODO to be integrated
			if (nbtTileEntity.hasKey("oc:adapter.blocks", NBT.TAG_LIST)) {
				nbtTileEntity.setTag("oc:adapter.blocks", rotate_list(rotationSteps, nbtTileEntity.getTagList("oc:adapter.blocks", NBT.TAG_COMPOUND)));
			}
			
			// bundled and rednet signals
			if (nbtTileEntity.hasKey("oc:rs.bundledInput", NBT.TAG_LIST)) {
				nbtTileEntity.setTag("oc:rs.bundledInput", rotate_list(rotationSteps, nbtTileEntity.getTagList("oc:rs.bundledInput", NBT.TAG_INT_ARRAY)));
			}
			if (nbtTileEntity.hasKey("oc:rs.bundledOutput", NBT.TAG_LIST)) {
				nbtTileEntity.setTag("oc:rs.bundledOutput", rotate_list(rotationSteps, nbtTileEntity.getTagList("oc:rs.bundledOutput", NBT.TAG_INT_ARRAY)));
			}
			if (nbtTileEntity.hasKey("oc:rs.rednetInput", NBT.TAG_LIST)) {
				nbtTileEntity.setTag("oc:rs.rednetInput", rotate_list(rotationSteps, nbtTileEntity.getTagList("oc:rs.rednetInput", NBT.TAG_INT_ARRAY)));
			}
			
			// simple redstone signals
			if (nbtTileEntity.hasKey("oc:rs.input", NBT.TAG_INT_ARRAY)) {
				nbtTileEntity.setIntArray("oc:rs.input", rotate_intArray(rotationSteps, nbtTileEntity.getIntArray("oc:rs.input")));
			}
			if (nbtTileEntity.hasKey("oc:rs.output", NBT.TAG_INT_ARRAY)) {
				nbtTileEntity.setIntArray("oc:rs.output", rotate_intArray(rotationSteps, nbtTileEntity.getIntArray("oc:rs.output")));
			}
			
			// yaw value
			if (nbtTileEntity.hasKey("oc:yaw", NBT.TAG_INT)) {
				final int facing = nbtTileEntity.getInteger("oc:yaw");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("oc:yaw", rotFacing[facing]);
					break;
				case 2:
					nbtTileEntity.setInteger("oc:yaw", rotFacing[rotFacing[facing]]);
					break;
				case 3:
					nbtTileEntity.setInteger("oc:yaw", rotFacing[rotFacing[rotFacing[facing]]]);
					break;
				default:
					break;
				}
			}
		}
		
		// *** metadata transformation
		// simple horizontal rotation by metadata
		if ( classBlockDiskDrive.isInstance(block)
		  || classBlockRaid.isInstance(block)
		  || classBlockCharger.isInstance(block)
		  || classBlockMicrocontroller.isInstance(block)
		  || classBlockRack.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrotHorizontal[metadata];
			case 2:
				return mrotHorizontal[mrotHorizontal[metadata]];
			case 3:
				return mrotHorizontal[mrotHorizontal[mrotHorizontal[metadata]]];
			default:
				return metadata;
			}
		}
		
		// full vertex rotation by metadata
		if ( classBlockKeyboard.isInstance(block)
		  || classBlockScreen.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrotFull[metadata];
			case 2:
				return mrotFull[mrotFull[metadata]];
			case 3:
				return mrotFull[mrotFull[mrotFull[metadata]]];
			default:
				return metadata;
			}
		}
		
		// running state & rotation by metadata
		if (classBlockCase.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotCase[metadata];
			case 2:
				return mrotCase[mrotCase[metadata]];
			case 3:
				return mrotCase[mrotCase[mrotCase[metadata]]];
			default:
				return metadata;
			}
		}
		
		// no metadata rotation
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
