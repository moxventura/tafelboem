package nl.jeeninga.tafelboem.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

/**
 * A parent-facing summary of the answer log.
 */
public final class StatsReport {
	public record TableStats(int table, int attempts, int correct, long medianCorrectMillis) {
		public int accuracyPercent() {
			return attempts == 0 ? 0 : Math.round(100f * correct / attempts);
		}
	}

	private final List<AnswerRecord> answers;

	private StatsReport(List<AnswerRecord> answers) {
		this.answers = List.copyOf(answers);
	}

	public static StatsReport of(List<AnswerRecord> answers) {
		return new StatsReport(answers);
	}

	public int totalAnswers() {
		return answers.size();
	}

	public int totalCorrect() {
		return (int) answers.stream().filter(AnswerRecord::correct).count();
	}

	public TableStats table(int table) {
		List<AnswerRecord> forTable = answers.stream()
				.filter(answer -> answer.a() == table || answer.b() == table)
				.toList();
		int correct = (int) forTable.stream().filter(AnswerRecord::correct).count();
		long median = median(forTable.stream().filter(AnswerRecord::correct).mapToLong(AnswerRecord::millis));
		return new TableStats(table, forTable.size(), correct, median);
	}

	/**
	 * The fact families the child struggles with most: most mistakes first, then slowest.
	 */
	public List<Fact> hardestFacts(int limit) {
		Map<Fact, List<AnswerRecord>> byFamily = new HashMap<>();
		for (AnswerRecord answer : answers) {
			byFamily.computeIfAbsent(answer.fact().family(), family -> new java.util.ArrayList<>()).add(answer);
		}

		Comparator<Map.Entry<Fact, List<AnswerRecord>>> byMistakes = Comparator.comparingLong(
				entry -> entry.getValue().stream().filter(answer -> !answer.correct()).count());
		Comparator<Map.Entry<Fact, List<AnswerRecord>>> byTime = Comparator.comparingLong(
				entry -> median(entry.getValue().stream().mapToLong(AnswerRecord::millis)));

		return byFamily.entrySet().stream()
				.sorted(byMistakes.thenComparing(byTime).reversed())
				.limit(limit)
				.map(Map.Entry::getKey)
				.toList();
	}

	private static long median(LongStream values) {
		long[] sorted = values.sorted().toArray();
		if (sorted.length == 0) {
			return 0;
		}
		int middle = sorted.length / 2;
		return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
	}
}
