package nl.jeeninga.tafelboem.core;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * One line of the fights log: how one kid's fight on one level ended.
 *
 * @param arena  the template name, or "default" for the plain arena from code
 * @param result {@link #WON}, {@link #LEFT} (through the exit door), {@link #DISCONNECTED} or {@link #DIED} (R1.3)
 * @param stars  0 unless won, see {@link Stars#forWin(int)}
 * @param seconds from his arrival (from 1.2; before, from the first kid's entry); 0 when he never came
 * @param kids   how many kids took part in the fight
 * @param farthestYeet the kid's farthest yeet in this fight in blocks (helicopter heights don't count); 0 in lines from before 1.1
 * @param buildSeconds how long the arena's build phase lasted (R1.2); 0 in lines from before 1.2
 * @param dodged   bombs thrown at this kid that missed (R1.3); 0 in lines from before 1.3
 * @param pickedUp lying bombs this kid walked into (R1.3); 0 in lines from before 1.3
 * @param trapped  trap landings after this kid's kicks, by {@link TrapKind#id()} (R1.3); empty in lines from before 1.3
 */
public record FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong,
		int dontKnow, int chickenOut, int stars, long seconds, int kids, String version, int farthestYeet, long buildSeconds,
		int dodged, int pickedUp, Map<String, Integer> trapped) {
	public static final String WON = "won";
	public static final String LEFT = "left";
	public static final String DISCONNECTED = "disconnected";
	/** Died in an arena on Normal or Hard (R1.3): counts as leaving. */
	public static final String DIED = "died";

	public FightRecord {
		// A line from before 1.3 has no traps: the log reads it as null.
		trapped = trapped == null ? Map.of() : Map.copyOf(trapped);
	}

	/** A fight without a farthest yeet or build time, as every line before 1.1. */
	public FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong, int dontKnow,
			int chickenOut, int stars, long seconds, int kids, String version) {
		this(timestamp, player, level, arena, result, answers, wrong, dontKnow, chickenOut, stars, seconds, kids, version, 0, 0);
	}

	/** A fight without a build time, as every line before 1.2. */
	public FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong, int dontKnow,
			int chickenOut, int stars, long seconds, int kids, String version, int farthestYeet) {
		this(timestamp, player, level, arena, result, answers, wrong, dontKnow, chickenOut, stars, seconds, kids, version, farthestYeet, 0);
	}

	/** A fight without dodges, pick-ups or traps, as every line before 1.3. */
	public FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong, int dontKnow,
			int chickenOut, int stars, long seconds, int kids, String version, int farthestYeet, long buildSeconds) {
		this(timestamp, player, level, arena, result, answers, wrong, dontKnow, chickenOut, stars, seconds, kids, version, farthestYeet,
				buildSeconds, 0, 0, Map.of());
	}

	public boolean won() {
		return WON.equals(result);
	}

	/** The line the game log gets for this fight, for an evaluation from the shared log alone. */
	public String describe() {
		return player + ": level " + level + " in arena " + arena + ", " + result + (won() ? " " + "★".repeat(stars) : "")
				+ " (" + answers + (answers == 1 ? " answer, " : " answers, ") + wrong + " wrong, " + dontKnow + " \"?\", " + chickenOut + " ESC, " + seconds + " s"
				+ (buildSeconds > 0 ? " after " + buildSeconds + " s of building" : "") + ", "
				+ kids + (kids == 1 ? " kid" : " kids") + (farthestYeet > 0 ? ", farthest yeet " + farthestYeet + " blocks" : "")
				+ (dodged > 0 ? ", dodged " + dodged : "") + (pickedUp > 0 ? ", picked up " + pickedUp : "")
				+ (trapped.isEmpty() ? "" : ", trapped " + trappedLine()) + ")";
	}

	private String trappedLine() {
		int total = trapped.values().stream().mapToInt(Integer::intValue).sum();
		return total + " (" + new TreeMap<>(trapped).entrySet().stream()
				.map(trap -> trap.getKey() + " " + trap.getValue())
				.collect(Collectors.joining(", ")) + ")";
	}
}
