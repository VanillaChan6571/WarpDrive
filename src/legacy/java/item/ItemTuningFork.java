package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.IWarpTool;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.SoundEvents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTuningFork extends ItemAbstractBase implements IWarpTool {
	
	public ItemTuningFork(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setMaxDamage(0);
		setMaxStackSize(1);
		setTranslationKey("warpdrive.tool.tuning_fork");
		setFull3D();
		setHasSubtypes(true);
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		for (int dyeColor = 0; dyeColor < 16; dyeColor++) {
			list.add(new ItemStack(this, 1, dyeColor));
		}
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		if (damage >= 0 && damage < 16) {
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + EnumDyeColor.byDyeDamage(damage).getName());
		}
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		if (damage >= 0 && damage < 16) {
			return getTranslationKey() + "." + EnumDyeColor.byDyeDamage(damage).getTranslationKey();
		}
		return getTranslationKey();
	}
	
	public static int getVideoChannel(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningFork)) {
			return -1;
		}
		return (itemStack.getItemDamage() % 16) + 100;
	}
	
	public static int getBeamFrequency(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningFork)) {
			return -1;
		}
		return ((itemStack.getItemDamage() % 16) + 1) * 10;
	}
	
	public static int getControlChannel(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningFork)) {
			return -1;
		}
		return ((itemStack.getItemDamage() % 16) + 2);
	}
	
	@Nonnull
	@Override
	public EnumActionResult onItemUse(@Nonnull final EntityPlayer entityPlayer,
	                                  @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumHand hand,
	                                  @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return EnumActionResult.FAIL;
		}
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(hand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity == null) {
			return EnumActionResult.FAIL;
		}
		
		final boolean hasVideoChannel = tileEntity instanceof IVideoChannel;
		final boolean hasBeamFrequency = tileEntity instanceof IBeamFrequency;
		final boolean hasControlChannel = tileEntity instanceof IControlChannel;
		if (!hasVideoChannel && !hasBeamFrequency && !hasControlChannel) {
			return EnumActionResult.FAIL;
		}
		if (hasVideoChannel && !(entityPlayer.isSneaking() && hasBeamFrequency)) {
			((IVideoChannel)tileEntity).setVideoChannel(getVideoChannel(itemStackHeld));
			Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.video_channel.set",
					tileEntity.getBlockType().getLocalizedName(),
					getVideoChannel(itemStackHeld)));
			world.playSound(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, SoundEvents.DING, SoundCategory.PLAYERS, 0.1F, 1F, false);
			
		} else if (hasControlChannel && !(entityPlayer.isSneaking() && hasBeamFrequency)) {
			((IControlChannel)tileEntity).setControlChannel(getControlChannel(itemStackHeld));
			Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.control_channel.set",
				tileEntity.getBlockType().getLocalizedName(),
				getControlChannel(itemStackHeld)));
			world.playSound(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, SoundEvents.DING, SoundCategory.PLAYERS, 0.1F, 1F, false);
			
		} else {
			// assert hasBeamFrequency;
			((IBeamFrequency)tileEntity).setBeamFrequency(getBeamFrequency(itemStackHeld));
			Commons.addChatMessage(entityPlayer, new TextComponentTranslation("warpdrive.beam_frequency.set",
					tileEntity.getBlockType().getLocalizedName(),
					getBeamFrequency(itemStackHeld)));
			world.playSound(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, SoundEvents.DING, SoundCategory.PLAYERS, 0.1F, 1F, false);
		}
		return EnumActionResult.SUCCESS;
	}
	
	@Override
	public boolean doesSneakBypassUse(@Nonnull final ItemStack itemStack,
	                                  @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos,
	                                  @Nonnull final EntityPlayer player) {
		final Block block = blockAccess.getBlockState(blockPos).getBlock();
		return block instanceof BlockCapacitor || super.doesSneakBypassUse(itemStack, blockAccess, blockPos, player);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		final WarpDriveText textTooltip = new WarpDriveText();
		textTooltip.append(null, "warpdrive.video_channel.tooltip",
		               new WarpDriveText(Commons.getStyleValue(), getVideoChannel(itemStack)) );
		textTooltip.append(null, "warpdrive.beam_frequency.tooltip",
		               new WarpDriveText(Commons.getStyleValue(), getBeamFrequency(itemStack)) );
		textTooltip.append(null, "warpdrive.control_channel.tooltip",
		               new WarpDriveText(Commons.getStyleValue(), getControlChannel(itemStack)) );
		textTooltip.appendLineBreak();
		textTooltip.append(null, "item.warpdrive.tool.tuning_fork.tooltip.usage");
		
		Commons.addTooltip(list, textTooltip.getFormattedText());
	}
}
