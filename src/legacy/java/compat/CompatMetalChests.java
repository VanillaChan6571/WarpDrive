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

public class CompatMetalChests implements IBlockTransformer {
	
	private static Class<?> classBlockMetalChest;
	
	public static void register() {
		try {
			classBlockMetalChest = Class.forName("t145.metalchests.blocks.BlockMetalChest");
			
			WarpDriveConfig.registerBlockTransformer("MetalChests", new CompatMetalChests());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBlockMetalChest.isInstance(block);
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
	
	private static final Map<String, String> rotFacingNames;
	static {
		final Map<String, String> map = new HashMap<>();
		map.put("north", "east");
		map.put("east", "south");
		map.put("south", "west");
		map.put("west", "north");
		rotFacingNames = Collections.unmodifiableMap(map);
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0 || nbtTileEntity == null) {
			return metadata;
		}
		
		// Metal chests (sorting are untested)
		if (nbtTileEntity.hasKey("Front")) {
			final String facing = nbtTileEntity.getString("Front");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setString("Front", rotFacingNames.get(facing));
				break;
			case 2:
				nbtTileEntity.setString("Front", rotFacingNames.get(rotFacingNames.get(facing)));
				break;
			case 3:
				nbtTileEntity.setString("Front", rotFacingNames.get(rotFacingNames.get(rotFacingNames.get(facing))));
				break;
			default:
				break;
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
