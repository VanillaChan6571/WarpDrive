package cr0s.warpdrive.block.collection;

/** Pure legacy scaling formulas kept separate so collector tuning can be regression-tested. */
final class CollectorTuning {

	private CollectorTuning() { }

	static int miningRadius(final double mediumFactor) {
		return 4 + (int) Math.floor(mediumFactor);
	}

	static int miningEnergy(final boolean atmosphere, final boolean mineAllBlocks,
	                        final boolean silkTouch) {
		double energy = atmosphere ? 2_500.0D : 1_500.0D;
		if (!mineAllBlocks) energy *= 15.0D;
		if (silkTouch) energy *= 1.5D;
		return (int) Math.round(energy);
	}

	static int treeFarmRadius(final double mediumFactor) {
		return 3 + (int) Math.floor(mediumFactor * 2.0D);
	}

	static int treeFarmDistance(final double mediumFactor) {
		return 8 + (int) Math.floor(mediumFactor * 6.0D);
	}

	static int treeFarmScanEnergy(final int radiusX, final int radiusZ) {
		return (1 + 2 * radiusX) * (1 + 2 * radiusZ);
	}
}
