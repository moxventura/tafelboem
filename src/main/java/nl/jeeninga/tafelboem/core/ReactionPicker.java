package nl.jeeninga.tafelboem.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Picks how Graaf Fout flies off after a kick and how he lands. Flights and landings come from
 * shuffled bags and never repeat back to back. Each bomb remembers its own last kick: its signature
 * landing plays when that kick didn't get it (and it doesn't look like the last landing overall), so
 * it comes every other kick of that bomb, whatever the other bombs did in between.
 */
public final class ReactionPicker {
	private final ShuffleBag<YeetStyle> flights;
	private final ShuffleBag<LandingStyle> landings;
	private YeetStyle lastFlight;
	private LandingStyle lastLanding;
	/** The bombs whose last kick got their signature. */
	private final Set<BombTier> signedLast = EnumSet.noneOf(BombTier.class);

	public ReactionPicker(Random random) {
		this.flights = new ShuffleBag<>(List.of(YeetStyle.values()), random);
		this.landings = new ShuffleBag<>(LandingStyle.GENERIC, random);
	}

	/**
	 * How he flies off: every flight once before any comes again, never the same twice in a row.
	 *
	 * @param inArena   only an arena has walls for the wall splat
	 * @param catalogue false: only the R1 flights
	 */
	public YeetStyle nextFlight(boolean inArena, boolean catalogue) {
		YeetStyle last = lastFlight;
		lastFlight = flights.next(style -> style != last && (inArena || !style.needsWalls()) && (catalogue || !style.isCatalogue()));
		return lastFlight;
	}

	/**
	 * How he lands after {@code flight}, which a kick with {@code bomb} sent him on.
	 *
	 * @param catalogue  false: no head-first, pancake or running, only the dizzy spell
	 * @param signatures false: no bomb signatures
	 */
	public LandingStyle nextLanding(BombTier bomb, YeetStyle flight, boolean catalogue, boolean signatures) {
		LandingStyle signature = LandingStyle.signatureOf(bomb);
		LandingStyle landing;
		if (flight.hasOwnEnding()) {
			landing = LandingStyle.DIZZY;
		} else if (signatures && !signedLast.contains(bomb) && !signature.looksLike(lastLanding)) {
			landing = signature;
		} else if (catalogue) {
			LandingStyle last = lastLanding;
			landing = landings.next(style -> !style.looksLike(last));
		} else {
			landing = LandingStyle.DIZZY;
		}
		if (landing == signature) {
			signedLast.add(bomb);
		} else {
			signedLast.remove(bomb);
		}
		played(landing);
		return landing;
	}

	/** A landing that played without being picked here: forced in a test, or the dizzy spell after flying out. */
	public void played(LandingStyle landing) {
		lastLanding = landing;
	}
}
