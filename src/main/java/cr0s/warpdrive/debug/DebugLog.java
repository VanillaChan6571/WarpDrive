package cr0s.warpdrive.debug;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Dedicated debug log for WarpDrive, written to logs/warpdrive-debug.log.
 *
 * Kept separate from the main Minecraft log for two reasons: the detail level needed to debug
 * client/server sync and jump NBT handling is far too noisy for latest.log, and a single
 * self-contained file is much easier to attach to a bug report.
 *
 * Every line records the wall clock, the logical side, and the thread. Side and thread matter a
 * lot here: most of the bugs found so far were "this ran on the wrong side" or "this ran on the
 * computer thread instead of the server thread".
 */
public final class DebugLog {

	private static final String FILE_NAME = "warpdrive-debug.log";
	private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("HH:mm:ss.SSS");
	private static final Object LOCK = new Object();

	private static volatile boolean enabled = true;
	private static Path logFile = null;
	private static boolean broken = false;

	private DebugLog() {
	}

	/** Called once from the mod constructor. Rotates the previous session's file. */
	public static void init() {
		synchronized (LOCK) {
			try {
				final Path logs = FMLPaths.GAMEDIR.get().resolve("logs");
				Files.createDirectories(logs);
				final Path target = logs.resolve(FILE_NAME);

				// Keep exactly one previous session so a report can cover "it worked last launch"
				if (Files.exists(target)) {
					Files.move(target, logs.resolve(FILE_NAME + ".1"), StandardCopyOption.REPLACE_EXISTING);
				}
				logFile = target;
				writeHeader();
				WarpDrive.logger.info("WarpDrive debug log: {}", logFile);
			} catch (final Exception exception) {
				broken = true;
				WarpDrive.logger.warn("Unable to open the WarpDrive debug log, continuing without it", exception);
			}
		}
	}

	public static boolean isEnabled() {
		return enabled && !broken;
	}

	public static void setEnabled(final boolean value) {
		enabled = value;
		log("DEBUG", "Debug logging {}", value ? "ENABLED" : "DISABLED");
	}

	@Nullable
	public static Path getLogFile() {
		return logFile;
	}

	/** Log with a category tag, using log4j-style {} placeholders. */
	public static void log(final String category, final String format, final Object... args) {
		write(category, null, format, args);
	}

	/** As above, but tags the line CLIENT or SERVER from the given world. */
	public static void logSided(@Nullable final World level, final String category,
	                            final String format, final Object... args) {
		write(category, level == null ? "?" : (level.isClientSide ? "CLIENT" : "SERVER"), format, args);
	}

	/** Multi-line blocks (state dumps) stay readable by indenting continuation lines. */
	public static void logBlock(final String category, final String title, final String body) {
		if (!isEnabled()) {
			return;
		}
		final StringBuilder builder = new StringBuilder(title).append('\n');
		for (final String line : body.split("\n")) {
			builder.append("        ").append(line).append('\n');
		}
		write(category, null, builder.toString().trim());
	}

	private static void write(final String category, @Nullable final String side,
	                          final String format, final Object... args) {
		if (!isEnabled()) {
			return;
		}
		final String line = String.format("[%s] [%s] [%s] %s%n",
			TIMESTAMP.format(new Date()),
			side == null ? Thread.currentThread().getName() : side + "/" + Thread.currentThread().getName(),
			category,
			expand(format, args));
		append(line);
	}

	/** Minimal log4j-style {} substitution so existing call sites port over unchanged. */
	private static String expand(final String format, final Object... args) {
		if (args == null || args.length == 0) {
			return format;
		}
		final StringBuilder builder = new StringBuilder();
		int argIndex = 0;
		int cursor = 0;
		while (cursor < format.length()) {
			final int marker = format.indexOf("{}", cursor);
			if (marker < 0 || argIndex >= args.length) {
				builder.append(format, cursor, format.length());
				break;
			}
			builder.append(format, cursor, marker).append(stringify(args[argIndex++]));
			cursor = marker + 2;
		}
		// Anything left over (e.g. a trailing throwable) still gets recorded
		while (argIndex < args.length) {
			builder.append(' ').append(stringify(args[argIndex++]));
		}
		return builder.toString();
	}

	private static String stringify(@Nullable final Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Throwable) {
			final Throwable throwable = (Throwable) value;
			final StringBuilder builder = new StringBuilder(throwable.toString());
			for (final StackTraceElement element : throwable.getStackTrace()) {
				builder.append("\n            at ").append(element);
			}
			return builder.toString();
		}
		return String.valueOf(value);
	}

	private static void append(final String line) {
		synchronized (LOCK) {
			if (logFile == null || broken) {
				return;
			}
			try {
				Files.write(logFile, line.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (final IOException exception) {
				broken = true;
				WarpDrive.logger.warn("WarpDrive debug log write failed, disabling it", exception);
			}
		}
	}

	private static void writeHeader() {
		final StringBuilder builder = new StringBuilder();
		builder.append("=== WarpDrive debug log ===").append('\n');
		builder.append("started    : ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append('\n');
		builder.append("warpdrive  : ").append(modVersion("warpdrive")).append('\n');
		builder.append("forge      : ").append(modVersion("forge")).append('\n');
		builder.append("cc-tweaked : ").append(modVersion("computercraft")).append('\n');
		builder.append("java       : ").append(System.getProperty("java.version"))
			.append(" (class file target matters: MC 1.16.5 needs Java 8 bytecode)").append('\n');
		builder.append("os         : ").append(System.getProperty("os.name")).append('\n');
		builder.append("===========================").append('\n');
		append(builder.toString());
	}

	private static String modVersion(final String modId) {
		try {
			return ModList.get().getModContainerById(modId)
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse("not present");
		} catch (final Exception exception) {
			return "unknown";
		}
	}
}
