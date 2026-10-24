package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * Stands in for a real client: remembers everything the server sent.
 */
final class RecordingQuizClient implements QuizClient {
	record Sent(ServerPlayer player, QuizS2C payload) {
	}

	record Track(ServerPlayer player, int entityId, int ticksAfterScreen, boolean kick) {
	}

	record Bar(ServerPlayer player, Component message) {
	}

	record Sound(ServerPlayer player, SoundEvent sound) {
	}

	final List<QuizS2C> sent = new ArrayList<>();
	final List<Sent> sentTo = new ArrayList<>();
	final List<Component> actionBars = new ArrayList<>();
	final List<Track> tracks = new ArrayList<>();
	final List<Bar> actionBarsTo = new ArrayList<>();
	final List<Sound> sounds = new ArrayList<>();

	@Override
	public boolean canReceive(ServerPlayer player) {
		return true;
	}

	@Override
	public void send(ServerPlayer player, QuizS2C payload) {
		sent.add(payload);
		sentTo.add(new Sent(player, payload));
	}

	@Override
	public void actionBar(ServerPlayer player, Component message) {
		actionBars.add(message);
		actionBarsTo.add(new Bar(player, message));
	}

	@Override
	public void sound(ServerPlayer player, SoundEvent sound) {
		sounds.add(new Sound(player, sound));
	}

	@Override
	public void track(ServerPlayer player, Entity target, int ticksAfterScreen, boolean kick) {
		tracks.add(new Track(player, target.getId(), ticksAfterScreen, kick));
	}

	QuizS2C last() {
		if (sent.isEmpty()) {
			throw new AssertionError("Nothing was sent to the client");
		}
		return sent.getLast();
	}
}
