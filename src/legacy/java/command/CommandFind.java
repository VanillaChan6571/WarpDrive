package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.data.GlobalRegion;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;

public class CommandFind extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "wfind";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return "/" + getName() + " (<shipName>)"
		       + "\nshipName: name of the ship to find. Exact casing is preferred.";
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		// parse arguments
		final String nameToken;
		final EntityPlayerMP entityPlayer = commandSender instanceof EntityPlayerMP ? (EntityPlayerMP) commandSender : null;
		if (args.length == 0) {
			if (entityPlayer == null) {
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			final GlobalRegion globalRegion = GlobalRegionManager.getNearest(EnumGlobalRegionType.SHIP, entityPlayer.world, entityPlayer.getPosition());
			if (globalRegion != null) {
				Commons.addChatMessage(commandSender, new TextComponentString(String.format("Ship '%s' found in %s",
				                                                                            globalRegion.name,
				                                                                            globalRegion.getFormattedLocation())));
			} else {
				Commons.addChatMessage(commandSender, new TextComponentString(String.format("No ship found in %s",
				                                                                            Commons.format(entityPlayer.world) )));
			}
			return;
			
		} else if (args.length == 1) {
			if ( args[0].equalsIgnoreCase("help")
			  || args[0].equalsIgnoreCase("?") ) {
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			nameToken = args[0];
			
		} else {
			final StringBuilder nameBuilder = new StringBuilder();
			for (final String param : args) {
				if (nameBuilder.length() > 0) {
					nameBuilder.append(" ");
				}
				nameBuilder.append(param);
			}
			nameToken = nameBuilder.toString();
		}
		
		final String result = GlobalRegionManager.listByKeyword(EnumGlobalRegionType.SHIP, nameToken);
		Commons.addChatMessage(commandSender, new TextComponentString(result));
	}
}
