package nl.jeeninga.tafelboem.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.Outcome;

class PlaySummariesTest {
	private static AnswerRecord answer(String player, long timestamp) {
		return new AnswerRecord(timestamp, player, 7, 8, 56, Outcome.CORRECT, 3000, 1, "fight", AnswerRecord.VIA_KEY, 0, "1.0.1", 1);
	}

	@Test
	void aKidWhoLeavesGetsTheirLineAndIsForgotten() {
		PlaySummaries summaries = new PlaySummaries();
		summaries.joined("Mark", 0L);
		summaries.answered(answer("Mark", 60_000));
		summaries.answered(answer("Mark", 120_000));
		summaries.fought(new FightRecord(0L, "Mark", 1, "default", FightRecord.WON, 2, 0, 0, 0, 3, 60, 1, "1.0.1"));

		assertEquals(Optional.of("Mark: 3.0 min in the world, 2 answers in 1.0 min of play (2.0 per minute), 100% right, 0 \"?\", 0 ESC;"
				+ " fights: level 1 won ★★★"), summaries.left("Mark", 180_000));
		assertEquals(Optional.empty(), summaries.left("Mark", 200_000), "forgotten after leaving");
	}

	@Test
	void aKidWhoDidNothingGetsNoLine() {
		PlaySummaries summaries = new PlaySummaries();
		summaries.joined("Daan", 0L);
		assertEquals(Optional.empty(), summaries.left("Daan", 60_000));
		assertEquals(Optional.empty(), summaries.left("Nobody", 60_000));
	}

	@Test
	void eachKidHasTheirOwnSummary() {
		PlaySummaries summaries = new PlaySummaries();
		summaries.answered(answer("Mark", 10_000));
		summaries.answered(answer("Daan", 20_000));
		assertEquals(Optional.of("Daan: 1 answer, 100% right, 0 \"?\", 0 ESC; no fights"), summaries.left("Daan", 30_000));
	}
}
