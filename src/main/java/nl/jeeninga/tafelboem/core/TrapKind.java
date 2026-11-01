package nl.jeeninga.tafelboem.core;

import java.util.Locale;

/**
 * The blocks a yeet after a right answer may pull Graaf Fout into (R1.3), and what he does in each.
 * Honey is no trap.
 */
public enum TrapKind {
	CACTUS(LandingStyle.PRICKLED, true),
	BERRY_BUSH(LandingStyle.PRICKLED, false),
	MAGMA(LandingStyle.FRANTIC_RUN, true),
	COBWEB(LandingStyle.WEBBED, false),
	SLIME(LandingStyle.BOING, true),
	/**
	 * A bomb from a dodge: it goes off under him, our usual safe boom, and he gets the soot face. That
	 * is a trap's landing, never logged as a signature.
	 */
	LYING_BOMB(LandingStyle.SOOT, true);

	private final LandingStyle reaction;
	private final boolean landsOnTop;

	TrapKind(LandingStyle reaction, boolean landsOnTop) {
		this.reaction = reaction;
		this.landsOnTop = landsOnTop;
	}

	public LandingStyle reaction() {
		return reaction;
	}

	/** A solid block he lands on; otherwise (a bush, a cobweb) he lands in it. */
	public boolean landsOnTop() {
		return landsOnTop;
	}

	/** Cactus and berries prick: he says "Au au au!". */
	public boolean isPrickly() {
		return reaction == LandingStyle.PRICKLED;
	}

	/** For the logs. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
