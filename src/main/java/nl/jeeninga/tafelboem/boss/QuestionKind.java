package nl.jeeninga.tafelboem.boss;

import nl.jeeninga.tafelboem.core.YeetKind;

/**
 * What an open question of a fight is for (R1.4), kept per kid in {@code BossFight}: a right answer on a bomb
 * he threw kicks it back, on a Terug! question it blows up on him, on a juggle question it super-yeets him. The
 * context is what the answer log says, so Terug! sums can be read apart.
 */
enum QuestionKind {
	/** A bomb he threw at the kid. */
	BOMB("fight", YeetKind.KICK),
	/** A Terugbom the kid threw back hit him. */
	TERUG("terug", YeetKind.BACK),
	/** A Terugbom the kid threw back hit him in mid-yeet: he hangs frozen until this question ends. */
	JUGGLE("juggle", YeetKind.SUPER);

	private final String context;
	private final YeetKind yeetKind;

	QuestionKind(String context, YeetKind yeetKind) {
		this.context = context;
		this.yeetKind = yeetKind;
	}

	String context() {
		return context;
	}

	/** What a right answer to this question yeets him with. */
	YeetKind yeetKind() {
		return yeetKind;
	}
}
