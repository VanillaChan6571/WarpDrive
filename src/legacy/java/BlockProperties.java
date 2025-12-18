package cr0s.warpdrive.data;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.util.EnumFacing;

public class BlockProperties {
	
	// Common block properties
	public static final PropertyBool                         ACTIVE               = PropertyBool.create("active");
	public static final UnlistedPropertyBlockState           CAMOUFLAGE           = new UnlistedPropertyBlockState("camouflage");
	public static final PropertyBool                         CONNECTED            = PropertyBool.create("connected");
	public static final PropertyDirection                    FACING               = PropertyDirection.create("facing");
	public static final PropertyDirection                    FACING_HORIZONTAL    = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyEnum<EnumHorizontalSpinning> HORIZONTAL_SPINNING  = PropertyEnum.create("spinning", EnumHorizontalSpinning.class);
	
}
