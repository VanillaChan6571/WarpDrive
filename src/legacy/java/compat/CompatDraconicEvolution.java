package cr0s.warpdrive.compat;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants.NBT;

public class CompatDraconicEvolution implements IBlockTransformer {
	
	// common block for fast detection
	private static Class<?> classBlockBlockDE;
	
	// block anchors by lore (portal)
	private static Class<?> classBlockDislocatorReceptacle;
	private static Class<?> classBlockPortal;
	
	// blocks with only metadata
	private static Class<?> classBlockFlowGate;
	private static Class<?> classBlockGenerator;
	private static Class<?> classBlockGrinder;
	private static Class<?> classBlockPotentiometer;
	
	// blocks with just rotation
	private static Class<?> classBlockDislocatorPedestal;
	private static Class<?> classBlockDraconiumChest;
	private static Class<?> classBlockPlacedItem;
	
	// blocks with rotation and position(s)
	// private static Class<?> classBlockCraftingInjector;
	// private static Class<?> classBlockEnergyCrystal;
	// private static Class<?> classBlockEnergyStorageCore;
	// private static Class<?> classBlockEnergyPylon;
	private static Class<?> classBlockInvisECoreBlock;
	// private static Class<?> classBlockParticleGenerator;
	// private static Class<?> classBlockReactorComponent;
	// private static Class<?> classBlockReactorCore;
	
	public static void register() {
		try {
			classBlockBlockDE = Class.forName("com.brandon3055.brandonscore.blocks.BlockBCore");
			
			// *** block anchors by lore (portal)
			classBlockDislocatorReceptacle = Class.forName("com.brandon3055.draconicevolution.blocks.DislocatorReceptacle");
			classBlockPortal = Class.forName("com.brandon3055.draconicevolution.blocks.Portal");
			
			// *** blocks with only metadata
			classBlockFlowGate = Class.forName("com.brandon3055.draconicevolution.blocks.machines.FlowGate");
			classBlockGenerator = Class.forName("com.brandon3055.draconicevolution.blocks.machines.Generator");
			classBlockGrinder = Class.forName("com.brandon3055.draconicevolution.blocks.machines.Grinder");
			classBlockPotentiometer = Class.forName("com.brandon3055.draconicevolution.blocks.Potentiometer");
			
			// *** blocks with just rotation
			classBlockDislocatorPedestal = Class.forName("com.brandon3055.draconicevolution.blocks.DislocatorPedestal");
			classBlockDraconiumChest = Class.forName("com.brandon3055.draconicevolution.blocks.DraconiumChest");
			classBlockPlacedItem = Class.forName("com.brandon3055.draconicevolution.blocks.PlacedItem");
			
			// *** blocks with rotation and position(s)
			// classBlockCraftingInjector = Class.forName("com.brandon3055.draconicevolution.blocks.machines.CraftingInjector");
			// classBlockEnergyCrystal = Class.forName("com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal");
			// classBlockEnergyStorageCore = Class.forName("com.brandon3055.draconicevolution.blocks.machines.EnergyStorageCore");
			// classBlockEnergyPylon = Class.forName("com.brandon3055.draconicevolution.blocks.machines.EnergyPylon");
			classBlockInvisECoreBlock = Class.forName("com.brandon3055.draconicevolution.blocks.InvisECoreBlock");
			// classBlockParticleGenerator = Class.forName("com.brandon3055.draconicevolution.blocks.ParticleGenerator");
			// classBlockReactorComponent = Class.forName("com.brandon3055.draconicevolution.blocks.reactor.ReactorComponent");
			// classBlockReactorCore = Class.forName("com.brandon3055.draconicevolution.blocks.reactor.ReactorCore");
			
			WarpDriveConfig.registerBlockTransformer("DraconicEvolution", new CompatDraconicEvolution());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockBlockDE.isInstance(block);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		if ( classBlockDislocatorReceptacle.isInstance(block)
		  || classBlockPortal.isInstance(block) ) {
			reason.append(Commons.getStyleWarning(), "warpdrive.compat.guide.draconic_evolution_portal");
			return false;
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
	
	/*
	// *** block anchors by lore (portal)
	com.brandon3055.draconicevolution.blocks.DislocatorReceptacle
		list<int>    BCManagedData.CRYSTAL_LINK_POS (x y z to be clarified)
		list<int>    BCManagedData.CRYSTAL_POS (x y z to be clarified)
		list<int>    BCManagedData.SPAWN_POS (x y z to be clarified)
	com.brandon3055.draconicevolution.blocks.Portal
		list<int>    BCManagedData.masterPos (x y z offset from dislocator to this block)
	
	// *** blocks with only metadata
	com.brandon3055.draconicevolution.blocks.machines.FlowGate
		metadata    0 / 1 3 2 4 / 5 / (6 7 ?) / 8 / 9 11 10 12 / 13 / (14 15 ?)
	com.brandon3055.draconicevolution.blocks.machines.Generator
	com.brandon3055.draconicevolution.blocks.machines.Grinder
		metadata    0 1 5 3 4 2
	com.brandon3055.draconicevolution.blocks.Potentiometer
		metadata 0 / 1 3 2 4 / 5
	
	// *** blocks with just rotation
	com.brandon3055.draconicevolution.blocks.DislocatorPedestal
		int   BCManagedData.rotation int -7 to 8 clockwise => ((old + 8 + 4) % 16) - 8
	com.brandon3055.draconicevolution.blocks.DraconiumChest
		byte  BCManagedData.facing  0 1 5 3 4 2
	com.brandon3055.draconicevolution.blocks.PlacedItem
	    int   BCManagedData.rotation0 + 4 or -4 only when metadata is 0 or 1
		int   Facing 0 1 5 3 4 2
		metadata    0 1 5 3 4 2
	
	// *** blocks with rotation and position(s)
	com.brandon3055.draconicevolution.blocks.machines.CraftingInjector
		list<int>    BCManagedData.lastCorePos x y z (defaults to 0 0 0, absolute position)
	com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal
		int          BCManagedData.facing 0 1 5 3 4 2 (optional)
		list<byte[]> LinkedCrystals (x y z offset from another crystal to this block)
	com.brandon3055.draconicevolution.blocks.machines.EnergyStorageCore
		bool         BCManagedData.stabilizersOK 1 when offsets are valid
		list<int>    BCManagedData.stabOffset0/1/2/3 x y z (defaults to 0 -1 0, offset from stabilizer to this block)
	com.brandon3055.draconicevolution.blocks.machines.EnergyPylon
		list<int>    BCManagedData.coreOffset (defaults to 0 -1 0, x y z offset from core to this block)
		bool         BCManagedData.structureValid 1 when offsets are valid
	com.brandon3055.draconicevolution.blocks.InvisECoreBlock
		list<int>    BCManagedData.coreOffset (defaults to ? ? ?, x y z offset from core to this block)
	com.brandon3055.draconicevolution.blocks.ParticleGenerator
		list<int>    BCManagedData.coreOffset (defaults to 0 -1 0, x y z offset from core to this block)
		bool         BCManagedData.hasCoreLock 1 when offsets are valid
	com.brandon3055.draconicevolution.blocks.reactor.ReactorComponent
		list<int>    BCManagedData.coreOffset (defaults to 0 0 0, x y z offset from core to this block)
		bool         BCManagedData.isBound 1 when offset is valid
	com.brandon3055.draconicevolution.blocks.reactor.ReactorCore
		list<int>    BCManagedData.componentPosition0/1/2/3/4/5 (defaults to 0 0 0, x y z offset from component to this block)
		(0 to 5 are ordered like EnumFacing)
	*/
	
	//                                                   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final byte[] byteFacing          = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  intFacing           = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  rotFlowGate         = {  0,  3,  4,  2,  1,  5,  6,  7,  8, 11, 12, 10,  9, 13, 14, 15 };
	private static final int[]  rotPotentiometer    = {  0,  3,  4,  2,  1,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 && nbtTileEntity == null) {
			return metadata;
		}
		
		// *** blocks with only metadata
		// FlowGate
		if (classBlockFlowGate.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotFlowGate[metadata];
			case 2:
				return rotFlowGate[rotFlowGate[metadata]];
			case 3:
				return rotFlowGate[rotFlowGate[rotFlowGate[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Generator & Grinder
		if ( classBlockGenerator.isInstance(block)
		  || classBlockGrinder.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return intFacing[metadata];
			case 2:
				return intFacing[intFacing[metadata]];
			case 3:
				return intFacing[intFacing[intFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Potentiometer
		if (classBlockPotentiometer.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotPotentiometer[metadata];
			case 2:
				return rotPotentiometer[rotPotentiometer[metadata]];
			case 3:
				return rotPotentiometer[rotPotentiometer[rotPotentiometer[metadata]]];
			default:
				return metadata;
			}
		}
		
		
		final NBTTagCompound tagCompoundBCManagedData;
		if (nbtTileEntity != null && nbtTileEntity.hasKey("BCManagedData")) {
			tagCompoundBCManagedData = nbtTileEntity.getCompoundTag("BCManagedData");
		} else {
			tagCompoundBCManagedData = null;
		}
		
		// *** blocks with just rotation
		// Dislocator pedestal
		if (classBlockDislocatorPedestal.isInstance(block)) {
			if (tagCompoundBCManagedData == null) {
				return metadata;
			}
			if (rotationSteps > 0) {
				final int rotationOld = tagCompoundBCManagedData.getInteger("rotation");
				final int rotationNew = ((rotationOld + 8 + 4 * rotationSteps) % 16) - 8;
				tagCompoundBCManagedData.setInteger("rotation", rotationNew);
			}
			return metadata;
		}
		
		// Draconium chest
		if (classBlockDraconiumChest.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return rotPotentiometer[metadata];
			case 2:
				return rotPotentiometer[rotPotentiometer[metadata]];
			case 3:
				return rotPotentiometer[rotPotentiometer[rotPotentiometer[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Placed item rotates by metadata vertically, by NBT horizontally
		if (classBlockPlacedItem.isInstance(block)) {
			if (tagCompoundBCManagedData == null) {
				return metadata;
			}
			if (metadata == 0 || metadata == 1) {// placed horizontally
				final int rotationOld = tagCompoundBCManagedData.getInteger("rotation0");
				final int rotationNew;
				if (metadata == 0) {
					rotationNew = (rotationOld + 4 * rotationSteps) % 16;
				} else {
					rotationNew = (rotationOld + 12 * rotationSteps) % 16;
				}
				tagCompoundBCManagedData.setInteger("rotation0", rotationNew);
				return metadata;
			}
			
			// (placed vertically)
			final byte facing = nbtTileEntity.getByte("Facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setByte("Facing", byteFacing[facing]);
				return intFacing[metadata];
			case 2:
				nbtTileEntity.setByte("Facing", byteFacing[byteFacing[facing]]);
				return intFacing[intFacing[metadata]];
			case 3:
				nbtTileEntity.setByte("Facing", byteFacing[byteFacing[byteFacing[facing]]]);
				return intFacing[intFacing[intFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// from there on, we need BCManagedData, so skip the other blocks altogether
		if (tagCompoundBCManagedData == null) {
			return metadata;
		}
		
		// *** blocks with rotation and position(s)
		// common optional "facing" property for EnergyCrystal and ReactorComponent
		if (tagCompoundBCManagedData.hasKey("facing")) {
			final int facing = tagCompoundBCManagedData.getInteger("facing");
			switch (rotationSteps) {
			case 1:
				tagCompoundBCManagedData.setInteger("facing", intFacing[facing]);
				break;
			case 2:
				tagCompoundBCManagedData.setInteger("facing", intFacing[intFacing[facing]]);
				break;
			case 3:
				tagCompoundBCManagedData.setInteger("facing", intFacing[intFacing[intFacing[facing]]]);
				break;
			default:
				break;
			}
		}
		
		// common optional "CoreDirection" property for ParticleGenerator
		if (tagCompoundBCManagedData.hasKey("CoreDirection")) {
			final int facing = tagCompoundBCManagedData.getInteger("CoreDirection");
			switch (rotationSteps) {
			case 1:
				tagCompoundBCManagedData.setInteger("CoreDirection", intFacing[facing]);
				break;
			case 2:
				tagCompoundBCManagedData.setInteger("CoreDirection", intFacing[intFacing[facing]]);
				break;
			case 3:
				tagCompoundBCManagedData.setInteger("CoreDirection", intFacing[intFacing[intFacing[facing]]]);
				break;
			default:
				break;
			}
		}
		
		// absolute coordinate "lastCorePos" for CraftingInjector
		if (tagCompoundBCManagedData.hasKey("lastCorePos")) {
			final NBTTagList tagListLastCorePos = tagCompoundBCManagedData.getTagList("lastCorePos", NBT.TAG_INT);
			// There's "isValid" flag and it's an absolute coordinate defaulting to 0 0 0.
			// After jump, it would load 'random' chunks, so we're checking if it's inside the ship before transforming.
			// Position can be far outside the ship, so we'll reset to default if it's outside the ship.
			final int x = tagListLastCorePos.getIntAt(0);
			final int y = tagListLastCorePos.getIntAt(1);
			final int z = tagListLastCorePos.getIntAt(2);
			if (transformation.isInside(x, y, z)) {
				final BlockPos targetLink = transformation.apply(x, y, z);
				tagListLastCorePos.set(0, new NBTTagInt(targetLink.getX()));
				tagListLastCorePos.set(1, new NBTTagInt(targetLink.getY()));
				tagListLastCorePos.set(2, new NBTTagInt(targetLink.getZ()));
			} else {
				tagListLastCorePos.set(0, new NBTTagInt(0));
				tagListLastCorePos.set(1, new NBTTagInt(0));
				tagListLastCorePos.set(2, new NBTTagInt(0));
			}
		}
		
		// from now on we're transforming relative coordinates, so we'll need the block old and new coordinates of this block
		final BlockPos blockPosOld = new BlockPos(
				nbtTileEntity.getInteger("x"),
				nbtTileEntity.getInteger("y"),
				nbtTileEntity.getInteger("z"));
		final BlockPos blockPosNew = transformation.apply(blockPosOld);
		
		// EnergyCrystal
		if (nbtTileEntity.hasKey("LinkedCrystals")) {
			final NBTTagList tagListOldLinkedCrystals = nbtTileEntity.getTagList("LinkedCrystals", NBT.TAG_BYTE_ARRAY);
			final int countLinks = tagListOldLinkedCrystals.tagCount();
			if (countLinks > 0) {
				final NBTTagList tagListNewLinkedCrystals = new NBTTagList();
				for (int index = 0; index < countLinks; index++) {
					final NBTTagByteArray listLinkedCrystal = (NBTTagByteArray) tagListOldLinkedCrystals.get(index);
					final byte[] byteLink = listLinkedCrystal.getByteArray();
					final int x = blockPosOld.getX() - byteLink[0];
					final int y = blockPosOld.getY() - byteLink[1];
					final int z = blockPosOld.getZ() - byteLink[2];
					if (transformation.isInside(x, y, z)) {
						final BlockPos targetLink = transformation.apply(x, y, z);
						byteLink[0] = (byte) (blockPosNew.getX() - targetLink.getX());
						byteLink[1] = (byte) (blockPosNew.getY() - targetLink.getY());
						byteLink[2] = (byte) (blockPosNew.getZ() - targetLink.getZ());
						tagListNewLinkedCrystals.appendTag(listLinkedCrystal);
					} else {// (outside ship)
						// remove the link
						byteLink[0] = (byte) 0;
						byteLink[1] = (byte) 0;
						byteLink[2] = (byte) 0;
					}
				}
				nbtTileEntity.setTag("LinkedCrystals", tagListNewLinkedCrystals);
			}
		}
		
		// EnergyStorageCore
		if (tagCompoundBCManagedData.getBoolean("stabilizersOK")) {
			for (int index = 0; index < 4; index++) {
				final String tagName = String.format("stabOffset%d", index);
				final NBTTagList tagListOffset = tagCompoundBCManagedData.getTagList(tagName, NBT.TAG_INT);
				final int x = blockPosOld.getX() - tagListOffset.getIntAt(0);
				final int y = blockPosOld.getY() - tagListOffset.getIntAt(1);
				final int z = blockPosOld.getZ() - tagListOffset.getIntAt(2);
				if (transformation.isInside(x, y, z)) {
					final BlockPos targetStabilizer = transformation.apply(x, y, z);
					tagListOffset.set(0, new NBTTagInt(blockPosNew.getX() - targetStabilizer.getX()));
					tagListOffset.set(1, new NBTTagInt(blockPosNew.getY() - targetStabilizer.getY()));
					tagListOffset.set(2, new NBTTagInt(blockPosNew.getZ() - targetStabilizer.getZ()));
				} else {// (outside ship)
					// remove the link
					tagListOffset.set(0, new NBTTagInt(0));
					tagListOffset.set(1, new NBTTagInt(0));
					tagListOffset.set(2, new NBTTagInt(0));
				}
			}
		}
		
		// EnergyPylon, InvisECoreBlock, ParticleGenerator, ReactorComponent
		if (tagCompoundBCManagedData.hasKey("coreOffset")) {
			final NBTTagList tagListOffset = tagCompoundBCManagedData.getTagList("coreOffset", NBT.TAG_INT);
			if ( tagCompoundBCManagedData.getBoolean("structureValid")
			  || classBlockInvisECoreBlock.isInstance(block)
			  || tagCompoundBCManagedData.getBoolean("hasCoreLock")
			  || tagCompoundBCManagedData.getBoolean("isBound") ) {
				final int x = blockPosOld.getX() - tagListOffset.getIntAt(0);
				final int y = blockPosOld.getY() - tagListOffset.getIntAt(1);
				final int z = blockPosOld.getZ() - tagListOffset.getIntAt(2);
				if (transformation.isInside(x, y, z)) {
					final BlockPos targetStabilizer = transformation.apply(x, y, z);
					tagListOffset.set(0, new NBTTagInt(blockPosNew.getX() - targetStabilizer.getX()));
					tagListOffset.set(1, new NBTTagInt(blockPosNew.getY() - targetStabilizer.getY()));
					tagListOffset.set(2, new NBTTagInt(blockPosNew.getZ() - targetStabilizer.getZ()));
				} else {// (outside ship)
					// remove the link
					tagListOffset.set(0, new NBTTagInt(0));
					tagListOffset.set(1, new NBTTagInt(0));
					tagListOffset.set(2, new NBTTagInt(0));
				}
			} else {// (not bound or invalid)
				// remove the link
				tagListOffset.set(0, new NBTTagInt(0));
				tagListOffset.set(1, new NBTTagInt(0));
				tagListOffset.set(2, new NBTTagInt(0));
			}
		}
		
		// ReactorCore
		if (tagCompoundBCManagedData.hasKey("componentPosition0")) {
			final HashMap<String, NBTTagList> mapNewPosition = new HashMap<>(6);
			
			for (int facing = 0; facing < 6; facing++) {
				// rotate the key name
				final String tagOldName = String.format("componentPosition%d", facing);
				final String tagNewName;
				switch (rotationSteps) {
				case 1:
					tagNewName = String.format("componentPosition%d", intFacing[facing]);
					break;
				case 2:
					tagNewName = String.format("componentPosition%d", intFacing[intFacing[facing]]);
					break;
				case 3:
					tagNewName = String.format("componentPosition%d", intFacing[intFacing[intFacing[facing]]]);
					break;
				default:
					tagNewName = tagOldName;
					break;
				}
				
				// get current offset
				final NBTTagList tagListOffset = tagCompoundBCManagedData.getTagList(tagOldName, NBT.TAG_INT);
				
				// transform as needed
				if ( tagListOffset.getIntAt(0) != 0
				  || tagListOffset.getIntAt(1) != 0
				  || tagListOffset.getIntAt(2) != 0 ) {
					final int x = blockPosOld.getX() - tagListOffset.getIntAt(0);
					final int y = blockPosOld.getY() - tagListOffset.getIntAt(1);
					final int z = blockPosOld.getZ() - tagListOffset.getIntAt(2);
					if (transformation.isInside(x, y, z)) {
						final BlockPos targetComponent = transformation.apply(x, y, z);
						tagListOffset.set(0, new NBTTagInt(blockPosNew.getX() - targetComponent.getX()));
						tagListOffset.set(1, new NBTTagInt(blockPosNew.getY() - targetComponent.getY()));
						tagListOffset.set(2, new NBTTagInt(blockPosNew.getZ() - targetComponent.getZ()));
					} else {// (outside ship)
						// remove the link
						tagListOffset.set(0, new NBTTagInt(0));
						tagListOffset.set(1, new NBTTagInt(0));
						tagListOffset.set(2, new NBTTagInt(0));
					}
				}
				
				// save the new value
				mapNewPosition.put(tagNewName, tagListOffset);
				tagCompoundBCManagedData.removeTag(tagOldName);
			}
			
			// apply the new position
			for (final Entry<String, NBTTagList> entry : mapNewPosition.entrySet()) {
				tagCompoundBCManagedData.setTag(entry.getKey(), entry.getValue());
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
