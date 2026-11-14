package nl.jeeninga.tafelboem.core;

/**
 * Whether the kids build the arena before Graaf Fout comes (R1.2), and for how long per level: the
 * easier the level, the longer they build.
 */
public record BuildPhase(boolean on, int makkelijkSeconds, int gemiddeldSeconds, int moeilijkSeconds) {
	public static final BuildPhase STANDARD = new BuildPhase(true, 300, 240, 180);
	/** 1.1.2: no build phase, he comes 3 s after the first kid. */
	public static final BuildPhase OFF = new BuildPhase(false, 0, 0, 0);

	/** The same time on every level: for tests, which can't wait minutes. */
	public static BuildPhase everyLevel(int seconds) {
		return new BuildPhase(true, seconds, seconds, seconds);
	}

	/**
	 * The build phase a world plays with: the parent's switch, and for the client GameTests one short time on every
	 * level, whatever the switch says, so they still play the build phase that is off by default (R1.4).
	 *
	 * @param testSeconds the client GameTests' build time ({@code -Dtafelboem.buildSeconds}); null in a real game
	 */
	public static BuildPhase choose(boolean switchedOn, Integer testSeconds) {
		if (testSeconds != null) {
			return everyLevel(testSeconds);
		}
		return switchedOn ? STANDARD : OFF;
	}

	public int seconds(FightLevel level) {
		return switch (level) {
			case MAKKELIJK -> makkelijkSeconds;
			case GEMIDDELD -> gemiddeldSeconds;
			case MOEILIJK -> moeilijkSeconds;
			default -> throw new IllegalArgumentException("No arena, so no build phase, for " + level);
		};
	}
}
