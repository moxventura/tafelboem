package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Random;

/**
 * Picks one of a level's arenas at random, never the same one twice in a row when there is a choice.
 */
public final class ArenaPicker {
	private final Random random;
	private String last;

	public ArenaPicker(Random random) {
		this.random = random;
	}

	public String pick(List<String> names) {
		if (names.isEmpty()) {
			throw new IllegalArgumentException("No arenas to pick from");
		}
		List<String> choices = names.size() > 1 ? names.stream().filter(name -> !name.equals(last)).toList() : names;
		last = choices.get(random.nextInt(choices.size()));
		return last;
	}
}
