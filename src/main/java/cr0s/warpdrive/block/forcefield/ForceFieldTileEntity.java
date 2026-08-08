package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/** Ownership link from a projected field block back to the projector that sustains it. */
public class ForceFieldTileEntity extends TileEntity implements ITickableTileEntity {

	private static final String TAG_PROJECTOR = "projector";
	private static final String TAG_CAMOUFLAGE = "camouflage";
	private BlockPos projectorPos;
	@Nullable private BlockState camouflage;
	private int beamFrequency = -1;
	private int validationTicks = 20;
	private int graceChecks = 3;

	public ForceFieldTileEntity() {
		super(Registration.FORCE_FIELD_TILE.get());
	}

	public void setProjector(final ForceFieldProjectorTileEntity projector,
	                         @Nullable final BlockState camouflage) {
		projectorPos = projector.getBlockPos().immutable();
		beamFrequency = projector.getBeamFrequency();
		this.camouflage = camouflage;
		graceChecks = 3;
		setChanged();
		if (level != null) {
			final BlockState blockState = getBlockState();
			if (blockState.hasProperty(ForceFieldBlock.CAMOUFLAGED)
			 && blockState.getValue(ForceFieldBlock.CAMOUFLAGED) != (camouflage != null)) {
				level.setBlock(getBlockPos(), blockState.setValue(
					ForceFieldBlock.CAMOUFLAGED, camouflage != null), 3);
			} else if (!level.isClientSide) {
				level.sendBlockUpdated(getBlockPos(), blockState, blockState, 3);
			}
		}
	}

	@Nullable
	public BlockState getCamouflage() { return camouflage; }

	public boolean isOwnedBy(final ForceFieldProjectorTileEntity projector) {
		return projectorPos != null && projectorPos.equals(projector.getBlockPos());
	}

	public int getBeamFrequency() {
		return beamFrequency;
	}

	@Nullable
	public ForceFieldProjectorTileEntity getProjector() {
		if (level == null || projectorPos == null || !level.hasChunkAt(projectorPos)) {
			return null;
		}
		final TileEntity tileEntity = level.getBlockEntity(projectorPos);
		if (tileEntity instanceof ForceFieldProjectorTileEntity) {
			final ForceFieldProjectorTileEntity projector = (ForceFieldProjectorTileEntity) tileEntity;
			if (projector.isActive() && projector.isPartOfForceField(getBlockPos())) {
				graceChecks = 3;
				return projector;
			}
		}
		return null;
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide || --validationTicks > 0) {
			return;
		}
		validationTicks = 20;
		if (projectorPos == null) {
			removeAfterGrace();
			return;
		}
		// Do not destroy a valid edge of a field merely because its projector chunk is unloaded.
		if (!level.hasChunkAt(projectorPos)) {
			return;
		}
		if (getProjector() == null) {
			removeAfterGrace();
		}
	}

	private void removeAfterGrace() {
		if (--graceChecks < 0 && level != null) {
			level.removeBlock(getBlockPos(), false);
		}
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		projectorPos = tagCompound.contains(TAG_PROJECTOR)
			? BlockPos.of(tagCompound.getLong(TAG_PROJECTOR)) : null;
		beamFrequency = tagCompound.contains(IBeamFrequency.BEAM_FREQUENCY_TAG)
			? tagCompound.getInt(IBeamFrequency.BEAM_FREQUENCY_TAG) : -1;
		camouflage = tagCompound.contains(TAG_CAMOUFLAGE)
			? NBTUtil.readBlockState(tagCompound.getCompound(TAG_CAMOUFLAGE)) : null;
		graceChecks = 3;
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		if (projectorPos != null) tagCompound.putLong(TAG_PROJECTOR, projectorPos.asLong());
		tagCompound.putInt(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		if (camouflage != null) tagCompound.put(TAG_CAMOUFLAGE, NBTUtil.writeBlockState(camouflage));
		return tagCompound;
	}

	@Override
	public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getBlockPos(), 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager networkManager,
	                         final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}
}
