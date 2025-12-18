package cr0s.warpdrive.world;

import cr0s.warpdrive.FastSetBlockState;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.structures.OrbInstance;
import cr0s.warpdrive.data.JumpBlock;

import javax.annotation.Nonnull;
import java.util.ArrayList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

/*
 2014-06-07 21:41:45 [Infos] [STDOUT] Generating star (class 0) at -579 257 1162
 2014-06-07 21:41:45 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -579, 257, 1162
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saved 310248 blocks
 2014-06-07 21:41:45 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 95.646ms, total: 95.646ms
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:41:45 [Infos] [STDOUT] [ESG] Saved 23706 blocks
 2014-06-07 21:41:45 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 15.427ms, total: 15.427ms

 2014-06-07 21:42:03 [Infos] [STDOUT] Generating star (class 1) at -554 257 1045
 2014-06-07 21:42:03 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -554, 257, 1045
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saved 1099136 blocks
 2014-06-07 21:42:03 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 37.404ms, total: 37.404ms
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:03 [Infos] [STDOUT] [ESG] Saved 50646 blocks
 2014-06-07 21:42:03 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 34.369ms, total: 34.369ms

 2014-06-07 21:42:39 [Infos] [STDOUT] Generating star (class 2) at -404 257 978
 2014-06-07 21:42:39 [Infos] [Minecraft-Client] [CHAT] /generate: generating star at -404, 257, 978
 2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saved 2144432 blocks
 2014-06-07 21:42:39 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 85.523ms, total: 85.523ms
 2014-06-07 21:42:39 [Infos] [STDOUT] [ESG] Saving blocks...
 2014-06-07 21:42:40 [Infos] [STDOUT] [ESG] Saved 76699 blocks
 2014-06-07 21:42:40 [Infos] [STDOUT] [PROF] {EntitySphereGen.saveSphereBlocks} self: 9.286ms, total: 9.286ms

 */
public final class EntitySphereGen extends Entity {
	
	public int xCoord;
	public int yCoord;
	public int zCoord;
	
	private int radius;
	private int gasColor;
	
	private static final int BLOCKS_PER_TICK = 5000;
	
	private static final int STATE_SAVING = 0;
	private static final int STATE_SETUP = 1;
	private static final int STATE_DELETE = 2;
	private static final int STATE_STOP = 3;
	private int state = STATE_DELETE;
	private int ticksDelay = 0;
	
	private int currentIndex = 0;
	private int pregenSize = 0;
	
	private ArrayList<JumpBlock> blocks;
	private ArrayList<Boolean> isSurfaces;
	private OrbInstance orbInstance;
	private boolean replace;
	
	public EntitySphereGen(final World world) {
		super(world);
	}
	
	public EntitySphereGen(final World world, final int x, final int y, final int z,
	                       final OrbInstance orbInstance, final boolean replace) {
		super(world);
		
		this.xCoord = x;
		this.posX = x;
		this.yCoord = y;
		this.posY = y;
		this.zCoord = z;
		this.posZ = z;
		this.orbInstance = orbInstance;
		this.gasColor = world.rand.nextInt(12);
		this.replace = replace;
		
		constructionFinalizer();
	}
	
	public void killEntity() {
		this.state = STATE_STOP;
		final int minY_clamped = Math.max(0, yCoord - radius);
		final int maxY_clamped = Math.min(255, yCoord + radius);
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		for (int x = xCoord - radius; x <= xCoord + radius; x++) {
			for (int z = zCoord - radius; z <= zCoord + radius; z++) {
				for (int y = minY_clamped; y <= maxY_clamped; y++) {
					mutableBlockPos.setPos(x, y, z);
					final IBlockState blockState = world.getBlockState(mutableBlockPos);
					if (blockState.getBlock() != Blocks.AIR) {
						world.notifyBlockUpdate(mutableBlockPos, blockState, blockState, 3);
					}
				}
			}
		}
		world.removeEntity(this);
	}
	
	@Override
	public void onUpdate() {
		if (world.isRemote) {
			return;
		}
		 
		if (ticksDelay > 0) {
			ticksDelay--;
			return;
		}
		
		switch (state) {
		case STATE_SAVING:
			tickScheduleBlocks();
			this.state = STATE_SETUP;
			break;
		
		case STATE_SETUP:
			if (currentIndex >= blocks.size() - 1)
				this.state = STATE_DELETE;
			else
				tickPlaceBlocks();
			break;
		
		case STATE_DELETE:
			currentIndex = 0;
			killEntity();
			break;
		
		default:
			WarpDrive.logger.error(String.format("%s Invalid state %s. Killing entity...",
			                                     this, state));
			killEntity();
			break;
		}
	}
	
	private void tickPlaceBlocks() {
		final int blocksToMove = Math.min(BLOCKS_PER_TICK, blocks.size() - currentIndex);
		LocalProfiler.start("[EntitySphereGen] Placing blocks from " + currentIndex + " to " + (currentIndex + blocksToMove) + "/" + blocks.size());
		
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		for (int index = 0; index < blocksToMove; index++) {
			if (currentIndex >= blocks.size())
				break;
			final JumpBlock jumpBlock = blocks.get(currentIndex);
			mutableBlockPos.setPos(jumpBlock.x, jumpBlock.y, jumpBlock.z);
			if (isSurfaces.get(currentIndex) && jumpBlock.x % 4 == 0 && jumpBlock.z % 4 == 0) {
				world.setBlockState(mutableBlockPos, jumpBlock.block.getStateFromMeta(jumpBlock.blockMeta), 2);
			} else {
				FastSetBlockState.setBlockStateNoLight(world, mutableBlockPos, jumpBlock.block.getStateFromMeta(jumpBlock.blockMeta), 2);
			}
			currentIndex++;
		}
		
		LocalProfiler.stop();
	}
	
	private void tickScheduleBlocks() {
		LocalProfiler.start("[EntitySphereGen] Saving blocks, radius " + radius);
		
		// square radius from center of block
		final double sqRadiusHigh = (radius + 0.5D) * (radius + 0.5D);
		final double sqRadiusLow = (radius - 0.5D) * (radius - 0.5D);
		
		// sphere
		final int ceilRadius = radius + 1;
		
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		for (int x = 0; x <= ceilRadius; x++) {
			final double x2 = (x + 0.5D) * (x + 0.5D);
			for (int y = 0; y <= ceilRadius; y++) {
				final double x2y2 = x2 + (y + 0.5D) * (y + 0.5D);
				for (int z = 0; z <= ceilRadius; z++) {
					final double dSqRange = x2y2 + (z + 0.5D) * (z + 0.5D); // Square distance from current position to center
					
					// Skip too far blocks
					if (dSqRange > sqRadiusHigh) {
						continue;
					}
					final boolean isSurface = dSqRange > sqRadiusLow;
					
					// Add blocks to memory
					final int intSqRadius = (int) Math.round(dSqRange);
					final GenericSet<Filler> orbShell = orbInstance.getFillerSetFromSquareRange(intSqRadius);
					
					// WarpDrive.logger.info(String.format("dSqRange %.3f sqRadiusHigh %.3f %.3f",
					//                                     dSqRange, sqRadiusHigh, sqRadiusLow));
					// note: placing block is faster from bottom to top due to skylight computations
					addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord - y, zCoord + z));
					if (x != 0) {
						addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord - y, zCoord + z));
					}
					if (y != 0) {
						addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord + y, zCoord + z));
						if (x != 0) {
							addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord + y, zCoord + z));
						}
					}
					if (z != 0) {
						addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord - y, zCoord - z));
						if (x != 0) {
							addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord - y, zCoord - z));
						}
						if (y != 0) {
							addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord + x, yCoord + y, zCoord - z));
							if (x != 0) {
								addBlock(isSurface, new JumpBlock(orbShell.getRandomUnit(rand), xCoord - x, yCoord + y, zCoord - z));
							}
						}
					}
				}
			}
		}
		if (blocks != null && blocks.size() > pregenSize) {
			WarpDrive.logger.warn(String.format("[EntitySphereGen] Saved %s blocks (estimated to %d)",
			                                    blocks.size(), pregenSize));
		}
		LocalProfiler.stop();
	}
	
	private void addBlock(final boolean isSurface, final JumpBlock jumpBlock) {
		if (blocks == null) {
			return;
		}
		// Replace water with random gas (ship in moon)
		if (world.getBlockState(new BlockPos(jumpBlock.x, jumpBlock.y, jumpBlock.z)).getBlock().isAssociatedBlock(Blocks.WATER)) {
			if (world.rand.nextInt(50) != 1) {
				jumpBlock.block = WarpDrive.blockGas;
				jumpBlock.blockMeta = gasColor;
			}
			blocks.add(jumpBlock);
			isSurfaces.add(isSurface);
			return;
		}
		// Do not replace existing blocks if fillingSphere is true
		if (!replace && !world.isAirBlock(new BlockPos(jumpBlock.x, jumpBlock.y, jumpBlock.z))) {
			return;
		}
		blocks.add(jumpBlock);
		isSurfaces.add(isSurface);
	}
	
	@Override
	protected void entityInit() {
		noClip = true;
	}
	
	private void constructionFinalizer() {
		radius = orbInstance.getTotalThickness();
		pregenSize = (int) Math.ceil(Math.PI * 4.0F / 3.0F * Math.pow(radius + 1, 3));
		blocks = new ArrayList<>(this.pregenSize);
		isSurfaces = new ArrayList<>(this.pregenSize);
		
		state = STATE_SAVING;
		ticksDelay = world.rand.nextInt(60);
	}
	
	@Override
	public void readEntityFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		xCoord = tagCompound.getInteger("warpdrive:xCoord");
		yCoord = tagCompound.getInteger("warpdrive:yCoord");
		zCoord = tagCompound.getInteger("warpdrive:zCoord");
		orbInstance = new OrbInstance(tagCompound.getCompoundTag("warpdrive:orbInstance"));
		gasColor = tagCompound.getInteger("warpdrive:gasColor");
		replace = tagCompound.getBoolean("warpdrive:replace");
		
		constructionFinalizer();
		WarpDrive.logger.info(String.format("%s Reloaded from NBT",
		                                    this));
	}
	
	@Override
	public void writeEntityToNBT(final NBTTagCompound tagCompound) {
		tagCompound.setInteger("warpdrive:xCoord", xCoord);
		tagCompound.setInteger("warpdrive:yCoord", yCoord);
		tagCompound.setInteger("warpdrive:zCoord", zCoord);
		tagCompound.setTag("warpdrive:orbInstance", orbInstance.writeToNBT(new NBTTagCompound()));
		tagCompound.setInteger("warpdrive:gasColor", gasColor);
		tagCompound.setBoolean("warpdrive:replace", replace);
	}
	
	// override to skip the block bounding override on client side
	@Override
	public void setPositionAndRotation(final double x, final double y, final double z, final float yaw, final float pitch) {
		//	super.setPositionAndRotation(x, y, z, yaw, pitch);
		this.setPosition(x, y, z);
		this.setRotation(yaw, pitch);
	}
	
	@Override
	public boolean shouldRenderInPass(final int pass) {
		return false;
	}
}