package cr0s.warpdrive.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
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
}
