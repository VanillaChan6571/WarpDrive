package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IParticleContainerItem;
import cr0s.warpdrive.api.Particle;
import cr0s.warpdrive.api.ParticleStack;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPlasmaTorch extends ItemAbstractBase implements IParticleContainerItem {
	
	public ItemPlasmaTorch(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setMaxDamage(0);
		setMaxStackSize(1);
		setTranslationKey("warpdrive.tool.plasma_torch." + enumTier.getName());
		
		addPropertyOverride(new ResourceLocation(WarpDrive.MODID, "fill"), new IItemPropertyGetter() {
			@SideOnly(Side.CLIENT)
			@Override
			public float apply(@Nonnull final ItemStack itemStack, @Nullable final World world, @Nullable final EntityLivingBase entity) {
				final ParticleStack particleStack = getParticleStack(itemStack);
				if (particleStack != null) {
					return (float) particleStack.getAmount() / getCapacity(itemStack);
				}
				return 0.0F;
			}
		});
	}
	
	/*
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		String variant = "empty";
		final ParticleStack particleStack = getParticleStack(itemStack);
		if (particleStack != null) {
			variant = particleStack.getTranslationKey().replace("warpdrive.particle.", "");
		}
		ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + variant);
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	/**/
	
	@Nonnull
	public static ItemStack getItemStackNoCache(@Nonnull final EnumTier enumTier, @Nullable final Particle particle, final int amount) {
		return WarpDrive.itemPlasmaTorch[enumTier.getIndex()].getItemStackNoCache(particle, amount);
	}
	
	@Nonnull
	public ItemStack getItemStackNoCache(@Nullable final Particle particle, final int amount) {
		final ItemStack itemStack = new ItemStack(this, 1, 0);
		ParticleStack particleStack = null;
		if ( particle != null
		   && amount != 0 ) {
			particleStack = new ParticleStack(particle, amount);
			final NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setTag(IParticleContainerItem.TAG_PARTICLE, particleStack.writeToNBT(new NBTTagCompound()));
			itemStack.setTagCompound(tagCompound);
		}
		updateDamageLevel(itemStack, particleStack);
		return itemStack;
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		list.add(getItemStackNoCache(null, 0));
		// list.add(getItemStackNoCache(ParticleRegistry.ION, 1000));
		// list.add(getItemStackNoCache(ParticleRegistry.PROTON, 1000));
		// list.add(getItemStackNoCache(ParticleRegistry.ANTIMATTER, 1000));
		// list.add(getItemStackNoCache(ParticleRegistry.STRANGE_MATTER, 1000));
		// list.add(getItemStackNoCache(ParticleRegistry.TACHYONS, 1000));
	}
	
	@Override
	public boolean hasContainerItem(@Nonnull final ItemStack itemStack) {
		return true;
	}
	
	@Nonnull
	@Override
	public Item getContainerItem() {
		return Item.getItemFromBlock(Blocks.FIRE);
	}
	
	@Nonnull
	@Override
	public ItemStack getContainerItem(@Nonnull final ItemStack itemStackFilled) {
		final ParticleStack particleStack = getParticleStack(itemStackFilled);
		if (particleStack != null) {
			final int amountToConsume = getAmountToConsume(itemStackFilled);
			if (amountToConsume > 0) {
				final int amountLeft = particleStack.getAmount() - amountToConsume;
				if (amountLeft <= 0) {
					return getItemStackNoCache(null, 0);
				}
				return getItemStackNoCache(particleStack.getParticle(), amountLeft);
			}
		}
		return ItemStack.EMPTY;
	}
	
	@Override
	public void setAmountToConsume(@Nonnull final ItemStack itemStack, final int amountToConsume) {
		final ParticleStack particleStack = getParticleStack(itemStack);
		if ( particleStack == null
		  || particleStack.getParticle() == null ) {
			return;
		}
		NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (amountToConsume > 0) {
			if (tagCompound == null) {
				tagCompound = new NBTTagCompound();
			}
			tagCompound.setInteger(IParticleContainerItem.TAG_AMOUNT_TO_CONSUME, amountToConsume);
			tagCompound.setLong(IParticleContainerItem.TAG_TICK_TO_CONSUME, System.currentTimeMillis());
			
		} else if (tagCompound != null) {
			tagCompound.removeTag(IParticleContainerItem.TAG_AMOUNT_TO_CONSUME);
			tagCompound.removeTag(IParticleContainerItem.TAG_TICK_TO_CONSUME);
			if (tagCompound.isEmpty()) {
				itemStack.setTagCompound(null);
			}
		}
	}
	
	private int getAmountToConsume(@Nonnull final ItemStack itemStack) {
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound != null) {
			// when taking a recipe output, the recipe is matched again before checking for recipients, so we can assume it's in the same tick
			final long tickToConsume = tagCompound.getInteger(IParticleContainerItem.TAG_TICK_TO_CONSUME);
			if (System.currentTimeMillis() - tickToConsume < 50L) {
				return tagCompound.getInteger(IParticleContainerItem.TAG_AMOUNT_TO_CONSUME);
			} else {
				tagCompound.removeTag(IParticleContainerItem.TAG_AMOUNT_TO_CONSUME);
				tagCompound.removeTag(IParticleContainerItem.TAG_TICK_TO_CONSUME);
			}
		}
		return 0;
	}
	
	private static int getDamageLevel(@Nonnull final ItemStack itemStack, final ParticleStack particleStack) {
		if (!(itemStack.getItem() instanceof ItemPlasmaTorch)) {
			WarpDrive.logger.error(String.format("Invalid ItemStack passed, expecting ItemPlasmaTorch: %s",
			                                     itemStack));
			return itemStack.getItemDamage();
		}
		if ( particleStack == null
		  || particleStack.getParticle() == null ) {
			return 0;
		}
		final ItemPlasmaTorch itemPlasmaTorch = (ItemPlasmaTorch) itemStack.getItem();
		final int type = particleStack.getParticle().getColorIndex() % 5;
		final double ratio = particleStack.getAmount() / (double) itemPlasmaTorch.getCapacity(itemStack);
		final int offset = (ratio < 0.2) ? 0 : (ratio < 0.4) ? 1 : (ratio < 0.6) ? 2 : (ratio < 0.8) ? 3 : (ratio < 1.0) ? 4 : 5;
		return (1 + type * 6 + offset);
	}
	
	private static void updateDamageLevel(@Nonnull final ItemStack itemStack, final ParticleStack particleStack) {
		itemStack.setItemDamage(getDamageLevel(itemStack, particleStack));
	}
	
	@Override
	public ParticleStack getParticleStack(@Nonnull final ItemStack itemStack) {
		if ( itemStack.getCount() != 1
		  || itemStack.getItem() != this ) {
			return null;
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound == null) {
			return null;
		}
		if (!tagCompound.hasKey(IParticleContainerItem.TAG_PARTICLE)) {
			return null;
		}
		return ParticleStack.loadFromNBT(tagCompound.getCompoundTag(IParticleContainerItem.TAG_PARTICLE));
	}
	
	@Override
	public int getCapacity(final ItemStack container) {
		return WarpDriveConfig.PLASMA_TORCH_CAPACITY_BY_TIER[enumTier.getIndex()];
	}
	
	@Override
	public boolean isEmpty(final ItemStack itemStack) {
		final ParticleStack particleStack = getParticleStack(itemStack);
		return particleStack == null
		    || particleStack.isEmpty();
	}
	
	@Override
	public int fill(final ItemStack itemStack, final ParticleStack resource, final boolean doFill) {
		if (itemStack.getCount() != 1) {
			return 0;
		}
		ParticleStack particleStack = getParticleStack(itemStack);
		if ( particleStack == null
		  || particleStack.getParticle() == null ) {
			particleStack = new ParticleStack(resource.getParticle(), 0);
		} else if ( !particleStack.isParticleEqual(resource)
		         || particleStack.getAmount() >= getCapacity(itemStack) ) {
			return 0;
		}
		final int transfer = Math.min(resource.getAmount(), getCapacity(itemStack) - particleStack.getAmount());
		if (doFill) {
			particleStack.fill(transfer);
			
			final NBTTagCompound tagCompound = itemStack.hasTagCompound() ? itemStack.getTagCompound() : new NBTTagCompound();
			assert tagCompound != null;
			tagCompound.setTag(IParticleContainerItem.TAG_PARTICLE, particleStack.writeToNBT(new NBTTagCompound()));
			if (!itemStack.hasTagCompound()) {
				itemStack.setTagCompound(tagCompound);
			}
			updateDamageLevel(itemStack, particleStack);
		}
		return transfer;
	}
	
	@Override
	public ParticleStack drain(final ItemStack itemStack, final ParticleStack resource, final boolean doDrain) {
		final ParticleStack particleStack = getParticleStack(itemStack);
		if ( particleStack == null
		  || particleStack.getParticle() == null
		  || !particleStack.isParticleEqual(resource) 
		  || particleStack.getAmount() <= 0 ) {
			return null;
		}
		final int transfer = Math.min(resource.getAmount(), particleStack.getAmount());
		if (doDrain) {
			particleStack.fill(-transfer);
			
			final NBTTagCompound tagCompound = itemStack.hasTagCompound() ? itemStack.getTagCompound() : new NBTTagCompound();
			assert tagCompound != null;
			tagCompound.setTag(IParticleContainerItem.TAG_PARTICLE, particleStack.writeToNBT(new NBTTagCompound()));
			if (!itemStack.hasTagCompound()) {
				itemStack.setTagCompound(tagCompound);
			}
			updateDamageLevel(itemStack, particleStack);
		}
		return resource.copy(transfer);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		if (!(itemStack.getItem() instanceof  ItemPlasmaTorch)) {
			WarpDrive.logger.error(String.format("Invalid ItemStack passed, expecting ItemPlasmaTorch: %s",
			                                     itemStack));
			return;
		}
		final ItemPlasmaTorch itemPlasmaTorch = (ItemPlasmaTorch) itemStack.getItem();
		final ParticleStack particleStack = itemPlasmaTorch.getParticleStack(itemStack);
		final String tooltip;
		if ( particleStack == null
		  || particleStack.getParticle() == null ) {
			tooltip = new TextComponentTranslation("item.warpdrive.tool.plasma_torch.tooltip.empty").getFormattedText();
			Commons.addTooltip(list, tooltip);
			
		} else {
			final Particle particle = particleStack.getParticle();
			
			tooltip = new TextComponentTranslation("item.warpdrive.tool.plasma_torch.tooltip.filled",
			                                       particleStack.getAmount(), particle.getLocalizedName()).getFormattedText();
			Commons.addTooltip(list, tooltip);
			
			final String particleTooltip = particle.getLocalizedTooltip();
			if (!particleTooltip.isEmpty()) {
				Commons.addTooltip(list, particleTooltip);
			}
		}
	}
}
