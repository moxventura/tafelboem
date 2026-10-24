package nl.jeeninga.tafelboem.core;

import java.util.stream.LongStream;

/** The middle of some answer times, shared by the stats and the yeet-o-meter. */
public final class Medians {
	private Medians() {
	}

	/** The median: the mean of the two middle values for an even count, and 0 for none. */
	public static long of(LongStream values) {
		long[] sorted = values.sorted().toArray();
		if (sorted.length == 0) {
			return 0;
		}
		int middle = sorted.length / 2;
		return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
	}
}
