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

public class CompatCustomNPCs implements IBlockTransformer {
	
	// CustomNPCs
	private static Class<?> classBlockBorder;
	private static Class<?> classBlockCarpentryBench;
	private static Class<?> classBlockBuilder;
	private static Class<?> classBlockMailbox;
	private static Class<?> classBlockNpcRedstone;
	
	public static void register() {
		try {
			// customNPC
			classBlockBorder         = Class.forName("noppes.npcs.blocks.BlockBorder");
			classBlockBuilder        = Class.forName("noppes.npcs.blocks.BlockBuilder");
			classBlockCarpentryBench = Class.forName("noppes.npcs.blocks.BlockCarpentryBench");
			classBlockMailbox        = Class.forName("noppes.npcs.blocks.BlockMailbox");
			classBlockNpcRedstone    = Class.forName("noppes.npcs.blocks.BlockNpcRedstone");
			
			WarpDriveConfig.registerBlockTransformer("CustomNPCs", new CompatCustomNPCs());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockBorder.isInstance(block)
		    || classBlockBuilder.isInstance(block)
		    || classBlockCarpentryBench.isInstance(block)
		    || classBlockMailbox.isInstance(block)
		    || classBlockNpcRedstone.isInstance(block);
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
	
	// Transformation handling required as of CustomNPCs_1.12.2(30Jan19):
	// noppes.npcs.blocks.BlockInterface
	//      noppes.npcs.blocks.BlockBorder
	//          meta    ROTATION 0 1 2 3
	//          int     BorderRotation  0 1 2 3
	//      noppes.npcs.blocks.BlockBuilder
	//          meta    ROTATION 0 1 2 3
	//          int     Rotation (relative to block => ignore it?)
	//      noppes.npcs.blocks.BlockCarpentryBench
	//          meta    ROTATION 0 1 2 3
	//      noppes.npcs.blocks.BlockCopy
	//          meta    -none-
	//      noppes.npcs.blocks.BlockMailbox
	//          meta    ROTATION 0 1 2 3 | TYPE 0 4 8
	//      noppes.npcs.BlockNpcRedstone
	//          meta    -none-
	//          int     BlockOnRangeX/BlockOnRangeZ
	//          int     BlockOffRangeX/BlockOffRangeZ
	//      noppes.npcs.blocks.BlockWaypoint
	//          meta    -none-
	
	// BlockDoor
	//      noppes.npcs.blocks.BlockNpcDoorInterface
	//          noppes.npcs.blocks.BlockScriptedDoor
	//              meta    (same as vanilla)
	
	
	// -----------------------------------------          {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrot4                  = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotMailbox            = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if ( nbtTileEntity != null
		  && nbtTileEntity.hasKey("BorderRotation") ) {
			final int BorderRotation = nbtTileEntity.getInteger("BorderRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("BorderRotation", mrot4[BorderRotation]);
				break;
			case 2:
				nbtTileEntity.setInteger("BorderRotation", mrot4[mrot4[BorderRotation]]);
				break;
			case 3:
				nbtTileEntity.setInteger("BorderRotation", mrot4[mrot4[mrot4[BorderRotation]]]);
				break;
			default:
				break;
			}
		}
		
		if ( nbtTileEntity != null
		  && nbtTileEntity.hasKey("BlockOnRangeX") ) {
			final int BlockOnRangeX = nbtTileEntity.getInteger("BlockOnRangeX");
			final int BlockOnRangeZ = nbtTileEntity.getInteger("BlockOnRangeZ");
			final int BlockOffRangeX = nbtTileEntity.getInteger("BlockOffRangeX");
			final int BlockOffRangeZ = nbtTileEntity.getInteger("BlockOffRangeZ");
			switch (rotationSteps) {
			case 1:
			case 3:
				nbtTileEntity.setInteger("BlockOnRangeX", BlockOnRangeZ);
				nbtTileEntity.setInteger("BlockOnRangeZ", BlockOnRangeX);
				nbtTileEntity.setInteger("BlockOffRangeX", BlockOffRangeZ);
				nbtTileEntity.setInteger("BlockOffRangeZ", BlockOffRangeX);
				break;
				
			default:
				break;
			}
		}
		
		
		if ( classBlockBorder.isInstance(block)
		  || classBlockBuilder.isInstance(block)
		  || classBlockCarpentryBench.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrot4[metadata];
			case 2:
				return mrot4[mrot4[metadata]];
			case 3:
				return mrot4[mrot4[mrot4[metadata]]];
			default:
				return metadata;
			}
		}
		if (classBlockMailbox.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotMailbox[metadata];
			case 2:
				return mrotMailbox[mrotMailbox[metadata]];
			case 3:
				return mrotMailbox[mrotMailbox[mrotMailbox[metadata]]];
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
