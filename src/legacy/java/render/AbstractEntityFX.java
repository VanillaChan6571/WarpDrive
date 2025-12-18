package cr0s.warpdrive.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class AbstractEntityFX extends Particle {
	
	// particles are no longer entities on 1.10+, so we can't use the entityId as a seed
	private static int nextSeed;
	private final int seed = nextSeed++;
	
	public AbstractEntityFX(final World world, final double x, final double y, final double z,
	                        final double xSpeed, final double ySpeed, final double zSpeed) {
		super(world, x, y, z, xSpeed, ySpeed, zSpeed);
	}
	
	// extend current life
	public void refresh() {
		particleMaxAge = Math.max(particleMaxAge, particleAge + 20);
	}
	
	// get seed
	protected int getSeed() { return seed; }
	
	// return private properties
	public World getWorld() {
		return world;
	}
	
	public double getX() {
		return posX;
	}
	
	public double getY() {
		return posY;
	}
	
	public double getZ() {
		return posZ;
	}
}