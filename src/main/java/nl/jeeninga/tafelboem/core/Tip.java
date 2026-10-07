package nl.jeeninga.tafelboem.core;

import java.util.List;

/**
 * A derived-fact strategy hint, as a translation key plus its numbers. The last argument is always
 * the product, so the screen can highlight it.
 */
public record Tip(String key, List<Integer> args) {
	public Tip {
		args = List.copyOf(args);
	}
}
