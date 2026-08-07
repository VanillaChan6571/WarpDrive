package cr0s.warpdrive.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.GlassBlock;

/** Glass equivalent of {@link SimpleBlock}, retaining vanilla same-glass face culling. */
public class SimpleGlassBlock extends GlassBlock {

	public SimpleGlassBlock(final AbstractBlock.Properties properties) {
		this(properties, true);
	}

	public SimpleGlassBlock(final AbstractBlock.Properties properties, final boolean dropsSelf) {
		super(dropsSelf ? properties : properties.noDrops());
	}
}
