package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;

public class TileEntityForceFieldRelay extends TileEntityAbstractForceField implements IForceFieldUpgrade {

	// persistent properties
	private EnumForceFieldUpgrade upgrade = EnumForceFieldUpgrade.NONE;
	
	public TileEntityForceFieldRelay() {
		super();
		
		peripheralName = "warpdriveForceFieldRelay";
		doRequireUpgradeToInterface();
	}
	
	// onFirstUpdateTick
	// update
	
	protected EnumForceFieldUpgrade getUpgrade() {
		if (upgrade == null) {
			return EnumForceFieldUpgrade.NONE;
		}
		return upgrade;
	}
	
	protected void setUpgrade(final EnumForceFieldUpgrade upgrade) {
		this.upgrade = upgrade;
		markDirty();
	}
	
	@Override
	protected WarpDriveText getUpgradeStatus(final boolean isAnimated) {
		final WarpDriveText warpDriveText = new WarpDriveText(null, "warpdrive.upgrade.status_line.header");
		final EnumForceFieldUpgrade enumForceFieldUpgrade = getUpgrade();
		final String keyName = ItemForceFieldUpgrade.getItemStack(enumForceFieldUpgrade).getTranslationKey() + ".name";
		final int value = enumForceFieldUpgrade == EnumForceFieldUpgrade.NONE ? 0 : 1;
		final Style style = value == 0 ? Commons.getStyleDisabled() : Commons.getStyleCorrect();
		warpDriveText.append(Commons.getStyleDisabled(), "- %1$s/%2$s x %3$s",
		                     new WarpDriveText(Commons.getStyleValue(), "%1$s", value),
		                     1,
		                     new WarpDriveText(style, keyName) );
		return warpDriveText;
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		setUpgrade(EnumForceFieldUpgrade.get(tagCompound.getByte("upgrade")));
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setByte("upgrade", (byte) getUpgrade().ordinal());
		return tagCompound;
	}
	
	@Override
	public Object[] getEnergyRequired() {
		return new Object[] { false, "No energy consumption" };
	}
	
	@Override
	public IForceFieldUpgradeEffector getUpgradeEffector(final Object container) {
		return isEnabled ? getUpgrade() : null;
	}
	
	@Override
	public float getUpgradeValue(final Object container) {
		return isEnabled ? getUpgrade().getUpgradeValue(container) * (1.0F + (enumTier.getIndex() - 1) * ForceFieldSetup.FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_RELAY_TIER) : 0.0F;
	}
}
