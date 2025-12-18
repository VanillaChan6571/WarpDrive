package cr0s.warpdrive.data;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;
import java.util.HashMap;

public enum EnumComponentType implements IStringSerializable {
	// data storage
	// (comparator)
	MEMORY_CRYSTAL             ("memory_crystal"),
	MEMORY_CLUSTER             ("memory_cluster"),
	
	// processing
	// (circuits)
	DIAMOND_CRYSTAL            ("diamond_crystal"),
	EMERALD_CRYSTAL            ("emerald_crystal"),
	
	// energy storage
	// (redstone)
	CAPACITIVE_CRYSTAL         ("capacitive_crystal"),
	CAPACITIVE_CLUSTER         ("capacitive_cluster"),
	
	// networking interface
	ENDER_COIL                 ("ender_coil"),
	DIAMOND_COIL               ("diamond_coil"),
	COMPUTER_INTERFACE         ("computer_interface"),
	
	// breathing
	BONE_CHARCOAL              ("bone_charcoal"),
	ACTIVATED_CARBON           ("activated_carbon"),
	AIR_CANISTER               ("air_canister_empty"),
	
	// human interface
	// (redstone)
	FLAT_SCREEN                ("flat_screen"),
	HOLOGRAPHIC_PROJECTOR      ("holographic_projector"),
	
	// mechanical
	GLASS_TANK                 ("glass_tank"),
	MOTOR                      ("motor"),
	PUMP                       ("pump"),
	
	// optical
	LENS                       ("lens"),
	ZOOM                       ("zoom"),
	DIFFRACTION_GRATING        ("diffraction_grating"),
	
	// energy interface
	POWER_INTERFACE            ("power_interface"),
	SUPERCONDUCTOR             ("superconductor"),
	
	// crafting components
	LASER_MEDIUM_EMPTY         ("laser_medium_empty"),
	ELECTROMAGNETIC_PROJECTOR  ("electromagnetic_projector"),
	REACTOR_CORE               ("reactor_core"),
	
	// rubber material
	RAW_RUBBER                 ("raw_rubber"),
	RUBBER                     ("rubber"),
	
	// composite materials
	BIOPULP                    ("biopulp"),
	BIOFIBER                   ("biofiber"),
	
	RAW_CERAMIC                ("raw_ceramic"),
	CERAMIC                    ("ceramic"),
	
	RAW_CARBON_FIBER           ("raw_carbon_fiber"),
	RAW_CARBON_MESH            ("raw_carbon_mesh"),
	CARBON_FIBER               ("carbon_fiber"),
	;
	
	private final String name;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumComponentType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumComponentType.values().length;
		for (final EnumComponentType componentType : values()) {
			ID_MAP.put(componentType.ordinal(), componentType);
		}
	}
	
	EnumComponentType(final String name) {
		this.name = name;
	}
	
	public static EnumComponentType get(final int damage) {
		return ID_MAP.get(damage);
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
}
