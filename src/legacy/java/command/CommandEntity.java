package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nonnull;

public class CommandEntity extends AbstractCommand {
	
	private static final List<String> entitiesNoRemoval = Arrays.asList(
			"item.EntityItemFrame_",
			"item.EntityPainting_"
			);
	private static final List<String> entitiesNoCount = Arrays.asList(
			"item.EntityItemFrame_",
			"item.EntityPainting_"
			);
	
	private static final Style styleFound  = new Style().setColor(TextFormatting.WHITE);
	private static final Style styleNumber = new Style().setColor(TextFormatting.WHITE);
	private static final Style styleFactor = new Style().setColor(TextFormatting.DARK_GRAY);
	private static final Style styleName   = new Style().setColor(TextFormatting.LIGHT_PURPLE);
	
	@Nonnull
	@Override
	public String getName() {
		return "wentity";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return "/" + getName() + " <radius> <filter> <kill?>"
			+ "\nradius: - or <= 0 to check all loaded in current world, 1+ blocks around player"
			+ "\nfilter: * to get all, anything else is a case insensitive string"
			+ "\nkill: yes/y/1 to kill, anything else is ignored";
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		if (args.length > 3) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		int radius = 20;
		String filter = "";
		boolean kill = false;
		try {
			if (args.length > 0) {
				final String par = args[0].toLowerCase();
				if (par.equals("-") || par.equals("world") || par.equals("global") || par.equals("*")) {
					radius = -1;
				} else {
					radius = Integer.parseInt(par);
				}
			}
			if ( args.length > 1
			  && !args[1].equalsIgnoreCase("*") ) {
				filter = args[1];
			}
			if (args.length > 2) {
				final String par = args[2].toLowerCase();
				kill = par.equals("y") || par.equals("yes") || par.equals("1");
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		WarpDrive.logger.info(String.format("/%s %d '*%s*' %s", getName(), radius, filter, kill));
		
		final List<Entity> entities;
		if (radius <= 0) {
			final World world;
			if (commandSender instanceof EntityPlayerMP) {
				world = ((EntityPlayerMP) commandSender).world;
			} else {
				world = DimensionManager.getWorld(0);
			}
			entities = new ArrayList<>(world.loadedEntityList);
		} else {
			if (!(commandSender instanceof EntityPlayerMP)) {
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			final EntityPlayerMP entityPlayer = (EntityPlayerMP) commandSender;
			entities = entityPlayer.world.getEntitiesWithinAABBExcludingEntity(entityPlayer, new AxisAlignedBB(
					Math.floor(entityPlayer.posX    ), Math.floor(entityPlayer.posY    ), Math.floor(entityPlayer.posZ    ),
					Math.floor(entityPlayer.posX + 1), Math.floor(entityPlayer.posY + 1), Math.floor(entityPlayer.posZ + 1)).grow(radius, radius, radius));
		}
		final HashMap<String, Integer> counts = new HashMap<>(entities.size());
		int count = 0;
		for (final Entity entity : entities) {
			String name = entity.getClass().getCanonicalName();
			if (name == null) {
				name = "-null-";
			} else {
				name = name.replaceAll("net\\.minecraft\\.entity\\.", "") + "_";
			}
			if (filter.isEmpty() && !entitiesNoCount.isEmpty()) {
				boolean isCountable = true;
				for (final String entityNoCount : entitiesNoCount) {
					if (name.contains(entityNoCount)) {
						isCountable = false;
						break;
					}
				}
				if (!isCountable) {
					continue;
				}
			}
			if (filter.isEmpty() || name.contains(filter)) {
				// update statistics
				count++;
				if (!counts.containsKey(name)) {
					counts.put(name, 1);
				} else {
					counts.put(name, counts.get(name) + 1);
				}
				if (!filter.isEmpty()) {
					if (count == 1) {
						Commons.addChatMessage(commandSender, new WarpDriveText(styleFound, "warpdrive.command.found_title"));
					}
					Commons.addChatMessage(commandSender, new WarpDriveText(styleFound, "warpdrive.command.found_line",
					                                                        entity));
				}
				// remove entity
				if (kill && !entity.isEntityInvulnerable(WarpDrive.damageAsphyxia)) {
					if (!entitiesNoRemoval.isEmpty()) {
						boolean isRemovable = true;
						for (final String entityNoRemoval : entitiesNoRemoval) {
							if (name.contains(entityNoRemoval)) {
								isRemovable = false;
								break;
							}
						}
						if (!isRemovable) {
							continue;
						}
					}
					entity.setDead();
				}
			}
		}
		if (count == 0) {
			final ITextComponent textComponent = new TextComponentTranslation("warpdrive.command.no_matching_entity", radius).setStyle(Commons.getStyleWarning());
			Commons.addChatMessage(commandSender, textComponent);
			return;
		}
		
		ITextComponent textComponent = new TextComponentTranslation("warpdrive.command.x_matching_entities", count, radius).setStyle(Commons.getStyleCorrect());
		Commons.addChatMessage(commandSender, textComponent);
		if (counts.size() < 10) {
			for (final Entry<String, Integer> entry : counts.entrySet()) {
				textComponent = new TextComponentString(entry.getValue().toString()).setStyle(styleNumber)
						                .appendSibling(new TextComponentString("x").setStyle(styleFactor))
						                .appendSibling(new TextComponentString(entry.getKey()).setStyle(styleName));
				textComponent.getStyle().setColor(TextFormatting.WHITE);
				Commons.addChatMessage(commandSender, textComponent);
			}
		} else {
			textComponent = new TextComponentString("");
			boolean isFirst = true;
			for (final Entry<String, Integer> entry : counts.entrySet()) {
				if (isFirst) {
					isFirst = false;
				} else {
					textComponent.appendSibling(new TextComponentString(", ").setStyle(styleFactor));
				}
				textComponent.appendSibling(new TextComponentString(entry.getValue().toString()).setStyle(styleNumber))
				             .appendSibling(new TextComponentString("x").setStyle(styleFactor))
				             .appendSibling(new TextComponentString(entry.getKey()).setStyle(styleName));
			}
			Commons.addChatMessage(commandSender, textComponent);
		}
	}
}