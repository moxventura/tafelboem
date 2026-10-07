package nl.jeeninga.tafelboem.game;

import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.core.Outcome;

/**
 * Lets a boss fight react to a question about one of its bombs. Without a listener (free play) a
 * correct answer makes the bomb go off where it stands.
 */
public interface QuizListener {
	/** Called on ESC; return false when the fight's chicken-out limit is used up ("?" instead). */
	default boolean allowChickenOut() {
		return true;
	}

	void onCorrect(ServerPlayer player, QuizSession session);

	void onMiss(ServerPlayer player, QuizSession session, Outcome outcome);
}
