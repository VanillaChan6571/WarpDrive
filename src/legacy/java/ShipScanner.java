package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.computer.ISecurityStation;
import cr0s.warpdrive.block.BlockSecurityStation;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;

class ShipScanner {
	
	// inputs
	private final IBlockAccess blockAccess;
	private final BlockPos blockPosCore;
	private final int minX, minY, minZ;
	private final int maxX, maxY, maxZ;
	
	// execution
	private int x;
	private int y;
	private int z;
	private final MutableBlockPos mutableBlockPos;
	
	// output
	public int mass = 0;
	public int volume = 0;
	public BlockPos posSecurityStation = null;
	
	ShipScanner(final IBlockAccess blockAccess,
	            final BlockPos blockPosCore,
	            final int minX, final int minY, final int minZ,
	            final int maxX, final int maxY, final int maxZ) {
		this.blockAccess = blockAccess;
		this.blockPosCore = blockPosCore.toImmutable();
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		x = this.minX;
		y = this.minY;
		z = this.minZ;
		mutableBlockPos = new MutableBlockPos(x, y, z);
	}
	
	boolean tick() {
		int countBlocks = 0;
		
		try {
			while (countBlocks < WarpDriveConfig.SHIP_VOLUME_SCAN_BLOCKS_PER_TICK) {
				mutableBlockPos.setPos(x, y, z);
				final Block block = blockAccess.getBlockState(mutableBlockPos).getBlock();
				countBlocks++;
				
				// skipping vanilla air & ignored blocks
				if (block != Blocks.AIR && !Dictionary.BLOCKS_LEFTBEHIND.contains(block)) {
					volume++;
					
					if (!Dictionary.BLOCKS_NOMASS.contains(block)) {
						mass++;
						
						// keep the security station closest to the ship core
						if ( block instanceof BlockSecurityStation
						  && ( posSecurityStation == null
						    || blockPosCore == null
						    || blockPosCore.distanceSq(posSecurityStation) > blockPosCore.distanceSq(mutableBlockPos) ) ) {
							// keep only enabled security stations
							final TileEntity tileEntity = blockAccess.getTileEntity(mutableBlockPos);
							if ( tileEntity instanceof ISecurityStation
							  && ((ISecurityStation) tileEntity).getIsEnabled() ) {
								posSecurityStation = mutableBlockPos.toImmutable();
							}
						}
					}
				}
				
				// loop y first to stay in same chunk, then z, then x
				y++;
				if (y > maxY) {
					y = minY;
					z++;
					if (z > maxZ) {
						z = minZ;
						x++;
						if (x > maxX) {
							return true;
						}
					}
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		return false;
	}
}
