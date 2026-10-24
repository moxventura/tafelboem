package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Random;

/**
 * Picks how Graaf Fout flies off after a kick and how he lands. Flights and landings come from
 * shuffled bags and never repeat back to back. A bomb's signature landing plays whenever the last
 * landing wasn't that signature, so it comes every other kick of that bomb.
 */
public final class ReactionPicker {
	private final ShuffleBag<YeetStyle> flights;
	private final ShuffleBag<LandingStyle> landings;
	private YeetStyle lastFlight;
	private LandingStyle lastLanding;

	public ReactionPicker(Random random) {
		this.flights = new ShuffleBag<>(List.of(YeetStyle.values()), random);
		this.landings = new ShuffleBag<>(LandingStyle.GENERIC, random);
	}

	/** How he flies off: every flight once before any comes again, never the same twice in a row. */
	public YeetStyle nextFlight() {
		YeetStyle last = lastFlight;
		lastFlight = flights.next(style -> style != last);
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
		} else if (signatures && !signature.looksLike(lastLanding)) {
			landing = signature;
		} else if (catalogue) {
			LandingStyle last = lastLanding;
			landing = landings.next(style -> !style.looksLike(last));
		} else {
			landing = LandingStyle.DIZZY;
		}
		played(landing);
		return landing;
	}

	/** A landing that played without being picked here: forced in a test, or the dizzy spell after flying out. */
	public void played(LandingStyle landing) {
		lastLanding = landing;
	}
}
