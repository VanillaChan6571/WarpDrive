package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class CompatEmbers implements IBlockTransformer {
	
	public static void register() {
		try {
			@SuppressWarnings("unused") // just a basic check
			final Class<?> classDummyCheck = Class.forName("teamroots.embers.block.BlockBase");
			
			WarpDriveConfig.registerBlockTransformer("Embers", new CompatEmbers());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		final ResourceLocation registryName = block.getRegistryName();
		assert registryName != null;
		if (!registryName.getNamespace().equals("embers")) {
			return false;
		}
		return registryNameRotating.contains(registryName.getPath());
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
	
	/*
	*** not supported (yet)
	explosion charm pedestal (what is it?)

	*** not implemented
	furnace
	item gauge (dial)
	knowledge table
	mech actuator
	steam engine
	
	*** no rotation
	embers:alchemy_pedestal
	embers:bin
	embers:block_furnace
	embers:block_lantern
	embers:block_tank
	embers:boiler
	embers:catalyzer
	embers:cinder_plinth
	embers:combustor
	embers:copper_cell
	embers:creative_ember_source
	embers:ember_activator
	embers:ember_siphon
	embers:item_dropper
	embers:mech_core
	embers:mixer
	embers:reactor
	embers:stamper_base
	embers:stamper
	
	*** metadata back rotation (metadata codes relative position in the world so we need to compensate the rotation)
	embers:advanced_edge (crystal cell)
	embers:field_chart
	embers:stone_edge (caminite ring)
		1 7 5 3 / 2 9 6 4 / 8
	embers:inferno_forge_edge
		0 6 4 2 / 1 7 5 3 / 8 14 12 10 / 9 15 13 11
	embers:mech_edge (ember_bore, heat coil, large_tank)
		0 6 4 2 / 1 7 5 3
	
	*** metadata rotation only
	embers:auto_hammer
	embers:breaker
	embers:catalytic_plug
	embers:charger
	embers:clockwork_attenuator
	embers:ember_gauge
	embers:ember_funnel
	embers:ember_injector
	embers:fluid_gauge
	embers:mech_accessor
	embers:ember_receiver
	embers:ember_relay
	embers:stirling
	embers:vacuum
		metadata facing
	embers:dawnstone_anvil
		metadata 0 1
	embers:mini_boiler
		metadata 0 1 2 3
	embers:mechanical_pump
		metadata 2 5 3 4 / 8 11 9 10 (facing | 6)
	
	*** tile entity rotation
	embers:alchemy_tablet
		north/south/east/west   compound
	embers:beam_cannon
		metadata facing
		targetX/Y/Z int absolute coordinates
	embers:beam_splitter
		metadata 0 2        when rotating right, 2 -> 0 will switch Left and Right
		targetLeftX/Y/Z int (optional)
		targetRightX/Y/Z int (optional)
	embers:ember_emitter
		metadata facing
		north/south/east/west   int 0 (not connected) / 1 (connected to lever)
		targetX/Y/Z int absolute coordinates (optional)
	embers:fluid_transfer
	embers:item_transfer
		metadata 0 / 1 / 2 / 3 / 4 10 6 8 / 5 11 7 9 (facing << 1 | 1)
		from0/.../from5 boolean 0 (unused ?)
	embers:mixer
		northTank/southTank/eastTank/westTank   compound
	embers:ember_pulser
		metadata facing
		north/south/east/west   int 0 (unused ?)
	embers:item_pipe
	embers:item_pump
	embers:pipe
	embers:pump
		north/south/east/west   int 0 (not connected) / 1 (pipe) / 2 (machine)
		from0/.../from5 boolean 0 (unused ?)
	*/
	
	
	private static final Set<String> registryNameRotating;
	private static final Set<String> registryNameFacing;
	private static final Map<String, String> rotSideNames;
	static {
		Set<String> set = new HashSet<>(30);
		// *** metadata back rotation
		set.add("advanced_edge");
		set.add("field_chart");
		set.add("stone_edge");
		set.add("inferno_forge_edge");
		set.add("mech_edge");
		
		// *** metadata rotation only
		set.add("auto_hammer");
		set.add("breaker");
		set.add("catalytic_plug");
		set.add("charger");
		set.add("clockwork_attenuator");
		set.add("ember_gauge");
		set.add("ember_funnel");
		set.add("ember_injector");
		set.add("fluid_gauge");
		set.add("mech_accessor");
		set.add("ember_receiver");
		set.add("ember_relay");
		set.add("stirling");
		set.add("vacuum");
		
		set.add("dawnstone_anvil");
		set.add("mini_boiler");
		set.add("mechanical_pump");
		
		// *** tile entity rotation
		set.add("alchemy_tablet");
		set.add("beam_cannon");
		set.add("beam_splitter");
		set.add("ember_emitter");
		set.add("fluid_transfer");
		set.add("item_transfer");
		set.add("mixer");
		set.add("ember_pulser");
		set.add("item_pipe");
		set.add("item_pump");
		set.add("pipe");
		set.add("pump");
		registryNameRotating = Collections.unmodifiableSet(set);
		
		// simple facing rotation
		set = new HashSet<>(15);
		set.add("auto_hammer");
		set.add("breaker");
		set.add("catalytic_plug");
		set.add("charger");
		set.add("clockwork_attenuator");
		set.add("ember_gauge");
		set.add("ember_funnel");
		set.add("ember_injector");
		set.add("fluid_gauge");
		set.add("mech_accessor");
		set.add("ember_receiver");
		set.add("ember_relay");
		set.add("stirling");
		set.add("vacuum");
		registryNameFacing = Collections.unmodifiableSet(set);
		
		final Map<String, String> map = new HashMap<>();
		map.put("east", "south");
		map.put("south", "west");
		map.put("west", "north");
		map.put("north", "east");
		map.put("eastTank", "southTank");
		map.put("southTank", "westTank");
		map.put("westTank", "northTank");
		map.put("northTank", "eastTank");
		map.put("from2", "from5");
		map.put("from5", "from3");
		map.put("from3", "from4");
		map.put("from4", "from2");
		rotSideNames = Collections.unmodifiableMap(map);
	}
	
	//                                                 0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[] rotStoneEdge       = {  0,  7,  9,  1,  2,  3,  4,  5,  8,  6, 10, 11, 12, 13, 14, 15 };
	private static final int[] rotMechEdge        = {  6,  7,  0,  1,  2,  3,  4,  5,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[] rotForgeEdge       = {  6,  7,  0,  1,  2,  3,  4,  5, 14, 15,  8,  9, 10, 11, 12, 13 };
	
	private static final int[] rotFacing          = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[] rotDawnstoneAnvil  = {  1,  0,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[] rotHorizontal      = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[] rotFacingOr6       = {  0,  1,  5,  4,  2,  3,  6,  7, 11, 10,  8,  9, 12, 13, 14, 15 };
	private static final int[] rot1or2xFacing     = {  0,  1,  2,  3, 10, 11,  8,  9,  4,  5,  6,  7, 12, 13, 14, 15 };
	private static final int[] rotSplitter        = {  2,  1,  0,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		final ResourceLocation registryName = block.getRegistryName();
		assert registryName != null;
		final String registryPath = registryName.getPath();
		
		// *** metadata back rotation
		if ( registryPath.equals("advanced_edge")
		  || registryPath.equals("field_chart")
		  || registryPath.equals("stone_edge") ) {
			switch (rotationSteps) {
			case 1:
				return rotStoneEdge[metadata];
			case 2:
				return rotStoneEdge[rotStoneEdge[metadata]];
			case 3:
				return rotStoneEdge[rotStoneEdge[rotStoneEdge[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (registryPath.equals("mech_edge")) {
			switch (rotationSteps) {
			case 1:
				return rotMechEdge[metadata];
			case 2:
				return rotMechEdge[rotMechEdge[metadata]];
			case 3:
				return rotMechEdge[rotMechEdge[rotMechEdge[metadata]]];
			default:
				return metadata;
			}
		}
		
		if (registryPath.equals("inferno_forge_edge")) {
			switch (rotationSteps) {
			case 1:
				return rotForgeEdge[metadata];
			case 2:
				return rotForgeEdge[rotForgeEdge[metadata]];
			case 3:
				return rotForgeEdge[rotForgeEdge[rotForgeEdge[metadata]]];
			default:
				return metadata;
			}
		}
		
		
		// *** metadata rotation only
		// simple facing metadata
		if (registryNameFacing.contains(registryPath)) {
			switch (rotationSteps) {
			case 1:
				return rotFacing[metadata];
			case 2:
				return rotFacing[rotFacing[metadata]];
			case 3:
				return rotFacing[rotFacing[rotFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// simple facing metadata
		if (registryPath.equals("dawnstone_anvil")) {
			switch (rotationSteps) {
			case 1:
				return rotDawnstoneAnvil[metadata];
			case 2:
				return rotDawnstoneAnvil[rotDawnstoneAnvil[metadata]];
			case 3:
				return rotDawnstoneAnvil[rotDawnstoneAnvil[rotDawnstoneAnvil[metadata]]];
			default:
				return metadata;
			}
		}
		
		// horizontal rotation only
		if (registryPath.equals("mini_boiler")) {
			switch (rotationSteps) {
			case 1:
				return rotHorizontal[metadata];
			case 2:
				return rotHorizontal[rotHorizontal[metadata]];
			case 3:
				return rotHorizontal[rotHorizontal[rotHorizontal[metadata]]];
			default:
				return metadata;
			}
		}
		
		// facing metadata with offset of 6
		if (registryPath.equals("mechanical_pump")) {
			switch (rotationSteps) {
			case 1:
				return rotFacingOr6[metadata];
			case 2:
				return rotFacingOr6[rotFacingOr6[metadata]];
			case 3:
				return rotFacingOr6[rotFacingOr6[rotFacingOr6[metadata]]];
			default:
				return metadata;
			}
		}
		
		
		// *** tile entity rotation
		// rotate by name optionally
		if ( nbtTileEntity.hasKey("east")
		  || nbtTileEntity.hasKey("eastTank")
		  || nbtTileEntity.hasKey("from2") ) {
			final Map<String, NBTBase> map = new HashMap<>();
			for (final String key : rotSideNames.keySet()) {
				if (nbtTileEntity.hasKey(key)) {
					final NBTBase tag = nbtTileEntity.getTag(key);
					switch (rotationSteps) {
					case 1:
						map.put(rotSideNames.get(key), tag);
						break;
					case 2:
						map.put(rotSideNames.get(rotSideNames.get(key)), tag);
						break;
					case 3:
						map.put(rotSideNames.get(rotSideNames.get(rotSideNames.get(key))), tag);
						break;
					default:
						map.put(key, tag);
						break;
					}
					nbtTileEntity.removeTag(key);
				}
			}
			if (!map.isEmpty()) {
				for (final Entry<String, NBTBase> entry : map.entrySet()) {
					nbtTileEntity.setTag(entry.getKey(), entry.getValue());
				}
			}
		}
		
		// reposition coordinates
		if (nbtTileEntity.hasKey("targetX")) {
			final int targetX = nbtTileEntity.getInteger("targetX");
			final int targetY = nbtTileEntity.getInteger("targetY");
			final int targetZ = nbtTileEntity.getInteger("targetZ");
			if (transformation.isInside(targetX, targetY, targetZ)) {
				final BlockPos chunkCoordinates = transformation.apply(targetX, targetY, targetZ);
				nbtTileEntity.setInteger("targetX", chunkCoordinates.getX());
				nbtTileEntity.setInteger("targetY", chunkCoordinates.getY());
				nbtTileEntity.setInteger("targetZ", chunkCoordinates.getZ());
			}
		}
		
		// facing metadata
		if ( registryPath.equals("beam_cannon")
		  || registryPath.equals("ember_emitter")
		  || registryPath.equals("ember_pulser") ) {
			switch (rotationSteps) {
			case 1:
				return rotFacing[metadata];
			case 2:
				return rotFacing[rotFacing[metadata]];
			case 3:
				return rotFacing[rotFacing[rotFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// 1 or 2 x facing metadata
		if ( registryPath.equals("fluid_transfer")
		  || registryPath.equals("item_transfer") ) {
			switch (rotationSteps) {
			case 1:
				return rot1or2xFacing[metadata];
			case 2:
				return rot1or2xFacing[rot1or2xFacing[metadata]];
			case 3:
				return rot1or2xFacing[rot1or2xFacing[rot1or2xFacing[metadata]]];
			default:
				return metadata;
			}
		}
		
		// beam splitter will switch Left and Right target when rotating right from metadata 2 to 0
		// metadata 0 2        when rotating right, 2 -> 0 will switch Left and Right
		if (registryPath.equals("beam_splitter")) {
			// we transform targets, remove tag, and memorize the result so we can switch sides depending on rotation
			int targetLeftX = -1;
			int targetLeftY = -1;
			int targetLeftZ = -1;
			int targetRightX = -1;
			int targetRightY = -1;
			int targetRightZ = -1;
			if (nbtTileEntity.hasKey("targetLeftX")) {
				targetLeftX = nbtTileEntity.getInteger("targetLeftX");
				targetLeftY = nbtTileEntity.getInteger("targetLeftY");
				targetLeftZ = nbtTileEntity.getInteger("targetLeftZ");
				if (transformation.isInside(targetLeftX, targetLeftY, targetLeftZ)) {
					final BlockPos chunkCoordinates = transformation.apply(targetLeftX, targetLeftY, targetLeftZ);
					targetLeftX = chunkCoordinates.getX();
					targetLeftY = chunkCoordinates.getY();
					targetLeftZ = chunkCoordinates.getZ();
				}
				nbtTileEntity.removeTag("targetLeftX");
				nbtTileEntity.removeTag("targetLeftY");
				nbtTileEntity.removeTag("targetLeftZ");
			}
			if (nbtTileEntity.hasKey("targetRightX")) {
				targetRightX = nbtTileEntity.getInteger("targetRightX");
				targetRightY = nbtTileEntity.getInteger("targetRightY");
				targetRightZ = nbtTileEntity.getInteger("targetRightZ");
				if (transformation.isInside(targetRightX, targetRightY, targetRightZ)) {
					final BlockPos chunkCoordinates = transformation.apply(targetRightX, targetRightY, targetRightZ);
					targetRightX = chunkCoordinates.getX();
					targetRightY = chunkCoordinates.getY();
					targetRightZ = chunkCoordinates.getZ();
				}
				nbtTileEntity.removeTag("targetRightX");
				nbtTileEntity.removeTag("targetRightY");
				nbtTileEntity.removeTag("targetRightZ");
			}
			
			// compute if we need to switch sides or not
			// case 1: metadata is currently 0
			// rotation 0    1    2    3
			// metadata 0 -> 2 -> 0 -> 2
			// switch   N    N    Y    Y
			// => keep if no rotation or (metadata == 0 && rotation == 1)
			// case 1: metadata is currently 2
			// rotation 0    1    2    3
			// metadata 2 -> 0 -> 2 -> 0
			// switch   N    Y    Y    N
			// => keep if no rotation or (metadata == 2 && rotation == 3)
			if ( (rotationSteps == 0)
			  || (metadata == 0 && rotationSteps == 1)
			  || (metadata == 2 && rotationSteps == 3) ) {
				if (targetLeftY != -1) {
					nbtTileEntity.setInteger("targetLeftX", targetLeftX);
					nbtTileEntity.setInteger("targetLeftY", targetLeftY);
					nbtTileEntity.setInteger("targetLeftZ", targetLeftZ);
				}
				if (targetRightY != -1) {
					nbtTileEntity.setInteger("targetRightX", targetRightX);
					nbtTileEntity.setInteger("targetRightY", targetRightY);
					nbtTileEntity.setInteger("targetRightZ", targetRightZ);
				}
			} else {
				if (targetRightY != -1) {
					nbtTileEntity.setInteger("targetLeftX", targetRightX);
					nbtTileEntity.setInteger("targetLeftY", targetRightY);
					nbtTileEntity.setInteger("targetLeftZ", targetRightZ);
				}
				if (targetLeftY != -1) {
					nbtTileEntity.setInteger("targetRightX", targetLeftX);
					nbtTileEntity.setInteger("targetRightY", targetLeftY);
					nbtTileEntity.setInteger("targetRightZ", targetLeftZ);
				}
			}
			
			// metadata 0 2
			switch (rotationSteps) {
			case 1:
				return rotSplitter[metadata];
			case 2:
				return rotSplitter[rotSplitter[metadata]];
			case 3:
				return rotSplitter[rotSplitter[rotSplitter[metadata]]];
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
