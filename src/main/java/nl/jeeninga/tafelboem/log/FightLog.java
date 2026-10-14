package nl.jeeninga.tafelboem.log;

import java.nio.file.Path;
import java.util.List;

import nl.jeeninga.tafelboem.core.FightRecord;

/**
 * One JSON line per fight per kid in {@code <world>/tafelboem/fights-<player>.jsonl}: the numbers
 * for the evaluation, and where the stars at the doors come from.
 */
public final class FightLog {
	private final JsonLines<FightRecord> lines;

	public FightLog(Path directory) {
		this.lines = new JsonLines<>(directory, "fights", FightRecord.class);
	}

	public void append(FightRecord record) {
		lines.append(record.player(), record);
	}

	public List<FightRecord> read(String player) {
		return lines.read(player);
	}
}
