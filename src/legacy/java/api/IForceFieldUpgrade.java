package cr0s.warpdrive.api;

public interface IForceFieldUpgrade {
	// Get the effect calculation object for this upgrade category
	IForceFieldUpgradeEffector getUpgradeEffector(final Object container);
	
	// Bonus provided to this upgrade category (can be positive or negative)
	float getUpgradeValue(final Object container);
}
