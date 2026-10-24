package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

class AnswerTimesTest {
	private static final Fact SUM = new Fact(3, 7);

	@Test
	void noUsualTimeBeforeThreeRightAnswers() {
		AnswerTimes times = new AnswerTimes();
		times.record(SUM, Outcome.CORRECT, 4000);
		times.record(SUM, Outcome.CORRECT, 4000);

		assertTrue(times.usual(SUM).isEmpty());
		assertFalse(AnswerTimes.isFast(times.usual(SUM), 100), "without a usual time nothing is fast");

		times.record(SUM, Outcome.CORRECT, 4000);
		assertEquals(OptionalLong.of(4000), times.usual(SUM));
	}

	@Test
	void theUsualTimeIsTheMedianOfTheLastFiveRightAnswers() {
		AnswerTimes times = new AnswerTimes();
		for (long millis : new long[] {20_000, 20_000, 20_000, 3000, 5000, 4000, 6000, 2000}) {
			times.record(SUM, Outcome.CORRECT, millis);
		}
		// The last five are 3000, 5000, 4000, 6000 and 2000: the slow start no longer counts.
		assertEquals(OptionalLong.of(4000), times.usual(SUM));
	}

	@Test
	void onlyRightAnswersCountAndBothOrdersShareATime() {
		AnswerTimes times = new AnswerTimes();
		times.record(SUM, Outcome.CORRECT, 3000);
		times.record(SUM.commuted(), Outcome.CORRECT, 3000);
		times.record(SUM, Outcome.WRONG, 100);
		times.record(SUM, Outcome.DONT_KNOW, 100);
		times.record(SUM.commuted(), Outcome.CORRECT, 3000);

		assertEquals(OptionalLong.of(3000), times.usual(SUM));
		assertEquals(OptionalLong.of(3000), times.usual(SUM.commuted()));
		assertTrue(times.usual(new Fact(3, 8)).isEmpty(), "another sum has its own time");
	}

	@Test
	void fastIsClearlyFasterThanUsual() {
		OptionalLong usual = OptionalLong.of(5000);

		assertTrue(AnswerTimes.isFast(usual, 3999));
		assertFalse(AnswerTimes.isFast(usual, 4000), "0.8 × 5000 is not faster than itself");
		assertFalse(AnswerTimes.isFast(usual, 4500), "a little faster is just a normal answer");
	}

	@Test
	void theUsualTimeIsTakenBeforeTheAnswerCounts() {
		AnswerTimes times = new AnswerTimes();
		for (int i = 0; i < 3; i++) {
			times.record(SUM, Outcome.CORRECT, 5000);
		}
		OptionalLong before = times.usual(SUM);
		times.record(SUM, Outcome.CORRECT, 1000);

		assertTrue(AnswerTimes.isFast(before, 1000), "an answer is judged against the time from before it");
	}
}
