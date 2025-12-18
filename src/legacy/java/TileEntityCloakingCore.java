package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CloakedArea;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map.Entry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants.NBT;

import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.hash.TLongIntHashMap;

public class TileEntityCloakingCore extends TileEntityAbstractEnergyCoreOrController {
	
	// global properties
	private static final int CLOAKING_CORE_SOUND_UPDATE_TICKS = 40;
	private static final int DISTANCE_INNER_COILS_BLOCKS = 2;
	private static final int LASER_REFRESH_TICKS = 100;
	private static final int LASER_DURATION_TICKS = 110;
	
	// inner coils color map
	private static final float[] innerCoilColor_r = { 1.00f, 1.00f, 1.00f, 1.00f, 0.75f, 0.25f, 0.00f, 0.00f, 0.00f, 0.00f, 0.50f, 1.00f };
	private static final float[] innerCoilColor_g = { 0.00f, 0.25f, 0.75f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 0.50f, 0.25f, 0.00f, 0.00f };
	private static final float[] innerCoilColor_b = { 0.25f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.50f, 1.00f, 1.00f, 1.00f, 1.00f, 0.75f };
	
	private static final UpgradeSlot upgradeSlotTransparency = new UpgradeSlot("cloaking.transparency",
	                                                                           ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_CRYSTAL, 6),
	                                                                           1);
	
	// persistent properties
	// spatial cloaking field parameters
	private final boolean[] isValidInnerCoils = { false, false, false, false, false, false };
	private final int[] distanceOuterCoils_blocks = { 0, 0, 0, 0, 0, 0 };   // 0 means invalid
	private final TLongIntHashMap chunkIndexVolume = new TLongIntHashMap();
	
	// computed properties
	private int minX = 0;
	private int minY = 0;
	private int minZ = 0;
	private int maxX = 0;
	private int maxY = 0;
	private int maxZ = 0;
	private AxisAlignedBB aabbArea = null;
	private boolean isFullyTransparent = false;
	private boolean isFullScanRequired = false;
	private boolean isCloaking = false;
	private long timeLastCloakScanDone = -1;
	private CloakScanner cloakScanner = null;
	private int volume = 0;
	private int energyRequired = 0;
	private int updateRate_ticks = 1;
	private int updateTicks = 0;
	private int laserDrawingTicks = 0;
	
	private boolean soundPlayed = false;
	private int soundTicks = 0;
	
	public TileEntityCloakingCore() {
		super();
		
		peripheralName = "warpdriveCloakingCore";
		// addMethods(new String[] {});
		CC_scripts = Arrays.asList("enable", "disable");
		
		registerUpgradeSlot(upgradeSlotTransparency);
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.CLOAKING_MAX_ENERGY_STORED,
		                     16384, 0,
		                     "EV", 2, "HV", 0);
		isFullyTransparent = hasUpgrade(upgradeSlotTransparency);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// Reset sound timer
		soundTicks--;
		if (soundTicks < 0) {
			soundTicks = CLOAKING_CORE_SOUND_UPDATE_TICKS;
			soundPlayed = false;
		}
		
		// rescan cloak volume if it's too old
		if ( isFullScanRequired
		  || timeLastCloakScanDone + WarpDriveConfig.CLOAKING_VOLUME_SCAN_AGE_TOLERANCE_SECONDS * 20L < world.getTotalWorldTime() ) {
			timeLastCloakScanDone = -1;
		}
		
		// scan cloak content progressively
		if (timeLastCloakScanDone <= 0L) {
			timeLastCloakScanDone = world.getTotalWorldTime();
			
			// start scanning if dimensions are properly set
			if ( minX != pos.getX() && maxX != pos.getX()
			  && minY != pos.getY() && maxY != pos.getY()
			  && minZ != pos.getZ() && maxZ != pos.getZ() ) {
			    cloakScanner = new CloakScanner(world, minX, minY, minZ, maxX, maxY, maxZ, isFullyTransparent, isFullScanRequired);
				isFullScanRequired = false;
			    if (WarpDriveConfig.LOGGING_CLOAKING) {
				    WarpDrive.logger.info(String.format("%s scanning started",
				                                        this));
			    }
			}
		}
		if (cloakScanner != null) {
			if (cloakScanner.tick()) {// scan is done
				for (final Entry<Long, Integer> entryChunkIndexVolume : cloakScanner.chunkIndexVolume.entrySet()) {
					chunkIndexVolume.put(entryChunkIndexVolume.getKey(), entryChunkIndexVolume.getValue());
				}
				cloakScanner = null;
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s scanning done",
					                                    this ));
				}
			}
		}
		
		boolean isLaserRefreshNeeded = false;
		
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = updateRate_ticks; // resetting timer
			
			isCloaking = WarpDrive.cloaks.isAreaExists(world, pos);
			final boolean hasEnoughPower = energy_consume(energyRequired, !isAssemblyValid || !isEnabled);
			final boolean isVolumeDefined = volume > 13; // when freshly started or during ship jump
			final boolean shouldBeCloaking = isAssemblyValid && isVolumeDefined && isEnabled && hasEnoughPower;
			
			if (!isCloaking) {
				if (shouldBeCloaking) {// start cloaking
					updateCoils(true, true);
					isLaserRefreshNeeded = true;
					
					// register cloak
					final CloakedArea area = WarpDrive.cloaks.updateCloakedArea(world, pos, isFullyTransparent,
					                                                            minX, minY, minZ, maxX, maxY, maxZ);
					if (!soundPlayed) {
						soundPlayed = true;
						world.playSound(null, pos, SoundEvents.CLOAK, SoundCategory.BLOCKS, 4.0F, 1F);
					}
					
					// start cloaking
					area.sendCloakPacketToPlayersEx(false);
				}
				
			} else {// is cloaking
				if (shouldBeCloaking) {// maintain cloaking
					// Refresh the field (workaround to re-synchronize players since client may 'eat up' the packets)
					final CloakedArea area = WarpDrive.cloaks.getCloakedArea(world, pos);
					if (area == null) {
						WarpDrive.logger.error(String.format("Cloaked area lost %s",
						                                     Commons.format(world, pos) ));
					} else {
						area.sendCloakPacketToPlayersEx(false); // re-cloak field
					}
					
				} else {// stop cloaking
					if (WarpDriveConfig.LOGGING_CLOAKING) {
						WarpDrive.logger.info(String.format("%s Disabled, cloak field going down...",
						                                    this ));
					}
					
					if (isEnabled) {// collapsing
						if (!isAssemblyValid) {
							if (WarpDriveConfig.LOGGING_CLOAKING) {
								WarpDrive.logger.info(String.format("%s Coil(s) lost, cloak field is collapsing...",
								                                    this ));
							}
							energy_consume(energy_getEnergyStored());
							
						} else if (!hasEnoughPower) {
							if (WarpDriveConfig.LOGGING_CLOAKING) {
								WarpDrive.logger.info(String.format("%s Low power, cloak field is collapsing...",
								                                    this ));
							}
						}
					}
					
					updateCoils(true, false);
					disableCloakingField();
				}
			} 
		}
		
		laserDrawingTicks--;
		if (isLaserRefreshNeeded || laserDrawingTicks <= 0) {
			laserDrawingTicks = LASER_REFRESH_TICKS;
			
			if (isEnabled && isAssemblyValid) {
				drawLasers();
			}
		}
	}
	
	@Override
	public void onBlockBroken(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		setIsEnabled(false);
		updateCoils(false, false);
		disableCloakingField();
		
		super.onBlockBroken(world, blockPos, blockState);
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		final int maxOuterCoilDistance = WarpDriveConfig.CLOAKING_MAX_FIELD_RADIUS - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		boolean isRefreshNeeded = false;
		int countIntegrity = 1; // 1 for the core + 1 per coil
		final StringBuilder messageInnerCoils = new StringBuilder();
		final StringBuilder messageOuterCoils = new StringBuilder();
		
		// Directions to check (all six directions: left, right, up, down, front, back)
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		for (final EnumFacing direction : EnumFacing.values()) {
			
			// check validity of inner coil
			mutableBlockPos.setPos(pos).move(direction, DISTANCE_INNER_COILS_BLOCKS);
			IBlockState blockState = world.getBlockState(mutableBlockPos);
			final boolean isInnerValid = blockState.getBlock() instanceof BlockCloakingCoil;
			if (isInnerValid) {
				BlockCloakingCoil.setBlockState(world, mutableBlockPos, blockState, true, isCloaking, false, direction);
			}
			
			// whenever a change is detected, force a laser redraw 
			if (isInnerValid != isValidInnerCoils[direction.ordinal()]) {
				isRefreshNeeded = true;
				isValidInnerCoils[direction.ordinal()] = isInnerValid;
			}
			
			// update validity results
			if (isValidInnerCoils[direction.ordinal()]) {
				countIntegrity++;
			} else {
				if (messageInnerCoils.length() != 0) {
					messageInnerCoils.append(", ");
				}
				messageInnerCoils.append(direction.name());
			}
			
			// check validity of outer coil
			mutableBlockPos.setPos(pos).move(direction, distanceOuterCoils_blocks[direction.ordinal()]);
			blockState = world.getBlockState(mutableBlockPos);
			final boolean isActualOuterValid = blockState.getBlock() instanceof BlockCloakingCoil;
			if (isActualOuterValid) {
				BlockCloakingCoil.setBlockState(world, mutableBlockPos, blockState, true, isCloaking, true, direction);
				countIntegrity++;
				continue;
			}
			
			// find closest outer coil
			int newCoilDistance = 0;
			for (int distance = DISTANCE_INNER_COILS_BLOCKS + 1; distance < maxOuterCoilDistance; distance++) {
				mutableBlockPos.setPos(pos).move(direction, distance);
				blockState = world.getBlockState(mutableBlockPos);
				if (blockState.getBlock() instanceof BlockCloakingCoil) {
					BlockCloakingCoil.setBlockState(world, mutableBlockPos, blockState, true, isCloaking, true, direction);
					newCoilDistance = distance;
					break;
				}
			}
			
			// whenever a change is detected, force a laser redraw
			final int oldCoilDistance = distanceOuterCoils_blocks[direction.ordinal()];
			if (newCoilDistance != oldCoilDistance) {
				isRefreshNeeded = true;
				distanceOuterCoils_blocks[direction.ordinal()] = newCoilDistance;
			}
			
			// update validity results
			if (newCoilDistance > 0) {
				countIntegrity++;
			} else {
				if (messageOuterCoils.length() != 0) {
					messageOuterCoils.append(", ");
				}
				messageOuterCoils.append(direction.name());
			}
		}
		
		// build status message
		final float integrity = countIntegrity / 13.0F;
		if (messageInnerCoils.length() > 0 && messageOuterCoils.length() > 0) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.cloaking_core.missing_channeling_and_projecting_coils",
			                  Math.round(100.0F * integrity), messageInnerCoils, messageOuterCoils);
		} else if (messageInnerCoils.length() > 0) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.cloaking_core.missing_channeling_coils",
			                  Math.round(100.0F * integrity), messageInnerCoils);
		} else if (messageOuterCoils.length() > 0) {
			textReason.append(Commons.getStyleWarning(), "warpdrive.cloaking_core.missing_projecting_coils",
			                  Math.round(100.0F * integrity), messageOuterCoils);
		} else {
			textReason.append(Commons.getStyleCorrect(), "warpdrive.cloaking_core.valid");
		}
		
		// update transparency flag
		final boolean isFullyTransparent_new = hasUpgrade(upgradeSlotTransparency);
		if (isFullyTransparent != isFullyTransparent_new) {
			isRefreshNeeded = true;
			isFullyTransparent = isFullyTransparent_new;
		}
		
		// update cloaking field parameters defined by coils
		isValid = isValid && countIntegrity >= 13;
		minX =               pos.getX() - distanceOuterCoils_blocks[4] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		maxX =               pos.getX() + distanceOuterCoils_blocks[5] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		minY = Math.max(  0, pos.getY() - distanceOuterCoils_blocks[0] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS);
		maxY = Math.min(255, pos.getY() + distanceOuterCoils_blocks[1] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS);
		minZ =               pos.getZ() - distanceOuterCoils_blocks[2] - WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		maxZ =               pos.getZ() + distanceOuterCoils_blocks[3] + WarpDriveConfig.CLOAKING_COIL_CAPTURE_BLOCKS;
		if (aabbArea == null || isRefreshNeeded) {
			aabbArea = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
		}
		
		// refresh cloaking area only when needed
		if (isCloaking && isRefreshNeeded) {
			disableCloakingField();
		}
		
		// purge cache & scanner if we're invalid
		if (!isValid) {
			cloakScanner = null;
			chunkIndexVolume.clear();
		}
		
		// scan volume
		if (isValid && isRefreshNeeded) {
			if (cloakScanner != null) {
				if (WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s Aborting current scan due to major changes detected.",
					                                    this ));
				}
				cloakScanner = null;
			}
			isFullScanRequired = true;
		}
		
		return isValid;
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		super.doUpdateParameters(isDirty);
		
		final int minChunkX = minX >> 4;
		final int maxChunkX = maxX >> 4;
		final int minChunkZ = minZ >> 4;
		final int maxChunkZ = maxZ >> 4;
		
		// scan chunk volume, purging invalid positions and computing total volume
		int volume_new = 0;
		for (final TLongIntIterator longIntIterator = chunkIndexVolume.iterator(); longIntIterator.hasNext(); ) {
			longIntIterator.advance();
			final long chunkIndex = longIntIterator.key();
			final int chunkX = (int) (chunkIndex         & 0xFFFFFFFFL);
			final int chunkZ = (int) ((chunkIndex >> 32) & 0xFFFFFFFFL);
			if ( chunkX < minChunkX || chunkX > maxChunkX
			  || chunkZ < minChunkZ || chunkZ > maxChunkZ ) {
				longIntIterator.remove();
				if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
					WarpDrive.logger.info(String.format("%s Chunk (%d %d) %s was removed from cache.",
					                                    this,
					                                    chunkX, chunkZ,
					                                    Commons.format(world, new ChunkPos(chunkX, chunkZ).getBlock(8, 128, 8)) ));
				}
			}
			
			if (WarpDrive.isDev && WarpDriveConfig.LOGGING_CLOAKING) {
				WarpDrive.logger.info(String.format("%s Chunk (%d %d) %s has a volume of %d blocks.",
				                                    this,
				                                    chunkX, chunkZ,
				                                    Commons.format(world, new ChunkPos(chunkX, chunkZ).getBlock(8, 128, 8)),
				                                    longIntIterator.value() ));
			}
			volume_new += longIntIterator.value();
		}
		volume = volume_new;
		if (isFullyTransparent) {
			energyRequired = volume * WarpDriveConfig.CLOAKING_TIER2_ENERGY_PER_BLOCK;
			updateRate_ticks = WarpDriveConfig.CLOAKING_TIER2_FIELD_REFRESH_INTERVAL_TICKS;
		} else {
			energyRequired = volume * WarpDriveConfig.CLOAKING_TIER1_ENERGY_PER_BLOCK;
			updateRate_ticks = WarpDriveConfig.CLOAKING_TIER1_FIELD_REFRESH_INTERVAL_TICKS;
		}
		
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s Requiring %d EU for %d blocks",
			                                    this, energyRequired, volume));
		}
	}
	
	private void updateCoils(final boolean isConnected, final boolean isActive) {
		// update core, only if it's still present/connected
		if (isConnected) {
			updateBlockState(null, BlockProperties.ACTIVE, isActive);
		}
		
		// update coils
		for (final EnumFacing direction : EnumFacing.VALUES) {
			if (isValidInnerCoils[direction.ordinal()]) {
				updateCoil(isConnected, isActive, direction, DISTANCE_INNER_COILS_BLOCKS);
			}
			if (distanceOuterCoils_blocks[direction.ordinal()] > 0) {
				updateCoil(isConnected, isActive, direction, distanceOuterCoils_blocks[direction.ordinal()]);
			}
		}
	}
	
	private void updateCoil(final boolean isConnected, final boolean isActive, final EnumFacing direction, final int distance) {
		final BlockPos blockPos = pos.offset(direction, distance);
		final IBlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock() instanceof BlockCloakingCoil) {
			if (isConnected) {
				if (distance == DISTANCE_INNER_COILS_BLOCKS) {
					BlockCloakingCoil.setBlockState(world, blockPos, blockState, true, isActive, false, EnumFacing.DOWN);
				} else {
					BlockCloakingCoil.setBlockState(world, blockPos, blockState, true, isActive, true, direction);
				}
			} else {
				BlockCloakingCoil.setBlockState(world, blockPos, blockState, false, false, true, EnumFacing.DOWN);
			}
		}
	}
	
	private void drawLasers() {
		float r;
		float g;
		float b;
		if (!isCloaking) {// out of energy
			r = 0.75f;
			g = 0.50f;
			b = 0.50f;
		} else if (!isFullyTransparent) {
			r = 0.00f;
			g = 1.00f;
			b = 0.25f;
		} else {
			r = 0.00f;
			g = 0.25f;
			b = 1.00f;
		}
		
		// Directions to check (all six directions: left, right, up, down, front, back)
		for (final EnumFacing direction : EnumFacing.values()) {
			if ( isValidInnerCoils[direction.ordinal()]
			  && distanceOuterCoils_blocks[direction.ordinal()] > 0) {
				PacketHandler.sendBeamPacketToPlayersInArea(world,
				        new Vector3(
				                   pos.getX() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.getXOffset(),
				                   pos.getY() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.getYOffset(),
				                   pos.getZ() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * direction.getZOffset()),
				        new Vector3(
				                   pos.getX() + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.getXOffset(),
				                   pos.getY() + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.getYOffset(),
				                   pos.getZ() + 0.5D + distanceOuterCoils_blocks[direction.ordinal()] * direction.getZOffset()),
				        r, g, b,
				        LASER_DURATION_TICKS,
				        aabbArea);
			}
		}
		
		// draw connecting coils
		for (int i = 0; i < 5; i++) {
			final EnumFacing start = EnumFacing.VALUES[i];
			for (int j = i + 1; j < 6; j++) {
				final EnumFacing stop = EnumFacing.VALUES[j];
				// skip mirrored coils (removing the inner lines)
				if (start.getOpposite() == stop) {
					continue;
				}
				
				// draw a random colored beam
				final int mapIndex = world.rand.nextInt(innerCoilColor_b.length);
				r = innerCoilColor_r[mapIndex];
				g = innerCoilColor_g[mapIndex];
				b = innerCoilColor_b[mapIndex];
				
				PacketHandler.sendBeamPacketToPlayersInArea(world,
					new Vector3(
							pos.getX() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.getXOffset() + 0.2D * stop .getXOffset(),
							pos.getY() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.getYOffset() + 0.2D * stop .getYOffset(),
							pos.getZ() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * start.getZOffset() + 0.2D * stop .getZOffset()),
					new Vector3(
							pos.getX() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .getXOffset() + 0.2D * start.getXOffset(),
							pos.getY() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .getYOffset() + 0.2D * start.getYOffset(),
							pos.getZ() + 0.5D + (DISTANCE_INNER_COILS_BLOCKS + 0.3D) * stop .getZOffset() + 0.2D * start.getZOffset()),
					r, g, b,
					LASER_DURATION_TICKS,
					aabbArea);
			}
		}
	}
	
	public void disableCloakingField() {
		if (WarpDrive.cloaks.isAreaExists(world, pos)) {
			WarpDrive.cloaks.removeCloakedArea(world.provider.getDimension(), pos);
			
			if (!soundPlayed) {
				soundPlayed = true;
				world.playSound(null, pos, SoundEvents.DECLOAK, SoundCategory.BLOCKS, 4.0F, 1F);
			}
		}
	}
	
	@Override
	public WarpDriveText getStatusHeader() {
		if (world == null) {
			return super.getStatusHeader();
		}
		
		final WarpDriveText textStatus;
		if (!isAssemblyValid) {
			textStatus = textValidityIssues;
		} else if (!isEnabled) {
			textStatus = new WarpDriveText(Commons.getStyleNormal(), "warpdrive.cloaking_core.disabled",
			                               isFullyTransparent ? 2 : 1,
			                               volume);
		} else if (!isCloaking) {
			textStatus = new WarpDriveText(Commons.getStyleWarning(), "warpdrive.cloaking_core.low_power",
			                               isFullyTransparent ? 2 : 1,
			                               volume);
		} else {
			textStatus = new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.cloaking_core.cloaking",
			                               isFullyTransparent ? 2 : 1,
			                               volume);
		}
		return super.getStatusHeader().append(textStatus);
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		final byte[] isValidInnerCoilsAsBytes = tagCompound.getByteArray("isValidInnerCoils");
		final byte[] distanceOuterCoilsAsBytes = tagCompound.getByteArray("distanceOuterCoils");
		if ( isValidInnerCoilsAsBytes.length == 6
		  && distanceOuterCoilsAsBytes.length == 6 ) {
			for (int index = 0; index < 6; index++) {
				isValidInnerCoils[index] = isValidInnerCoilsAsBytes[index] != 0;
				distanceOuterCoils_blocks[index] = distanceOuterCoilsAsBytes[index];
			}
		}
		
		final NBTTagList tagList = tagCompound.getTagList("chunks", NBT.TAG_COMPOUND);
		chunkIndexVolume.clear();
		for (final NBTBase tagBaseEntry : tagList) {
			final NBTTagCompound tagCompoundEntry = (NBTTagCompound) tagBaseEntry;
			final long chunkIndex = tagCompoundEntry.getLong("index");
			final int chunkVolume = tagCompoundEntry.getInteger("volume");
			chunkIndexVolume.put(chunkIndex, chunkVolume);
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		final byte[] isValidInnerCoilsAsBytes = new byte[6];
		final byte[] distanceOuterCoilsAsBytes = new byte[6];
		for (int index = 0; index < 6; index++) {
			isValidInnerCoilsAsBytes[index] = isValidInnerCoils[index] ? (byte) 1 : (byte) 0;
			distanceOuterCoilsAsBytes[index] = (byte) distanceOuterCoils_blocks[index];
		}
		tagCompound.setByteArray("isValidInnerCoils", isValidInnerCoilsAsBytes);
		tagCompound.setByteArray("distanceOuterCoils", distanceOuterCoilsAsBytes);
		
		final NBTTagList tagList = new NBTTagList();
		chunkIndexVolume.forEachEntry((chunkIndex, chunkVolume) -> {
			final NBTTagCompound tagCompoundEntry = new NBTTagCompound();
			tagCompoundEntry.setLong("index", chunkIndex);
			tagCompoundEntry.setInteger("volume", chunkVolume);
			tagList.appendTag(tagCompoundEntry);
			return true;
		});
		tagCompound.setTag("chunks", tagList);
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = super.getUpdateTag();
		
		tagCompound.removeTag("chunks");
		
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(@Nonnull final NetworkManager networkManager, @Nonnull final SPacketUpdateTileEntity packet) {
		super.onDataPacket(networkManager, packet);
		
		// no operation
	}
	
	// Common OC/CC methods
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		final double energyRate = energyRequired / (double) updateRate_ticks;
		return new Object[] {
				true,
				EnergyWrapper.convert((long) Math.ceil(energyRate), units) };
	}
	
	// OpenComputers callback methods
	// (none)
	
	// ComputerCraft IPeripheral methods
	// (none)
	
	// TileEntityAbstractEnergy methods
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
}
