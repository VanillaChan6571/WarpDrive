package cr0s.warpdrive.data;

import net.minecraft.block.state.IBlockState;

import net.minecraftforge.common.property.IUnlistedProperty;

public class UnlistedPropertyBlockState implements IUnlistedProperty<IBlockState> {
	
	private final String name;
	
	UnlistedPropertyBlockState(final String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean isValid(final IBlockState value) {
		return true;
	}
	
	@Override
	public Class<IBlockState> getType() {
		return IBlockState.class;
	}
	
	@Override
	public String valueToString(final IBlockState value) {
		return value.toString();
	}
}