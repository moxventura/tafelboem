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

	public int seconds(FightLevel level) {
		return switch (level) {
			case MAKKELIJK -> makkelijkSeconds;
			case GEMIDDELD -> gemiddeldSeconds;
			case MOEILIJK -> moeilijkSeconds;
			default -> throw new IllegalArgumentException("No arena, so no build phase, for " + level);
		};
	}
}
