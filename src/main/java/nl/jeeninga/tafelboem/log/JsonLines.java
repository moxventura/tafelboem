package nl.jeeninga.tafelboem.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One JSON line per record in {@code <prefix>-<player>.jsonl}. Local only, so a parent can open it
 * in a spreadsheet; nothing is sent anywhere.
 */
final class JsonLines<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger("tafelboem");
	private static final Gson GSON = new Gson();

	private final Path directory;
	private final String prefix;
	private final Class<T> type;

	JsonLines(Path directory, String prefix, Class<T> type) {
		this.directory = directory;
		this.prefix = prefix;
		this.type = type;
	}

	void append(String player, T record) {
		try {
			Files.createDirectories(directory);
			Files.writeString(file(player), GSON.toJson(record) + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOGGER.error("Could not write the {} log for {}", prefix, player, e);
		}
	}

	List<T> read(String player) {
		Path file = file(player);
		List<T> records = new ArrayList<>();
		if (!Files.exists(file)) {
			return records;
		}

		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (line.isBlank()) {
					continue;
				}
				T record = parse(line);
				if (record != null) {
					records.add(record);
				} else {
					LOGGER.warn("Skipping an unreadable {} log line for {}: {}", prefix, player, line);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Could not read the {} log for {}", prefix, player, e);
		}
		return records;
	}

	/** The record on this line, or null when the line is broken or a literal {@code null}. */
	private @Nullable T parse(String line) {
		try {
			return GSON.fromJson(line, type);
		} catch (JsonParseException e) {
			return null;
		}
	}

	private Path file(String player) {
		return directory.resolve(prefix + "-" + player.replaceAll("[^A-Za-z0-9_-]", "_") + ".jsonl");
	}
}
