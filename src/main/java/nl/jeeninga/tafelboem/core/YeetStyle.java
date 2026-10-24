package nl.jeeninga.tafelboem.core;

/**
 * The ways Graaf Fout can fly off when a kid kicks a bomb back. Playtest 1: the spinning yeet was
 * the funniest thing in the game, so every style spins.
 */
public enum YeetStyle {
	/** Launched up and away in a high arc, spinning. */
	CLASSIC,
	/** Bounces three times like a ball, boing, boing, boing. */
	BOUNCE,
	/** Spins up slowly on his cape like a propeller, then drops. */
	HELICOPTER,
	/** Flies off, comes back and faceplants near the kid. */
	BOOMERANG,
	/** Rolls and flips head over heels while he spins: a real tumble. */
	TUMBLE,
	/** Flies low and fast into the arena wall, sticks there flat and slides down (R1.1, only in an arena). */
	WALL_SPLAT;

	/** The boomerang ends face down and the wall splat slides down the wall: that is their landing, with the dizzy spell after it. */
	public boolean hasOwnEnding() {
		return this == BOOMERANG || this == WALL_SPLAT;
	}

	/** Only an arena has walls to splat against. */
	public boolean needsWalls() {
		return this == WALL_SPLAT;
	}

	/** New in R1.1, behind the {@code yeetCatalogue} switch. */
	public boolean isCatalogue() {
		return this == WALL_SPLAT;
	}
}
