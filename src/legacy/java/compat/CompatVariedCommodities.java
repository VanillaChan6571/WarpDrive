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

public class CompatVariedCommodities implements IBlockTransformer {
	
	// Varied commodities
	private static Class<?> classBlockBasicContainer;
	private static Class<?> classBlockBlood;
	private static Class<?> classBlockCarpentryBench;
	
	public static void register() {
		try {
			// varied commodities
			classBlockBasicContainer = Class.forName("noppes.vc.blocks.BlockBasicContainer");
			classBlockBlood          = Class.forName("noppes.vc.blocks.BlockBlood");
			classBlockCarpentryBench = Class.forName("noppes.vc.blocks.BlockCarpentryBench");
			
			WarpDriveConfig.registerBlockTransformer("VariedCommodities", new CompatVariedCommodities());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockBasicContainer.isInstance(block);
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
	Transformation handling required:
	noppes.vc.blocks.BlockBasicContainer
		noppes.vc.blocks.BlockBanner / variedcommodities:banner
			int     BannerRotation  0 1 2 3
		noppes.vc.blocks.BlockBigSign / variedcommodities:big_sign
			int     SignRotation    0 1 2 3
		noppes.vc.blocks.BlockBlood / variedcommodities:blood_block
		    bool    HideNorth
		    bool    HideSouth
		    bool    HideEast
		    bool    HideWest
		    int     Rotation        0 1 2 3
		noppes.vc.blocks.BlockCarpentryBench / variedcommodities:carpentry_bench
			meta    0 1 2 3 / 4 5 6 7
		noppes.vc.blocks.BlockCouchWood / variedcommodities:couch_wood
			int     BannerRotation  0 1 2 3
		noppes.vc.blocks.BlockCouchWool / variedcommodities:couch_wool
			int     BannerRotation  0 1 2 3
		noppes.vc.blocks.BlockTallLamp / variedcommodities:tall_lamp
			int     BannerRotation  0 1 2 3
		noppes.vc.blocks.BlockTombstone / variedcommodities:tombstone
			int     SignRotation    0 1 2 3
		noppes.vc.blocks.BlockTrading / variedcommodities:trading_block
			int     BannerRotation  0 1 2 3
		noppes.vc.blocks.BlockWallBanner / variedcommodities:wall_banner
			int     BannerRotation  0 1 2 3
		
		noppes.vc.blocks.BlockBasicRotated
			noppes.vc.blocks.BlockBarrel / variedcommodities:barrel
				int     BannerRotation  0 2 4 6 / 1 3 5 7
			noppes.vc.blocks.BlockBasicLightable
				BlockCampfire / variedcommodities:campfire
					int     BannerRotation  0 1 2 3 4 5 6 7
				BlockCandle / variedcommodities:candle
					int     BannerRotation  0 1 2 3 4 5 6 7
				BlockLamp / variedcommodities:lamp
					int     BannerRotation  0 1 2 3 4 5 6 7
			noppes.vc.blocks.BlockBasicTrigger
				BlockPedestal / variedcommodities:pedestal
					int     BannerRotation  0 1 2 3
				BlockWeaponRack / variedcommodities:weapon_rack
					int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockBeam / variedcommodities:beam
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockBook / variedcommodities:book
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockChair / variedcommodities:chair
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockCrate / variedcommodities:crate
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockShelf / variedcommodities:shelf
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockSign / variedcommodities:sign
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockStool / variedcommodities:stool
				int     BannerRotation  0 1 2 3
			noppes.vc.blocks.BlockTable / variedcommodities:table                always 2 ?
				int     BannerRotation  0 1 2 3
	
	No handling required:
	noppes.vc.blocks.BlockCrystal
	noppes.vc.blocks.BlockPlaceholder
	
	*/
	
	// -----------------------------------------          {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotCarpentryBench     = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   rot4                   = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   rot8                   = {  2,  3,  4,  5,  6,  7,  0,  1,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return metadata;
		}
		
		// BannerRotation NBT with no metadata change
		if ( nbtTileEntity != null
		  && nbtTileEntity.hasKey("BannerRotation") ) {
			// get the rotation matrix
			final String idTileEntity = nbtTileEntity.getString("id");
			final int[] rot;
			switch(idTileEntity) {
			case "variedcommodities:barrel":
			case "variedcommodities:campfire":
			case "variedcommodities:candle":
			case "variedcommodities:lamp":
				rot = rot8;
				break;
				
			default:
				rot = rot4;
				break;
			}
			
			// apply
			final int BannerRotation = nbtTileEntity.getInteger("BannerRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("BannerRotation", rot[BannerRotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("BannerRotation", rot[rot[BannerRotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("BannerRotation", rot[rot[rot[BannerRotation]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// Carpentry bench is just metadata
		if (classBlockCarpentryBench.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotCarpentryBench[metadata];
			case 2:
				return mrotCarpentryBench[mrotCarpentryBench[metadata]];
			case 3:
				return mrotCarpentryBench[mrotCarpentryBench[mrotCarpentryBench[metadata]]];
			default:
				return metadata;
			}
		}
		
		// Signs
		if ( nbtTileEntity != null 
		  && nbtTileEntity.hasKey("SignRotation") ) {
			final int SignRotation = nbtTileEntity.getInteger("SignRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("SignRotation", rot4[SignRotation]);
				return metadata;
			case 2:
				nbtTileEntity.setInteger("SignRotation", rot4[rot4[SignRotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setInteger("SignRotation", rot4[rot4[rot4[SignRotation]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// Blood use compass directions
		if ( classBlockBlood.isInstance(block)
		  && nbtTileEntity != null ) {
			final boolean HideNorth = nbtTileEntity.getBoolean("HideNorth");
			final boolean HideEast  = nbtTileEntity.getBoolean("HideEast");
			final boolean HideSouth = nbtTileEntity.getBoolean("HideSouth");
			final boolean HideWest  = nbtTileEntity.getBoolean("HideWest");
			final int Rotation  = nbtTileEntity.getInteger("Rotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setBoolean("HideNorth", HideWest );
				nbtTileEntity.setBoolean("HideEast" , HideNorth);
				nbtTileEntity.setBoolean("HideSouth", HideEast );
				nbtTileEntity.setBoolean("HideWest" , HideSouth);
				nbtTileEntity.setInteger("Rotation", rot4[Rotation]);
				return metadata;
			case 2:
				nbtTileEntity.setBoolean("HideNorth", HideSouth);
				nbtTileEntity.setBoolean("HideEast" , HideWest );
				nbtTileEntity.setBoolean("HideSouth", HideNorth);
				nbtTileEntity.setBoolean("HideWest" , HideEast );
				nbtTileEntity.setInteger("Rotation", rot4[rot4[Rotation]]);
				return metadata;
			case 3:
				nbtTileEntity.setBoolean("HideNorth", HideEast );
				nbtTileEntity.setBoolean("HideEast" , HideSouth);
				nbtTileEntity.setBoolean("HideSouth", HideWest );
				nbtTileEntity.setBoolean("HideWest" , HideNorth);
				nbtTileEntity.setInteger("Rotation", rot4[rot4[rot4[Rotation]]]);
				return metadata;
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
