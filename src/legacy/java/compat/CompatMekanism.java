package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CompatMekanism implements IBlockTransformer {
	
	private static Class<?> tileEntityBasicBlock;
	private static Class<?> tileEntityBoundingBlock;
	private static Class<?> tileEntityGlowPanel;
	private static Class<?> tileEntitySidedPipe;
	
	public static void register() {
		try {
			tileEntityBasicBlock = Class.forName("mekanism.common.tile.prefab.TileEntityBasicBlock");
			tileEntityBoundingBlock = Class.forName("mekanism.common.tile.TileEntityBoundingBlock");
			// (not needed: mekanism.common.tile.TileEntityCardboardBox)
			tileEntityGlowPanel = Class.forName("mekanism.common.tile.TileEntityGlowPanel");
			tileEntitySidedPipe = Class.forName("mekanism.common.tile.transmitter.TileEntitySidedPipe");
			
			WarpDriveConfig.registerBlockTransformer("Mekanism", new CompatMekanism());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return tileEntityBasicBlock.isInstance(tileEntity)
		    || tileEntityBoundingBlock.isInstance(tileEntity)
		    || tileEntityGlowPanel.isInstance(tileEntity)
		    || tileEntitySidedPipe.isInstance(tileEntity);
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
	
	private static final int[] rotFacing           = {  0,  1,  5,  4,  2,  3 };
	
	private static final Map<String, String> rotConnectionNames;
	static {
		final Map<String, String> map = new HashMap<>();
		map.put("connection2", "connection5");
		map.put("connection5", "connection3");
		map.put("connection3", "connection4");
		map.put("connection4", "connection2");
		rotConnectionNames = Collections.unmodifiableMap(map);
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// basic blocks
		if (nbtTileEntity.hasKey("facing")) {
			final int facing = nbtTileEntity.getInteger("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("facing", rotFacing[facing]);
				break;
			case 2:
				nbtTileEntity.setInteger("facing", rotFacing[rotFacing[facing]]);
				break;
			case 3:
				nbtTileEntity.setInteger("facing", rotFacing[rotFacing[rotFacing[facing]]]);
				break;
			default:
				break;
			}
		}
		
		// glowstone panels
		if (nbtTileEntity.hasKey("side")) {
			final int side = nbtTileEntity.getInteger("side");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setInteger("side", rotFacing[side]);
				break;
			case 2:
				nbtTileEntity.setInteger("side", rotFacing[rotFacing[side]]);
				break;
			case 3:
				nbtTileEntity.setInteger("side", rotFacing[rotFacing[rotFacing[side]]]);
				break;
			default:
				break;
			}
		}
		
		// sided pipes, including duct/pipe/cable/etc.
		final HashMap<String, NBTBase> mapRotated = new HashMap<>(rotConnectionNames.size());
		for (final String key : rotConnectionNames.keySet()) {
			if (nbtTileEntity.hasKey(key)) {
				final NBTBase nbtBase = nbtTileEntity.getTag(key);
				nbtTileEntity.removeTag(key);
				switch (rotationSteps) {
				case 1:
					mapRotated.put(rotConnectionNames.get(key), nbtBase);
					break;
				case 2:
					mapRotated.put(rotConnectionNames.get(rotConnectionNames.get(key)), nbtBase);
					break;
				case 3:
					mapRotated.put(rotConnectionNames.get(rotConnectionNames.get(rotConnectionNames.get(key))), nbtBase);
					break;
				default:
					mapRotated.put(key, nbtBase);
					break;
				}
			}
		}
		for (final Map.Entry<String, NBTBase> entry : mapRotated.entrySet()) {
			nbtTileEntity.setTag(entry.getKey(), entry.getValue());
		}
		
		// bounding blocks
		if ( nbtTileEntity.hasKey("mainX")
		  && nbtTileEntity.hasKey("mainY")
		  && nbtTileEntity.hasKey("mainZ") ) {
			final BlockPos mainTarget = transformation.apply(nbtTileEntity.getInteger("mainX"), nbtTileEntity.getInteger("mainY"), nbtTileEntity.getInteger("mainZ"));
			nbtTileEntity.setInteger("mainX", mainTarget.getX());
			nbtTileEntity.setInteger("mainY", mainTarget.getY());
			nbtTileEntity.setInteger("mainZ", mainTarget.getZ());
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
