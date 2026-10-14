package nl.jeeninga.tafelboem.core;

/**
 * One line of the fights log: how one kid's fight on one level ended.
 *
 * @param arena  the template name, or "default" for the plain arena from code
 * @param result {@link #WON}, {@link #LEFT} (through the exit door) or {@link #DISCONNECTED}
 * @param stars  0 unless won, see {@link Stars#forWin(int)}
 * @param kids   how many kids took part in the fight
 */
public record FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong,
		int dontKnow, int chickenOut, int stars, long seconds, int kids, String version) {
	public static final String WON = "won";
	public static final String LEFT = "left";
	public static final String DISCONNECTED = "disconnected";

	public boolean won() {
		return WON.equals(result);
	}

	/** The line the game log gets for this fight, for an evaluation from the shared log alone. */
	public String describe() {
		return player + ": level " + level + " in arena " + arena + ", " + result + (won() ? " " + "★".repeat(stars) : "")
				+ " (" + answers + (answers == 1 ? " answer, " : " answers, ") + wrong + " wrong, " + dontKnow + " \"?\", " + chickenOut + " ESC, " + seconds + " s, "
				+ kids + (kids == 1 ? " kid)" : " kids)");
	}
}
