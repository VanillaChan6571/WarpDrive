package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.entity.EntityNPC;

import javax.annotation.Nonnull;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommandNPC extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "wnpc";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return String.format("/%s <name> (<scale>) (<texture>) ({<nbt>})\nName may contain space using _ character",
		                     getName() );
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		final World world = commandSender.getEntityWorld();
		final BlockPos blockPos = commandSender.getPosition();
		
		//noinspection ConstantConditions
		if (world == null || blockPos == null) {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.invalid_location").setStyle(Commons.getStyleWarning())));
			return;
		}
		
		if (args.length < 1 || args.length > 4) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		if (!FMLCommonHandler.instance().getEffectiveSide().isServer()) {
			return;
		}
		
		// get default parameter values
		final String name = args[0].replace("_", " ");
		float scale = 1.0F;
		String texturePath = "";
		String stringNBT = "";
		int indexArg = 1;
		
		// parse arguments
		if (args.length > indexArg) {
			try {
				scale = Commons.toFloat(args[indexArg]);
				indexArg++;
			} catch (final NumberFormatException exception) {
				// skip to next argument
			}
		}
		
		if (args.length > indexArg) {
			texturePath = args[indexArg];
			indexArg++;
		}
		
		if (args.length > indexArg) {
			stringNBT = args[indexArg];
			indexArg++;
		}
		
		if (args.length > indexArg) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		// spawn the entity
		final EntityNPC entityNPC = new EntityNPC(world);
		entityNPC.setPosition(blockPos.getX() + 0.5D, blockPos.getY() + 0.1D, blockPos.getZ() + 0.5D);
		entityNPC.setCustomNameTag(name);
		entityNPC.setSizeScale(scale);
		entityNPC.setTextureString(texturePath);
		if (!stringNBT.isEmpty()) {
			final NBTTagCompound tagCompound;
			try {
				tagCompound = JsonToNBT.getTagFromJson(stringNBT);
			} catch (final NBTException exception) {
				WarpDrive.logger.error(exception.getMessage());
				Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
				return;
			}
			entityNPC.deserializeNBT(tagCompound);
		}
		world.spawnEntity(entityNPC);
		Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("Added NPC %1$s",
		                                                                                             entityNPC )));
	}
	
}
