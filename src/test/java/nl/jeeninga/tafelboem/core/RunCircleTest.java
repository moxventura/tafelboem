package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RunCircleTest {
	@Test
	void heRunsRoundWhereHeLanded() {
		RunCircle circle = new RunCircle(10, 10, 0);
		RunCircle.Point first = circle.next(null, List.of(), point -> true);
		assertNotNull(first);
		assertTrue(Math.hypot(first.x() - 10, first.z() - 10) < 0.5, "his first step starts where he landed: " + first);
		for (int i = 0; i < 40; i++) {
			RunCircle.Point point = circle.next(null, List.of(), p -> true);
			assertEquals(RunCircle.RADIUS, Math.hypot(point.x() - circle.centreX(), point.z() - circle.centreZ()), 1e-9);
		}
	}

	@Test
	void neverCloserThanTwoBlocksToAKid() {
		RunCircle circle = new RunCircle(0, 0, 0);
		RunCircle.Point kid = new RunCircle.Point(-2.5, 1.0);
		for (int i = 0; i < 200; i++) {
			RunCircle.Point point = circle.next(null, List.of(kid), p -> true);
			if (point != null) {
				assertTrue(Math.hypot(point.x() - kid.x(), point.z() - kid.z()) >= RunCircle.KID_DISTANCE, "step " + i + ": " + point);
			}
		}
	}

	@Test
	void heStaysOverTheArenaFloorAndTurnsRoundAtItsEdge() {
		ArenaBox arena = ArenaBox.of(0, 0, 0, 20, 10, 20);
		RunCircle circle = new RunCircle(0.5, 10, 0);
		for (int i = 0; i < 200; i++) {
			RunCircle.Point point = circle.next(arena, List.of(), p -> true);
			if (point != null) {
				assertTrue(arena.containsColumn(point.x(), point.z()), "step " + i + ": " + point);
			}
		}
	}

	@Test
	void withNoGroundAroundHeStandsStill() {
		RunCircle circle = new RunCircle(0, 0, 0);
		assertNull(circle.next(null, List.of(), point -> false));
	}
}
