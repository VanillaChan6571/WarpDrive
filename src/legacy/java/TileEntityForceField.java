package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.VectorI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileEntityForceField extends TileEntity {
	
	private BlockPos blockPosProjector;
	
	// cache parameters used for rendering, force projector check for others
	private int cache_beamFrequency;
	public IBlockState cache_blockStateCamouflage;
	protected int cache_colorMultiplierCamouflage;
	protected int cache_lightCamouflage;
	
	// number of projectors check ignored before self-destruction
	private int gracePeriod_calls = 3;
	
	public TileEntityForceField() {
		super();
	}
	
	// saved properties
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		if (tagCompound.hasKey("projector")) {// are we server side and is it a valid force field block?
			blockPosProjector = Commons.createBlockPosFromNBT(tagCompound.getCompoundTag("projector"));
			cache_beamFrequency = tagCompound.getInteger(IBeamFrequency.BEAM_FREQUENCY_TAG);
		} else {
			blockPosProjector = null;
			cache_beamFrequency = -1;
		}
		if (tagCompound.hasKey("camouflage")) {
			final NBTTagCompound nbtCamouflage = tagCompound.getCompoundTag("camouflage");
			try {
				cache_blockStateCamouflage = Block.getBlockFromName(nbtCamouflage.getString("block")).getStateFromMeta(nbtCamouflage.getByte("meta"));
				cache_colorMultiplierCamouflage = nbtCamouflage.getInteger("color");
				cache_lightCamouflage = nbtCamouflage.getByte("light");
				if (Dictionary.BLOCKS_NOCAMOUFLAGE.contains(cache_blockStateCamouflage.getBlock())) {
					cache_blockStateCamouflage = null;
					cache_colorMultiplierCamouflage = 0;
					cache_lightCamouflage = 0;
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				cache_blockStateCamouflage = null;
				cache_colorMultiplierCamouflage = 0;
				cache_lightCamouflage = 0;
			}
		} else {
			cache_blockStateCamouflage = null;
			cache_colorMultiplierCamouflage = 0;
			cache_lightCamouflage = 0;
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (blockPosProjector != null) {
			tagCompound.setTag("projector", Commons.writeBlockPosToNBT(blockPosProjector, new NBTTagCompound()));
			tagCompound.setInteger(IBeamFrequency.BEAM_FREQUENCY_TAG, cache_beamFrequency);
			if (cache_blockStateCamouflage != null && cache_blockStateCamouflage.getBlock() != Blocks.AIR) {
				final NBTTagCompound nbtCamouflage = new NBTTagCompound();
				assert cache_blockStateCamouflage.getBlock().getRegistryName() != null;
				nbtCamouflage.setString("block", cache_blockStateCamouflage.getBlock().getRegistryName().toString());
				nbtCamouflage.setByte("meta", (byte) cache_blockStateCamouflage.getBlock().getMetaFromState(cache_blockStateCamouflage));
				nbtCamouflage.setInteger("color", cache_colorMultiplierCamouflage);
				nbtCamouflage.setByte("light", (byte) cache_lightCamouflage);
				tagCompound.setTag("camouflage", nbtCamouflage);
			}
		}
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = writeToNBT(super.getUpdateTag());
		
		tagCompound.removeTag("projector");
		tagCompound.removeTag(IBeamFrequency.BEAM_FREQUENCY_TAG);
		
		return tagCompound;
	}
	
	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, getBlockMetadata(), getUpdateTag());
	}
	
	@Override
	public void onDataPacket(@Nonnull final NetworkManager networkManager, @Nonnull final SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
	}
	
	public void setProjector(final BlockPos blockPos) {
		blockPosProjector = blockPos.toImmutable();
		final ForceFieldSetup forceFieldSetup = getForceFieldSetup();
		if (forceFieldSetup != null) {
			cache_beamFrequency = forceFieldSetup.beamFrequency;
			final IBlockState blockStateCamouflage = forceFieldSetup.getCamouflageBlockState();
			if ( blockStateCamouflage == null
			  || getBlockMetadata() == blockStateCamouflage.getBlock().getMetaFromState(blockStateCamouflage)) {
				cache_blockStateCamouflage = blockStateCamouflage;
				cache_colorMultiplierCamouflage = forceFieldSetup.getCamouflageColorMultiplier();
				cache_lightCamouflage = forceFieldSetup.getCamouflageLight();
			}
		}
		final IBlockState blockState = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, blockState, blockState, 3);
	}
	
	public TileEntityForceFieldProjector getProjector(@Nullable final TileEntityForceFieldProjector tileEntityForceFieldProjectorCandidate) {
		if (blockPosProjector != null) {
			// test candidate to save a call to getTileEntity()
			if ( tileEntityForceFieldProjectorCandidate != null
			  && blockPosProjector.equals(tileEntityForceFieldProjectorCandidate.getPos()) ) {
				return tileEntityForceFieldProjectorCandidate;
			}
			
			final TileEntity tileEntity = world.getTileEntity(blockPosProjector);
			if (tileEntity instanceof TileEntityForceFieldProjector) {
				final TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector) tileEntity;
				if (world.isRemote) {
					return tileEntityForceFieldProjector;
					
				} else if (tileEntityForceFieldProjector.isPartOfForceField(new VectorI(this))) {
					if (tileEntityForceFieldProjector.isActive()) {
						return tileEntityForceFieldProjector;
					} else {
						// projector is disabled or out of power
						world.setBlockToAir(pos);
						if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
							WarpDrive.logger.info(String.format("Removed a force field from an offline projector %s",
							                                    Commons.format(world, pos)));
						}
					}
				}
			}
		}
		
		if (!world.isRemote) {
			gracePeriod_calls--;
			if (gracePeriod_calls < 0) {
				world.setBlockToAir(pos);
				if (WarpDriveConfig.LOGGING_FORCE_FIELD) {
					WarpDrive.logger.info(String.format("Removed a force field with no projector defined %s",
					                                    Commons.format(world, pos)));
				}
			}
		}
		
		return null;
	}
	
	public ForceFieldSetup getForceFieldSetup() {
		final TileEntityForceFieldProjector tileEntityForceFieldProjector = getProjector(null);
		if (tileEntityForceFieldProjector == null) {
			return null;
		}
		return tileEntityForceFieldProjector.getForceFieldSetup();
	}
}
