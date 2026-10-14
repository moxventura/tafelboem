package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class QuestionPickerTest {
	private final QuestionPicker picker = new QuestionPicker(new Random(42));

	@Test
	void neverAsksTheSameFamilyTwiceInARow() {
		Fact previous = picker.next();
		for (int i = 0; i < 2000; i++) {
			Fact current = picker.next();
			assertNotEquals(previous.family(), current.family(), "repeat at pick " + i);
			picker.record(current, i % 4 == 0 ? Outcome.WRONG : Outcome.CORRECT, 3000);
			previous = current;
		}
	}

	@ParameterizedTest
	@EnumSource(value = Outcome.class, names = {"WRONG", "DONT_KNOW", "CHICKEN_OUT"})
	void aMissedFactIsAskedAgainAfterOneOtherQuestion(Outcome miss) {
		Fact missed = picker.next();
		picker.record(missed, miss, 4000);

		Fact inBetween = picker.next();
		picker.record(inBetween, Outcome.CORRECT, 2000);

		assertNotEquals(missed.family(), inBetween.family());
		assertEquals(missed, picker.next());
	}

	@Test
	void aCorrectAnswerIsNotReasked() {
		Fact fact = picker.next();
		picker.record(fact, Outcome.CORRECT, 2000);
		Fact second = picker.next();
		picker.record(second, Outcome.CORRECT, 2000);
		// With 55 families and no misses the chance of a random repeat here is small but not zero,
		// so assert on the scheduling instead of the random outcome.
		assertTrue(picker.pendingReasks().isEmpty());
	}

	@Test
	void missedFactsComeUpMoreOftenThanOthers() {
		Fact hard = new Fact(7, 8);
		for (int i = 0; i < 5; i++) {
			picker.replay(hard, Outcome.WRONG, 9000);
		}

		// The extra weight fades as the child gets it right, so look at the short term.
		Map<Fact, Integer> counts = new HashMap<>();
		for (int i = 0; i < 110; i++) {
			Fact fact = picker.next();
			counts.merge(fact.family(), 1, Integer::sum);
			picker.record(fact, Outcome.CORRECT, 2000);
		}

		double average = 110.0 / 55;
		int hardCount = counts.getOrDefault(hard.family(), 0);
		assertTrue(hardCount >= 2 * average, "hard fact count " + hardCount);
	}

	@Test
	void everyFamilyAndBothOrdersShowUp() {
		Set<Fact> seen = new HashSet<>();
		for (int i = 0; i < 3000; i++) {
			Fact fact = picker.next();
			seen.add(fact);
			picker.record(fact, Outcome.CORRECT, 2000);
		}
		assertEquals(100, seen.size());
	}

	@Test
	void replayUpdatesHistoryWithoutSchedulingReasks() {
		picker.replay(new Fact(6, 7), Outcome.WRONG, 5000);
		assertTrue(picker.pendingReasks().isEmpty());
	}

	@Test
	void anEasyLevelOnlyAsksItsOwnTables() {
		for (int i = 0; i < 1000; i++) {
			Fact fact = picker.next(FightLevel.MAKKELIJK);
			assertTrue(FightLevel.MAKKELIJK.includes(fact), fact + " is not in the tables of 1, 2, 5 and 10");
			picker.record(fact, i % 5 == 0 ? Outcome.WRONG : Outcome.CORRECT, 3000);
		}
	}

	@Test
	void aboutHalfOfTheQuestionsOnTheHardLevelAreTheSumsItAdds() {
		int added = 0;
		int total = 2000;
		for (int i = 0; i < total; i++) {
			Fact fact = picker.next(FightLevel.MOEILIJK);
			if (FightLevel.MOEILIJK.isAdded(fact)) {
				added++;
			}
			picker.record(fact, Outcome.CORRECT, 3000);
		}
		double share = (double) added / total;
		assertTrue(share > 0.4 && share < 0.6, "share of 6×6, 6×7, 6×8, 7×7, 7×8 and 8×8: " + share);
	}

	@Test
	void aReaskFromAHarderLevelWaitsUntilALevelThatHasIt() {
		Fact hard = new Fact(7, 8);
		picker.record(hard, Outcome.WRONG, 9000);
		for (int i = 0; i < 20; i++) {
			Fact fact = picker.next(FightLevel.MAKKELIJK);
			assertNotEquals(hard.family(), fact.family());
			picker.record(fact, Outcome.CORRECT, 2000);
		}
		assertEquals(hard, picker.next(FightLevel.MOEILIJK));
	}
}
