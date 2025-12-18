package cr0s.warpdrive.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface ITransformation {
	
	// Return the World where the block or entity will be deployed.
	World getTargetWorld();
	
	// Return the number of 90 deg rotations to the right.
	// Possible values are 0 (forward), 1 (turn right), 2 (turn back) and 3 (turning left).
	byte getRotationSteps();
	
	// reserved
	float getRotationYaw();
	
	// Return true if the provided coordinates are strictly inside the ship.
	// Use this to keep connections with a remote location, or break the related links. 
	boolean isInside(final double x, final double y, final double z);
	
	// Return true if the provided coordinates are strictly inside the ship.
	// Use this to keep connections with a remote location, or break the related links. 
	boolean isInside(final int x, final int y, final int z);
	
	// Translate then rotate the provided world coordinates.
	// Rotation is always relative to the Ship core.
	// Use this to transform floating coordinates, notably sub-components of a block.
	Vec3d apply(final double sourceX, final double sourceY, final double sourceZ);
	
	// Translate then rotate the provided world coordinates.
	// Rotation is always relative to the Ship core.
	// Use this to transform a block aligned coordinate.
	BlockPos apply(final int sourceX, final int sourceY, final int sourceZ);
	
	// Return the new coordinates for the provided TileEntity.
	// Deprecated: use apply(tileEntity.getPos()) instead.
	@Deprecated
	BlockPos apply(final TileEntity tileEntity);
	
	// Translate then rotate the provided world coordinates.
	// Rotation is always relative to the Ship core.
	BlockPos apply(final BlockPos blockPos);
}