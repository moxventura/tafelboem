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

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.AnswerRecord;

/**
 * One JSON line per answer in {@code <world>/tafelboem/answers-<player>.jsonl}. Local only, so a
 * parent can open it in a spreadsheet; nothing is sent anywhere.
 */
public final class AnswerLog {
	private static final Gson GSON = new Gson();

	private final Path directory;

	public AnswerLog(Path directory) {
		this.directory = directory;
	}

	public void append(AnswerRecord record) {
		try {
			Files.createDirectories(directory);
			Files.writeString(file(record.player()), GSON.toJson(record) + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			TafelBoem.LOGGER.error("Could not write answer log for {}", record.player(), e);
		}
	}

	public List<AnswerRecord> read(String player) {
		Path file = file(player);
		List<AnswerRecord> records = new ArrayList<>();
		if (!Files.exists(file)) {
			return records;
		}

		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (line.isBlank()) {
					continue;
				}
				try {
					records.add(GSON.fromJson(line, AnswerRecord.class));
				} catch (JsonParseException e) {
					TafelBoem.LOGGER.warn("Skipping unreadable answer log line for {}: {}", player, line);
				}
			}
		} catch (IOException e) {
			TafelBoem.LOGGER.error("Could not read answer log for {}", player, e);
		}
		return records;
	}

	private Path file(String player) {
		return directory.resolve("answers-" + player.replaceAll("[^A-Za-z0-9_-]", "_") + ".jsonl");
	}
}
