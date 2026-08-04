package cr0s.warpdrive.block.breathing;

/**
 * Breathable air that has spread from a source, ported from 1.12.2 BlockAirFlow.
 *
 * The simulation places these to mark the volume air has actually reached, and removes them as the
 * concentration falls to zero. Concentration itself lives in the chunk's air data rather than in
 * blockstate, so that pressurising a large room does not churn thousands of block updates.
 */
public class AirFlowBlock extends AbstractAirBlock {

	public AirFlowBlock() {
		super();
	}
}
