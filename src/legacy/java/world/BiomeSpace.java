package cr0s.warpdrive.world;

import net.minecraft.world.biome.Biome;

public class BiomeSpace extends Biome {
	
    public BiomeSpace(final BiomeProperties biomeProperties) {
        super(biomeProperties);
        
        this.decorator.treesPerChunk = 0;
        //this.temperature = 1F;
        this.decorator.flowersPerChunk = 0;
        this.decorator.grassPerChunk = 0;
        this.setRegistryName("Space");
    }

    @Override
	public float getSpawningChance() {
        return 0.0F;
    }
	
	@Override
	public boolean getEnableSnow() {
		return false;
	}
}
