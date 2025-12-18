package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;

public class CommandBed extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "wbed";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return getName() + " (<playerName>)"
		       + "\nplayerName: name of the player home to find. Exact casing is required.";
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		// parse arguments
		EntityPlayerMP[] entityPlayerMPs = null;
		if (args.length == 0) {
			if (commandSender instanceof EntityPlayerMP) {
				entityPlayerMPs = new EntityPlayerMP[1];
				entityPlayerMPs[0] = (EntityPlayerMP) commandSender;
			} else {
				Commons.addChatMessage(commandSender, getPrefix().appendSibling(new WarpDriveText(Commons.getStyleWarning(), "warpdrive.command.player_required")));
				return;
			}
			
		} else if (args.length == 1) {
			if ( args[0].equalsIgnoreCase("help")
			  || args[0].equalsIgnoreCase("?") ) {
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			if ( commandSender instanceof EntityPlayerMP
			  && !((EntityPlayerMP) commandSender).capabilities.isCreativeMode ) {
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			
			final EntityPlayerMP[] entityPlayerMPs_found = Commons.getOnlinePlayerByNameOrSelector(commandSender, args[0]);
			if (entityPlayerMPs_found != null) {
				entityPlayerMPs = entityPlayerMPs_found;
			} else if (commandSender instanceof EntityPlayerMP) {
				entityPlayerMPs = new EntityPlayerMP[1];
				entityPlayerMPs[0] = (EntityPlayerMP) commandSender;
			} else {
				Commons.addChatMessage(commandSender, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.command.player_not_found",
				                                                        args[0] ));
				return;
			}
		}
		
		assert entityPlayerMPs != null;
		for (final EntityPlayerMP entityPlayerMP : entityPlayerMPs) {
			final BlockPos bedLocation = entityPlayerMP.getBedLocation(entityPlayerMP.world.provider.getDimension());
			if (bedLocation == null) {
				Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.command.no_bed_to_teleport_to_self",
				                                                         Commons.format(entityPlayerMP.world) ));
				if (args.length != 0) {
					Commons.addChatMessage(commandSender, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.command.no_bed_to_teleport_to_other",
					                                                        entityPlayerMP.getName(),
					                                                        Commons.format(entityPlayerMP.world) ));
				}
				continue;
			}
			
			final Block block = entityPlayerMP.world.getBlockState(bedLocation).getBlock();
			if (!(block instanceof BlockBed)) {
				Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.command.lost_bed_can_t_teleport_self",
				                                                         Commons.format(entityPlayerMP.world) ));
				if (args.length != 0) {
					Commons.addChatMessage(commandSender, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.command.lost_bed_can_t_teleport_other",
					                                                        entityPlayerMP.getName(),
					                                                        Commons.format(entityPlayerMP.world) ));
				}
				continue;
			}
			
			entityPlayerMP.setPositionAndUpdate(bedLocation.getX() + 0.5D, bedLocation.getY() + 0.5D, bedLocation.getZ() + 0.5D);
			
			if (args.length == 0) {
				Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.command.teleporting_to_x",
				                                                         Commons.format(entityPlayerMP.world, bedLocation) ));
			} else {
				Commons.addChatMessage(entityPlayerMP, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.command.teleporting_by_x_to_y",
				                                                         commandSender,
				                                                         Commons.format(entityPlayerMP.world, bedLocation) ));
				Commons.addChatMessage(commandSender, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.command.teleporting_player_x_to_y",
				                                                        entityPlayerMP.getName(),
				                                                        Commons.format(entityPlayerMP.world, bedLocation) ));
			}
		}
	}
}
