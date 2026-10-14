package nl.jeeninga.tafelboem.log;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import nl.jeeninga.tafelboem.core.AnswerRecord;

/**
 * One JSON line per answer in {@code <world>/tafelboem/answers-<player>.jsonl}. Local only, so a
 * parent can open it in a spreadsheet; nothing is sent anywhere.
 */
public final class AnswerLog {
	private final JsonLines<AnswerRecord> lines;

	public AnswerLog(Path directory) {
		this.lines = new JsonLines<>(directory, "answers", AnswerRecord.class);
	}

	public void append(AnswerRecord record) {
		lines.append(record.player(), record);
	}

	public List<AnswerRecord> read(String player) {
		return lines.read(player);
	}

	/** Hears every answer appended from now on. */
	public void listen(Consumer<AnswerRecord> listener) {
		lines.listen(listener);
	}
}
