package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EscapeModelTest {
	@Test
	void biggerBlastsGiveMoreTime() {
		assertTrue(EscapeModel.escapeTicks(4.0, 8000) > EscapeModel.escapeTicks(2.0, 8000));
	}

	@Test
	void evenATinyBlastGivesAtLeastOneAndAHalfSeconds() {
		assertTrue(EscapeModel.escapeTicks(0.5, 8000) >= 30);
	}

	@Test
	void timeIsEnoughToSprintOutOfTheRadiusWithMargin() {
		double radius = 5.0;
		int ticksToSprintOut = (int) Math.ceil(radius / EscapeModel.SPRINT_BLOCKS_PER_SECOND * 20);
		assertTrue(EscapeModel.escapeTicks(radius, 8000) >= ticksToSprintOut * 1.5);
	}

	@Test
	void aFastAnswerEarnsBonusTime() {
		assertEquals(EscapeModel.escapeTicks(4.0, 8000) + EscapeModel.FAST_BONUS_TICKS, EscapeModel.escapeTicks(4.0, 3000));
	}
}
