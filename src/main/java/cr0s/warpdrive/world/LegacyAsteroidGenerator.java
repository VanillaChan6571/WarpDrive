package cr0s.warpdrive.world;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ISeedReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The data and metaball geometry used by WarpDrive 1.12.2's {@code MetaOrbInstance}.
 *
 * The old implementation built an entire orb at once, sometimes through an entity spread over
 * several ticks. In 1.16 each caller deterministically reconstructs the same orb and asks this
 * class to write only the current chunk's X/Z slice. That retains the old silhouettes, shell
 * sizes, and filler probabilities without force-loading neighbours or retaining a large block
 * list.
 */
final class LegacyAsteroidGenerator {

	static final int MIN_CENTRE_Y = 55;
	static final int MAX_CENTRE_Y = 127;
	static final int MIN_BORDER_Y = 5;
	static final int MAX_BORDER_Y = 200;
	private static final String[] GAS_COLORS = {
		"blue", "red", "green", "yellow", "dark", "darkness",
		"white", "milk", "orange", "siren", "gray", "violet"
	};

	private interface StateSource {
		BlockState get();
	}

	private static final class Deposit {
		private final double ratio;
		private final StateSource state;

		private Deposit(final double ratio, final StateSource state) {
			this.ratio = ratio;
			this.state = state;
		}
	}

	private static final class BaseMaterial {
		private final int weight;
		private final StateSource state;

		private BaseMaterial(final int weight, final StateSource state) {
			this.weight = weight;
			this.state = state;
		}
	}

	/** One instantiated legacy fillerSet. */
	private static final class MaterialSet {
		private final Deposit[] deposits;
		private final BaseMaterial[] bases;
		private final double totalRatio;
		private final int totalWeight;

		private MaterialSet(final Deposit[] deposits, final BaseMaterial... bases) {
			this.deposits = deposits;
			this.bases = bases;
			double ratio = 0.0D;
			for (final Deposit deposit : deposits) {
				ratio += deposit.ratio;
			}
			totalRatio = ratio;
			int weight = 0;
			for (final BaseMaterial base : bases) {
				weight += base.weight;
			}
			totalWeight = weight;
		}

		private BlockState pick(final long seed, final int x, final int y, final int z) {
			final double value = unitHash(seed, x, y, z);
			double cumulative = 0.0D;
			for (final Deposit deposit : deposits) {
				cumulative += deposit.ratio;
				if (value < cumulative) {
					return deposit.state.get();
				}
			}

			// Ratio entries have fixed odds. The remaining interval is shared by weighted bases.
			final double weightedValue = totalRatio >= 1.0D
				? 0.0D : (value - totalRatio) / (1.0D - totalRatio);
			int roll = Math.min(totalWeight - 1, (int) (weightedValue * totalWeight));
			for (final BaseMaterial base : bases) {
				roll -= base.weight;
				if (roll < 0) {
					return base.state.get();
				}
			}
			return bases[0].state.get();
		}
	}

	private static final class WeightedSet {
		private final int weight;
		private final MaterialSet materialSet;

		private WeightedSet(final int weight, final MaterialSet materialSet) {
			this.weight = weight;
			this.materialSet = materialSet;
		}
	}

	private static final class MaterialGroup {
		private final WeightedSet[] sets;
		private final int totalWeight;

		private MaterialGroup(final WeightedSet... sets) {
			this.sets = sets;
			int total = 0;
			for (final WeightedSet set : sets) {
				total += set.weight;
			}
			totalWeight = total;
		}

		private MaterialSet pick(final Random random) {
			int roll = random.nextInt(totalWeight);
			for (final WeightedSet set : sets) {
				roll -= set.weight;
				if (roll < 0) {
					return set.materialSet;
				}
			}
			return sets[0].materialSet;
		}
	}

	private static final class ShellDefinition {
		private final int minThickness;
		private final int maxThickness;
		private final MaterialGroup materials;

		private ShellDefinition(final int minThickness, final int maxThickness,
		                        final MaterialGroup materials) {
			this.minThickness = minThickness;
			this.maxThickness = maxThickness;
			this.materials = materials;
		}
	}

	private static final class Variant {
		private final int minCores;
		private final int maxCores;
		private final double minRadius;
		private final double relativeRadius;
		private final StateSource core;
		private final ShellDefinition[] shells;

		private Variant(final int minCores, final int maxCores,
		                final double minRadius, final double relativeRadius,
		                final StateSource core, final ShellDefinition... shells) {
			this.minCores = minCores;
			this.maxCores = maxCores;
			this.minRadius = minRadius;
			this.relativeRadius = relativeRadius;
			this.core = core;
			this.shells = shells;
		}
	}

	private static final class Core {
		private final int x;
		private final int y;
		private final int z;

		private Core(final int x, final int y, final int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		private boolean isAt(final int x, final int y, final int z) {
			return this.x == x && this.y == y && this.z == z;
		}
	}

	private static final class MetaOrb {
		private final int x;
		private final int y;
		private final int z;
		private final int bound;
		private final int totalThickness;
		private final Core[] cores;
		private final StateSource coreState;
		private final MaterialSet[] shellMaterials;
		private final int[] shellThicknesses;
		private final long materialSeed;

		private MetaOrb(final int x, final int y, final int z, final int bound,
		                final int totalThickness, final Core[] cores, final StateSource coreState,
		                final MaterialSet[] shellMaterials, final int[] shellThicknesses,
		                final long materialSeed) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.bound = bound;
			this.totalThickness = totalThickness;
			this.cores = cores;
			this.coreState = coreState;
			this.shellMaterials = shellMaterials;
			this.shellThicknesses = shellThicknesses;
			this.materialSeed = materialSeed;
		}
	}

	private static StateSource state(final Block block) {
		return block::defaultBlockState;
	}

	private static Deposit deposit(final double ratio, final Block block) {
		return new Deposit(ratio, state(block));
	}

	private static Deposit deposit(final double ratio, final StateSource state) {
		return new Deposit(ratio, state);
	}

	private static BaseMaterial base(final int weight, final Block block) {
		return new BaseMaterial(weight, state(block));
	}

	private static BaseMaterial base(final int weight, final StateSource state) {
		return new BaseMaterial(weight, state);
	}

	private static MaterialSet set(final Deposit[] deposits, final BaseMaterial... bases) {
		return new MaterialSet(deposits, bases);
	}

	private static WeightedSet weighted(final int weight, final MaterialSet materialSet) {
		return new WeightedSet(weight, materialSet);
	}

	private static MaterialSet overlay(final MaterialSet original,
	                                   final Deposit[] extraDeposits,
	                                   final BaseMaterial... extraBases) {
		final Deposit[] deposits = new Deposit[extraDeposits.length + original.deposits.length];
		System.arraycopy(extraDeposits, 0, deposits, 0, extraDeposits.length);
		System.arraycopy(original.deposits, 0, deposits, extraDeposits.length, original.deposits.length);
		final BaseMaterial[] bases = new BaseMaterial[original.bases.length + extraBases.length];
		System.arraycopy(original.bases, 0, bases, 0, original.bases.length);
		System.arraycopy(extraBases, 0, bases, original.bases.length, extraBases.length);
		return new MaterialSet(deposits, bases);
	}

	private static final Deposit[] NO_DEPOSITS = {};
	private static final Deposit[] COMMON_DEPOSITS = {
		deposit(0.060D, Blocks.COAL_ORE),
		deposit(0.040D, Blocks.IRON_ORE),
		deposit(0.008D, Blocks.REDSTONE_ORE)
	};
	private static final Deposit[] UNCOMMON_DEPOSITS = {
		deposit(0.008D, Blocks.REDSTONE_ORE),
		deposit(0.010D, Blocks.GOLD_ORE),
		deposit(0.008D, Blocks.LAPIS_ORE)
	};
	private static final Deposit[] RARE_DEPOSITS = {
		deposit(0.004D, Blocks.GOLD_ORE),
		deposit(0.004D, Blocks.DIAMOND_ORE),
		// In the vanilla-only 1.12 filler group, 90% of rare sets selected emerald gems.
		deposit(0.0009D, Blocks.EMERALD_ORE)
	};
	private static final Deposit[] ALL_ORE_DEPOSITS = {
		deposit(0.060D, Blocks.COAL_ORE),
		deposit(0.040D, Blocks.IRON_ORE),
		deposit(0.008D, Blocks.REDSTONE_ORE),
		deposit(0.008D, Blocks.REDSTONE_ORE),
		deposit(0.010D, Blocks.GOLD_ORE),
		deposit(0.008D, Blocks.LAPIS_ORE),
		deposit(0.004D, Blocks.GOLD_ORE),
		deposit(0.004D, Blocks.DIAMOND_ORE),
		deposit(0.0009D, Blocks.EMERALD_ORE)
	};
	private static final Deposit[] NETHER_DEPOSITS = {
		deposit(0.070D, Blocks.NETHER_QUARTZ_ORE),
		deposit(0.0075D, Blocks.GLOWSTONE),
		deposit(0.010D, Blocks.LAVA)
	};

	private static final MaterialGroup COMMON = new MaterialGroup(
		weighted(20, set(COMMON_DEPOSITS, base(1, Blocks.STONE))),
		weighted(5, set(COMMON_DEPOSITS, base(5, Blocks.STONE), base(1, Blocks.COBBLESTONE))),
		weighted(1, set(COMMON_DEPOSITS, base(1, Blocks.COBBLESTONE))),
		weighted(2, set(COMMON_DEPOSITS, base(1, Blocks.SANDSTONE), base(1, Blocks.CLAY)))
	);
	private static final MaterialGroup UNCOMMON = new MaterialGroup(
		weighted(10, set(UNCOMMON_DEPOSITS, base(1, Blocks.STONE))),
		weighted(5, set(UNCOMMON_DEPOSITS, base(5, Blocks.STONE), base(1, Blocks.COBBLESTONE))),
		weighted(1, set(UNCOMMON_DEPOSITS, base(1, Blocks.COBBLESTONE))),
		weighted(2, set(UNCOMMON_DEPOSITS, base(1, Blocks.SANDSTONE), base(1, Blocks.CLAY)))
	);
	private static final MaterialSet RARE_STONE_SET = set(RARE_DEPOSITS,
		base(100, Blocks.STONE), base(5, Blocks.IRON_ORE), base(1, Blocks.LAVA));
	private static final MaterialSet RARE_OBSIDIAN_SET = set(RARE_DEPOSITS,
		base(100, Blocks.OBSIDIAN), base(5, Blocks.DIAMOND_ORE));
	private static final MaterialSet NETHER_SET = set(NETHER_DEPOSITS,
		base(100, Blocks.NETHERRACK), base(5, Blocks.LAVA));
	private static final MaterialSet END_SET = new MaterialSet(
		new Deposit[]{ deposit(0.001D, () -> Registration.IRIDIUM_BLOCK.get().defaultBlockState()) },
		base(100, Blocks.END_STONE));

	private static final MaterialGroup RARE = new MaterialGroup(
		weighted(10, RARE_STONE_SET), weighted(2, RARE_OBSIDIAN_SET));
	private static final MaterialGroup STONE_LARGE = new MaterialGroup(
		weighted(1, set(ALL_ORE_DEPOSITS, base(10, Blocks.STONE), base(5, Blocks.COBBLESTONE)))
	);
	private static final MaterialGroup NETHER = new MaterialGroup(
		weighted(1, NETHER_SET)
	);
	private static final MaterialGroup END = new MaterialGroup(
		weighted(1, END_SET)
	);
	private static final MaterialGroup SURFACE = createSurfaceGroup();
	private static final MaterialGroup NETHER_SURFACE = createNetherSurfaceGroup();
	private static final MaterialGroup END_SURFACE = createEndSurfaceGroup();

	private static final Variant COMMON_CLUSTER = new Variant(6, 10, 4.0D, 0.5D, null,
		new ShellDefinition(2, 5, COMMON));
	private static final Variant UNCOMMON_MEDIUM = new Variant(1, 3, 2.0D, 0.5D, null,
		new ShellDefinition(2, 4, UNCOMMON));
	private static final Variant COMMON_GEODE = new Variant(5, 15, 1.0D, 0.5D, null,
		new ShellDefinition(6, 7, COMMON),
		new ShellDefinition(0, 1, SURFACE));
	private static final Variant RARE_GEODE = new Variant(1, 2, 2.0D, 0.5D, state(Blocks.DIAMOND_ORE),
		new ShellDefinition(2, 4, RARE),
		new ShellDefinition(1, 1, UNCOMMON));
	private static final Variant STONE_LARGE_VARIANT = new Variant(1, 2, 3.0D, 0.8D, state(Blocks.DIAMOND_ORE),
		new ShellDefinition(2, 6, STONE_LARGE));
	private static final Variant NETHER_SMALL = new Variant(1, 2, 2.0D, 0.5D, state(Blocks.DIAMOND_ORE),
		new ShellDefinition(2, 6, NETHER));
	private static final Variant END_SMALL = new Variant(1, 2, 2.0D, 0.5D, state(Blocks.DIAMOND_ORE),
		new ShellDefinition(2, 6, END));

	private static MaterialGroup createSurfaceGroup() {
		final List<WeightedSet> sets = new ArrayList<>();
		sets.add(weighted(75, set(NO_DEPOSITS, base(100, Blocks.STONE))));
		sets.add(weighted(45, set(NO_DEPOSITS, base(100, Blocks.STONE), base(20, Blocks.COBBLESTONE))));
		sets.add(weighted(20, set(NO_DEPOSITS, base(100, Blocks.DIRT))));
		sets.add(weighted(30, set(NO_DEPOSITS, base(100, Blocks.PACKED_ICE), base(20, Blocks.ICE))));

		final Block[] terracotta = {
			Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
			Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
			Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
			Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
			Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA,
			Blocks.BLACK_TERRACOTTA
		};
		final Block[] concrete = {
			Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE,
			Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE,
			Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE,
			Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
			Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE,
			Blocks.BLACK_CONCRETE
		};
		for (final Block block : terracotta) {
			sets.add(weighted(5, set(NO_DEPOSITS, base(100, block))));
		}
		for (final Block block : concrete) {
			sets.add(weighted(5, set(NO_DEPOSITS, base(100, block))));
		}
		sets.add(weighted(5, set(NO_DEPOSITS, base(100, Blocks.TERRACOTTA))));
		sets.add(weighted(10, set(NO_DEPOSITS, base(100, Blocks.SANDSTONE))));
		return new MaterialGroup(sets.toArray(new WeightedSet[0]));
	}

	private static MaterialGroup createNetherSurfaceGroup() {
		return new MaterialGroup(
			weighted(50, set(NO_DEPOSITS, base(100, Blocks.NETHERRACK))),
			weighted(50, set(NO_DEPOSITS, base(100, Blocks.NETHERRACK), base(2, Blocks.NETHER_QUARTZ_ORE))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.ORANGE_TERRACOTTA))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.PINK_TERRACOTTA))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.RED_TERRACOTTA))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.ORANGE_CONCRETE))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.PINK_CONCRETE))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.RED_CONCRETE))),
			weighted(20, set(NO_DEPOSITS, base(100, Blocks.SOUL_SAND)))
		);
	}

	private static MaterialGroup createEndSurfaceGroup() {
		return new MaterialGroup(
			weighted(50, set(NO_DEPOSITS, base(100, Blocks.END_STONE))),
			weighted(50, set(NO_DEPOSITS, base(100, Blocks.END_STONE), base(15, Blocks.OBSIDIAN))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.WHITE_TERRACOTTA))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.YELLOW_TERRACOTTA))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.WHITE_CONCRETE))),
			weighted(10, set(NO_DEPOSITS, base(100, Blocks.YELLOW_CONCRETE))),
			weighted(15, set(NO_DEPOSITS, base(100, Blocks.CLAY))),
			weighted(15, set(NO_DEPOSITS, base(100, Blocks.SANDSTONE)))
		);
	}

	private static MaterialSet gasSet(final Random random) {
		final String color = GAS_COLORS[random.nextInt(GAS_COLORS.length)];
		final StateSource gas = () -> Registration.GAS_BLOCKS.get(color).get().defaultBlockState();
		return set(NO_DEPOSITS, base(1, gas));
	}

	/** Active vanilla entries from fillerSets-default.xml:moon.core. */
	private static MaterialSet moonCore(final Random random) {
		int roll = random.nextInt(18);
		if ((roll -= 6) < 0) {
			return set(NO_DEPOSITS, base(100, Blocks.LAVA));
		}
		if ((roll -= 5) < 0) {
			return RARE_STONE_SET;
		}
		if ((roll -= 1) < 0) {
			return RARE_OBSIDIAN_SET;
		}
		if ((roll -= 2) < 0) {
			return set(new Deposit[]{ deposit(0.075D, Blocks.ICE), deposit(0.005D, Blocks.PACKED_ICE) },
				base(100, Blocks.WATER));
		}
		if ((roll -= 1) < 0) {
			return set(new Deposit[]{ deposit(0.020D, Blocks.LAPIS_BLOCK) },
				base(100, Blocks.PACKED_ICE));
		}
		if ((roll -= 1) < 0) {
			return set(new Deposit[]{ deposit(0.005D, Blocks.DIAMOND_BLOCK) },
				base(100, Blocks.QUARTZ_BLOCK), base(100, Blocks.CHISELED_QUARTZ_BLOCK));
		}
		if ((roll -= 1) < 0) {
			return set(new Deposit[]{
				deposit(0.0015D, () -> Registration.IRIDIUM_BLOCK.get().defaultBlockState()) },
				base(100, Blocks.QUARTZ_BLOCK));
		}
		return set(new Deposit[]{ deposit(0.005D, Blocks.IRON_BLOCK) },
			base(33, Blocks.QUARTZ_PILLAR), base(33, Blocks.QUARTZ_BRICKS),
			base(33, Blocks.SMOOTH_QUARTZ));
	}

	/** The non-empty vanilla entries from fillerSets-default.xml:moon.flow. */
	private static MaterialSet moonFlow(final Random random) {
		int roll = random.nextInt(20);
		if ((roll -= 5) < 0) return set(NO_DEPOSITS, base(100, Blocks.LAVA));
		if ((roll -= 1) < 0) return set(NO_DEPOSITS, base(100, Blocks.SAND));
		if ((roll -= 1) < 0) return set(NO_DEPOSITS, base(100, Blocks.GRAVEL));
		if ((roll -= 3) < 0) return set(NO_DEPOSITS,
			base(100, Blocks.ICE), base(10, Blocks.PACKED_ICE));
		if ((roll -= 5) < 0) return set(NO_DEPOSITS, base(100, Blocks.CLAY));
		return set(NO_DEPOSITS, base(100, Blocks.WATER));
	}

	private static MaterialSet bodyShell(final CelestialSystemLayout.Profile profile,
	                                     final int shellIndex, final long seed) {
		final Random random = new Random(seed);
		switch (profile) {
		case OVERWORLD_BASE:
			if (shellIndex == 0) return moonCore(random);
			if (shellIndex == 1) return UNCOMMON.pick(random);
			if (shellIndex == 2) return moonFlow(random);
			if (shellIndex == 3) return COMMON.pick(random);
			return SURFACE.pick(random);

		case OVERWORLD_RARE:
			if (shellIndex == 0) return moonCore(random);
			if (shellIndex == 1) return RARE.pick(random);
			if (shellIndex == 2) return moonFlow(random);
			if (shellIndex == 3) return COMMON.pick(random);
			return SURFACE.pick(random);

		case OVERWORLD_EMPTY:
			if (shellIndex == 0 || shellIndex == 3) {
				return set(NO_DEPOSITS, base(100, Blocks.OBSIDIAN));
			}
			if (shellIndex == 1) return gasSet(random);
			if (shellIndex == 2) return set(new Deposit[]{ deposit(0.001D, Blocks.GLOWSTONE) },
				base(100, Blocks.OBSIDIAN));
			if (shellIndex == 4) return COMMON.pick(random);
			return SURFACE.pick(random);

		case OVERWORLD_SHIP:
			if (shellIndex == 0) return gasSet(random);
			if (shellIndex == 1) return UNCOMMON.pick(random);
			if (shellIndex == 2) return set(NO_DEPOSITS, base(100, Blocks.OBSIDIAN));
			if (shellIndex == 3) return COMMON.pick(random);
			return SURFACE.pick(random);

		case OVERWORLD_CORRUPTED:
			if (shellIndex == 0) return RARE.pick(random);
			if (shellIndex == 1) return overlay(UNCOMMON.pick(random), new Deposit[]{
				deposit(0.10D, Blocks.GRAVEL), deposit(0.05D, Blocks.LAVA) });
			if (shellIndex == 2) return overlay(COMMON.pick(random), new Deposit[]{
				deposit(0.10D, Blocks.SAND), deposit(0.20D, Blocks.AIR) });
			return overlay(SURFACE.pick(random),
				new Deposit[]{ deposit(0.35D, Blocks.AIR) });

		case NETHER:
			if (shellIndex == 0) return set(NO_DEPOSITS, base(1, Blocks.AIR));
			if (shellIndex == 1) return set(NO_DEPOSITS, base(100, Blocks.OBSIDIAN));
			if (shellIndex == 2) return overlay(NETHER_SET,
				new Deposit[]{ deposit(0.20D, Blocks.RED_SAND) });
			if (shellIndex == 3) return overlay(NETHER_SET,
				new Deposit[]{ deposit(0.20D, Blocks.NETHER_BRICKS) });
			return NETHER_SURFACE.pick(random);

		case NETHER_CORRUPTED:
			if (shellIndex == 0) return set(NO_DEPOSITS, base(1, Blocks.AIR));
			if (shellIndex == 1) return set(NO_DEPOSITS, base(100, Blocks.OBSIDIAN));
			if (shellIndex == 2) return overlay(NETHER_SET,
				new Deposit[]{ deposit(0.20D, Blocks.RED_SAND) });
			if (shellIndex == 3) return overlay(NETHER_SET, new Deposit[]{
				deposit(0.20D, Blocks.NETHER_BRICKS), deposit(0.20D, Blocks.AIR) });
			return overlay(NETHER_SURFACE.pick(random),
				new Deposit[]{ deposit(0.25D, Blocks.AIR) });

		case END_HOLLOW:
			if (shellIndex == 0) return set(new Deposit[]{ deposit(0.001D, Blocks.OBSIDIAN) },
				base(100, Blocks.AIR));
			if (shellIndex == 1) return set(NO_DEPOSITS, base(100, Blocks.OBSIDIAN));
			if (shellIndex == 2) return overlay(END_SET, NO_DEPOSITS, base(60, Blocks.OBSIDIAN));
			return END_SURFACE.pick(random);

		case END:
			if (shellIndex == 0) return overlay(END_SET, NO_DEPOSITS,
				base(30, Blocks.OBSIDIAN),
				base(1, () -> Registration.IRIDIUM_BLOCK.get().defaultBlockState()));
			if (shellIndex == 1) return overlay(END_SET, NO_DEPOSITS, base(60, Blocks.OBSIDIAN));
			return END_SURFACE.pick(random);

		default:
			return set(NO_DEPOSITS, base(1, Blocks.STONE));
		}
	}

	private LegacyAsteroidGenerator() {
	}

	/** Selects an asteroid exactly from the active vanilla 1.12 structure entries. */
	private static Variant pickAsteroidVariant(final Random random) {
		final double value = random.nextDouble();
		if (value < 0.040D) {
			return NETHER_SMALL;
		}
		if (value < 0.068D) {
			return END_SMALL;
		}

		// The remaining probability is distributed by the XML weights 100/100/30/10/10.
		int roll = random.nextInt(250);
		if ((roll -= 100) < 0) return COMMON_CLUSTER;
		if ((roll -= 100) < 0) return UNCOMMON_MEDIUM;
		if ((roll -= 30) < 0) return COMMON_GEODE;
		if ((roll -= 10) < 0) return RARE_GEODE;
		return STONE_LARGE_VARIANT;
	}

	static boolean placeRandomAsteroidSlice(final ISeedReader world, final ChunkPos chunk,
	                                        final Random random, final int x, final int y, final int z) {
		return placeSlice(world, chunk, instantiate(pickAsteroidVariant(random), random, x, y, z));
	}

	static boolean placeRandomGasCloudSlice(final ISeedReader world, final ChunkPos chunk,
	                                        final Random random, final int x, final int y, final int z) {
		final boolean big = random.nextBoolean();
		final int color = random.nextInt(GAS_COLORS.length);
		final StateSource gas = () -> Registration.GAS_BLOCKS.get(GAS_COLORS[color])
			.get().defaultBlockState();
		final MaterialGroup gasGroup = new MaterialGroup(
			weighted(1, new MaterialSet(NO_DEPOSITS, new BaseMaterial(1, gas))));
		final Variant cloud = big
			? new Variant(5, 10, 5.0D, 0.5D, null, new ShellDefinition(5, 15, gasGroup))
			: new Variant(3, 7, 3.0D, 0.5D, null, new ShellDefinition(2, 8, gasGroup));
		return placeSlice(world, chunk, instantiate(cloud, random, x, y, z));
	}

	/** Writes a legacy layered Orb as a deterministic slice of a native 1.16 celestial body. */
	static boolean placeCelestialBodySlice(final ISeedReader world, final ChunkPos chunk,
	                                      final CelestialSystemLayout.Body body) {
		final int chunkMinX = chunk.getMinBlockX();
		final int chunkMinZ = chunk.getMinBlockZ();
		final int chunkMaxX = chunkMinX + 15;
		final int chunkMaxZ = chunkMinZ + 15;
		if (body.x + body.radius + 1 < chunkMinX || body.x - body.radius - 1 > chunkMaxX
		 || body.z + body.radius + 1 < chunkMinZ || body.z - body.radius - 1 > chunkMaxZ) {
			return false;
		}

		final MaterialSet[] materials = new MaterialSet[body.shellThicknesses.length];
		for (int index = 0; index < materials.length; index++) {
			materials[index] = bodyShell(body.profile, index, body.shellSeeds[index]);
		}

		final int minX = Math.max(chunkMinX, body.x - body.radius - 1);
		final int maxX = Math.min(chunkMaxX, body.x + body.radius + 1);
		final int minZ = Math.max(chunkMinZ, body.z - body.radius - 1);
		final int maxZ = Math.min(chunkMaxZ, body.z + body.radius + 1);
		final int minY = Math.max(1, body.y - body.radius - 1);
		final int maxY = Math.min(254, body.y + body.radius + 1);
		final double squaredRadius = (body.radius + 0.5D) * (body.radius + 0.5D);
		final BlockPos.Mutable pos = new BlockPos.Mutable();
		boolean placed = false;

		for (int x = minX; x <= maxX; x++) {
			final double dx = Math.abs(x - body.x) + 0.5D;
			for (int z = minZ; z <= maxZ; z++) {
				final double dz = Math.abs(z - body.z) + 0.5D;
				final double horizontalSquared = dx * dx + dz * dz;
				if (horizontalSquared > squaredRadius) {
					continue;
				}
				for (int y = minY; y <= maxY; y++) {
					final double dy = Math.abs(y - body.y) + 0.5D;
					final double squaredRange = horizontalSquared + dy * dy;
					if (squaredRange > squaredRadius) {
						continue;
					}

					final int shell = bodyShellForRange(body.shellThicknesses,
						(int) Math.round(squaredRange));
					final BlockState blockState = materials[shell].pick(
						body.shellSeeds[shell], x, y, z);
					// An air filler creates a cavity. GasBlock also reports itself as air, so compare the
					// actual block rather than BlockState#isAir here.
					if (blockState.getBlock() == Blocks.AIR) {
						continue;
					}

					pos.set(x, y, z);
					if (!world.getBlockState(pos).isAir()) {
						continue;
					}
					world.setBlock(pos, blockState, 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	private static int bodyShellForRange(final int[] thicknesses, final int squaredRange) {
		int radius = 0;
		for (int index = 0; index < thicknesses.length; index++) {
			radius += thicknesses[index];
			if (squaredRange <= radius * radius) {
				return index;
			}
		}
		return thicknesses.length - 1;
	}

	private static MetaOrb instantiate(final Variant variant, final Random random,
	                                   final int x, final int requestedY, final int z) {
		final MaterialSet[] shellMaterials = new MaterialSet[variant.shells.length];
		final int[] shellThicknesses = new int[variant.shells.length];
		int totalThickness = 0;
		for (int index = 0; index < variant.shells.length; index++) {
			final ShellDefinition shell = variant.shells[index];
			// OrbShell.instantiate selected its dynamic filler import before rolling thickness.
			shellMaterials[index] = shell.materials.pick(random);
			shellThicknesses[index] = randomRange(random, shell.minThickness, shell.maxThickness);
			totalThickness += shellThicknesses[index];
		}

		final int requestedCores = randomRange(random, variant.minCores, variant.maxCores);
		final double radiusMax = Math.max(variant.minRadius, variant.relativeRadius * totalThickness);
		final double diameter = Math.max(1.0D, 2.0D * radiusMax);
		final List<Core> cores = new ArrayList<>(requestedCores);
		int radiusActual = 0;
		for (int index = 0; index < requestedCores; index++) {
			for (int attempt = 0; attempt < 10; attempt++) {
				final Core candidate = new Core(
					(int) Math.round(-radiusMax + diameter * random.nextDouble()),
					(int) Math.round(-radiusMax + diameter * random.nextDouble()),
					(int) Math.round(-radiusMax + diameter * random.nextDouble()));
				if (contains(cores, candidate)) {
					continue;
				}
				cores.add(candidate);
				radiusActual = Math.max(radiusActual,
					Math.max(Math.abs(candidate.x), Math.max(Math.abs(candidate.y), Math.abs(candidate.z))));
				break;
			}
		}

		// This deliberately retains MetaOrbInstance's conservative locations.size() term.
		final int bound = totalThickness + radiusActual + cores.size();
		final int y = Math.min(MAX_BORDER_Y - bound,
			Math.max(requestedY, MIN_BORDER_Y + bound));
		return new MetaOrb(x, y, z, bound, totalThickness, cores.toArray(new Core[0]),
			variant.core, shellMaterials, shellThicknesses, random.nextLong());
	}

	private static boolean contains(final List<Core> cores, final Core candidate) {
		for (final Core core : cores) {
			if (core.isAt(candidate.x, candidate.y, candidate.z)) {
				return true;
			}
		}
		return false;
	}

	/** Implements the inverse-square metaball equation from MetaOrbInstance, one chunk at a time. */
	private static boolean placeSlice(final ISeedReader world, final ChunkPos chunk, final MetaOrb orb) {
		final int chunkMinX = chunk.getMinBlockX();
		final int chunkMinZ = chunk.getMinBlockZ();
		final int chunkMaxX = chunkMinX + 15;
		final int chunkMaxZ = chunkMinZ + 15;
		if (orb.x + orb.bound < chunkMinX || orb.x - orb.bound > chunkMaxX
		 || orb.z + orb.bound < chunkMinZ || orb.z - orb.bound > chunkMaxZ) {
			return false;
		}

		final double squaredRadiusHigh = (orb.totalThickness + 0.5D) * (orb.totalThickness + 0.5D);
		final int minX = Math.max(chunkMinX, orb.x - orb.bound);
		final int maxX = Math.min(chunkMaxX, orb.x + orb.bound);
		final int minZ = Math.max(chunkMinZ, orb.z - orb.bound);
		final int maxZ = Math.min(chunkMaxZ, orb.z + orb.bound);
		final int minY = Math.max(1, orb.y - orb.bound);
		final int maxY = Math.min(254, orb.y + orb.bound);
		final BlockPos.Mutable pos = new BlockPos.Mutable();
		boolean placed = false;

		for (int x = minX; x <= maxX; x++) {
			final int relativeX = x - orb.x;
			for (int z = minZ; z <= maxZ; z++) {
				final int relativeZ = z - orb.z;
				for (int y = minY; y <= maxY; y++) {
					final int relativeY = y - orb.y;
					double reciprocalSum = 0.0D;
					for (final Core core : orb.cores) {
						final double dx = relativeX - core.x + 0.5D;
						final double dy = relativeY - core.y + 0.5D;
						final double dz = relativeZ - core.z + 0.5D;
						reciprocalSum += 1.0D / (dx * dx + dy * dy + dz * dz);
					}
					final double squaredStrength = orb.cores.length / reciprocalSum;
					if (squaredStrength > squaredRadiusHigh) {
						continue;
					}

					pos.set(x, y, z);
					if (!world.getBlockState(pos).isAir()) {
						continue;
					}

					BlockState blockState = null;
					if (orb.coreState != null) {
						for (final Core core : orb.cores) {
							if (core.isAt(relativeX, relativeY, relativeZ)) {
								blockState = orb.coreState.get();
								break;
							}
						}
					}
					if (blockState == null) {
						final int squaredRange = (int) Math.round(squaredStrength);
						blockState = shellFor(orb, squaredRange).pick(orb.materialSeed, x, y, z);
					}
					world.setBlock(pos, blockState, 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	private static MaterialSet shellFor(final MetaOrb orb, final int squaredRange) {
		int radius = 0;
		for (int index = 0; index < orb.shellMaterials.length; index++) {
			radius += orb.shellThicknesses[index];
			if (squaredRange <= radius * radius) {
				return orb.shellMaterials[index];
			}
		}
		return orb.shellMaterials[orb.shellMaterials.length - 1];
	}

	private static int randomRange(final Random random, final int min, final int max) {
		return min == max ? min : min + random.nextInt(max - min + 1);
	}

	static long seed(final long worldSeed, final int x, final int z, final long salt) {
		return mix(worldSeed ^ (long) x * 341873128712L ^ (long) z * 132897987541L ^ salt);
	}

	private static double unitHash(final long seed, final int x, final int y, final int z) {
		final long value = mix(seed ^ (long) x * 0x632BE59BD9B4E019L
			^ (long) y * 0x9E3779B97F4A7C15L ^ (long) z * 0x94D049BB133111EBL);
		return (value >>> 11) * 0x1.0p-53;
	}

	private static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}
}
