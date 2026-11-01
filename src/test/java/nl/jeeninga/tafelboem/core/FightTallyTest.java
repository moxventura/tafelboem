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
		tally.dodge();
		tally.dodge();
		tally.pickUp();
		tally.trap(TrapKind.COBWEB);
		tally.trap(TrapKind.CACTUS);
		tally.trap(TrapKind.CACTUS);

		assertEquals(2, tally.dodged());
		assertEquals(1, tally.pickedUp());
		assertEquals(Map.of("cactus", 2, "cobweb", 1), tally.trapped());
		assertEquals(0, tally.answers(), "none of them is an answer");
	}
}
