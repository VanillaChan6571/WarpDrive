package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.computer.ICoreSignature;

import javax.annotation.Nonnull;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class GlobalRegion extends GlobalPosition {
	
	// persistent properties
	public final EnumGlobalRegionType type;
	public final UUID uuid;
	public String name;
	
	public int maxX, maxY, maxZ;
	public int minX, minY, minZ;
	
	public int mass;
	public double isolationRate;
	
	// computed properties
	private AxisAlignedBB cache_aabbArea;
	
	private GlobalRegion(
			final int dimensionId, final BlockPos blockPos,
			final EnumGlobalRegionType type, final UUID uuid, final String name,
			final AxisAlignedBB aabbArea,
			final int mass, final double isolationRate) {
		super(dimensionId, blockPos);
		this.type = type;
		this.uuid = uuid;
		if (aabbArea == null) {
			this.minX = blockPos.getX();
			this.minY = blockPos.getY();
			this.minZ = blockPos.getZ();
			this.maxX = blockPos.getX();
			this.maxY = blockPos.getY();
			this.maxZ = blockPos.getZ();
		} else {
			this.minX = (int) aabbArea.minX;
			this.minY = (int) aabbArea.minY;
			this.minZ = (int) aabbArea.minZ;
			this.maxX = (int) aabbArea.maxX - 1;
			this.maxY = (int) aabbArea.maxY - 1;
			this.maxZ = (int) aabbArea.maxZ - 1;
		}
		this.mass = mass;
		this.isolationRate = isolationRate;
		this.name = name;
		
		this.cache_aabbArea = null;
	}
	
	public GlobalRegion(final IGlobalRegionProvider globalRegionProvider) {
		this(globalRegionProvider.getDimension(), globalRegionProvider.getBlockPos(),
		     globalRegionProvider.getGlobalRegionType(), globalRegionProvider.getSignatureUUID(), globalRegionProvider.getSignatureName(),
		     globalRegionProvider.getGlobalRegionArea(),
		     globalRegionProvider.getMass(), globalRegionProvider.getIsolationRate() );
	}
	
	public boolean sameCoordinates(final IGlobalRegionProvider globalRegionProvider) {
		return dimensionId == globalRegionProvider.getDimension()
			&& x == globalRegionProvider.getBlockPos().getX()
			&& y == globalRegionProvider.getBlockPos().getY()
			&& z == globalRegionProvider.getBlockPos().getZ();
	}
	
	public void update(final IGlobalRegionProvider globalRegionProvider) {
		if (WarpDrive.isDev) {
			assert type == globalRegionProvider.getGlobalRegionType();
			assert uuid.equals(globalRegionProvider.getSignatureUUID());
		}
		final AxisAlignedBB aabbAreaUpdated = globalRegionProvider.getGlobalRegionArea();
		if (aabbAreaUpdated != null) {
			minX = (int) aabbAreaUpdated.minX;
			minY = (int) aabbAreaUpdated.minY;
			minZ = (int) aabbAreaUpdated.minZ;
			maxX = (int) aabbAreaUpdated.maxX - 1;
			maxY = (int) aabbAreaUpdated.maxY - 1;
			maxZ = (int) aabbAreaUpdated.maxZ - 1;
			cache_aabbArea = null;
		}
		mass = globalRegionProvider.getMass();
		isolationRate = globalRegionProvider.getIsolationRate();
		name = globalRegionProvider.getSignatureName();
	}
	
	public boolean contains(@Nonnull final BlockPos blockPos) {
		return    minX <= blockPos.getX() && blockPos.getX() <= maxX
		       && minY <= blockPos.getY() && blockPos.getY() <= maxY
		       && minZ <= blockPos.getZ() && blockPos.getZ() <= maxZ;
	}
	
	public AxisAlignedBB getArea() {
		if (cache_aabbArea == null) {
			cache_aabbArea = new AxisAlignedBB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
		}
		return cache_aabbArea;
	}
	
	public GlobalRegion(final NBTTagCompound tagCompound) {
		super(tagCompound);
		type = EnumGlobalRegionType.getByName(tagCompound.getString("type"));
		name = tagCompound.getString(ICoreSignature.NAME_TAG);
		UUID uuidLocal = new UUID(tagCompound.getLong(ICoreSignature.UUID_MOST_TAG), tagCompound.getLong(ICoreSignature.UUID_LEAST_TAG));
		if (uuidLocal.getMostSignificantBits() == 0L && uuidLocal.getLeastSignificantBits() == 0L) {
			uuidLocal = UUID.randomUUID();
		}
		uuid = uuidLocal;
		maxX = tagCompound.getInteger("maxX");
		maxY = tagCompound.getInteger("maxY");
		maxZ = tagCompound.getInteger("maxZ");
		minX = tagCompound.getInteger("minX");
		minY = tagCompound.getInteger("minY");
		minZ = tagCompound.getInteger("minZ");
		mass = tagCompound.getInteger("mass");
		isolationRate = tagCompound.getDouble("isolationRate");
		
		cache_aabbArea = null;
	}
	
	@Override
	public void writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setString("type", type.getName());
		if (name != null && !name.isEmpty()) {
			tagCompound.setString(ICoreSignature.NAME_TAG, name);
		}
		if (uuid != null) {
			tagCompound.setLong(ICoreSignature.UUID_MOST_TAG, uuid.getMostSignificantBits());
			tagCompound.setLong(ICoreSignature.UUID_LEAST_TAG, uuid.getLeastSignificantBits());
		}
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("maxZ", maxZ);
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("mass", mass);
		tagCompound.setDouble("isolationRate", isolationRate);
	}
	
	public String getFormattedLocation() {
		final CelestialObject celestialObject = CelestialObjectManager.get(false, dimensionId, x, z);
		if (celestialObject == null) {
			return String.format("DIM%d @ (%d %d %d)",
			                     dimensionId,
			                     x, y, z);
		} else {
			return String.format("%s [DIM%d] @ (%d %d %d)",
			                     celestialObject.getDisplayName(),
			                     dimensionId,
			                     x, y, z);
		}
	}
	
	@Override
	public int hashCode() {
		return dimensionId << 24 + (x >> 10) << 12 + y << 10 + (z >> 10);
	}
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
		                     getClass().getSimpleName(),
			                 type, uuid,
			                 dimensionId,
			                 x, y, z,
			                 minX, minY, minZ,
			                 maxX, maxY, maxZ);
	}
}