package cr0s.warpdrive.api;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import java.util.Locale;

import net.minecraft.item.EnumRarity;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Particle {
	
	protected final String registryName;
	
	protected int color;
	protected int colorIndex;
	
	protected IRarity rarity = EnumRarity.COMMON;
	private int entityLifespan;
	private float radiationLevel = 0.0F;
	private float explosionStrength = 0.0F;
	
	public Particle(final String registryName) {
		this.registryName = registryName.toLowerCase(Locale.ENGLISH);
	}
	
	public Particle setRarity(@Nonnull final IRarity rarity) {
		this.rarity = rarity;
		return this;
	}
	
	public Particle setColorIndex(final int colorIndex) {
		this.colorIndex = Commons.clamp(0, 4, colorIndex);
		return this;
	}
	
	public Particle setColor(final int colorIndex) {
		this.color = Commons.clamp(0x000000, 0xFFFFFF, colorIndex);
		return this;
	}
	
	public Particle setColor(final int red, final int green, final int blue) {
		this.color = (Commons.clamp(0, 255, red) << 16) + (Commons.clamp(0, 255,  green) << 8) + Commons.clamp(0, 255, blue);
		return this;
	}
	
	public Particle setEntityLifespan(final int entityLifespan) {
		this.entityLifespan = entityLifespan;
		return this;
	}
	
	public Particle setRadiationLevel(final float radiationLevel) {
		this.radiationLevel = radiationLevel;
		return this;
	}
	
	public Particle setExplosionStrength(final float explosionStrength) {
		this.explosionStrength = explosionStrength;
		return this;
	}
	
	public final String getRegistryName()
	{
		return this.registryName;
	}
	
	@SideOnly(Side.CLIENT)
	public String getLocalizedName() {
		final String unlocalizedName = getTranslationKey();
		return unlocalizedName == null ? "" : new TextComponentTranslation(unlocalizedName + ".name").getFormattedText();
	}
	
	@SideOnly(Side.CLIENT)
	public String getLocalizedTooltip() {
		final String unlocalizedName = getTranslationKey();
		return unlocalizedName == null ? "" : new TextComponentTranslation(unlocalizedName + ".tooltip").getFormattedText();
	}
	
	public String getTranslationKey()
	{
		return "warpdrive.particle." + this.registryName;
	}
	
	/* Default Accessors */
	
	@Nonnull
	public IRarity getForgeRarity() {
		return rarity;
	}
	
	public int getColorIndex() {
		return colorIndex;
	}
	
	public int getColor() {
		return color;
	}
	
	public int getEntityLifespan() {
		return entityLifespan;
	}
	
	/* Effector */
	
	public void onWorldEffect(final World world, final Vector3 v3Position, final int amount) {
		if (world.isRemote) {
			return;
		}
		if (radiationLevel > 0.0F) {
			final float strength = radiationLevel * amount / 1000.0F;
			WarpDrive.damageIrradiation.onWorldEffect(world, v3Position, strength);
		}
		if (explosionStrength > 0.0F) {
			final float amountFactor = Math.max(1.25F, amount / 1000.0F);
			world.newExplosion(null, v3Position.x, v3Position.y, v3Position.z, explosionStrength * amountFactor, true, true);
			WarpDrive.logger.info(String.format("Explosion %s with strength %.3f due to %d mg of %s",
			                                    Commons.format(world, v3Position),
			                                    explosionStrength * amountFactor,
			                                    amount,
			                                    this));
		}
	}
	
	@Override
	public String toString() {
		return String.format("Particle %s (%s, RGB 0x%x %d, %d ticks, %.3f Sv, strength %.3f)",
		                     registryName,
		                     rarity,
		                     color, colorIndex,
		                     entityLifespan,
		                     radiationLevel,
		                     explosionStrength);
	}
}

