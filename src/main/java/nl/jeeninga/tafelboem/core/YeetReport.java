package nl.jeeninga.tafelboem.core;

import java.util.Locale;
import java.util.OptionalLong;

/**
 * One kick for the game log's "TafelBoem yeet:" line: the answer, how much further it sent Graaf
 * Fout, how he flew and landed, and whether it beat the kid's record. The action bar never reaches
 * the log, so this is how a playtest shows what the yeet-o-meter did.
 *
 * @param usual        the kid's usual time for this sum from before the answer; empty without history
 * @param landing      the landing's id, see {@code LandingStyle.id()}
 * @param high         the blocks are how high he flew (the helicopter), not how far
 * @param previousBest the kid's farthest yeet before this one, when it was a record
 */
public record YeetReport(String kicker, BombTier bomb, Fact fact, long millis, OptionalLong usual, boolean fast, int streak,
		double factor, YeetStyle flight, String landing, boolean signature, int blocks, boolean high, boolean flewOut,
		boolean record, int previousBest) {
	public String describe() {
		StringBuilder line = new StringBuilder(kicker).append(" kicked a ").append(bomb.id()).append(" on ").append(fact)
				.append(" in ").append(millis).append(" ms (usual ").append(usual.isPresent() ? usual.getAsLong() + " ms" : "none")
				.append(fast ? ", fast" : "").append(", ").append(streak).append(" in a row")
				.append(String.format(Locale.ROOT, ", x%.2f): ", factor))
				.append(flight.name().toLowerCase(Locale.ROOT)).append(" + ").append(landing).append(signature ? " (signature)" : "")
				.append(", ").append(blocks).append(high ? " blocks high" : " blocks")
				.append(flewOut ? ", out of the arena" : "");
		if (record) {
			line.append(", a record (was ").append(previousBest).append(")");
		}
		return line.toString();
	}
}
