package nl.jeeninga.tafelboem.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Hands out items in shuffled rounds: every item comes up once before any comes up again. The caller
 * says which items are allowed right now (not the one just shown, say); a new round starts when
 * none of the allowed ones is left in this one.
 */
public final class ShuffleBag<T> {
	private final List<T> items;
	private final Random random;
	private final Deque<T> round = new ArrayDeque<>();

	public ShuffleBag(List<T> items, Random random) {
		this.items = List.copyOf(items);
		this.random = random;
	}

	public T next(Predicate<T> allowed) {
		T next = take(allowed);
		if (next == null) {
			List<T> shuffled = new ArrayList<>(items);
			Collections.shuffle(shuffled, random);
			round.clear();
			round.addAll(shuffled);
			next = take(allowed);
		}
		if (next == null) {
			throw new IllegalArgumentException("None of " + items + " is allowed");
		}
		return next;
	}

	/** The first allowed item left in this round, taken out of it; null when there is none. */
	private T take(Predicate<T> allowed) {
		for (Iterator<T> it = round.iterator(); it.hasNext(); ) {
			T item = it.next();
			if (allowed.test(item)) {
				it.remove();
				return item;
			}
		}
		return null;
	}
}
