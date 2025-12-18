package cr0s.warpdrive;

import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.BlockStatePos;
import cr0s.warpdrive.data.GlobalPosition;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.ChunkHandler;
import cr0s.warpdrive.world.SpaceTeleporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import com.mojang.authlib.GameProfile;

/**
 * Common static methods
 */
public class Commons {
	
	private static final String CHAR_FORMATTING = "" + (char) 167;
	private static final List<EnumBlockRenderType> ALLOWED_RENDER_TYPES = Arrays.asList(
		EnumBlockRenderType.INVISIBLE,
//		EnumBlockRenderType.LIQUID,
		EnumBlockRenderType.ENTITYBLOCK_ANIMATED,
		EnumBlockRenderType.MODEL
	);
	
	private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(0, 2, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
	private static final CopyOnWriteArraySet<UUID> uuidGameProfileFilling = new CopyOnWriteArraySet<>();
	
	private static Method methodThrowable_getStackTraceElement;
	
	static {
		try {
			methodThrowable_getStackTraceElement = Throwable.class.getDeclaredMethod("getStackTraceElement",
			                                                                         int.class);
			methodThrowable_getStackTraceElement.setAccessible(true);
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
	}
	
	@SuppressWarnings("ConstantConditions") // IDE says "§" == CHAR_FORMATTING, execution says otherwise
	@Nonnull
	public static String updateEscapeCodes(@Nonnull final String message) {
		return message
		       .replace("§", CHAR_FORMATTING)
		       .replace("\\n", "\n")
		       .replace(CHAR_FORMATTING + "r", CHAR_FORMATTING + "7")
		       .replaceAll("\u00A0", " ");  // u00A0 is 'NO-BREAK SPACE'
	}
	
	@Nonnull
	public static String removeFormatting(@Nonnull final String message) {
		return updateEscapeCodes(message)
		       .replaceAll("(" + CHAR_FORMATTING + ".)", "");
	}
	
	private static boolean isFormatColor(final char chr) {
		return chr >= 48 && chr <= 57
		    || chr >= 97 && chr <= 102
		    || chr >= 65 && chr <= 70;
	}
	
	private static boolean isFormatSpecial(final char chr) {
		return chr >= 107 && chr <= 111
		    || chr >= 75 && chr <= 79
		    || chr == 114
		    || chr == 82;
	}
	
	// inspired by FontRender.getFormatFromString
	@Nonnull
	private static String getFormatFromString(@Nonnull final String message) {
		final int indexLastChar = message.length() - 1;
		StringBuilder result = new StringBuilder();
		int indexEscapeCode = -1;
		while ((indexEscapeCode = message.indexOf(167, indexEscapeCode + 1)) != -1) {
			if (indexEscapeCode < indexLastChar) {
				final char chr = message.charAt(indexEscapeCode + 1);
				
				if (isFormatColor(chr)) {
					result = new StringBuilder("\u00a7" + chr);
				} else if (isFormatSpecial(chr)) {
					result.append("\u00a7").append(chr);
				}
			}
		}
		
		return result.toString();
	}
	
	@Nonnull
	public static Style getStyleCommand() {
		return new Style().setColor(TextFormatting.AQUA);
	}
	@Nonnull
	public static Style getStyleHeader() {
		return new Style().setColor(TextFormatting.GOLD);
	}
	@Nonnull
	public static Style getStyleCorrect() {
		return new Style().setColor(TextFormatting.GREEN);
	}
	@Nonnull
	public static Style getStyleDisabled() {
		return new Style().setColor(TextFormatting.GRAY);
	}
	@Nonnull
	public static Style getStyleNormal() {
		return new Style().setColor(TextFormatting.WHITE);
	}
	@Nonnull
	public static Style getStyleValue() {
		return new Style().setColor(TextFormatting.YELLOW);
	}
	@Nonnull
	public static Style getStyleVoltage() {
		return new Style().setColor(TextFormatting.DARK_GREEN);
	}
	@Nonnull
	public static Style getStyleWarning() {
		return new Style().setColor(TextFormatting.RED);
	}
	
	@Nonnull
	public static WarpDriveText getChatPrefix(@Nonnull final Block block) {
		return getChatPrefix(block.getTranslationKey() + ".name");
	}
	
	@Nonnull
	public static WarpDriveText getChatPrefix(@Nonnull final ItemStack itemStack) {
		return getChatPrefix(itemStack.getTranslationKey() + ".name");
	}
	
	@Nonnull
	public static WarpDriveText getChatPrefix(@Nonnull final String translationKey) {
		return new WarpDriveText(getStyleHeader(), "warpdrive.guide.prefix",
		                         new TextComponentTranslation(translationKey));
	}
	
	@Nonnull
	public static WarpDriveText getNamedPrefix(@Nonnull final String name) {
		return new WarpDriveText(getStyleHeader(), "warpdrive.guide.prefix",
		                         new TextComponentString(name));
	}
	
	@Nonnull
	public static WarpDriveText getChatValue(final boolean bool) {
		if (bool) {
			return new WarpDriveText(getStyleCorrect(), "true");
		} else {
			return new WarpDriveText(getStyleWarning(), "false");
		}
	}
	
	@Nonnull
	public static WarpDriveText getChatValue(final int value) {
		return new WarpDriveText(getStyleValue(), "%s", value);
	}
	
	@Nonnull
	public static WarpDriveText getChatValue(@Nonnull final String value) {
		if (value.equals("???")) {
			return new WarpDriveText(getStyleDisabled(), "???");
		} else {
			return new WarpDriveText(getStyleValue(), "%s", value);
		}
	}
	
	public static void addChatMessage(final ICommandSender commandSender, @Nonnull final ITextComponent textComponent) {
		final String message = textComponent.getFormattedText();
		if (commandSender == null) {
			WarpDrive.logger.error(String.format("Unable to send message to NULL sender: %s",
			                                     message));
			return;
		}
		
		// skip empty messages
		if (message.isEmpty()) {
			return;
		}
		
		final String[] lines = updateEscapeCodes(message).split("\n");
		if (lines.length == 1) {
			commandSender.sendMessage(textComponent);
			
		} else {
			String formatNextLine = "";
			for (final String line : lines) {
				commandSender.sendMessage(new TextComponentString(formatNextLine + line));
				
				// compute remaining format
				int index = 0;
				while (index < line.length()) {
					if (line.charAt(index) == (char) 167 && index + 1 < line.length()) {
						index++;
						final char charFormat = line.charAt(index);
						if (charFormat == 'r') {
							formatNextLine = CHAR_FORMATTING + charFormat;
						} else {
							formatNextLine += CHAR_FORMATTING + charFormat;
						}
					}
					index++;
				}
			}
		}
		
		// logger.info(message);
	}
	
	// return cleaned tooltip for comparing independently from spacing, formatting and casing
	private static String getComparableTooltipLine(final String line) {
		String lineCleaned = removeFormatting(line)
				                     .toLowerCase()
				                     .replace("-", " ")
				                     .replace(".", " ")
				                     .replace(",", " ")
				                     .replace(":", " ")
				                     .replace(";", " ")
				                     .replace("  ", " ")
				                     .trim();
		if (lineCleaned.startsWith("- ")) {
			lineCleaned = lineCleaned.substring(2).trim();
		}
		if (lineCleaned.endsWith(":")) {
			lineCleaned = lineCleaned.substring(0, lineCleaned.length() - 1).trim();
		}
		return lineCleaned;
	}
	
	// add tooltip information with text formatting and line splitting
	// will ensure it fits on minimum screen width
	public static void addTooltip(final List<String> list, @Nonnull final String tooltip) {
		// skip empty tooltip
		if (tooltip.isEmpty()) {
			return;
		}
		
		// apply requested formatting
		final String[] lines = updateEscapeCodes(tooltip).split("\n");
		
		// add new lines
		for (final String line : lines) {
			// skip redundant information
			boolean isExisting = false;
			final String cleanToAdd = getComparableTooltipLine(line);
			for (final String lineExisting : list) {
				final String cleanExisting = getComparableTooltipLine(lineExisting);
				if (cleanExisting.equals(cleanToAdd)) {
					isExisting = true;
					break;
				}
			}
			if (isExisting) {
				continue;
			}
			
			// apply screen formatting/cesure
			String lineRemaining = line;
			String formatNextLine = "";
			while (!lineRemaining.isEmpty()) {
				int indexToCut = formatNextLine.length();
				int displayLength = 0;
				final int length = lineRemaining.length();
				while (indexToCut < length && displayLength <= 38) {
					if (lineRemaining.charAt(indexToCut) == (char) 167 && indexToCut + 1 < length) {
						indexToCut++;
					} else {
						displayLength++;
					}
					indexToCut++;
				}
				if (indexToCut < length) {
					indexToCut = lineRemaining.substring(0, indexToCut).lastIndexOf(' ');
					if (indexToCut == -1 || indexToCut == 0) {// no space available, show the whole line 'as is'
						list.add(lineRemaining);
						lineRemaining = "";
					} else {// cut at last space
						list.add(lineRemaining.substring(0, indexToCut).replaceAll("\u00A0", " "));
						
						// compute remaining format
						int index = formatNextLine.length();
						while (index <= indexToCut) {
							if (lineRemaining.charAt(index) == (char) 167 && index + 1 < indexToCut) {
								index++;
								formatNextLine += CHAR_FORMATTING + lineRemaining.charAt(index);
							}
							index++;
						}
						
						// cut for next line, recovering current format
						lineRemaining = formatNextLine + lineRemaining.substring(indexToCut + 1);
					}
				} else {
					list.add(lineRemaining.replaceAll("\u00A0", " "));
					lineRemaining = "";
				}
			}
		}
	}
	
	public static boolean isKeyPressed(@Nonnull final KeyBinding keyBinding) {
		try {
			final int keyCode = keyBinding.getKeyCode();
			return keyCode < 0 ? Mouse.isButtonDown(keyCode + 100) : Keyboard.isKeyDown(keyCode);
		} catch(final Exception exception) {
			if (throttleMe(keyBinding.getDisplayName())) {
				exception.printStackTrace();
				WarpDrive.logger.error(String.format("Exception trying to get key pressed status for %s",
				                                     keyBinding.getDisplayName() ));
			}
			return false;
		}
	}
	
	public static Field getField(final Class<?> clazz, final String deobfuscatedName, final String obfuscatedName) {
		Field fieldToReturn = null;
		
		try {
			fieldToReturn = clazz.getDeclaredField(deobfuscatedName);
		} catch (final Exception exception1) {
			try {
				fieldToReturn = clazz.getDeclaredField(obfuscatedName);
			} catch (final Exception exception2) {
				exception2.printStackTrace(WarpDrive.printStreamError);
				final StringBuilder map = new StringBuilder();
				for (final Field fieldDeclared : clazz.getDeclaredFields()) {
					if (map.length() > 0) {
						map.append(", ");
					}
					map.append(fieldDeclared.getName());
				}
				WarpDrive.logger.error(String.format("Unable to find %1$s field in %2$s class. Available fields are: %3$s",
				                                     deobfuscatedName, clazz.toString(), map.toString()));
			}
		}
		if (fieldToReturn != null) {
			fieldToReturn.setAccessible(true);
		}
		return fieldToReturn;
	}
	
	public static String format(final long value) {
		// alternate: BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_EVEN).toPlainString(),
		return String.format("%,d", Math.round(value));
	}
	
	@Nonnull
	public static String format(final Object[] arguments) {
		final StringBuilder result = new StringBuilder();
		if (arguments != null && arguments.length > 0) {
			for (final Object argument : arguments) {
				if (result.length() > 0) {
					result.append(", ");
				}
				if (argument instanceof String) {
					result.append("\"").append(argument).append("\"");
				} else {
					result.append(argument);
				}
			}
		}
		return result.toString();
	}
	
	@Nonnull
	public static String formatHexadecimal(final int[] ints) {
		final StringBuilder result = new StringBuilder();
		if (ints != null && ints.length > 0) {
			for (final int value : ints) {
				if (result.length() > 0) {
					result.append(", ");
				}
				result.append(String.format("0x%8x", value));
			}
		}
		return result.toString();
	}
	
	@Nonnull
	public static String format(final World world) {
		if (world == null) {
			return "~NULL~";
		}
		
		// world.getProviderName() is MultiplayerChunkCache on client, ServerChunkCache on local server, (undefined method) on dedicated server
		
		// world.provider.getSaveFolder() is null for the Overworld, other dimensions shall define it
		String saveFolder;
		try {
			saveFolder = world.provider.getSaveFolder();
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			saveFolder = "<Exception DIM" + world.provider.getDimension() + ">";
		}
		if (saveFolder == null || saveFolder.isEmpty()) {
			final int dimension = world.provider.getDimension();
			if (dimension != 0) {
				assert false;
				return String.format("~invalid dimension %d~", dimension);
			}
			
			// world.getWorldInfo().getWorldName() is MpServer on client side, or the server.properties' world name on server side
			final String worldName = world.getWorldInfo().getWorldName();
			if (worldName.equals("MpServer")) {
				return "overworld";
			}
			return worldName;
		}
		return saveFolder;
	}
	
	public static String format(final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		if (blockAccess instanceof World) {
			return format((World) blockAccess, blockPos);
		}
		return String.format("@ %s (%d %d %d)",
		                     blockAccess,
		                     blockPos.getX(), blockPos.getY(), blockPos.getZ() );
	}
	
	public static String format(final World world, @Nonnull final BlockPos blockPos) {
		return format(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}
	
	public static String format(final World world, final int x, final int y, final int z) {
		return String.format("@ %s (%d %d %d)",
		                     format(world),
		                     x, y, z );
	}
	
	public static String format(final World world, @Nonnull final Vector3 vector3) {
		return format(world, vector3.x, vector3.y, vector3.z);
	}
	
	public static String format(final World world, final double x, final double y, final double z) {
		return String.format("@ %s (%.2f %.2f %.2f)",
		                     format(world),
		                     x, y, z );
	}
	
	public static String format(@Nonnull final Entity entity) {
		if (entity.world != null) {
			return format(entity.world, entity.posX, entity.posY, entity.posZ);
		}
		return String.format("@ DIM%d (%.2f %.2f %.2f)",
		                     entity.dimension,
		                     entity.posX, entity.posY, entity.posZ );
	}
	
	public static String format(@Nonnull final GlobalPosition globalPosition) {
		return String.format("@ DIM%d (%d %d %d)",
		                     globalPosition.dimensionId,
		                     globalPosition.x, globalPosition.y, globalPosition.z );
	}
	
	public static String format(@Nonnull final ItemStack itemStack) {
		final String stringNBT;
		if (itemStack.hasTagCompound()) {
			stringNBT = " " + itemStack.getTagCompound();
		} else {
			stringNBT = "";
		}
		return String.format("%dx%s@%d (%s)%s",
		                     itemStack.getCount(),
		                     itemStack.getItem().getRegistryName(),
		                     itemStack.getItemDamage(),
		                     itemStack.getDisplayName(),
		                     stringNBT);
	}
	
	public static String format(@Nonnull final IBlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final Block block = blockState.getBlock();
		try {
			final ItemStack itemStack = block.getPickBlock(blockState, null, world, blockPos, null);
			return new WarpDriveText(null, itemStack.getTranslationKey() + ".name").getFormattedText();
		} catch (final Exception exception1) {
			try {
				return new WarpDriveText(null, block.getTranslationKey() + ".name").getFormattedText();
			} catch (final Exception exception2) {
				return blockState.toString();
			}
		}
	}
	
	public static String format(@Nonnull final Material material) {
		String name = material.toString();
		try {
			for (final Field field : Material.class.getDeclaredFields()) {
				if (field.get(null) == material) {
					name = field.getName();
					break;
				}
			}
		} catch (final Exception exception) {
			// no operation
		}
		return name;
	}
	
	@Nonnull
	public static String sanitizeFileName(@Nonnull final String name) {
		return name.replace("/", "")
		           .replace(".", "")
		           .replace(":", "")
		           .replace("\\", ".");
	}
	
	public static ItemStack copyWithSize(@Nonnull final ItemStack itemStack, final int newSize) {
		final ItemStack ret = itemStack.copy();
		ret.setCount(newSize);
		return ret;
	}
	
	
	// searching methods
	
	public static final EnumFacing[] FACINGS_VERTICAL = { EnumFacing.DOWN, EnumFacing.UP };
	public static final VectorI[] DIRECTIONS_UP_CONE = {
			// up
			new VectorI( 0,  1,  0),
			// horizontal
			new VectorI( 1,  0,  0),
			new VectorI( 0,  0,  1),
			new VectorI(-1,  0,  0),
			new VectorI( 0,  0, -1),
			// up & horizontal (see acacia wood)
			new VectorI( 1,  1,  0),
			new VectorI( 0,  1,  1),
			new VectorI(-1,  1,  0),
			new VectorI( 0,  1, -1),
			// up in diagonals (see dark oak wood)
			new VectorI( 1,  1,  1),
			new VectorI(-1,  1,  1),
			new VectorI(-1,  1, -1),
			new VectorI( 1,  1, -1) };
	public static final VectorI[] DIRECTIONS_HORIZONTAL = {
			new VectorI( 1,  0,  0),
			new VectorI( 0,  0,  1),
			new VectorI(-1,  0,  0),
			new VectorI( 0,  0, -1) };
	public static final VectorI[] DIRECTIONS_VERTICAL = {
			new VectorI( 0, -1,  0),
			new VectorI( 0,  1,  0) };
	public static final VectorI[] DIRECTIONS_ANY = {
			new VectorI( 0, -1,  0),
			new VectorI( 0,  1,  0),
			new VectorI( 1,  0,  0),
			new VectorI( 0,  0,  1),
			new VectorI(-1,  0,  0),
			new VectorI( 0,  0, -1) };
	
	public static Set<BlockPos> getConnectedBlocks(final World world, final BlockPos start, final VectorI[] directions, final Set<Block> whitelist, final int maxRange, final BlockPos... ignore) {
		return getConnectedBlocks(world, Collections.singletonList(start), directions, whitelist, maxRange, ignore);
	}
	
	public static Set<BlockPos> getConnectedBlocks(final World world, final Collection<BlockPos> start, final VectorI[] directions, final Set<Block> whitelist, final int maxRange, final BlockPos... ignore) {
		final Set<BlockPos> toIgnore = new HashSet<>();
		if (ignore != null) {
			toIgnore.addAll(Arrays.asList(ignore));
		}
		
		Set<BlockPos> toIterate = new HashSet<>(start);
		
		Set<BlockPos> toIterateNext;
		
		final Set<BlockPos> iterated = new HashSet<>();
		
		int range = 0;
		while(!toIterate.isEmpty() && range < maxRange) {
			toIterateNext = new HashSet<>();
			for (final BlockPos current : toIterate) {
				final IBlockState blockStateCurrent = getBlockState_noChunkLoading(world, current);
				if ( blockStateCurrent != null
				  && whitelist.contains(blockStateCurrent.getBlock()) ) {
					iterated.add(current);
				}
				
				for(final VectorI direction : directions) {
					final BlockPos next = new BlockPos(current.getX() + direction.x,
					                                   current.getY() + direction.y,
					                                   current.getZ() + direction.z );
					if (!iterated.contains(next) && !toIgnore.contains(next) && !toIterate.contains(next) && !toIterateNext.contains(next)) {
						final IBlockState blockStateNext = getBlockState_noChunkLoading(world, next);
						if ( blockStateNext != null
						  && whitelist.contains(blockStateNext.getBlock())) {
							toIterateNext.add(next);
						}
					}
				}
			}
			toIterate = toIterateNext;
			range++;
		}
		
		return iterated;
	}
	
	@Nonnull
	public static Set<BlockStatePos> getConnectedBlockStatePos(@Nonnull final IBlockAccess blockAccess, @Nonnull final Collection<BlockPos> start,
	                                                           @Nonnull final VectorI[] directions, @Nonnull final Set<Block> blockConnecting,
	                                                           @Nonnull final Set<Block> blockResults, final int maxRange) {
		Set<BlockPos> toIterate = new HashSet<>(start.size() * 4);
		final Set<BlockPos> blockPosIterated = new HashSet<>(64);
		final Set<BlockStatePos> blockStatePosResults = new HashSet<>(64);
		
		// preload the starting positions
		for (final BlockPos blockPos : start) {
			final IBlockState blockState = getBlockState_noChunkLoading(blockAccess, blockPos);
			if (blockState != null) {
				// always iterate starting position, even if it's not a connecting block
				toIterate.add(blockPos);
				// export starting position when they are also results
				if (blockResults.contains(blockState.getBlock())) {
					blockStatePosResults.add(new BlockStatePos(blockPos, blockState));
				}
			}
		}
		
		Set<BlockPos> toIterateNext;
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		
		int range = 0;
		while(!toIterate.isEmpty() && range < maxRange) {
			toIterateNext = new HashSet<>();
			for (final BlockPos current : toIterate) {
				for(final VectorI direction : directions) {
					mutableBlockPos.setPos(current.getX() + direction.x,
					                       current.getY() + direction.y,
					                       current.getZ() + direction.z );
					if (!blockPosIterated.contains(mutableBlockPos)) {
						// add to ignore list
						final BlockPos blockPosNext = mutableBlockPos.toImmutable();
						blockPosIterated.add(blockPosNext);
						
						final IBlockState blockStateNext = getBlockState_noChunkLoading(blockAccess, mutableBlockPos);
						if (blockStateNext != null) {
							// only iterate connecting blocks
							if (blockConnecting.contains(blockStateNext.getBlock())) {
								toIterateNext.add(blockPosNext);
							}
							// export results, even when not connecting
							if (blockResults.contains(blockStateNext.getBlock())) {
								blockStatePosResults.add(new BlockStatePos(blockPosNext, blockAccess.getBlockState(blockPosNext)));
							}
						}
					}
				}
			}
			toIterate = toIterateNext;
			range++;
		}
		return blockStatePosResults;
	}
	
	// data manipulation methods
	
	public static String toString(final Object object) {
		if (object == null) {
			return "null";
		}
		if (object instanceof String) {
			return (String) object;
		}
		return object.toString();
	}
	
	public static int toInt(final double d) {
		return (int) Math.round(d);
	}
	
	public static int toInt(final Object object) {
		return toInt(toDouble(object));
	}
	
	public static double toDouble(final Object object) {
		if (object == null) {
			return 0.0D;
		}
		assert !(object instanceof Object[]);
		return Double.parseDouble(object.toString());
	}
	
	public static float toFloat(final Object object) {
		if (object == null) {
			return 0.0F;
		}
		assert !(object instanceof Object[]);
		return Float.parseFloat(object.toString());
	}
	
	public static boolean toBool(final Object object) {
		if (object == null) {
			 return false;
		}
		assert !(object instanceof Object[]);
		if (object instanceof Boolean) {
			 return ((Boolean) object);
		}
		final String string = object.toString();
		return string.equals("true") || string.equals("1.0") || string.equals("1") || string.equals("y") || string.equals("yes");
	}
	
	public static int clamp(final int min, final int max, final int value) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static long clamp(final long min, final long max, final long value) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static float clamp(final float min, final float max, final float value) {
		return Math.min(max, Math.max(value, min));
	}
	
	public static double clamp(final double min, final double max, final double value) {
		return Math.min(max, Math.max(value, min));
	}
	
	// clamping while keeping the sign
	public static float clampMantisse(final float min, final float max, final float value) {
		return Math.min(max, Math.max(Math.abs(value), min)) * Math.signum(value == 0.0F ? 1.0F : value);
	}
	
	// clamping while keeping the sign
	public static double clampMantisse(final double min, final double max, final double value) {
		return Math.min(max, Math.max(Math.abs(value), min)) * Math.signum(value == 0.0D ? 1.0D : value);
	}
	
	public static int randomRange(final Random random, final int min, final int max) {
		return min + ((max - min > 0) ? random.nextInt(max - min + 1) : 0);
	}
	
	public static double randomRange(final Random random, final double min, final double max) {
		return min + ((max - min > 0) ? random.nextDouble() * (max - min) : 0);
	}
	
	
	// configurable interpolation
	
	public static double interpolate(@Nonnull final double[] xValues, @Nonnull final double[] yValues, final double xInput) {
		if (WarpDrive.isDev) {
			assert xValues.length == yValues.length;
			assert xValues.length > 1;
		}
		
		// clamp to minimum
		if (xInput < xValues[0]) {
			return yValues[0];
		}
		
		for (int index = 0; index < xValues.length - 1; index++) {
			if (xInput < xValues[index + 1]) {
				return interpolate(xValues[index], yValues[index], xValues[index + 1], yValues[index + 1], xInput);
			}
		}
		
		// clamp to maximum
		return yValues[yValues.length - 1];
	}
	
	public static double interpolate(final double xMin, final double yMin, final double xMax, final double yMax, final double x) {
		return yMin + (x - xMin) * (yMax - yMin) / (xMax - xMin);
	}
	
	public static EnumFacing getHorizontalDirectionFromEntity(@Nullable final EntityLivingBase entityLiving) {
		if (entityLiving != null) {
			final int direction = Math.round(entityLiving.rotationYaw / 90.0F) & 3;
			switch (direction) {
			default:
			case 0:
				return EnumFacing.NORTH;
			case 1:
				return EnumFacing.EAST;
			case 2:
				return EnumFacing.SOUTH;
			case 3:
				return EnumFacing.WEST;
			}
		}
		return EnumFacing.NORTH;
	}
	
	public static EnumFacing getFacingFromEntity(@Nullable final EntityLivingBase entityLivingBase) {
		if (entityLivingBase != null) {
			final EnumFacing facing;
			if (entityLivingBase.rotationPitch > 45) {
				facing = EnumFacing.UP;
			} else if (entityLivingBase.rotationPitch < -45) {
				facing = EnumFacing.DOWN;
			} else {
				final int direction = Math.round(entityLivingBase.rotationYaw / 90.0F) & 3;
				switch (direction) {
					case 0:
						facing = EnumFacing.NORTH;
						break;
					case 1:
						facing = EnumFacing.EAST;
						break;
					case 2:
						facing = EnumFacing.SOUTH;
						break;
					case 3:
						facing = EnumFacing.WEST;
						break;
					default:
						facing = EnumFacing.NORTH;
						break;
				}
			}
			if (entityLivingBase.isSneaking()) {
				return facing.getOpposite();
			}
			return facing;
		}
		return EnumFacing.UP;
	}
	
	private static final ConcurrentHashMap<String, Long> throttle_timePreviousForKey_ms = new ConcurrentHashMap<>(16);
	public static boolean throttleMe(final String keyword) {
		return throttleMe(keyword,WarpDriveConfig.LOGGING_THROTTLE_MS);
	}
	public static boolean throttleMe(final String keyword, final long delay_ms) {
		final Long timeLastLog_ms = throttle_timePreviousForKey_ms.getOrDefault(keyword, Long.MIN_VALUE);
		final long timeCurrent_ms = System.currentTimeMillis();
		if (timeCurrent_ms > timeLastLog_ms + delay_ms) {
			throttle_timePreviousForKey_ms.put(keyword, timeCurrent_ms);
			return true;
		}
		return false;
	}
	
	public static boolean isSafeThread() {
		final String name = Thread.currentThread().getName();
		return name.equals("Server thread") || name.equals("Client thread");
	}
	
	public static boolean isClientThread() {
		final String name = Thread.currentThread().getName();
		return name.equals("Client thread");
	}
	
	public static boolean isServerThread() {
		final String name = Thread.currentThread().getName();
		return name.equals("Server thread");
	}
	
	// loosely inspired by crunchify
	private static long dumpAllThreads_lastDump_ms = Long.MIN_VALUE;
	public static void dumpAllThreads() {
		// only dump once per second
		final long currentTime_ms = System.currentTimeMillis();
		if (dumpAllThreads_lastDump_ms + 1000L >= currentTime_ms) {
			return;
		}
		dumpAllThreads_lastDump_ms = currentTime_ms;
		
		final StringBuilder stringBuilder = new StringBuilder();
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
		for (final ThreadInfo threadInfo : threadInfos) {
			stringBuilder.append("\n\"");
			stringBuilder.append(threadInfo.getThreadName());
			stringBuilder.append("\"\n\tjava.lang.Thread.State: ");
			stringBuilder.append(threadInfo.getThreadState());
			final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
			for (final StackTraceElement stackTraceElement : stackTraceElements) {
				stringBuilder.append("\n\t\tat ");
				stringBuilder.append(stackTraceElement);
			}
			stringBuilder.append("\n");
		}
		WarpDrive.logger.error(stringBuilder.toString());
	}
	
	@Nonnull
	public static String getMethodName(final int depth) {
		try {
			final StackTraceElement stackTraceElement = (StackTraceElement) methodThrowable_getStackTraceElement.invoke(
					new Throwable(), depth + 1);
			return stackTraceElement.getMethodName();
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			return "-?-";
		}
	}
	
	public static void writeNBTToFile(@Nonnull final String fileName, @Nonnull final NBTTagCompound tagCompound) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.debug(String.format("writeNBTToFile %s",
			                                     fileName));
		}
		
		try {
			final File file = new File(fileName);
			if (!file.exists()) {
				//noinspection ResultOfMethodCallIgnored
				file.createNewFile();
			}
			
			final FileOutputStream fileoutputstream = new FileOutputStream(file);
			
			CompressedStreamTools.writeCompressed(tagCompound, fileoutputstream);
			
			fileoutputstream.close();
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
	}
	
	public static NBTTagCompound readNBTFromFile(@Nonnull final String fileName) {
		if (WarpDrive.isDev) {
			WarpDrive.logger.debug(String.format("readNBTFromFile %s",
			                                     fileName));
		}
		
		try {
			final File file = new File(fileName);
			if (!file.exists()) {
				return null;
			}
			
			final FileInputStream fileinputstream = new FileInputStream(file);
			final NBTTagCompound tagCompound = CompressedStreamTools.readCompressed(fileinputstream);
			
			fileinputstream.close();
			
			return tagCompound;
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		
		return null;
	}
	
	public static BlockPos createBlockPosFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		final int x = tagCompound.getInteger("x");
		final int y = tagCompound.getInteger("y");
		final int z = tagCompound.getInteger("z");
		return new BlockPos(x, y, z);
	}
	
	public static NBTTagCompound writeBlockPosToNBT(@Nonnull final BlockPos blockPos, @Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setInteger("x", blockPos.getX());
		tagCompound.setInteger("y", blockPos.getY());
		tagCompound.setInteger("z", blockPos.getZ());
		return tagCompound;
	}
	
	public static EntityPlayerMP[] getOnlinePlayerByNameOrSelector(@Nonnull final ICommandSender commandSender, final String playerNameOrSelector) {
		final MinecraftServer server = commandSender.getServer();
		assert server != null;
		final List<EntityPlayerMP> onlinePlayers = server.getPlayerList().getPlayers();
		for (final EntityPlayerMP onlinePlayer : onlinePlayers) {
			if (onlinePlayer.getName().equalsIgnoreCase(playerNameOrSelector)) {
				return new EntityPlayerMP[] { onlinePlayer };
			}
		}
		
		try {
			final List<EntityPlayerMP> entityPlayerMPs_found = EntitySelector.matchEntities(commandSender, playerNameOrSelector, EntityPlayerMP.class);
			if (!entityPlayerMPs_found.isEmpty()) {
				return entityPlayerMPs_found.toArray(new EntityPlayerMP[0]);
			}
		} catch (final CommandException exception) {
			WarpDrive.logger.error(String.format("Exception from %s with selector %s",
			                                     commandSender, playerNameOrSelector));
		}
		
		return null;
	}
	
	@Nullable
	public static EntityPlayerMP getOnlinePlayerByName(final String namePlayer) {
		final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		assert server != null;
		return server.getPlayerList().getPlayerByUsername(namePlayer);
	}
	
	@Nullable
	public static EntityPlayerMP getOnlinePlayerByUUID(@Nonnull final UUID uuidPlayer) {
		final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		assert server != null;
		return server.getPlayerList().getPlayerByUUID(uuidPlayer);
	}
	
	public interface ProfilePropertiesAvailableCallback {
		void profilePropertiesAvailable(@Nonnull final GameProfile gameProfile);
	}
	
	@Nonnull
	public static GameProfile getGameProfile(@Nonnull final UUID uuidPlayer, @Nonnull final String namePlayer,
	                                         final ProfilePropertiesAvailableCallback profilePropertiesAvailableCallback) {
		// validate context
		if ( TileEntitySkull.profileCache == null
		  || TileEntitySkull.sessionService == null ) {
			return new GameProfile(uuidPlayer, namePlayer);
		}
		
		// check the cache first
		final GameProfile gameProfileCached = TileEntitySkull.profileCache.getProfileByUUID(uuidPlayer);
		if ( gameProfileCached != null
		  && !gameProfileCached.getProperties().isEmpty() ) {
			if (profilePropertiesAvailableCallback != null) {
				profilePropertiesAvailableCallback.profilePropertiesAvailable(gameProfileCached);
			}
			return gameProfileCached;
		}
		final GameProfile gameProfileEmpty = gameProfileCached != null ? gameProfileCached : new GameProfile(uuidPlayer, namePlayer);
		// return directly if properties aren't required or filling is already ongoing
		if ( profilePropertiesAvailableCallback == null
		  || uuidGameProfileFilling.contains(uuidPlayer) ) {
			return gameProfileEmpty;
		}
		
		// return an empty profile while a thread will query the server
		uuidGameProfileFilling.add(uuidPlayer);
		THREAD_POOL.submit(() -> {
			final GameProfile gameProfileFilled = TileEntitySkull.sessionService.fillProfileProperties(gameProfileEmpty, true);
			
			// return to main thread before updating our cache and informing caller
			Minecraft.getMinecraft().addScheduledTask(() -> {
				if (gameProfileEmpty != gameProfileFilled) {// when successful, a new object is created, so we might as well use it
					TileEntitySkull.profileCache.addEntry(gameProfileFilled);
				}
				profilePropertiesAvailableCallback.profilePropertiesAvailable(gameProfileFilled);
				uuidGameProfileFilling.remove(uuidPlayer);
			});
		});
		
		return gameProfileEmpty;
	}
	
	public static int colorARGBtoInt(final int alpha, final int red, final int green, final int blue) {
		return (clamp(0, 255, alpha) << 24)
		     + (clamp(0, 255, red  ) << 16)
			 + (clamp(0, 255, green) <<  8)
			 +  clamp(0, 255, blue );
	}
	
	public static void messageToAllPlayersInArea(@Nonnull final IGlobalRegionProvider globalRegionProvider, @Nonnull final WarpDriveText textComponent) {
		final AxisAlignedBB globalRegionArea = globalRegionProvider.getGlobalRegionArea();
		final WarpDriveText messagePrefixed = Commons.getNamedPrefix(globalRegionProvider.getSignatureName())
		                                             .appendSibling(textComponent);
		
		WarpDrive.logger.info(String.format("%s messageToAllPlayersInArea: %s",
		                                    globalRegionProvider, textComponent.getFormattedText()));
		final WorldServer worldServer = Commons.getOrCreateWorldServer(globalRegionProvider.getDimension());
		for (final EntityPlayer entityPlayer : worldServer.playerEntities) {
			if (!entityPlayer.getEntityBoundingBox().intersects(globalRegionArea)) {
				continue;
			}
			
			Commons.addChatMessage(entityPlayer, messagePrefixed);
		}
	}
	
	public static void moveEntity(@Nonnull final Entity entity, @Nonnull final World worldDestination, @Nonnull final Vector3 v3Destination) {
		if (entity.world.isRemote) {
			WarpDrive.logger.error(String.format("Skipping remote movement for entity %s destination %s",
			                                     entity, Commons.format(worldDestination, v3Destination) ));
			return;
		}
		if (!entity.isEntityAlive()) {
			WarpDrive.logger.warn(String.format("Skipping movement for dead entity %s destination %s",
			                                    entity, Commons.format(worldDestination, v3Destination) ));
			return;
		}
		
		// change to another dimension if needed
		if (worldDestination != entity.world) {
			final World worldSource = entity.world;
			final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			final WorldServer worldServerFrom = getOrCreateWorldServer(worldSource.provider.getDimension());
			final WorldServer worldServerTo   = getOrCreateWorldServer(worldDestination.provider.getDimension());
			final SpaceTeleporter teleporter = new SpaceTeleporter(worldServerTo, v3Destination);
			
			if (entity instanceof EntityPlayerMP) {
				final EntityPlayerMP player = (EntityPlayerMP) entity;
				server.getPlayerList().transferPlayerToDimension(player, worldDestination.provider.getDimension(), teleporter);
				player.invulnerableDimensionChange = true;
				player.connection.captureCurrentPosition();
			} else {
				server.getPlayerList().transferEntityToWorld(entity, worldSource.provider.getDimension(), worldServerFrom, worldServerTo, teleporter);
			}
			
		} else {// just update position
			if (entity instanceof EntityPlayerMP) {
				final EntityPlayerMP player = (EntityPlayerMP) entity;
				player.setPositionAndUpdate(v3Destination.x, v3Destination.y, v3Destination.z);
			} else {
				entity.setLocationAndAngles(v3Destination.x, v3Destination.y, v3Destination.z, entity.rotationYaw, entity.rotationPitch);
				worldDestination.updateEntityWithOptionalForce(entity, false);
			}
		}
	}
	
	public static WorldServer getOrCreateWorldServer(final int dimensionId) {
		WorldServer worldServer = DimensionManager.getWorld(dimensionId);
		
		if (worldServer == null) {
			try {
				final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
				worldServer = server.getWorld(dimensionId);
				if (worldServer.provider.getDimension() != dimensionId) {
					throw new RuntimeException(String.format("Inconsistent dimension id %d, expecting %d",
					                                         worldServer.provider.getDimension(), dimensionId ));
				}
			} catch (final Exception exception) {
				WarpDrive.logger.error(String.format("%s: Failed to initialize dimension %d",
				                                     exception.getMessage(),
				                                     dimensionId));
				if (WarpDrive.isDev) {
					exception.printStackTrace(WarpDrive.printStreamError);
				}
				worldServer = null;
			}
		}
		
		return worldServer;
	}
	
	// server side version of EntityLivingBase.rayTrace
	private static final double BLOCK_REACH_DISTANCE = 5.0D;    // this is a client side hardcoded value, applicable to creative players
	public static RayTraceResult getInteractingBlock(@Nonnull final World world, @Nonnull final EntityPlayer entityPlayer) {
		return getInteractingBlock(world, entityPlayer, BLOCK_REACH_DISTANCE);
	}
	public static RayTraceResult getInteractingBlock(@Nonnull final World world, @Nonnull final EntityPlayer entityPlayer, final double distance) {
		final Vec3d vec3Position = new Vec3d(entityPlayer.posX, entityPlayer.posY + entityPlayer.eyeHeight, entityPlayer.posZ);
		final Vec3d vec3Look = entityPlayer.getLook(1.0F);
		final Vec3d vec3Target = vec3Position.add(vec3Look.x * distance, vec3Look.y * distance, vec3Look.z * distance);
		return world.rayTraceBlocks(vec3Position, vec3Target, false, false, true);
	}
	
	// Fluid registry fix
	// As of MC1.7.10 CoFH is remapping blocks without updating the fluid registry
	// This imply that call to FluidRegistry.lookupFluidForBlock() for Water and Lava will return null
	// We're remapping it using unlocalized names, since those don't change
	private static HashMap<String, Fluid> fluidByBlockName;
	
	public static Fluid fluid_getByBlock(@Nonnull final Block block) {
		// validate context
		if (!(block instanceof BlockLiquid)) {
//			if (WarpDrive.isDev) {
				WarpDrive.logger.warn(String.format("Invalid lookup for fluid block not derived from BlockLiquid %s",
				                      block));
//			}
			return null;
		}
		
		//  build cache on first call
		if (fluidByBlockName == null) {
			final Map<String, Fluid> fluidsRegistry = FluidRegistry.getRegisteredFluids();
			final HashMap<String, Fluid> map = new HashMap<>(100);
			for (final Fluid fluid : fluidsRegistry.values()) {
				final Block blockFluid = fluid.getBlock();
				if (blockFluid != null) {
					map.put(blockFluid.getTranslationKey(), fluid);
				}
			}
			fluidByBlockName = map;
		}
		// final Fluid fluid = FluidRegistry.lookupFluidForBlock(blockState.getBlock()); @TODO MC1.10 fluid detection
		return fluidByBlockName.get(block.getTranslationKey());
	}
	
	public static EnumFacing getDirection(final int index) {
		if (index < 0 || index > 5) {
			return null;
		}
		return EnumFacing.byIndex(index);
	}
	
	public static int getOrdinal(@Nullable final EnumFacing direction) {
		if (direction == null) {
			return 6;
		}
		return direction.ordinal();
	}
	
	public static boolean isValidCamouflage(@Nullable final IBlockState blockState) {
		// fast check
		if ( blockState == null
		  || blockState.getBlock() == Blocks.AIR
		  || !ALLOWED_RENDER_TYPES.contains(blockState.getRenderType())
		  || Dictionary.BLOCKS_NOCAMOUFLAGE.contains(blockState.getBlock()) ) {
			return false;
		}
		
		if (blockState instanceof IExtendedBlockState) {
			final IExtendedBlockState extendedBlockState = (IExtendedBlockState) blockState;
			// own camouflage blocks
			if (extendedBlockState.getUnlistedProperties().containsKey(BlockProperties.CAMOUFLAGE)) {// failed: add it to the fast check
				extendedBlockState.getValue(BlockProperties.CAMOUFLAGE);
				// failed: add it to the fast check
				WarpDrive.logger.error(String.format("Recursive camouflage block detected for block state %s, updating dictionary with %s = NOCAMOUFLAGE to prevent further errors",
				                                     blockState,
				                                     blockState.getBlock().getRegistryName()));
				Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockState.getBlock());
				return false;
			}
			// other mods camouflage blocks
			for (final IUnlistedProperty<?> property : extendedBlockState.getUnlistedNames()) {
				if (property.getType().toString().contains("IBlockState")) {// failed: add it to the fast check
					WarpDrive.logger.error(String.format("Suspicious camouflage block detected for block state %s, updating dictionary with %s = NOCAMOUFLAGE to prevent further errors",
					                                     blockState,
					                                     blockState.getBlock().getRegistryName()));
					Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockState.getBlock());
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean isChunkLoaded(final IBlockAccess blockAccess, final int x, final int z) {
		if (blockAccess instanceof WorldServer) {
			if (isSafeThread()) {
				return ChunkHandler.isLoaded((WorldServer) blockAccess, x, 64, z);
			} else {
				final ChunkProviderServer chunkProviderServer = ((WorldServer) blockAccess).getChunkProvider();
				final Chunk chunk = chunkProviderServer.getLoadedChunk(x >> 4, z >> 4);
				return chunk != null && chunk.isLoaded();
			}
		}
		return true;
	}
	
	public static IBlockState getBlockState_noChunkLoading(@Nullable final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		// skip unloaded worlds
		if (blockAccess == null) {
			return null;
		}
		// skip unloaded chunks
		if (!isChunkLoaded(blockAccess, blockPos.getX(), blockPos.getZ())) {
			return null;
		}
		return blockAccess.getBlockState(blockPos);
	}
	
	public static boolean isReplaceableOreGen(@Nonnull final World world, @Nonnull final BlockPos blockPos) {
		final IBlockState blockStateActual = world.getBlockState(blockPos);
		final Block blockActual = blockStateActual.getBlock();
		return blockActual.isReplaceableOreGen(blockStateActual, world, blockPos,
		                                       blockState -> blockState != null
		                                                     && ( blockState.getBlock() == Blocks.AIR
		                                                       || blockState.getBlock() == Blocks.STONE
		                                                       || blockState.getBlock() == Blocks.NETHERRACK
		                                                       || blockState.getBlock() == Blocks.END_STONE ) );
	}
}
