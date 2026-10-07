package nl.jeeninga.tafelboem.core;

public enum Outcome {
	CORRECT,
	WRONG,
	/** The child pressed "?" (Ik weet het niet). */
	DONT_KNOW,
	/** The child closed the question screen (ESC). */
	CHICKEN_OUT;

	public boolean isMiss() {
		return this != CORRECT;
	}
}
