package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.forcefield.BlockForceFieldProjector;
import cr0s.warpdrive.block.forcefield.BlockForceFieldRelay;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumTier;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemForceFieldShape extends ItemAbstractBase {	
	
	private static ItemStack[] itemStackCache;
	
	public ItemForceFieldShape(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setHasSubtypes(true);
		setTranslationKey("warpdrive.force_field.shape");
		
		itemStackCache = new ItemStack[EnumForceFieldShape.length];
	}
	
	@Nonnull
	public static ItemStack getItemStack(final EnumForceFieldShape forceFieldShape) {
		if (forceFieldShape != null) {
			final int damage = forceFieldShape.ordinal();
			if (itemStackCache[damage] == null) {
				itemStackCache[damage] = new ItemStack(WarpDrive.itemForceFieldShape, 1, damage);
			}
			return itemStackCache[damage];
		}
		return ItemStack.EMPTY;
	}
	
	@Nonnull
	public static ItemStack getItemStackNoCache(@Nonnull final EnumForceFieldShape forceFieldShape, final int amount) {
		return new ItemStack(WarpDrive.itemForceFieldShape, amount, forceFieldShape.ordinal());
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		if (damage >= 0 && damage < EnumForceFieldShape.length) {
			return getTranslationKey() + "." + EnumForceFieldShape.get(damage).getName();
		}
		return getTranslationKey();
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		for(final EnumForceFieldShape enumForceFieldShape : EnumForceFieldShape.values()) {
			if (enumForceFieldShape != EnumForceFieldShape.NONE) {
				list.add(new ItemStack(this, 1, enumForceFieldShape.ordinal()));
			}
		}
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		if (damage >= 0 && damage < EnumComponentType.length) {
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + EnumForceFieldShape.get(damage).getName());
		}
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	@Override
	public boolean doesSneakBypassUse(@Nonnull final ItemStack itemStack,
	                                  @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos,
	                                  @Nonnull final EntityPlayer player) {
		final Block block = blockAccess.getBlockState(blockPos).getBlock();
		return block instanceof BlockForceFieldRelay || block instanceof BlockForceFieldProjector || super.doesSneakBypassUse(itemStack, blockAccess, blockPos, player);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, "\n");
		
		Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.force_field.shape.tooltip.usage").getFormattedText());
	}
}