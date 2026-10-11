package nl.jeeninga.tafelboem.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Hands out yeet styles from a shuffled bag: every style comes up once before any repeats, and the
 * same style never comes twice in a row, not even across bags.
 */
public final class YeetPicker {
	private final Random random;
	private final Deque<YeetStyle> bag = new ArrayDeque<>();
	private YeetStyle last;

	public YeetPicker(Random random) {
		this.random = random;
	}

	public YeetStyle next() {
		if (bag.isEmpty()) {
			refill();
		}
		last = bag.pop();
		return last;
	}

	private void refill() {
		List<YeetStyle> styles = new ArrayList<>(List.of(YeetStyle.values()));
		Collections.shuffle(styles, random);
		if (styles.getFirst() == last) {
			Collections.swap(styles, 0, styles.size() - 1);
		}
		bag.addAll(styles);
	}
}
