package cr0s.warpdrive.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

/**
 * A plain WarpDrive construction block. Drops are supplied by ordinary 1.16 loot tables.
 */
public class SimpleBlock extends Block {

	public SimpleBlock(final AbstractBlock.Properties properties) {
		this(properties, true);
	}

	public SimpleBlock(final AbstractBlock.Properties properties, final boolean dropsSelf) {
		super(dropsSelf ? properties : properties.noDrops());
	}
}
