package cr0s.warpdrive.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Creative energy source block. Right-click opens the rate/face configuration screen.
 */
public class CreativeEnergyBlock extends Block {

	public CreativeEnergyBlock() {
		super(Block.Properties.of(Material.METAL)
			.strength(-1.0F, 3600000.0F)   // creative-only: unbreakable in survival
			.harvestTool(ToolType.PICKAXE)
			.noOcclusion());
	}

	@Override
	public boolean hasTileEntity(final BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
		return new CreativeEnergyTileEntity();
	}

	@Override
	public ActionResultType use(final BlockState state, final World world, final BlockPos pos,
	                            final PlayerEntity player, final Hand hand, final BlockRayTraceResult hit) {
		if (hand != Hand.MAIN_HAND) {
			return ActionResultType.PASS;
		}
		if (world.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		final TileEntity tileEntity = world.getBlockEntity(pos);
		if (tileEntity instanceof CreativeEnergyTileEntity && player instanceof ServerPlayerEntity) {
			NetworkHooks.openGui((ServerPlayerEntity) player, (CreativeEnergyTileEntity) tileEntity,
				buffer -> buffer.writeBlockPos(pos));
			return ActionResultType.CONSUME;
		}
		return ActionResultType.PASS;
	}
}
