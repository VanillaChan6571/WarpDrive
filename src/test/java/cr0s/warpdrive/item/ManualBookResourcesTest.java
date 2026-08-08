package cr0s.warpdrive.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Static contract checks for the 1.16 Patchouli resource tree. */
public class ManualBookResourcesTest {

	private static final Path RESOURCES = Paths.get("src", "main", "resources");
	private static final Path BOOK = RESOURCES.resolve(Paths.get(
		"data", "warpdrive", "patchouli_books", "warpdrive_manual"));
	private static final Pattern BOOK_LINK = Pattern.compile("\\$\\(l:([^\\)]+)\\)");
	private static final Set<String> PAGE_TYPES = new HashSet<>(Arrays.asList(
		"text", "spotlight", "smelting", "crafting", "image"));

	@Test
	public void bookDefinitionUsesTheCompatibilityItem() throws IOException {
		final JsonObject book = readJson(BOOK.resolve("book.json"));
		assertTrue(book.get("dont_generate_book").getAsBoolean());
		assertEquals("warpdrive:book", book.get("custom_book_item").getAsString());
		assertEquals("warpdrive:book", book.get("model").getAsString());
		assertEquals("warpdrive", book.get("creative_tab").getAsString());

		final JsonObject recipe = readJson(RESOURCES.resolve(Paths.get(
			"data", "warpdrive", "recipes", "book.json")));
		assertEquals("forge:conditional", recipe.get("type").getAsString());
		final JsonObject condition = recipe.getAsJsonArray("recipes").get(0).getAsJsonObject()
			.getAsJsonArray("conditions").get(0).getAsJsonObject();
		assertEquals("forge:mod_loaded", condition.get("type").getAsString());
		assertEquals("patchouli", condition.get("modid").getAsString());
	}

	@Test
	public void localizedTreesMirrorAndAllJsonParses() throws IOException {
		final Set<String> english = relativeJsonFiles(BOOK.resolve("en_us"));
		final Set<String> chinese = relativeJsonFiles(BOOK.resolve("zh_cn"));
		assertEquals(english, chinese);
		assertEquals(32, english.size());
		assertEquals(65, relativeJsonFiles(BOOK).size());
	}

	@Test
	public void allManualReferencesResolve() throws IOException {
		validateLocale("en_us");
		validateLocale("zh_cn");
	}

	private static void validateLocale(final String locale) throws IOException {
		final Path localeRoot = BOOK.resolve(locale);
		final Path entriesRoot = localeRoot.resolve("entries");
		final Set<String> entries = relativeJsonFiles(entriesRoot).stream()
			.map(path -> path.substring(0, path.length() - ".json".length()))
			.collect(Collectors.toSet());

		for (final String relative : relativeJsonFiles(localeRoot)) {
			final Path file = localeRoot.resolve(relative);
			final String raw = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			final JsonObject json = new JsonParser().parse(raw).getAsJsonObject();

			if (json.has("icon")) validateItemReference(json.get("icon").getAsString(), file);
			if (relative.startsWith("entries/")) {
				assertTrue("Missing category for " + file, json.has("category"));
				final Path category = localeRoot.resolve(Paths.get(
					"categories", json.get("category").getAsString() + ".json"));
				assertTrue("Unknown category in " + file + ": " + category, Files.isRegularFile(category));
			}

			if (json.has("pages")) {
				for (final JsonElement element : json.getAsJsonArray("pages")) {
					final JsonObject page = element.getAsJsonObject();
					assertTrue("Unsupported page type in " + file,
						PAGE_TYPES.contains(page.get("type").getAsString()));
					if (page.has("item")) validateItemReference(page.get("item").getAsString(), file);
					validateRecipe(page, "recipe", file);
					validateRecipe(page, "recipe2", file);
					validateImages(page, file);
				}
			}

			final Matcher matcher = BOOK_LINK.matcher(raw);
			while (matcher.find()) {
				final String target = matcher.group(1);
				if (!target.startsWith("http://") && !target.startsWith("https://")) {
					assertTrue("Broken manual link in " + file + ": " + target, entries.contains(target));
				}
			}
		}
	}

	private static void validateItemReference(final String reference, final Path source) {
		if (!reference.startsWith("warpdrive:")) return;
		String path = reference.substring("warpdrive:".length());
		final int nbtStart = path.indexOf('{');
		if (nbtStart >= 0) path = path.substring(0, nbtStart);
		assertFalse("Legacy metadata item reference in " + source + ": " + reference,
			path.contains(":") || path.contains("@"));
		assertTrue("Missing item model in " + source + ": " + reference,
			Files.isRegularFile(RESOURCES.resolve(Paths.get(
				"assets", "warpdrive", "models", "item", path + ".json"))));
	}

	private static void validateRecipe(final JsonObject page, final String property,
	                                   final Path source) {
		if (!page.has(property)) return;
		final String recipe = page.get(property).getAsString();
		assertTrue("Manual recipe is not namespaced in " + source + ": " + recipe,
			recipe.startsWith("warpdrive:"));
		final String path = recipe.substring("warpdrive:".length());
		assertFalse("Legacy recipe id in " + source + ": " + recipe,
			path.contains(":") || path.contains("@"));
		assertTrue("Missing manual recipe in " + source + ": " + recipe,
			Files.isRegularFile(RESOURCES.resolve(Paths.get(
				"data", "warpdrive", "recipes", path + ".json"))));
	}

	private static void validateImages(final JsonObject page, final Path source) {
		if (!page.has("images")) return;
		final JsonArray images = page.getAsJsonArray("images");
		for (final JsonElement element : images) {
			final String image = element.getAsString();
			if (!image.startsWith("warpdrive:")) continue;
			assertTrue("Missing manual image in " + source + ": " + image,
				Files.isRegularFile(RESOURCES.resolve(Paths.get(
					"assets", "warpdrive", image.substring("warpdrive:".length())))));
		}
	}

	private static Set<String> relativeJsonFiles(final Path root) throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			return paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".json"))
				.peek(path -> {
					try {
						readJson(path);
					} catch (final IOException exception) {
						throw new RuntimeException(exception);
					}
				})
				.map(path -> root.relativize(path).toString().replace('\\', '/'))
				.collect(Collectors.toSet());
		}
	}

	private static JsonObject readJson(final Path path) throws IOException {
		return new JsonParser().parse(new String(
			Files.readAllBytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
	}
}
