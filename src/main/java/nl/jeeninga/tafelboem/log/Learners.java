package nl.jeeninga.tafelboem.log;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.AnswerTimes;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.Outcome;
import nl.jeeninga.tafelboem.core.QuestionPicker;

/**
 * Each child's question picker and usual answer times, rebuilt from their answer log so progress
 * survives restarts.
 */
public final class Learners {
	private record Learner(QuestionPicker picker, AnswerTimes times) {
	}

	private final AnswerLog log;
	private final Map<UUID, Learner> learners = new HashMap<>();

	public Learners(AnswerLog log) {
		this.log = log;
	}

	public Fact nextQuestion(ServerPlayer player, FightLevel level) {
		return learner(player).picker().next(level);
	}

	public void record(ServerPlayer player, Fact fact, Outcome outcome, int answer, long millis, BombTier tier, String context,
			String via, int typedDigits, FightLevel level) {
		Learner learner = learner(player);
		learner.picker().record(fact, outcome, millis);
		learner.times().record(fact, outcome, millis);
		log.append(new AnswerRecord(System.currentTimeMillis(), player.getPlainTextName(), fact.a(), fact.b(), answer,
				outcome, millis, tier.tier(), context, via, typedDigits, TafelBoem.version(), level.number()));
	}

	/** This kid's usual time for this sum, from the answers so far; ask before recording a new one. */
	public OptionalLong usualMillis(ServerPlayer player, Fact fact) {
		return learner(player).times().usual(fact);
	}

	public AnswerLog log() {
		return log;
	}

	private Learner learner(ServerPlayer player) {
		return learners.computeIfAbsent(player.getUUID(), id -> {
			Learner learner = new Learner(new QuestionPicker(new Random()), new AnswerTimes());
			for (AnswerRecord record : log.read(player.getPlainTextName())) {
				learner.picker().replay(record.fact(), record.outcome(), record.millis());
				learner.times().record(record.fact(), record.outcome(), record.millis());
			}
			return learner;
		});
	}
}
