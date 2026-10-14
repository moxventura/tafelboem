package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * One kid's play since joining the world, written as a single line to the game log when they leave.
 * The parent shares that log after a playtest, so the evaluation's numbers are all in it.
 */
public final class PlaySummary {
	private final long joinedAt;
	private long firstAnswer = -1;
	private long lastAnswer = -1;
	private int answers;
	private int correct;
	private int dontKnow;
	private int chickenOut;
	private final List<FightRecord> fights = new ArrayList<>();

	/** @param joinedAt when the kid joined, in epoch millis; -1 when unknown */
	public PlaySummary(long joinedAt) {
		this.joinedAt = joinedAt;
	}

	public void answered(long timestamp, Outcome outcome) {
		if (firstAnswer < 0) {
			firstAnswer = timestamp;
		}
		lastAnswer = timestamp;
		answers++;
		switch (outcome) {
			case CORRECT -> correct++;
			case DONT_KNOW -> dontKnow++;
			case CHICKEN_OUT -> chickenOut++;
			default -> {
			}
		}
	}

	public void fought(FightRecord fight) {
		fights.add(fight);
	}

	public boolean isEmpty() {
		return answers == 0 && fights.isEmpty();
	}

	/**
	 * Answers per minute count from the first answer to the last, so time spent in the hub picking a
	 * door doesn't drag the number down; the same way R0.1's evaluation counted it.
	 */
	public String describe(String player, long now) {
		StringBuilder line = new StringBuilder(player).append(": ");
		if (joinedAt >= 0) {
			line.append(String.format(Locale.ROOT, "%.1f min in the world, ", (now - joinedAt) / 60_000.0));
		}
		line.append(answers).append(answers == 1 ? " answer" : " answers");
		if (answers > 1 && lastAnswer > firstAnswer) {
			double playMinutes = (lastAnswer - firstAnswer) / 60_000.0;
			line.append(String.format(Locale.ROOT, " in %.1f min of play (%.1f per minute)", playMinutes, answers / playMinutes));
		}
		if (answers > 0) {
			line.append(", ").append(Math.round(100f * correct / answers)).append("% right, ")
					.append(dontKnow).append(" \"?\", ").append(chickenOut).append(" ESC");
		}
		line.append("; ").append(fights.isEmpty() ? "no fights"
				: "fights: " + fights.stream().map(PlaySummary::shortly).collect(Collectors.joining(", ")));
		return line.toString();
	}

	private static String shortly(FightRecord fight) {
		return "level " + fight.level() + " " + fight.result() + (fight.won() ? " " + "★".repeat(fight.stars()) : "");
	}
}
