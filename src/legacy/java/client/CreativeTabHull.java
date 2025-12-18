package cr0s.warpdrive.client;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CreativeTabHull extends CreativeTabAbstractBase {
	
	public CreativeTabHull(final String label) {
		super(label, 1618);
	}
	
	@Nonnull
	@Override
	@SideOnly(Side.CLIENT)
	public ItemStack createIcon() {
		final int tier = 1 + random.nextInt(EnumTier.length - 1);
		final int metadata = random.nextInt(16);
		switch (random.nextInt(7)) {
		case 0: return new ItemStack(WarpDrive.blockHulls_plain[tier][0], 1, metadata);
		case 1: return new ItemStack(WarpDrive.blockHulls_plain[tier][1], 1, metadata);
		case 2: return new ItemStack(WarpDrive.blockHulls_glass[tier], 1, metadata);
		case 3: return new ItemStack(WarpDrive.blockHulls_slab[tier][metadata], 1, 0);
		case 4: return new ItemStack(WarpDrive.blockHulls_slab[tier][metadata], 1, 2);
		case 5: return new ItemStack(WarpDrive.blockHulls_stairs[tier][metadata], 1);
		case 6: return new ItemStack(WarpDrive.blockHulls_omnipanel[tier], 1, metadata);
		}
		return new ItemStack(Blocks.OBSIDIAN, 1);
    }
}
