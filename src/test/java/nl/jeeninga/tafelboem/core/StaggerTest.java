package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaggerTest {
	@Test
	void withoutAHitHeNeverSkips() {
		Stagger stagger = new Stagger();
		for (int i = 0; i < 5; i++) {
			assertFalse(stagger.skipsThrow());
		}
	}

	@Test
	void aHitMakesHimSkipHisNextThrow() {
		Stagger stagger = new Stagger();
		stagger.hit();
		assertTrue(stagger.skipsThrow());
		assertFalse(stagger.skipsThrow(), "and only that one");
	}

	@Test
	void twoHitsBeforeAThrowSkipOnlyOneThrow() {
		Stagger stagger = new Stagger();
		stagger.hit();
		stagger.hit();
		assertTrue(stagger.skipsThrow());
		assertFalse(stagger.skipsThrow());
	}

	@Test
	void aStaggerRightAfterASkippedThrowIsUsedUpSoHeSkipsAtMostOneThrowInTwo() {
		Stagger stagger = new Stagger();
		stagger.hit();
		assertTrue(stagger.skipsThrow(), "skipped");
		stagger.hit();
		assertFalse(stagger.skipsThrow(), "the next one he throws, whoever hit him");
		stagger.hit();
		assertTrue(stagger.skipsThrow(), "and the one after that may be skipped again");
	}

	@Test
	void twoKidsHittingHimAllTheTimeStillLetHimThrowEveryOtherTime() {
		Stagger stagger = new Stagger();
		int thrown = 0;
		for (int round = 0; round < 10; round++) {
			stagger.hit();
			stagger.hit();
			if (!stagger.skipsThrow()) {
				thrown++;
			}
		}
		assertTrue(thrown == 5, "5 of 10 throws: " + thrown);
	}
}
