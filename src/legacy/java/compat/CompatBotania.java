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

public class CompatBotania implements IBlockTransformer {
	
	private static Class<?> classBlockMod;
	private static Class<?> classBlockAvatar;
	private static Class<?> classBlockFelPumpkin;
	private static Class<?> classBlockSpecialFlower;
	private static Class<?> classBlockRedString;
	private static Class<?> classBlockTinyPotato;
	
	public static void register() {
		try {
			classBlockMod           = Class.forName("vazkii.botania.common.block.BlockMod");
			classBlockAvatar        = Class.forName("vazkii.botania.common.block.BlockAvatar");
			classBlockFelPumpkin    = Class.forName("vazkii.botania.common.block.BlockFelPumpkin");
			classBlockSpecialFlower = Class.forName("vazkii.botania.common.block.BlockSpecialFlower");
			classBlockRedString     = Class.forName("vazkii.botania.common.block.string.BlockRedString");
			classBlockTinyPotato    = Class.forName("vazkii.botania.common.block.decor.BlockTinyPotato");
			
			WarpDriveConfig.registerBlockTransformer("botania", new CompatBotania());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockMod.isInstance(block)
			|| classBlockSpecialFlower.isInstance(block);
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
	
	// -----------------------------------------    {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotFacing       = { 0, 1, 5, 4, 2, 3, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotFelPumpkin   = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if ( classBlockAvatar.isInstance(block)
		  || classBlockRedString.isInstance(block) ) {
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
		if ( classBlockFelPumpkin.isInstance(block)
		  || classBlockTinyPotato.isInstance(block) ) {// 0 1 2 3
			switch (rotationSteps) {
			case 1:
				return mrotFelPumpkin[metadata];
			case 2:
				return mrotFelPumpkin[mrotFelPumpkin[metadata]];
			case 3:
				return mrotFelPumpkin[mrotFelPumpkin[mrotFelPumpkin[metadata]]];
			default:
				return metadata;
			}
		}
		
		if ( nbtTileEntity != null
		  && nbtTileEntity.hasKey("bindX")
		  && nbtTileEntity.hasKey("bindY") 
		  && nbtTileEntity.hasKey("bindZ") ) {
			final BlockPos targetBind = transformation.apply(nbtTileEntity.getInteger("bindX"), nbtTileEntity.getInteger("bindY"), nbtTileEntity.getInteger("bindZ"));
			nbtTileEntity.setInteger("bindX", targetBind.getX());
			nbtTileEntity.setInteger("bindY", targetBind.getY());
			nbtTileEntity.setInteger("bindZ", targetBind.getZ());
		}
		
		if ( nbtTileEntity != null
		  && nbtTileEntity.hasKey("subTileCmp") ) {
			final NBTTagCompound nbtSubTileCmp = nbtTileEntity.getCompoundTag("subTileCmp");
			if ( nbtSubTileCmp.hasKey("collectorX")
			  && nbtSubTileCmp.hasKey("collectorY")
			  && nbtSubTileCmp.hasKey("collectorZ") ) {
				final BlockPos targetCollector = transformation.apply(nbtSubTileCmp.getInteger("collectorX"), nbtSubTileCmp.getInteger("collectorY"), nbtSubTileCmp.getInteger("collectorZ"));
				nbtSubTileCmp.setInteger("collectorX", targetCollector.getX());
				nbtSubTileCmp.setInteger("collectorY", targetCollector.getY());
				nbtSubTileCmp.setInteger("collectorZ", targetCollector.getZ());
			}
		}
		
		if ( nbtTileEntity != null
		  && nbtTileEntity.hasKey("rotationX") ) {
			final float rotationX = nbtTileEntity.getInteger("rotationX");
			nbtTileEntity.setFloat("rotationX", (rotationX + 270.0F * rotationSteps) % 360.0F);
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
