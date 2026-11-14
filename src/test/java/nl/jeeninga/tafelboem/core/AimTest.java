package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AimTest {
	/** 21 × 21, x and z from 100 to 120. */
	private static final ArenaBox FLOOR = ArenaBox.of(100, 60, 100, 21, 6, 21);

	@Test
	void theLevelsHaveTheSpecsLeadAndFlightFactor() {
		assertEquals(new Aim.Tuning(0.70, 0.85, 12), Aim.Tuning.defaults(1));
		assertEquals(new Aim.Tuning(0.85, 0.75, 12), Aim.Tuning.defaults(2));
		assertEquals(new Aim.Tuning(0.95, 0.68, 12), Aim.Tuning.defaults(3));
		assertEquals(Aim.Tuning.defaults(1), Aim.Tuning.defaults(0), "the R0.1 fight never aims ahead; level 1 is the safe answer");
	}

	@Test
	void todaysFlightTimeIsBetween15And40Ticks() {
		assertEquals(15, Aim.baseTicks(0));
		assertEquals(26, Aim.baseTicks(10));
		assertEquals(40, Aim.baseTicks(100));
	}

	@Test
	void theFlightTimeIsTodaysTimesTheFactorOfTheLevel() {
		assertEquals(22, Aim.flightTicks(10, Aim.Tuning.defaults(1)), "26 × 0.85");
		assertEquals(20, Aim.flightTicks(10, Aim.Tuning.defaults(2)), "26 × 0.75 = 19.5");
		assertEquals(18, Aim.flightTicks(10, Aim.Tuning.defaults(3)), "26 × 0.68");
	}

	@Test
	void aFlightIsNeverShorterThanTwelveTicks() {
		assertEquals(12, Aim.flightTicks(0, Aim.Tuning.defaults(3)), "15 × 0.68 = 10.2 is raised to the floor");
		assertEquals(12, Aim.flightTicks(0, new Aim.Tuning(0.95, 0.1, 12)));
		assertEquals(5, Aim.flightTicks(0, new Aim.Tuning(0.95, 0.1, 5)), "the floor is the parent's to tune");
	}

	@Test
	void aKidWhoStandsStillIsAimedAtWhereTheyStandOnEveryLevel() {
		for (int level = 1; level <= 3; level++) {
			Aim.Plan plan = Aim.plan(110.5, 105.5, 110.5, 115.5, 0, 0, Aim.Tuning.defaults(level), FLOOR);
			assertEquals(110.5, plan.x(), 1e-9, "level " + level);
			assertEquals(115.5, plan.z(), 1e-9, "level " + level);
		}
	}

	@Test
	void aRunningKidIsAimedAheadByTheLeadOfTheirMovementDuringTheFlight() {
		// Level 3: 95% of 0.2 blocks a tick, over the ticks the bomb flies.
		Aim.Tuning tuning = Aim.Tuning.defaults(3);
		Aim.Plan plan = Aim.plan(110.5, 102.5, 105.5, 114.5, 0.2, 0, tuning, FLOOR);

		assertEquals(105.5 + 0.2 * plan.ticks() * 0.95, plan.x(), 1e-9, "where they will be when it lands, times the lead");
		assertEquals(114.5, plan.z(), 1e-9);
		assertEquals(Aim.flightTicks(Math.hypot(plan.x() - 110.5, plan.z() - 102.5), tuning), plan.ticks(), "the flight time is for the aim point");
	}

	@Test
	void theLeadGrowsWithTheLevel() {
		double[] ahead = new double[3];
		for (int level = 1; level <= 3; level++) {
			Aim.Plan plan = Aim.plan(110.5, 102.5, 105.5, 114.5, 0.2, 0, Aim.Tuning.defaults(level), FLOOR);
			ahead[level - 1] = plan.x() - 105.5;
		}
		assertTrue(ahead[0] > 0, "the aim is ahead of the kid");
		assertTrue(ahead[2] > ahead[0], "level 3 aims further ahead than level 1: " + ahead[2] + " against " + ahead[0]);
	}

	@Test
	void aKidRunningAwayFromHimIsAimedAtFurtherOutAndTheBombFliesLonger() {
		Aim.Tuning tuning = Aim.Tuning.defaults(1);
		Aim.Plan still = Aim.plan(110.5, 102.5, 110.5, 108.5, 0, 0, tuning, FLOOR);
		Aim.Plan away = Aim.plan(110.5, 102.5, 110.5, 108.5, 0, 0.25, tuning, FLOOR);

		assertTrue(away.z() > still.z());
		assertTrue(away.ticks() >= still.ticks());
	}

	@Test
	void theAimPointStaysInsideTheArenasFloor() {
		Aim.Plan east = Aim.plan(110.5, 102.5, 119.5, 110.5, 0.4, 0, Aim.Tuning.defaults(3), FLOOR);
		assertEquals(120.5, east.x(), 1e-9, "against the east edge: the floor ends at 121, half a block in");

		Aim.Plan northWest = Aim.plan(110.5, 118.5, 101.0, 101.0, -0.4, -0.4, Aim.Tuning.defaults(3), FLOOR);
		assertEquals(100.5, northWest.x(), 1e-9);
		assertEquals(100.5, northWest.z(), 1e-9);
	}

	@Test
	void theFlightTimeSettlesOnTheAimPointItIsAimedAt() {
		for (int level = 1; level <= 3; level++) {
			Aim.Tuning tuning = Aim.Tuning.defaults(level);
			for (double speed = 0; speed <= 0.3; speed += 0.05) {
				Aim.Plan plan = Aim.plan(110.5, 102.5, 108.5, 116.5, speed, -speed / 2, tuning, FLOOR);
				int expected = Aim.flightTicks(Math.hypot(plan.x() - 110.5, plan.z() - 102.5), tuning);
				assertTrue(Math.abs(plan.ticks() - expected) <= 1, "level " + level + " speed " + speed + ": " + plan + " against " + expected);
			}
		}
	}
}
