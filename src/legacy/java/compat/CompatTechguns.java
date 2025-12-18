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

public class CompatTechguns implements IBlockTransformer {
	
	private static Class<?> classBlockTGDoor3x3;
	
	private static Class<?> classBlockBasicMachine;
	private static Class<?> classBlockExplosiveCharge;
	private static Class<?> classBlockSimpleMachine;
	private static Class<?> classBlockMultiBlockMachine;
	
	public static void register() {
		try {
			classBlockTGDoor3x3 = Class.forName("techguns.blocks.BlockTGDoor3x3");
			
			classBlockBasicMachine = Class.forName("techguns.blocks.machines.BasicMachine");
			classBlockExplosiveCharge = Class.forName("techguns.blocks.machines.BlockExplosiveCharge");
			classBlockSimpleMachine = Class.forName("techguns.blocks.machines.SimpleMachine");
			classBlockMultiBlockMachine = Class.forName("techguns.blocks.machines.MultiBlockMachine");
			
			WarpDriveConfig.registerBlockTransformer("Techguns", new CompatTechguns());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockTGDoor3x3.isInstance(block)
		    || classBlockBasicMachine.isInstance(block);
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
	
	// Transformation using default handler:
	//
	// Block ladder: (metadata) 0 4 8 12 / 1 5 9 13 / 2 6 10 14 / 3 7 11 15             mrotLadder              techguns.blocks.BlockTGLadder
	// Block lamp: (metadata) 0 / 1 / 2 5 3 4 / 6 / 7 / 8 11 9 10 / 12 / 13             mrotLamp                techguns.blocks.BlockTGLamp
	
	// Transformation handling required:
	//
	// Block 3x3 Door: (metadata) 0 4 / 8 12                                            mrotDoor3x3             techguns.blocks.BlockTGDoor3x3
	//
	// Basic machine and its inherited
	// Block basicmachine (metadata 0 1 2): rotation (byte) 0 1 2 3                     rotBasicMachine         techguns.blocks.machines.BasicMachine
	// Block basicmachine (metadata 3): turretDeath (boolean) false                     (forcing turret death to resync the entity)
	// Block explosive_charge: (metadata) 0 / 1 / 2 / 3 / 4 10 6 8 / 5 11 7 9           mrotExplosiveCharge     techguns.blocks.machines.BlockExplosiveCharge
	// Block simplemachine1/2: (metadata) 0 4 8 12 / 1 5 9 13 / 2 6 10 14 / 3 7 11 15   mrotSimpleMachine       techguns.blocks.machines.SimpleMachine
	//
	// Block multiblockmachine:                                                                                 techguns.blocks.machines.MultiBlockMachine
	// Fabricator & Reaction chamber parts (metadata 0/1/2/3/4/5): not formed
	// Fabricator slave (metadata 8 / 9): masterX/Y/Z (integer) as absolute coordinates
	// Fabricator master (metadata 10): multiblockDirection (byte) 2 5 3 4              rotMultiblockDirection
	// Reaction chamber slave (metadata 11 / 12): masterX/Y/Z (integer) as absolute coordinates
	// Reaction chamber master (metadata 13): multiblockDirection (byte) 2 5 3 4        rotMultiblockDirection
	//
	// Block oredrill:
	// Ore drill slave (metadata 0/1/2/3): not formed
	// Ore drill master (metadata 4): not formed
	// Ore drill slave (metadata 8/9/10/11/13): masterX/Y/Z (integer) as absolute coordinates
	// Ore drill master (metadata 12): multiblockDirection (byte) 5 ?                   rotMultiblockDirection
	
	// -----------------------------------------          {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotExplosiveCharge    = {  0,  1,  2,  3, 10, 11,  8,  9,  4,  5,  6,  7, 12, 13, 14, 15 };
	private static final int[]   mrotDoor3x3            = {  4,  1,  2,  3,  0,  5,  6,  7, 12,  9, 10, 11,  8, 13, 14, 15 };
	private static final byte[]  rotBasicMachine        = {  1,  2,  3,  0 };
	private static final int[]   mrotSimpleMachine      = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };
	private static final byte[]  rotMultiblockDirection = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 14, 13, 11, 12, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if (classBlockTGDoor3x3.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotDoor3x3[metadata];
			case 2:
				return mrotDoor3x3[mrotDoor3x3[metadata]];
			case 3:
				return mrotDoor3x3[mrotDoor3x3[mrotDoor3x3[metadata]]];
			default:
				return metadata;
			}
		}
		
		// BasicMachine is parent class for other blocks with different rotations, hence we need to exclude the latest 
		if ( classBlockBasicMachine.isInstance(block)
		  && !classBlockExplosiveCharge.isInstance(block)
		  && !classBlockSimpleMachine.isInstance(block)
		  && !classBlockMultiBlockMachine.isInstance(block)
		  && nbtTileEntity != null ) {
			if (nbtTileEntity.hasKey("rotation")) {
				final byte rotation =  nbtTileEntity.getByte("rotation");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("rotation", rotBasicMachine[rotation]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("rotation", rotBasicMachine[rotBasicMachine[rotation]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("rotation", rotBasicMachine[rotBasicMachine[rotBasicMachine[rotation]]]);
					return metadata;
				default:
					return metadata;
				}
			}
			
			// repair turret on jump to ensure it's visible
			if (nbtTileEntity.hasKey("turretDeath")) {
				nbtTileEntity.setBoolean("turretDeath", true);
				nbtTileEntity.setInteger("repairTime", 0);
			}
		}
		
		if (classBlockExplosiveCharge.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotExplosiveCharge[metadata];
			case 2:
				return mrotExplosiveCharge[mrotExplosiveCharge[metadata]];
			case 3:
				return mrotExplosiveCharge[mrotExplosiveCharge[mrotExplosiveCharge[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockSimpleMachine.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotSimpleMachine[metadata];
			case 2:
				return mrotSimpleMachine[mrotSimpleMachine[metadata]];
			case 3:
				return mrotSimpleMachine[mrotSimpleMachine[mrotSimpleMachine[metadata]]];
			default:
				return metadata;
			}
		}
		
		if ( classBlockMultiBlockMachine.isInstance(block)
		  && nbtTileEntity != null ) {
			// Reference to the master block for slave
			// or Rotation for the Master block
			if ( nbtTileEntity.hasKey("hasMaster")
			  && nbtTileEntity.getBoolean("hasMaster") ) {
				final int xMaster = nbtTileEntity.getInteger("masterX");
				final int yMaster = nbtTileEntity.getInteger("masterY");
				final int zMaster = nbtTileEntity.getInteger("masterZ");
				final BlockPos chunkCoordinatesMaster = transformation.apply(xMaster, yMaster, zMaster);
				nbtTileEntity.setInteger("masterX", chunkCoordinatesMaster.getX());
				nbtTileEntity.setInteger("masterY", chunkCoordinatesMaster.getY());
				nbtTileEntity.setInteger("masterZ", chunkCoordinatesMaster.getZ());
				
			} else if (nbtTileEntity.hasKey("multiblockDirection")) {
				final byte rotation = nbtTileEntity.getByte("multiblockDirection");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("multiblockDirection", rotMultiblockDirection[rotation]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("multiblockDirection", rotMultiblockDirection[rotMultiblockDirection[rotation]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("multiblockDirection", rotMultiblockDirection[rotMultiblockDirection[rotMultiblockDirection[rotation]]]);
					return metadata;
				default:
					return metadata;
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
