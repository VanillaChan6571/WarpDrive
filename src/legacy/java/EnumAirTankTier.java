package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

import net.minecraftforge.common.IRarity;

public enum EnumAirTankTier implements IStringSerializable {
	
	CANISTER ("canister", 0, EnumTier.BASIC   ),
	BASIC    ("basic"   , 1, EnumTier.BASIC   ),
	ADVANCED ("advanced", 2, EnumTier.ADVANCED),
	SUPERIOR ("superior", 3, EnumTier.SUPERIOR);
	
	private final String name;
	private final int index;
	private final EnumTier enumTier;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumAirTankTier> ID_MAP;
	
	static {
		length = EnumAirTankTier.values().length;
		ID_MAP = new HashMap<>(length);
		for (final EnumAirTankTier enumAirTankTier : values()) {
			ID_MAP.put(enumAirTankTier.index, enumAirTankTier);
		}
	}
	
	EnumAirTankTier(final String name, final int index, final EnumTier enumTier) {
		this.name = name;
		this.index = index;
		this.enumTier = enumTier;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public static EnumAirTankTier get(final int index) {
		return ID_MAP.get(index);
	}
	
	public int getIndex() {
		return index;
	}
	
	public EnumTier getTier(){
		return enumTier;
	}
	
	@Nonnull
	public IRarity getForgeRarity() {
		return enumTier.getForgeRarity();
	}
}
