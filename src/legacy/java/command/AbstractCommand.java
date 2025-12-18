package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;

import net.minecraft.command.CommandBase;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public abstract class AbstractCommand extends CommandBase {
	
	public ITextComponent getPrefix() {
		return new TextComponentString("/" + getName()).setStyle(Commons.getStyleHeader())
		                                               .appendSibling(new TextComponentString(" "));
	}
	
}
