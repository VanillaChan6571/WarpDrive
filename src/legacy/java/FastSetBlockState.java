package cr0s.warpdrive;

import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class FastSetBlockState {
	
	// This code is a straight copy from Vanilla net.minecraft.world.World.setBlockState to remove lighting computations
	public static boolean setBlockStateNoLight(@Nonnull final World world, @Nonnull final BlockPos blockPosPassed, @Nonnull final IBlockState blockStateNew, final int flags) {
		assert !world.captureBlockSnapshots;
		if (!Commons.isSafeThread()) {
			throw new ConcurrentModificationException(String.format("setBlockstate %s to %s 0x%x",
			                                                        Commons.format(world, blockPosPassed), blockStateNew, flags));
		}
		
		if (!WarpDriveConfig.G_ENABLE_FAST_SET_BLOCKSTATE) {
			return world.setBlockState(blockPosPassed, blockStateNew, flags);
		}
		
		if (world.isOutsideBuildHeight(blockPosPassed)) {
			return false;
		} else if (!world.isRemote && world.getWorldInfo().getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
			return false;
		} else {
			final Chunk chunk = world.getChunk(blockPosPassed);
			
			/*
			final BlockPos blockPos = blockPosPassed.toImmutable(); // Forge - prevent mutable BlockPos leaks
			BlockSnapshot blockSnapshot = null;
			if (world.captureBlockSnapshots && !world.isRemote) {
				blockSnapshot = BlockSnapshot.getBlockSnapshot(world, blockPos, flags);
				world.capturedBlockSnapshots.add(blockSnapshot);
			}
			final IBlockState blockStateOld = world.getBlockState(blockPos);
			final int lightOld = blockStateOld.getLightValue(world, blockPos);
			final int opacityOld = blockStateOld.getLightOpacity(world, blockPos);
			/**/
			final BlockPos blockPos = blockPosPassed instanceof MutableBlockPos ? blockPosPassed.toImmutable() : blockPosPassed; // Forge - prevent mutable BlockPos leaks
			
			
			// final IBlockState blockStateEffective = chunk.setBlockState(blockPos, blockStateNew);
			final IBlockState blockStateEffective = chunk_setBlockState(chunk, blockPos, blockStateNew);
			
			if (blockStateEffective == null) {
				/*
				if (blockSnapshot != null) {
					world.capturedBlockSnapshots.remove(blockSnapshot);
				}
				/**/
				return false;
			} else {
				/*
				if ( blockStateNew.getLightOpacity(world, blockPos) != opacityOld
				  || blockStateNew.getLightValue(world, blockPos) != lightOld ) {
					world.profiler.startSection("checkLight");
					world.checkLight(blockPos);
					world.profiler.endSection();
				}
				if (blockSnapshot == null) {// Don't notify clients or update physics while capturing blockstates
					world.markAndNotifyBlock(blockPos, chunk, blockStateEffective, blockStateNew, flags);
				}
				/**/
				return true;
			}
		}
	}
	
	// This code is a straight copy from Vanilla net.minecraft.world.chunk.Chunk.setBlockState to remove lighting computations
	@Nullable
	public static IBlockState chunk_setBlockState(@Nonnull final Chunk chunk, final BlockPos pos, final IBlockState state)
	{
		// report properties as locals
		final World world = chunk.getWorld();
		final ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
		
		final int i = pos.getX() & 15;
		final int j = pos.getY();
		final int k = pos.getZ() & 15;
		// final int l = k << 4 | i;
		
		/* FIXME
		if (j >= chunk.precipitationHeightMap[l] - 1)
		{
			chunk.precipitationHeightMap[l] = -999;
		}
		/**/
		
		// final int i1 = chunk.heightMap[l];
		final IBlockState iblockstate = chunk.getBlockState(pos);
		
		if (iblockstate == state)
		{
			return null;
		}
		else
		{
			final Block block = state.getBlock();
			final Block block1 = iblockstate.getBlock();
			// final int k1 = iblockstate.getLightOpacity(world, pos); // Relocate old light value lookup here, so that it is called before TE is removed.
			ExtendedBlockStorage extendedblockstorage = storageArrays[j >> 4];
			// boolean flag = false;
			
			if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE)
			{
				if (block == Blocks.AIR)
				{
					return null;
				}
				
				extendedblockstorage = new ExtendedBlockStorage(j >> 4 << 4, chunk.getWorld().provider.hasSkyLight());
				storageArrays[j >> 4] = extendedblockstorage;
				// flag = j >= i1;
			}
			
			extendedblockstorage.set(i, j & 15, k, state);
			
			//if (block1 != block)
			{
				if (!world.isRemote)
				{
					if (block1 != block) //Only fire block breaks when the block changes.
						block1.breakBlock(world, pos, iblockstate);
					final TileEntity te = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
					if (te != null && te.shouldRefresh(world, pos, iblockstate, state)) world.removeTileEntity(pos);
				}
				else if (block1.hasTileEntity(iblockstate))
				{
					final TileEntity te = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
					if (te != null && te.shouldRefresh(world, pos, iblockstate, state))
						world.removeTileEntity(pos);
				}
			}
			
			if (extendedblockstorage.get(i, j & 15, k).getBlock() != block)
			{
				return null;
			}
			else
			{
				/*
				if (flag)
				{
					chunk.generateSkylightMap();
				}
				else
				{
					final int j1 = state.getLightOpacity(world, pos);
					
					if (j1 > 0)
					{
						if (j >= i1)
						{
							chunk.relightBlock(i, j + 1, k);
						}
					}
					else if (j == i1 - 1)
					{
						chunk.relightBlock(i, j, k);
					}
					
					if (j1 != k1 && (j1 < k1 || chunk.getLightFor(EnumSkyBlock.SKY, pos) > 0 || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) > 0))
					{
						chunk.propagateSkylightOcclusion(i, k);
					}
				}
				/**/
				
				// If capturing blocks, only run block physics for TE's. Non-TE's are handled in ForgeHooks.onPlaceItemIntoWorld
				if (!world.isRemote && block1 != block && (!world.captureBlockSnapshots || block.hasTileEntity(state)))
				{
					block.onBlockAdded(world, pos, state);
				}
				
				if (block.hasTileEntity(state))
				{
					TileEntity tileentity1 = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
					
					if (tileentity1 == null)
					{
						tileentity1 = block.createTileEntity(world, state);
						world.setTileEntity(pos, tileentity1);
					}
					
					if (tileentity1 != null)
					{
						tileentity1.updateContainingBlockInfo();
					}
				}
				
				chunk.markDirty();
				return iblockstate;
			}
		}
	}
}
