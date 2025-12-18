package cr0s.warpdrive.world;

import javax.annotation.Nonnull;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;

public class FakeWorldProvider extends WorldProvider {
	
	@Nonnull
	@Override
	public DimensionType getDimensionType() {
		return DimensionType.OVERWORLD;
	}
	
	@Nonnull
	@Override
	public Biome getBiomeForCoords(@Nonnull final BlockPos blockPos) {
		return Biomes.PLAINS;
	}
}
