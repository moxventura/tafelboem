package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * Stands in for a real client: remembers everything the server sent.
 */
final class RecordingQuizClient implements QuizClient {
	final List<QuizS2C> sent = new ArrayList<>();
	final List<Component> actionBars = new ArrayList<>();

	@Override
	public boolean canReceive(ServerPlayer player) {
		return true;
	}

	@Override
	public void send(ServerPlayer player, QuizS2C payload) {
		sent.add(payload);
	}

	@Override
	public void actionBar(ServerPlayer player, Component message) {
		actionBars.add(message);
	}

	QuizS2C last() {
		if (sent.isEmpty()) {
			throw new AssertionError("Nothing was sent to the client");
		}
		return sent.getLast();
	}
}
