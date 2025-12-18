package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.data.InventoryWrapper;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collection;

public class CommandDump extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "wdump";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return "/" + getName() + " (<inventory type>) (<player selector>)"
		       + "\nWrite loot table in console for selected inventory type of selected player"
		       + "\nInventory types are:"
		       + "\n- container: any item container below or next to player"
		       + "\n- enderchest: player's enderchest"
		       + "\n- hand: player's main hand"
		       + "\n- player: player's inventory";
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		// parse arguments
		if (args.length > 2) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
			
		}
		
		EntityPlayerMP entityPlayer = commandSender instanceof EntityPlayerMP ? (EntityPlayerMP) commandSender : null;
		final String subCommand;
		
		if (args.length == 0) {
			subCommand = "container";
			
		} else {
			if ( args[0].equalsIgnoreCase("help")
			  || args[0].equalsIgnoreCase("?") ) {
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			
			// get player context
			if (args.length == 2) {
				final EntityPlayerMP[] entityPlayers = Commons.getOnlinePlayerByNameOrSelector(commandSender, args[1]);
				if (entityPlayers == null || entityPlayers.length < 1) {
					Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
					                                                         .append(Commons.getStyleWarning(), "warpdrive.command.player_not_found",
					                                                                 args[1]) );
					return;
				}
				entityPlayer = entityPlayers[0];
			}
			
			// get sub command
			subCommand = args[0];
		}
		
		// validate context
		if (entityPlayer == null) {
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.getStyleWarning(), "warpdrive.command.player_required") );
			return;
		}
		
		// evaluate sub command
		final Object inventory;
		switch (subCommand.toLowerCase()) {
		case "container":
			final World world = entityPlayer.getEntityWorld();
			final BlockPos blockPos = entityPlayer.getPosition();
			
			//noinspection ConstantConditions
			if (world == null || blockPos == null) {
				Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
				                                                         .append(Commons.getStyleWarning(), "warpdrive.command.invalid_location") );
				return;
			}
			
			final Collection<Object> inventories = InventoryWrapper.getConnectedInventories(world, blockPos);
			if (inventories.isEmpty()) {
				Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
				                                                         .append(Commons.getStyleWarning(), "warpdrive.command.no_container") );
				return;
			}
			inventory = inventories.iterator().next();
			WarpDrive.logger.info(String.format("Dumping content from container %s:",
			                                    Commons.format(world, blockPos) ));
			break;
			
		case "enderchest":
			inventory = entityPlayer.getInventoryEnderChest();
			WarpDrive.logger.info(String.format("Dumping content from %s ender chest:",
			                                    entityPlayer.getDisplayNameString() ));
			break;
			
		case "hand":
			inventory = entityPlayer.getHeldItemMainhand();
			WarpDrive.logger.info(String.format("Dumping content from %s main hand:",
			                                    entityPlayer.getDisplayNameString() ));
			break;
			
		case "player":
			inventory = entityPlayer.inventory;
			WarpDrive.logger.info(String.format("Dumping content from %s inventory:",
			                                    entityPlayer.getDisplayNameString() ));
			break;
			
		default:
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.getStyleWarning(), "warpdrive.command.invalid_parameter", 
			                                                                 args[0])
			                                                         .appendLineBreak()
			                                                         .appendSibling(new TextComponentString(getUsage(commandSender))) );
			return;
		}
		
		// actually dump
		final int size = InventoryWrapper.getSize(inventory);
		if (size == 0) {
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.getStyleWarning(), "warpdrive.command.empty_inventory") );
		}
		for (int indexSlot = 0; indexSlot < size; indexSlot++) {
			final ItemStack itemStack = InventoryWrapper.getStackInSlot(inventory, indexSlot);
			if (itemStack != ItemStack.EMPTY && !itemStack.isEmpty()) {
				final ResourceLocation uniqueIdentifier = itemStack.getItem().getRegistryName();
				assert uniqueIdentifier != null;
				final String stringDamage = itemStack.getItemDamage() == 0 ? "" : String.format(" damage=\"%d\"", itemStack.getItemDamage());
				final String stringNBT = !itemStack.hasTagCompound() ? "" : String.format(" nbt=\"%s\"", itemStack.getTagCompound());
				WarpDrive.logger.info(String.format("Slot %3d is <loot item=\"%s:%s\"%s minQuantity=\"%d\" maxQuantity=\"%d\"%s weight=\"1\" /><!-- %s -->",
				                                    indexSlot,
				                                    uniqueIdentifier.getNamespace(), uniqueIdentifier.getPath(),
				                                    stringDamage,
				                                    itemStack.getCount(), itemStack.getCount(),
				                                    stringNBT,
				                                    itemStack.getDisplayName()));
			}
		}
	}
}
