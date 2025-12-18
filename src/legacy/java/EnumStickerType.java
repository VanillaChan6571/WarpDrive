package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.IStringSerializable;

public enum EnumStickerType implements IStringSerializable {
	
	// directions
	ARROW_UP            ("arrow_up"         , 50),
	ARROW_DOWN          ("arrow_down"       , 51),
	ARROW_LEFT          ("arrow_left"       , 52),
	ARROW_RIGHT         ("arrow_right"      , 53),
	ARROW_VERTICAL      ("arrow_vertical"   , 54),
	ARROW_HORIZONTAL    ("arrow_horizontal" , 55),
	ARROW_LEFT_TURN     ("arrow_left_turn"  , 56),
	ARROW_RIGHT_TURN    ("arrow_right_turn" , 57),
	ARROW_Y             ("arrow_y"          , 58),
	ARROW_CROSS         ("arrow_cross"      , 59),
	
	// hazards
	COLD                ("cold"             , 100),
	CORROSIVE           ("corrosive"        , 101),    // skin irritation or corrosion, metal corrosion
	ELECTRIC            ("electric"         , 102),
	ENVIRONMENT         ("environment"      , 103),    // dead fish
	EXPLOSIVE           ("explosive"        , 104),
	FLAMMABLE           ("flammable"        , 105),    // flame
	HEALTH              ("health"           , 106),
	HEAT                ("heat"             , 107),
	INFECTIOUS          ("infectious"       , 108),
	LASER               ("laser"            , 109),
	NOISE               ("noise"            , 110),   // noise reduction headset
	OXIDIZING           ("oxidizing"        , 111),   // flame over circle
	PRESSURE            ("pressure"         , 112),   // gaz pressure
	RADIATION           ("radiation"        , 113),
	TOXIC               ("toxic"            , 114),   // acute toxicity is skull and crossbones
	WARNING             ("warning"          , 115),   // exclamation mark
	;
	
	private final String name;
	private final int metadata;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumStickerType> ID_MAP = new HashMap<>();
	
	static {
		length = EnumStickerType.values().length;
		for (final EnumStickerType enumStickerType : values()) {
			ID_MAP.put(enumStickerType.getMetadata(), enumStickerType);
		}
	}
	
	EnumStickerType(final String name, final int metadata) {
		this.name = name;
		this.metadata = metadata;
	}
	
	public static EnumStickerType byMetadata(final int metadata) {
		return ID_MAP.get(metadata);
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
	
	public int getMetadata() {
		return metadata;
	}
}
