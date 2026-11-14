package nl.jeeninga.tafelboem.core;

/**
 * The throws a throw-back costs Graaf Fout (R1.4): a hit makes him skip his next throw, but never two
 * throws in a row, so two kids can't keep him from throwing at all.
 */
public final class Stagger {
	private boolean pending;
	private boolean skippedLast;

	/** A throw-back hit him. */
	public void hit() {
		pending = true;
	}

	/**
	 * He is about to throw.
	 *
	 * @return whether he skips this throw instead; a stagger that comes right after a skipped throw is used up
	 */
	public boolean skipsThrow() {
		boolean skip = pending && !skippedLast;
		pending = false;
		skippedLast = skip;
		return skip;
	}
}
