package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.block.hull.BlockHullSlab.EnumVariant;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockHullSlab extends ItemBlockHull {
	
	private final Block blockSlab;
	
	public ItemBlockHullSlab(final Block blockSlab) {
		super(blockSlab);
		
		this.blockSlab = blockSlab;
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(@Nonnull final ItemStack itemstack) {
		return getTranslationKey();
	}
	
	@Nonnull
	@Override
	public EnumActionResult onItemUse(@Nonnull final EntityPlayer entityPlayer,
	                                  @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumHand hand,
	                                  @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(hand);
		if (itemStackHeld.isEmpty()) {
			return EnumActionResult.FAIL;
		}
		
		// check if clicked block can be interacted with
		@SuppressWarnings("deprecation")
		final IBlockState blockStateItem = blockSlab.getStateFromMeta(itemStackHeld.getItemDamage());
		final int metadataItem = itemStackHeld.getItemDamage();
		final EnumVariant variantItem = blockStateItem.getValue(BlockHullSlab.VARIANT);
		
		final IBlockState blockStateWorld = world.getBlockState(blockPos);
		final EnumVariant variantWorld = blockStateWorld.getBlock() == blockSlab ? blockStateWorld.getValue(BlockHullSlab.VARIANT) : EnumVariant.PLAIN_FULL;
		
		if ( blockStateWorld.getBlock() == blockSlab
		  && !variantItem.getIsDouble()
		  && !variantWorld.getIsDouble()
		  && variantWorld.getIsPlain() == variantItem.getIsPlain() ) {
			if (!entityPlayer.canPlayerEdit(blockPos, facing, itemStackHeld)) {
				return EnumActionResult.FAIL;
			}
			
			// try to merge slabs when right-clicking directly the inner face
			if (variantWorld.getFacing() == facing.getOpposite()) {
				final AxisAlignedBB boundingBox = blockStateWorld.getCollisionBoundingBox(world, blockPos);
				if (boundingBox != null && world.checkNoEntityCollision(boundingBox)) {
					final EnumVariant variantNew;
					if (variantWorld.getIsPlain()) {// plain
						variantNew = EnumVariant.PLAIN_FULL;
					} else {
						switch (facing) {
						default:
						case DOWN:
						case UP:
							variantNew = EnumVariant.TILED_FULL_Y;
							break;
						case NORTH:
						case SOUTH:
							variantNew = EnumVariant.TILED_FULL_Z;
							break;
						case WEST:
						case EAST:
							variantNew = EnumVariant.TILED_FULL_X;
							break;
						}
					}
					world.setBlockState(blockPos, blockSlab.getDefaultState().withProperty(BlockHullSlab.VARIANT, variantNew), 3);
					
					final SoundType soundtype = blockSlab.getSoundType(blockStateWorld, world, blockPos, entityPlayer);
					world.playSound(entityPlayer, blockPos, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
					                (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
					itemStackHeld.shrink(1);
				}
				
				return EnumActionResult.SUCCESS;
			}
			
		} else {
			// check is closer block can be interacted with
			final BlockPos blockPosSide = blockPos.offset(facing);
			final IBlockState blockStateSide = world.getBlockState(blockPosSide);
			final EnumVariant variantSide = blockStateSide.getBlock() == blockSlab ? blockStateSide.getValue(BlockHullSlab.VARIANT) : EnumVariant.PLAIN_FULL;
			
			if ( blockStateSide.getBlock() == blockSlab
			  && !variantItem.getIsDouble()
			  && !variantSide.getIsDouble()
			  && variantSide.getIsPlain() == variantItem.getIsPlain() ) {
				if (!entityPlayer.canPlayerEdit(blockPosSide, facing, itemStackHeld)) {
					return EnumActionResult.FAIL;
				}
				
				// try to place ignoring the existing block
				final IBlockState blockStatePlaced = blockSlab.getStateForPlacement(world, blockPosSide, facing, hitX, hitY, hitZ, metadataItem, entityPlayer, hand);
				final EnumFacing enumFacingPlaced = blockStatePlaced.getValue(BlockHullSlab.VARIANT).getFacing().getOpposite();
				
				// try to merge slabs when right-clicking on a side block
				if (enumFacingPlaced == blockStateSide.getValue(BlockHullSlab.VARIANT).getFacing()) {
					final AxisAlignedBB boundingBox = blockStateWorld.getCollisionBoundingBox(world, blockPosSide);
					if (boundingBox != null && world.checkNoEntityCollision(boundingBox)) {
						final EnumVariant variantNew;
						if (variantSide.getIsPlain()) {// plain
							variantNew = EnumVariant.PLAIN_FULL;
						} else {
							switch (enumFacingPlaced) {
							default:
							case DOWN:
							case UP:
								variantNew = EnumVariant.TILED_FULL_Y;
								break;
							case NORTH:
							case SOUTH:
								variantNew = EnumVariant.TILED_FULL_Z;
								break;
							case WEST:
							case EAST:
								variantNew = EnumVariant.TILED_FULL_X;
								break;
							}
						}
						world.setBlockState(blockPosSide, blockSlab.getDefaultState().withProperty(BlockHullSlab.VARIANT, variantNew), 3);
						
						final SoundType soundtype = blockSlab.getSoundType(blockStateWorld, world, blockPosSide, entityPlayer);
						world.playSound(entityPlayer, blockPosSide, soundtype.getPlaceSound(), SoundCategory.BLOCKS,
						                (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
						itemStackHeld.shrink(1);
					}
					
					return EnumActionResult.SUCCESS;
				}
				
			}
		}
		
		return super.onItemUse(entityPlayer, world, blockPos, hand, facing, hitX, hitY, hitZ);
	}
	
	@Override
	public boolean canPlaceBlockOnSide(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                   @Nonnull final EntityPlayer entityPlayer, @Nonnull final ItemStack itemStack) {
		// check if clicked block can be interacted with
		@SuppressWarnings("deprecation")
		final IBlockState blockStateItem = blockSlab.getStateFromMeta(itemStack.getItemDamage());
		final EnumVariant variantItem = blockStateItem.getValue(BlockHullSlab.VARIANT);
		
		final IBlockState blockStateWorld = world.getBlockState(blockPos);
		final EnumVariant variantWorld = blockStateWorld.getBlock() == blockSlab ? blockStateWorld.getValue(BlockHullSlab.VARIANT) : EnumVariant.PLAIN_FULL;
		
		if ( blockStateWorld.getBlock() == blockSlab
		  && !variantItem.getIsDouble()
		  && !variantWorld.getIsDouble()
		  && variantWorld.getIsPlain() == variantItem.getIsPlain() ) {
			return true;
		}
		
		// check the block on our side
		final BlockPos blockPosSide = blockPos.offset(facing);
		final IBlockState blockStateSide = world.getBlockState(blockPosSide);
		final EnumVariant variantSide = blockStateSide.getBlock() == blockSlab ? blockStateSide.getValue(BlockHullSlab.VARIANT) : EnumVariant.PLAIN_FULL;
		if ( blockStateSide.getBlock() == blockSlab
		  && variantSide.getIsPlain() == variantItem.getIsPlain() ) {
			return true;
		}
		
		// default behavior
		return super.canPlaceBlockOnSide(world, blockPos, facing, entityPlayer, itemStack);
	}
}
