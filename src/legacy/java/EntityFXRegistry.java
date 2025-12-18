package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.render.AbstractEntityFX;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Registry of all active entity FX on this client
 * 
 * @author LemADEC
 */
public class EntityFXRegistry {
	
	private static final HashMap<Integer, CopyOnWriteArraySet<WeakReference<AbstractEntityFX>>> REGISTRY = new HashMap<>();
	private static int countAdd = 0;
	private static int countRemove = 0;
	private static int countRead = 0;
	
	public EntityFXRegistry() {
	}
	
	private static int computeHashcode(final AbstractEntityFX entityFX) {
		return computeHashcode(entityFX.getWorld().provider.getDimension(),
		                       MathHelper.floor(entityFX.getX()),
		                       MathHelper.floor(entityFX.getY()),
		                       MathHelper.floor(entityFX.getZ()));
	}
	
	private static int computeHashcode(final World world, final Vector3 v3Position) {
		return computeHashcode(world.provider.getDimension(),
		                       MathHelper.floor(v3Position.x),
		                       MathHelper.floor(v3Position.y),
		                       MathHelper.floor(v3Position.z));
	}
	
	private static int computeHashcode(final int dimensionId, final int x, final int y, final int z) {
		return (dimensionId << 24) ^ ((x & 0xFFFF) << 8) ^ (y << 16) ^ (z & 0xFFFF);
	}
	
	private static void logStats(final int trigger) {
		if ((trigger & 0x3FF) != 0) {
			return;
		}
		
		int sizeTotal = 0;
		int sizeClusterMax = 0;
		for (final CopyOnWriteArraySet<WeakReference<AbstractEntityFX>> items : REGISTRY.values()) {
			final int size = items.size();
			sizeTotal += size;
			sizeClusterMax = Math.max(sizeClusterMax, size);
		}
		
		WarpDrive.logger.info(String.format("AbstractEntityFX REGISTRY stats: read %d add %d remove %d => %.3f read, currently holding %d items %d hashes %d maxCluster",
		                                    countRead, countAdd, countRemove,
		                                    ((float) countRead) / (countRemove + countRead + countAdd),
		                                    sizeTotal, REGISTRY.size(), sizeClusterMax));
	}
	
	public static AbstractEntityFX get(final World world, final Vector3 v3Position, final double rangeMax) {
		countRead++;
		if (WarpDriveConfig.LOGGING_ENTITY_FX) {
			logStats(countRead);
		}
		
		// get by hashcode
		final Integer hashcode = computeHashcode(world, v3Position);
		final CopyOnWriteArraySet<WeakReference<AbstractEntityFX>> setRegistryItems = REGISTRY.get(hashcode);
		if (setRegistryItems == null) {
			return null;
		}
		
		final double rangeMaxSquare = rangeMax * rangeMax;
		
		// get the exact match
		for (final WeakReference<AbstractEntityFX> weakEntityFX : setRegistryItems) {
			if (weakEntityFX == null) {
				countRemove++;
				setRegistryItems.remove(null);
				continue;
			}
			final AbstractEntityFX entityFX = weakEntityFX.get();
			if ( entityFX == null
			  || !entityFX.isAlive() ) {
				countRemove++;
				setRegistryItems.remove(weakEntityFX);
				continue;
			}
			final double rangeSquared = v3Position.distanceTo_square(entityFX);
			if (rangeSquared < rangeMaxSquare) {
				return entityFX;
			}
		}
		return null;
	}
	
	public static boolean add(final AbstractEntityFX entityFX) {
		countRead++;
		if (WarpDriveConfig.LOGGING_ENTITY_FX) {
			logStats(countRead);
		}
		
		// get by hashcode
		final Integer hashcode = computeHashcode(entityFX);
		CopyOnWriteArraySet<WeakReference<AbstractEntityFX>> setRegistryItems = REGISTRY.get(hashcode);
		if (setRegistryItems == null) {
			setRegistryItems = new CopyOnWriteArraySet<>();
			REGISTRY.put(hashcode, setRegistryItems);
		} else {
			// get the exact match
			final Vector3 v3Position = new Vector3(entityFX);
			for (final WeakReference<AbstractEntityFX> weakEntityFX : setRegistryItems) {
				if (weakEntityFX == null) {
					countRemove++;
					setRegistryItems.remove(null);
					continue;
				}
				final AbstractEntityFX entityFX_existing = weakEntityFX.get();
				if ( entityFX_existing == null
				  || !entityFX_existing.isAlive() ) {
					countRemove++;
					setRegistryItems.remove(weakEntityFX);
					continue;
				}
				if (entityFX == entityFX_existing) {
					if (WarpDriveConfig.LOGGING_ENTITY_FX) {
						printRegistry("already registered");
					}
					return false;
				}
				if (v3Position.distanceTo_square(entityFX) < 0.01D) {
					if (WarpDriveConfig.LOGGING_ENTITY_FX) {
						printRegistry("existing entity at location");
					}
					return false;
				}
			}
		}
		
		// not found => add
		countAdd++;
		setRegistryItems.add(new WeakReference<>(entityFX));
		if (WarpDriveConfig.LOGGING_ENTITY_FX) {
			printRegistry("added");
		}
		return true;
	}
	
	private static void printRegistry(final String trigger) {
		WarpDrive.logger.info(String.format("AbstractEntityFX REGISTRY (%d entries after %s):",
		                                    REGISTRY.size(), trigger));
		
		for (final Entry<Integer, CopyOnWriteArraySet<WeakReference<AbstractEntityFX>>> entryRegistryItems : REGISTRY.entrySet()) {
			final StringBuilder message = new StringBuilder();
			final Iterator<WeakReference<AbstractEntityFX>> iterator = entryRegistryItems.getValue().iterator();
			while (iterator.hasNext()) {
				final WeakReference<AbstractEntityFX> weakEntityFX = iterator.next();
				if (weakEntityFX == null) {
					countRemove++;
					iterator.remove();
					continue;
				}
				final AbstractEntityFX entityFX = weakEntityFX.get();
				if (entityFX == null) {
					countRemove++;
					iterator.remove();
					continue;
				}
				message.append(String.format("\n- %s",
				                             entityFX));
			}
			WarpDrive.logger.info(String.format("- %d entries with hashcode 0x%8X: %s",
			                                    entryRegistryItems.getValue().size(),
			                                    entryRegistryItems.getKey(),
			                                    message.toString()));
		}
	}
}
