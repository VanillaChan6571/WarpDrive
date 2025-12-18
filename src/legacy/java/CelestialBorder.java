package cr0s.warpdrive.data;

import javax.annotation.Nonnull;

import net.minecraft.world.border.EnumBorderStatus;
import net.minecraft.world.border.WorldBorder;


/**
 * An overloaded world border to allow rectangular shapes.
 *
 * @author LemADEC
 */
public class CelestialBorder extends WorldBorder {
	
	public int centerX, centerZ;
	public int radiusX, radiusZ;
	
	public CelestialBorder(final int centerX, final int centerZ,
	                       final int radiusX, final int radiusZ) {
		this.centerX = centerX;
		this.centerZ = centerZ;
		this.radiusX = radiusX;
		this.radiusZ = radiusZ;
	}
	
	@Nonnull
	@Override
	public EnumBorderStatus getStatus() {
		return EnumBorderStatus.STATIONARY;
	}
	
	@Override
	public double minX() {
		return centerX - radiusX;
	}
	
	@Override
	public double minZ() {
		return centerZ - radiusZ;
	}
	
	@Override
	public double maxX() {
		return centerX + radiusX;
	}
	
	@Override
	public double maxZ() {
		return centerZ + radiusZ;
	}
	
	@Override
	public double getCenterX()
	{
		return centerX;
	}
	
	@Override
	public double getCenterZ()
	{
		return centerZ;
	}
	
	@Override
	public void setCenter(final double x, final double z) {
		assert false;
	}
	
	@Override
	public double getDiameter() {
		assert false;
		return super.getDiameter();
	}
	
	@Override
	public long getTimeUntilTarget() {
		assert false;
		return 0L;
	}
	
	@Override
	public double getTargetSize() {
		return 2 * Math.max(radiusX, radiusZ);
	}
	
	@Override
	public void setTransition(final double newSize) {
		super.setTransition(newSize);
	}
	
	@Override
	public void setTransition(final double oldSize, final double newSize, final long time) {
		super.setTransition(oldSize, newSize, time);
	}
	
	@Override
	public void setSize(final int size) {
		// no operation
	}
	
	@Override
	public int getSize() {
		return 2 * Math.max(radiusX, radiusZ);
	}
	
	@Override
	public String toString() {
		return String.format("CelestialBorder [@ %d %d Border(%d %d)]",
		                     centerX, centerZ,
		                     2 * radiusX, 2 * radiusZ );
	}
}