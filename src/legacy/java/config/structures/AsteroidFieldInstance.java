package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumStructureGroup;
import cr0s.warpdrive.world.WorldGenSmallShip;
import cr0s.warpdrive.world.WorldGenStation;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AsteroidFieldInstance extends AbstractStructureInstance {
	
	public AsteroidFieldInstance(final AsteroidField asteroidField, final Random random) {
		super(asteroidField, random);
	}
	
	public AsteroidFieldInstance(final NBTTagCompound tagCompound) {
		super(tagCompound);
		// TODO not implemented
	}
	
	@Override
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		// TODO not implemented
		return tagCompound;
	}
	
	private static float binomialRandom(final World world) {
		final float linear = world.rand.nextFloat();
		// ideal sphere repartition = x ^ 0.5 (sqrt)
		// Dilution but slow to compute = 0.5 * ( x ^ 0.3 + 1 + (x - 1) ^ 3 )
		// Optimized 'pushed out' form = 1.25 - 0.625 / (0.5 + 2 * x)
		// Natural sphere with ring = (1 - x ^ 2.5) * x ^ 0.5 + x ^ 4
		
		// rectangular approach: return 0.5F * linear + 0.5F * linear * linear;
		return 1.25F - 0.625F / (0.5F + 2.0F * linear);
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		LocalProfiler.start("AsteroidFieldInstance.generate");
		// 6.0.1 au = 120 radius with 60 to 140 big + 60 to 140 small + 5 to 13 gas
		// 45238 blocks surface with 120 to 280 asteroids => 161 to 376 blocks per asteroid (big & small)
		
		// 6.0.2 av big = 80 to 180 radius with 40 to 90 big + 80 to 200 small + 5 to 13 gas
		// 20106 to 101787 surface with 120 to 290 asteroids => 69 to 848 blocks per asteroid
		
		// 6.0.2 av small = 30 to 80 radius with 2 to 22 big + 15 to 75 small + 0 to 3 gas
		// 2827 to 20106 surface with 17 to 97 asteroids => 29 to 1182 blocks per asteroid
		
		// random distanced one = 89727 surface 256 asteroids => 350 blocks per asteroid

		/*
		boolean isBig = world.rand.nextInt(3) == 1;
		int numOfBigAsteroids, numOfSmallAsteroids, numOfClouds, maxDistance, maxHeight;
		if (isBig) {
			numOfBigAsteroids = 40 + world.rand.nextInt(50);
			numOfSmallAsteroids = 80 + world.rand.nextInt(120);
			numOfClouds = 5 + world.rand.nextInt(8);
			maxDistance = 80 + world.rand.nextInt(100);
			maxHeight = 40 + world.rand.nextInt(40);
		} else {
			numOfBigAsteroids = 2 + world.rand.nextInt(20);
			numOfSmallAsteroids = 15 + world.rand.nextInt(60);
			numOfClouds = 0 + world.rand.nextInt(3);
			maxDistance = 30 + world.rand.nextInt(50);
			maxHeight = 30 + world.rand.nextInt(30);
		}
		/**/
		
		final float surfacePerAsteroid = 80.0F + world.rand.nextFloat() * 300;
		final int maxDistance = 30 + world.rand.nextInt(170);
		final int maxDistanceBig = Math.round(maxDistance * (0.6F + 0.2F * world.rand.nextFloat()));
		final int maxDistanceSmall = Math.round(maxDistance * 1.1F);
		final float bigRatio = 0.3F + world.rand.nextFloat() * 0.3F;
		final float surfaceBig = (float) (Math.PI * Math.pow(maxDistanceBig, 2));
		final float surfaceSmall = (float) (Math.PI * Math.pow(maxDistanceSmall, 2));
		final int numOfBigAsteroids = Math.round(bigRatio * surfaceBig / surfacePerAsteroid);
		final int numOfSmallAsteroids = Math.round((1.0F - bigRatio) * surfaceSmall / surfacePerAsteroid);
		final int numOfClouds = Math.round(numOfBigAsteroids * 1.0F / (10.0F + world.rand.nextInt(10)));
		final int maxHeight = 70 + world.rand.nextInt(50);
		final int y2 = Math.min(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - maxHeight,
		                  Math.max(blockPos.getY(), WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + maxHeight));
		WarpDrive.logger.info(String.format("Generating asteroid field %s qty %d, %d, %d over %d, %d surfacePerAsteroid %.1f",
		                                    Commons.format(world, blockPos.getX(), y2, blockPos.getZ()),
		                                    numOfBigAsteroids, numOfSmallAsteroids, numOfClouds,
		                                    maxDistance, maxHeight, surfacePerAsteroid));
		
		// Setting up of big asteroids
		for (int i = 1; i <= numOfBigAsteroids; i++) {
			final float binomial = binomialRandom(world);
			final double bearing = world.rand.nextFloat() * 2.0D * Math.PI;
			final double yawn = world.rand.nextFloat() * Math.PI;
			final float horizontalRange = Math.max(6.0F, binomial * maxDistanceBig);
			final float verticalRange = Math.max(3.0F, binomial * maxHeight);
			
			final int aX = (int) (blockPos.getX() + Math.round(horizontalRange * Math.cos(bearing)));
			final int aY = (int) (y2 + Math.round(verticalRange * Math.cos(yawn)));
			final int aZ = (int) (blockPos.getZ() + Math.round(horizontalRange * Math.sin(bearing)));
			
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.info(String.format("Big asteroid: %.3f %.3f r %.3f r makes (%3d %3d %3d)",
					(double) binomial, bearing, yawn, aX, aY, aZ));
			}
			
			// Place an asteroid
			final AbstractStructure asteroid = StructureManager.getStructure(world.rand, EnumStructureGroup.ASTEROIDS.getName(), null);
			if (asteroid == null) {
				WarpDrive.logger.error(String.format("Unable to generate big asteroid %s, probably a bad configuration",
				                                     Commons.format(world, aX, aY, aZ)));
			} else {
				asteroid.generate(world, world.rand, new BlockPos(aX, aY, aZ));
			}
		}
		
		// Setting up small asteroids
		for (int i = 1; i <= numOfSmallAsteroids; i++) {
			final float binomial = binomialRandom(world);
			final double bearing = world.rand.nextFloat() * 2.0D * Math.PI;
			final double yawn = world.rand.nextFloat() * Math.PI;
			final float horizontalRange = Math.max(6.0F, binomial * maxDistanceSmall);
			final float verticalRange = Math.max(3.0F, binomial * maxHeight);
			
			final int aX = (int) (blockPos.getX() + Math.round(horizontalRange * Math.cos(bearing)));
			final int aY = (int) (y2 + Math.round(verticalRange * Math.cos(yawn)));
			final int aZ = (int) (blockPos.getZ() + Math.round(horizontalRange * Math.sin(bearing)));
			
			// Placing
			if (world.rand.nextInt(400) != 1) {
				final AbstractStructure asteroid = StructureManager.getStructure(world.rand, EnumStructureGroup.ASTEROIDS.getName(), null);
				if (asteroid == null) {
					WarpDrive.logger.error(String.format("Unable to generate small asteroid %s, probably a bad configuration",
					                                     Commons.format(world, aX, aY, aZ)));
				} else {
					asteroid.generate(world, world.rand, new BlockPos(aX, aY, aZ));
				}
			} else {
				if (world.rand.nextInt(20) != 1) {
					generateSmallShip(world, aX, aY, aZ, 8);
				} else {
					generateStation(world, aX, aY, aZ, 8);
				}
			}
		}
		
		// Setting up gas clouds
		for (int i = 1; i <= numOfClouds; i++) {
			final float binomial = binomialRandom(world);
			final double bearing = world.rand.nextFloat() * 2.0D * Math.PI;
			final double yawn = world.rand.nextFloat() * Math.PI;
			final float horizontalRange = Math.max(6.0F, binomial * maxDistanceBig);
			final float verticalRange = Math.max(3.0F, binomial * maxHeight);
			
			final int aX = (int) (blockPos.getX() + Math.round(horizontalRange * Math.cos(bearing)));
			final int aY = (int) (y2 + Math.round(verticalRange * Math.cos(yawn)));
			final int aZ = (int) (blockPos.getZ() + Math.round(horizontalRange * Math.sin(bearing)));
			
			// Placing
			if (world.rand.nextBoolean()) {
				final AbstractStructure gasCloud = StructureManager.getStructure(world.rand, EnumStructureGroup.GAS_CLOUDS.getName(), null);
				if (gasCloud == null) {
					WarpDrive.logger.error(String.format("Unable to generate gas cloud %s, probably a bad configuration",
					                                     Commons.format(world, aX, aY, aZ)));
				} else {
					gasCloud.generate(world, world.rand, new BlockPos(aX, aY, aZ));
				}
			}
		}
		
		LocalProfiler.stop();
		return true;
	}
	
	private static void generateSmallShip(final World world, final int x, final int y, final int z, final int jitter) {
		final int x2 = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		final int y2 = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		final int z2 = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		WarpDrive.logger.info(String.format("Generating small ship %s",
		                                    Commons.format(world, x2, y2, z2)));
		new WorldGenSmallShip(world.rand.nextFloat() > 0.2F, false).generate(world, world.rand, new BlockPos(x2, y2, z2));
	}
	
	private static void generateStation(final World world, final int x, final int y, final int z, final int jitter) {
		final int x2 = x + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		final int y2 = y + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		final int z2 = z + (((world.rand.nextBoolean()) ? -1 : 1) * world.rand.nextInt(jitter));
		WarpDrive.logger.info(String.format("Generating station %s",
		                                    Commons.format(world, x2, y2, z2)));
		new WorldGenStation(world.rand.nextBoolean()).generate(world, world.rand, new BlockPos(x2, y2, z2));
	}
}
