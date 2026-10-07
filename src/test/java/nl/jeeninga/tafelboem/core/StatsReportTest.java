package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class StatsReportTest {
	private static AnswerRecord answer(int a, int b, boolean correct, long ms) {
		return new AnswerRecord(0L, "Sanne", a, b, correct ? a * b : 0, correct ? Outcome.CORRECT : Outcome.WRONG, ms, 1, "fight");
	}

	@Test
	void accuracyAndMedianPerTable() {
		StatsReport report = StatsReport.of(List.of(
				answer(7, 8, true, 4000),
				answer(8, 7, false, 9000),
				answer(7, 3, true, 2000),
				answer(2, 5, true, 1000)));

		StatsReport.TableStats seven = report.table(7);
		assertEquals(3, seven.attempts());
		assertEquals(2, seven.correct());
		assertEquals(3000, seven.medianCorrectMillis());
		assertEquals(0, report.table(9).attempts());
	}

	@Test
	void squaresCountOnceForTheirTable() {
		StatsReport report = StatsReport.of(List.of(answer(6, 6, true, 3000)));
		assertEquals(1, report.table(6).attempts());
	}

	@Test
	void hardestFactsRankByMistakesThenTime() {
		StatsReport report = StatsReport.of(List.of(
				answer(7, 8, false, 9000),
				answer(8, 7, false, 9000),
				answer(6, 7, false, 5000),
				answer(6, 7, true, 5000),
				answer(3, 4, true, 8000),
				answer(2, 2, true, 1000)));

		List<Fact> hardest = report.hardestFacts(3);
		assertEquals(List.of(new Fact(7, 8), new Fact(6, 7), new Fact(3, 4)), hardest);
	}

	@Test
	void totalsCoverAllAnswers() {
		StatsReport report = StatsReport.of(List.of(answer(7, 8, false, 9000), answer(2, 2, true, 1000)));
		assertEquals(2, report.totalAnswers());
		assertEquals(1, report.totalCorrect());
	}
}
