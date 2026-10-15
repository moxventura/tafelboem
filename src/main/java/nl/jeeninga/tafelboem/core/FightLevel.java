package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Set;

/**
 * The levels behind the hub's doors, plus the R0.1 fight the summon item still starts in other
 * worlds. A level decides which tables are asked, which bombs Graaf Fout throws and how many hits
 * he takes.
 */
public enum FightLevel {
	/** Outside the TafelBoem world: the R0.1 fight, all tables. */
	CLASSIC(0, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), BombTier.KNALLETJE, BombTier.GEWONE_TNT),
	MAKKELIJK(1, Set.of(1, 2, 5, 10), BombTier.KNALLETJE, BombTier.GEWONE_TNT),
	GEMIDDELD(2, Set.of(1, 2, 3, 4, 5, 9, 10), BombTier.GEWONE_TNT, BombTier.KIPPENBOM),
	MOEILIJK(3, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), BombTier.KIPPENBOM, BombTier.KIPPENBOM);

	/** The levels behind the hub's doors, easiest first. */
	public static final List<FightLevel> DOORS = List.of(MAKKELIJK, GEMIDDELD, MOEILIJK);

	private final int number;
	private final Set<Integer> tables;
	private final BombTier usualBomb;
	private final BombTier everyThirdBomb;

	FightLevel(int number, Set<Integer> tables, BombTier usualBomb, BombTier everyThirdBomb) {
		this.number = number;
		this.tables = tables;
		this.usualBomb = usualBomb;
		this.everyThirdBomb = everyThirdBomb;
	}

	/** 1–3 for the doors; 0 for the R0.1 fight, which is also what the logs say outside an arena. */
	public int number() {
		return number;
	}

	/** 10 + level: 11, 12 and 13. The R0.1 fight keeps its 11. */
	public int hitsRequired() {
		return 10 + Math.max(1, number);
	}

	/**
	 * Half hearts a typed wrong answer costs: 1 heart on Gemiddeld, 2 on Moeilijk, nothing on
	 * Makkelijk or in the R0.1 fight.
	 */
	public int wrongAnswerDamage() {
		return switch (this) {
			case GEMIDDELD -> 2;
			case MOEILIJK -> 4;
			default -> 0;
		};
	}

	/** A sum belongs to a table when either factor is that table, as at school: 7 × 2 is in the table of 2. */
	public boolean includes(Fact fact) {
		return tables.contains(fact.a()) || tables.contains(fact.b());
	}

	/** Levels 2 and 3 ask about half their questions from the sums they add to the level before. */
	public boolean focusesOnAdded() {
		return this == GEMIDDELD || this == MOEILIJK;
	}

	/** A sum this level asks and the level before didn't; always false for a level without that focus. */
	public boolean isAdded(Fact fact) {
		return switch (this) {
			case GEMIDDELD -> includes(fact) && !MAKKELIJK.includes(fact);
			case MOEILIJK -> includes(fact) && !GEMIDDELD.includes(fact);
			default -> false;
		};
	}

	/** Throws count from 1. Every third throw, starting with the second, is the bigger bomb, as in R0.1. */
	public BombTier bombForThrow(int throwCount) {
		return throwCount % 3 == 2 ? everyThirdBomb : usualBomb;
	}

	/** The bombs shown beside this level's door. */
	public List<BombTier> bombs() {
		return usualBomb == everyThirdBomb ? List.of(usualBomb) : List.of(usualBomb, everyThirdBomb);
	}

	public static FightLevel byNumber(int number) {
		for (FightLevel level : values()) {
			if (level.number == number) {
				return level;
			}
		}
		throw new IllegalArgumentException("No level " + number);
	}
}
