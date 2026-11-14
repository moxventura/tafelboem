package nl.jeeninga.tafelboem.boss;

/**
 * What a throw looked like when it left his hand (R1.4): for the "TafelBoem throw:" line, and for counting whether the
 * kid stood still between the throw and the landing.
 *
 * @param distance from him to the aim point, in blocks
 * @param ticks    how long the bomb flies
 * @param speed    the kid's speed then, in blocks per second
 * @param kidX     where the kid stood when he threw
 */
record ThrowInfo(double distance, int ticks, double speed, double kidX, double kidZ) {
}
