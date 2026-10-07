package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Chooses the next sum for one child.
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

	public Fact next() {
		Fact chosen = takeDueReask();
		if (chosen == null) {
			chosen = weightedRandom();
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

	private Fact takeDueReask() {
		for (Iterator<PendingReask> it = pending.iterator(); it.hasNext(); ) {
			PendingReask reask = it.next();
			if (reask.questionsToWait == 0 && !reask.fact.family().equals(lastFamily)) {
				it.remove();
				return reask.fact;
			}
		}
		return null;
	}

	private Fact weightedRandom() {
		List<Fact> candidates = new ArrayList<>();
		List<Double> weights = new ArrayList<>();
		double total = 0;
		for (Fact fact : Fact.all()) {
			if (fact.a() > fact.b() || fact.equals(lastFamily) || isPending(fact)) {
				continue;
			}
			double weight = weight(stats.get(fact));
			candidates.add(fact);
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
