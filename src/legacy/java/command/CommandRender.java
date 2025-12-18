package cr0s.warpdrive.command;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class CommandRender extends AbstractCommand {
	
	@Nonnull
	@Override
	public String getName() {
		return "wrender";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Nonnull
	@Override
	public String getUsage(@Nonnull final ICommandSender commandSender) {
		return "/" + getName();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void execute(@Nonnull final MinecraftServer server, @Nonnull final ICommandSender commandSender, @Nonnull final String[] args) {
		// parse arguments
		if (args.length > 0) {
			Commons.addChatMessage(commandSender, new TextComponentString(getUsage(commandSender)));
			return;
		}
		
		final EntityPlayerMP entityPlayer = commandSender instanceof EntityPlayerMP ? (EntityPlayerMP) commandSender : null;
		
		// validate context
		if (entityPlayer == null) {
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.getStyleWarning(), "warpdrive.command.player_required") );
			return;
		}
		
		// evaluate sub command
		final World world = entityPlayer.getEntityWorld();
		BlockPos blockPos = entityPlayer.getPosition();
		
		//noinspection ConstantConditions
		if (world == null || blockPos == null) {
			Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
			                                                         .append(Commons.getStyleWarning(), "warpdrive.command.invalid_location") );
			return;
		}
		IBlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			blockPos = blockPos.down();
			blockState = world.getBlockState(blockPos);
		}
		
		Commons.addChatMessage(commandSender, new WarpDriveText().append(getPrefix())
		                                                         .appendInLine(Commons.getStyleCorrect(), "Dumping render details %s",
		                                                                       Commons.format(world, blockPos) ) );
		final Block block = blockState.getBlock();
		
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "Blockstate is %s (%s)",
		                                                                 Commons.getChatValue(blockState.toString()),
		                                                                 Commons.getChatValue(block.getMetaFromState(blockState))));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "Light opacity is %s / useNeighborBrightness is %s",
		                                                                 Commons.getChatValue(block.getLightOpacity(blockState)),
		                                                                 Commons.getChatValue(block.getUseNeighborBrightness(blockState))));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "isFullBlock is %s / isFullCube is %s / isAir is %s",
		                                                                 Commons.getChatValue(block.isFullBlock(blockState)),
		                                                                 Commons.getChatValue(block.isFullCube(blockState)),
		                                                                 Commons.getChatValue(block.isAir(blockState, world, blockPos))));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "isBlockNormalCube is %s / isNormalCube is %s",
		                                                                 Commons.getChatValue(block.isBlockNormalCube(blockState)),
		                                                                 Commons.getChatValue(block.isNormalCube(blockState))));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "isTopSolid is %s / causesSuffocation is %s",
		                                                                 Commons.getChatValue(block.isTopSolid(blockState)),
		                                                                 Commons.getChatValue(block.causesSuffocation(blockState))));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "Material isOpaque %s / Material blocksLight is %s",
		                                                                 Commons.getChatValue(blockState.getMaterial().isOpaque()),
		                                                                 Commons.getChatValue(blockState.getMaterial().blocksLight())));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "Material isLiquid %s / Material isSolid %s",
		                                                                 Commons.getChatValue(blockState.getMaterial().isLiquid()),
		                                                                 Commons.getChatValue(blockState.getMaterial().isSolid())));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "isOpaqueCube is %s  / isTranslucent %s",
		                                                                 Commons.getChatValue(blockState.isOpaqueCube()),
		                                                                 WarpDrive.proxy.isDedicatedServer() ? Commons.getChatValue("???") : Commons.getChatValue(blockState.isTranslucent())));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "renderLayer is %s  / renderType is %s",
		                                                                 WarpDrive.proxy.isDedicatedServer() ? Commons.getChatValue("???") : Commons.getChatValue(block.getRenderLayer().toString()),
		                                                                 Commons.getChatValue(block.getRenderType(blockState).toString())));
		Commons.addChatMessage(commandSender, new WarpDriveText().append(Commons.getStyleNormal(), "isSideSolid D %s, U %s, N %s, S %s, W %s, E %s",
		                                                                 Commons.getChatValue(block.isSideSolid(blockState, world, blockPos, EnumFacing.DOWN)),
		                                                                 Commons.getChatValue(block.isSideSolid(blockState, world, blockPos, EnumFacing.UP)),
		                                                                 Commons.getChatValue(block.isSideSolid(blockState, world, blockPos, EnumFacing.NORTH)),
		                                                                 Commons.getChatValue(block.isSideSolid(blockState, world, blockPos, EnumFacing.SOUTH)),
		                                                                 Commons.getChatValue(block.isSideSolid(blockState, world, blockPos, EnumFacing.WEST)),
		                                                                 Commons.getChatValue(block.isSideSolid(blockState, world, blockPos, EnumFacing.EAST))));
	}
}
