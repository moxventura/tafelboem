package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Chooses the next sum for one child, from the tables of the level being played.
 *
 * <p>R0 version: every fact family is in play, missed and slow families are weighted up, and a
 * missed fact is asked again after exactly one other question (corrective feedback with
 * re-response). The full mastery and spacing model arrives in R1.
 */
public final class QuestionPicker {
	static final double MISS_DECAY = 0.7;
	static final double MISS_WEIGHT = 2.0;
	static final double SLOW_WEIGHT = 1.0;
	static final long SLOW_MILLIS = 6000;
	static final int REASK_AFTER_QUESTIONS = 1;

	private final RandomGenerator random;
	private final Map<Fact, FamilyStats> stats = new HashMap<>();
	private final List<PendingReask> pending = new ArrayList<>();
	private Fact lastFamily;

	public QuestionPicker(RandomGenerator random) {
		this.random = random;
	}

	/** Free play and the R0.1 fight: every table. */
	public Fact next() {
		return next(FightLevel.CLASSIC);
	}

	public Fact next(FightLevel level) {
		Fact chosen = takeDueReask(level);
		if (chosen == null) {
			chosen = weightedRandom(level);
		}

		for (PendingReask reask : pending) {
			reask.questionsToWait = Math.max(0, reask.questionsToWait - 1);
		}
		lastFamily = chosen.family();
		return chosen;
	}

	/**
	 * Records a live answer: updates the weights and schedules a re-ask after a miss.
	 */
	public void record(Fact fact, Outcome outcome, long millis) {
		replay(fact, outcome, millis);
		if (outcome.isMiss()) {
			pending.removeIf(reask -> reask.fact.family().equals(fact.family()));
			pending.add(new PendingReask(fact, REASK_AFTER_QUESTIONS));
		}
	}

	/**
	 * Feeds a historical answer (from the answer log) into the weights without scheduling re-asks.
	 */
	public void replay(Fact fact, Outcome outcome, long millis) {
		stats.computeIfAbsent(fact.family(), family -> new FamilyStats()).update(outcome, millis);
	}

	public List<Fact> pendingReasks() {
		return pending.stream().map(reask -> reask.fact).toList();
	}

	/** A re-ask the level doesn't have waits until a level that has it. */
	private Fact takeDueReask(FightLevel level) {
		for (Iterator<PendingReask> it = pending.iterator(); it.hasNext(); ) {
			PendingReask reask = it.next();
			if (reask.questionsToWait == 0 && level.includes(reask.fact) && !reask.fact.family().equals(lastFamily)) {
				it.remove();
				return reask.fact;
			}
		}
		return null;
	}

	private Fact weightedRandom(FightLevel level) {
		// On levels 2 and 3 a coin decides whether this question comes from the tables the level adds.
		boolean fromAdded = level.focusesOnAdded() && random.nextBoolean();
		List<Fact> candidates = candidates(level, fact -> !level.focusesOnAdded() || level.isAdded(fact) == fromAdded);
		if (candidates.isEmpty()) {
			candidates = candidates(level, fact -> true);
		}

		List<Double> weights = new ArrayList<>();
		double total = 0;
		for (Fact fact : candidates) {
			double weight = weight(stats.get(fact));
			weights.add(weight);
			total += weight;
		}

		double roll = random.nextDouble() * total;
		Fact family = candidates.getLast();
		for (int i = 0; i < candidates.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) {
				family = candidates.get(i);
				break;
			}
		}
		return random.nextBoolean() ? family : family.commuted();
	}

	/** The families the level asks, minus the one just asked and the ones waiting for a re-ask. */
	private List<Fact> candidates(FightLevel level, Predicate<Fact> filter) {
		List<Fact> candidates = new ArrayList<>();
		for (Fact fact : Fact.all()) {
			if (fact.a() > fact.b() || fact.equals(lastFamily) || isPending(fact) || !level.includes(fact) || !filter.test(fact)) {
				continue;
			}
			candidates.add(fact);
		}
		return candidates;
	}

	private boolean isPending(Fact family) {
		return pending.stream().anyMatch(reask -> reask.fact.family().equals(family));
	}

	private static double weight(FamilyStats stats) {
		if (stats == null) {
			return 1.0;
		}
		return 1.0 + MISS_WEIGHT * stats.recentMisses + (stats.lastMillis > SLOW_MILLIS ? SLOW_WEIGHT : 0.0);
	}

	private static final class FamilyStats {
		double recentMisses;
		long lastMillis;

		void update(Outcome outcome, long millis) {
			recentMisses = recentMisses * MISS_DECAY + (outcome.isMiss() ? 1.0 : 0.0);
			lastMillis = millis;
		}
	}

	private static final class PendingReask {
		final Fact fact;
		int questionsToWait;

		PendingReask(Fact fact, int questionsToWait) {
			this.fact = fact;
			this.questionsToWait = questionsToWait;
		}
	}
}
