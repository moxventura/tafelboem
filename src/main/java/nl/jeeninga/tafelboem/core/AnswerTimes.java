package nl.jeeninga.tafelboem.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * How fast a kid usually answers each sum (3 × 7 and 7 × 3 together): the median of their last 5
 * right answers. An answer clearly faster than that is "fast" and yeets Graaf Fout further. Ask for
 * the usual time before recording an answer, so an answer never counts towards its own.
 */
public final class AnswerTimes {
	static final int RECENT = 5;
	static final int MIN_ANSWERS = 3;
	/** Fast means under 80% of the usual time: a little faster is just a normal answer. */
	static final double FAST_SHARE = 0.8;

	private final Map<Fact, Deque<Long>> recent = new HashMap<>();

	public void record(Fact fact, Outcome outcome, long millis) {
		if (outcome != Outcome.CORRECT) {
			return;
		}
		Deque<Long> times = recent.computeIfAbsent(fact.family(), family -> new ArrayDeque<>());
		times.addLast(millis);
		if (times.size() > RECENT) {
			times.removeFirst();
		}
	}

	/** The usual time for this sum, or empty with fewer than 3 right answers on it. */
	public OptionalLong usual(Fact fact) {
		Deque<Long> times = recent.get(fact.family());
		if (times == null || times.size() < MIN_ANSWERS) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(Medians.of(times.stream().mapToLong(Long::longValue)));
	}

	public static boolean isFast(OptionalLong usual, long millis) {
		return usual.isPresent() && millis < usual.getAsLong() * FAST_SHARE;
	}
}
