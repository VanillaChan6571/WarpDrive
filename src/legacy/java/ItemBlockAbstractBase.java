package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IItemBase;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.event.TooltipHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemBlockAbstractBase extends ItemBlock implements IItemBase {
	
	final boolean hasUniqueName;
	
	// warning: ItemBlock is created during registration, while block is still being constructed.
	// As such, we can't use block properties from constructor
	public ItemBlockAbstractBase(final Block block, final boolean hasSubtypes, final boolean hasUniqueName) {
		super(block);
		
		setHasSubtypes(hasSubtypes);
		setTranslationKey(block.getTranslationKey());
		this.hasUniqueName = hasUniqueName;
	}
	
	@Override
	public int getMetadata(final int damage) {
		return damage;
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(@Nonnull final ItemStack itemStack) {
		if ( hasUniqueName
		  || !(block instanceof BlockAbstractContainer)
		  || !((BlockAbstractContainer) block).hasSubBlocks ) {
			return getTranslationKey();
		}
		return getTranslationKey() + itemStack.getItemDamage();
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(@Nonnull final ItemStack itemStack) {
		if ( !(block instanceof IBlockBase) ) {
			return EnumTier.BASIC;
		}
		return ((IBlockBase) block).getTier(itemStack);
	}
	
	@Nonnull
	@Override
	public IRarity getForgeRarity(@Nonnull final ItemStack itemStack) {
		final IRarity rarityDefault = super.getForgeRarity(itemStack);
		if ( !(block instanceof IBlockBase) ) {
			return rarityDefault;
		}
		final IRarity rarityItemStack = ((IBlockBase) block).getForgeRarity(itemStack);
		if ( rarityItemStack instanceof EnumRarity
		  && rarityDefault instanceof EnumRarity ) {
			return ((EnumRarity) rarityItemStack).ordinal() > ((EnumRarity) rarityDefault).ordinal() ? rarityItemStack : rarityDefault;
		}
		return rarityItemStack;
	}
	
	public ITextComponent getStatus(final World world, @Nonnull final ItemStack itemStack) {
		final IBlockState blockState = TooltipHandler.getStateForPlacement(block,
		                                                                   world, null, EnumFacing.DOWN,
		                                                                   0.0F, 0.0F, 0.0F, itemStack.getMetadata(),
		                                                                   null, EnumHand.MAIN_HAND);
		
		final TileEntity tileEntity = block.createTileEntity(world, blockState);
		if (tileEntity instanceof TileEntityAbstractBase) {
			return ((TileEntityAbstractBase) tileEntity).getStatus(itemStack, blockState);
			
		} else {// (not a tile entity provider)
			return new TextComponentString("");
		}
	}
	
	@Override
	public void onEntityExpireEvent(final EntityItem entityItem, final ItemStack itemStack) {
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		ClientProxy.modelInitialisation(this);
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		if (hasUniqueName) {
			final ResourceLocation resourceLocation = getRegistryName();
			assert resourceLocation != null;
			return new ModelResourceLocation(resourceLocation, "inventory");
		}
		
		return ClientProxy.getModelResourceLocation(itemStack);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		final String tooltipItemStack = getTranslationKey(itemStack) + ".tooltip";
		if (I18n.hasKey(tooltipItemStack)) {
			Commons.addTooltip(list, new TextComponentTranslation(tooltipItemStack).getFormattedText());
		}
		
		final String tooltipName = getTranslationKey() + ".tooltip";
		if ((!tooltipItemStack.equals(tooltipName)) && I18n.hasKey(tooltipName)) {
			Commons.addTooltip(list, new TextComponentTranslation(tooltipName).getFormattedText());
		}
		
		String tooltipNameWithoutTier = tooltipName;
		for (final EnumTier enumTier : EnumTier.values()) {
			tooltipNameWithoutTier = tooltipNameWithoutTier.replace("." + enumTier.getName(), "");
		}
		if ((!tooltipNameWithoutTier.equals(tooltipItemStack)) && I18n.hasKey(tooltipNameWithoutTier)) {
			Commons.addTooltip(list, new TextComponentTranslation(tooltipNameWithoutTier).getFormattedText());
		}
		
		Commons.addTooltip(list, getStatus(world, itemStack).getFormattedText());
		
		super.addInformation(itemStack, world, list, advancedItemTooltips);
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s {%s} %s",
		                     getClass().getSimpleName(),
		                     Integer.toHexString(hashCode()),
		                     REGISTRY.getNameForObject(this),
		                     getTranslationKey());
	}
}
