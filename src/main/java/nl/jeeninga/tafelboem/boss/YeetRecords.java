package nl.jeeninga.tafelboem.boss;

import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.core.YeetKind;

/**
 * Each kid's farthest yeet of each kind so far. Only a TafelBoem world keeps them (from its fights log); a fight
 * without them shows no records.
 */
public interface YeetRecords {
	/** The kid's farthest yeet of this kind so far in blocks; 0 when there is none on file. */
	int best(ServerPlayer kid, YeetKind kind);

	/** A new farthest yeet of this kind, kept for the rest of the session; after that the fights log has it. */
	void beaten(ServerPlayer kid, YeetKind kind, int blocks);
}
