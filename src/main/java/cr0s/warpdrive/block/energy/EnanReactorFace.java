package cr0s.warpdrive.block.energy;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact core-air and stabilization-laser coordinates from the 1.12.2 ReactorFace table.
 * Laser entries have an instability index; all other entries are spaces that must remain air.
 */
public final class EnanReactorFace {

	public static final int MAX_INSTABILITIES = 16;
	private static final Map<String, EnanReactorFace> BY_NAME = new HashMap<>();
	private static final Map<EnanReactorTier, List<EnanReactorFace>> FACES =
		new EnumMap<>(EnanReactorTier.class);
	private static final Map<EnanReactorTier, List<EnanReactorFace>> LASERS =
		new EnumMap<>(EnanReactorTier.class);
	public static final EnanReactorFace UNKNOWN =
		new EnanReactorFace(null, -1, "unknown", BlockPos.ZERO, null);

	static {
		for (final EnanReactorTier tier : EnanReactorTier.values()) {
			FACES.put(tier, new ArrayList<>());
			LASERS.put(tier, new ArrayList<>());
		}

		addLaser(EnanReactorTier.BASIC, 0, "laser.basic.south", 0, 0, -2, Direction.NORTH);
		addLaser(EnanReactorTier.BASIC, 1, "laser.basic.north", 0, 0, 2, Direction.SOUTH);
		addLaser(EnanReactorTier.BASIC, 2, "laser.basic.east", -2, 0, 0, Direction.WEST);
		addLaser(EnanReactorTier.BASIC, 3, "laser.basic.west", 2, 0, 0, Direction.EAST);

		addLaser(EnanReactorTier.ADVANCED, 0, "laser.advanced.south+", 1, 2, -3, Direction.NORTH);
		addLaser(EnanReactorTier.ADVANCED, 1, "laser.advanced.south-", -1, 4, -3, Direction.NORTH);
		addLaser(EnanReactorTier.ADVANCED, 2, "laser.advanced.north-", -1, 2, 3, Direction.SOUTH);
		addLaser(EnanReactorTier.ADVANCED, 3, "laser.advanced.north+", 1, 4, 3, Direction.SOUTH);
		addLaser(EnanReactorTier.ADVANCED, 4, "laser.advanced.east-", -3, 2, -1, Direction.WEST);
		addLaser(EnanReactorTier.ADVANCED, 5, "laser.advanced.east+", -3, 4, 1, Direction.WEST);
		addLaser(EnanReactorTier.ADVANCED, 6, "laser.advanced.west+", 3, 2, 1, Direction.EAST);
		addLaser(EnanReactorTier.ADVANCED, 7, "laser.advanced.west-", 3, 4, -1, Direction.EAST);
		addCoreAir(EnanReactorTier.ADVANCED, 1, 0, 3, 0);

		addLaser(EnanReactorTier.SUPERIOR, 0, "laser.superior.south+", 1, 3, -4, Direction.NORTH);
		addLaser(EnanReactorTier.SUPERIOR, 1, "laser.superior.south-", -1, 5, -4, Direction.NORTH);
		addLaser(EnanReactorTier.SUPERIOR, 2, "laser.superior.north-", -1, 3, 4, Direction.SOUTH);
		addLaser(EnanReactorTier.SUPERIOR, 3, "laser.superior.north+", 1, 5, 4, Direction.SOUTH);
		addLaser(EnanReactorTier.SUPERIOR, 4, "laser.superior.east-", -4, 3, -1, Direction.WEST);
		addLaser(EnanReactorTier.SUPERIOR, 5, "laser.superior.east+", -4, 5, 1, Direction.WEST);
		addLaser(EnanReactorTier.SUPERIOR, 6, "laser.superior.west+", 4, 3, 1, Direction.EAST);
		addLaser(EnanReactorTier.SUPERIOR, 7, "laser.superior.west-", 4, 5, -1, Direction.EAST);
		addLaser(EnanReactorTier.SUPERIOR, 8, "laser.superior.south--", -2, 2, -4, Direction.NORTH);
		addLaser(EnanReactorTier.SUPERIOR, 9, "laser.superior.south++", 2, 6, -4, Direction.NORTH);
		addLaser(EnanReactorTier.SUPERIOR, 10, "laser.superior.north++", 2, 2, 4, Direction.SOUTH);
		addLaser(EnanReactorTier.SUPERIOR, 11, "laser.superior.north--", -2, 6, 4, Direction.SOUTH);
		addLaser(EnanReactorTier.SUPERIOR, 12, "laser.superior.east++", -4, 2, 2, Direction.WEST);
		addLaser(EnanReactorTier.SUPERIOR, 13, "laser.superior.east--", -4, 6, -2, Direction.WEST);
		addLaser(EnanReactorTier.SUPERIOR, 14, "laser.superior.west--", 4, 2, -2, Direction.EAST);
		addLaser(EnanReactorTier.SUPERIOR, 15, "laser.superior.west++", 4, 6, 2, Direction.EAST);
		addCoreAir(EnanReactorTier.SUPERIOR, 2, 0, 4, 0);

		for (final EnanReactorTier tier : EnanReactorTier.values()) {
			FACES.put(tier, Collections.unmodifiableList(FACES.get(tier)));
			LASERS.put(tier, Collections.unmodifiableList(LASERS.get(tier)));
		}
	}

	@Nullable private final EnanReactorTier tier;
	private final int instabilityIndex;
	private final String name;
	private final BlockPos offset;
	@Nullable private final Direction laserFacing;

	private EnanReactorFace(@Nullable final EnanReactorTier tier, final int instabilityIndex,
	                        final String name, final BlockPos offset,
	                        @Nullable final Direction laserFacing) {
		this.tier = tier;
		this.instabilityIndex = instabilityIndex;
		this.name = name;
		this.offset = offset;
		this.laserFacing = laserFacing;
		BY_NAME.put(name, this);
	}

	private static void addLaser(final EnanReactorTier tier, final int instabilityIndex,
	                             final String name, final int x, final int y, final int z,
	                             final Direction laserFacing) {
		final EnanReactorFace laser = new EnanReactorFace(tier, instabilityIndex, name,
			new BlockPos(x, y, z), laserFacing);
		FACES.get(tier).add(laser);
		LASERS.get(tier).add(laser);
		final BlockPos lensOffset = laser.offset.subtract(new BlockPos(
			laserFacing.getStepX(), laserFacing.getStepY(), laserFacing.getStepZ()));
		FACES.get(tier).add(new EnanReactorFace(tier, -1, name + ".lens", lensOffset, null));
	}

	private static void addCoreAir(final EnanReactorTier tier, final int radius,
	                               final int xOffset, final int yOffset, final int zOffset) {
		final double squaredRadiusHigh = (radius + 0.5D) * (radius + 0.5D);
		final int radiusCeil = radius + 1;
		for (int x = -radiusCeil; x <= radiusCeil; x++) {
			final double x2 = (x + 0.5D) * (x + 0.5D);
			for (int y = -radiusCeil; y <= radiusCeil; y++) {
				final double x2y2 = x2 + (y + 0.5D) * (y + 0.5D);
				for (int z = -radiusCeil; z <= radiusCeil; z++) {
					final double squaredRange = x2y2 + (z + 0.5D) * (z + 0.5D);
					if (squaredRange > squaredRadiusHigh) continue;
					final String name = String.format("core.%s.[%d,%d,%d]", tier.getName(), x, y, z);
					final EnanReactorFace air = new EnanReactorFace(tier, -1, name,
						new BlockPos(xOffset + x, yOffset + y, zOffset + z), null);
					FACES.get(tier).add(air);
				}
			}
		}
	}

	public static List<EnanReactorFace> getFaces(final EnanReactorTier tier) {
		return FACES.get(tier);
	}

	public static List<EnanReactorFace> getLasers(final EnanReactorTier tier) {
		return LASERS.get(tier);
	}

	public static EnanReactorFace byName(final String name) {
		final EnanReactorFace face = BY_NAME.get(name);
		return face == null ? UNKNOWN : face;
	}

	@Nullable public EnanReactorTier getTier() { return tier; }
	public int getInstabilityIndex() { return instabilityIndex; }
	public String getName() { return name; }
	public BlockPos getOffset() { return offset; }
	@Nullable public Direction getLaserFacing() { return laserFacing; }
	public boolean isLaser() { return instabilityIndex >= 0; }
}
