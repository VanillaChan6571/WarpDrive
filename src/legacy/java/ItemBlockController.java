package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.api.computer.IMultiBlockCoreOrController;
import cr0s.warpdrive.block.ItemBlockAbstractBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemBlockController extends ItemBlockAbstractBase {
	
	public ItemBlockController(final Block block) {
		super(block, false, false);
		
		setMaxStackSize(1);
	}
	
	protected static String getName(@Nonnull final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemBlockController)) {
			return "";
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			return "";
		}
		final String name = tagCompound.getString(ICoreSignature.NAME_TAG);
		final UUID uuid = new UUID(tagCompound.getLong(ICoreSignature.UUID_MOST_TAG), tagCompound.getLong(ICoreSignature.UUID_LEAST_TAG));
		if (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L) {
			return "";
		}
		return name;
	}
	
	@Nullable
	protected static UUID getSignature(@Nonnull final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemBlockController)) {
			return null;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			return null;
		}
		final UUID uuid = new UUID(tagCompound.getLong(ICoreSignature.UUID_MOST_TAG), tagCompound.getLong(ICoreSignature.UUID_LEAST_TAG));
		if (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L) {
			return null;
		}
		return uuid;
	}
	
	protected static ItemStack setNameAndSignature(@Nonnull final ItemStack itemStack, @Nullable final String name, @Nullable final UUID uuid) {
		if (!(itemStack.getItem() instanceof ItemBlockController)) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		if ( name == null
		  || name.isEmpty() ) {
			tagCompound.removeTag(ICoreSignature.NAME_TAG);
		} else {
			tagCompound.setString(ICoreSignature.NAME_TAG, name);
		}
		if (uuid == null || (uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L)) {
			tagCompound.removeTag(ICoreSignature.UUID_MOST_TAG);
			tagCompound.removeTag(ICoreSignature.UUID_LEAST_TAG);
		} else {
			tagCompound.setLong(ICoreSignature.UUID_MOST_TAG, uuid.getMostSignificantBits());
			tagCompound.setLong(ICoreSignature.UUID_LEAST_TAG, uuid.getLeastSignificantBits());
		}
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	@Nonnull
	@Override
	public EnumActionResult onItemUse(@Nonnull final EntityPlayer entityPlayer,
	                                  @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumHand hand,
	                                  @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return EnumActionResult.FAIL;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(hand);
		if (itemStackHeld.isEmpty()) {
			return EnumActionResult.FAIL;
		}
		
		// check if clicked block can be interacted with
		final IBlockState blockState = world.getBlockState(blockPos);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		
		if (!(tileEntity instanceof IMultiBlockCoreOrController)) {
			return super.onItemUse(entityPlayer, world, blockPos, hand, facing, hitX, hitY, hitZ);
		}
		if (!entityPlayer.canPlayerEdit(blockPos, facing, itemStackHeld)) {
			return EnumActionResult.FAIL;
		}
		
		final UUID uuidSignatureFromItem = getSignature(itemStackHeld);
		final String nameSignatureFromItem = getName(itemStackHeld);
		final UUID uuidSignatureFromBlock = ((IMultiBlockCoreOrController) tileEntity).getSignatureUUID();
		final String nameSignatureFromBlock = ((IMultiBlockCoreOrController) tileEntity).getSignatureName();
		final String nameItem = itemStackHeld.getDisplayName();
		final String nameBlock = Commons.format(blockState, world, blockPos);
		if (entityPlayer.isSneaking()) {// get block signature
			if ( uuidSignatureFromBlock == null
			  || nameSignatureFromBlock == null
			  || nameSignatureFromBlock.isEmpty() ) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.core_signature.get_missing",
				                                                       null, nameItem, nameBlock ));
				
			} else if (uuidSignatureFromBlock.equals(uuidSignatureFromItem)) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.core_signature.get_same",
				                                                       nameSignatureFromItem, nameItem, nameBlock ));
				
			} else {
				final ItemStack itemStackNew = setNameAndSignature(itemStackHeld, nameSignatureFromBlock, uuidSignatureFromBlock);
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.core_signature.get",
				                                                       nameSignatureFromBlock, nameItem, nameBlock ));
				world.playSound(entityPlayer.posX + 0.5D, entityPlayer.posY + 0.5D, entityPlayer.posZ + 0.5D,
				                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS,
				                1.0F, 1.8F + 0.2F * world.rand.nextFloat(), false);
			}
			
		} else {// set block signature
			if (uuidSignatureFromItem == null) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.core_signature.set_missing",
				                                                       null, nameItem, nameBlock ));
				
			} else if (uuidSignatureFromItem.equals(uuidSignatureFromBlock)) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.core_signature.set_same",
				                                                       nameSignatureFromItem, nameItem, nameBlock ));
				
			} else {
				final boolean isSuccess = ((IMultiBlockCoreOrController) tileEntity).setSignature(uuidSignatureFromItem, nameSignatureFromItem);
				if (isSuccess) {
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.core_signature.set",
					                                                       nameSignatureFromItem, nameItem, nameBlock));
					world.playSound(entityPlayer.posX + 0.5D, entityPlayer.posY + 0.5D, entityPlayer.posZ + 0.5D,
					                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS,
					                1.0F, 1.2F + 0.2F * world.rand.nextFloat(), false);
				} else {
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.core_signature.set_not_supported",
					                                                       null, nameItem, nameBlock ));
				}
			}
		}
		
		return EnumActionResult.SUCCESS;
	}
}
