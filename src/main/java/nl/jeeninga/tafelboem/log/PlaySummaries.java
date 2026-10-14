package nl.jeeninga.tafelboem.log;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.PlaySummary;

/**
 * Each kid's {@link PlaySummary} while they're in the world. It hears every answer and fight from
 * the logs; the line goes to the game log when the kid leaves, which includes closing the world.
 */
public final class PlaySummaries {
	private final Map<String, PlaySummary> summaries = new HashMap<>();

	public void joined(String player, long now) {
		summaries.put(player, new PlaySummary(now));
	}

	public void answered(AnswerRecord answer) {
		summary(answer.player()).answered(answer.timestamp(), answer.outcome());
	}

	public void fought(FightRecord fight) {
		summary(fight.player()).fought(fight);
	}

	/** This kid's line, if they answered or fought anything; either way they are forgotten. */
	public Optional<String> left(String player, long now) {
		PlaySummary summary = summaries.remove(player);
		return summary == null || summary.isEmpty() ? Optional.empty() : Optional.of(summary.describe(player, now));
	}

	private PlaySummary summary(String player) {
		return summaries.computeIfAbsent(player, name -> new PlaySummary(-1));
	}
}
