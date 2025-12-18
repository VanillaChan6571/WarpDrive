package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;

@SuppressWarnings("unused") // Core mod
public class GravityManager {
	
	private static final double OVERWORLD_ENTITY_GRAVITY = 0.080000000000000002D;	// Default value from Vanilla
	private static final double OVERWORLD_ITEM_GRAVITY = 0.039999999105930328D;	// Default value from Vanilla
	private static final double OVERWORLD_ITEM_GRAVITY2 = 0.9800000190734863D;	// Default value from Vanilla
	private static final double HYPERSPACE_FIELD_ENTITY_GRAVITY = 0.035D;
	private static final double HYPERSPACE_VOID_ENTITY_JITTER = 0.005D;
	private static final double SPACE_FIELD_ENTITY_GRAVITY = 0.025D;
	private static final double SPACE_FIELD_ITEM_GRAVITY = 0.02D;
	private static final double SPACE_FIELD_ITEM_GRAVITY2 = 0.60D;
	private static final double SPACE_VOID_GRAVITY = 0.001D;
	private static final double SPACE_VOID_GRAVITY_JETPACK_SNEAK = 0.02D;
	private static final double SPACE_VOID_GRAVITY_RAW_SNEAK = 0.005D; // 0.001 = no mvt
	
	private static boolean isAdvancedRocketryLoaded = false;
	private static Class<?> classGravityHandler;
	private static Method methodIPlanetaryProvider_applyGravity;
	
	@SuppressWarnings("unused") // Core mod
	public static void applyEntityItemGravity(@Nonnull final EntityItem entityItem) {
		final double gravity = CelestialObjectManager.getGravity(entityItem);
		if (gravity == CelestialObject.GRAVITY_NORMAL) {// reroute to AdvancedRocketry if we're set to normal, they'll reroute to Galacticraft on their own
			if (!isAdvancedRocketryLoaded) {
				isAdvancedRocketryLoaded = true;
				try {
					classGravityHandler = Class.forName("zmaster587.advancedRocketry.util.GravityHandler");
					methodIPlanetaryProvider_applyGravity = classGravityHandler.getMethod("applyGravity", Entity.class);
				} catch (final ClassNotFoundException | NoSuchMethodException exception) {
					exception.printStackTrace(WarpDrive.printStreamError);
					classGravityHandler = null;
				}
			}
			
			if (classGravityHandler != null) {
				try {
					methodIPlanetaryProvider_applyGravity.invoke(null, entityItem);
				} catch (final InvocationTargetException | IllegalAccessException  exception) {// report and prevent further calls
					exception.printStackTrace(WarpDrive.printStreamError);
					classGravityHandler = null;
				}
				return;
			}
		}
		
		// fall-back to our own system
		entityItem.motionY -= getItemGravity(entityItem);
	}
	
	@SuppressWarnings("unused") // Core mod
	public static double getGravityForEntity(final Entity entity) {
		final double gravity = CelestialObjectManager.getGravity(entity);
		if (gravity == CelestialObject.GRAVITY_NONE) {
			return SPACE_VOID_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_NORMAL) {
			return OVERWORLD_ENTITY_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_LEGACY_SPACE || gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE) {
			// Is entity in hyper-space?
			final boolean inHyperspace = gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE;
			
			if (isEntityInGraviField(entity)) {
				if (inHyperspace) {
					return HYPERSPACE_FIELD_ENTITY_GRAVITY;
				} else {
					return SPACE_FIELD_ENTITY_GRAVITY;
				}
			} else {
				final double jitter = inHyperspace ? (entity.world.rand.nextDouble() - 0.5D) * 2.0D * HYPERSPACE_VOID_ENTITY_JITTER : 0.0D;
				if (entity instanceof EntityPlayer) {
					final EntityPlayer player = (EntityPlayer) entity;
					
					if (player.isSneaking()) {
						for (final ItemStack armor : player.getArmorInventoryList()) {
							if (armor != null) {
								if (Dictionary.ITEMS_FLYINSPACE.contains(armor.getItem())) {
									return SPACE_VOID_GRAVITY_JETPACK_SNEAK;
								}
							}
						}
						return SPACE_VOID_GRAVITY_RAW_SNEAK;
					} else {
						// FIXME: compensate jetpack
					}
				}
				
				return SPACE_VOID_GRAVITY + jitter;
			}
		}
		
		return gravity * OVERWORLD_ENTITY_GRAVITY;
	}
	
	@SuppressWarnings("unused") // Core mod
	public static double getNegGravityForEntity(final Entity entity) {
		return -getGravityForEntity(entity);
	}
	
	@SuppressWarnings("unused") // Core mod
	public static double getItemGravity(final EntityItem entity) {
		final double gravity = CelestialObjectManager.getGravity(entity);
		if (gravity == CelestialObject.GRAVITY_NONE) {
			return SPACE_VOID_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_NORMAL) {
			return OVERWORLD_ITEM_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_LEGACY_SPACE || gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE) {
			if (isEntityInGraviField(entity)) {
				return SPACE_FIELD_ITEM_GRAVITY;
			} else {
				return SPACE_VOID_GRAVITY;
			}
		} 
		
		return gravity * OVERWORLD_ITEM_GRAVITY;
	}
	
	@SuppressWarnings("unused") // Core mod
	public static double getItemGravity2(final EntityItem entity) {
		final double gravity = CelestialObjectManager.getGravity(entity);
		if (gravity == CelestialObject.GRAVITY_NONE) {
			return SPACE_VOID_GRAVITY;
		}
		
		if (gravity == CelestialObject.GRAVITY_NORMAL) {
			return OVERWORLD_ITEM_GRAVITY2;
		}
		
		if (gravity == CelestialObject.GRAVITY_LEGACY_SPACE || gravity == CelestialObject.GRAVITY_LEGACY_HYPERSPACE) {
			if (isEntityInGraviField(entity)) {
				return SPACE_FIELD_ITEM_GRAVITY2;
			} else {
				return SPACE_VOID_GRAVITY;
			}
		}
		
		return gravity * OVERWORLD_ITEM_GRAVITY2;
	}
	
	private static boolean isEntityInGraviField(final Entity entity) {
		final int y = MathHelper.floor(entity.posY);
		final int x = MathHelper.floor(entity.posX);
		final int z = MathHelper.floor(entity.posZ);
		final int CHECK_DISTANCE = 20;
		
		// Search non-air blocks under player
		final MutableBlockPos blockPos = new MutableBlockPos(x, y, z);
		for (int ny = y; ny > (y - CHECK_DISTANCE); ny--) {
			blockPos.setY(ny);
			final IBlockState blockState = entity.world.getBlockState(blockPos);
			if (!blockState.getBlock().isAir(blockState, entity.world, blockPos)) {
				final AxisAlignedBB axisAlignedBB = blockState.getCollisionBoundingBox(entity.world, blockPos);
				if (axisAlignedBB != null && axisAlignedBB.getAverageEdgeLength() > 0.90D) {
					return true;
				}
			}
		}
		
		return false;
	}
}
