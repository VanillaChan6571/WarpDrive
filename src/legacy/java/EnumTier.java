package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.item.EnumRarity;
import net.minecraft.util.IStringSerializable;

import net.minecraftforge.common.IRarity;

public enum EnumTier implements IStringSerializable {
	
	CREATIVE ("creative", 0, EnumRarity.EPIC     ),
	BASIC    ("basic"   , 1, EnumRarity.COMMON   ),
	ADVANCED ("advanced", 2, EnumRarity.UNCOMMON ),
	SUPERIOR ("superior", 3, EnumRarity.RARE     );
	
	private final String name;
	private final int index;
	private final IRarity rarity;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumTier> ID_MAP;
	private static final EnumTier[] tblNonCreatives;
	
	static {
		length = EnumTier.values().length;
		ID_MAP = new HashMap<>(length);
		final ArrayList<EnumTier> list = new ArrayList<>(length);
		for (final EnumTier enumTier : values()) {
			ID_MAP.put(enumTier.index, enumTier);
			if (enumTier != CREATIVE) {
				list.add(enumTier);
			}
		}
		tblNonCreatives = list.toArray(new EnumTier[0]);
	}
	
	EnumTier(final String name, final int index, @Nonnull final IRarity rarity) {
		this.name = name;
		this.index = index;
		this.rarity = rarity;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static EnumTier get(final int index) {
		return ID_MAP.get(index);
	}
	
	public int getIndex() {
		return index;
	}
	
	@Nonnull
	public IRarity getForgeRarity() {
		return rarity;
	}
	
	public static EnumTier[] nonCreative() {
		return tblNonCreatives;
	}
}
