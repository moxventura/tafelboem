package nl.jeeninga.tafelboem.boss;

import java.util.OptionalLong;
import java.util.UUID;

import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.YeetBoost;
import nl.jeeninga.tafelboem.core.YeetKind;
import nl.jeeninga.tafelboem.core.YeetPower;

/**
 * A right answer that kicked a bomb back at Graaf Fout: who kicked, the sum and the bomb, and what
 * the answer adds to the yeet.
 *
 * @param usual  the kid's usual time for this sum from before this answer; empty without enough history
 * @param streak right answers in a row by this kid in this fight, this one included
 */
record Kick(UUID kicker, Fact fact, BombTier tier, long millis, OptionalLong usual, boolean fast, int streak, YeetKind kind) {
	/** How much further this kick yeets him; the yeet-o-meter's factor (1 with the boost switched off) times the kind's. */
	double factor(boolean boost) {
		return YeetPower.factor(kind, boost ? YeetBoost.factor(fast, streak) : 1.0);
	}
}
