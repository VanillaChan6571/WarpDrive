package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Loaded-chunk topology snapshot for the accelerator core. */
final class AcceleratorAssembly {

	private static final int MAX_RANGE_SQUARED = 192 * 192;
	private static final int MAX_TRAJECTORY_POINTS = 4_096;
	private static final Direction[] HORIZONTAL = {
		Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

	private AcceleratorAssembly() { }

	static Result scan(final ServerWorld world, final BlockPos core) {
		final Result result = new Result();
		final BlockPos first = findFirstShell(world, core);
		if (first == null) {
			result.status = "Missing connected void shell";
			return result;
		}

		final Queue<BlockPos> pending = new ArrayDeque<>();
		pending.add(first);
		result.shells.add(first);
		while (!pending.isEmpty() && result.shells.size() < MAX_TRAJECTORY_POINTS) {
			final BlockPos current = pending.remove();
			for (final Direction direction : HORIZONTAL) {
				final BlockPos next = current.relative(direction);
				if (next.distSqr(core) > MAX_RANGE_SQUARED || result.shells.contains(next)) continue;
				if (!world.hasChunkAt(next)) {
					result.loaded = false;
					continue;
				}
				if (isShell(world.getBlockState(next).getBlock())) {
					final BlockPos immutable = next.immutable();
					result.shells.add(immutable);
					pending.add(immutable);
				}
			}
		}
		if (result.shells.size() >= MAX_TRAJECTORY_POINTS) {
			result.status = "Trajectory exceeds scan limit";
			return result;
		}

		for (final BlockPos shell : result.shells) inspectTrajectoryPoint(world, shell, result);
		if (result.shells.size() < 7) {
			result.status = "Trajectory is too short (minimum 7 void shells)";
			return result;
		}
		if (!result.jammed.isEmpty()) {
			result.status = result.jammed.size() + " jammed trajectory point(s)";
			return result;
		}
		for (int tier = 0; tier < result.magnetCounts.length; tier++) {
			if (result.magnetCounts[tier] > 0 && result.chillerCounts[tier] == 0) {
				result.status = "Missing " + AcceleratorTier.values()[tier].getName() + " chiller";
				return result;
			}
		}
		result.valid = result.loaded;
		result.status = result.loaded ? "Assembly is valid" : "Trajectory crosses unloaded chunks";
		return result;
	}

	@Nullable
	private static BlockPos findFirstShell(final ServerWorld world, final BlockPos core) {
		BlockPos closest = null;
		double distanceClosest = Double.MAX_VALUE;
		for (int x = -3; x <= 3; x++) {
			for (int y = -3; y <= 3; y++) {
				for (int z = -3; z <= 3; z++) {
					final BlockPos candidate = core.offset(x, y, z);
					if (!world.hasChunkAt(candidate)
					 || !isShell(world.getBlockState(candidate).getBlock())) continue;
					final double distance = candidate.distSqr(core);
					if (distance < distanceClosest) {
						distanceClosest = distance;
						closest = candidate.immutable();
					}
				}
			}
		}
		return closest;
	}

	private static void inspectTrajectoryPoint(final ServerWorld world, final BlockPos shell,
	                                           final Result result) {
		int connected = 0;
		Direction trajectoryDirection = null;
		for (final Direction direction : HORIZONTAL) {
			if (result.shells.contains(shell.relative(direction))) {
				connected++;
				if (trajectoryDirection == null) trajectoryDirection = direction;
			}
		}
		if (connected == 0 || connected > 3) result.jammed.add(shell);

		// Glass shells are transfer paths. Plain shells form the magnetized accelerator path.
		if (world.getBlockState(shell).getBlock() == Registration.VOID_SHELL_PLAIN.get()) {
			int pairs = 0;
			final int verticalTier = sameMagnetTier(world, shell.above(), shell.below());
			if (verticalTier > 0) {
				result.magnetCounts[verticalTier - 1] += 2;
				result.highestTier = Math.max(result.highestTier, verticalTier);
				pairs++;
			}
			if (trajectoryDirection != null) {
				final Direction left = trajectoryDirection.getCounterClockWise();
				final int horizontalTier = sameMagnetTier(world,
					shell.relative(left), shell.relative(left.getOpposite()));
				if (horizontalTier > 0) {
					result.magnetCounts[horizontalTier - 1] += 2;
					result.highestTier = Math.max(result.highestTier, horizontalTier);
					pairs++;
				}
			}
			if (pairs == 0) result.jammed.add(shell);
		}

		for (int x = -2; x <= 2; x++) {
			for (int y = -2; y <= 2; y++) {
				for (int z = -2; z <= 2; z++) {
					final BlockPos position = shell.offset(x, y, z);
					if (!world.hasChunkAt(position)) continue;
					final Block block = world.getBlockState(position).getBlock();
					final int chillerTier = getChillerTier(block);
					if (chillerTier > 0 && result.chillers.add(position.immutable())) {
						result.chillerCounts[chillerTier - 1]++;
					}
					if (block == Registration.PARTICLES_COLLIDER_BLOCK.get()) {
						result.colliders.add(position.immutable());
					}
					final TileEntity tileEntity = world.getBlockEntity(position);
					if (tileEntity instanceof ParticlesInjectorTileEntity) {
						result.injectors.add(position.immutable());
						result.controlPoints.add(position.immutable());
					} else if (tileEntity instanceof AcceleratorControlPointTileEntity) {
						result.controlPoints.add(position.immutable());
					}
				}
			}
		}
	}

	private static int sameMagnetTier(final ServerWorld world,
	                                  final BlockPos first, final BlockPos second) {
		if (!world.hasChunkAt(first) || !world.hasChunkAt(second)) return 0;
		final int tierFirst = getMagnetTier(world.getBlockState(first).getBlock());
		return tierFirst > 0 && tierFirst == getMagnetTier(world.getBlockState(second).getBlock())
			? tierFirst : 0;
	}

	private static int getMagnetTier(final Block block) {
		for (final Map.Entry<String, RegistryObject<Block>> entry
			: Registration.ELECTROMAGNET_BLOCKS.entrySet()) {
			if (entry.getValue().get() != block) continue;
			if (entry.getKey().contains("basic")) return 1;
			if (entry.getKey().contains("advanced")) return 2;
			if (entry.getKey().contains("superior")) return 3;
		}
		return 0;
	}

	private static int getChillerTier(final Block block) {
		if (!(block instanceof ChillerBlock)) return 0;
		return ((ChillerBlock) block).getTier().getIndex();
	}

	private static boolean isShell(final Block block) {
		return block == Registration.VOID_SHELL_PLAIN.get()
			|| block == Registration.VOID_SHELL_GLASS.get();
	}

	static final class Result {
		boolean valid;
		boolean loaded = true;
		String status = "Not scanned";
		int highestTier = 1;
		final int[] magnetCounts = new int[3];
		final int[] chillerCounts = new int[3];
		final Set<BlockPos> shells = new LinkedHashSet<>();
		final Set<BlockPos> jammed = new HashSet<>();
		final Set<BlockPos> chillers = new LinkedHashSet<>();
		final Set<BlockPos> injectors = new LinkedHashSet<>();
		final Set<BlockPos> controlPoints = new LinkedHashSet<>();
		final Set<BlockPos> colliders = new LinkedHashSet<>();

		int getMagnetCount() {
			return magnetCounts[0] + magnetCounts[1] + magnetCounts[2];
		}

		List<Object[]> describeControlPoints(final ServerWorld world) {
			final List<Object[]> result = new ArrayList<>();
			for (final BlockPos position : controlPoints) {
				final TileEntity tileEntity = world.getBlockEntity(position);
				if (!(tileEntity instanceof AcceleratorControlPointTileEntity)) continue;
				final AcceleratorControlPointTileEntity control =
					(AcceleratorControlPointTileEntity) tileEntity;
				final String type = tileEntity instanceof ParticlesInjectorTileEntity
					? "Injector" : isNearCollider(position) ? "Collider" : "Control";
				result.add(new Object[]{ position.getX(), position.getY(), position.getZ(),
					highestTier, type, control.isEnabled(), control.getControlChannel() });
			}
			return result;
		}

		private boolean isNearCollider(final BlockPos position) {
			for (final BlockPos collider : colliders) {
				if (collider.distSqr(position) <= 9.0D) return true;
			}
			return false;
		}
	}
}
