package cr0s.warpdrive.world;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class HyperSpaceWorldProvider extends AbstractWorldProvider {
	
	public HyperSpaceWorldProvider() {
		super();
		
		biomeProvider  = new BiomeProviderSingle(WarpDrive.biomeSpace);
		nether = true;
	}
	
	@Override
	protected void init() {
		super.init();
		
		world.setSeaLevel(0);
	}
	
	@Nonnull
	@Override
	public DimensionType getDimensionType() {
		return WarpDrive.dimensionTypeHyperSpace;
	}
	
	@Override
	public boolean canRespawnHere() {
		return true;
	}
	
	@Override
	public boolean isSurfaceWorld() {
		return true;
	}
	
	@Override
	public int getAverageGroundLevel() {
		return 1;
	}
	
	@Override
	public double getHorizon() {
		return -256;
	}
	
	@Override
	public void updateWeather() {
		super.resetRainAndThunder();
	}
	
	@Nonnull
	@Override
	public Biome getBiomeForCoords(@Nonnull final BlockPos blockPos) {
		return WarpDrive.biomeSpace;
	}
	
	@Override
	public void setAllowedSpawnTypes(final boolean allowHostile, final boolean allowPeaceful) {
		super.setAllowedSpawnTypes(true, true);
	}
	
	@Override
	public float calculateCelestialAngle(final long time, final float partialTick) {
		// returns the clock angle: 0 is noon, 0.5 is midnight on the vanilla clock
		// daylight is required to enable IC2, NuclearCraft and EnderIO solar panels
		// we want no solar power in hyperspace => permanent midnight
		return 0.5F;
	}
	
	@Override
	protected void generateLightBrightnessTable() {
		final float ambient = 0.1F;
		
		for (int i = 0; i <= 15; ++i) {
			final float f1 = 1.0F - i / 15.0F;
			lightBrightnessTable[i] = (1.0F - f1) / (f1 * 3.0F + 1.0F) * (1.0F - ambient) + ambient;
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean isSkyColored() {
		return true;
	}
	
	@Override
	public int getRespawnDimension(@Nonnull final EntityPlayerMP entityPlayerMP) {
		if (entityPlayerMP.world == null) {
			WarpDrive.logger.error(String.format("Invalid player passed to getRespawnDimension: %s", entityPlayerMP));
			return 0;
		}
		return CelestialObjectManager.getHyperspaceDimensionId(entityPlayerMP.world, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
	}
	
	@Nonnull
	@Override
	public IChunkGenerator createChunkGenerator() {
		return new HyperSpaceChunkProvider(world, 46);
	}
	
	@Override
	public boolean canBlockFreeze(@Nonnull final BlockPos blockPos, final boolean byWater) {
		return true;
	}
	
	@Override
	public boolean isDaytime() {
		// true is required to enable GregTech solar boiler and Mekanism solar panels
		// we want no solar power in hyperspace => false
		return false;
	}
	
	@Override
	public boolean canDoLightning(@Nonnull final Chunk chunk) {
		return false;
	}
	
	@Override
	public boolean canDoRainSnowIce(@Nonnull final Chunk chunk) {
		return false;
	}
}