package nl.jeeninga.tafelboem.boss;

import java.util.UUID;

import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;

/** A right answer that kicked a bomb back at Graaf Fout: who kicked, the sum and the bomb. */
record Kick(UUID kicker, Fact fact, BombTier tier) {
}
