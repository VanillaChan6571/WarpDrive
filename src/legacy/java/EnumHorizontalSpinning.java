package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.util.HashMap;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

public enum EnumHorizontalSpinning implements IStringSerializable {
	
	DOWN_NORTH(EnumFacing.DOWN , "down_north", EnumFacing.NORTH),
	DOWN_SOUTH(EnumFacing.DOWN , "down_south", EnumFacing.SOUTH),
	DOWN_WEST (EnumFacing.DOWN , "down_west" , EnumFacing.WEST ),
	DOWN_EAST (EnumFacing.DOWN , "down_east" , EnumFacing.EAST ),
	UP_NORTH  (EnumFacing.UP   , "up_north"  , EnumFacing.NORTH),
	UP_SOUTH  (EnumFacing.UP   , "up_south"  , EnumFacing.SOUTH),
	UP_WEST   (EnumFacing.UP   , "up_west"   , EnumFacing.WEST ),
	UP_EAST   (EnumFacing.UP   , "up_east"   , EnumFacing.EAST ),
	NORTH     (EnumFacing.NORTH, "north"     , EnumFacing.NORTH),
	SOUTH     (EnumFacing.SOUTH, "south"     , EnumFacing.SOUTH),
	WEST      (EnumFacing.WEST , "west"      , EnumFacing.WEST ),
	EAST      (EnumFacing.EAST , "east"      , EnumFacing.EAST );
	
	public final EnumFacing facing;
	public final String     name;
	public final EnumFacing spinning;
	
	// cached values
	public static final int length;
	private static final HashMap<Integer, EnumHorizontalSpinning> ID_MAP = new HashMap<>();
	
	static {
		length = EnumHorizontalSpinning.values().length;
		for (final EnumHorizontalSpinning cameraType : values()) {
			ID_MAP.put(cameraType.ordinal(), cameraType);
		}
	}
	
	EnumHorizontalSpinning(@Nonnull final EnumFacing facing, @Nonnull final String name, @Nonnull final EnumFacing spinning) {
		this.facing = facing;
		this.name = name;
		this.spinning = spinning;
	}
	
	public static EnumHorizontalSpinning get(final int id) {
		return ID_MAP.get(id);
	}
	
	public static EnumHorizontalSpinning get(@Nonnull final EnumFacing facing, @Nonnull final EnumFacing spinning) throws RuntimeException {
		// enforce spinning for vertical orientations
		final EnumFacing spinningCorrected;
		if (facing.getYOffset() != 0) {
			spinningCorrected = spinning;
		} else {
			spinningCorrected = facing;
		}
		
		// find the right combo
		for (final EnumHorizontalSpinning enumHorizontalSpinning : EnumHorizontalSpinning.values()) {
			if ( enumHorizontalSpinning.facing.equals(facing)
			  && enumHorizontalSpinning.spinning.equals(spinningCorrected) ) {
				return enumHorizontalSpinning;
			}
		}
		throw new RuntimeException(String.format("There's no HorizontalSpinning with facing %s spinning %s",
		                                         facing, spinning ));
	}
	
	@Nonnull
	@Override
	public String getName() { return name; }
}
