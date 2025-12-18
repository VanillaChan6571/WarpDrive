package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTuningDriver extends ItemAbstractBase implements IWarpTool {
	
	public static final int MODE_VIDEO_CHANNEL = 0;
	public static final int MODE_BEAM_FREQUENCY = 1;
	public static final int MODE_CONTROL_CHANNEL = 2;
	
	public ItemTuningDriver(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setMaxDamage(0);
		setCreativeTab(WarpDrive.creativeTabMain);
		setMaxStackSize(1);
		setTranslationKey("warpdrive.tool.tuning_driver");
		setFull3D();
		setHasSubtypes(true);
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		for (int metadata = 0; metadata < 3; metadata++) {
			list.add(new ItemStack(this, 1, metadata));
		}
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(@Nonnull final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		switch (damage) {
		case MODE_VIDEO_CHANNEL:
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-video_channel");
			break;
		case MODE_BEAM_FREQUENCY:
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-beam_frequency");
			break;
		case MODE_CONTROL_CHANNEL:
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-control_channel");
			break;
		default:
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-invalid");
			break;
		}
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(@Nonnull final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		switch (damage) {
		case MODE_VIDEO_CHANNEL  : return getTranslationKey() + ".video_channel";
		case MODE_BEAM_FREQUENCY : return getTranslationKey() + ".beam_frequency";
		case MODE_CONTROL_CHANNEL: return getTranslationKey() + ".control_channel";
		default: return getTranslationKey();
		}
	}
	
	public static int getVideoChannel(@Nonnull final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver)) {
			return -1;
		}
		if (!itemStack.hasTagCompound()) {
			return -1;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		assert tagCompound != null;
		if (tagCompound.hasKey(IVideoChannel.VIDEO_CHANNEL_TAG)) {
			return tagCompound.getInteger(IVideoChannel.VIDEO_CHANNEL_TAG);
		}
		return -1;
	}
	
	@Nonnull
	public static ItemStack setVideoChannel(@Nonnull final ItemStack itemStack, final int videoChannel) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver) || videoChannel == -1) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		tagCompound.setInteger(IVideoChannel.VIDEO_CHANNEL_TAG, videoChannel);
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	public static int getBeamFrequency(@Nonnull final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver)) {
			return -1;
		}
		if (!itemStack.hasTagCompound()) {
			return -1;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if ( tagCompound != null
		  && tagCompound.hasKey(IBeamFrequency.BEAM_FREQUENCY_TAG) ) {
			return tagCompound.getInteger(IBeamFrequency.BEAM_FREQUENCY_TAG);
		}
		return -1;
	}
	
	@Nonnull
	public static ItemStack setBeamFrequency(@Nonnull final ItemStack itemStack, final int beamFrequency) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver) || beamFrequency == -1) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		tagCompound.setInteger(IBeamFrequency.BEAM_FREQUENCY_TAG, beamFrequency);
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	public static int getControlChannel(@Nonnull final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver)) {
			return -1;
		}
		if (!itemStack.hasTagCompound()) {
			return -1;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if ( tagCompound != null
		  && tagCompound.hasKey(IControlChannel.CONTROL_CHANNEL_TAG) ) {
			return tagCompound.getInteger(IControlChannel.CONTROL_CHANNEL_TAG);
		}
		return -1;
	}
	
	@Nonnull
	public static ItemStack setControlChannel(@Nonnull final ItemStack itemStack, final int controlChannel) {
		if (!(itemStack.getItem() instanceof ItemTuningDriver) || controlChannel == -1) {
			return itemStack;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			tagCompound = new NBTTagCompound();
		}
		tagCompound.setInteger(IControlChannel.CONTROL_CHANNEL_TAG, controlChannel);
		itemStack.setTagCompound(tagCompound);
		return itemStack;
	}
	
	@Nonnull
	public static ItemStack setValue(@Nonnull final ItemStack itemStack, final int dye) {
		switch (itemStack.getItemDamage()) {
		case MODE_VIDEO_CHANNEL  : return setVideoChannel(itemStack, dye);
		case MODE_BEAM_FREQUENCY : return setBeamFrequency(itemStack, dye);
		case MODE_CONTROL_CHANNEL: return setControlChannel(itemStack, dye);
		default                  : return itemStack;
		}
	}
	
	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(@Nonnull final World world, @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand hand) {
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(hand);
		
		if ( world.isRemote
		  || !(itemStackHeld.getItem() instanceof ItemTuningDriver) ) {
			return new ActionResult<>(EnumActionResult.PASS, itemStackHeld);
		}
		// check if a block is in players reach 
		final RayTraceResult movingObjectPosition = Commons.getInteractingBlock(world, entityPlayer);
		if (movingObjectPosition.typeOfHit != Type.MISS) {
			return new ActionResult<>(EnumActionResult.PASS, itemStackHeld);
		}
		
		if (entityPlayer.isSneaking() && entityPlayer.capabilities.isCreativeMode) {
			switch (itemStackHeld.getItemDamage()) {
			case MODE_VIDEO_CHANNEL:
				setVideoChannel(itemStackHeld, 1 + world.rand.nextInt(IVideoChannel.VIDEO_CHANNEL_MAX));
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.video_channel.get",
					entityPlayer.getName(),
					getVideoChannel(itemStackHeld)));
				return new ActionResult<>(EnumActionResult.SUCCESS, itemStackHeld);
			
			case MODE_BEAM_FREQUENCY:
				setBeamFrequency(itemStackHeld, 1 + world.rand.nextInt(IBeamFrequency.BEAM_FREQUENCY_MAX));
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.beam_frequency.get",
					entityPlayer.getName(),
					getBeamFrequency(itemStackHeld)));
				return new ActionResult<>(EnumActionResult.SUCCESS, itemStackHeld);
			
			case MODE_CONTROL_CHANNEL:
				setControlChannel(itemStackHeld, world.rand.nextInt(IControlChannel.CONTROL_CHANNEL_MAX));
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.control_channel.get",
					entityPlayer.getName(),
					getControlChannel(itemStackHeld)));
				return new ActionResult<>(EnumActionResult.SUCCESS, itemStackHeld);
			
			default:
				return new ActionResult<>(EnumActionResult.PASS, itemStackHeld);
			}
			
		} else {
			switch (itemStackHeld.getItemDamage()) {
			case MODE_VIDEO_CHANNEL:
				itemStackHeld.setItemDamage(MODE_BEAM_FREQUENCY);
				entityPlayer.setHeldItem(hand, itemStackHeld);
				break;
			
			case MODE_BEAM_FREQUENCY:
				itemStackHeld.setItemDamage(MODE_CONTROL_CHANNEL);
				entityPlayer.setHeldItem(hand, itemStackHeld);
				break;
			
			case MODE_CONTROL_CHANNEL:
				itemStackHeld.setItemDamage(MODE_VIDEO_CHANNEL);
				entityPlayer.setHeldItem(hand, itemStackHeld);
				break;
			
			default:
				itemStackHeld.setItemDamage(MODE_VIDEO_CHANNEL);
				break;
			}
			world.playSound(entityPlayer.posX, entityPlayer.posY, entityPlayer.posZ, SoundEvents.DING, SoundCategory.PLAYERS, 0.1F, 1F, false);
			return new ActionResult<>(EnumActionResult.SUCCESS, itemStackHeld);
		}
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
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity == null) {
			return EnumActionResult.FAIL;
		}
		
		switch (itemStackHeld.getItemDamage()) {
		case MODE_VIDEO_CHANNEL:
			if (tileEntity instanceof IVideoChannel) {
				if (entityPlayer.isSneaking()) {
					setVideoChannel(itemStackHeld, ((IVideoChannel) tileEntity).getVideoChannel());
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.video_channel.get",
							tileEntity.getBlockType().getLocalizedName(),
							getVideoChannel(itemStackHeld)));
				} else {
					((IVideoChannel) tileEntity).setVideoChannel(getVideoChannel(itemStackHeld));
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.video_channel.set",
							tileEntity.getBlockType().getLocalizedName(),
							getVideoChannel(itemStackHeld)));
				}
				return EnumActionResult.SUCCESS;
			}
			return EnumActionResult.FAIL;
			
		case MODE_BEAM_FREQUENCY:
			if (tileEntity instanceof IBeamFrequency) {
				if (entityPlayer.isSneaking()) {
					setBeamFrequency(itemStackHeld, ((IBeamFrequency) tileEntity).getBeamFrequency());
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.beam_frequency.get",
							tileEntity.getBlockType().getLocalizedName(),
							getBeamFrequency(itemStackHeld)));
				} else {
					((IBeamFrequency) tileEntity).setBeamFrequency(getBeamFrequency(itemStackHeld));
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.beam_frequency.set",
							tileEntity.getBlockType().getLocalizedName(),
							getBeamFrequency(itemStackHeld)));
				}
				return EnumActionResult.SUCCESS;
			}
			return EnumActionResult.FAIL;
		
		case MODE_CONTROL_CHANNEL:
			if (tileEntity instanceof IControlChannel) {
				if (entityPlayer.isSneaking()) {
					setControlChannel(itemStackHeld, ((IControlChannel) tileEntity).getControlChannel());
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.control_channel.get",
							tileEntity.getBlockType().getLocalizedName(),
							getControlChannel(itemStackHeld)));
				} else {
					((IControlChannel) tileEntity).setControlChannel(getControlChannel(itemStackHeld));
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.control_channel.set",
							tileEntity.getBlockType().getLocalizedName(),
							getControlChannel(itemStackHeld)));
				}
				return EnumActionResult.SUCCESS;
			}
			return EnumActionResult.FAIL;
		
		default:
			return EnumActionResult.FAIL;
		}
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
		switch (itemStack.getItemDamage()) {
		case MODE_VIDEO_CHANNEL:
			textTooltip.append(null, "warpdrive.video_channel.tooltip",
			                   new WarpDriveText(Commons.getStyleValue(), getVideoChannel(itemStack)) );
			break;
		case MODE_BEAM_FREQUENCY:
			textTooltip.append(null, "warpdrive.beam_frequency.tooltip",
			                   new WarpDriveText(Commons.getStyleValue(), getBeamFrequency(itemStack)) );
			break;
		case MODE_CONTROL_CHANNEL:
			textTooltip.append(null, "warpdrive.control_channel.tooltip",
			                   new WarpDriveText(Commons.getStyleValue(), getControlChannel(itemStack)) );
			break;
		default:
			textTooltip.append(new TextComponentString("I'm broken :("));
			break;
		}
		
		textTooltip.appendLineBreak();
		textTooltip.append(null, "item.warpdrive.tool.tuning_driver.tooltip.usage");
		
		Commons.addTooltip(list, textTooltip.getFormattedText());
	}
}
