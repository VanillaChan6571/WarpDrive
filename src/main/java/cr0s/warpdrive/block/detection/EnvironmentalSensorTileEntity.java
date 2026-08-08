package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.data.DimensionAltitude;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side environmental readings exposed through the optional CC:Tweaked peripheral. */
public class EnvironmentalSensorTileEntity extends TileEntity implements ITickableTileEntity {

	private static final String TAG_ENABLED = "isEnabled";
	private boolean enabled = true;
	private int airConcentration;
	private int updateTicks;

	public EnvironmentalSensorTileEntity() {
		super(Registration.ENVIRONMENTAL_SENSOR_TILE.get());
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || --updateTicks > 0) return;
		updateTicks = 20;
		refreshAtmosphere();
		final BlockState blockState = getBlockState();
		if (blockState.hasProperty(EnvironmentalSensorBlock.ACTIVE)
		 && blockState.getValue(EnvironmentalSensorBlock.ACTIVE) != enabled) {
			level.setBlock(getBlockPos(), blockState.setValue(
				EnvironmentalSensorBlock.ACTIVE, enabled), 3);
		}
	}

	private void refreshAtmosphere() {
		if (level == null) return;
		if (!DimensionAltitude.isSpaceOrHyperspace(level)) {
			airConcentration = 32;
			return;
		}
		final StateAir stateAir = new StateAir(ChunkHandler.getChunkData(
			level, getBlockPos().getX(), getBlockPos().getZ()));
		try {
			stateAir.refresh(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
			airConcentration = stateAir.concentration;
		} catch (final ExceptionChunkNotLoaded exception) {
			airConcentration = 0;
		}
	}

	public Object[] enable(@Nullable final Boolean requested) {
		if (requested != null && enabled != requested) {
			enabled = requested;
			setChanged();
			updateTicks = 0;
		}
		return new Object[]{ enabled };
	}

	private Object[] disabled() { return new Object[]{ false, "Sensor is disabled." }; }

	public Object[] getAtmosphere() {
		return enabled ? new Object[]{ true, airConcentration > 0, airConcentration } : disabled();
	}

	public Object[] getBiome() {
		if (!enabled || level == null) return disabled();
		final Biome biome = level.getBiome(getBlockPos());
		final ResourceLocation registryName = biome.getRegistryName();
		final List<BiomeDictionary.Type> types = new ArrayList<>();
		if (registryName != null) {
			final RegistryKey<Biome> key = RegistryKey.create(Registry.BIOME_REGISTRY, registryName);
			types.addAll(BiomeDictionary.getTypes(key));
		}
		types.sort(Comparator.comparing(BiomeDictionary.Type::getName));
		final Object[] result = new Object[2 + types.size()];
		result[0] = true;
		result[1] = registryName == null ? biome.toString() : registryName.toString();
		for (int index = 0; index < types.size(); index++) {
			result[index + 2] = types.get(index).getName();
		}
		return result;
	}

	public Object[] getHumidity() {
		if (!enabled || level == null) return disabled();
		final float downfall = level.getBiome(getBlockPos()).getDownfall();
		return new Object[]{ true, downfall > 0.85F ? "WET"
			: downfall < 0.15F ? "DRY" : "MEDIUM", downfall };
	}

	public Object[] getTemperature() {
		if (!enabled || level == null) return disabled();
		final Biome biome = level.getBiome(getBlockPos());
		final float baseTemperature = biome.getBaseTemperature();
		final String category = baseTemperature < 0.2F ? "COLD"
			: baseTemperature < 1.0F ? "MEDIUM" : "WARM";
		return new Object[]{ true, category, biome.getTemperature(getBlockPos()) };
	}

	public Object[] getWeather() {
		if (!enabled || level == null) return disabled();
		final boolean snow = level.getBiome(getBlockPos()).getPrecipitation() == Biome.RainType.SNOW;
		final IServerWorldInfo worldInfo = level instanceof ServerWorld
			? ((ServerWorld) level).getServer().getWorldData().overworldData() : null;
		if (level.isThundering()) {
			return new Object[]{ true, snow ? "BLIZZARD" : "THUNDER",
				worldInfo == null ? 0 : worldInfo.getThunderTime() / 20 };
		}
		if (level.isRaining()) {
			return new Object[]{ true, snow ? "SNOW" : "RAIN",
				worldInfo == null ? 0 : worldInfo.getRainTime() / 20 };
		}
		return new Object[]{ true, "CLEAR",
			worldInfo == null ? 0 : worldInfo.getClearWeatherTime() / 20 };
	}

	public Object[] getWorldTime() {
		if (!enabled || level == null) return disabled();
		final long shifted = 6000L + level.getDayTime();
		final int day = (int) (shifted / 24000L);
		final int dayTime = (int) (2400L * Math.floorMod(shifted, 24000L) / 24000L);
		return new Object[]{ true, day, dayTime / 100, (dayTime % 100) * 60 / 100,
			level.getGameTime() / 20L };
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		enabled = !tagCompound.contains(TAG_ENABLED) || tagCompound.getBoolean(TAG_ENABLED);
		updateTicks = 0;
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putBoolean(TAG_ENABLED, enabled);
		return tagCompound;
	}
}
