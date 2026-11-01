package nl.jeeninga.tafelboem.core;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One arena's build phase, as a {@code TafelBoem build:} line in the game log for the evaluation:
 * how long they built, how it ended, and what each kid placed from the kit.
 *
 * @param seconds from the first kid's entry until he came, or until the last kid left
 * @param ending  {@link #BELL}, {@link #TIME} or {@link #EVERYONE_LEFT}
 * @param rangBy  the kid who rang the bell first; null unless the bell ended it
 * @param kids    how many kids were in the run
 * @param placed  per kid, in the order they came: item id → how many placed
 */
public record BuildRecord(int level, long seconds, String ending, String rangBy, int kids, Map<String, Map<String, Integer>> placed) {
	public static final String BELL = "bell";
	public static final String TIME = "time";
	public static final String EVERYONE_LEFT = "everyone left";

	public String describe() {
		String how = switch (ending) {
			case BELL -> "the bell (" + rangBy + ")";
			case TIME -> "time ran out";
			default -> ending;
		};
		String byKid = placed.entrySet().stream().map(kid -> kid.getKey() + " " + placedBy(kid.getValue())).collect(Collectors.joining(", "));
		return "level " + level + ", built " + seconds + " s, " + how + ", " + kids + (kids == 1 ? " kid" : " kids") + "; placed: " + byKid;
	}

	private static String placedBy(Map<String, Integer> items) {
		int total = items.values().stream().mapToInt(Integer::intValue).sum();
		if (total == 0) {
			return "nothing";
		}
		return total + " (" + items.entrySet().stream()
				.filter(item -> item.getValue() > 0)
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
				.map(item -> item.getKey() + " " + item.getValue())
				.collect(Collectors.joining(", ")) + ")";
	}
}
