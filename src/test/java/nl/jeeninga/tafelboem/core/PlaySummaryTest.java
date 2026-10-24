package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlaySummaryTest {
	private static FightRecord fight(int level, String result, int stars) {
		return new FightRecord(0L, "Mark", level, "default", result, 12, 1, 0, 0, stars, 154, 1, "1.0.1");
	}

	@Test
	void oneLineWithTheEvaluationNumbers() {
		PlaySummary summary = new PlaySummary(0L);
		Outcome[] outcomes = {Outcome.CORRECT, Outcome.CORRECT, Outcome.WRONG, Outcome.CORRECT, Outcome.DONT_KNOW,
				Outcome.CORRECT, Outcome.CORRECT, Outcome.CHICKEN_OUT, Outcome.CORRECT, Outcome.CORRECT};
		for (int i = 0; i < outcomes.length; i++) {
			summary.answered(60_000 + i * 12_000L, outcomes[i]);
		}
		summary.fought(fight(1, FightRecord.WON, 3));
		summary.fought(fight(2, FightRecord.LEFT, 0));

		assertEquals("Mark: 5.0 min in the world, 10 answers in 1.8 min of play (5.6 per minute), 70% right, 1 \"?\", 1 ESC;"
				+ " fights: level 1 won ★★★, level 2 left", summary.describe("Mark", 300_000));
	}

	@Test
	void withoutAJoinTimeOrASecondAnswerItLeavesThoseOut() {
		PlaySummary summary = new PlaySummary(-1);
		summary.answered(10_000, Outcome.CORRECT);
		assertEquals("Mark: 1 answer, 100% right, 0 \"?\", 0 ESC; no fights", summary.describe("Mark", 20_000));
	}

	@Test
	void nothingDoneIsEmpty() {
		PlaySummary summary = new PlaySummary(0L);
		assertTrue(summary.isEmpty());
		summary.fought(fight(3, FightRecord.LEFT, 0));
		assertFalse(summary.isEmpty());
	}

	@Test
	void aFightInOneLine() {
		assertEquals("Mark: level 2 in arena default, won ★★ (12 answers, 1 wrong, 0 \"?\", 0 ESC, 154 s, 1 kid)",
				fight(2, FightRecord.WON, 2).describe());
		FightRecord left = new FightRecord(0L, "Daan", 3, "tafelboem:level_3/kasteel", FightRecord.LEFT, 3, 0, 1, 0, 0, 40, 2, "1.0.1");
		assertEquals("Daan: level 3 in arena tafelboem:level_3/kasteel, left (3 answers, 0 wrong, 1 \"?\", 0 ESC, 40 s, 2 kids)",
				left.describe());
		FightRecord one = new FightRecord(0L, "Daan", 1, "default", FightRecord.LEFT, 1, 0, 0, 0, 0, 13, 1, "1.0.1");
		assertEquals("Daan: level 1 in arena default, left (1 answer, 0 wrong, 0 \"?\", 0 ESC, 13 s, 1 kid)", one.describe());
	}

	@Test
	void aFightLineSaysTheFarthestYeet() {
		FightRecord fight = new FightRecord(0L, "Mark", 1, "default", FightRecord.WON, 11, 0, 0, 0, 3, 120, 1, "1.1.0", 31);
		assertEquals("Mark: level 1 in arena default, won ★★★ (11 answers, 0 wrong, 0 \"?\", 0 ESC, 120 s, 1 kid, farthest yeet 31 blocks)",
				fight.describe());
	}
}
