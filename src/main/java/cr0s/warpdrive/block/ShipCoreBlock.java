package cr0s.warpdrive.block;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;

/**
 * Ship Core Block - The main control block for WarpDrive ships
 *
 * Features:
 * - Acts as the center point for ship structure scanning
 * - Stores energy for warp jumps
 * - Interfaces with ComputerCraft for control
 * - Must be present on every ship
 */
public class ShipCoreBlock extends Block {

	public ShipCoreBlock() {
		super(Properties.of(Material.METAL)
				.strength(5.0f, 6.0f)
				.requiresCorrectToolForDrops()
				.harvestTool(ToolType.PICKAXE)
				.harvestLevel(2)
		);
		// Registry name is set automatically by DeferredRegister
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new ShipCoreTileEntity();
	}

	@Override
	public ActionResultType use(BlockState state, World world, BlockPos pos,
	                             PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		// Only process main hand to prevent double-toggling
		if (player.isShiftKeyDown() && hand == Hand.MAIN_HAND) {
			TileEntity te = world.getBlockEntity(pos);
			if (te instanceof ShipCoreTileEntity) {
				ShipCoreTileEntity shipCore = (ShipCoreTileEntity) te;
				shipCore.toggleBoundingBoxDisplay(player);
				return ActionResultType.SUCCESS;
			}
		}
		return ActionResultType.PASS;
	}
}
