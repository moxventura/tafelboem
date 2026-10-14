package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class StarsTest {
	private static FightRecord won(int level, int stars) {
		return new FightRecord(0L, "Sanne", level, "default", FightRecord.WON, 12, 0, 0, 0, stars, 120, 1, "1.0.0");
	}

	private static FightRecord left(int level) {
		return new FightRecord(0L, "Sanne", level, "default", FightRecord.LEFT, 3, 0, 0, 0, 0, 40, 1, "1.0.0");
	}

	@Test
	void noWrongAnswerIsThreeStars() {
		assertEquals(3, Stars.forWin(0));
	}

	@Test
	void atMostTwoWrongAnswersIsTwoStars() {
		assertEquals(2, Stars.forWin(1));
		assertEquals(2, Stars.forWin(2));
	}

	@Test
	void aWinIsAlwaysWorthAStar() {
		assertEquals(1, Stars.forWin(3));
		assertEquals(1, Stars.forWin(20));
	}

	@Test
	void questionMarkAndEscNeverCostAStar() {
		FightTally tally = new FightTally();
		tally.record(Outcome.CORRECT);
		tally.record(Outcome.DONT_KNOW);
		tally.record(Outcome.CHICKEN_OUT);
		tally.record(Outcome.CORRECT);
		assertEquals(4, tally.answers());
		assertEquals(0, tally.wrong());
		assertEquals(1, tally.dontKnow());
		assertEquals(1, tally.chickenOut());
		assertEquals(3, tally.stars(true));
	}

	@Test
	void typedWrongAnswersCount() {
		FightTally tally = new FightTally();
		tally.record(Outcome.WRONG);
		tally.record(Outcome.CORRECT);
		assertEquals(1, tally.wrong());
		assertEquals(2, tally.stars(true));
	}

	@Test
	void noStarsWithoutAWinOrWithoutAnAnswer() {
		FightTally tally = new FightTally();
		assertEquals(0, tally.stars(true), "a kid who never answered in this fight");
		tally.record(Outcome.CORRECT);
		assertEquals(0, tally.stars(false));
	}

	@Test
	void theBestWinOnALevelCounts() {
		List<FightRecord> fights = List.of(won(2, 1), won(2, 3), left(2), won(1, 2));
		assertEquals(3, Stars.best(fights, 2));
		assertEquals(2, Stars.best(fights, 1));
		assertEquals(0, Stars.best(fights, 3));
	}

	@Test
	void starsShowFilledThenEmpty() {
		assertEquals("★★☆", Stars.show(2));
		assertEquals("☆☆☆", Stars.show(0));
		assertEquals("★★★", Stars.show(3));
	}

	@Test
	void aHandEditedLogCannotShowMoreThanThreeStarsOrFewerThanNone() {
		assertEquals("★★★", Stars.show(7));
		assertEquals("☆☆☆", Stars.show(-1));
	}
}
