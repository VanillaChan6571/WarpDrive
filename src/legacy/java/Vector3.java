package cr0s.warpdrive.data;

import cr0s.warpdrive.render.AbstractEntityFX;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.EnumFacing;

/**
 * Vector3 Class is used for defining objects in a 3D space.
 *
 * @author Calclavia
 */

@SuppressWarnings("WeakerAccess")
public class Vector3 implements Cloneable {
	
	public double x;
	public double y;
	public double z;
	
	public Vector3() {
		this(0, 0, 0);
	}
	
	public Vector3(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3(final Entity entity) {
		x = entity.posX;
		y = entity.posY;
		z = entity.posZ;
	}
	
	public Vector3(final AbstractEntityFX entityFX) {
		x = entityFX.getX();
		y = entityFX.getY();
		z = entityFX.getZ();
	}
	
	public Vector3(final TileEntity tileEntity) {
		x = tileEntity.getPos().getX();
		y = tileEntity.getPos().getY();
		z = tileEntity.getPos().getZ();
	}
	
	public Vector3(final Vec3d vec3) {
		x = vec3.x;
		y = vec3.y;
		z = vec3.z;
	}
	
	public Vector3(final RayTraceResult rayTraceResult) {
		x = rayTraceResult.getBlockPos().getX();
		y = rayTraceResult.getBlockPos().getY();
		z = rayTraceResult.getBlockPos().getZ();
	}
	
	public Vector3(final BlockPos blockPos) {
		x = blockPos.getX();
		y = blockPos.getY();
		z = blockPos.getZ();
	}
	
	public Vector3(final EnumFacing direction) {
		x = direction.getXOffset();
		y = direction.getYOffset();
		z = direction.getZOffset();
	}
	
	/**
	 * Returns the coordinates as integers, ideal for block placement.
	 */
	public int intX() {
		return (int) Math.floor(x);
	}
	
	public int intY() {
		return (int) Math.floor(y);
	}
	
	public int intZ() {
		return (int) Math.floor(z);
	}
	
	/**
	 * Makes a new copy of this Vector. Prevents variable referencing problems.
	 */
	@SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
	public Vector3 clone() {
		return new Vector3(x, y, z);
	}
	
	/**
	 * Converts this vector three into a Minecraft Vec3d object
	 */
	public Vec3d toVec3d() {
		return new Vec3d(x, y, z);
	}
	
	public double getMagnitude() {
		return Math.sqrt(getMagnitudeSquared());
	}
	
	public double getMagnitudeSquared() {
		return x * x + y * y + z * z;
	}
	
	public Vector3 normalize() {
		final double d = getMagnitude();
		
		if (d != 0) {
			scale(1 / d);
		}
		
		return this;
	}
	
	/**
	 * Gets the distance between two vectors
	 *
	 * @return The distance
	 */
	public static double distance(final Vector3 v1, final Vector3 v2) {
		final double dX = v1.x - v2.x;
		final double dY = v1.y - v2.y;
		final double dZ = v1.z - v2.z;
		return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
	}
	
	public double distanceTo(final Vector3 vector3) {
		final double dX = vector3.x - x;
		final double dY = vector3.y - y;
		final double dZ = vector3.z - z;
		return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
	}
	
	public double distanceTo_square(final Vector3 vector3) {
		final double dX = vector3.x - x;
		final double dY = vector3.y - y;
		final double dZ = vector3.z - z;
		return dX * dX + dY * dY + dZ * dZ;
	}
	
	public double distanceTo_square(final Entity entity) {
		final double dX = entity.posX - x;
		final double dY = entity.posY - y;
		final double dZ = entity.posZ - z;
		return dX * dX + dY * dY + dZ * dZ;
	}
	
	public double distanceTo_square(final AbstractEntityFX entityFX) {
		double var2 = entityFX.getX() - x;
		double var4 = entityFX.getY() - y;
		double var6 = entityFX.getZ() - z;
		return var2 * var2 + var4 * var4 + var6 * var6;
	}
	
	/**
	 * Multiplies the vector by negative one.
	 */
	public Vector3 invert() {
		scale(-1);
		return this;
	}
	
	public Vector3 translate(final Vec3d vector3) {
		x += vector3.x;
		y += vector3.y;
		z += vector3.z;
		return this;
	}
	
	public Vector3 translate(final Vector3 vector3) {
		x += vector3.x;
		y += vector3.y;
		z += vector3.z;
		return this;
	}
	
	public Vector3 translate(final double amount) {
		x += amount;
		y += amount;
		z += amount;
		return this;
	}
	
	// modify current vector by translation of amount block in side direction
	public Vector3 translate(final EnumFacing side, final double amount) {
		switch (side) {
		case DOWN:
			y -= amount;
			break;
		case UP:
			y += amount;
			break;
		case NORTH:
			z -= amount;
			break;
		case SOUTH:
			z += amount;
			break;
		case WEST:
			x -= amount;
			break;
		case EAST:
			x += amount;
			break;
		default:
			break;
		}
		
		return this;
	}
	
	// modify current vector by translation of 1 block in side direction
	public Vector3 translate(final EnumFacing side) {
		x += side.getXOffset();
		y += side.getYOffset();
		z += side.getZOffset();
		return this;
	}
	
	public static Vector3 translate(final Vector3 translate, final Vector3 offset) {
		translate.x += offset.x;
		translate.y += offset.y;
		translate.z += offset.z;
		return translate;
	}
	
	public Vector3 translateFactor(final Vector3 direction, final double factor) {
		this.x += direction.x * factor;
		this.y += direction.y * factor;
		this.z += direction.z * factor;
		return this;
	}
	
	public Vector3 subtract(final Vector3 amount) {
		return translate(amount.clone().invert());
	}
	
	public Vector3 scale(final double amount) {
		x *= amount;
		y *= amount;
		z *= amount;
		return this;
	}
	
	public Vector3 scale(final Vector3 amount) {
		x *= amount.x;
		y *= amount.y;
		z *= amount.z;
		return this;
	}
	
	public static Vector3 scale(final Vector3 vector3, final double amount) {
		return vector3.scale(amount);
	}
	
	public static Vector3 scale(final Vector3 vector3, final Vector3 amount) {
		return vector3.scale(amount);
	}
	
	/**
	 * Static versions of a lot of functions
	 */
	public Vector3 round() {
		return new Vector3(Math.round(x), Math.round(y), Math.round(z));
	}
	
	public Vector3 ceil() {
		return new Vector3(Math.ceil(x), Math.ceil(y), Math.ceil(z));
	}
	
	public Vector3 floor() {
		return new Vector3(Math.floor(x), Math.floor(y), Math.floor(z));
	}
	
	public Vector3 toRound() {
		x = Math.round(x);
		y = Math.round(y);
		z = Math.round(z);
		return this;
	}
	
	public Vector3 toCeil() {
		x = Math.ceil(x);
		y = Math.ceil(y);
		z = Math.ceil(z);
		return this;
	}
	
	public Vector3 toFloor() {
		x = Math.floor(x);
		y = Math.floor(y);
		z = Math.floor(z);
		return this;
	}
	
	public BlockPos getBlockPos() {
		return new BlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
	}
	
	/**
	 * Gets all entities inside of this position in block space.
	 */
	public List<Entity> getEntitiesWithin(final World world, final Class<? extends Entity> clazz) {
		return world.getEntitiesWithinAABB(clazz,
				new AxisAlignedBB(intX(), intY(), intZ(), intX() + 1, intY() + 1, intZ() + 1));
	}
	
	/**
	 * Gets a position relative to a position's side
	 */
	public Vector3 modifyPositionFromSide(final EnumFacing side, final double amount) {
		switch (side.ordinal()) {
		case 0:
			y -= amount;
			break;
		
		case 1:
			y += amount;
			break;
		
		case 2:
			z -= amount;
			break;
		
		case 3:
			z += amount;
			break;
		
		case 4:
			x -= amount;
			break;
		
		case 5:
			x += amount;
			break;
		
		default:
			break;
		}
		
		return this;
	}
	
	public Vector3 modifyPositionFromSide(final EnumFacing side) {
		modifyPositionFromSide(side, 1);
		return this;
	}
	
	/**
	 * Cross product functions
	 *
	 * @return The cross product between this vector and another.
	 */
	public Vector3 crossProduct(final Vector3 vector3) {
		return new Vector3(y * vector3.z - z * vector3.y, z * vector3.x - x * vector3.z, x * vector3.y - y * vector3.x);
	}
	
	public Vector3 xCrossProduct() {
		return new Vector3(0.0D, z, -y);
	}
	
	public Vector3 zCrossProduct() {
		return new Vector3(-y, x, 0.0D);
	}
	
	public double dotProduct(final Vector3 vector3) {
		return x * vector3.x + y * vector3.y + z * vector3.z;
	}
	
	/**
	 * @return The perpendicular vector.
	 */
	public Vector3 getPerpendicular() {
		if (z == 0.0F) {
			return zCrossProduct();
		}
		
		return xCrossProduct();
	}
	
	/**
	 * @return True if this Vector3 is zero.
	 */
	public boolean isZero() {
		return (x == 0.0F) && (y == 0.0F) && (z == 0.0F);
	}
	
	/**
	 * Rotate by a this vector around an axis.
	 *
	 * @return The new Vector3 rotation.
	 */
	public Vector3 rotate(final float angle, final Vector3 axis) {
		return translateMatrix(getRotationMatrix(angle, axis), clone());
	}
	
	public double[] getRotationMatrix(final float angle_deg) {
		final double[] matrix = new double[16];
		final Vector3 axis = clone().normalize();
		final double xn = axis.x;
		final double yn = axis.y;
		final double zn = axis.z;
		final float angle_rad = angle_deg * 0.0174532925F;
		final float cos = (float) Math.cos(angle_rad);
		final float oCos = 1.0F - cos;
		final float sin = (float) Math.sin(angle_rad);
		matrix[0] = (xn * xn * oCos + cos);
		matrix[1] = (yn * xn * oCos + zn * sin);
		matrix[2] = (xn * zn * oCos - yn * sin);
		matrix[4] = (xn * yn * oCos - zn * sin);
		matrix[5] = (yn * yn * oCos + cos);
		matrix[6] = (yn * zn * oCos + xn * sin);
		matrix[8] = (xn * zn * oCos + yn * sin);
		matrix[9] = (yn * zn * oCos - xn * sin);
		matrix[10] = (zn * zn * oCos + cos);
		matrix[15] = 1.0F;
		return matrix;
	}
	
	public static Vector3 translateMatrix(final double[] matrix, final Vector3 translation) {
		final double x = translation.x * matrix[0] + translation.y * matrix[1] + translation.z * matrix[2] + matrix[3];
		final double y = translation.x * matrix[4] + translation.y * matrix[5] + translation.z * matrix[6] + matrix[7];
		final double z = translation.x * matrix[8] + translation.y * matrix[9] + translation.z * matrix[10] + matrix[11];
		translation.x = x;
		translation.y = y;
		translation.z = z;
		return translation;
	}
	
	public static double[] getRotationMatrix(final float angle, final Vector3 axis) {
		return axis.getRotationMatrix(angle);
	}
	
	/**
	 * Rotates this Vector by a yaw, pitch and roll values.
	 */
	public void rotate(final double yaw, final double pitch, final double roll) {
		final double yawRadians = Math.toRadians(yaw);
		final double pitchRadians = Math.toRadians(pitch);
		final double rollRadians = Math.toRadians(roll);
		final double oldX = x;
		final double oldY = y;
		final double oldZ = z;
		x = oldX * Math.cos(yawRadians) * Math.cos(pitchRadians) + oldZ
				* (Math.cos(yawRadians) * Math.sin(pitchRadians) * Math.sin(rollRadians) - Math.sin(yawRadians) * Math.cos(rollRadians)) + oldY
				* (Math.cos(yawRadians) * Math.sin(pitchRadians) * Math.cos(rollRadians) + Math.sin(yawRadians) * Math.sin(rollRadians));
		z = oldX * Math.sin(yawRadians) * Math.cos(pitchRadians) + oldZ
				* (Math.sin(yawRadians) * Math.sin(pitchRadians) * Math.sin(rollRadians) + Math.cos(yawRadians) * Math.cos(rollRadians)) + oldY
				* (Math.sin(yawRadians) * Math.sin(pitchRadians) * Math.cos(rollRadians) - Math.cos(yawRadians) * Math.sin(rollRadians));
		y = -oldX * Math.sin(pitchRadians) + oldZ * Math.cos(pitchRadians) * Math.sin(rollRadians) + oldY * Math.cos(pitchRadians) * Math.cos(rollRadians);
	}
	
	/**
	 * Rotates a point by a yaw and pitch around the anchor 0,0 by a specific
	 * angle.
	 */
	public void rotate(final double yaw, final double pitch) {
		rotate(yaw, pitch, 0);
	}
	
	public void rotate(final double yaw) {
		final double yawRadians = Math.toRadians(yaw);
		final double oldX = x;
		final double oldZ = z;
		
		if (yaw != 0) {
			x = oldX * Math.cos(yawRadians) - oldZ * Math.sin(yawRadians);
			z = oldX * Math.sin(yawRadians) + oldZ * Math.cos(yawRadians);
		}
	}
	
	/**
	 * Gets the delta look position based on the rotation yaw and pitch.
	 * Minecraft coordinates are messed up. Y and Z are flipped. Yaw is
	 * displaced by 90 degrees. Pitch is inverted.
	 */
	public static Vector3 getDeltaPositionFromRotation(final float rotationYaw1, final float rotationPitch1) {
		final float rotationYaw2 = rotationYaw1 + 90;
		final float rotationPitch2 = -rotationPitch1;
		return new Vector3(Math.cos(Math.toRadians(rotationYaw2)), Math.sin(Math.toRadians(rotationPitch2)), Math.sin(Math.toRadians(rotationYaw2)));
	}
	
	/**
	 * Gets the angle between this vector and another vector.
	 *
	 * @return Angle in degrees
	 */
	public double getAngle(final Vector3 vector3) {
		return anglePreNorm(clone().normalize(), vector3.clone().normalize());
	}
	
	public static double getAngle(final Vector3 v1, final Vector3 v2) {
		return v1.getAngle(v2);
	}
	
	public double anglePreNorm(final Vector3 vector3) {
		return Math.acos(dotProduct(vector3));
	}
	
	public static double anglePreNorm(final Vector3 v1, final Vector3 v2) {
		return Math.acos(v1.clone().dotProduct(v2));
	}
	
	
	public static Vector3 createFromNBT(final NBTTagCompound tagCompound) {
		final Vector3 vector = new Vector3();
		vector.readFromNBT(tagCompound);
		return vector;
	}
	
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		x = tagCompound.getDouble("x");
		y = tagCompound.getDouble("y");
		z = tagCompound.getDouble("z");
	}
	
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setDouble("x", x);
		tagCompound.setDouble("y", y);
		tagCompound.setDouble("z", z);
		return tagCompound;
	}
	
	public static Vector3 UP() {
		return new Vector3(0, 1, 0);
	}
	
	public static Vector3 DOWN() {
		return new Vector3(0, -1, 0);
	}
	
	public static Vector3 NORTH() {
		return new Vector3(0, 0, -1);
	}
	
	public static Vector3 SOUTH() {
		return new Vector3(0, 0, 1);
	}
	
	public static Vector3 WEST() {
		return new Vector3(-1, 0, 0);
	}
	
	public static Vector3 EAST() {
		return new Vector3(1, 0, 0);
	}
	
	/**
	 * RayTrace Code, retrieved from MachineMuse.
	 *
	 * @author MachineMuse
	 */
	public RayTraceResult rayTrace(final World world, final float rotationYaw, final float rotationPitch, final boolean collisionFlag, final double reachDistance) {
		// Somehow this destroys the playerPosition vector -.-
		final RayTraceResult pickedBlock = rayTraceBlocks(world, rotationYaw, rotationPitch, reachDistance);
	final 	RayTraceResult pickedEntity = rayTraceEntities(world, rotationYaw, rotationPitch, reachDistance);
		
		if (pickedBlock == null) {
			return pickedEntity;
		} else if (pickedEntity == null) {
			return pickedBlock;
		} else {
			final double dBlock = distanceTo(new Vector3(pickedBlock.hitVec));
			final double dEntity = distanceTo(new Vector3(pickedEntity.hitVec));
			
			if (dEntity < dBlock) {
				return pickedEntity;
			} else {
				return pickedBlock;
			}
		}
	}
	
	public RayTraceResult rayTraceBlocks(final World world, final float rotationYaw, final float rotationPitch, final double reachDistance) {
		final Vector3 lookVector = getDeltaPositionFromRotation(rotationYaw, rotationPitch);
		final Vector3 reachPoint = this.clone().translateFactor(lookVector, reachDistance);
		return world.rayTraceBlocks(toVec3d(), reachPoint.toVec3d());// TODO: Removed collision flag
	}
	
	public RayTraceResult rayTraceEntities(final World world, final float rotationYaw, final float rotationPitch, final double reachDistance) {
		RayTraceResult pickedEntity = null;
		final Vec3d startingPosition = toVec3d();
		final Vec3d look = getDeltaPositionFromRotation(rotationYaw, rotationPitch).toVec3d();
  	    final Vec3d reachPoint = new Vec3d(
  	    		startingPosition.x + look.x * reachDistance,
			    startingPosition.y + look.y * reachDistance,
			    startingPosition.z + look.z * reachDistance);
		final double playerBorder = 1.1 * reachDistance;
		final AxisAlignedBB boxToScan = new AxisAlignedBB(-playerBorder, -playerBorder, -playerBorder, playerBorder, playerBorder, playerBorder);
		final List<Entity> entitiesHit = world.getEntitiesWithinAABBExcludingEntity(null, boxToScan);
		double closestEntity = reachDistance;
		
		if (entitiesHit.isEmpty()) {
			return null;
		}
		
		for (final Entity entityHit : entitiesHit) {
			if ( entityHit != null
			  && entityHit.canBeCollidedWith() ) {
				final float border = entityHit.getCollisionBorderSize();
				final AxisAlignedBB aabb = entityHit.getEntityBoundingBox().expand(border, border, border);
				final RayTraceResult hitMOP = aabb.calculateIntercept(startingPosition, reachPoint);
				
				if (hitMOP != null) {
					if (aabb.contains(startingPosition)) {
						if (0.0D < closestEntity || closestEntity == 0.0D) {
							pickedEntity = new RayTraceResult(entityHit);
							pickedEntity.hitVec = hitMOP.hitVec;
							closestEntity = 0.0D;
						}
					} else {
						final double distance = startingPosition.distanceTo(hitMOP.hitVec);

						if (distance < closestEntity || closestEntity == 0.0D) {
							pickedEntity = new RayTraceResult(entityHit);
							pickedEntity.hitVec = hitMOP.hitVec;
							closestEntity = distance;
						}
					}
				}
			}
		}
		
		return pickedEntity;
	}
	
	@Override
	public int hashCode() {
		return ("X:" + x + "Y:" + y + "Z:" + z).hashCode();
	}
	
	@Override
	public boolean equals(final Object object) {
		if (object instanceof Vector3) {
			final Vector3 vector3 = (Vector3) object;
			return x == vector3.x && y == vector3.y && z == vector3.z;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("Vector3 [%.3f %.3f %.3f]", x, y, z);
	}
}