package nl.jeeninga.tafelboem.core;

import java.util.Locale;

/**
 * What sent Graaf Fout flying (R1.4): a kicked bomb, a Terugbom thrown back ({@code back}), or the
 * juggle's super yeet ({@code super}). Each has its own personal best on the yeet-o-meter, and its mark
 * on the yeet line of the log.
 */
public enum YeetKind {
	KICK(1.0),
	/** A right answer on a Terug! question: ×1.25. */
	BACK(1.25),
	/** A right answer on a juggle question: ×1.5. */
	SUPER(1.5);

	private final double multiplier;

	YeetKind(double multiplier) {
		this.multiplier = multiplier;
	}

	/** On top of the yeet-o-meter's own factor ({@link YeetBoost}). */
	public double multiplier() {
		return multiplier;
	}

	/** For the logs: {@code kick}, {@code back} or {@code super}. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
