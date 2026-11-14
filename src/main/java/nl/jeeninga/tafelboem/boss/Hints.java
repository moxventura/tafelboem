package nl.jeeninga.tafelboem.boss;

import net.minecraft.server.level.ServerPlayer;

/**
 * Which kids were told how to throw a Terugbom (R1.4): once per kid per world. A TafelBoem world remembers it in
 * the fights log; a fight without it tells each kid once per fight.
 */
public interface Hints {
	/** Whether this kid was told in this session or in an earlier fight. */
	boolean seen(ServerPlayer kid);

	/** This kid was told just now. */
	void shown(ServerPlayer kid);
}
