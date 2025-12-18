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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatComputerCraft implements IBlockTransformer {
	
	private static Class<?> classBlockGeneric;
	
	private static Class<?> classBlockAdvancedModem;
	private static Class<?> classBlockComputerBase;
	private static Class<?> classBlockCable;
	private static Class<?> classBlockPeripheral;
	private static Class<?> classBlockTurtle;
	private static Class<?> classBlockWiredModemFull;
	
	public static void register(final boolean isCCTweakedLoaded) {
		try {
			// several changes are introduced in the CC-Tweaked fork, notably:
			// - non-ticking tile entities are separated
			// - BlockCable changed location
			if (!isCCTweakedLoaded) {// original mod
				WarpDrive.logger.info("Loading ComputerCraft compatibility with its original flavor...");
				classBlockGeneric        = Class.forName("dan200.computercraft.shared.common.BlockGeneric");
				
				classBlockAdvancedModem  = Class.forName("dan200.computercraft.shared.peripheral.modem.BlockAdvancedModem");
				classBlockComputerBase   = Class.forName("dan200.computercraft.shared.computer.blocks.BlockComputerBase");
				classBlockCable          = Class.forName("dan200.computercraft.shared.peripheral.common.BlockCable");
				classBlockPeripheral     = Class.forName("dan200.computercraft.shared.peripheral.common.BlockPeripheral");
				classBlockTurtle         = Class.forName("dan200.computercraft.shared.turtle.blocks.BlockTurtle");
				classBlockWiredModemFull = null;
				
			} else {// CC-Tweaked fork
				WarpDrive.logger.info("Loading ComputerCraft compatibility with its CC-Tweaked fork...");
				classBlockGeneric        = Class.forName("dan200.computercraft.shared.common.BlockGeneric");
				
				classBlockAdvancedModem  = Class.forName("dan200.computercraft.shared.peripheral.modem.wireless.BlockAdvancedModem");
				classBlockComputerBase   = Class.forName("dan200.computercraft.shared.computer.blocks.BlockComputerBase");
				classBlockCable          = Class.forName("dan200.computercraft.shared.peripheral.modem.wired.BlockCable");
				classBlockPeripheral     = Class.forName("dan200.computercraft.shared.peripheral.common.BlockPeripheral");
				classBlockTurtle         = Class.forName("dan200.computercraft.shared.turtle.blocks.BlockTurtle");
				classBlockWiredModemFull = Class.forName("dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull");
			}
			
			WarpDriveConfig.registerBlockTransformer("computercraft", new CompatComputerCraft());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockGeneric.isInstance(block)
		    && (classBlockWiredModemFull == null || !classBlockWiredModemFull.isInstance(block));
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
	
	//                                                0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17
	// computer rotations: normal (2-5), advanced (8-13)
	private static final int[] mrotComputer      = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };
	// peripheral rotations: wireless modem (0-1/6-9), disk drive (2-5), monitor (10/12), printer (11), speaker (13)
	private static final int[] mrotPeripheral    = {  0,  1,  5,  4,  2,  3,  9,  8,  6,  7, 10, 11, 12, 13, 14, 15 };
	// cable rotations: wired modem (0-5), with cable (6-11), just cable (13)
	private static final int[] mrotWiredModem    = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 };
	// advanced modem rotations: wired modem (0-5), with cable (6-11), just cable (13)
	private static final int[] mrotAdvancedModem = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	// NBT rotations for monitor, printer, speaker and turtles
	// printer, speaker and turtle rotation is 2 5 3 4
	// monitor rotation is 2 5 3 4 / 8 11 9 10 / 14 17 15 16
	private static final int[] rotDir            = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 17, 16, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// computers are rotating with metadata only
		if ( classBlockComputerBase.isInstance(block)
		  && !classBlockTurtle.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrotComputer[metadata];
			case 2:
				return mrotComputer[mrotComputer[metadata]];
			case 3:
				return mrotComputer[mrotComputer[mrotComputer[metadata]]];
			default:
				return metadata;
			}
		}
		
		// cables are rotating with metadata only
		if (classBlockCable.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotWiredModem[metadata];
			case 2:
				return mrotWiredModem[mrotWiredModem[metadata]];
			case 3:
				return mrotWiredModem[mrotWiredModem[mrotWiredModem[metadata]]];
			default:
				return metadata;
			}
		}
		
		// advanced modems are rotating with metadata only
		if (classBlockAdvancedModem.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotAdvancedModem[metadata];
			case 2:
				return mrotAdvancedModem[mrotAdvancedModem[metadata]];
			case 3:
				return mrotAdvancedModem[mrotAdvancedModem[mrotAdvancedModem[metadata]]];
			default:
				return metadata;
			}
		}
		
		// disk drive, wireless modem, monitor, printer are over optimized...
		if (classBlockPeripheral.isInstance(block)) {
			// disk drive and wireless modem are rotating with metadata only
			if ( metadata >= 0
			  && metadata <= 9 ) {
				switch (rotationSteps) {
				case 1:
					return mrotPeripheral[metadata];
				case 2:
					return mrotPeripheral[mrotPeripheral[metadata]];
				case 3:
					return mrotPeripheral[mrotPeripheral[mrotPeripheral[metadata]]];
				default:
					return metadata;
				}
			}
			
			// monitor, printer and speaker are rotating with NBT only through the dir tag
			if (!nbtTileEntity.hasKey("dir")) {// unknown
				WarpDrive.logger.error(String.format("Unknown ComputerCraft Peripheral block %s with metadata %d and tile entity %s",
				                                     block, metadata, nbtTileEntity));
				return metadata;
			}
			
		} else if (!nbtTileEntity.hasKey("dir")) {// unknown
			WarpDrive.logger.error(String.format("Unknown ComputerCraft directional block %s with metadata %d and tile entity %s",
			                                     block, metadata, nbtTileEntity));
			return metadata;
		}
		
		// turtles and others
		final int dir = nbtTileEntity.getInteger("dir");
		switch (rotationSteps) {
		case 1:
			nbtTileEntity.setInteger("dir", rotDir[dir]);
			return metadata;
		case 2:
			nbtTileEntity.setInteger("dir", rotDir[rotDir[dir]]);
			return metadata;
		case 3:
			nbtTileEntity.setInteger("dir", rotDir[rotDir[rotDir[dir]]]);
			return metadata;
		default:
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
