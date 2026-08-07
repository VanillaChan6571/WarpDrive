package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.block.AbstractOmnipanelBlock;
import net.minecraft.block.AbstractBlock;

/**
 * Structural form of the connected omnipanel geometry.
 *
 * Machine behavior is intentionally absent during the catalog port; this class only supplies the
 * faithful panel shape and connectivity shared with the Energy Air Shield.
 */
public final class HullOmnipanelBlock extends AbstractOmnipanelBlock {

	public HullOmnipanelBlock(final AbstractBlock.Properties properties) {
		super(properties.noOcclusion());
	}
}
