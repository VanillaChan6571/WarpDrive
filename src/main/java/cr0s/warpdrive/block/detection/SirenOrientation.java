package cr0s.warpdrive.block.detection;

import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

/** Twelve mounting/spin combinations from 1.12.2 {@code EnumHorizontalSpinning}. */
public enum SirenOrientation implements IStringSerializable {

	DOWN_NORTH(Direction.DOWN, "down_north", Direction.NORTH),
	DOWN_SOUTH(Direction.DOWN, "down_south", Direction.SOUTH),
	DOWN_WEST(Direction.DOWN, "down_west", Direction.WEST),
	DOWN_EAST(Direction.DOWN, "down_east", Direction.EAST),
	UP_NORTH(Direction.UP, "up_north", Direction.NORTH),
	UP_SOUTH(Direction.UP, "up_south", Direction.SOUTH),
	UP_WEST(Direction.UP, "up_west", Direction.WEST),
	UP_EAST(Direction.UP, "up_east", Direction.EAST),
	NORTH(Direction.NORTH, "north", Direction.NORTH),
	SOUTH(Direction.SOUTH, "south", Direction.SOUTH),
	WEST(Direction.WEST, "west", Direction.WEST),
	EAST(Direction.EAST, "east", Direction.EAST);

	private final Direction facing;
	private final String name;
	private final Direction spinning;

	SirenOrientation(final Direction facing, final String name, final Direction spinning) {
		this.facing = facing;
		this.name = name;
		this.spinning = spinning;
	}

	public Direction getFacing() {
		return facing;
	}

	public Direction getSpinning() {
		return spinning;
	}

	public static SirenOrientation of(final Direction facing, final Direction spinning) {
		final Direction corrected = facing.getAxis().isVertical() ? spinning : facing;
		for (final SirenOrientation orientation : values()) {
			if (orientation.facing == facing && orientation.spinning == corrected) {
				return orientation;
			}
		}
		return NORTH;
	}

	@Nonnull
	@Override
	public String getSerializedName() {
		return name;
	}
}
