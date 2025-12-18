package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockWarpIsolation extends BlockAbstractBase {
	
	public BlockWarpIsolation(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(3.5F);
		setTranslationKey("warpdrive.detection.warp_isolation");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, new TextComponentTranslation(getTranslationKey() + ".tooltip.usage",
		                                                      WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS,
		                                                      Math.round(WarpDriveConfig.RADAR_MIN_ISOLATION_EFFECT * 100.0D),
		                                                      WarpDriveConfig.RADAR_MAX_ISOLATION_BLOCKS,
		                                                      Math.round(WarpDriveConfig.RADAR_MAX_ISOLATION_EFFECT * 100.0D),
		                                                      WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE + 1).getFormattedText());
	}
}