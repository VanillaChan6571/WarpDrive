package cr0s.warpdrive.item;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBreathingHelmet;
import cr0s.warpdrive.api.IItemBase;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemWarpArmor extends ItemArmor implements IItemBase, IBreathingHelmet {
	
	public static final String[] suffixes = {  "boots", "leggings", "chestplate", "helmet" };
	
	protected final EnumTier enumTier;
	
	public ItemWarpArmor(final String registryName, final EnumTier enumTier,
	                     final ArmorMaterial armorMaterial, final int renderIndex, final EntityEquipmentSlot entityEquipmentSlot) {
		super(armorMaterial, renderIndex, entityEquipmentSlot);
		
		this.enumTier = enumTier;
		setTranslationKey("warpdrive.armor." + enumTier.getName() + "." + suffixes[entityEquipmentSlot.getIndex()]);
		setRegistryName(registryName);
		setCreativeTab(WarpDrive.creativeTabMain);
		WarpDrive.register(this);
	}
	
	@Nonnull
	@Override
	public String getArmorTexture(@Nonnull final ItemStack itemStack, final Entity entity, final EntityEquipmentSlot slot, @Nullable final String renderingType) {
		return "warpdrive:textures/armor/warp_armor_" + (armorType == EntityEquipmentSlot.LEGS ? 2 : 1) + ".png";
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(final ItemStack itemStack) {
		return enumTier;
	}
	
	@Nonnull
	@Override
	public IRarity getForgeRarity(@Nonnull final ItemStack itemStack) {
		return getTier(itemStack).getForgeRarity();
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
		return ClientProxy.getModelResourceLocation(itemStack);
	}
	
	@Override
	public boolean canBreath(final EntityLivingBase entityLivingBase) {
		return armorType == EntityEquipmentSlot.HEAD;
	}
}