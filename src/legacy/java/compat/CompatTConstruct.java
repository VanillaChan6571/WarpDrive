package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
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

import net.minecraftforge.common.util.Constants.NBT;

public class CompatTConstruct implements IBlockTransformer {
	
	private static Class<?> classBlockChannel;
	private static Class<?> classBlockFaucet;
	
	private static Class<?> classBlockCasting;
	private static Class<?> classBlockToolForge;
	private static Class<?> classBlockToolTable;
	private static Class<?> classBlockRack;
	
	private static Class<?> classBlockSearedFurnaceController;
	private static Class<?> classBlockSmelteryController;
	private static Class<?> classBlockTinkerTankController;
	
	private static Class<?> classBlockSlimeChannel;
	
	private static Class<?> classBlockEnumSmeltery;
	private static Class<?> classBlockSmelteryIO;
	
	private static Class<?> classBlockStairsBase;
	private static Class<?> classEnumBlockSlab;
	
	public static void register() {
		try {
			classBlockChannel = Class.forName("slimeknights.tconstruct.smeltery.block.BlockChannel");
			classBlockFaucet = Class.forName("slimeknights.tconstruct.smeltery.block.BlockFaucet");
			
			classBlockCasting = Class.forName("slimeknights.tconstruct.smeltery.block.BlockCasting");
			classBlockToolForge = Class.forName("slimeknights.tconstruct.tools.common.block.BlockToolForge");
			classBlockToolTable = Class.forName("slimeknights.tconstruct.tools.common.block.BlockToolTable");
			classBlockRack = Class.forName("slimeknights.tconstruct.gadgets.block.BlockRack");
			
			classBlockSearedFurnaceController = Class.forName("slimeknights.tconstruct.smeltery.block.BlockSearedFurnaceController");
			classBlockSmelteryController = Class.forName("slimeknights.tconstruct.smeltery.block.BlockSmelteryController");
			classBlockTinkerTankController = Class.forName("slimeknights.tconstruct.smeltery.block.BlockTinkerTankController");
			
			classBlockSlimeChannel = Class.forName("slimeknights.tconstruct.gadgets.block.BlockSlimeChannel");
			
			classBlockEnumSmeltery = Class.forName("slimeknights.tconstruct.smeltery.block.BlockEnumSmeltery");
			classBlockSmelteryIO = Class.forName("slimeknights.tconstruct.smeltery.block.BlockSmelteryIO"); // Smeltery Drain
			
			classBlockStairsBase = Class.forName("slimeknights.mantle.block.BlockStairsBase");
			classEnumBlockSlab = Class.forName("slimeknights.mantle.block.EnumBlockSlab");
			WarpDriveConfig.registerBlockTransformer("tconstruct", new CompatTConstruct());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockChannel.isInstance(block)
		    || classBlockFaucet.isInstance(block)
		       
		    || classBlockCasting.isInstance(block)
		    || classBlockToolForge.isInstance(block)
		    || classBlockToolTable.isInstance(block)
		    || classBlockRack.isInstance(block)
		       
			|| classBlockSearedFurnaceController.isInstance(block)
		    || classBlockSmelteryController.isInstance(block)
		    || classBlockTinkerTankController.isInstance(block)
		       
		    || classBlockSlimeChannel.isInstance(block)
		       
		    || classBlockEnumSmeltery.isInstance(block)
//		    || classBlockSmelteryIO.isInstance(block)       (derived from classBlockEnumSmeltery, no point to test it here)
		       
		    || classBlockStairsBase.isInstance(block)
		    || classEnumBlockSlab.isInstance(block);
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
	As of Mantle-1.12-1.3.3.39 + TConstruct-1.12.2-2.11.0.106
	
	Derived from Block
	slimeknights.tconstruct.shared.block.BlockGlow
		=> use default rotation handler
	slimeknights.tconstruct.gadgets.block.BlockPunji
		=> use default rotation handler
	
	Derived from BlockContainer
	slimeknights.tconstruct.smeltery.block.BlockChannel                     minecraft:tconstruct.channel
		connections byte[4]  (index rotation 0 1 2 3)
			0 (no connection), 1 (input), 2 (output)
		is_flowing  byte[4]  (we're assuming it's the same as connections)
	slimeknights.tconstruct.smeltery.block.BlockFaucet                      minecraft:tconstruct.faucet
		direction   2 5 3 4
		metadata    2 5 3 4
	slimeknights.tconstruct.gadgets.block.BlockWoodenHopper                 TileEntityHopper
		metadata    2 5 3 4
		=> use default rotation handler
	
	Derived from BlockInventory
	slimeknights.tconstruct.smeltery.block.BlockCasting                     minecraft:tconstruct.casting_basin / TileCasting
		ForgeData.facing    int     2 5 3 4
	Derived from BlockInventory > BlockTable
	slimeknights.tconstruct.tools.common.block.BlockToolForge               TileToolForge
		ForgeData.facing    int     2 5 3 4
	slimeknights.tconstruct.tools.common.block.BlockToolTable               TileCraftingStation, TileStencilTable, TilePartBuilder, TileToolStation, TilePatternChest, TilePartChest
		ForgeData.facing    int     2 5 3 4
	slimeknights.tconstruct.gadgets.block.BlockRack                         minecraft:tconstruct.item_rack / minecraft:tconstruct.drying_rack
		ForgeData.facing    int     2 5 3 4
		metadata    0 14 / 1 15 / 2 6 4 8 / 3 7 5 9 / 10 12 / 11 13
	
	Derived from BlockMultiblockController
	slimeknights.tconstruct.smeltery.block.BlockSearedFurnaceController     minecraft:tconstruct.seared_furnace
		active  boolean true (it's formed) / false (invalid structure)
		minPos.X/Y/Z    int
		maxPos.X/Y/Z    int
		tanks           List<Compound>
			X/Y/Z    int
		metadata    0 1 2 3
	slimeknights.tconstruct.smeltery.block.BlockSmelteryController          minecraft:tconstruct.smeltery_controller
		active  boolean true (it's formed) / false (invalid structure)
		insidePos.X/Y/Z int  (inside, Y value seems random)
		minPos.X/Y/Z    int  (the inner side content)
		maxPos.X/Y/Z    int  (the inner side content)
		tanks           List<Compound>
			X/Y/Z    int
		metadata    2 5 3 4
	slimeknights.tconstruct.smeltery.block.BlockTinkerTankController        minecraft:tconstruct.tinker_tank
		active  boolean true (it's formed) / false (invalid structure)
		minPos.X/Y/Z    int
		maxPos.X/Y/Z    int
		metadata    2 5 3 4
	
	Derived from slimeknights.mantle.block.EnumBlock
	slimeknights.tconstruct.gadgets.block.BlockSlimeChannel                 minecraft:tconstruct.slime_channel
		ForgeData.side        2 5 3 4
		ForgeData.direction   0 2 4 6 / 1 3 5 7
		metadata 0x7 type / 0x8 powered
	
	Derived from slimeknights.mantle.block.EnumBlock
	             > slimeknights.tconstruct.smeltery.block.BlockEnumSmeltery
	slimeknights.tconstruct.smeltery.block.BlockSeared                      minecraft:tconstruct.smeltery_component
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
		metadata (type)
	slimeknights.tconstruct.smeltery.block.BlockSearedGlass                 minecraft:tconstruct.smeltery_component
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
		metadata (type)
	slimeknights.tconstruct.smeltery.block.BlockSmelteryIO                  minecraft:tconstruct.smeltery_drain
		masterState int     (Block.getStateId())
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
		metadata    0 4 8 12 / 1 5 9 13 / 2 6 10 14 / 3 7 11 15
	slimeknights.tconstruct.smeltery.block.BlockTank                        minecraft:tconstruct.smeltery_component
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
	
	Derived from slimeknights.mantle.block.BlockStairsBase
	slimeknights.tconstruct.smeltery.block.BlockSearedStairs                minecraft:tconstruct.smeltery_component
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
		metadata (see vanilla BlockStairs)
	
	Derived from slimeknights.mantle.block.EnumBlockSlab
	BlockSearedSlab                                                         minecraft:tconstruct.smeltery_component
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
		metadata (type)
	BlockSearedSlab2                                                        minecraft:tconstruct.smeltery_component
		hasMaster   boolean true (it's formed) / false (no controller)
		xCenter/yCenter/zCenter int (the controller)
		metadata (type)
	*/
	
	//                                                  0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[]  mrotStair          = {  2,  3,  1,  0,  6,  7,  5,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  mrotFacing         = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  mrotHorizontal     = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  mrotDrain          = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };
	private static final int[]  mrotRack           = { 14, 15,  6,  7,  8,  9,  4,  5,  2,  3, 12, 13, 10, 11,  0,  1 };
	private static final int[]  rotSlimeDirection  = {  2,  3,  4,  5,  6,  7,  0,  1,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		// *** NBT transformation
		if (nbtTileEntity != null) {
			// ForgeData compound
			if (nbtTileEntity.hasKey("ForgeData", NBT.TAG_COMPOUND)) {
				final NBTTagCompound nbtForgeData = nbtTileEntity.getCompoundTag("ForgeData");
				
				// facing from Tables, Castings and Racks is just facing
				if (nbtForgeData.hasKey("facing")) {
					final int facing = nbtForgeData.getInteger("facing");
					switch (rotationSteps) {
					case 1:
						nbtForgeData.setInteger("facing", mrotFacing[facing]);
						break;
					case 2:
						nbtForgeData.setInteger("facing", mrotFacing[mrotFacing[facing]]);
						break;
					case 3:
						nbtForgeData.setInteger("facing", mrotFacing[mrotFacing[mrotFacing[facing]]]);
						break;
					default:
						break;
					}
				}
				
				// side from slime channels is just facing
				if ( nbtForgeData.hasKey("side")
				  && nbtForgeData.hasKey("direction") ) {
					final int side = nbtForgeData.getInteger("side");
					switch (rotationSteps) {
					case 1:
						nbtForgeData.setInteger("side", mrotFacing[side]);
						break;
					case 2:
						nbtForgeData.setInteger("side", mrotFacing[mrotFacing[side]]);
						break;
					case 3:
						nbtForgeData.setInteger("side", mrotFacing[mrotFacing[mrotFacing[side]]]);
						break;
					default:
						break;
					}
					
					// direction from slime channels is power and horizontal rotation
					if (side == 0 || side == 1) {
						final int direction = nbtForgeData.getInteger("direction");
						switch (rotationSteps) {
						case 1:
							nbtForgeData.setInteger("direction", rotSlimeDirection[direction]);
							break;
						case 2:
							nbtForgeData.setInteger("direction", rotSlimeDirection[rotSlimeDirection[direction]]);
							break;
						case 3:
							nbtForgeData.setInteger("direction", rotSlimeDirection[rotSlimeDirection[rotSlimeDirection[direction]]]);
							break;
						default:
							break;
						}
					}
				}
			}
			
			// Channel connections
			if (nbtTileEntity.hasKey("connections")) {
				final byte[] bytesOldConnections = nbtTileEntity.getByteArray("connections");
				final byte[] bytesNewConnections = bytesOldConnections.clone();
				for (int sideOld = 0; sideOld < 4; sideOld++) {
					final byte byteConnection = bytesOldConnections[sideOld];
					bytesNewConnections[(sideOld + rotationSteps) % 4] = byteConnection;
				}
				nbtTileEntity.setByteArray("connections", bytesNewConnections);
			}
			
			// smeltery components
			if (nbtTileEntity.getBoolean("hasMaster")) {// (defined and non-zero means there's a master/controller)
				if ( nbtTileEntity.hasKey("xCenter", NBT.TAG_INT)
				  && nbtTileEntity.hasKey("yCenter", NBT.TAG_INT) 
				  && nbtTileEntity.hasKey("zCenter", NBT.TAG_INT) ) {
					final BlockPos blockPosCenter = transformation.apply(
							nbtTileEntity.getInteger("xCenter"),
							nbtTileEntity.getInteger("yCenter"),
							nbtTileEntity.getInteger("zCenter") );
					nbtTileEntity.setInteger("xCenter", blockPosCenter.getX());
					nbtTileEntity.setInteger("yCenter", blockPosCenter.getY());
					nbtTileEntity.setInteger("zCenter", blockPosCenter.getZ());
				} else {
					WarpDrive.logger.warn(String.format("Missing center coordinates for 'smeltery' component %s:%s %s",
					                                    block, metadata, nbtTileEntity));
				}
			}
			
			// controllers
			if (nbtTileEntity.getBoolean("active")) {// (defined and non-zero means the structure is valid)
				// mandatory min/max position of the inner volume
				if ( nbtTileEntity.hasKey("minPos", NBT.TAG_COMPOUND)
				  && nbtTileEntity.hasKey("maxPos", NBT.TAG_COMPOUND) ) {
					final NBTTagCompound nbtMinOldPos = nbtTileEntity.getCompoundTag("minPos");
					final NBTTagCompound nbtMaxOldPos = nbtTileEntity.getCompoundTag("maxPos");
					
					if ( nbtMinOldPos.hasKey("X", NBT.TAG_INT)
				      && nbtMinOldPos.hasKey("Y", NBT.TAG_INT)
				      && nbtMinOldPos.hasKey("Z", NBT.TAG_INT)
					  && nbtMaxOldPos.hasKey("X", NBT.TAG_INT)
					  && nbtMaxOldPos.hasKey("Y", NBT.TAG_INT)
					  && nbtMaxOldPos.hasKey("Z", NBT.TAG_INT) ) {
						final BlockPos blockPosNew1 = transformation.apply(
								nbtMinOldPos.getInteger("X"),
								nbtMinOldPos.getInteger("Y"),
								nbtMinOldPos.getInteger("Z") );
						final BlockPos blockPosNew2 = transformation.apply(
								nbtMaxOldPos.getInteger("X"),
								nbtMaxOldPos.getInteger("Y"),
								nbtMaxOldPos.getInteger("Z") );
						
						nbtMinOldPos.setInteger("X", Math.min(blockPosNew1.getX(), blockPosNew2.getX()));
						nbtMinOldPos.setInteger("Y", Math.min(blockPosNew1.getY(), blockPosNew2.getY()));
						nbtMinOldPos.setInteger("Z", Math.min(blockPosNew1.getZ(), blockPosNew2.getZ()));
						nbtMaxOldPos.setInteger("X", Math.max(blockPosNew1.getX(), blockPosNew2.getX()));
						nbtMaxOldPos.setInteger("Y", Math.max(blockPosNew1.getY(), blockPosNew2.getY()));
						nbtMaxOldPos.setInteger("Z", Math.max(blockPosNew1.getZ(), blockPosNew2.getZ()));
					} else {
						WarpDrive.logger.warn(String.format("Missing X/Y/Z components for inner volume of controller %s:%s %s",
						                                    block, metadata, nbtTileEntity));
					}
				} else {
					WarpDrive.logger.warn(String.format("Missing minPos/maxPos compound data for component %s:%s %s",
					                                    block, metadata, nbtTileEntity));
				}
				
				// optional list of tank's absolute position
				if (nbtTileEntity.hasKey("tanks", NBT.TAG_LIST)) {
					final NBTTagList listTanks = nbtTileEntity.getTagList("tanks", NBT.TAG_COMPOUND);
					for (int index = 0; index < listTanks.tagCount(); index++) {
						final NBTTagCompound nbtValue = (NBTTagCompound) listTanks.get(index);
						
						if ( nbtValue.hasKey("X", NBT.TAG_INT)
						  && nbtValue.hasKey("Y", NBT.TAG_INT)
						  && nbtValue.hasKey("Z", NBT.TAG_INT) ) {
							final BlockPos blockPosNew = transformation.apply(
									nbtValue.getInteger("X"),
									nbtValue.getInteger("Y"),
									nbtValue.getInteger("Z") );
							
							nbtValue.setInteger("X", blockPosNew.getX());
							nbtValue.setInteger("Y", blockPosNew.getY());
							nbtValue.setInteger("Z", blockPosNew.getZ());
						} else {
							WarpDrive.logger.warn(String.format("Missing X/Y/Z components for tank#%d of controller %s:%s %s",
							                                    index, block, metadata, nbtTileEntity));
						}
						listTanks.set(index, nbtValue);
					}
				}
				
				// optional insidePos absolute position
				if (nbtTileEntity.hasKey("insidePos", NBT.TAG_COMPOUND)) {
					final NBTTagCompound nbtInsidePos = nbtTileEntity.getCompoundTag("insidePos");
					if ( nbtInsidePos.hasKey("X", NBT.TAG_INT)
					  && nbtInsidePos.hasKey("Y", NBT.TAG_INT)
					  && nbtInsidePos.hasKey("Z", NBT.TAG_INT) ) {
						final BlockPos blockPosNew = transformation.apply(
								nbtInsidePos.getInteger("X"),
								nbtInsidePos.getInteger("Y"),
								nbtInsidePos.getInteger("Z") );
						
						nbtInsidePos.setInteger("X", blockPosNew.getX());
						nbtInsidePos.setInteger("Y", blockPosNew.getY());
						nbtInsidePos.setInteger("Z", blockPosNew.getZ());
					} else {
						WarpDrive.logger.warn(String.format("Missing X/Y/Z components for insidePos of controller %s:%s %s",
						                                    block, metadata, nbtTileEntity));
					}
				}
			}
		}
		
		// *** metadata rotation
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// Rack is custom type & facing
		if (classBlockRack.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotRack[metadata];
			case 2:
				return mrotRack[mrotRack[metadata]];
			case 3:
				return mrotRack[mrotRack[mrotRack[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Seared furnace is horizontal facing
		if (classBlockSearedFurnaceController.isInstance(block)) {
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
		
		// Faucet, Smeltery controller, Tinker Tank are just facing
		if ( classBlockFaucet.isInstance(block)
		  || classBlockSmelteryController.isInstance(block)
		  || classBlockTinkerTankController.isInstance(block) ) {
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
		
		// Smeltery drain is type & horizontal facing
		if (classBlockSmelteryIO.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotDrain[metadata];
			case 2:
				return mrotDrain[mrotDrain[metadata]];
			case 3:
				return mrotDrain[mrotDrain[mrotDrain[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Stairs is like vanilla
		if (classBlockStairsBase.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotStair[metadata];
			case 2:
				return mrotStair[mrotStair[metadata]];
			case 3:
				return mrotStair[mrotStair[mrotStair[metadata]]];
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
