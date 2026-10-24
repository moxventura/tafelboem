package nl.jeeninga.tafelboem.boss;

import net.minecraft.server.level.ServerPlayer;

/**
 * Each kid's farthest yeet so far. Only a TafelBoem world keeps them (from its fights log); a fight
 * without them shows no records.
 */
public interface YeetRecords {
	/** The kid's farthest yeet so far in blocks; 0 when there is none on file. */
	int best(ServerPlayer kid);

	/** A new farthest yeet, kept for the rest of the session; after that the fights log has it. */
	void beaten(ServerPlayer kid, int blocks);
}
