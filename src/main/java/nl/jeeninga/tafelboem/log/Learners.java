package nl.jeeninga.tafelboem.log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.core.QuestionPicker;

/**
 * Each child's question picker, rebuilt from their answer log so progress survives restarts.
 */
public final class Learners {
	private final AnswerLog log;
	private final Map<UUID, QuestionPicker> pickers = new HashMap<>();

	public Learners(AnswerLog log) {
		this.log = log;
	}

	public Fact nextQuestion(ServerPlayer player) {
		return picker(player).next();
	}

	public void record(ServerPlayer player, Fact fact, Outcome outcome, int answer, long millis, BombTier tier, String context) {
		picker(player).record(fact, outcome, millis);
		log.append(new AnswerRecord(System.currentTimeMillis(), player.getPlainTextName(), fact.a(), fact.b(), answer,
				outcome, millis, tier.tier(), context));
	}

	public AnswerLog log() {
		return log;
	}

	private QuestionPicker picker(ServerPlayer player) {
		return pickers.computeIfAbsent(player.getUUID(), id -> {
			QuestionPicker picker = new QuestionPicker(new Random());
			for (AnswerRecord record : log.read(player.getPlainTextName())) {
				picker.replay(record.fact(), record.outcome(), record.millis());
			}
			return picker;
		});
	}
}
