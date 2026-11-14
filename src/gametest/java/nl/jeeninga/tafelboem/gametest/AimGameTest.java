package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.Aim;
import nl.jeeninga.tafelboem.core.Dodging;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.Pacing;
import nl.jeeninga.tafelboem.core.ThrowReport;

/**
 * R1.4's aim: he throws where a running kid will be, so a kid who runs straight is hit, a kid who turns after the
 * throw dodges, a kid who stands still is always hit, and after a dodge his next throw is 90 ticks away.
 */
public class AimGameTest {
	/** 4.3 blocks a second: a kid walking. */
	private static final double WALKING = 0.216;

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aKidWhoStandsStillIsHitOnLevel1(GameTestHelper helper) {
		standingKidIsHit(helper, FightLevel.MAKKELIJK);
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aKidWhoStandsStillIsHitOnLevel2(GameTestHelper helper) {
		standingKidIsHit(helper, FightLevel.GEMIDDELD);
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aKidWhoStandsStillIsHitOnLevel3(GameTestHelper helper) {
		standingKidIsHit(helper, FightLevel.MOEILIJK);
	}

	private static void standingKidIsHit(GameTestHelper helper, FightLevel level) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), level, true);

		helper.succeedWhen(() -> {
			FightTally tally = arena.fight.tally(arena.kid.getUUID());
			helper.assertTrue(tally.thrownAt() >= 1, "wait for his first throw to land");
			helper.assertTrue(tally.dodged() == 0, "a kid who stands still is always hit");
			helper.assertTrue(tally.still() >= 1, "and counts as standing still");
			helper.assertTrue(arena.askedAnything(), "so the question comes");
			ThrowReport line = arena.fight.lastThrow();
			helper.assertTrue(line != null && line.level() == level.number() && line.hit() && line.still(), "the throw line says so: " + line);
			helper.assertTrue(line.flightTicks() >= Aim.MIN_FLIGHT_TICKS, "and the flight was at least 12 ticks: " + line);
			arena.end();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aKidRunningStraightAtWalkingSpeedIsHitOnLevel3(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), FightLevel.MOEILIJK, true);
		// Along the south edge of the floor, west to east: across his line of fire, the way a kid dodges. Far from him,
		// so the bomb flies long (about 19 ticks at his first throw, when the kid is about 4 blocks east of the middle).
		arena.kid.teleportTo(arena.box.middleX() - 9.5, arena.floorY, arena.box.middleZ() + 9);
		arena.walk(arena.kid, WALKING, 0);

		helper.succeedWhen(() -> {
			FightTally tally = arena.fight.tally(arena.kid.getUUID());
			helper.assertTrue(tally.thrownAt() >= 1, "wait for his first throw to land");
			ThrowReport line = arena.fight.lastThrow();
			// Without his lead the bomb would land where the kid stood at the throw: this far behind them, well out of the zone.
			double behind = line.flightTicks() * WALKING;
			helper.assertTrue(behind > Dodging.HIT_RADIUS + 0.5, "the flight was long enough that only the lead hits: " + line + ", "
					+ behind + " blocks walked");
			helper.assertTrue(tally.dodged() == 0, "he aimed where the kid was going: a hit, " + line);
			helper.assertTrue(tally.still() == 0, "and the kid was moving: " + arena.fight.lastThrow());
			helper.assertTrue(arena.askedAnything(), "so the question comes");
			arena.end();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void aKidWhoTurnsAfterTheThrowDodges(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), FightLevel.MOEILIJK, true);
		arena.kid.teleportTo(arena.box.middleX() - 9, arena.floorY, arena.box.middleZ() + 5);
		arena.walk(arena.kid, WALKING, 0);
		boolean[] turned = {false};
		helper.onEachTick(() -> {
			// His bomb is in the air, aimed ahead: now the kid turns round.
			if (!turned[0] && !arena.bombsInTheAir().isEmpty()) {
				turned[0] = true;
				arena.walk(arena.kid, -WALKING, 0);
			}
		});

		helper.succeedWhen(() -> {
			FightTally tally = arena.fight.tally(arena.kid.getUUID());
			helper.assertTrue(tally.thrownAt() >= 1, "wait for his first throw to land");
			helper.assertTrue(tally.dodged() == 1, "a kid who turned is dodged: " + arena.fight.lastThrow());
			helper.assertFalse(arena.askedAnything(), "so no question");
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "and nobody is rooted");
			arena.end();
		});
	}

	@GameTest(maxTicks = 500, padding = 48)
	@Fight
	public void afterADodgeHisNextThrowComes90TicksLater(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), FightLevel.MOEILIJK, true);
		arena.kid.teleportTo(arena.box.middleX() - 9, arena.floorY, arena.box.middleZ() + 5);
		arena.walk(arena.kid, WALKING, 0);
		long[] dodgedAt = {-1};
		long[] nextThrowAt = {-1};
		boolean[] turned = {false};
		helper.onEachTick(() -> {
			if (!turned[0] && !arena.bombsInTheAir().isEmpty()) {
				turned[0] = true;
				arena.walk(arena.kid, -WALKING, 0);
			}
			if (dodgedAt[0] < 0 && arena.fight.tally(arena.kid.getUUID()).dodged() == 1) {
				dodgedAt[0] = helper.getTick();
				arena.walk(arena.kid, 0, 0);
			} else if (dodgedAt[0] >= 0 && nextThrowAt[0] < 0 && !arena.bombsInTheAir().isEmpty()) {
				nextThrowAt[0] = helper.getTick();
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(nextThrowAt[0] >= 0, "wait for his next throw");
			long wait = nextThrowAt[0] - dodgedAt[0];
			helper.assertTrue(Math.abs(wait - Pacing.AFTER_DODGE_COOLDOWN_TICKS) <= 2, "his next throw came " + wait + " ticks after the dodge, not 90");
			arena.end();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void theLandingRingMarksWhereTheBombWillLand(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());

		helper.succeedWhen(() -> {
			helper.assertFalse(arena.bombsInTheAir().isEmpty(), "wait for his first throw");
			List<Vec3> rings = arena.fight.landingRings();
			helper.assertTrue(rings.size() == 1, "one ring while the bomb flies: " + rings);
			Vec3 ring = rings.getFirst();
			helper.assertTrue(Math.abs(ring.x - arena.kid.getX()) <= 0.71 && Math.abs(ring.z - arena.kid.getZ()) <= 0.71,
					"centred on the block where the standing kid is: " + ring + " against " + arena.kid.position());
			arena.end();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void withTheRingSwitchedOffNoRingShows(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.landingRing = false;
		ArenaFight arena = new ArenaFight(helper, config);

		helper.succeedWhen(() -> {
			helper.assertFalse(arena.bombsInTheAir().isEmpty(), "wait for his first throw");
			helper.assertTrue(arena.fight.landingRings().isEmpty(), "with the switch off there is no ring");
			arena.end();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void withDodgingOffHeAimsAsInOnePointTwoAndWritesNoThrowLine(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.dodging = false;
		ArenaFight arena = new ArenaFight(helper, config);
		boolean[] ringSeen = {false};
		helper.onEachTick(() -> ringSeen[0] |= !arena.fight.landingRings().isEmpty());

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.askedAnything(), "wait for the question: wherever it lands, its kid answers");
			helper.assertFalse(ringSeen[0], "no ring");
			helper.assertTrue(arena.fight.lastThrow() == null, "no throw line");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).thrownAt() == 0, "and nothing counted");
			arena.end();
		});
	}
}
