package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class FightTallyTest {
	@Test
	void rightAnswersInARowMakeAStreak() {
		FightTally tally = new FightTally();
		tally.record(Outcome.CORRECT);
		tally.record(Outcome.CORRECT);
		tally.record(Outcome.CORRECT);

		assertEquals(3, tally.streak());
	}

	@Test
	void onlyATypedWrongAnswerEndsTheStreak() {
		FightTally tally = new FightTally();
		tally.record(Outcome.CORRECT);
		tally.record(Outcome.CORRECT);
		tally.record(Outcome.DONT_KNOW);
		tally.record(Outcome.CHICKEN_OUT);
		assertEquals(2, tally.streak(), "\"?\" and ESC keep it, so \"?\" stays better than guessing");

		tally.record(Outcome.WRONG);
		assertEquals(0, tally.streak());
		tally.record(Outcome.CORRECT);
		assertEquals(1, tally.streak());
	}

	@Test
	void theFarthestYeetIsKept() {
		FightTally tally = new FightTally();
		assertEquals(0, tally.farthestYeet());

		tally.yeeted(12);
		tally.yeeted(31);
		tally.yeeted(7);
		assertEquals(31, tally.farthestYeet());
	}

	@Test
	void dodgesPickUpsAndTrapsAreCountedPerKind() {
		FightTally tally = new FightTally();
		tally.thrownAt(false, true);
		tally.thrownAt(false, true);
		tally.pickUp();
		tally.trap(TrapKind.COBWEB);
		tally.trap(TrapKind.CACTUS);
		tally.trap(TrapKind.CACTUS);

		assertEquals(2, tally.dodged());
		assertEquals(1, tally.pickedUp());
		assertEquals(Map.of("cactus", 2, "cobweb", 1), tally.trapped());
		assertEquals(0, tally.answers(), "none of them is an answer");
	}

	@Test
	void everyThrowAtAFreeKidCountsAndTheDodgedAndStillOnesCountOfThose() {
		FightTally tally = new FightTally();
		tally.thrownAt(true, false);
		tally.thrownAt(false, true);
		tally.thrownAt(false, false);
		tally.thrownAt(true, false);

		assertEquals(4, tally.thrownAt());
		assertEquals(2, tally.still());
		assertEquals(1, tally.dodged());
	}

	@Test
	void catchesThrowBacksHitsJugglesAndTerugAnswersAreCountedApart() {
		FightTally tally = new FightTally();
		tally.catches();
		tally.catches();
		tally.catches();
		tally.throwBack();
		tally.throwBack();
		tally.backHit();
		tally.juggle();
		tally.terug(true);
		tally.terug(false);
		tally.terug(true);

		assertEquals(3, tally.caught());
		assertEquals(2, tally.thrownBack());
		assertEquals(1, tally.backHits());
		assertEquals(1, tally.juggles());
		assertEquals(2, tally.terugRight());
		assertEquals(1, tally.terugWrong());
		assertEquals(0, tally.answers(), "answers are recorded through record(), not here");
	}

	@Test
	void eachKindOfYeetKeepsItsOwnFarthest() {
		FightTally tally = new FightTally();
		tally.yeeted(31);
		tally.yeeted(YeetKind.BACK, 18);
		tally.yeeted(YeetKind.BACK, 12);
		tally.yeeted(YeetKind.SUPER, 22);
		tally.yeeted(YeetKind.KICK, 5);

		assertEquals(31, tally.farthestYeet());
		assertEquals(18, tally.farthestBack());
		assertEquals(22, tally.farthestSuper());
	}
}
