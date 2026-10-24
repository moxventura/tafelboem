package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Locale;

/**
 * What Graaf Fout does once a yeet has put him back on the ground. None lasts more than 2 s, and
 * {@link Pacing} takes the extra time off the pause before his next throw.
 */
public enum LandingStyle {
	/** Stars circle his head: the R1 landing, and the end of the boomerang and the wall splat. */
	DIZZY(30),
	/** Stuck head-first in the ground with his legs wiggling, then he pops out. */
	HEAD_FIRST(40),
	/** Squashed flat, waddles, then boing back into shape. */
	PANCAKE(40),
	/** Runs a small circle with smoke coming off his pants, arms waving. */
	FRANTIC_RUN(40),
	/** The Knalletje's signature: hops on one leg holding his toe, with confetti around his head. */
	TOE_HOP(40),
	/** The Gewone TNT's signature: a soot face with white eyes, coughing smoke. */
	SOOT(40),
	/** The Kippenbom's signature: angry chickens chase him round. */
	CHASED(40);

	/** The landings after any bomb; the others are signatures. */
	public static final List<LandingStyle> GENERIC = List.of(DIZZY, HEAD_FIRST, PANCAKE, FRANTIC_RUN);

	private final int ticks;

	LandingStyle(int ticks) {
		this.ticks = ticks;
	}

	public int ticks() {
		return ticks;
	}

	public boolean isSignature() {
		return !GENERIC.contains(this);
	}

	/** Running from chickens looks like running round in a panic, so the two count as one for "never twice in a row". */
	public boolean looksLike(LandingStyle other) {
		return this == other || other != null && isRunning() && other.isRunning();
	}

	private boolean isRunning() {
		return this == FRANTIC_RUN || this == CHASED;
	}

	/** For the game log. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static LandingStyle signatureOf(BombTier bomb) {
		return switch (bomb) {
			case KNALLETJE -> TOE_HOP;
			case GEWONE_TNT -> SOOT;
			case KIPPENBOM -> CHASED;
		};
	}
}
