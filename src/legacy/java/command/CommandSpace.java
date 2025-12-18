package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;

import javax.annotation.Nonnull;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

public class CommandSpace extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "space";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return "/space (<playerName>) ([overworld|nether|end|theend|space|hyper|hyperspace|<dimensionId>])";
	}
	
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		// set defaults
		int dimensionIdTarget = Integer.MAX_VALUE;
		
		EntityPlayerMP[] entityPlayerMPs = null;
		if (commandSender instanceof EntityPlayerMP) {
			entityPlayerMPs = new EntityPlayerMP[1];
			entityPlayerMPs[0] = (EntityPlayerMP) commandSender;
		}
		
		// parse arguments
		//noinspection StatementWithEmptyBody
		if (args.length == 0) {
			// nop
		} else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
				Commons.addChatMessage(commandSender,  new TextComponentString(getUsage(commandSender)));
				return;
			}
			
			final EntityPlayerMP[] entityPlayerMPs_found = Commons.getOnlinePlayerByNameOrSelector(commandSender, args[0]);
			if (entityPlayerMPs_found != null) {
				entityPlayerMPs = entityPlayerMPs_found;
			} else if (commandSender instanceof EntityPlayer) {
				dimensionIdTarget = CelestialObjectManager.getDimensionId(args[0], (EntityPlayer) commandSender);
			} else {
				Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.player_not_found", args[0]).setStyle(Commons.getStyleWarning())));
				return;
			}
			
		} else if (args.length == 2) {
			final EntityPlayerMP[] entityPlayerMPs_found = Commons.getOnlinePlayerByNameOrSelector(commandSender, args[0]);
			if (entityPlayerMPs_found != null) {
				entityPlayerMPs = entityPlayerMPs_found;
			} else {
				Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.player_not_found", args[0]).setStyle(Commons.getStyleWarning())));
				return;
			}
			dimensionIdTarget = CelestialObjectManager.getDimensionId(args[1], entityPlayerMPs[0]);
			
		} else {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.too_many_arguments", args.length).setStyle(Commons.getStyleWarning())));
			return;
		}
		
		// check player
		if (entityPlayerMPs == null || entityPlayerMPs.length <= 0) {
			Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.player_not_found", args[0]).setStyle(Commons.getStyleWarning())));
			return;
		}
		
		for (final EntityPlayerMP entityPlayerMP : entityPlayerMPs) {
			// toggle between overworld and space if no dimension was provided
			int xTarget = MathHelper.floor(entityPlayerMP.posX);
			int yTarget = Math.min(255, Math.max(0, MathHelper.floor(entityPlayerMP.posY)));
			int zTarget = MathHelper.floor(entityPlayerMP.posZ);
			final CelestialObject celestialObjectCurrent = CelestialObjectManager.get(entityPlayerMP.world, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
			if (dimensionIdTarget == Integer.MAX_VALUE) {
				if (celestialObjectCurrent == null) {
					Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.player_in_unknown_dimension",
					                                                                                             entityPlayerMP.getName(), entityPlayerMP.world.provider.getDimension()).setStyle(Commons.getStyleWarning())));
					Commons.addChatMessage(commandSender, new TextComponentTranslation("warpdrive.command.specify_explicit_dimension").setStyle(Commons.getStyleCorrect()));
					continue;
				}
				if ( celestialObjectCurrent.isSpace()
				  || celestialObjectCurrent.isHyperspace() ) {
					// in space or hyperspace => move to closest child
					final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(entityPlayerMP.world, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
					if (celestialObjectChild == null) {
						dimensionIdTarget = 0;
					} else if (celestialObjectChild.isVirtual()) {
						Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.player_can_t_go_virtual",
						                                                                                             entityPlayerMP.getName(), celestialObjectChild.getDisplayName()).setStyle(Commons.getStyleWarning()) ));
						Commons.addChatMessage(commandSender, new TextComponentTranslation("warpdrive.command.specify_explicit_dimension").setStyle(Commons.getStyleCorrect()));
						continue;
					} else {
						dimensionIdTarget = celestialObjectChild.dimensionId;
						final VectorI vEntry = celestialObjectChild.getEntryOffset();
						xTarget += vEntry.x;
						yTarget += vEntry.y;
						zTarget += vEntry.z;
					}
				} else {
					// on a planet => move to space
					if ( celestialObjectCurrent.parent == null
					  || celestialObjectCurrent.parent.isVirtual() ) {
						dimensionIdTarget = 0;
						
					} else {
						dimensionIdTarget = celestialObjectCurrent.parent.dimensionId;
						final VectorI vEntry = celestialObjectCurrent.getEntryOffset();
						xTarget -= vEntry.x;
						yTarget -= vEntry.y;
						zTarget -= vEntry.z;
					}
				}
				
			} else {
				// adjust offset when it's directly above or below us
				if ( celestialObjectCurrent != null
				  && celestialObjectCurrent.parent != null
				  && celestialObjectCurrent.parent.dimensionId == dimensionIdTarget ) {// moving to parent explicitly
					final VectorI vEntry = celestialObjectCurrent.getEntryOffset();
					xTarget -= vEntry.x;
					yTarget -= vEntry.y;
					zTarget -= vEntry.z;
				} else {
					final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(entityPlayerMP.world, (int) entityPlayerMP.posX, (int) entityPlayerMP.posZ);
					if ( celestialObjectChild != null
					  && celestialObjectChild.dimensionId == dimensionIdTarget ) {// moving to child explicitly
						final VectorI vEntry = celestialObjectChild.getEntryOffset();
						xTarget += vEntry.x;
						yTarget += vEntry.y;
						zTarget += vEntry.z;
					}
				}
			}
			
			// get target celestial object
			final CelestialObject celestialObjectTarget = CelestialObjectManager.get(false, dimensionIdTarget, xTarget, zTarget);
			
			// force to center if we're outside the border
			if ( celestialObjectTarget != null
			  && !celestialObjectTarget.isInsideBorder(xTarget, zTarget) ) {
				// outside 
				xTarget = celestialObjectTarget.dimensionCenterX;
				zTarget = celestialObjectTarget.dimensionCenterZ;
			}
			
			// get target world
			final WorldServer worldTarget = Commons.getOrCreateWorldServer(dimensionIdTarget);
			if (worldTarget == null) {
				Commons.addChatMessage(commandSender, getPrefix().appendSibling(new TextComponentTranslation("warpdrive.command.undefined_dimension",
				                                                                                             dimensionIdTarget).setStyle(Commons.getStyleWarning())));
				continue;
			}
			
			// inform player
			final ITextComponent textComponent = new TextComponentTranslation("warpdrive.command.teleporting_player_x_to_y",
			                                             entityPlayerMP.getName(),
			                                             Commons.format(worldTarget)).setStyle(Commons.getStyleCorrect());
			Commons.addChatMessage(commandSender, textComponent);
			WarpDrive.logger.info(textComponent.getUnformattedText());
			if (commandSender != entityPlayerMP) {
				Commons.addChatMessage(entityPlayerMP, new TextComponentTranslation("warpdrive.command.teleporting_by_x_to_y",
				                                                                    commandSender.getName(), Commons.format(worldTarget), dimensionIdTarget).setStyle(Commons.getStyleCorrect()));
			}
			
			// find a good spot
			
			if ( (worldTarget.isAirBlock(new BlockPos(xTarget, yTarget - 1, zTarget)) && !entityPlayerMP.capabilities.allowFlying)
			  || !worldTarget.isAirBlock(new BlockPos(xTarget, yTarget    , zTarget))
			  || !worldTarget.isAirBlock(new BlockPos(xTarget, yTarget + 1, zTarget)) ) {// non solid ground and can't fly, or inside blocks
				yTarget = worldTarget.getTopSolidOrLiquidBlock(new BlockPos(xTarget, yTarget, zTarget)).getY() + 1;
				if (yTarget == 0) {
					yTarget = 128;
				} else {
					for (int safeY = yTarget - 3; safeY > Math.max(1, yTarget - 20); safeY--) {
						if (!worldTarget.isAirBlock(new BlockPos(xTarget, safeY - 1, zTarget))
						  && worldTarget.isAirBlock(new BlockPos(xTarget, safeY    , zTarget))
						  && worldTarget.isAirBlock(new BlockPos(xTarget, safeY + 1, zTarget))) {
							yTarget = safeY;
							break;
						}
					}
				}
			}
			
			// actual teleportation
			Commons.moveEntity(entityPlayerMP, worldTarget, new Vector3(xTarget + 0.5D, yTarget + 0.2D, zTarget + 0.5D));
		}
	}
}
