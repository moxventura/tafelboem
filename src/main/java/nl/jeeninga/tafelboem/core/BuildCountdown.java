package nl.jeeninga.tafelboem.core;

import java.util.Locale;

/**
 * The time until Graaf Fout comes, in ticks: what the boss bar shows, which seconds the arena's chat
 * calls out (10, 5, 3, 2, 1) and the bell, which brings him within 10 s but never later. A countdown
 * on building, never on answering a sum.
 */
public final class BuildCountdown {
	public static final int TICKS_PER_SECOND = 20;
	public static final int BELL_SECONDS = 10;
	private static final int[] CALLS = {10, 5, 3, 2, 1};

	private final int total;
	private int remaining;
	/** The first of {@link #CALLS} not called out yet. */
	private int nextCall;

	public BuildCountdown(int ticks) {
		this.total = ticks;
		this.remaining = ticks;
		skipCallsAtOrAbove(ticks);
	}

	/** One tick on. Returns the second to call out in chat now, or 0. */
	public int tick() {
		if (remaining > 0) {
			remaining--;
		}
		int call = 0;
		while (nextCall < CALLS.length && remaining <= CALLS[nextCall] * TICKS_PER_SECOND) {
			call = CALLS[nextCall++];
		}
		return call;
	}

	/**
	 * The bell: at most {@link #BELL_SECONDS} left, never more than before. True when it brought him
	 * closer; the bell's own chat line then says the seconds, so the calls go on below them.
	 */
	public boolean ring() {
		int bell = BELL_SECONDS * TICKS_PER_SECOND;
		if (remaining <= bell) {
			return false;
		}
		remaining = bell;
		skipCallsAtOrAbove(remaining);
		return true;
	}

	public boolean isOver() {
		return remaining == 0;
	}

	public int ticksLeft() {
		return remaining;
	}

	/** Rounded up, so the clock never shows 0:00 while he hasn't come yet. */
	public int secondsLeft() {
		return (remaining + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
	}

	/** As the boss bar shows it: "4:59". */
	public String clock() {
		int seconds = secondsLeft();
		return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
	}

	/** How full the boss bar is: 1 at the start, 0 when he comes. */
	public float progress() {
		return total == 0 ? 0F : (float) remaining / total;
	}

	private void skipCallsAtOrAbove(int ticks) {
		while (nextCall < CALLS.length && CALLS[nextCall] * TICKS_PER_SECOND >= ticks) {
			nextCall++;
		}
	}
}
