package cr0s.warpdrive.block.collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CollectorTuningTest {

	@Test
	public void miningRadiusAndEnergyRetainLegacyScaling() {
		assertEquals(4, CollectorTuning.miningRadius(0.0D));
		assertEquals(5, CollectorTuning.miningRadius(1.0D));
		assertEquals(8, CollectorTuning.miningRadius(4.5D));
		assertEquals(2_500, CollectorTuning.miningEnergy(true, true, false));
		assertEquals(37_500, CollectorTuning.miningEnergy(true, false, false));
		assertEquals(56_250, CollectorTuning.miningEnergy(true, false, true));
		assertEquals(2_250, CollectorTuning.miningEnergy(false, true, true));
	}

	@Test
	public void treeFarmAreaRetainsLegacyScaling() {
		assertEquals(3, CollectorTuning.treeFarmRadius(0.0D));
		assertEquals(5, CollectorTuning.treeFarmRadius(1.0D));
		assertEquals(12, CollectorTuning.treeFarmRadius(4.5D));
		assertEquals(8, CollectorTuning.treeFarmDistance(0.0D));
		assertEquals(35, CollectorTuning.treeFarmDistance(4.5D));
		assertEquals(49, CollectorTuning.treeFarmScanEnergy(3, 3));
		assertEquals(143, CollectorTuning.treeFarmScanEnergy(5, 6));
	}
}
