package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IForceFieldShape;
import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.block.TileEntityAbstractBase;
import cr0s.warpdrive.block.TileEntityAbstractBase.UpgradeSlot;
import cr0s.warpdrive.block.forcefield.TileEntityForceFieldProjector;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ForceFieldSetup extends GlobalPosition {
	
	private static final float FORCEFIELD_BASE_SCAN_SPEED_BLOCKS_PER_SECOND = 100;
	private static final float FORCEFIELD_BASE_PLACE_SPEED_BLOCKS_PER_SECOND = 20;
	private static final float FORCEFIELD_MAX_SCAN_SPEED_BLOCKS_PER_SECOND = 10000;
	private static final float FORCEFIELD_MAX_PLACE_SPEED_BLOCKS_PER_SECOND = 4000;
	private static final float FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_PROJECTOR_TIER = 0.50F;
	public static final float FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_RELAY_TIER = 0.25F;
	public static final double FORCEFIELD_ACCELERATION_FACTOR = 0.16D;
	public static final int FORCEFIELD_RELAY_RANGE = 20;
	private static final int FORCEFIELD_MAX_FACTOR_ENTITY_COST = 5;
	private static final double FORCEFIELD_TAU_FACTOR_ENTITY_COST = - Math.log(ForceFieldSetup.FORCEFIELD_MAX_FACTOR_ENTITY_COST);
	
	public final int beamFrequency;
	public final EnumTier enumTier;
	public final Set<TileEntityForceFieldProjector> projectors = new HashSet<>();
	private IBlockState blockStateCamouflage;
	private int colorMultiplierCamouflage;
	private int lightCamouflage;
	private final HashMap<IForceFieldUpgradeEffector, Float> upgrades = new HashMap<>(EnumForceFieldUpgrade.length);
	public final Collection<Object> inventories = new ArrayList<>(12);
	
	public float scanSpeed;
	public float placeSpeed;
	public double startupEnergyCost;
	public double scanEnergyCost;
	public double placeEnergyCost;
	private double entityEnergyCost;
	
	public float breaking_maxHardness;
	public float temperatureLevel;
	public float accelerationLevel;
	public boolean hasCollection;
	public boolean hasFusion;
	public boolean isInverted;
	public boolean hasStabilize;
	public float thickness;
	public float pumping_maxViscosity;
	
	// Projector provided properties
	public float rotationYaw;
	public float rotationPitch;
	public float rotationRoll;
	public VectorI vTranslation = new VectorI(0, 0, 0);
	public final VectorI vMin = new VectorI(-8, -8, -8);
	public final VectorI vMax = new VectorI( 8,  8,  8);
	public IForceFieldShape shapeProvider;
	public boolean isDoubleSided = true;
	
	public ForceFieldSetup(final int dimensionId, final BlockPos blockPos, final EnumTier enumTier, final int beamFrequency) {
		super(dimensionId, blockPos);
		
		this.enumTier = enumTier;
		this.beamFrequency = beamFrequency;
		refresh();
		if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
			WarpDrive.logger.info(String.format("Force field projector energy costs: startup %.3f / %d scan %.3f place %.3f entity %.3f"
				                                    + " speeds: scan %.3f place %.3f"
				                                    + " sustain cost: scan %.3f place %.3f",
				startupEnergyCost, Math.round(startupEnergyCost + placeEnergyCost * placeSpeed * TileEntityForceFieldProjector.PROJECTOR_PROJECTION_UPDATE_TICKS / 20.0F),
				scanEnergyCost, placeEnergyCost, entityEnergyCost,
				scanSpeed, placeSpeed,
				(scanEnergyCost * scanSpeed), (placeEnergyCost * placeSpeed)));
		}
	}
	
	public boolean isAccessGranted(final EntityPlayer entityPlayer, final EnumPermissionNode enumPermissionNode) {
		return false; // TODO
	}
	
	@Nullable
	public IBlockState getCamouflageBlockState() {
		if (Commons.isValidCamouflage(blockStateCamouflage)) {
			return blockStateCamouflage;
		}
		return null;
	}
	
	public int getCamouflageColorMultiplier() {
		if (Commons.isValidCamouflage(blockStateCamouflage)) {
			return colorMultiplierCamouflage;
		}
		return 0;
	}
	
	public int getCamouflageLight() {
		if (Commons.isValidCamouflage(blockStateCamouflage)) {
			return lightCamouflage;
		}
		return 0;
	}
	
	private float getScaledUpgrade(final IForceFieldUpgradeEffector effector) {
		final Float scaledValue = upgrades.get(effector);
		return scaledValue == null ? 0.0F : scaledValue;
	}
	
	private void refresh() {
		final Set<TileEntity> tileEntities = ForceFieldRegistry.getTileEntities(beamFrequency, getWorldServerIfLoaded(), getBlockPos());
		final HashMap<IForceFieldUpgradeEffector, Float> upgradeValues = new HashMap<>(EnumForceFieldUpgrade.length);
		Vector3 v3Min = new Vector3(-1.0D, -1.0D, -1.0D);
		Vector3 v3Max = new Vector3( 1.0D,  1.0D,  1.0D);
		Vector3 v3Translation = new Vector3(0.0D, 0.0D, 0.0D);
		
		for (final TileEntity tileEntity : tileEntities) {
			// sanity check
			if (tileEntity == null) {
				WarpDrive.logger.error(String.format("Invalid tile entity returned from ForgeFieldRegistry at %s",
				                                     this));
				continue;
			}
			// only consider same dimension
			if ( !tileEntity.hasWorld()
			  || tileEntity.getWorld().provider.getDimension() != dimensionId ) {
				continue;
			}
			// ensure entity is constructed
			if (tileEntity instanceof TileEntityAbstractBase) {
				((TileEntityAbstractBase) tileEntity).finishConstruction();
			}
			
			// projectors
			if (tileEntity instanceof TileEntityForceFieldProjector) {
				final TileEntityForceFieldProjector projector = (TileEntityForceFieldProjector) tileEntity;
				if (tileEntity.getPos().getX() == x && tileEntity.getPos().getY() == y && tileEntity.getPos().getZ() == z) {
					shapeProvider = projector.getShapeProvider();
					isDoubleSided = projector.isDoubleSided;
					vTranslation = new VectorI(projector);
					rotationYaw = projector.getRotationYaw();
					rotationPitch = projector.getRotationPitch();
					rotationRoll = projector.getRotationRoll();
					v3Min = projector.getMin();
					v3Max = projector.getMax();
					v3Translation = projector.getTranslation();
					for (final Entry<UpgradeSlot, Integer> entry : projector.getUpgradesOfType(null).entrySet()) {
						if (entry.getKey().itemStack.getItem() instanceof IForceFieldUpgrade) {
							final IForceFieldUpgrade forceFieldUpgrade = (IForceFieldUpgrade) entry.getKey().itemStack.getItem();
							final IForceFieldUpgradeEffector upgradeEffector = forceFieldUpgrade.getUpgradeEffector(entry.getKey().itemStack);
							if (upgradeEffector != null) {
								Float currentValue = upgradeValues.get(upgradeEffector);
								if (currentValue == null) {
									currentValue = 0.0F;
								}
								float addedValue = forceFieldUpgrade.getUpgradeValue(entry.getKey().itemStack) * entry.getValue();
								addedValue *= 1 + (enumTier.getIndex() - 1) * FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_PROJECTOR_TIER;
								upgradeValues.put(upgradeEffector, currentValue + addedValue);
							}
						}
					}
					
				} else {
					if ( ((TileEntityForceFieldProjector) tileEntity).getIsEnabled()
					  && ((TileEntityForceFieldProjector) tileEntity).isCalculated()
					  && ((TileEntityForceFieldProjector) tileEntity).isAssemblyValid() ) {
						projectors.add((TileEntityForceFieldProjector) tileEntity);
					}
				}
			}
			
			// upgrade blocks (namely, relays)
			if (tileEntity instanceof IForceFieldUpgrade) {
				final IForceFieldUpgradeEffector upgradeEffector = ((IForceFieldUpgrade) tileEntity).getUpgradeEffector(tileEntity);
				if (upgradeEffector != null) {
					Float currentValue = upgradeValues.get(upgradeEffector);
					if (currentValue == null) {
						currentValue = 0.0F;
					}
					upgradeValues.put(upgradeEffector, currentValue + ((IForceFieldUpgrade) tileEntity).getUpgradeValue(tileEntity));
					
					// camouflage identification
					if (upgradeEffector == EnumForceFieldUpgrade.CAMOUFLAGE) {
						final BlockPos blockPosCamouflage = tileEntity.getPos().offset(EnumFacing.UP);
						final IBlockState blockStateCandidate = tileEntity.getWorld().getBlockState(blockPosCamouflage);
						if (Commons.isValidCamouflage(blockStateCandidate)) {
							blockStateCamouflage = blockStateCandidate;
							colorMultiplierCamouflage = 0x808080; // blockStateCandidate.colorMultiplier(tileEntity.getWorld(), blockPosCamouflage);
							lightCamouflage = blockStateCandidate.getLightValue(tileEntity.getWorld(), blockPosCamouflage);
						}
					}
					
					// container identification
					if (upgradeEffector == EnumForceFieldUpgrade.ITEM_PORT) {
						inventories.addAll(InventoryWrapper.getConnectedInventories(tileEntity.getWorld(), tileEntity.getPos()));
					}
				}
			}
		}
		
		if (shapeProvider == null) {
			WarpDrive.logger.error(String.format("ForceFieldSetup without a projector or null shape returned @ (%d %d %d), while #%d has %d registered tile entities",
			                                     x, y, z,
			                                     beamFrequency,
			                                     tileEntities.size()));
		}
		
		// set default coefficients, depending on projector
		scanSpeed = FORCEFIELD_BASE_SCAN_SPEED_BLOCKS_PER_SECOND * (isDoubleSided ? 2.1F : 1.0F);
		placeSpeed = FORCEFIELD_BASE_PLACE_SPEED_BLOCKS_PER_SECOND * (isDoubleSided ? 2.1F : 1.0F);
		startupEnergyCost = 60.0F + 20.0F * (enumTier.getIndex() - 1);
		scanEnergyCost = 0.4F + 0.4F * (enumTier.getIndex() - 1);
		placeEnergyCost = 3.0F + 3.0F * (enumTier.getIndex() - 1);
		entityEnergyCost = 2.0F;
		if (isDoubleSided) {
			scanSpeed *= 2.1F;
			placeSpeed *= 2.1F;
			startupEnergyCost += 20.0F * (enumTier.getIndex() - 1);
			scanEnergyCost *= 0.45F;
			placeEnergyCost *= 0.45F;
			entityEnergyCost *= 0.45F;
		}
		
		// apply scaling
		float speedRatio;
		for (final Map.Entry<IForceFieldUpgradeEffector, Float> entry : upgradeValues.entrySet()) {
			final float scaledValue = entry.getKey().getScaledValue(1.0F, entry.getValue());
			if (scaledValue != 0.0F) {
				upgrades.put(entry.getKey(), scaledValue);
				
				speedRatio = entry.getKey().getScanSpeedFactor(scaledValue);
				if (speedRatio > 0.0F) {
					scanSpeed *= speedRatio;
				}
				speedRatio = entry.getKey().getPlaceSpeedFactor(scaledValue);
				if (speedRatio > 0.0F) {
					placeSpeed *= speedRatio;
				}
				
				startupEnergyCost += entry.getKey().getStartupEnergyCost(scaledValue);
				scanEnergyCost += entry.getKey().getScanEnergyCost(scaledValue);
				placeEnergyCost += entry.getKey().getPlaceEnergyCost(scaledValue);
				entityEnergyCost += entry.getKey().getEntityEffectEnergyCost(scaledValue);
			}
		}
		
		// finalize coefficients
		scanSpeed = Math.min(FORCEFIELD_MAX_SCAN_SPEED_BLOCKS_PER_SECOND, scanSpeed);
		placeSpeed = Math.min(FORCEFIELD_MAX_PLACE_SPEED_BLOCKS_PER_SECOND, placeSpeed);
		
		// acceleration is a compound of attraction and repulsion
		accelerationLevel = getScaledUpgrade(EnumForceFieldUpgrade.ATTRACTION) - getScaledUpgrade(EnumForceFieldUpgrade.REPULSION);
		
		// collection, fusion, inversion and stabilize just needs to be defined
		hasCollection = getScaledUpgrade(EnumForceFieldUpgrade.ITEM_PORT) > 0.0F && accelerationLevel > 1.0F;
		hasFusion = getScaledUpgrade(EnumForceFieldUpgrade.FUSION) > 0.0F;
		isInverted = getScaledUpgrade(EnumForceFieldUpgrade.INVERSION) > 0.0F;
		hasStabilize = getScaledUpgrade(EnumForceFieldUpgrade.STABILIZATION) > 0.0F && accelerationLevel < 1.0F;
		
		// temperature is a compound of cooling and heating
		temperatureLevel = Math.max(0.1F, 300.0F + getScaledUpgrade(EnumForceFieldUpgrade.HEATING) - getScaledUpgrade(EnumForceFieldUpgrade.COOLING));
		
		// disintegration, pump and thickness is the actual value
		breaking_maxHardness = getScaledUpgrade(EnumForceFieldUpgrade.BREAKING);
		pumping_maxViscosity = getScaledUpgrade(EnumForceFieldUpgrade.PUMPING);
		thickness = 1.0F + getScaledUpgrade(EnumForceFieldUpgrade.THICKNESS);
		
		// range is maximum distance
		double range = getScaledUpgrade(EnumForceFieldUpgrade.RANGE);
		if (range == 0.0D) {
			range = 8.0D;
			v3Min = new Vector3(-1.0D, -1.0D, -1.0D);
			v3Max = new Vector3( 1.0D,  1.0D,  1.0D);
		}
		if (hasFusion || isInverted) {
			range = Math.min(64.0D, range);
		}
		vMin.x = (int) Math.round(Math.min(0.0D, Math.max(-1.0D, v3Min.x)) * range);
		vMin.y = (int) Math.round(Math.min(0.0D, Math.max(-1.0D, v3Min.y)) * range);
		vMin.z = (int) Math.round(Math.min(0.0D, Math.max(-1.0D, v3Min.z)) * range);
		vMax.x = (int) Math.round(Math.min(1.0D, Math.max( 0.0D, v3Max.x)) * range);
		vMax.y = (int) Math.round(Math.min(1.0D, Math.max( 0.0D, v3Max.y)) * range);
		vMax.z = (int) Math.round(Math.min(1.0D, Math.max( 0.0D, v3Max.z)) * range);
		vTranslation.x += (int) Math.round(Math.min(1.0D, Math.max(-1.0D, v3Translation.x)) * range);
		vTranslation.y += (int) Math.round(Math.min(1.0D, Math.max(-1.0D, v3Translation.y)) * range);
		vTranslation.z += (int) Math.round(Math.min(1.0D, Math.max(-1.0D, v3Translation.z)) * range);
	}
	
	public double getEntityEnergyCost(final int countEntityInteractions) {
		return entityEnergyCost * FORCEFIELD_MAX_FACTOR_ENTITY_COST * Math.exp(FORCEFIELD_TAU_FACTOR_ENTITY_COST / countEntityInteractions);
	}
	
	public int onEntityEffect(final World world, final BlockPos blockPos, final Entity entity) {
		int countdown = 0;
		final TileEntity tileEntity = world.getTileEntity(getBlockPos());
		if (tileEntity instanceof TileEntityForceFieldProjector) {
			if (((TileEntityForceFieldProjector) tileEntity).onEntityInteracted(entity.getUniqueID())) {
				for (final Map.Entry<IForceFieldUpgradeEffector, Float> entry : upgrades.entrySet()) {
					Float value = entry.getValue();
					if ( entry.getKey() == EnumForceFieldUpgrade.COOLING
					  || entry.getKey() == EnumForceFieldUpgrade.HEATING ) {
						value = temperatureLevel;
					} else if ( entry.getKey() == EnumForceFieldUpgrade.ATTRACTION
					         || entry.getKey() == EnumForceFieldUpgrade.REPULSION ) {
						value = accelerationLevel;
					}
					countdown += entry.getKey().onEntityEffect(value, world, x, y, z, blockPos, entity);
				}
			}
		}
		return countdown;
	}
	
	public double applyDamage(final World world, final DamageSource damageSource, final double damageLevel) {
		assert damageSource != null;
		final TileEntity tileEntity = world.getTileEntity(getBlockPos());
		if (tileEntity instanceof TileEntityForceFieldProjector) {
			final double scaledDamage;
			if (damageSource.damageType.contains("explosion")) {
				scaledDamage = damageLevel / WarpDriveConfig.FORCE_FIELD_PROJECTOR_EXPLOSION_SCALE + entityEnergyCost * 0.1D;
			} else if (damageSource.damageType.contains("laser")) {
				final double scaleLaserEnergyPerFullProjector = WarpDriveConfig.FORCE_FIELD_PROJECTOR_MAX_LASER_REQUIRED
				                                              * (WarpDriveConfig.LASER_CANNON_MAX_LASER_ENERGY - WarpDriveConfig.LASER_CANNON_BLOCK_HIT_ENERGY_MAX)
				                                              / WarpDriveConfig.FORCE_FIELD_PROJECTOR_MAX_ENERGY_STORED_BY_TIER[EnumTier.SUPERIOR.getIndex()];
				scaledDamage = damageLevel / scaleLaserEnergyPerFullProjector + entityEnergyCost * 5.0D;
			} else {
				WarpDrive.logger.warn(String.format("%s Unknown damage source %s '%s' %.1f",
				                                    this, damageSource, damageSource.getDamageType(), damageLevel));
				scaledDamage = Commons.clamp(0, 10000, damageLevel);
			}
			((TileEntityForceFieldProjector) tileEntity).onEnergyDamage(scaledDamage);
			return 0.0D;
		}
		return damageLevel;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ DIM%d (%d %d %d) (%d %d %d) -> (%d %d %d)",
			getClass().getSimpleName(), dimensionId,
			x, y, z,
			vMin.x, vMin.y, vMin.z,
			vMax.x, vMax.y, vMax.z);
	}
}
