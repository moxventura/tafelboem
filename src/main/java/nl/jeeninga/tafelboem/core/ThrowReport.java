package nl.jeeninga.tafelboem.core;

import java.util.Locale;

/**
 * One throw of Graaf Fout at a free kid for the game log's "TafelBoem throw:" line (R1.4): how the
 * evaluation reads the dodge rate per level against the spec's ranges.
 *
 * @param distance    from him to the aim point, in blocks
 * @param flightTicks how long the bomb flew
 * @param speed       the kid's speed when he threw, in blocks per second
 * @param still       the kid moved less than a block between the throw and the landing
 * @param hit         false: the kid dodged it
 */
public record ThrowReport(String kid, int level, double distance, int flightTicks, double speed, boolean still, boolean hit) {
	public String describe() {
		return String.format(Locale.ROOT, "%s: level %d, %.1f blocks, %d ticks, kid at %.1f blocks/s%s: %s", kid, level, distance, flightTicks, speed,
				still ? ", stood still" : "", hit ? "hit" : "dodged");
	}
}
