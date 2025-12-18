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

public class CompatParziStarWars implements IBlockTransformer {
	
	private static Class<?> classTileEntityRotate;
	private static Class<?> classTileEntityAncientJediStatue;
	private static Class<?> classTileEntityAntenna;
	private static Class<?> classTileEntityConsoleHoth1;
	private static Class<?> classTileEntityConsoleHoth2;
	private static Class<?> classTileEntityConsoleHoth3;
	private static Class<?> classTileEntityDeathStarDoor;
	private static Class<?> classTileEntityLadder;
	private static Class<?> classTileEntityLightsaberForge;
	private static Class<?> classTileEntityMV;
	
	public static void register() {
		try {
			classTileEntityRotate            = Class.forName("com.parzivail.util.block.TileEntityRotate");
			classTileEntityAncientJediStatue = Class.forName("com.parzivail.pswm.tileentities.TileEntityAncientJediStatue");
			classTileEntityAntenna           = Class.forName("com.parzivail.pswm.tileentities.TileEntityAntenna");
			classTileEntityConsoleHoth1      = Class.forName("com.parzivail.pswm.tileentities.TileEntityConsoleHoth1");
			classTileEntityConsoleHoth2      = Class.forName("com.parzivail.pswm.tileentities.TileEntityConsoleHoth2");
			classTileEntityConsoleHoth3      = Class.forName("com.parzivail.pswm.tileentities.TileEntityConsoleHoth3");
			classTileEntityDeathStarDoor     = Class.forName("com.parzivail.pswm.tileentities.TileEntityDeathStarDoor");
			classTileEntityLadder            = Class.forName("com.parzivail.pswm.tileentities.TileEntityLadder");
			classTileEntityLightsaberForge   = Class.forName("com.parzivail.pswm.tileentities.TileEntityLightsaberForge");
			classTileEntityMV                = Class.forName("com.parzivail.pswm.tileentities.TileEntityMV");
			
			WarpDriveConfig.registerBlockTransformer("starwarsmod", new CompatParziStarWars());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classTileEntityRotate            .isInstance(tileEntity)
		    || classTileEntityAncientJediStatue .isInstance(tileEntity)
		    || classTileEntityAntenna           .isInstance(tileEntity)
		    || classTileEntityConsoleHoth1      .isInstance(tileEntity)
		    || classTileEntityConsoleHoth2      .isInstance(tileEntity)
		    || classTileEntityConsoleHoth3      .isInstance(tileEntity)
		    || classTileEntityDeathStarDoor     .isInstance(tileEntity)
		    || classTileEntityLadder            .isInstance(tileEntity)
		    || classTileEntityLightsaberForge   .isInstance(tileEntity)
		    || classTileEntityMV                .isInstance(tileEntity) ;
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
		short/int facing	0 1 2 3
		com.parzivail.pswm.tileentities.TileEntityDeathStarDoor
		com.parzivail.pswm.tileentities.TileEntityLightsaberForge
		com.parzivail.pswm.tileentities.TileEntityConsoleHoth1
		com.parzivail.pswm.tileentities.TileEntityConsoleHoth2
		com.parzivail.pswm.tileentities.TileEntityConsoleHoth3
		com.parzivail.pswm.tileentities.TileEntityLadder
		com.parzivail.pswm.tileentities.TileEntityAntenna
		com.parzivail.pswm.tileentities.TileEntityAncientJediStatue
		com.parzivail.pswm.tileentities.TileEntityRotate
			TileEntityDoorHoth
			TileEntityFloorLight
			TileEntityFloorLight2
			TileEntityGunRack
			TileEntityHothCeilingLight2
			TileEntityMedicalConsole
			TileEntityMedicalConsole2
			TileEntityPanelHoth
			TileEntityPipeClampedMass
			TileEntityPipeDoubleOffsetBot
			TileEntityPipeDoubleOffsetBotSpecial
			TileEntityPipeDoubleOffsetTopSpecial
			TileEntityPipeDoubleOffsetTopSpecial
			TileEntityPipeMass
			TileEntityPipeSleevedMass
			TileEntityTarget
		
		
		int	facing	0 1 2 3 4 5 6 7
		TileEntityMV
		TileEntityFloorLight	(inherit from TileEntityRotate)
	 */
	private static final short[] mrot4 = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrot8 = {  0,  3,  4,  5,  6,  7,  8,  1,  2,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		if (nbtTileEntity.hasKey("facing")) {
			final String id = nbtTileEntity.getString("id");
			if ( id.equals("teFloorLight")
			  || id.equals("teMoistureVaporator") ) {
				final int facing = nbtTileEntity.getInteger("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("facing", mrot8[facing]);
					break;
				case 2:
					nbtTileEntity.setInteger("facing", mrot8[mrot8[facing]]);
					break;
				case 3:
					nbtTileEntity.setInteger("facing", mrot8[mrot8[mrot8[facing]]]);
					break;
				default:
					break;
				}
				
			} else {
				final short facing = nbtTileEntity.getShort("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setShort("facing", mrot4[facing]);
					break;
				case 2:
					nbtTileEntity.setShort("facing", mrot4[mrot4[facing]]);
					break;
				case 3:
					nbtTileEntity.setShort("facing", mrot4[mrot4[mrot4[facing]]]);
					break;
				default:
					break;
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
