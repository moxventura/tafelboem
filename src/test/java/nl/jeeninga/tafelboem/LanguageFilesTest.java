package nl.jeeninga.tafelboem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.jupiter.api.Test;

/**
 * Dutch and English must stay in sync, including the numbers each message expects.
 */
class LanguageFilesTest {
	private static final Path LANG = Path.of("src/main/resources/assets/tafelboem/lang");
	private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?s");

	@Test
	void dutchAndEnglishHaveTheSameKeys() throws IOException {
		assertEquals(read("en_us.json").keySet(), read("nl_nl.json").keySet());
	}

	@Test
	void dutchAndEnglishUseTheSameArguments() throws IOException {
		Map<String, String> english = read("en_us.json");
		Map<String, String> dutch = read("nl_nl.json");
		for (String key : english.keySet()) {
			assertEquals(arguments(english.get(key)), arguments(dutch.get(key)), key);
		}
	}

	/** The set of argument positions a message uses, so "%s %s" and "%1$s %2$s" compare equal. */
	private static TreeSet<Integer> arguments(String message) {
		TreeSet<Integer> used = new TreeSet<>();
		Matcher matcher = PLACEHOLDER.matcher(message);
		int next = 1;
		while (matcher.find()) {
			used.add(matcher.group(1) == null ? next++ : Integer.parseInt(matcher.group(1).replace("$", "")));
		}
		return used;
	}

	private static Map<String, String> read(String file) throws IOException {
		Map<String, String> entries = new Gson().fromJson(Files.readString(LANG.resolve(file)), new TypeToken<Map<String, String>>() { }.getType());
		return new TreeMap<>(entries);
	}
}
