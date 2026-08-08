package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;

/** One optional network upgrade mounted on a passive relay. */
public class ForceFieldRelayTileEntity extends AbstractForceFieldTileEntity {

	private static final String TAG_UPGRADE = "upgrade";
	private ForceFieldUpgrade upgrade = ForceFieldUpgrade.NONE;

	public ForceFieldRelayTileEntity() {
		super(Registration.FORCE_FIELD_RELAY_TILE.get());
	}

	public ForceFieldTier getTier() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof ForceFieldRelayBlock
			? ((ForceFieldRelayBlock) blockState.getBlock()).getTier() : ForceFieldTier.BASIC;
	}

	public ForceFieldUpgrade getUpgrade() {
		return upgrade;
	}

	public void setUpgrade(final ForceFieldUpgrade upgrade) {
		this.upgrade = upgrade == null ? ForceFieldUpgrade.NONE : upgrade;
		setChanged();
		final BlockState blockState = getBlockState();
		if (level != null && blockState.getBlock() instanceof ForceFieldRelayBlock
		 && blockState.getValue(ForceFieldRelayBlock.UPGRADE) != this.upgrade) {
			level.setBlock(getBlockPos(), blockState.setValue(ForceFieldRelayBlock.UPGRADE, this.upgrade), 3);
		}
	}

	public float getUpgradeValue() {
		return isEnabled() && upgrade != ForceFieldUpgrade.NONE
			? upgrade.getBaseValue() * (1.0F + 0.25F * getTier().ordinal()) : 0.0F;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide) return;
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof ForceFieldRelayBlock
		 && blockState.getValue(ForceFieldRelayBlock.ACTIVE) != isConnected()) {
			level.setBlock(getBlockPos(), blockState.setValue(ForceFieldRelayBlock.ACTIVE, isConnected()), 3);
		}
	}

	public Object[] state() {
		return new Object[]{ isConnected() ? "connected" : "not connected", isEnabled(),
			isConnected(), upgrade.getSerializedName() };
	}

	@Override
	public int getMaxEnergyStored() {
		return 0;
	}

	@Override
	protected int getMaxReceive() {
		return 0;
	}

	@Override
	protected int getMaxExtract() {
		return 0;
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		upgrade = ForceFieldUpgrade.fromRegistrySuffix(tagCompound.getString(TAG_UPGRADE));
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putString(TAG_UPGRADE, upgrade.getSerializedName());
		return tagCompound;
	}
}
