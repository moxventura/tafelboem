package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BuildCountdownTest {
	/** Ticks the countdown until it is over and returns every second it called out, in order. */
	private static List<Integer> callsUntilOver(BuildCountdown countdown) {
		List<Integer> calls = new ArrayList<>();
		while (!countdown.isOver()) {
			int call = countdown.tick();
			if (call > 0) {
				calls.add(call);
			}
		}
		return calls;
	}

	@Test
	void theClockShowsMinutesAndSecondsRoundedUp() {
		BuildCountdown countdown = new BuildCountdown(300 * 20);
		assertEquals("5:00", countdown.clock());
		countdown.tick();
		assertEquals("5:00", countdown.clock());
		for (int i = 0; i < 20; i++) {
			countdown.tick();
		}
		assertEquals("4:59", countdown.clock());
		assertEquals(299, countdown.secondsLeft());
	}

	@Test
	void whenTheTimeRunsOutTheChatCallsTenFiveThreeTwoOne() {
		assertEquals(List.of(10, 5, 3, 2, 1), callsUntilOver(new BuildCountdown(15 * 20)));
	}

	@Test
	void aShortCountdownOnlyCallsTheSecondsItStillHas() {
		// 1.1.2's 3 s before he comes: nothing at 10, 5 or 3, since it starts there.
		assertEquals(List.of(2, 1), callsUntilOver(new BuildCountdown(60)));
	}

	@Test
	void theBellBringsHimWithinTenSecondsAndTheChatGoesOnFromThere() {
		BuildCountdown countdown = new BuildCountdown(300 * 20);
		assertTrue(countdown.ring());
		assertEquals(200, countdown.ticksLeft());
		assertEquals(10, countdown.secondsLeft());
		// The bell's own line says 10; the chat goes on with 5, 3, 2, 1.
		assertEquals(List.of(5, 3, 2, 1), callsUntilOver(countdown));
	}

	@Test
	void theBellNeverMakesItLonger() {
		BuildCountdown countdown = new BuildCountdown(8 * 20);
		assertFalse(countdown.ring());
		assertEquals(160, countdown.ticksLeft());
	}

	@Test
	void ringingTwiceChangesNothingTheSecondTime() {
		BuildCountdown countdown = new BuildCountdown(300 * 20);
		assertTrue(countdown.ring());
		countdown.tick();
		assertFalse(countdown.ring());
		assertEquals(199, countdown.ticksLeft());
	}

	@Test
	void overAtZeroAndTheBarEmpties() {
		BuildCountdown countdown = new BuildCountdown(40);
		assertEquals(1F, countdown.progress());
		for (int i = 0; i < 20; i++) {
			countdown.tick();
		}
		assertEquals(0.5F, countdown.progress());
		callsUntilOver(countdown);
		assertTrue(countdown.isOver());
		assertEquals(0, countdown.tick(), "no call after the end");
		assertEquals(0, countdown.ticksLeft());
	}
}
