package cr0s.warpdrive.data;

import com.google.common.collect.ImmutableSet;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.atomic.BlockAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.BlockChiller;
import cr0s.warpdrive.block.atomic.BlockElectromagnetPlain;
import cr0s.warpdrive.block.atomic.BlockParticlesCollider;
import cr0s.warpdrive.block.atomic.BlockParticlesInjector;
import cr0s.warpdrive.block.atomic.BlockVoidShellPlain;
import cr0s.warpdrive.block.atomic.TileEntityAcceleratorControlPoint;
import cr0s.warpdrive.block.atomic.TileEntityParticlesInjector;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.block.energy.TileEntityCapacitor;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class AcceleratorSetup extends GlobalPosition {
	
	private static final int ACCELERATOR_MAX_RANGE_SQUARED = 192 * 192;
	
	// raw data
	private HashMap<VectorI, TrajectoryPoint> trajectoryAccelerator;
	private HashMap<VectorI, TrajectoryPoint> trajectoryTransfer;
	
	// computed values
	private boolean isDirty = false;
	private final int[] countMagnets = new int[3];
	private final int[] countChillers = new int[3];
	private final HashMap<VectorI, Integer> controlPoints = new HashMap<>();
	
	public final HashSet<BlockPos> setChillers = new HashSet<>();
	public final HashSet<BlockPos> setCapacitors = new HashSet<>();
	public final int energy_maxStorage;
	public final Set<VectorI> setJammed = new HashSet<>();
	public final TreeMap<Integer, VectorI> mapInjectors = new TreeMap<>();
	public final Integer[] keyInjectors;
	public final ArrayList<TrajectoryPoint> listColliders = new ArrayList<>();
	private Object[][] matrixControlPoints = new Object[0][];
	
	private VectorI vMin;
	private VectorI vMax;
	
	// Cooldown duration
	//   increase with number of electromagnets ^0.5
	//   increase with tier (0.75 + 0.25 * x)
	//   decrease with number of cooling vents ^0.6
	//   has a minimum duration function of number of electromagnets per cooling vent
	// Cooldown energy
	//   increase with tier (0.25 > 1.0 > 4.0)
	//   increase with number of cooling vents
	
	public double   temperatureTarget_K;
	public double[] temperatures_cooling_K_perTick = new double[3];
	public double   temperature_coolingEnergyCost_perTick;
	public double[] temperatures_sustainEnergyCost_perTick = new double[3];
	public double   particleEnergy_energyCost_perTick;
	
	protected boolean isAssemblyValid = true;
	protected WarpDriveText textValidityIssues = new WarpDriveText(Commons.getStyleWarning(), "-undefined accelerator setup-");
	private boolean isLoaded = true;
	
	public AcceleratorSetup(final int dimensionId, @Nonnull final BlockPos blockPos) {
		super(dimensionId, blockPos.getX(), blockPos.getY(), blockPos.getZ());
		
		LocalProfiler.start(String.format("[AcceleratorSetup] Scanning @ DIM%d (%d %d %d)",
		                                  dimensionId, x, y, z));
		
		final WorldServer world = getWorldServerIfLoaded();
		if (world == null) {
			if (WarpDriveConfig.LOGGING_ACCELERATOR) {
				WarpDrive.logger.warn(String.format("Accelerator scan cancelled: Dimension %d isn't loaded", dimensionId));
			}
		} else {
			refresh(world);
		}
		
		// cache energy stats
		if ( world == null
		  || setCapacitors.isEmpty() ) {
			energy_maxStorage = 0;
		} else {
			int maxStorage = 0;
			for (final TileEntityCapacitor tileEntityCapacitor : getCapacitors()) {
				maxStorage += tileEntityCapacitor.energy_getMaxStorage();
			}
			energy_maxStorage = maxStorage;
		}
		
		// sort injectors by their video channels
		if (mapInjectors.isEmpty()) {
			keyInjectors = null;
		} else {
			keyInjectors = mapInjectors.keySet().toArray(new Integer[0]);
		}
		
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(String.format("Accelerator length: %d + %d including %d + %d + %d magnets, %d chillers and %d capacitors"
											  + " cool down: %.3f %.3f %.3f /t %.3f EU/t"
											  + " sustain: %.3f %.3f %.3f EU/t"
											  + " acceleration: %.3f /particle",
			        trajectoryAccelerator == null ? -1 : trajectoryAccelerator.size(), trajectoryTransfer == null ? -1 : trajectoryTransfer.size(),
			                                    countMagnets[0], countMagnets[1], countMagnets[2], setChillers.size(), setCapacitors.size(),
			                                    temperatures_cooling_K_perTick[0], temperatures_cooling_K_perTick[1], temperatures_cooling_K_perTick[2],
			                                    temperature_coolingEnergyCost_perTick,
			                                    temperatures_sustainEnergyCost_perTick[0], temperatures_sustainEnergyCost_perTick[1], temperatures_sustainEnergyCost_perTick[2],
			                                    particleEnergy_energyCost_perTick));
		}
		
		LocalProfiler.stop();
	}
	
	// add the vector with it's surrounding block so we can catch 'added' blocks
	private void addToBoundingBox(@Nonnull final VectorI vector, final int range) {
		vMin.x = Math.min(vMin.x, vector.x - range);
		vMin.y = Math.min(vMin.y, vector.y - range);
		vMin.z = Math.min(vMin.z, vector.z - range);
		vMax.x = Math.max(vMax.x, vector.x + range);
		vMax.y = Math.max(vMax.y, vector.y + range);
		vMax.z = Math.max(vMax.z, vector.z + range);
	}
	
	private void refresh(final WorldServer world) {
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			WarpDrive.logger.warn("Accelerator scan cancelled: client side");
			return;
		}
		
		// get all accelerator and transfer trajectory points
		fillTrajectoryPoints(world);
		if (trajectoryAccelerator == null) {
			assert !isAssemblyValid;
			return;
		}
		
		// scan the accelerators
		computeCountsAndBoundingBox();
		computeVectorArrays(world);
		
		// compute values
		final int indexHighest = countMagnets[2] > 0 ? 2 : countMagnets[1] > 0 ? 1 : 0; 
		temperatureTarget_K = WarpDriveConfig.ACCELERATOR_TEMPERATURES_K[indexHighest];
		
		final double coolingFactor = 10.0 / (countMagnets[0] + countMagnets[1] + countMagnets[2]);
		temperatures_cooling_K_perTick[0] = (countChillers[0] * 1.00 + countChillers[1] * 0.75 + countChillers[2] * 0.5) * coolingFactor;
		temperatures_cooling_K_perTick[1] = (countChillers[1] * 1.00 + countChillers[2] * 0.75) * coolingFactor;
		temperatures_cooling_K_perTick[2] = (countChillers[2] * 1.00) * coolingFactor;
		
		temperature_coolingEnergyCost_perTick = countChillers[0] * 10.0 + countChillers[1] * 20.0 + countChillers[2] * 40.0;
		
		temperatures_sustainEnergyCost_perTick[0] = countChillers[0] * 1.00 + countChillers[1] * 2.0 + countChillers[2] * 2.0;
		temperatures_sustainEnergyCost_perTick[1] = countChillers[0] * 0.50 + countChillers[1] * 2.0 + countChillers[2] * 3.0;
		temperatures_sustainEnergyCost_perTick[2] = countChillers[0] * 0.25 + countChillers[1] * 1.0 + countChillers[2] * 4.0;
		
		particleEnergy_energyCost_perTick = countMagnets[0] * 1.00 + countMagnets[1] * 2.00 + countMagnets[2] * 3.00;
	}
	
	private void fillTrajectoryPoints(final WorldServer world) {
		// find closest connected VoidShell
		Set<Block> whitelist = ImmutableSet.of(
			WarpDrive.blockElectromagnets_plain[1],
			WarpDrive.blockElectromagnets_glass[1],
			WarpDrive.blockElectromagnets_plain[2],
			WarpDrive.blockElectromagnets_glass[2],
			WarpDrive.blockElectromagnets_plain[3],
			WarpDrive.blockElectromagnets_glass[3],
			WarpDrive.blockVoidShellPlain,
			WarpDrive.blockVoidShellGlass);
		final Set<BlockPos> connections = Commons.getConnectedBlocks(world, getBlockPos(), Commons.DIRECTIONS_ANY, whitelist, 3);
		VectorI firstVoidShell = null;
		for (final BlockPos connection : connections) {
			final Block block = world.getBlockState(connection).getBlock();
			if (block instanceof BlockVoidShellPlain) {
				firstVoidShell = new VectorI(connection);
				break;
			}
		}
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
		WarpDrive.logger.info(String.format("First void shell is %s", firstVoidShell));
		}
		if (firstVoidShell == null) {
			isAssemblyValid = false;
			textValidityIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_void_shell_connection");
			return;
		}
		
		// find initial direction
		whitelist = ImmutableSet.of(
			WarpDrive.blockVoidShellPlain,
			WarpDrive.blockVoidShellGlass);
		TrajectoryPoint trajectoryPoint = null;
		for (final EnumFacing direction : EnumFacing.HORIZONTALS) {
			final BlockPos next = new BlockPos(firstVoidShell.x + direction.getXOffset(),
			                                   firstVoidShell.y + direction.getYOffset(),
			                                   firstVoidShell.z + direction.getZOffset() );
			final IBlockState blockStateNext = Commons.getBlockState_noChunkLoading(world, next);
			if ( blockStateNext != null
			  && whitelist.contains(blockStateNext.getBlock()) ) {
				trajectoryPoint = new TrajectoryPoint(world, firstVoidShell.translate(direction), direction);
				break;
			}
		}
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(String.format("First one is %s", trajectoryPoint));
		}
		if (trajectoryPoint == null) {
			isAssemblyValid = false;
			textValidityIssues = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_void_shell_connection");
			return;
		}
		
		// scan all connected void shells
		trajectoryAccelerator = new HashMap<>();
		trajectoryTransfer = new HashMap<>();
		
		final HashSet<TrajectoryPoint> acceleratorToAdd = new HashSet<>();
		acceleratorToAdd.add(trajectoryPoint.clone());
		trajectoryPoint = new TrajectoryPoint(world, trajectoryPoint.translate(trajectoryPoint.directionBackward), trajectoryPoint.directionBackward);
		acceleratorToAdd.add(trajectoryPoint);
		
		final HashSet<TrajectoryPoint> transferToAdd = new HashSet<>();
		while(!acceleratorToAdd.isEmpty()) {
			// add all accelerators found
			for (final TrajectoryPoint trajectoryToAdd : acceleratorToAdd) {
				trajectoryPoint = trajectoryToAdd;
				while ( trajectoryPoint.hasNoMissingVoidShells()
				     && isInRange(trajectoryPoint)
				     && !trajectoryAccelerator.containsKey(trajectoryPoint)) {
					if (WarpDriveConfig.LOGGING_ACCELERATOR) {
						WarpDrive.logger.info(String.format("Adding accelerator %s", trajectoryPoint));
					}
					final TrajectoryPoint trajectoryPointToAdd = trajectoryPoint.clone();
					trajectoryAccelerator.put(trajectoryPointToAdd, trajectoryPointToAdd);
					if (trajectoryPoint.vJunctionForward != null) {
						transferToAdd.add(new TrajectoryPoint(world, trajectoryPoint, true));
					}
					if (trajectoryPoint.vJunctionBackward != null) {
						transferToAdd.add(new TrajectoryPoint(world, trajectoryPoint, false));
					}
					trajectoryPoint = new TrajectoryPoint(world, trajectoryPoint.translate(trajectoryPoint.directionForward), trajectoryPoint.directionForward);
				}
			}
			acceleratorToAdd.clear();
			
			// add all transfer found
			for (final TrajectoryPoint trajectoryToAdd : transferToAdd) {
				trajectoryPoint = trajectoryToAdd;
				while ( trajectoryPoint.hasNoMissingVoidShells()
				     && isInRange(trajectoryPoint)
				     && !trajectoryTransfer.containsKey(trajectoryPoint)
				     && !trajectoryPoint.needsReevaluation()) {
					if (WarpDriveConfig.LOGGING_ACCELERATOR) {
						WarpDrive.logger.info(String.format("Adding transfer %s", trajectoryPoint));
					}
					final TrajectoryPoint trajectoryPointToAdd = trajectoryPoint.clone();
					trajectoryTransfer.put(trajectoryPointToAdd, trajectoryPointToAdd);
					trajectoryPoint = new TrajectoryPoint(world, trajectoryPoint, true);
				}
				if (trajectoryPoint.needsReevaluation()) {
					// rebuild as an accelerator point from the next one forward
					trajectoryPoint = new TrajectoryPoint(world, trajectoryPoint.translate(trajectoryPoint.directionForward), trajectoryPoint.directionForward);
					acceleratorToAdd.add(trajectoryPoint.clone());
					// also go backward in case it's broken
					trajectoryPoint = new TrajectoryPoint(world, trajectoryPoint.translate(trajectoryPoint.directionBackward), trajectoryPoint.directionBackward);
					acceleratorToAdd.add(trajectoryPoint);
				}
			}
			transferToAdd.clear();
		}
	}
	
	private boolean isInRange(@Nonnull final TrajectoryPoint trajectoryPoint) {
		final double distanceSquared = trajectoryPoint.distance2To(getBlockPos());
		return distanceSquared <= ACCELERATOR_MAX_RANGE_SQUARED;
	}
	
	private void computeCountsAndBoundingBox() {
		boolean isFirst = true;
		for (final TrajectoryPoint trajectoryPoint : trajectoryAccelerator.values()) {
			// check bounding area
			if (isFirst) {
				vMin = trajectoryPoint.getVectorI();
				vMax = trajectoryPoint.getVectorI();
				isFirst = false;
			}
			addToBoundingBox(trajectoryPoint, 2);
			
			// count main magnets
			final int indexTier = (trajectoryPoint.type & TrajectoryPoint.MASK_TIERS) - 1;
			if ((trajectoryPoint.type & TrajectoryPoint.MAGNETS_HORIZONTAL) != 0 && indexTier >= 0) {
				countMagnets[indexTier] += 2;
			}
			if ((trajectoryPoint.type & TrajectoryPoint.MAGNETS_VERTICAL) != 0 && indexTier >= 0) {
				countMagnets[indexTier] += 2;
			}
			
			// count input/output magnets
			if ((trajectoryPoint.type & TrajectoryPoint.MASK_IS_INPUT) != 0 && indexTier > 0) {
				countMagnets[indexTier - 1] += 12;
			}
			if ((trajectoryPoint.type & TrajectoryPoint.MASK_IS_OUTPUT) != 0 && indexTier < 2) {
				countMagnets[indexTier + 1] += 12;
			}
		}
		if (WarpDriveConfig.LOGGING_ACCELERATOR) {
			WarpDrive.logger.info(String.format("Bounding box is %s to %s", vMin, vMax));
		}
	}
	
	private void computeVectorArrays(@Nonnull final WorldServer world) {
		final WarpDriveText textReason = new WarpDriveText();
		boolean isValid = true; 
		// check for chillers, injectors and colliders blocks
		for (final TrajectoryPoint trajectoryPoint : trajectoryAccelerator.values()) {
			// check for invalid setup
			if (!trajectoryPoint.getStatus(textReason)) {
 				setJammed.add(trajectoryPoint);
 				isValid = false;
			}
			
			// check for injectors
			VectorI vectorToAdd = trajectoryPoint.clone(trajectoryPoint.directionForward.getOpposite());
			final Block blockForward = vectorToAdd.getBlock(world);
			if (blockForward instanceof BlockParticlesInjector) {
				final int controlChannel = ((TileEntityParticlesInjector) vectorToAdd.getTileEntity(world)).getControlChannel();
				mapInjectors.put(controlChannel, vectorToAdd);
				addToBoundingBox(vectorToAdd, 1);
			} else {
				vectorToAdd = trajectoryPoint.clone(trajectoryPoint.directionBackward.getOpposite());
				final Block blockBackward = vectorToAdd.getBlock(world);
				if (blockBackward instanceof BlockParticlesInjector) {
					final int controlChannel = ((TileEntityParticlesInjector) vectorToAdd.getTileEntity(world)).getControlChannel();
					mapInjectors.put(controlChannel, vectorToAdd);
					addToBoundingBox(vectorToAdd, 1);
				}
			}
			
			// collect control points and colliders
			if (trajectoryPoint.vControlPoint != null) {
				controlPoints.put(trajectoryPoint.vControlPoint, trajectoryPoint.type);
				addToBoundingBox(trajectoryPoint.vControlPoint, 1);
				if (trajectoryPoint.isCollider()) {
					listColliders.add(trajectoryPoint);
				}
			}
			
			// check corners when there's at least 1 set of main magnets and no control point
			if (trajectoryPoint.vControlPoint == null && (trajectoryPoint.type & TrajectoryPoint.MASK_MAGNETS_BOTH) != 0) {
				scanCorners(world, trajectoryPoint, trajectoryPoint.directionForward);
				if (trajectoryPoint.directionForward != trajectoryPoint.directionBackward.getOpposite()) {
					scanCorners(world, trajectoryPoint, trajectoryPoint.directionBackward);
				}
			}
		}
		
		// check counts
		if (mapInjectors.isEmpty()) {
			isValid = false;
			textReason.append(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_injector");
		}
		if (listColliders.isEmpty()) {
			isValid = false;
			textReason.append(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_collider_node");
		}
		if (countMagnets[2] > 0 && countChillers[2] == 0) {
			isValid = false;
			textReason.append(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_superior_chiller");
		} else if (countMagnets[1] > 0 && countChillers[1] == 0) {
			isValid = false;
			textReason.append(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_advanced_chiller");
		} else if (countMagnets[0] > 0 && countChillers[0] == 0) {
			isValid = false;
			textReason.append(Commons.getStyleWarning(), "warpdrive.accelerator.status_line.missing_basic_chiller");
		}
		
		// build matrix description for Computer APIs
		matrixControlPoints = new Object[controlPoints.size() + mapInjectors.size()][];
		int index = 0;
		for (final Entry<VectorI, Integer> entryControlPoint : controlPoints.entrySet()) {
			final Integer tier = TrajectoryPoint.getTier(entryControlPoint.getValue());
			final String type = TrajectoryPoint.isCollider(entryControlPoint.getValue()) ? "Collider" :
			                    TrajectoryPoint.isOutput(entryControlPoint.getValue()) ? "Output" :
			                    TrajectoryPoint.isInput(entryControlPoint.getValue()) ? "Input" : "?";
			final TileEntity tileEntity = world.getTileEntity(entryControlPoint.getKey().getBlockPos());
			final Boolean isEnabled = (tileEntity instanceof TileEntityAcceleratorControlPoint) && ((TileEntityAcceleratorControlPoint) tileEntity).getIsEnabled();
			final Integer controlChannel = (tileEntity instanceof IControlChannel) ? ((IControlChannel) tileEntity).getControlChannel() : -1;
			
			matrixControlPoints[index++] = new Object[] {
					entryControlPoint.getKey().x, entryControlPoint.getKey().y, entryControlPoint.getKey().z,
					tier, type, isEnabled, controlChannel };
		}
		for (final Entry<Integer, VectorI> entryInjector : mapInjectors.entrySet()) {
			final Integer tier = 1;
			final String type = "Injector";
			final TileEntity tileEntity = world.getTileEntity(entryInjector.getValue().getBlockPos());
			final Boolean isEnabled = (tileEntity instanceof TileEntityParticlesInjector) && ((TileEntityParticlesInjector) tileEntity).getIsEnabled();
			final Integer controlChannel = (tileEntity instanceof IControlChannel) ? ((IControlChannel) tileEntity).getControlChannel() : -1;
			
			matrixControlPoints[index++] = new Object[] {
					entryInjector.getValue().x, entryInjector.getValue().y, entryInjector.getValue().z,
					tier, type, isEnabled, controlChannel };
		}
		
		// update validity status
		isAssemblyValid = isValid;
		textValidityIssues = textReason;
	}
	
	private void scanCorners(@Nonnull final WorldServer world, @Nonnull final VectorI vCenter, @Nonnull final EnumFacing forgeDirection) {
		final EnumFacing directionLeft = forgeDirection.rotateYCCW();
		final EnumFacing directionRight = forgeDirection.rotateY();
		for (int indexCorner = 0; indexCorner < 4; indexCorner++) {
			final VectorI vector = new VectorI(
				vCenter.x + ((indexCorner & 1) != 0 ? directionLeft.getXOffset() : directionRight.getXOffset()),
				vCenter.y + ((indexCorner & 2) != 0 ? 1 : -1),
			    vCenter.z + ((indexCorner & 1) != 0 ? directionLeft.getZOffset() : directionRight.getZOffset()));
			final Block block = vector.getBlock(world);
			if (block instanceof BlockChiller) {
				final EnumTier enumTier = ((BlockChiller) block).getTier(ItemStack.EMPTY);
				setChillers.add(vector.getBlockPos());
				countChillers[enumTier.getIndex() - 1]++;
			} else if (block instanceof BlockCapacitor) {
				final TileEntity tileEntity = vector.getTileEntity(world);
				if (tileEntity instanceof TileEntityCapacitor) {
					setCapacitors.add(vector.getBlockPos());
				} else {
					WarpDrive.logger.error(String.format("Invalid tile entity detected for subspace capacitor at %s", vector));
				}
			}
		}
	}
	
	public int getMass() {
		if (trajectoryAccelerator == null) {
			return 0;
		}
		return trajectoryAccelerator.size() + trajectoryTransfer.size() + setCapacitors.size() + controlPoints.size()
		       + countMagnets[0] + countMagnets[1] + countMagnets[2]
		       + countChillers[0] + countChillers[1] + countChillers[2];
	}
	
	public boolean isMajorChange(final AcceleratorSetup acceleratorSetup) {
		return acceleratorSetup == null
			|| trajectoryAccelerator == null
		    || acceleratorSetup.trajectoryAccelerator == null
		    || trajectoryAccelerator.size() != acceleratorSetup.trajectoryAccelerator.size()
		    || trajectoryTransfer.size() != acceleratorSetup.trajectoryTransfer.size()
		    || countMagnets[0] != acceleratorSetup.countMagnets[0]
		    || countMagnets[1] != acceleratorSetup.countMagnets[1]
		    || countMagnets[2] != acceleratorSetup.countMagnets[2]
		    || countChillers[0] != acceleratorSetup.countChillers[0]
		    || countChillers[1] != acceleratorSetup.countChillers[1]
		    || countChillers[2] != acceleratorSetup.countChillers[2];
	}
	
	public boolean isBlockUpdated(final World world, final VectorI vector, final Block block) {
		boolean checkDirectConnection = false;
		boolean checkRangedConnection = false;
		boolean checkCornerConnection = false;
		// check explicit inclusions
		if (block instanceof BlockChiller) {
			if (setChillers.contains(vector.getBlockPos())) {
				return true;
			}
			checkCornerConnection = true;
			
		} else if (block instanceof BlockVoidShellPlain) {
			if (isTrajectoryPoint(vector)) {
				return true;
			}
			checkDirectConnection = true;
			
		} else if (block instanceof BlockParticlesInjector) {
			checkDirectConnection = true;
			
		} else if (block instanceof BlockElectromagnetPlain) {
			checkDirectConnection = true;
			checkCornerConnection = true;
			
		} else if (block instanceof BlockParticlesCollider) {
			checkCornerConnection = true;
			
		} else if (block instanceof BlockAcceleratorControlPoint) {
			checkRangedConnection = true;
			
		} else if (block instanceof BlockCapacitor) {
			final TileEntity tileEntity = vector.getTileEntity(world);
			if (tileEntity instanceof TileEntityCapacitor) {
				if (setCapacitors.contains(vector.getBlockPos())) {
					return true;
				}
			}
			checkCornerConnection = true;
		}
		
		// check connections
		if (checkDirectConnection || checkCornerConnection) {
			for (final EnumFacing forgeDirection : EnumFacing.VALUES) {
				final Block blockConnected = vector.translate(forgeDirection).getBlock(world);
				if (blockConnected instanceof BlockVoidShellPlain) {
					if (isTrajectoryPoint(vector)) {
						return true;
					}
				} else if (checkCornerConnection && blockConnected instanceof BlockElectromagnetPlain) {
					for (final EnumFacing forgeDirection2 : EnumFacing.VALUES) {
						final Block blockSubConnected = vector.translate(forgeDirection2).getBlock(world);
						if (blockSubConnected instanceof BlockVoidShellPlain) {
							if (isTrajectoryPoint(vector)) {
								return true;
							}
						}
					}
				}
			}
		}
		
		if (checkRangedConnection) {
			for (final EnumFacing forgeDirection : EnumFacing.VALUES) {
				final Block blockConnected = vector.translate(forgeDirection, 2).getBlock(world);
				if (blockConnected instanceof BlockVoidShellPlain) {
					if (isTrajectoryPoint(vector)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isTrajectoryPoint(final VectorI vectorI) {
		return trajectoryAccelerator.containsKey(vectorI) || trajectoryTransfer.containsKey(vectorI);
	}
	
	public TrajectoryPoint getTrajectoryPoint(final VectorI vectorI) {
		final TrajectoryPoint trajectoryPoint = trajectoryAccelerator.get(vectorI);
		if (trajectoryPoint != null) {
			return trajectoryPoint;
		}
		return trajectoryTransfer.get(vectorI);
	}
	
	public AxisAlignedBB getBoundingBox() {
		if (vMin == null || vMax == null) {
			return null;
		}
		return new AxisAlignedBB(
			vMin.x, vMin.y, vMin.z,
			vMax.x, vMax.y, vMax.z);
	}
	
	// sanity check
	public boolean isDirty() {
		if (trajectoryAccelerator == null) {
			return false;
		}
		return isDirty;
	}
	
	public boolean isAssemblyValid() {
		return isAssemblyValid;
	}
	
	public boolean getAssemblyStatus(@Nonnull final WarpDriveText textReason) {
		textReason.append(textValidityIssues);
		return isAssemblyValid;
	}
	
	@Override
	public boolean isLoaded() {
		return super.isLoaded() && isLoaded;
	}
	
	// Pseudo-API for energy
	@Nonnull
	private HashSet<TileEntityCapacitor> getCapacitors() {
		final HashSet<TileEntityCapacitor> setTileEntityCapacitors = new HashSet<>(setCapacitors.size());
		final World world = getWorldServerIfLoaded();
		if (world == null) {
			WarpDrive.logger.warn(String.format("%s World is no longer loaded, please report to mod author",
			                                    this));
			if (Commons.throttleMe("AcceleratorSetup.getCapacitors")) {
				new RuntimeException().printStackTrace(WarpDrive.printStreamWarn);
			}
			isLoaded = false;
			return setTileEntityCapacitors;
		}
		for (final BlockPos blockPosCapacitor : setCapacitors) {
			final TileEntity tileEntity = world.getTileEntity(blockPosCapacitor);
			if (tileEntity instanceof TileEntityCapacitor) {
				final TileEntityCapacitor tileEntityCapacitor = (TileEntityCapacitor) tileEntity;
				tileEntityCapacitor.finishConstruction();
				setTileEntityCapacitors.add(tileEntityCapacitor);
			} else {
				isDirty = true;
			}
		}
		isLoaded = true;
		return setTileEntityCapacitors;
	}
	
	public long energy_getEnergyStored() {
		final HashSet<TileEntityCapacitor> capacitors = getCapacitors();
		if (!isLoaded()) {
			return Integer.MAX_VALUE;
		}
		long energyStored = 0;
		for (final TileEntityCapacitor tileEntityCapacitor : capacitors) {
			energyStored += tileEntityCapacitor.energy_getEnergyStored();
		}
		return energyStored;
	}
	
	public int energy_getPotentialOutput() {
		final HashSet<TileEntityCapacitor> capacitors = getCapacitors();
		if (!isLoaded()) {
			return Integer.MAX_VALUE;
		}
		long potentialOutput = 0;
		for (final TileEntityCapacitor tileEntityCapacitor : capacitors) {
			potentialOutput = Math.min(potentialOutput + tileEntityCapacitor.energy_getPotentialOutput(), Integer.MAX_VALUE);
		}
		return (int) potentialOutput;
	}
	
	public int energy_getMaxStorage() {
		return energy_maxStorage;
	}
	
	public void energy_consume(final int amount_internal) {
		assert amount_internal > 0;
		assert isLoaded;
		final HashSet<TileEntityCapacitor> setTileEntityCapacitors = getCapacitors();
		final int countCapacitors = setTileEntityCapacitors.size();
		assert countCapacitors > 0;
		
		// first, draw average from all
		final int energyMean = amount_internal / countCapacitors;
		int energyConsumed = 0;
		int energyLeft = amount_internal - energyMean * countCapacitors;
		for (final TileEntityCapacitor tileEntityCapacitor : setTileEntityCapacitors) {
			final int energyToConsume = Math.min(tileEntityCapacitor.energy_getPotentialOutput(), energyMean + energyLeft);
			tileEntityCapacitor.energy_consume(energyToConsume);
			energyConsumed += energyToConsume;
			energyLeft += (energyMean - energyToConsume);
		}
		if (energyConsumed + energyLeft != amount_internal) {
			if (Commons.throttleMe("AcceleratorSetup.energy_consume")) {
				WarpDrive.logger.error(String.format("Inconsistent accelerator energy consumption 1/2 amount %d count %d (%s) mean %d consumed %d left %d",
				                                     amount_internal, countCapacitors, setTileEntityCapacitors.size(), energyMean, energyConsumed, energyLeft));
			}
			assert false;
		}
		// then, draw remaining in no special order
		if (energyLeft > 0) {
			for (final TileEntityCapacitor tileEntityCapacitor : setTileEntityCapacitors) {
				final int energyToConsume = Math.min(tileEntityCapacitor.energy_getPotentialOutput(), energyLeft);
				tileEntityCapacitor.energy_consume(energyToConsume);
				energyConsumed += energyToConsume;
				energyLeft -= energyToConsume;
			}
		}
		if ( energyConsumed != amount_internal
		  || energyLeft != 0 ) {
			if (Commons.throttleMe("AcceleratorSetup.energy_consume")) {
				WarpDrive.logger.error(String.format("Inconsistent accelerator energy consumption 2/2 amount %d count %d (%s) mean %d consumed %d left %d (0)",
				                                     amount_internal, countCapacitors, setTileEntityCapacitors.size(), energyMean, energyConsumed, energyLeft));
			}
			assert false;
		}
	}
	
	// Pseudo-API for computers
	@Nonnull
	public Object[][] getControlPoints() {
		return matrixControlPoints;
	}
	
	
	@Override
	public String toString() {
		if (vMin == null || vMax == null) {
			return String.format("%s @ DIM%d (%d %d %d) (-null-) -> (-null)",
				getClass().getSimpleName(), dimensionId,
				x, y, z);
		}
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			x, y, z,
			vMin.x, vMin.y, vMin.z,
			vMax.x, vMax.y, vMax.z);
	}
}
