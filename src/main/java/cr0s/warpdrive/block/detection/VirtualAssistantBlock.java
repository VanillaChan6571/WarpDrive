package cr0s.warpdrive.block.detection;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Energy-backed chat command receiver. */
public class VirtualAssistantBlock extends Block {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private final VirtualAssistantTier tier;

	public VirtualAssistantBlock(final VirtualAssistantTier tier) {
		super(AbstractBlock.Properties.of(Material.METAL)
			.strength(3.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
		this.tier = tier;
		registerDefaultState(getStateDefinition().any().setValue(ACTIVE, false));
	}

	public VirtualAssistantTier getTier() { return tier; }

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override public boolean hasTileEntity(final BlockState blockState) { return true; }

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState,
	                                   @Nonnull final IBlockReader world) {
		return new VirtualAssistantTileEntity();
	}

	@Override
	public void setPlacedBy(final World world, final BlockPos blockPos, final BlockState blockState,
	                       @Nullable final LivingEntity placer, final ItemStack itemStack) {
		super.setPlacedBy(world, blockPos, blockState, placer, itemStack);
		if (!itemStack.hasCustomHoverName()) return;
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof VirtualAssistantTileEntity) {
			((VirtualAssistantTileEntity) tileEntity).setAssistantName(itemStack.getHoverName().getString());
		}
	}
}
