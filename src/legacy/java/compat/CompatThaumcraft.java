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

public class CompatThaumcraft implements IBlockTransformer {
	
	private static Class<?> interfaceIBlockFacing;
	private static Class<?> interfaceIBlockFacingHorizontal;
	
	private static Class<?> classBlockPillar;
	private static Class<?> classBlockChestHungry;
	
	private static Class<?> classBlockBannerTC;
	private static Class<?> classBlockMirror;
	private static Class<?> classBlockAlembic;
	private static Class<?> classBlockJar;
	private static Class<?> classBlockTube;
	
	public static void register() {
		try {
			interfaceIBlockFacing = Class.forName("thaumcraft.common.blocks.IBlockFacing");
			interfaceIBlockFacingHorizontal = Class.forName("thaumcraft.common.blocks.IBlockFacingHorizontal");
			
			classBlockPillar = Class.forName("thaumcraft.common.blocks.basic.BlockPillar");
			classBlockChestHungry = Class.forName("thaumcraft.common.blocks.devices.BlockHungryChest");
			
			classBlockBannerTC = Class.forName("thaumcraft.common.blocks.basic.BlockBannerTC");
			classBlockMirror = Class.forName("thaumcraft.common.blocks.devices.BlockMirror");
			classBlockAlembic = Class.forName("thaumcraft.common.blocks.essentia.BlockAlembic");
			classBlockJar = Class.forName("thaumcraft.common.blocks.essentia.BlockJar");
			classBlockTube = Class.forName("thaumcraft.common.blocks.essentia.BlockTube");
			
			WarpDriveConfig.registerBlockTransformer("thaumcraft", new CompatThaumcraft());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return interfaceIBlockFacing.isInstance(block)
			|| interfaceIBlockFacingHorizontal.isInstance(block)
		    || classBlockPillar.isInstance(block)
		    || classBlockChestHungry.isInstance(block)
		    || classBlockBannerTC.isInstance(block)
		    || classBlockMirror.isInstance(block)
		    || classBlockAlembic.isInstance(block)
		    || classBlockJar.isInstance(block)
		    || classBlockTube.isInstance(block);
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
	
	// As of 1.12.2-6.1.BETA24
	// Blocks
	// IBlockFacing                       (metadata) -EnumFacing-                   thaumcraft.common.blocks.IBlockFacing
	// IBlockFacingHorizontal             (metadata) -EnumFacing-                   thaumcraft.common.blocks.IBlockFacingHorizontal
	// BlockPillar                        (metadata) -EnumFacing-                   thaumcraft.common.blocks.basic.BlockPillar
	// BlockHungryChest                   (metadata) -EnumFacing-                   thaumcraft.common.blocks.devices.BlockHungryChest
	//
	// TileEntities
	// TileAlembic                        facing (byte) -EnumFacing-                thaumcraft.common.blocks.essentia.BlockAlembic
	// TileBanner                         facing (byte) -EnumFacing-                thaumcraft.common.blocks.basic.BlockBannerTC
	// TileJarFillable                    facing (byte) -EnumFacing-                thaumcraft.common.blocks.essentia.BlockJar
	// TileMirror, TileMirrorEssentia     linkX/Y/Z, linkDim (int)                  thaumcraft.common.blocks.devices.BlockMirror
	// TileTube, TileTubeBuffer           side (int) -EnumFacing-                   thaumcraft.common.blocks.essentia.BlockTube
	//
	// Entities
	// EntityArcaneBore                   faceing (byte)                            thaumcraft.common.entities.construct.EntityArcaneBore
	// SealHarvest ?                      taskface (byte)                           thaumcraft:harvest
	// SealEntity ?                       face (byte)
	//
	// Items
	// ItemHandMirror                     linkX/Y/Z, linkDim (int)                  thaumcraft.common.items.tools.ItemHandMirror
	// BlockMirrorItem                    linkX/Y/Z, linkDim (int)                  thaumcraft.common.blocks.devices.BlockMirrorItem
	
	// As of 1.7.10-x
	// Vanilla supported: stairs
	// Not rotating: arcane workbench, deconstruction table, crystals, candles, crucible, alchemical centrifuge
	
	// Transformation handling required:
	// Tile Hungry chest: (metadata) 2 5 3 4						mrotHungryChest thaumcraft.common.blocks.BlockChestHungry
	// Tile jar: facing (byte) 2 5 3 4								rotForgeByte	thaumcraft.common.blocks.BlockJar
	// Tile vis relay: orientation (short) 0 / 1 / 2 5 3 4			rotForgeShort	thaumcraft.common.blocks.BlockMetalDevice
	// Tile arcane lamp: orientation (int) 2 5 3 4					rotForgeInt		thaumcraft.common.blocks.BlockMetalDevice
	// Tile syphon (Arcane alembic): facing (byte) 2 5 3 4			rotForgeByte	thaumcraft.common.blocks.BlockMetalDevice
	// Tile mirror: (metadata) 0 / 1 / 2 5 3 4 / 6 / 7 / 8 11 9 10	mrotMirror		thaumcraft.common.blocks.BlockMirror
	// Tile mirror: linkX/Y/Z (int)									n/a				thaumcraft.common.blocks.BlockMirror
	// Tile table: (metadata) 0 1 / 2 5 3 4 / 6 9 7 8				mrotTable		thaumcraft.common.blocks.BlockTable
	// Tile tube, Tile tube valve: side (int) 0 / 1 / 2 5 3 4		rotForgeInt		thaumcraft.common.blocks.BlockTube
	// Tile essentia crystalizer: face (byte) 0 / 1 / 2 5 3 4		rotForgeByte	thaumcraft.common.blocks.BlockTube
	// Tile bellows: orientation (byte) 0 / 1 / 2 5 3 4				rotForgeByte	thaumcraft.common.blocks.BlockWoodenDevice
	// Tile arcane bore base: orientation (int) 2 5 3 4				rotForgeInt		thaumcraft.common.blocks.BlockWoodenDevice
	// Tile banner: facing (byte) 0 4 8 12							rotBanner		thaumcraft.common.blocks.BlockWoodenDevice
	
	// -----------------------------------------        {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   mrotFacingEnable     = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };
	private static final int[]   mrotFacingHorizontal = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[]  rotForgeByte         = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]   rotForgeInt          = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[]  rotBanner            = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if ( interfaceIBlockFacing.isInstance(block)
		  || interfaceIBlockFacingHorizontal.isInstance(block)
		  || classBlockChestHungry.isInstance(block) ) {
			switch (rotationSteps) {
			case 1:
				return mrotFacingEnable[metadata];
			case 2:
				return mrotFacingEnable[mrotFacingEnable[metadata]];
			case 3:
				return mrotFacingEnable[mrotFacingEnable[mrotFacingEnable[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (classBlockPillar.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotFacingHorizontal[metadata];
			case 2:
				return mrotFacingHorizontal[mrotFacingHorizontal[metadata]];
			case 3:
				return mrotFacingHorizontal[mrotFacingHorizontal[mrotFacingHorizontal[metadata]]];
			default:
				return metadata;
			}
		}
		
		if ( classBlockAlembic.isInstance(block)
		  || classBlockJar.isInstance(block) ) {
			if (nbtTileEntity.hasKey("facing")) {
				final short facing = nbtTileEntity.getByte("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("facing", rotForgeByte[facing]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("facing", rotForgeByte[rotForgeByte[facing]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("facing", rotForgeByte[rotForgeByte[rotForgeByte[facing]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		
		if (classBlockBannerTC.isInstance(block)) {
			if (nbtTileEntity.hasKey("facing")) {
				final short facing = nbtTileEntity.getByte("facing");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setByte("facing", rotBanner[facing]);
					return metadata;
				case 2:
					nbtTileEntity.setByte("facing", rotBanner[rotBanner[facing]]);
					return metadata;
				case 3:
					nbtTileEntity.setByte("facing", rotBanner[rotBanner[rotBanner[facing]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		
		if (classBlockTube.isInstance(block)) {
			if (nbtTileEntity.hasKey("side")) {
				final int side = nbtTileEntity.getInteger("side");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setInteger("side", rotForgeInt[side]);
					return metadata;
				case 2:
					nbtTileEntity.setInteger("side", rotForgeInt[rotForgeInt[side]]);
					return metadata;
				case 3:
					nbtTileEntity.setInteger("side", rotForgeInt[rotForgeInt[rotForgeInt[side]]]);
					return metadata;
				default:
					return metadata;
				}
			}
		}
		
		if (classBlockMirror.isInstance(block)) {
			if (nbtTileEntity.hasKey("linkX") && nbtTileEntity.hasKey("linkY") && nbtTileEntity.hasKey("linkZ") && nbtTileEntity.hasKey("linkDim")) {
				// final int dimensionId = nbtTileEntity.getInteger("linkDim");
				final BlockPos targetLink = transformation.apply(nbtTileEntity.getInteger("linkX"), nbtTileEntity.getInteger("linkY"), nbtTileEntity.getInteger("linkZ"));
				nbtTileEntity.setInteger("linkX", targetLink.getX());
				nbtTileEntity.setInteger("linkY", targetLink.getY());
				nbtTileEntity.setInteger("linkZ", targetLink.getZ());
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
