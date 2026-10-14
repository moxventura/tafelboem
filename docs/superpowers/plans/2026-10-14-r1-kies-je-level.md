# R1 "Kies je level" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A TafelBoem world type with a hub, a door per level and an arena per level, where the kids
pick Makkelijk, Gemiddeld or Moeilijk, earn stars, and the mod logs every fight for the evaluation.

**Architecture:** The rules stay in the pure-Java `core` package with JUnit tests: the levels
(`FightLevel`), the picker per level, stars and tallies, the fight record, the box maths
(`ArenaBox`) and the plain hub and arena shapes (`Shape`, `HubShape`, `ArenaShape`). The boss fight
learns a level and an optional arena. A new `world` package owns the TafelBoem world: recognising
it, placing the hub and arenas (from code, or from structure templates that replace them), the
doors, the run of each arena, the stars at the doors, the void and the way home. Everything
Minecraft-side is checked with GameTests and, for the real world type, a client GameTest with
screenshots.

**Tech Stack:** Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Minecraft 26.2 (unobfuscated
names: `Identifier`, `EntityTypes`, `ServerLevel.setRespawnData`, `GameRules.RESPAWN_RADIUS`),
JUnit 6, Fabric GameTests and client GameTests, Python + Pillow for the door texture.

**Spec:** `docs/superpowers/specs/2026-10-07-tafelboem-design.md`, section *R1 "Kies je level"
(detailed scope)*, including *Spike results (2026-10-14)*. Findings P12–P17 are in
`docs/evaluations/R0.1.md`. The throwaway spike is on the local branch `spike/r1-world` (reference
only, never merge it).

## Global Constraints

- The `core` package imports nothing from `net.minecraft`, `com.mojang` or `net.fabricmc`
  (enforced by `CoreArchitectureTest`).
- Every user-facing string exists in both `en_us.json` and `nl_nl.json` with the same arguments
  (enforced by `LanguageFilesTest`).
- The villain never states a wrong fact and never comments on the child's ability.
- Our booms never hurt players (`PlayerSafeExplosion`), and a correct answer never hurts the child.
- New features get a config toggle: `stars` and `arenaCraters` in `config/tafelboem.json`, both
  default `true`. The world type itself is a choice at world creation and needs none.
- Outside a TafelBoem world nothing changes: the summon item starts the R0.1 fight
  (`FightLevel.CLASSIC`: all tables, Knalletjes with every 3rd a Gewone TNT, 11 hits) and
  `/tafelboem arena` stays.
- Logs stay local JSON lines in `<world>/tafelboem/`: `answers-<player>.jsonl` and
  `fights-<player>.jsonl`.
- Vanilla look: vanilla blocks and widgets, no new colours.
- Code style: tabs, Javadoc on every class, comments only where the why isn't obvious, names as in
  the surrounding code (Dutch game terms: Knalletje, Gewone TNT, Kippenbom, Graaf Fout).
- Commits use the repo-local identity and end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Versions come from git tags. The first R1 publish is `v1.0.0`; tagging and `publishPack` only
  after Jesper says go.
- Test commands: `./gradlew test` (JUnit), `./gradlew runGameTest` (all GameTests, headless),
  `./gradlew runClientGameTest` (real client; needs the WSLg display).

---

### Task 1: Levels in `core`

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/core/FightLevel.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/core/FightLevelTest.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/core/Fight.java` (add `of(FightLevel)`)

**Interfaces:**
- Produces: `enum FightLevel { CLASSIC, MAKKELIJK, GEMIDDELD, MOEILIJK }` with
  `static final List<FightLevel> DOORS`, `int number()`, `int hitsRequired()`,
  `boolean includes(Fact)`, `boolean focusesOnAdded()`, `boolean isAdded(Fact)`,
  `BombTier bombForThrow(int throwCount)`, `List<BombTier> bombs()`, `static FightLevel byNumber(int)`;
  `static Fight Fight.of(FightLevel)`.

- [ ] **Step 1: Write the failing test**

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FightLevelTest {
	/** The 55 sum pairs: 7 × 8 and 8 × 7 are one pair. */
	private static Set<Fact> families(Predicate<Fact> filter) {
		return Fact.all().stream().filter(fact -> fact.a() <= fact.b()).filter(filter).collect(Collectors.toSet());
	}

	private static List<BombTier> throwsOf(FightLevel level, int count) {
		List<BombTier> bombs = new ArrayList<>();
		for (int throwCount = 1; throwCount <= count; throwCount++) {
			bombs.add(level.bombForThrow(throwCount));
		}
		return bombs;
	}

	@Test
	void eachLevelAddsTables() {
		assertEquals(34, families(FightLevel.MAKKELIJK::includes).size());
		assertEquals(49, families(FightLevel.GEMIDDELD::includes).size());
		assertEquals(55, families(FightLevel.MOEILIJK::includes).size());
		assertEquals(55, families(FightLevel.CLASSIC::includes).size());
	}

	@Test
	void aSumBelongsToATableWhenEitherFactorIsThatTable() {
		assertTrue(FightLevel.MAKKELIJK.includes(new Fact(7, 2)));
		assertTrue(FightLevel.MAKKELIJK.includes(new Fact(2, 7)));
		assertFalse(FightLevel.MAKKELIJK.includes(new Fact(7, 3)));
	}

	@Test
	void theAddedSumsAreTheOnesTheLevelBeforeDidNotAsk() {
		assertEquals(15, families(FightLevel.GEMIDDELD::isAdded).size());
		assertEquals(Set.of(new Fact(6, 6), new Fact(6, 7), new Fact(6, 8), new Fact(7, 7), new Fact(7, 8), new Fact(8, 8)),
				families(FightLevel.MOEILIJK::isAdded));
		assertEquals(0, families(FightLevel.MAKKELIJK::isAdded).size());
		assertEquals(0, families(FightLevel.CLASSIC::isAdded).size());
	}

	@Test
	void onlyLevelsTwoAndThreeFocusOnTheirAddedTables() {
		assertFalse(FightLevel.CLASSIC.focusesOnAdded());
		assertFalse(FightLevel.MAKKELIJK.focusesOnAdded());
		assertTrue(FightLevel.GEMIDDELD.focusesOnAdded());
		assertTrue(FightLevel.MOEILIJK.focusesOnAdded());
	}

	@Test
	void everyThirdThrowStartingWithTheSecondIsTheBiggerBomb() {
		assertEquals(List.of(BombTier.KNALLETJE, BombTier.GEWONE_TNT, BombTier.KNALLETJE, BombTier.KNALLETJE, BombTier.GEWONE_TNT),
				throwsOf(FightLevel.MAKKELIJK, 5));
		assertEquals(List.of(BombTier.GEWONE_TNT, BombTier.KIPPENBOM, BombTier.GEWONE_TNT, BombTier.GEWONE_TNT, BombTier.KIPPENBOM),
				throwsOf(FightLevel.GEMIDDELD, 5));
		assertEquals(List.of(BombTier.KIPPENBOM, BombTier.KIPPENBOM, BombTier.KIPPENBOM, BombTier.KIPPENBOM, BombTier.KIPPENBOM),
				throwsOf(FightLevel.MOEILIJK, 5));
		assertEquals(throwsOf(FightLevel.MAKKELIJK, 5), throwsOf(FightLevel.CLASSIC, 5), "the R0.1 fight keeps its bombs");
	}

	@Test
	void theBombsBesideTheDoor() {
		assertEquals(List.of(BombTier.KNALLETJE, BombTier.GEWONE_TNT), FightLevel.MAKKELIJK.bombs());
		assertEquals(List.of(BombTier.GEWONE_TNT, BombTier.KIPPENBOM), FightLevel.GEMIDDELD.bombs());
		assertEquals(List.of(BombTier.KIPPENBOM), FightLevel.MOEILIJK.bombs());
	}

	@ParameterizedTest
	@EnumSource(FightLevel.class)
	void theLevelSetsTheHits(FightLevel level) {
		int expected = switch (level) {
			case CLASSIC, MAKKELIJK -> 11;
			case GEMIDDELD -> 12;
			case MOEILIJK -> 13;
		};
		assertEquals(expected, level.hitsRequired());
		assertEquals(expected, Fight.of(level).hitsRequired());
	}

	@Test
	void levelsByNumber() {
		assertEquals(FightLevel.GEMIDDELD, FightLevel.byNumber(2));
		assertEquals(FightLevel.CLASSIC, FightLevel.byNumber(0));
		assertThrows(IllegalArgumentException.class, () -> FightLevel.byNumber(4));
		assertEquals(List.of(FightLevel.MAKKELIJK, FightLevel.GEMIDDELD, FightLevel.MOEILIJK), FightLevel.DOORS);
	}
}
```

- [ ] **Step 2: Run** `./gradlew test --tests '*FightLevelTest'`. Expected: compile failure
  (`FightLevel` does not exist).

- [ ] **Step 3: Implement `FightLevel`**

```java
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
```

- [ ] **Step 4: Add `Fight.of`** under the constructor in `Fight.java`:

```java
	/** The rules of one fight at a level: 10 + level hits, see {@link FightLevel#hitsRequired()}. */
	public static Fight of(FightLevel level) {
		return new Fight(Math.max(1, level.number()));
	}
```

- [ ] **Step 5: Run** `./gradlew test --tests '*FightLevelTest' --tests '*FightTest'`. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/nl/jeeninga/tafelboem/core/FightLevel.java src/main/java/nl/jeeninga/tafelboem/core/Fight.java src/test/java/nl/jeeninga/tafelboem/core/FightLevelTest.java
git commit -m "Add the three levels: their tables, their bombs and their hits

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Questions per level

**Files:**
- Modify: `src/main/java/nl/jeeninga/tafelboem/core/QuestionPicker.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/core/AnswerRecord.java` (add `int level`)
- Modify: `src/main/java/nl/jeeninga/tafelboem/log/Learners.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/game/QuizListener.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/game/SessionManager.java`
- Modify: `src/test/java/nl/jeeninga/tafelboem/core/QuestionPickerTest.java`
- Modify: `src/test/java/nl/jeeninga/tafelboem/core/StatsReportTest.java` (helpers get the new field)

**Interfaces:**
- Consumes: `FightLevel` (Task 1).
- Produces: `Fact QuestionPicker.next(FightLevel)` (the old `next()` asks `CLASSIC`);
  `AnswerRecord(..., String version, int level)`; `Learners.nextQuestion(ServerPlayer, FightLevel)`;
  `Learners.record(ServerPlayer, Fact, Outcome, int answer, long millis, BombTier, String context, String via, int typedDigits, FightLevel level)`;
  `default FightLevel QuizListener.fightLevel()` returning `CLASSIC`.

- [ ] **Step 1: Write the failing tests** (add to `QuestionPickerTest`)

```java
	@Test
	void anEasyLevelOnlyAsksItsOwnTables() {
		for (int i = 0; i < 1000; i++) {
			Fact fact = picker.next(FightLevel.MAKKELIJK);
			assertTrue(FightLevel.MAKKELIJK.includes(fact), fact + " is not in the tables of 1, 2, 5 and 10");
			picker.record(fact, i % 5 == 0 ? Outcome.WRONG : Outcome.CORRECT, 3000);
		}
	}

	@Test
	void aboutHalfOfTheQuestionsOnTheHardLevelAreTheSumsItAdds() {
		int added = 0;
		int total = 2000;
		for (int i = 0; i < total; i++) {
			Fact fact = picker.next(FightLevel.MOEILIJK);
			if (FightLevel.MOEILIJK.isAdded(fact)) {
				added++;
			}
			picker.record(fact, Outcome.CORRECT, 3000);
		}
		double share = (double) added / total;
		assertTrue(share > 0.4 && share < 0.6, "share of 6×6, 6×7, 6×8, 7×7, 7×8 and 8×8: " + share);
	}

	@Test
	void aReaskFromAHarderLevelWaitsUntilALevelThatHasIt() {
		Fact hard = new Fact(7, 8);
		picker.record(hard, Outcome.WRONG, 9000);
		for (int i = 0; i < 20; i++) {
			Fact fact = picker.next(FightLevel.MAKKELIJK);
			assertNotEquals(hard.family(), fact.family());
			picker.record(fact, Outcome.CORRECT, 2000);
		}
		assertEquals(hard, picker.next(FightLevel.MOEILIJK));
	}
```

- [ ] **Step 2: Run** `./gradlew test --tests '*QuestionPickerTest'`. Expected: compile failure
  (`next(FightLevel)` does not exist).

- [ ] **Step 3: Implement the picker per level.** In `QuestionPicker.java`: replace the class
  Javadoc's first paragraph with "Chooses the next sum for one child, from the tables of the
  level being played.", keep `R0 version` details, add `import java.util.function.Predicate;`, and
  replace `next()`, `takeDueReask()` and `weightedRandom()` with:

```java
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
```

  For `CLASSIC` and `MAKKELIJK` no coin is thrown, so the random sequence of the existing tests is
  unchanged.

- [ ] **Step 4: Record the level in the answer log.** `AnswerRecord` gets a last component
  `int level`, documented as "`@param level` the fight level: 1–3 in a TafelBoem arena, 0 elsewhere
  and in logs from before R1". Gson gives old lines `0`.
  - `Learners`: `nextQuestion(ServerPlayer player, FightLevel level)` returns
    `picker(player).next(level)`; `record(...)` gets a last parameter `FightLevel level` and passes
    `level.number()` as the record's last argument.
  - `QuizListener`: add
    ```java
    	/** The level whose tables the questions come from; free play and the R0.1 fight ask every table. */
    	default FightLevel fightLevel() {
    		return FightLevel.CLASSIC;
    	}
    ```
  - `SessionManager`: in `start`, `Fact fact = learners.nextQuestion(player, levelOf(listener));`;
    in `correct` and `miss`, pass `levelOf(session.listener())` as the new last argument of
    `learners.record(...)`; add
    ```java
    	private static FightLevel levelOf(@Nullable QuizListener listener) {
    		return listener == null ? FightLevel.CLASSIC : listener.fightLevel();
    	}
    ```
  - `StatsReportTest`: the two helpers pass `, 0` after `"0.1.3"`.
  - Run `grep -rn "new AnswerRecord(" src` and give every other call site the extra `0`.

- [ ] **Step 5: Run** `./gradlew test` and `./gradlew runGameTest`. Expected: all PASS (the
  GameTests still use free play and the R0.1 fight).

- [ ] **Step 6: Commit**

```bash
git add -A src
git commit -m "Ask the questions of the level being played, half from its new tables

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Stars, tallies and the fights log

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/core/Stars.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/core/FightTally.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/core/FightRecord.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/log/JsonLines.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/log/FightLog.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/log/AnswerLog.java` (delegate to `JsonLines`)
- Create: `src/test/java/nl/jeeninga/tafelboem/core/StarsTest.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/log/FightLogTest.java`

**Interfaces:**
- Produces: `Stars.MAX`, `static int Stars.forWin(int wrongAnswers)`,
  `static int Stars.best(List<FightRecord>, int level)`, `static String Stars.show(int)`;
  `FightTally` with `record(Outcome)`, `answers()`, `wrong()`, `dontKnow()`, `chickenOut()`,
  `int stars(boolean won)`;
  `record FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong, int dontKnow, int chickenOut, int stars, long seconds, int kids, String version)`
  with `WON = "won"`, `LEFT = "left"`, `DISCONNECTED = "disconnected"`, `boolean won()`;
  `FightLog(Path directory)` with `append(FightRecord)` and `List<FightRecord> read(String player)`.

- [ ] **Step 1: Write the failing tests**

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class StarsTest {
	private static FightRecord won(int level, int stars) {
		return new FightRecord(0L, "Sanne", level, "default", FightRecord.WON, 12, 0, 0, 0, stars, 120, 1, "1.0.0");
	}

	private static FightRecord left(int level) {
		return new FightRecord(0L, "Sanne", level, "default", FightRecord.LEFT, 3, 0, 0, 0, 0, 40, 1, "1.0.0");
	}

	@Test
	void noWrongAnswerIsThreeStars() {
		assertEquals(3, Stars.forWin(0));
	}

	@Test
	void atMostTwoWrongAnswersIsTwoStars() {
		assertEquals(2, Stars.forWin(1));
		assertEquals(2, Stars.forWin(2));
	}

	@Test
	void aWinIsAlwaysWorthAStar() {
		assertEquals(1, Stars.forWin(3));
		assertEquals(1, Stars.forWin(20));
	}

	@Test
	void questionMarkAndEscNeverCostAStar() {
		FightTally tally = new FightTally();
		tally.record(Outcome.CORRECT);
		tally.record(Outcome.DONT_KNOW);
		tally.record(Outcome.CHICKEN_OUT);
		tally.record(Outcome.CORRECT);
		assertEquals(4, tally.answers());
		assertEquals(0, tally.wrong());
		assertEquals(1, tally.dontKnow());
		assertEquals(1, tally.chickenOut());
		assertEquals(3, tally.stars(true));
	}

	@Test
	void typedWrongAnswersCount() {
		FightTally tally = new FightTally();
		tally.record(Outcome.WRONG);
		tally.record(Outcome.CORRECT);
		assertEquals(1, tally.wrong());
		assertEquals(2, tally.stars(true));
	}

	@Test
	void noStarsWithoutAWinOrWithoutAnAnswer() {
		FightTally tally = new FightTally();
		assertEquals(0, tally.stars(true), "a kid who never answered in this fight");
		tally.record(Outcome.CORRECT);
		assertEquals(0, tally.stars(false));
	}

	@Test
	void theBestWinOnALevelCounts() {
		List<FightRecord> fights = List.of(won(2, 1), won(2, 3), left(2), won(1, 2));
		assertEquals(3, Stars.best(fights, 2));
		assertEquals(2, Stars.best(fights, 1));
		assertEquals(0, Stars.best(fights, 3));
	}

	@Test
	void starsShowFilledThenEmpty() {
		assertEquals("★★☆", Stars.show(2));
		assertEquals("☆☆☆", Stars.show(0));
		assertEquals("★★★", Stars.show(3));
	}
}
```

```java
package nl.jeeninga.tafelboem.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.jeeninga.tafelboem.core.FightRecord;

class FightLogTest {
	@TempDir
	Path dir;

	private static FightRecord fight(String player, int level, String result, int stars) {
		return new FightRecord(1_000L, player, level, "default", result, 12, 1, 0, 0, stars, 150, 2, "1.0.0");
	}

	@Test
	void eachKidHasTheirOwnFileInOrder() {
		FightLog log = new FightLog(dir);
		log.append(fight("Sanne", 1, FightRecord.WON, 2));
		log.append(fight("Daan", 3, FightRecord.LEFT, 0));
		log.append(fight("Sanne", 2, FightRecord.WON, 3));

		assertEquals(List.of(fight("Sanne", 1, FightRecord.WON, 2), fight("Sanne", 2, FightRecord.WON, 3)), log.read("Sanne"));
		assertEquals(1, log.read("Daan").size());
		assertTrue(Files.exists(dir.resolve("fights-Sanne.jsonl")));
	}

	@Test
	void aBrokenLineIsSkippedAndAMissingFileIsEmpty() throws IOException {
		FightLog log = new FightLog(dir);
		assertEquals(List.of(), log.read("Nobody"));
		log.append(fight("Sanne", 1, FightRecord.WON, 1));
		Files.writeString(dir.resolve("fights-Sanne.jsonl"), "{ not json\n", StandardOpenOption.APPEND);
		assertEquals(1, log.read("Sanne").size());
	}
}
```

- [ ] **Step 2: Run** `./gradlew test --tests '*StarsTest' --tests '*FightLogTest'`. Expected:
  compile failure.

- [ ] **Step 3: Implement the core classes**

```java
package nl.jeeninga.tafelboem.core;

import java.util.List;

/**
 * Stars per kid per level: ★ for a win, ★★ with at most 2 wrong answers, ★★★ without one. Only
 * typed wrong answers count, so the stars reward "?" over guessing.
 */
public final class Stars {
	public static final int MAX = 3;

	private Stars() {
	}

	public static int forWin(int wrongAnswers) {
		return wrongAnswers == 0 ? 3 : wrongAnswers <= 2 ? 2 : 1;
	}

	/** The best stars on a level over all fights; 0 when the level was never won. */
	public static int best(List<FightRecord> fights, int level) {
		return fights.stream().filter(fight -> fight.level() == level && fight.won()).mapToInt(FightRecord::stars).max().orElse(0);
	}

	/** For the doors and the stats, such as "★★☆". */
	public static String show(int stars) {
		return "★".repeat(stars) + "☆".repeat(MAX - stars);
	}
}
```

```java
package nl.jeeninga.tafelboem.core;

/**
 * One kid's answers in one fight. Only typed wrong answers cost stars: "?" and ESC never do.
 */
public final class FightTally {
	private int answers;
	private int wrong;
	private int dontKnow;
	private int chickenOut;

	public void record(Outcome outcome) {
		answers++;
		switch (outcome) {
			case WRONG -> wrong++;
			case DONT_KNOW -> dontKnow++;
			case CHICKEN_OUT -> chickenOut++;
			default -> {
			}
		}
	}

	public int answers() {
		return answers;
	}

	public int wrong() {
		return wrong;
	}

	public int dontKnow() {
		return dontKnow;
	}

	public int chickenOut() {
		return chickenOut;
	}

	/** No stars without a win, and none for a kid who never answered in this fight. */
	public int stars(boolean won) {
		return won && answers > 0 ? Stars.forWin(wrong) : 0;
	}
}
```

```java
package nl.jeeninga.tafelboem.core;

/**
 * One line of the fights log: how one kid's fight on one level ended.
 *
 * @param arena  the template name, or "default" for the plain arena from code
 * @param result {@link #WON}, {@link #LEFT} (through the exit door) or {@link #DISCONNECTED}
 * @param stars  0 unless won, see {@link Stars#forWin(int)}
 * @param kids   how many kids took part in the fight
 */
public record FightRecord(long timestamp, String player, int level, String arena, String result, int answers, int wrong,
		int dontKnow, int chickenOut, int stars, long seconds, int kids, String version) {
	public static final String WON = "won";
	public static final String LEFT = "left";
	public static final String DISCONNECTED = "disconnected";

	public boolean won() {
		return WON.equals(result);
	}
}
```

- [ ] **Step 4: Implement the logs.** `JsonLines` holds what `AnswerLog` does today, for any
  record type; it logs through its own logger so it loads without the mod class:

```java
package nl.jeeninga.tafelboem.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One JSON line per record in {@code <prefix>-<player>.jsonl}. Local only, so a parent can open it
 * in a spreadsheet; nothing is sent anywhere.
 */
final class JsonLines<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger("tafelboem");
	private static final Gson GSON = new Gson();

	private final Path directory;
	private final String prefix;
	private final Class<T> type;

	JsonLines(Path directory, String prefix, Class<T> type) {
		this.directory = directory;
		this.prefix = prefix;
		this.type = type;
	}

	void append(String player, T record) {
		try {
			Files.createDirectories(directory);
			Files.writeString(file(player), GSON.toJson(record) + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOGGER.error("Could not write the {} log for {}", prefix, player, e);
		}
	}

	List<T> read(String player) {
		Path file = file(player);
		List<T> records = new ArrayList<>();
		if (!Files.exists(file)) {
			return records;
		}
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (line.isBlank()) {
					continue;
				}
				try {
					records.add(GSON.fromJson(line, type));
				} catch (JsonParseException e) {
					LOGGER.warn("Skipping an unreadable {} log line for {}: {}", prefix, player, line);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Could not read the {} log for {}", prefix, player, e);
		}
		return records;
	}

	private Path file(String player) {
		return directory.resolve(prefix + "-" + player.replaceAll("[^A-Za-z0-9_-]", "_") + ".jsonl");
	}
}
```

  `AnswerLog` keeps its public API and Javadoc and becomes:

```java
public final class AnswerLog {
	private final JsonLines<AnswerRecord> lines;

	public AnswerLog(Path directory) {
		this.lines = new JsonLines<>(directory, "answers", AnswerRecord.class);
	}

	public void append(AnswerRecord record) {
		lines.append(record.player(), record);
	}

	public List<AnswerRecord> read(String player) {
		return lines.read(player);
	}
}
```

  `FightLog` is the same with prefix `"fights"` and `FightRecord`, with the Javadoc "One JSON line
  per fight per kid in `<world>/tafelboem/fights-<player>.jsonl`: the numbers for the evaluation,
  and where the stars at the doors come from."

- [ ] **Step 5: Run** `./gradlew test` and `./gradlew runGameTest`. Expected: PASS (the answer log
  file names are unchanged).

- [ ] **Step 6: Commit**

```bash
git add -A src
git commit -m "Add stars, a tally per kid per fight, and the fights log

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Stats: decimals, "still practising" and a line per level (P16, P17)

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/core/StatsFormat.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/core/StatsFormatTest.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/core/StatsReport.java` (`stillPractising` replaces `hardestFacts`)
- Modify: `src/test/java/nl/jeeninga/tafelboem/core/StatsReportTest.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/config/TafelBoemConfig.java` (`stars`)
- Modify: `src/test/java/nl/jeeninga/tafelboem/config/TafelBoemConfigTest.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/command/TafelBoemCommand.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/TafelBoem.java` (`fightLog()`)
- Modify: both lang files

**Interfaces:**
- Consumes: `FightLog`, `FightRecord`, `Stars` (Task 3), `FightLevel.DOORS` (Task 1).
- Produces: `static String StatsFormat.seconds(long millis, @Nullable-by-contract String language)`;
  `List<Fact> StatsReport.stillPractising(int limit)`; config field `boolean stars = true`;
  `static FightLog TafelBoem.fightLog()`; lang keys `tafelboem.level.1` … `.3`,
  `tafelboem.stats.level`, `tafelboem.stats.level_no_stars`, `tafelboem.stats.hardest_none`.

- [ ] **Step 1: Write the failing tests**

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatsFormatTest {
	@Test
	void dutchGetsADecimalComma() {
		assertEquals("4,0", StatsFormat.seconds(4000, "nl_nl"));
	}

	@Test
	void englishGetsADecimalPoint() {
		assertEquals("4.0", StatsFormat.seconds(4000, "en_us"));
	}

	@Test
	void theServerConsoleGetsAPointToo() {
		assertEquals("6.5", StatsFormat.seconds(6500, null));
	}
}
```

  In `StatsReportTest`, replace `hardestFactsRankByMistakesThenTime` with:

```java
	@Test
	void stillPractisingListsMissedAndSlowSumsMostMistakesFirst() {
		StatsReport report = StatsReport.of(List.of(
				answer(7, 8, false, 9000),
				answer(8, 7, false, 9000),
				answer(6, 7, false, 5000),
				answer(6, 7, true, 5000),
				answer(3, 4, true, 8000),
				answer(2, 2, true, 1000)));
		assertEquals(List.of(new Fact(7, 8), new Fact(6, 7), new Fact(3, 4)), report.stillPractising(5));
	}

	@Test
	void sumsThatWereRightAndQuickAreNotStillPractising() {
		// Playtest 2: every sum but 1×10 was right, yet five showed up under "still practising".
		StatsReport report = StatsReport.of(List.of(answer(6, 6, true, 4100), answer(5, 9, true, 6000), answer(2, 2, true, 1000)));
		assertEquals(List.of(), report.stillPractising(5));
	}
```

  In `TafelBoemConfigTest.aMissingFileGivesDefaultsAndIsWrittenForTheParent`, add
  `assertTrue(config.stars);`.

- [ ] **Step 2: Run** `./gradlew test`. Expected: compile failure.

- [ ] **Step 3: Implement**

```java
package nl.jeeninga.tafelboem.core;

import java.util.Locale;

/**
 * Numbers in the stats follow the game language of whoever reads them: playtest 2 showed "4,0 s"
 * next to English text, because the PC was Dutch.
 */
public final class StatsFormat {
	private StatsFormat() {
	}

	/** @param language a Minecraft language code such as {@code nl_nl}; null for the server console */
	public static String seconds(long millis, String language) {
		Locale locale = language == null || language.isBlank() ? Locale.ROOT : Locale.forLanguageTag(language.replace('_', '-'));
		return String.format(locale, "%.1f", millis / 1000.0);
	}
}
```

  In `StatsReport`, replace `hardestFacts` with (same comparator as before, plus the filter):

```java
	/**
	 * The fact families the child is still practising: missed at least once or usually slower than
	 * 6 s, most mistakes first, then slowest. Empty when everything went right and quickly.
	 */
	public List<Fact> stillPractising(int limit) {
		Map<Fact, List<AnswerRecord>> byFamily = new HashMap<>();
		for (AnswerRecord answer : answers) {
			byFamily.computeIfAbsent(answer.fact().family(), family -> new java.util.ArrayList<>()).add(answer);
		}

		Comparator<Map.Entry<Fact, List<AnswerRecord>>> byMistakes = Comparator.comparingLong(
				entry -> entry.getValue().stream().filter(answer -> !answer.correct()).count());
		Comparator<Map.Entry<Fact, List<AnswerRecord>>> byTime = Comparator.comparingLong(
				entry -> median(entry.getValue().stream().mapToLong(AnswerRecord::millis)));

		return byFamily.entrySet().stream()
				.filter(entry -> entry.getValue().stream().anyMatch(answer -> !answer.correct())
						|| median(entry.getValue().stream().mapToLong(AnswerRecord::millis)) > QuestionPicker.SLOW_MILLIS)
				.sorted(byMistakes.thenComparing(byTime).reversed())
				.limit(limit)
				.map(Map.Entry::getKey)
				.toList();
	}
```

  In `TafelBoemConfig`, add after `cameraAssist`:

```java
	/** Stars at the doors, after a win and in the stats. When off they are still in the fights log. */
	public boolean stars = true;
```

  In `TafelBoem`: a field `private static @Nullable FightLog fightLog;`, created in
  `SERVER_STARTED` next to the answer log (same directory:
  `server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID)`), set to `null` in `SERVER_STOPPED`,
  and an accessor like `sessions()`:

```java
	public static FightLog fightLog() {
		if (fightLog == null) {
			throw new IllegalStateException("The TafelBoem fights log is only available while a server is running");
		}
		return fightLog;
	}
```

- [ ] **Step 4: Change `/tafelboem stats`.** In `TafelBoemCommand.stats`:
  - Right after the header line, a line per level played:
    ```java
    		List<FightRecord> fights = TafelBoem.fightLog().read(name);
    		for (FightLevel fightLevel : FightLevel.DOORS) {
    			List<FightRecord> onLevel = fights.stream().filter(fight -> fight.level() == fightLevel.number()).toList();
    			if (onLevel.isEmpty()) {
    				continue;
    			}
    			long won = onLevel.stream().filter(FightRecord::won).count();
    			Component levelName = Component.translatable("tafelboem.level." + fightLevel.number());
    			Component line = TafelBoem.config().stars
    					? Component.translatable("tafelboem.stats.level", levelName, Stars.show(Stars.best(fights, fightLevel.number())), won, onLevel.size())
    					: Component.translatable("tafelboem.stats.level_no_stars", levelName, won, onLevel.size());
    			source.sendSuccess(() -> line, false);
    		}
    ```
  - The seconds per table: `String language = source.getPlayer() != null ? source.getPlayer().clientInformation().language() : null;`
    once before the loop, then `String seconds = StatsFormat.seconds(stats.medianCorrectMillis(), language);`.
  - "Still practising": `List<Fact> practising = report.stillPractising(5);` and, when it is
    empty, send `Component.translatable("tafelboem.stats.hardest_none")` instead of the
    `tafelboem.stats.hardest` line.

- [ ] **Step 5: Add the lang keys.**

  | Key | en_us | nl_nl |
  |---|---|---|
  | `tafelboem.level.1` | Easy | Makkelijk |
  | `tafelboem.level.2` | Medium | Gemiddeld |
  | `tafelboem.level.3` | Hard | Moeilijk |
  | `tafelboem.stats.level` | `  %s: %s, %s of %s won` | `  %s: %s, %s van %s gewonnen` |
  | `tafelboem.stats.level_no_stars` | `  %s: %s of %s won` | `  %s: %s van %s gewonnen` |
  | `tafelboem.stats.hardest_none` | `  Still practising: nothing yet` | `  Nog aan het oefenen: nog niks` |

- [ ] **Step 6: Run** `./gradlew test` and `./gradlew runGameTest`. Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add -A src
git commit -m "Stats: decimals in the reader's language, honest \"still practising\", a line per level

Playtest 2 findings P16 and P17.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: A fight at a level

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/core/ArenaBox.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/core/ArenaBoxTest.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/boss/BossFight.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/boss/BossFights.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/BossFightGameTest.java`

**Interfaces:**
- Consumes: `FightLevel`, `Fight.of` (Task 1), `FightTally` (Task 3).
- Produces: `record ArenaBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)` with
  `static ArenaBox of(int x, int y, int z, int sizeX, int sizeY, int sizeZ)`,
  `boolean contains(int x, int y, int z)`, `boolean containsColumn(double x, double z)`,
  `boolean holds(double x, double y, double z)`, `double middleX()`, `double middleZ()`;
  `BossFight(ServerLevel, GraafFout, SessionManager, FightLevel, @Nullable ArenaBox)`,
  `FightLevel fightLevel()`, `boolean isWon()`, `FightTally tally(UUID kid)`,
  `List<UUID> participants()`, `@Nullable ArenaBox arena()`;
  `@Nullable BossFight BossFights.summon(ServerLevel, Vec3, FightLevel, @Nullable ArenaBox)`,
  `boolean BossFights.summon(ServerLevel, Vec3)` (unchanged behaviour, `CLASSIC`),
  `void BossFights.end(BossFight)`.

- [ ] **Step 1: Write the failing tests**

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArenaBoxTest {
	private final ArenaBox box = ArenaBox.of(10, 60, -5, 47, 7, 47);

	@Test
	void bothCornersAreInside() {
		assertTrue(box.contains(10, 60, -5));
		assertTrue(box.contains(56, 66, 41));
		assertFalse(box.contains(57, 66, 41));
		assertFalse(box.contains(10, 67, -5));
	}

	@Test
	void theSkyOverTheArenaBelongsToIt() {
		assertTrue(box.containsColumn(30.5, 10.5));
		assertTrue(box.holds(30.5, 200, 10.5));
	}

	@Test
	void besideItOrThroughItsBottomIsOut() {
		assertFalse(box.holds(9.9, 64, 10));
		assertFalse(box.holds(30, 57.9, 10));
		assertTrue(box.holds(30, 58, 10), "a jump or a bump just below the floor still counts");
	}

	@Test
	void theMiddleOfTheFootprint() {
		assertEquals(33.5, box.middleX());
		assertEquals(18.5, box.middleZ());
	}
}
```

  In `BossFightGameTest`, add (imports: `nl.jeeninga.tafelboem.core.BombTier`,
  `nl.jeeninga.tafelboem.core.FightLevel`, `nl.jeeninga.tafelboem.core.FightTally`):

```java
	@GameTest(maxTicks = 1200, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void onTheHardLevelHeThrowsOnlyKippenbommenAndCountsEveryAnswer(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		player.teleportTo(stand.x, stand.y, stand.z);
		BossFight fight = fights.summon(helper.getLevel(), stand.add(6, 1.5, 0), FightLevel.MOEILIJK, null);
		helper.assertTrue(fight != null, "Graaf Fout should appear");
		helper.assertTrue(fight.hitsRemaining() == 13, "level 3 needs 13 hits, not " + fight.hitsRemaining());

		Set<Integer> answered = new HashSet<>();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					helper.assertTrue(question.tier() == BombTier.KIPPENBOM.tier(), "only Kippenbommen on level 3, got tier " + question.tier());
					// The first answer is wrong, the rest right.
					int answer = answered.size() == 1 ? question.a() * question.b() + 1 : question.a() * question.b();
					sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, answer, QuizC2S.VIA_KEY));
				}
			}
		});

		helper.succeedWhen(() -> {
			FightTally tally = fight.tally(player.getUUID());
			helper.assertTrue(tally.answers() >= 3, "wait for three answers");
			helper.assertTrue(tally.wrong() == 1, "one wrong answer counted, not " + tally.wrong());
			fights.endAll();
		});
	}
```

- [ ] **Step 2: Run** `./gradlew test --tests '*ArenaBoxTest'` and `./gradlew compileGametestJava`.
  Expected: compile failures.

- [ ] **Step 3: Implement `ArenaBox`**

```java
package nl.jeeninga.tafelboem.core;

/**
 * A box of whole blocks with both corners included: a hub or arena slot, or the area a placed
 * arena takes up.
 */
public record ArenaBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
	public static ArenaBox of(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
		return new ArenaBox(x, y, z, x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1);
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	/** Above the footprint, at any height: the open sky over an arena still belongs to it. */
	public boolean containsColumn(double x, double z) {
		return x >= minX && x < maxX + 1 && z >= minZ && z < maxZ + 1;
	}

	/** A kid or Graaf Fout in the arena: over its footprint and not fallen through its bottom. */
	public boolean holds(double x, double y, double z) {
		return containsColumn(x, z) && y >= minY - 2;
	}

	public double middleX() {
		return (minX + maxX + 1) / 2.0;
	}

	public double middleZ() {
		return (minZ + maxZ + 1) / 2.0;
	}
}
```

- [ ] **Step 4: Give `BossFight` a level.**
  - Fields: replace `private final Fight fight = new Fight(1);` with
    `private final FightLevel fightLevel;`, `private final @Nullable ArenaBox arena;`,
    `private final Fight fight;` and `private final Map<UUID, FightTally> tallies = new HashMap<>();`.
  - Constructor `BossFight(ServerLevel level, GraafFout boss, SessionManager sessions, FightLevel fightLevel, @Nullable ArenaBox arena)`
    sets them, with `this.fight = Fight.of(fightLevel);`.
  - In `throwNextBomb`, `BombTier tier = fightLevel.bombForThrow(throwCount);`.
  - In `onCorrect`, first line `tally(player.getUUID()).record(Outcome.CORRECT);`; in `onMiss`,
    first line `tally(player.getUUID()).record(outcome);`.
  - New methods:
    ```java
    	@Override
    	public FightLevel fightLevel() {
    		return fightLevel;
    	}

    	/** The arena this fight is in; null for a fight from the summon item. Used from R1 on. */
    	public @Nullable ArenaBox arena() {
    		return arena;
    	}

    	public boolean isWon() {
    		return fight.isDefeated();
    	}

    	/** This kid's answers in this fight so far (empty for a kid who hasn't answered). */
    	public FightTally tally(UUID kid) {
    		return tallies.computeIfAbsent(kid, id -> new FightTally());
    	}

    	public List<UUID> participants() {
    		return List.copyOf(participants);
    	}
    ```
- [ ] **Step 5: `BossFights`.**

```java
	public boolean summon(ServerLevel level, Vec3 position) {
		return summon(level, position, FightLevel.CLASSIC, null) != null;
	}

	/**
	 * Starts a fight at a level. Outside an arena two fights may not be within 64 blocks; an arena
	 * keeps its fight apart by itself.
	 */
	public @Nullable BossFight summon(ServerLevel level, Vec3 position, FightLevel fightLevel, @Nullable ArenaBox arena) {
		if (arena == null && hasFightNear(level, position)) {
			return null;
		}
		GraafFout boss = ModEntities.GRAAF_FOUT.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return null;
		}
		boss.setPos(position);
		BossFight fight = new BossFight(level, boss, sessions, fightLevel, arena);
		level.addFreshEntity(boss);
		fights.add(fight);
		fight.start();
		return fight;
	}

	/** Ends one fight early: he poofs away and nobody gets a reward. */
	public void end(BossFight fight) {
		fight.end();
		fights.remove(fight);
	}
```

- [ ] **Step 6: Run** `./gradlew test` and `./gradlew runGameTest`. Expected: PASS, including the
  new GameTest and the R0.1 fight tests.

- [ ] **Step 7: Commit**

```bash
git add -A src
git commit -m "Fight Graaf Fout at a level: its bombs, its hits, a tally per kid

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: A fight inside an arena

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/game/ProtectedAreas.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/game/PlayerSafeExplosion.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/boss/GraafFout.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/boss/BossFight.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/config/TafelBoemConfig.java` (`arenaCraters`), `TafelBoemConfigTest`
- Modify: both lang files (`tafelboem.message.yeet_out`)
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/ArenaFightGameTest.java`
- Modify: `src/gametest/resources/fabric.mod.json` (register `ArenaFightGameTest`)

**Interfaces:**
- Consumes: `ArenaBox`, `BossFight.arena()` (Task 5).
- Produces: `static PlayerSafeExplosion PlayerSafeExplosion.sparing(List<Vec3> kids, double radius)`;
  `ProtectedAreas.protect(ServerLevel, ArenaBox)`, `unprotect(ServerLevel, ArenaBox)`,
  `boolean isProtected(ServerLevel, BlockPos)`, `clear()`;
  `GraafFout.teleportNear(ServerLevel, Vec3, @Nullable ArenaBox)`,
  `int GraafFout.comeBack(ServerLevel, Vec3 nearKid, ArenaBox)`; config `boolean arenaCraters = true`.

- [ ] **Step 1: Write the failing GameTests**

```java
package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.game.PlayerSafeExplosion;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;

/**
 * Graaf Fout in an arena: he never falls into the void for good, and craters spare the outside and
 * the floor under the kids.
 */
public class ArenaFightGameTest {
	@GameTest(maxTicks = 400, padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void whenHeFliesOutOfTheArenaHePoofsBackNearTheKids(GameTestHelper helper) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);
		// OpenField's stone floor is one block below the test's corner: that is the arena's bottom.
		BlockPos corner = helper.absolutePos(new BlockPos(-10, -1, -10));
		ArenaBox arena = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), 21, 6, 21);

		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		kid.teleportTo(arena.middleX(), corner.getY() + 1, arena.middleZ() + 5);
		BossFight fight = fights.summon(helper.getLevel(), new Vec3(arena.middleX(), corner.getY() + 2.5, arena.middleZ()), FightLevel.MAKKELIJK, arena);
		helper.assertTrue(fight != null, "Graaf Fout should appear");
		GraafFout boss = fight.boss();
		boss.yeet(kid.position(), 1.0, YeetStyle.CLASSIC, kid.position());
		boss.teleportTo(arena.middleX(), arena.minY() - 4, arena.middleZ());

		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(arena.holds(boss.getX(), boss.getY(), boss.getZ()), "he should be back in the arena");
			helper.assertTrue(client.actionBars.stream().anyMatch(ArenaFightGameTest::isFlewOut), "the yeet-o-meter says he flew out");
			helper.assertFalse(fight.isOver(), "the fight goes on");
			fights.endAll();
		});
	}

	@GameTest(padding = 48)
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	public void aCraterSparesObsidianBedrockAndTheFloorNearAKid(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 5));
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -2, dz), Blocks.BEDROCK.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
			}
		}
		BlockPos obsidian = centre.offset(2, 0, 0);
		level.setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		kid.teleportTo(centre.getX() + 0.5, centre.getY(), centre.getZ() - 2.5);

		level.explode(null, null, PlayerSafeExplosion.sparing(List.of(kid.position()), 3), centre.getX() + 0.5, centre.getY(),
				centre.getZ() + 0.5, 4.0F, false, Level.ExplosionInteraction.TNT);

		helper.assertTrue(level.getBlockState(centre.below()).isAir(), "the dirt under the boom is gone");
		helper.assertTrue(level.getBlockState(centre.offset(0, -1, -3)).is(Blocks.DIRT), "the floor under the kid stays");
		helper.assertTrue(level.getBlockState(obsidian).is(Blocks.OBSIDIAN), "obsidian never breaks");
		helper.assertTrue(level.getBlockState(centre.offset(0, -2, 0)).is(Blocks.BEDROCK), "bedrock never breaks");
		helper.succeed();
	}

	@GameTest(padding = 48)
	public void nothingBreaksInsideAProtectedArea(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 5));
		ArenaBox hub = ArenaBox.of(centre.getX() - 5, centre.getY() - 2, centre.getZ() - 5, 11, 6, 11);
		for (int dx = -3; dx <= 3; dx++) {
			level.setBlockAndUpdate(centre.offset(dx, -1, 0), Blocks.STONE.defaultBlockState());
		}
		ProtectedAreas.protect(level, hub);
		try {
			level.explode(null, null, PlayerSafeExplosion.INSTANCE, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 4.0F, false,
					Level.ExplosionInteraction.TNT);
			helper.assertTrue(level.getBlockState(centre.below()).is(Blocks.STONE), "the hub floor stays");
		} finally {
			ProtectedAreas.unprotect(level, hub);
		}
		helper.succeed();
	}

	private static boolean isFlewOut(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals("tafelboem.message.yeet_out");
	}

	private static java.nio.file.Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
```

  Register `"nl.jeeninga.tafelboem.gametest.ArenaFightGameTest"` under `fabric-gametest` in
  `src/gametest/resources/fabric.mod.json`. In `TafelBoemConfigTest`, add
  `assertTrue(config.arenaCraters);` to the defaults test.

- [ ] **Step 2: Run** `./gradlew runGameTest`. Expected: compile failure (`sparing`,
  `ProtectedAreas`).

- [ ] **Step 3: Protected areas and sparing explosions**

```java
package nl.jeeninga.tafelboem.game;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import nl.jeeninga.tafelboem.core.ArenaBox;

/**
 * Where our booms break no blocks, such as the hub of a TafelBoem world: a Kippenbom there still
 * goes boom, but doesn't blow a hole through to the void.
 */
public final class ProtectedAreas {
	private static final Map<ResourceKey<Level>, List<ArenaBox>> AREAS = new ConcurrentHashMap<>();

	private ProtectedAreas() {
	}

	public static void protect(ServerLevel level, ArenaBox box) {
		List<ArenaBox> boxes = AREAS.computeIfAbsent(level.dimension(), dimension -> new CopyOnWriteArrayList<>());
		if (!boxes.contains(box)) {
			boxes.add(box);
		}
	}

	public static void unprotect(ServerLevel level, ArenaBox box) {
		AREAS.getOrDefault(level.dimension(), List.of()).remove(box);
	}

	public static boolean isProtected(ServerLevel level, BlockPos pos) {
		return AREAS.getOrDefault(level.dimension(), List.of()).stream().anyMatch(box -> box.contains(pos.getX(), pos.getY(), pos.getZ()));
	}

	public static void clear() {
		AREAS.clear();
	}
}
```

  In `PlayerSafeExplosion`: add a field `private final Predicate<BlockPos> spared;` (the
  constructor takes it; `INSTANCE` and `SHOW` pass `pos -> false`), plus:

```java
	/** A show that breaks blocks, but never the floor within {@code radius} blocks of a kid (the safe floor). */
	public static PlayerSafeExplosion sparing(List<Vec3> kids, double radius) {
		double radiusSqr = radius * radius;
		return new PlayerSafeExplosion(false, pos -> kids.stream().anyMatch(kid -> Vec3.atCenterOf(pos).distanceToSqr(kid) < radiusSqr));
	}

	@Override
	public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
		if (spared.test(pos) || level instanceof ServerLevel server && ProtectedAreas.isProtected(server, pos)) {
			return false;
		}
		return super.shouldBlockExplode(explosion, level, pos, state, power);
	}
```

- [ ] **Step 4: Graaf Fout stays in the arena.** In `GraafFout`:
  - `teleportNear(ServerLevel level, Vec3 around)` delegates to the new overload with `null`.
  - The new overload tries up to 12 random spots 7–10 blocks from `around`; with an arena it only
    takes a spot over the arena's footprint whose ground is above the arena's bottom (outside the
    arena the heightmap is the void's); with no good spot it goes to the middle of the arena.
    The poof, teleport, poof and sound move into a private `appear(level, x, y, z)`.
    ```java
    	public void teleportNear(ServerLevel level, Vec3 around, @Nullable ArenaBox arena) {
    		for (int attempt = 0; attempt < 12; attempt++) {
    			double angle = random.nextDouble() * Math.PI * 2;
    			double distance = 7 + random.nextDouble() * 3;
    			double x = around.x + Math.cos(angle) * distance;
    			double z = around.z + Math.sin(angle) * distance;
    			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    			if (arena == null || arena.containsColumn(x, z) && ground > arena.minY()) {
    				appear(level, x, ground + HOVER_HEIGHT, z);
    				return;
    			}
    		}
    		// No ground around the kid (say, in a corner): the middle of the arena always has some.
    		double x = arena.middleX();
    		double z = arena.middleZ();
    		appear(level, x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) + HOVER_HEIGHT, z);
    	}
    ```
  - Add:
    ```java
    	/**
    	 * He flew out of the arena (or fell through a gap): poof back near the kids, dizzy as after a
    	 * normal landing when he was being yeeted.
    	 *
    	 * @return how far he flew in this yeet, or -1 when he wasn't being yeeted
    	 */
    	public int comeBack(ServerLevel level, Vec3 nearKid, ArenaBox arena) {
    		boolean wasYeeted = airborne;
    		int blocks = wasYeeted ? (int) Math.round(style == YeetStyle.HELICOPTER ? highest : furthest) : -1;
    		airborne = false;
    		setYeetPose(YeetPose.NONE);
    		setDeltaMovement(Vec3.ZERO);
    		armsUp(false);
    		teleportNear(level, nearKid, arena);
    		if (wasYeeted) {
    			dizzyTicks = DIZZY_TICKS;
    			setNoGravity(false);
    		} else {
    			setNoGravity(true);
    		}
    		return blocks;
    	}
    ```
- [ ] **Step 5: `BossFight` in an arena.**
  - `updateParticipants`: with an arena, "near" (boss bar) and "joins" (participant) both mean
    `arena.holds(player.getX(), player.getY(), player.getZ())`; without one, the radius checks stay.
  - `nextTarget`: with an arena, only participants the arena holds; without, as now.
  - `participantPlayers()`: also filter `arena == null || arena.holds(...)`, and extend its Javadoc:
    "In an arena, only the kids still in it."
  - `participantsNearBoss()`: returns `participantPlayers()` when there is an arena.
  - `throwNextBomb`: `boss.teleportNear(level, target.position(), arena);`.
  - `tick()`: right after the `!boss.isAlive()` check:
    ```java
    		if (arena != null && phase != Phase.FINALE && boss.getY() < arena.minY()) {
    			flewOut();
    		}
    ```
    with
    ```java
    	/** Out of the arena and into the void: say how far he got, then he comes straight back. */
    	private void flewOut() {
    		ServerPlayer kid = kicker() != null ? kicker() : participantPlayers().stream().findFirst().orElse(null);
    		Vec3 near = kid != null ? kid.position() : new Vec3(arena.middleX(), arena.minY() + 3, arena.middleZ());
    		int blocks = boss.comeBack(level, near, arena);
    		if (blocks < 0) {
    			return;
    		}
    		Component message = Component.translatable("tafelboem.message.yeet_out", boss.getDisplayName(), blocks)
    				.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    		for (ServerPlayer player : participantPlayers()) {
    			sessions.client().actionBar(player, message);
    		}
    		followKickerFor(LANDED_CAM_TICKS);
    	}
    ```
  - `onKickHit`: craters in an arena, when the toggle is on (`static final double SAFE_FLOOR_RADIUS = 3;`):
    ```java
    		boolean craters = arena != null && TafelBoem.config().arenaCraters;
    		ExplosionDamageCalculator boom = craters
    				? PlayerSafeExplosion.sparing(participantPlayers().stream().map(ServerPlayer::position).toList(), SAFE_FLOOR_RADIUS)
    				: PlayerSafeExplosion.SHOW;
    		level.explode(null, null, boom, boss.getX(), boss.getY() + 1, boss.getZ(), tier.blastRadius(), false,
    				craters ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
    ```
- [ ] **Step 6: Config and lang.** `TafelBoemConfig`:
  `/** In an arena the kicked bomb breaks blocks (never bedrock, obsidian or the floor near a kid). */ public boolean arenaCraters = true;`.
  Lang `tafelboem.message.yeet_out`: en `%s flew %s blocks, right out of the arena!`,
  nl `%s vloog %s blokken, de arena uit!`.

- [ ] **Step 7: Run** `./gradlew test` and `./gradlew runGameTest`. Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add -A src
git commit -m "Keep Graaf Fout in his arena and let its inside blow up

He poofs back when a yeet takes him over the wall, his teleports stay
over the arena, and the kicked bomb makes craters that spare bedrock,
obsidian and the floor near the kids. Protected areas break nothing.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: The plain hub and arenas as shapes (`core`)

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/core/Shape.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/core/ArenaShape.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/core/HubShape.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/core/ArenaPicker.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/core/ArenaShapeTest.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/core/HubShapeTest.java`
- Create: `src/test/java/nl/jeeninga/tafelboem/core/ArenaPickerTest.java`

**Interfaces:**
- Consumes: `FightLevel`, `BombTier`.
- Produces: `record Shape(String name, int sizeX, int sizeY, int sizeZ, List<Shape.Cell> cells, Shape.Spot standAt, Shape.Spot middle, List<Shape.Sign> signs, List<Shape.Frame> frames)`
  with `enum Material { BEDROCK, DIRT, GRASS, COARSE_DIRT, STONE, STONE_BRICKS, OBSIDIAN, DOOR }`,
  `enum Facing { NORTH, SOUTH, EAST, WEST }` (`dx()`, `dz()`),
  `record Cell(int x, int y, int z, Material material, int doorLevel, Facing facing)`,
  `record Spot(int x, int y, int z, Facing facing)`, `record Sign(int x, int y, int z, Facing facing, int level)`,
  `record Frame(int x, int y, int z, Facing facing, BombTier bomb)`, `Optional<Cell> cellAt(int x, int y, int z)`;
  `ArenaShape.SIZE = 47`, `static Shape ArenaShape.forLevel(int level)`;
  `HubShape.SIZE = 25`, `static Shape HubShape.standard()`;
  `ArenaPicker(Random)` with `String pick(List<String> names)`.

- [ ] **Step 1: Write the failing tests**

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArenaShapeTest {
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void fitsOneStructureBlock(int level) {
		Shape shape = ArenaShape.forLevel(level);
		assertTrue(shape.sizeX() <= 48 && shape.sizeY() <= 48 && shape.sizeZ() <= 48);
		for (Shape.Cell cell : shape.cells()) {
			assertTrue(cell.x() >= 0 && cell.x() < shape.sizeX() && cell.y() >= 0 && cell.y() < shape.sizeY()
					&& cell.z() >= 0 && cell.z() < shape.sizeZ(), "outside the shape: " + cell);
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void bedrockAndDirtUnderEveryFloorBlock(int level) {
		Shape shape = ArenaShape.forLevel(level);
		for (Shape.Cell top : shape.cells().stream().filter(cell -> cell.y() == 2).toList()) {
			assertEquals(Shape.Material.BEDROCK, shape.cellAt(top.x(), 0, top.z()).orElseThrow().material());
			assertEquals(Shape.Material.DIRT, shape.cellAt(top.x(), 1, top.z()).orElseThrow().material());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void obsidianWallsFourHighWithOneExitDoor(int level) {
		Shape shape = ArenaShape.forLevel(level);
		List<Shape.Cell> doors = shape.cells().stream().filter(cell -> cell.material() == Shape.Material.DOOR).toList();
		assertEquals(2, doors.size(), "one exit door, two blocks high");
		for (Shape.Cell door : doors) {
			assertEquals(0, door.doorLevel(), "the exit door leads back to the hub");
			assertEquals(Shape.Facing.NORTH, door.facing(), "it faces into the arena");
			assertEquals(ArenaShape.SIZE - 1, door.z(), "in the south wall");
		}
		for (Shape.Cell wall : shape.cells().stream().filter(cell -> cell.y() == 3).toList()) {
			for (int y = 3; y <= 6; y++) {
				Shape.Material material = shape.cellAt(wall.x(), y, wall.z()).orElseThrow().material();
				assertTrue(material == Shape.Material.OBSIDIAN || material == Shape.Material.DOOR && y <= 4,
						"wall at " + wall.x() + "," + y + "," + wall.z() + " is " + material);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void theKidsArriveThreeBlocksInFrontOfTheExitDoor(int level) {
		Shape shape = ArenaShape.forLevel(level);
		Shape.Spot spot = shape.standAt();
		assertEquals(new Shape.Spot(23, 3, ArenaShape.SIZE - 4, Shape.Facing.NORTH), spot);
		assertTrue(shape.cellAt(spot.x(), 3, spot.z()).isEmpty() && shape.cellAt(spot.x(), 4, spot.z()).isEmpty(), "room to stand");
		assertTrue(shape.cellAt(spot.x(), 2, spot.z()).isPresent(), "ground to stand on");
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void graafFoutAppearsInTheFreeMiddle(int level) {
		Shape shape = ArenaShape.forLevel(level);
		assertEquals(new Shape.Spot(23, 3, 23, Shape.Facing.NORTH), shape.middle());
		assertTrue(shape.cellAt(23, 3, 23).isEmpty());
		assertTrue(shape.cellAt(23, 2, 23).isPresent());
	}

	@Test
	void eachLevelLooksDifferent() {
		assertTrue(ArenaShape.forLevel(1).cellAt(0, 0, 0).isEmpty(), "level 1 is round");
		assertTrue(ArenaShape.forLevel(2).cellAt(0, 0, 0).isPresent(), "level 2 is square");
		assertTrue(ArenaShape.forLevel(3).cellAt(0, 0, 0).isEmpty(), "level 3 is round");
		assertEquals(Shape.Material.GRASS, ArenaShape.forLevel(1).cellAt(23, 2, 23).orElseThrow().material());
		assertEquals(Shape.Material.GRASS, ArenaShape.forLevel(2).cellAt(23, 2, 23).orElseThrow().material());
		assertEquals(Shape.Material.COARSE_DIRT, ArenaShape.forLevel(3).cellAt(23, 2, 23).orElseThrow().material());
		assertFalse(ArenaShape.forLevel(2).cells().isEmpty());
	}
}
```

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class HubShapeTest {
	private final Shape hub = HubShape.standard();

	@Test
	void fitsOneStructureBlock() {
		assertTrue(hub.sizeX() <= 48 && hub.sizeY() <= 48 && hub.sizeZ() <= 48);
	}

	@Test
	void aDoorPerLevelOnThePlatformFacingTheSpawn() {
		for (FightLevel level : FightLevel.DOORS) {
			List<Shape.Cell> door = hub.cells().stream()
					.filter(cell -> cell.material() == Shape.Material.DOOR && cell.doorLevel() == level.number())
					.toList();
			assertEquals(12, door.size(), "a 3 × 4 doorway for " + level);
			for (Shape.Cell cell : door) {
				assertEquals(Shape.Facing.SOUTH, cell.facing());
				assertEquals(Shape.Material.STONE, hub.cellAt(cell.x(), 0, cell.z()).orElseThrow().material(), "on the platform");
			}
		}
	}

	@Test
	void aSignAboveEachDoorAndItsBombsBesideIt() {
		assertEquals(List.of(1, 2, 3), hub.signs().stream().map(Shape.Sign::level).toList());
		assertEquals(List.of(BombTier.KNALLETJE, BombTier.GEWONE_TNT, BombTier.GEWONE_TNT, BombTier.KIPPENBOM, BombTier.KIPPENBOM),
				hub.frames().stream().map(Shape.Frame::bomb).toList());
	}

	@Test
	void theKidsSpawnOnThePlatformFacingTheDoors() {
		Shape.Spot spawn = hub.standAt();
		assertEquals(Shape.Facing.NORTH, spawn.facing());
		assertTrue(hub.cellAt(spawn.x(), 0, spawn.z()).isPresent());
		assertTrue(hub.cellAt(spawn.x(), 1, spawn.z()).isEmpty() && hub.cellAt(spawn.x(), 2, spawn.z()).isEmpty());
	}
}
```

```java
package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ArenaPickerTest {
	private static final List<String> ARENAS = List.of("tafelboem:level_2/kasteel", "tafelboem:level_2/schip", "tafelboem:level_2/bos");

	@Test
	void neverTheSameArenaTwiceInARow() {
		ArenaPicker picker = new ArenaPicker(new Random(3));
		String previous = picker.pick(ARENAS);
		for (int i = 0; i < 200; i++) {
			String current = picker.pick(ARENAS);
			assertNotEquals(previous, current);
			previous = current;
		}
	}

	@Test
	void everyArenaComesUp() {
		ArenaPicker picker = new ArenaPicker(new Random(4));
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 30; i++) {
			seen.add(picker.pick(ARENAS));
		}
		assertEquals(Set.copyOf(ARENAS), seen);
	}

	@Test
	void aSingleArenaIsAlwaysPicked() {
		ArenaPicker picker = new ArenaPicker(new Random(5));
		for (int i = 0; i < 5; i++) {
			assertEquals("default", picker.pick(List.of("default")));
		}
	}
}
```

- [ ] **Step 2: Run** `./gradlew test`. Expected: compile failure.

- [ ] **Step 3: Implement `Shape`**

```java
package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Optional;

/**
 * A plain hub or arena built from code: blocks relative to its corner, and the spots the world
 * needs (where the kids stand, the middle, signs and item frames). Pure Java, so the shapes can be
 * tested without Minecraft.
 */
public record Shape(String name, int sizeX, int sizeY, int sizeZ, List<Cell> cells, Spot standAt, Spot middle,
		List<Sign> signs, List<Frame> frames) {
	public enum Material {
		BEDROCK,
		DIRT,
		GRASS,
		COARSE_DIRT,
		STONE,
		STONE_BRICKS,
		OBSIDIAN,
		DOOR
	}

	public enum Facing {
		NORTH(0, -1),
		SOUTH(0, 1),
		EAST(1, 0),
		WEST(-1, 0);

		private final int dx;
		private final int dz;

		Facing(int dx, int dz) {
			this.dx = dx;
			this.dz = dz;
		}

		public int dx() {
			return dx;
		}

		public int dz() {
			return dz;
		}
	}

	/** One block. A door leads to its level (1–3) or back to the hub (0) and faces where the kids come from. */
	public record Cell(int x, int y, int z, Material material, int doorLevel, Facing facing) {
		static Cell of(int x, int y, int z, Material material) {
			return new Cell(x, y, z, material, 0, Facing.NORTH);
		}
	}

	public record Spot(int x, int y, int z, Facing facing) {
	}

	/** A sign naming a level, on the front of the block behind it. */
	public record Sign(int x, int y, int z, Facing facing, int level) {
	}

	/** An item frame showing a bomb, hanging on the block behind it. */
	public record Frame(int x, int y, int z, Facing facing, BombTier bomb) {
	}

	public Optional<Cell> cellAt(int x, int y, int z) {
		return cells.stream().filter(cell -> cell.x() == x && cell.y() == y && cell.z() == z).findFirst();
	}
}
```

- [ ] **Step 4: Implement `ArenaShape`** (47 wide, centre 23; floor of bedrock, dirt and a top
  layer at y 0–2; obsidian walls at y 3–6; the exit door at (23, 3–4, 46) facing north):

```java
package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain arenas, used while a level has no template: level 1 round on grass, level 2 square on
 * grass, level 3 round on a darker floor. Bedrock below and obsidian walls: the outside never
 * breaks, the inside may.
 */
public final class ArenaShape {
	public static final int SIZE = 47;
	static final int CENTRE = 23;
	static final int WALL_BOTTOM = 3;
	static final int WALL_HEIGHT = 4;

	private interface Area {
		boolean test(int x, int z);
	}

	private ArenaShape() {
	}

	public static Shape forLevel(int level) {
		return switch (level) {
			case 1 -> round(Shape.Material.GRASS);
			case 2 -> square(Shape.Material.GRASS);
			case 3 -> round(Shape.Material.COARSE_DIRT);
			default -> throw new IllegalArgumentException("No arena for level " + level);
		};
	}

	private static Shape round(Shape.Material top) {
		return build(top, (x, z) -> distance(x, z) <= CENTRE + 0.5, (x, z) -> distance(x, z) > CENTRE - 0.5);
	}

	private static Shape square(Shape.Material top) {
		return build(top, (x, z) -> true, (x, z) -> x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1);
	}

	private static Shape build(Shape.Material top, Area floor, Area wall) {
		List<Shape.Cell> cells = new ArrayList<>();
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				if (!floor.test(x, z)) {
					continue;
				}
				cells.add(Shape.Cell.of(x, 0, z, Shape.Material.BEDROCK));
				cells.add(Shape.Cell.of(x, 1, z, Shape.Material.DIRT));
				cells.add(Shape.Cell.of(x, 2, z, top));
				if (!wall.test(x, z)) {
					continue;
				}
				for (int y = WALL_BOTTOM; y < WALL_BOTTOM + WALL_HEIGHT; y++) {
					boolean door = x == CENTRE && z == SIZE - 1 && y < WALL_BOTTOM + 2;
					cells.add(door ? new Shape.Cell(x, y, z, Shape.Material.DOOR, 0, Shape.Facing.NORTH) : Shape.Cell.of(x, y, z, Shape.Material.OBSIDIAN));
				}
			}
		}
		return new Shape("default", SIZE, WALL_BOTTOM + WALL_HEIGHT, SIZE, List.copyOf(cells),
				new Shape.Spot(CENTRE, WALL_BOTTOM, SIZE - 4, Shape.Facing.NORTH), new Shape.Spot(CENTRE, WALL_BOTTOM, CENTRE, Shape.Facing.NORTH),
				List.of(), List.of());
	}

	private static double distance(int x, int z) {
		return Math.hypot(x - CENTRE, z - CENTRE);
	}
}
```

- [ ] **Step 5: Implement `HubShape`** (a round stone platform of radius 12 in a 25 × 6 × 25
  shape; per level a stone-brick frame at z = 5 around a 3 × 4 doorway, centred at x = 4, 12 and
  20; the sign on the lintel's south face; the bombs on the pillars' south faces; the spawn at
  (12, 1, 21) facing north):

```java
package nl.jeeninga.tafelboem.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain hub: a round stone platform with a door per level on its north side, a sign above each
 * door and that level's bombs beside it, and the spawn on the south side facing the doors.
 */
public final class HubShape {
	public static final int SIZE = 25;
	static final int CENTRE = 12;
	static final int RADIUS = 12;
	static final int DOOR_Z = 5;

	private HubShape() {
	}

	public static Shape standard() {
		List<Shape.Cell> cells = new ArrayList<>();
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				if (Math.hypot(x - CENTRE, z - CENTRE) <= RADIUS + 0.5) {
					cells.add(Shape.Cell.of(x, 0, z, Shape.Material.STONE));
				}
			}
		}

		List<Shape.Sign> signs = new ArrayList<>();
		List<Shape.Frame> frames = new ArrayList<>();
		for (FightLevel level : FightLevel.DOORS) {
			int centre = 4 + 8 * (level.number() - 1);
			for (int y = 1; y <= 5; y++) {
				cells.add(Shape.Cell.of(centre - 2, y, DOOR_Z, Shape.Material.STONE_BRICKS));
				cells.add(Shape.Cell.of(centre + 2, y, DOOR_Z, Shape.Material.STONE_BRICKS));
			}
			for (int x = centre - 1; x <= centre + 1; x++) {
				cells.add(Shape.Cell.of(x, 5, DOOR_Z, Shape.Material.STONE_BRICKS));
				for (int y = 1; y <= 4; y++) {
					cells.add(new Shape.Cell(x, y, DOOR_Z, Shape.Material.DOOR, level.number(), Shape.Facing.SOUTH));
				}
			}
			signs.add(new Shape.Sign(centre, 5, DOOR_Z + 1, Shape.Facing.SOUTH, level.number()));
			List<BombTier> bombs = level.bombs();
			frames.add(new Shape.Frame(centre - 2, 2, DOOR_Z + 1, Shape.Facing.SOUTH, bombs.getFirst()));
			if (bombs.size() > 1) {
				frames.add(new Shape.Frame(centre + 2, 2, DOOR_Z + 1, Shape.Facing.SOUTH, bombs.get(1)));
			}
		}
		return new Shape("default", SIZE, 6, SIZE, List.copyOf(cells), new Shape.Spot(CENTRE, 1, 21, Shape.Facing.NORTH),
				new Shape.Spot(CENTRE, 1, CENTRE, Shape.Facing.NORTH), List.copyOf(signs), List.copyOf(frames));
	}
}
```

- [ ] **Step 6: Implement `ArenaPicker`**

```java
package nl.jeeninga.tafelboem.core;

import java.util.List;
import java.util.Random;

/**
 * Picks one of a level's arenas at random, never the same one twice in a row when there is a choice.
 */
public final class ArenaPicker {
	private final Random random;
	private String last;

	public ArenaPicker(Random random) {
		this.random = random;
	}

	public String pick(List<String> names) {
		if (names.isEmpty()) {
			throw new IllegalArgumentException("No arenas to pick from");
		}
		List<String> choices = names.size() > 1 ? names.stream().filter(name -> !name.equals(last)).toList() : names;
		last = choices.get(random.nextInt(choices.size()));
		return last;
	}
}
```

- [ ] **Step 7: Run** `./gradlew test`. Expected: PASS, including `CoreArchitectureTest`.

- [ ] **Step 8: Commit**

```bash
git add -A src
git commit -m "Describe the plain hub and arenas as shapes, and pick arenas at random

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: The level door block

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/block/LevelDoorBlock.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/registry/ModBlocks.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/registry/ModCreativeTab.java`
- Modify: `tools/make_textures.py` (a `--level-door` mode), generate `textures/block/level_door.png`
- Create: `src/main/resources/assets/tafelboem/blockstates/level_door.json`,
  `models/block/level_door.json`, `items/level_door.json`
- Modify: both lang files
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/LevelDoorGameTest.java` (+ register it)

**Interfaces:**
- Produces: `LevelDoorBlock` with `IntegerProperty LEVEL` (0–3) and `EnumProperty<Direction> FACING`;
  `interface LevelDoorBlock.DoorUser { void walkedInto(ServerPlayer player, int level); }`,
  `static LevelDoorBlock.use(DoorUser)`, `LevelDoorBlock.NOBODY`; `ModBlocks.LEVEL_DOOR`,
  `ModBlocks.LEVEL_DOOR_ITEM`; lang keys `block.tafelboem.level_door`,
  `tafelboem.door.item.level`, `tafelboem.door.item.exit`.

- [ ] **Step 1: Write the failing GameTest**

```java
package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/** The hub's doors: you walk through them, they remember their level, and nobody can break them. */
public class LevelDoorGameTest {
	@GameTest
	public void aLevelDoorIsWalkThroughUnbreakableAndRemembersItsLevel(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModBlocks.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.LEVEL, 2));
		BlockState door = helper.getBlockState(pos);
		helper.assertTrue(door.getValue(LevelDoorBlock.LEVEL) == 2, "the door leads to level 2");
		helper.assertTrue(door.getCollisionShape(helper.getLevel(), helper.absolutePos(pos), CollisionContext.empty()).isEmpty(),
				"kids walk through it");
		helper.assertTrue(door.getDestroySpeed(helper.getLevel(), helper.absolutePos(pos)) < 0, "nobody can break it");
		helper.succeed();
	}
}
```

- [ ] **Step 2: Run** `./gradlew runGameTest`. Expected: compile failure.

- [ ] **Step 3: Implement the block**

```java
package nl.jeeninga.tafelboem.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A walk-through door in the hub (level 1–3) or an arena (level 0, back to the hub). It faces where
 * the kids come from: in an arena, the kids arrive 3 blocks in front of it.
 */
public class LevelDoorBlock extends Block {
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Who walked into which door is the TafelBoem world's business; the block only reports it. */
	public interface DoorUser {
		void walkedInto(ServerPlayer player, int level);
	}

	public static final DoorUser NOBODY = (player, level) -> {
	};
	private static DoorUser user = NOBODY;

	public LevelDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LEVEL, 1).setValue(FACING, Direction.NORTH));
	}

	public static void use(DoorUser door) {
		user = door;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LEVEL, FACING);
	}

	/** It faces the builder, so a door placed in an arena wall faces into the arena. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects,
			boolean pastEdges) {
		if (entity instanceof ServerPlayer player) {
			user.walkedInto(player, state.getValue(LEVEL));
		}
	}
}
```

  In `ModBlocks`: `public static LevelDoorBlock LEVEL_DOOR;` and `public static Item LEVEL_DOOR_ITEM;`,
  registered in `register()` like the bombs, with properties
  `.mapColor(MapColor.COLOR_PURPLE).noCollision().strength(-1.0F, 3600000.0F).noLootTable().lightLevel(state -> 11).sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK).setId(key)`.

  In `ModCreativeTab`, after the bombs, a door per level, each placing its own level:

```java
					for (int level = 1; level <= 3; level++) {
						output.accept(door(level, Component.translatable("tafelboem.door.item.level", Component.translatable("tafelboem.level." + level))));
					}
					output.accept(door(0, Component.translatable("tafelboem.door.item.exit")));
```

```java
	private static ItemStack door(int level, Component name) {
		ItemStack door = new ItemStack(ModBlocks.LEVEL_DOOR_ITEM);
		door.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LevelDoorBlock.LEVEL, level));
		door.set(DataComponents.ITEM_NAME, name);
		return door;
	}
```

- [ ] **Step 4: Texture, models and names.**
  - `tools/make_textures.py`, a new section at the end (like the Graaf Fout one):
    ```python
    # --- Level door -------------------------------------------------------------------------------
    # Usage: python3 tools/make_textures.py --level-door
    # A purple swirl like a portal, so the kids see at once that the door takes them somewhere.

    DOOR_DARK = (70, 25, 120)
    DOOR_LIGHT = (160, 80, 230)


    def level_door():
        import math

        img = blank(DOOR_DARK)
        for x in range(16):
            for y in range(16):
                dx, dy = x - 7.5, y - 7.5
                swirl = math.atan2(dy, dx) * 2 + math.hypot(dx, dy) * 0.9
                if int(math.floor(swirl)) % 2 == 0:
                    img.putpixel((x, y), DOOR_LIGHT + (255,))
        img.save(OUT / "level_door.png")
        print("wrote level_door")


    if __name__ == "__main__":
        import sys

        if len(sys.argv) == 2 and sys.argv[1] == "--level-door":
            level_door()
    ```
    Run `python3 tools/make_textures.py --level-door` and view the PNG with the Read tool. (The
    first `__main__` block regenerates the bomb textures identically; that is fine.)
  - `blockstates/level_door.json`: `{"variants": {"": {"model": "tafelboem:block/level_door"}}}`
  - `models/block/level_door.json`: `{"parent": "minecraft:block/cube_all", "textures": {"all": "tafelboem:block/level_door"}}`
  - `items/level_door.json`: `{"model": {"type": "minecraft:model", "model": "tafelboem:block/level_door"}}`
  - Lang:

  | Key | en_us | nl_nl |
  |---|---|---|
  | `block.tafelboem.level_door` | TafelBoem door | TafelBoem-deur |
  | `tafelboem.door.item.level` | Door to %s | Deur naar %s |
  | `tafelboem.door.item.exit` | Exit to the hub | Uitgang naar de hub |

- [ ] **Step 5: Run** `./gradlew build` and `./gradlew runGameTest`. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A src tools
git commit -m "Add the level door: walk-through, unbreakable, one per level and an exit

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: The world type, the hub, and placing arenas

**Files:**
- Create: `src/main/resources/data/tafelboem/worldgen/world_preset/tafelboem.json`
- Create: `src/main/resources/data/tafelboem/worldgen/biome/void.json`
- Create: `src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/TafelBoemWorlds.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/WorldLayout.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/Materials.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/PlacedArea.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/ShapePlacer.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/Hub.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/ArenaCatalog.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/command/ArenaSetup.java` (extract `applyRules`)
- Modify: both lang files (`generator.tafelboem.tafelboem`, `tafelboem.door.sign`)
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/TestWorld.java` (shared helper)
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/HubAndArenasGameTest.java` (+ register it)

**Interfaces:**
- Consumes: `Shape`, `HubShape`, `ArenaShape`, `ArenaPicker` (Task 7), `LevelDoorBlock` (Task 8),
  `ProtectedAreas` (Task 6), `ArenaBox` (Task 5).
- Produces:
  - `TafelBoemWorlds.VOID`, `static boolean TafelBoemWorlds.isTafelBoemWorld(ServerLevel)`.
  - `record WorldLayout(BlockPos hub, Map<FightLevel, BlockPos> arenas)` with `SLOT = 48`,
    `DEFAULT`, `static WorldLayout at(BlockPos hubCorner, int spacing)`, `ArenaBox hubSlot()`,
    `ArenaBox arenaSlot(FightLevel)`, `int voidY()`, `Optional<FightLevel> arenaAt(double x, double z)`,
    `static BlockPos cornerFor(ArenaBox slot, int sizeX, int sizeZ)`.
  - `record PlacedArea(String name, ArenaBox bounds, BlockPos standAt, float yaw, Vec3 middle, Map<Integer, List<BlockPos>> doors)`.
  - `ShapePlacer.clear(ServerLevel, ArenaBox)`, `ShapePlacer.placeShape(ServerLevel, BlockPos corner, Shape)`,
    `ShapePlacer.placeTemplate(ServerLevel, BlockPos corner, String name, StructureTemplate)`.
  - `Hub.TEMPLATE`, `static PlacedArea Hub.place(ServerLevel, WorldLayout)`.
  - `ArenaCatalog(StructureTemplateManager, Random, String folder)` with `DEFAULT_FOLDER = "level_"`,
    `List<Identifier> templatesFor(FightLevel)`,
    `PlacedArea place(ServerLevel, WorldLayout, FightLevel, Consumer<String> skipped)`,
    `static boolean hasExitDoor(StructureTemplate)`.
  - `static void ArenaSetup.applyRules(ServerLevel)`.
  - Gametest helper `TestWorld` (see Step 1).

- [ ] **Step 1: The shared GameTest helper and the failing tests.** `TestWorld` builds a world
  layout at the test's corner (hub slot first, then the arena slots 56 blocks apart), keeps those
  chunks ticking, and (from Task 10) runs a `TafelBoemWorld` there. Tests that use it need
  `padding = 232`. In this task it only offers the layout and the level:

```java
package nl.jeeninga.tafelboem.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.world.WorldLayout;

/**
 * A TafelBoem world inside a GameTest: the hub slot at the test's corner and the arena slots beside
 * it, 56 blocks apart. Use {@code padding = 232} so neighbouring tests stay clear.
 */
final class TestWorld {
	static final int SPACING = 56;
	static final int PADDING = 232;

	final GameTestHelper helper;
	final ServerLevel level;
	final WorldLayout layout;

	TestWorld(GameTestHelper helper) {
		this.helper = helper;
		this.level = helper.getLevel();
		this.layout = WorldLayout.at(helper.absolutePos(BlockPos.ZERO), SPACING);
		// Only the test's own chunks are force-loaded; keep the slots ticking too (see OpenField).
		helper.onEachTick(() -> {
			keepLoaded(layout.hubSlot());
			for (FightLevel fightLevel : FightLevel.DOORS) {
				keepLoaded(layout.arenaSlot(fightLevel));
			}
		});
	}

	private void keepLoaded(ArenaBox slot) {
		BlockPos centre = new BlockPos((int) slot.middleX(), slot.minY(), (int) slot.middleZ());
		level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(centre), 4);
	}
}
```

```java
package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.world.ArenaCatalog;
import nl.jeeninga.tafelboem.world.Hub;
import nl.jeeninga.tafelboem.world.PlacedArea;

/**
 * The hub and the arenas of a TafelBoem world, placed from code or from structure templates.
 */
public class HubAndArenasGameTest {
	@GameTest(padding = TestWorld.PADDING)
	public void theHubHasADoorPerLevelWithItsSignAndBombs(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		try {
			PlacedArea hub = Hub.place(world.level, world.layout);
			for (int level = 1; level <= 3; level++) {
				helper.assertTrue(hub.doors().getOrDefault(level, List.of()).size() == 12, "a 3 × 4 door to level " + level);
			}
			List<BombTier> bombs = new ArrayList<>();
			for (ItemFrame frame : world.level.getEntitiesOfClass(ItemFrame.class, aabb(world.layout.hubSlot()))) {
				for (BombTier tier : BombTier.values()) {
					if (frame.getItem().is(ModBlocks.bombItem(tier))) {
						bombs.add(tier);
					}
				}
			}
			helper.assertTrue(bombs.size() == 5, "five bombs in frames beside the doors, found " + bombs);
			int signs = 0;
			for (BlockPos pos : BlockPos.betweenClosed(corner(world.layout.hubSlot()), far(world.layout.hubSlot()))) {
				if (world.level.getBlockEntity(pos) instanceof SignBlockEntity sign
						&& sign.getFrontText().getMessage(1, false).getContents() instanceof TranslatableContents text
						&& text.getKey().equals("tafelboem.door.sign")) {
					signs++;
				}
			}
			helper.assertTrue(signs == 3, "a sign above each door, found " + signs);
		} finally {
			ProtectedAreas.unprotect(world.level, world.layout.hubSlot());
		}
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aBoomInTheHubBreaksNothingAndTheHubRepairsItself(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		try {
			PlacedArea hub = Hub.place(world.level, world.layout);
			BlockPos bomb = hub.standAt();
			BlockPos floor = bomb.below();
			BlockPos broken = floor.east(3);
			world.level.setBlockAndUpdate(broken, Blocks.AIR.defaultBlockState());
			world.level.setBlockAndUpdate(bomb, ModBlocks.bomb(BombTier.GEWONE_TNT).defaultBlockState());
			BombEffects.boom(world.level, bomb, BombTier.GEWONE_TNT);
			helper.assertTrue(world.level.getBlockState(floor).is(Blocks.STONE), "the hub floor under the boom stays");

			Hub.place(world.level, world.layout);
			helper.assertTrue(world.level.getBlockState(broken).is(Blocks.STONE), "placing the hub again repairs it");
		} finally {
			ProtectedAreas.unprotect(world.level, world.layout.hubSlot());
		}
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void withTwoTemplatesInALevelOneOfThemIsUsedAndNeverTwiceInARow(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		String folder = "test_choice_level_";
		saveTemplate(world, folder + "2/gold", Blocks.GOLD_BLOCK, true);
		saveTemplate(world, folder + "2/diamond", Blocks.DIAMOND_BLOCK, true);
		ArenaCatalog catalog = new ArenaCatalog(world.level.getServer().getStructureManager(), new Random(7), folder);

		String previous = null;
		for (int round = 0; round < 6; round++) {
			PlacedArea arena = catalog.place(world.level, world.layout, FightLevel.GEMIDDELD, name -> helper.fail("skipped " + name));
			boolean gold = contains(world, world.layout.arenaSlot(FightLevel.GEMIDDELD), Blocks.GOLD_BLOCK);
			boolean diamond = contains(world, world.layout.arenaSlot(FightLevel.GEMIDDELD), Blocks.DIAMOND_BLOCK);
			helper.assertTrue(gold != diamond, "exactly one of the two templates is placed");
			helper.assertTrue(!arena.name().equals(previous), "never the same arena twice in a row");
			previous = arena.name();
		}
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aTemplateWithoutAnExitDoorIsSkipped(GameTestHelper helper) {
		TestWorld world = new TestWorld(helper);
		String folder = "test_skip_level_";
		saveTemplate(world, folder + "3/no_exit", Blocks.GOLD_BLOCK, false);
		ArenaCatalog catalog = new ArenaCatalog(world.level.getServer().getStructureManager(), new Random(8), folder);
		List<String> skipped = new ArrayList<>();

		PlacedArea arena = catalog.place(world.level, world.layout, FightLevel.MOEILIJK, skipped::add);

		helper.assertTrue(skipped.equals(List.of("tafelboem:" + folder + "3/no_exit")), "the template is skipped: " + skipped);
		helper.assertTrue(arena.name().equals("default"), "the plain arena is used instead");
		helper.assertTrue(world.level.getBlockState(arena.standAt().below()).is(Blocks.COARSE_DIRT), "level 3's darker floor");
		helper.succeed();
	}

	/** Builds a 9 × 3 × 9 arena at the test's corner and saves it the way a structure block does. */
	private static void saveTemplate(TestWorld world, String path, Block marker, boolean exitDoor) {
		ServerLevel level = world.level;
		// 60 blocks above the test's corner: clear of the hub slot, which is 48 high.
		BlockPos build = world.helper.absolutePos(new BlockPos(0, 60, 0));
		for (BlockPos pos : BlockPos.betweenClosed(build, build.offset(8, 2, 8))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		for (BlockPos pos : BlockPos.betweenClosed(build, build.offset(8, 0, 8))) {
			level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
		}
		level.setBlockAndUpdate(build.offset(4, 1, 4), marker.defaultBlockState());
		if (exitDoor) {
			level.setBlockAndUpdate(build.offset(4, 1, 8), ModBlocks.LEVEL_DOOR.defaultBlockState().setValue(LevelDoorBlock.LEVEL, 0));
		}
		StructureTemplateManager manager = level.getServer().getStructureManager();
		Identifier id = TafelBoem.id(path);
		StructureTemplate template = manager.getOrCreate(id);
		template.fillFromWorld(level, build, new Vec3i(9, 3, 9), false, List.of());
		manager.save(id);
		for (BlockPos pos : BlockPos.betweenClosed(build, build.offset(8, 2, 8))) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	private static boolean contains(TestWorld world, ArenaBox slot, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(corner(slot), far(slot))) {
			if (world.level.getBlockState(pos).is(block)) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos corner(ArenaBox box) {
		return new BlockPos(box.minX(), box.minY(), box.minZ());
	}

	private static BlockPos far(ArenaBox box) {
		return new BlockPos(box.maxX(), box.maxY(), box.maxZ());
	}

	private static AABB aabb(ArenaBox box) {
		return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
	}
}
```

  Register `HubAndArenasGameTest` (and, from Task 8, `LevelDoorGameTest`) in
  `src/gametest/resources/fabric.mod.json`.

- [ ] **Step 2: Run** `./gradlew compileGametestJava`. Expected: compile failure (the `world`
  package does not exist).

- [ ] **Step 3: The world type.** Create the three data files with exactly the contents verified by
  the spike (see the spec's *Spike results*; also on branch `spike/r1-world`):
  - `data/tafelboem/worldgen/world_preset/tafelboem.json`: the vanilla `flat` world preset with the
    overworld's settings replaced by `"biome": "tafelboem:void"`, `"features": false`,
    `"lakes": false`, `"layers": [{"block": "minecraft:air", "height": 1}]`,
    `"structure_overrides": []`; the nether and end entries as in vanilla's `flat.json`.
  - `data/tafelboem/worldgen/biome/void.json`: vanilla's `the_void` biome with `"features": []`
    (so no stone start platform).
  - `data/minecraft/tags/worldgen/world_preset/normal.json`:
    `{"replace": false, "values": ["tafelboem:tafelboem"]}`.
  - Lang `generator.tafelboem.tafelboem`: `TafelBoem` in both files.

```java
package nl.jeeninga.tafelboem.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.FlatLevelSource;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Recognises a TafelBoem world. A world doesn't remember its world type, so it goes by the
 * overworld: a flat generator with our void biome (vanilla's {@code the_void} without its platform).
 */
public final class TafelBoemWorlds {
	public static final ResourceKey<Biome> VOID = ResourceKey.create(Registries.BIOME, TafelBoem.id("void"));

	private TafelBoemWorlds() {
	}

	public static boolean isTafelBoemWorld(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD
				&& level.getChunkSource().getGenerator() instanceof FlatLevelSource flat
				&& flat.settings().getBiome().is(VOID);
	}
}
```

- [ ] **Step 4: Layout, materials and the placed area**

```java
package nl.jeeninga.tafelboem.world;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;

import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * Where the hub and the arena slots are. Every slot is a 48³ box (what one structure block can
 * save); a hub or arena is placed in the middle of its slot.
 */
public record WorldLayout(BlockPos hub, Map<FightLevel, BlockPos> arenas) {
	public static final int SLOT = 48;
	/** The real world: the hub around spawn, the arenas 512 blocks apart so they never see each other. */
	public static final WorldLayout DEFAULT = at(new BlockPos(-SLOT / 2, 60, -SLOT / 2), 512);

	public static WorldLayout at(BlockPos hubCorner, int spacing) {
		Map<FightLevel, BlockPos> arenas = new EnumMap<>(FightLevel.class);
		for (FightLevel level : FightLevel.DOORS) {
			arenas.put(level, hubCorner.offset(spacing * level.number(), 0, 0));
		}
		return new WorldLayout(hubCorner, Map.copyOf(arenas));
	}

	public ArenaBox hubSlot() {
		return ArenaBox.of(hub.getX(), hub.getY(), hub.getZ(), SLOT, SLOT, SLOT);
	}

	public ArenaBox arenaSlot(FightLevel level) {
		BlockPos corner = arenas.get(level);
		return ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), SLOT, SLOT, SLOT);
	}

	/** Below this a kid has fallen into the void. */
	public int voidY() {
		return hub.getY() - 8;
	}

	/** The arena slot above or below this spot, if any. */
	public Optional<FightLevel> arenaAt(double x, double z) {
		return FightLevel.DOORS.stream().filter(level -> arenaSlot(level).containsColumn(x, z)).findFirst();
	}

	/** Where something of this size goes so it sits in the middle of the slot, on its bottom. */
	public static BlockPos cornerFor(ArenaBox slot, int sizeX, int sizeZ) {
		return new BlockPos(slot.minX() + (SLOT - sizeX) / 2, slot.minY(), slot.minZ() + (SLOT - sizeZ) / 2);
	}
}
```

```java
package nl.jeeninga.tafelboem.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/** The vanilla block for each material of a {@link Shape}. */
final class Materials {
	private Materials() {
	}

	static BlockState state(Shape.Cell cell) {
		return switch (cell.material()) {
			case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
			case DIRT -> Blocks.DIRT.defaultBlockState();
			case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case COARSE_DIRT -> Blocks.COARSE_DIRT.defaultBlockState();
			case STONE -> Blocks.STONE.defaultBlockState();
			case STONE_BRICKS -> Blocks.STONE_BRICKS.defaultBlockState();
			case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
			case DOOR -> ModBlocks.LEVEL_DOOR.defaultBlockState()
					.setValue(LevelDoorBlock.LEVEL, cell.doorLevel())
					.setValue(LevelDoorBlock.FACING, direction(cell.facing()));
		};
	}

	static Direction direction(Shape.Facing facing) {
		return switch (facing) {
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case EAST -> Direction.EAST;
			case WEST -> Direction.WEST;
		};
	}
}
```

```java
package nl.jeeninga.tafelboem.world;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.core.ArenaBox;

/**
 * A hub or arena as it was placed: where the kids stand (the spawn, or 3 blocks in front of the
 * exit door), where Graaf Fout appears, and every level door in it.
 *
 * @param name the template's name, or "default" for the plain one from code
 */
public record PlacedArea(String name, ArenaBox bounds, BlockPos standAt, float yaw, Vec3 middle, Map<Integer, List<BlockPos>> doors) {
}
```

- [ ] **Step 5: `ShapePlacer`**

```java
package nl.jeeninga.tafelboem.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * Puts a hub or an arena in its slot: either a plain {@link Shape} from code or a structure template
 * the kids saved. Placing again is how a hub repairs itself and an arena loses its craters.
 */
public final class ShapePlacer {
	private ShapePlacer() {
	}

	/** Empties a slot: every block to air, every entity but the players gone (items, chickens, frames). */
	public static void clear(ServerLevel level, ArenaBox slot) {
		for (BlockPos pos : BlockPos.betweenClosed(slot.minX(), slot.minY(), slot.minZ(), slot.maxX(), slot.maxY(), slot.maxZ())) {
			if (!level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		level.getEntities((Entity) null, aabb(slot), entity -> !(entity instanceof Player)).forEach(Entity::discard);
	}

	public static PlacedArea placeShape(ServerLevel level, BlockPos corner, Shape shape) {
		for (Shape.Cell cell : shape.cells()) {
			level.setBlock(corner.offset(cell.x(), cell.y(), cell.z()), Materials.state(cell), Block.UPDATE_CLIENTS);
		}
		for (Shape.Sign sign : shape.signs()) {
			placeSign(level, corner.offset(sign.x(), sign.y(), sign.z()), sign);
		}
		for (Shape.Frame frame : shape.frames()) {
			placeFrame(level, corner.offset(frame.x(), frame.y(), frame.z()), frame);
		}
		ArenaBox bounds = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), shape.sizeX(), shape.sizeY(), shape.sizeZ());
		Shape.Spot stand = shape.standAt();
		Shape.Spot middle = shape.middle();
		return new PlacedArea(shape.name(), bounds, corner.offset(stand.x(), stand.y(), stand.z()), Materials.direction(stand.facing()).toYRot(),
				Vec3.atBottomCenterOf(corner.offset(middle.x(), middle.y(), middle.z())), doors(level, bounds));
	}

	/**
	 * Places a template. The kids arrive 3 blocks in front of its exit door, on the highest solid
	 * block there; a template without one (a hub) puts them in its middle.
	 */
	public static PlacedArea placeTemplate(ServerLevel level, BlockPos corner, String name, StructureTemplate template) {
		template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
		Vec3i size = template.getSize();
		ArenaBox bounds = ArenaBox.of(corner.getX(), corner.getY(), corner.getZ(), size.getX(), size.getY(), size.getZ());
		Map<Integer, List<BlockPos>> doors = doors(level, bounds);
		BlockPos middle = ground(level, (int) Math.floor(bounds.middleX()), (int) Math.floor(bounds.middleZ()), bounds);

		List<BlockPos> exits = doors.getOrDefault(0, List.of());
		if (exits.isEmpty()) {
			return new PlacedArea(name, bounds, middle, Direction.NORTH.toYRot(), Vec3.atBottomCenterOf(middle), doors);
		}
		BlockPos exit = exits.getFirst();
		Direction facing = level.getBlockState(exit).getValue(LevelDoorBlock.FACING);
		BlockPos front = exit.relative(facing, 3);
		return new PlacedArea(name, bounds, ground(level, front.getX(), front.getZ(), bounds), facing.toYRot(), Vec3.atBottomCenterOf(middle), doors);
	}

	/** Every level door in the area, by level, lowest blocks first. */
	static Map<Integer, List<BlockPos>> doors(ServerLevel level, ArenaBox bounds) {
		Map<Integer, List<BlockPos>> doors = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlocks.LEVEL_DOOR)) {
				doors.computeIfAbsent(state.getValue(LevelDoorBlock.LEVEL), key -> new ArrayList<>()).add(pos.immutable());
			}
		}
		return doors;
	}

	/** The block above the highest solid block of this column inside the area. */
	private static BlockPos ground(ServerLevel level, int x, int z, ArenaBox bounds) {
		for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (level.getBlockState(pos).isSolid()) {
				return pos.above();
			}
		}
		return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
	}

	private static void placeSign(ServerLevel level, BlockPos pos, Shape.Sign sign) {
		level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Materials.direction(sign.facing())),
				Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(pos) instanceof SignBlockEntity entity) {
			// Translatable text, so every PC shows it in its own game language.
			entity.setText(entity.getFrontText()
					.setMessage(1, Component.translatable("tafelboem.door.sign", sign.level()))
					.setMessage(2, Component.translatable("tafelboem.level." + sign.level())), true);
			entity.setWaxed(true);
			entity.setChanged();
			level.sendBlockUpdated(pos, entity.getBlockState(), entity.getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Item frames can only be made fixed through their saved data, so the vanilla command does it. */
	private static void placeFrame(ServerLevel level, BlockPos pos, Shape.Frame frame) {
		String item = BuiltInRegistries.ITEM.getKey(ModBlocks.bombItem(frame.bomb())).toString();
		String command = String.format(Locale.ROOT, "summon minecraft:item_frame %d %d %d {Facing:%db,Fixed:1b,Item:{id:\"%s\",count:1}}",
				pos.getX(), pos.getY(), pos.getZ(), Materials.direction(frame.facing()).get3DDataValue(), item);
		MinecraftServer server = level.getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), command);
	}

	private static AABB aabb(ArenaBox box) {
		return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
	}
}
```

  If the summon command's NBT doesn't produce a fixed frame with the bomb in 26.2 (the GameTest
  counts the frames' bombs), check the item frame's codec field names in the 26.2 jar
  (`javap -p net.minecraft.world.entity.decoration.ItemFrame`) and adjust the NBT; don't fall back to
  unfixed frames, or the kids can take free bombs out of them.

- [ ] **Step 6: `Hub` and `ArenaCatalog`**

```java
package nl.jeeninga.tafelboem.world;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.HubShape;
import nl.jeeninga.tafelboem.game.ProtectedAreas;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * The hub of a TafelBoem world: built from code unless a template {@code tafelboem:hub} with all
 * three level doors exists. It is placed on every world load, so it repairs itself, and booms
 * break nothing in it.
 */
public final class Hub {
	public static final Identifier TEMPLATE = TafelBoem.id("hub");

	private Hub() {
	}

	public static PlacedArea place(ServerLevel level, WorldLayout layout) {
		ArenaBox slot = layout.hubSlot();
		ShapePlacer.clear(level, slot);
		Optional<StructureTemplate> template = level.getServer().getStructureManager().get(TEMPLATE).filter(Hub::hasAllDoors);
		PlacedArea hub = template
				.map(found -> ShapePlacer.placeTemplate(level, WorldLayout.cornerFor(slot, found.getSize().getX(), found.getSize().getZ()),
						TEMPLATE.toString(), found))
				.orElseGet(() -> ShapePlacer.placeShape(level, WorldLayout.cornerFor(slot, HubShape.SIZE, HubShape.SIZE), HubShape.standard()));
		ProtectedAreas.protect(level, slot);
		return hub;
	}

	private static boolean hasAllDoors(StructureTemplate template) {
		Set<Integer> levels = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.LEVEL_DOOR).stream()
				.map(info -> info.state().getValue(LevelDoorBlock.LEVEL))
				.collect(Collectors.toSet());
		if (!levels.containsAll(Set.of(1, 2, 3))) {
			TafelBoem.LOGGER.warn("The hub template {} needs a door to level 1, 2 and 3; using the plain hub", TEMPLATE);
			return false;
		}
		return true;
	}
}
```

```java
package nl.jeeninga.tafelboem.world;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.block.LevelDoorBlock;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.ArenaPicker;
import nl.jeeninga.tafelboem.core.ArenaShape;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.Shape;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * Each level's arenas: the templates named {@code tafelboem:level_<n>/<name>} (in the world's own
 * folder or in the mod), or the plain arena from code when there are none. A template without an
 * exit door is skipped.
 */
public final class ArenaCatalog {
	public static final String DEFAULT_FOLDER = "level_";

	private final StructureTemplateManager templates;
	private final Random random;
	private final String folder;
	private final Map<FightLevel, ArenaPicker> pickers = new EnumMap<>(FightLevel.class);

	/** @param folder {@link #DEFAULT_FOLDER}; GameTests use their own, so their templates don't mix */
	public ArenaCatalog(StructureTemplateManager templates, Random random, String folder) {
		this.templates = templates;
		this.random = random;
		this.folder = folder;
	}

	public List<Identifier> templatesFor(FightLevel level) {
		String prefix = folder + level.number() + "/";
		return templates.listTemplates()
				.filter(id -> id.getNamespace().equals(TafelBoem.MOD_ID) && id.getPath().startsWith(prefix))
				.sorted()
				.toList();
	}

	/** Clears the level's slot and places one of its arenas, picked at random, never twice in a row. */
	public PlacedArea place(ServerLevel level, WorldLayout layout, FightLevel fightLevel, Consumer<String> skipped) {
		ArenaBox slot = layout.arenaSlot(fightLevel);
		List<Identifier> candidates = new ArrayList<>(templatesFor(fightLevel));
		ArenaPicker picker = pickers.computeIfAbsent(fightLevel, key -> new ArenaPicker(random));
		while (!candidates.isEmpty()) {
			String name = picker.pick(candidates.stream().map(Identifier::toString).toList());
			Identifier id = Identifier.parse(name);
			Optional<StructureTemplate> template = templates.get(id);
			if (template.isPresent() && hasExitDoor(template.get())) {
				ShapePlacer.clear(level, slot);
				StructureTemplate found = template.get();
				return ShapePlacer.placeTemplate(level, WorldLayout.cornerFor(slot, found.getSize().getX(), found.getSize().getZ()), name, found);
			}
			skipped.accept(name);
			candidates.remove(id);
		}
		ShapePlacer.clear(level, slot);
		Shape shape = ArenaShape.forLevel(fightLevel.number());
		return ShapePlacer.placeShape(level, WorldLayout.cornerFor(slot, shape.sizeX(), shape.sizeZ()), shape);
	}

	public static boolean hasExitDoor(StructureTemplate template) {
		return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), ModBlocks.LEVEL_DOOR).stream()
				.anyMatch(info -> info.state().getValue(LevelDoorBlock.LEVEL) == 0);
	}
}
```

- [ ] **Step 7: `ArenaSetup.applyRules`.** Move the game-rule lines and the `time set day` /
  `weather clear` commands of `apply` into
  `/** Always day, no weather, no mobs, keep inventory: also the rules of a TafelBoem world. */ public static void applyRules(ServerLevel level)`,
  and call it first in `apply`. Lang `tafelboem.door.sign`: `Level %s` in both files.

- [ ] **Step 8: Run** `./gradlew build` and `./gradlew runGameTest`. Expected: PASS, including the
  four new GameTests and `ArenaSetupGameTest`.

- [ ] **Step 9: Commit**

```bash
git add -A src
git commit -m "Add the TafelBoem world type, the hub, and arenas from code or templates

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Playing the TafelBoem world: doors, fights, stars and the way home

**Files:**
- Create: `src/main/java/nl/jeeninga/tafelboem/world/ArenaRun.java`
- Create: `src/main/java/nl/jeeninga/tafelboem/world/TafelBoemWorld.java`
- Modify: `src/main/java/nl/jeeninga/tafelboem/TafelBoem.java` (wiring)
- Modify: both lang files
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/TestWorld.java` (run a world)
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/TafelBoemWorldGameTest.java` (+ register it)

**Interfaces:**
- Consumes: everything from Tasks 3, 5, 6, 8 and 9.
- Produces:
  - `ArenaRun` (package-private mutators) with `FightLevel level()`, `PlacedArea arena()`,
    `@Nullable BossFight fight()`, `Set<UUID> kids()`, `Set<UUID> everyone()`,
    `boolean isReturning()`.
  - `TafelBoemWorld(ServerLevel, WorldLayout, BossFights, FightLog, ArenaCatalog, QuizClient)` with
    `setUp()`, `PlacedArea placeHub()`, `PlacedArea hub()`, `Optional<ArenaRun> run(FightLevel)`,
    `enterDoor(ServerPlayer, int)`, `tick()`, `onJoin(ServerPlayer)`, `onDisconnect(ServerPlayer)`.
  - `static Optional<TafelBoemWorld> TafelBoem.world()`.
  - Lang keys `tafelboem.stars.new_best`, `tafelboem.door.stars`, `tafelboem.door.busy`,
    `tafelboem.arena.skipped`.

- [ ] **Step 1: Let `TestWorld` run a world.** Add to `TestWorld` (and its imports):

```java
	final RecordingQuizClient client = new RecordingQuizClient();
	SessionManager sessions;
	BossFights fights;
	FightLog log;
	TafelBoemWorld world;
	private final Set<Integer> answered = new HashSet<>();
	private final Set<UUID> wrongOnce = new HashSet<>();

	/** Runs a TafelBoem world in the layout: the hub placed, every question answered right. */
	TestWorld running() {
		Path dir = tempDir();
		sessions = new SessionManager(level.getServer(), new Learners(new AnswerLog(dir)), client);
		fights = new BossFights(sessions);
		log = new FightLog(dir);
		world = new TafelBoemWorld(level, layout, fights, log,
				new ArenaCatalog(level.getServer().getStructureManager(), new Random(1), ArenaCatalog.DEFAULT_FOLDER), client);
		world.placeHub();
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			world.tick();
			answerQuestions();
		});
		return this;
	}

	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	ServerPlayer kidInTheHub() {
		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		BlockPos at = world.hub().standAt();
		kid.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		return kid;
	}

	/** This kid's next answer is wrong, the ones after it right. */
	void answerWrongOnce(ServerPlayer kid) {
		wrongOnce.add(kid.getUUID());
	}

	void close() {
		fights.endAll();
		ProtectedAreas.unprotect(level, layout.hubSlot());
	}

	private void answerQuestions() {
		for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
			QuizS2C question = sent.payload();
			if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
				int answer = wrongOnce.remove(sent.player().getUUID()) ? question.a() * question.b() + 1 : question.a() * question.b();
				sessions.onClientAction(sent.player(), new QuizC2S(question.sessionId(), QuizC2S.ANSWER, answer, QuizC2S.VIA_KEY));
			}
		}
	}

	private static Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
```

- [ ] **Step 2: Write the failing GameTests**

```java
package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * Playing a TafelBoem world: through a door into a fight, together, to the stars, and back home.
 */
public class TafelBoemWorldGameTest {
	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void aLevelDoorLeadsToAFightAtThatLevel(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 2);

		helper.assertTrue(test.layout.arenaSlot(FightLevel.GEMIDDELD).containsColumn(kid.getX(), kid.getZ()), "the kid is in the level 2 arena");
		BlockPos below = kid.blockPosition().below();
		helper.assertTrue(test.level.getBlockState(below).is(Blocks.GRASS_BLOCK), "standing on the arena's grass");
		helper.succeedWhen(() -> {
			BossFight fight = test.world.run(FightLevel.GEMIDDELD).map(ArenaRun::fight).orElse(null);
			helper.assertTrue(fight != null, "wait for Graaf Fout");
			helper.assertTrue(fight.fightLevel() == FightLevel.GEMIDDELD, "a level 2 fight");
			helper.assertTrue(fight.arena() != null, "in its arena");
			test.close();
		});
	}

	@GameTest(maxTicks = 2400, padding = TestWorld.PADDING)
	public void everyKidThroughTheSameDoorJoinsTheSameFight(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer first = test.kidInTheHub();
		ServerPlayer second = test.kidInTheHub();
		ServerPlayer third = test.kidInTheHub();
		test.world.enterDoor(first, 1);
		boolean[] othersIn = {false};
		helper.onEachTick(() -> {
			// The other two come in once he's there.
			if (!othersIn[0] && test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).isPresent()) {
				test.world.enterDoor(second, 1);
				test.world.enterDoor(third, 1);
				othersIn[0] = true;
			}
		});

		helper.succeedWhen(() -> {
			BossFight fight = test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).orElse(null);
			helper.assertTrue(fight != null, "wait for Graaf Fout");
			for (ServerPlayer kid : List.of(first, second, third)) {
				helper.assertTrue(fight.tally(kid.getUUID()).answers() >= 1, "wait until every kid has had a turn");
			}
			test.close();
		});
	}

	@GameTest(maxTicks = 9000, padding = TestWorld.PADDING)
	public void aWonFightPaysTheRewardWritesTheStarsAndSendsEveryoneHome(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.answerWrongOnce(kid);
		test.world.enterDoor(kid, 2);

		helper.succeedWhen(() -> {
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().won(), "wait for the win");
			FightRecord won = fights.getFirst();
			helper.assertTrue(won.level() == 2 && won.wrong() == 1 && won.stars() == 2, "one wrong answer is ★★: " + won);
			helper.assertTrue(kid.getInventory().countItem(ModBlocks.bombItem(BombTier.KIPPENBOM)) == 4, "4 Kippenbommen");
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "wait until everyone is home");
			helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "back in the hub");
			test.close();
		});
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void theLastKidLeavingEndsTheFight(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 2);
		BossFight[] fight = {null};
		helper.onEachTick(() -> {
			if (fight[0] == null) {
				fight[0] = test.world.run(FightLevel.GEMIDDELD).map(ArenaRun::fight).orElse(null);
				if (fight[0] != null) {
					test.world.enterDoor(kid, 0);
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(fight[0] != null && fight[0].isOver(), "the fight is over");
			helper.assertFalse(fight[0].boss().isAlive(), "he poofed away");
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "the arena is free");
			helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "the kid is in the hub");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().result().equals(FightRecord.LEFT), "logged as left: " + fights);
			test.close();
		});
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void aKidFallingIntoTheVoidLandsSafely(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		kid.teleportTo(kid.getX(), test.layout.voidY() - 5, kid.getZ());
		boolean[] inArena = {false};
		helper.onEachTick(() -> {
			if (!inArena[0] && test.world.hub().bounds().holds(kid.getX(), kid.getY(), kid.getZ())) {
				test.world.enterDoor(kid, 1);
				kid.teleportTo(kid.getX(), test.layout.voidY() - 5, kid.getZ());
				inArena[0] = true;
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(inArena[0], "first back in the hub");
			BlockPos arrival = test.world.run(FightLevel.MAKKELIJK).orElseThrow().arena().standAt();
			helper.assertTrue(kid.blockPosition().equals(arrival), "then back at the arena's arrival spot");
			test.close();
		});
	}

	@GameTest(maxTicks = 200, padding = TestWorld.PADDING)
	public void walkingUpToADoorShowsYourOwnStars(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.log.append(new FightRecord(0L, kid.getPlainTextName(), 2, "default", FightRecord.WON, 12, 1, 0, 0, 2, 120, 1, "1.0.0"));
		BlockPos door = test.world.hub().doors().get(2).getFirst();
		kid.teleportTo(door.getX() + 0.5, door.getY(), door.getZ() + 1.5);

		helper.succeedWhen(() -> {
			helper.assertTrue(test.client.actionBars.stream().anyMatch(TafelBoemWorldGameTest::showsTwoStars), "the action bar shows ★★☆");
			test.close();
		});
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aKidWhoLogsInInsideAnEmptyArenaGoesToTheHub(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		kid.teleportTo(test.layout.arenaSlot(FightLevel.MOEILIJK).middleX(), test.layout.hub().getY() + 4,
				test.layout.arenaSlot(FightLevel.MOEILIJK).middleZ());
		test.world.onJoin(kid);
		helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "in the hub");
		test.close();
		helper.succeed();
	}

	private static boolean showsTwoStars(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals("tafelboem.door.stars")
				&& List.of(translatable.getArgs()).contains("★★☆");
	}
}
```

  Register `TafelBoemWorldGameTest` in `src/gametest/resources/fabric.mod.json`.

- [ ] **Step 3: Run** `./gradlew compileGametestJava`. Expected: compile failure.

- [ ] **Step 4: `ArenaRun`**

```java
package nl.jeeninga.tafelboem.world;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * One level's arena while kids are in it: the arena, the fight once he's there, who is in it now
 * and who took part, and when everyone goes home after a win.
 */
public final class ArenaRun {
	private final FightLevel level;
	private final PlacedArea arena;
	private final int startAt;
	private final long startedAtMillis;
	private final Set<UUID> kids = new LinkedHashSet<>();
	private final Set<UUID> everyone = new HashSet<>();
	private @Nullable BossFight fight;
	private int returnAt = -1;

	ArenaRun(FightLevel level, PlacedArea arena, int startAt, long startedAtMillis) {
		this.level = level;
		this.arena = arena;
		this.startAt = startAt;
		this.startedAtMillis = startedAtMillis;
	}

	public FightLevel level() {
		return level;
	}

	public PlacedArea arena() {
		return arena;
	}

	public @Nullable BossFight fight() {
		return fight;
	}

	public Set<UUID> kids() {
		return Set.copyOf(kids);
	}

	public Set<UUID> everyone() {
		return Set.copyOf(everyone);
	}

	public boolean isReturning() {
		return returnAt >= 0;
	}

	int startAt() {
		return startAt;
	}

	long startedAtMillis() {
		return startedAtMillis;
	}

	int returnAt() {
		return returnAt;
	}

	void join(UUID kid) {
		kids.add(kid);
		everyone.add(kid);
	}

	void remove(UUID kid) {
		kids.remove(kid);
	}

	void start(@Nullable BossFight fight) {
		this.fight = fight;
	}

	void returnHomeAt(int tick) {
		this.returnAt = tick;
	}
}
```

- [ ] **Step 5: `TafelBoemWorld`**

```java
package nl.jeeninga.tafelboem.world;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.command.ArenaSetup;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.FightTally;
import nl.jeeninga.tafelboem.core.Stars;
import nl.jeeninga.tafelboem.game.QuizClient;
import nl.jeeninga.tafelboem.log.FightLog;

/**
 * Everything that happens in a TafelBoem world: the hub, the doors, a fight per arena, the stars,
 * the void and the way home.
 */
public final class TafelBoemWorld {
	static final int START_DELAY_TICKS = 60;
	static final int RETURN_DELAY_TICKS = 100;
	static final int DOOR_COOLDOWN_TICKS = 40;
	static final double STARS_DISTANCE = 3;

	private final ServerLevel level;
	private final WorldLayout layout;
	private final BossFights fights;
	private final FightLog fightLog;
	private final ArenaCatalog catalog;
	private final QuizClient client;
	private final Map<FightLevel, ArenaRun> runs = new EnumMap<>(FightLevel.class);
	private final Map<UUID, Integer> doorCooldown = new HashMap<>();
	private final Map<String, int[]> bestStars = new HashMap<>();
	private PlacedArea hub;
	private int ticks;

	public TafelBoemWorld(ServerLevel level, WorldLayout layout, BossFights fights, FightLog fightLog, ArenaCatalog catalog, QuizClient client) {
		this.level = level;
		this.layout = layout;
		this.fights = fights;
		this.fightLog = fightLog;
		this.catalog = catalog;
		this.client = client;
	}

	/** On every world load: the world rules, a fresh hub, and the spawn in the middle of its spot. */
	public void setUp() {
		ArenaSetup.applyRules(level);
		placeHub();
		level.setRespawnData(LevelData.RespawnData.of(level.dimension(), hub.standAt(), hub.yaw(), 0F));
		level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
	}

	public PlacedArea placeHub() {
		hub = Hub.place(level, layout);
		return hub;
	}

	public PlacedArea hub() {
		return hub;
	}

	public Optional<ArenaRun> run(FightLevel fightLevel) {
		return Optional.ofNullable(runs.get(fightLevel));
	}

	/** A kid walked into a door: 1–3 lead to that level's arena, 0 back to the hub. */
	public void enterDoor(ServerPlayer player, int door) {
		if (doorCooldown.getOrDefault(player.getUUID(), Integer.MIN_VALUE) > ticks) {
			return;
		}
		doorCooldown.put(player.getUUID(), ticks + DOOR_COOLDOWN_TICKS);
		if (door == 0) {
			runOf(player).ifPresent(run -> leave(player, run, FightRecord.LEFT));
			teleport(player, hub.standAt(), hub.yaw());
			return;
		}

		FightLevel fightLevel = FightLevel.byNumber(door);
		ArenaRun run = runs.get(fightLevel);
		if (run != null && run.isReturning()) {
			player.sendSystemMessage(Component.translatable("tafelboem.door.busy"));
			return;
		}
		if (run == null) {
			PlacedArea arena = catalog.place(level, layout, fightLevel,
					name -> player.sendSystemMessage(Component.translatable("tafelboem.arena.skipped", name).withStyle(ChatFormatting.GOLD)));
			run = new ArenaRun(fightLevel, arena, ticks + START_DELAY_TICKS, System.currentTimeMillis());
			runs.put(fightLevel, run);
		}
		run.join(player.getUUID());
		teleport(player, run.arena().standAt(), run.arena().yaw());
	}

	public void tick() {
		ticks++;
		catchFallingKids();
		for (ArenaRun run : List.copyOf(runs.values())) {
			tick(run);
		}
		if (ticks % 10 == 0) {
			showStarsAtDoors();
		}
	}

	private void tick(ArenaRun run) {
		if (run.fight() == null && ticks >= run.startAt() && !run.kids().isEmpty()) {
			Vec3 spot = run.arena().middle().add(0, 1.5, 0);
			run.start(fights.summon(level, spot, run.level(), run.arena().bounds()));
		}

		// A kid who is somewhere else now (a command, another dimension) has left the fight.
		for (UUID id : run.kids()) {
			ServerPlayer kid = level.getServer().getPlayerList().getPlayer(id);
			if (kid != null && (kid.level() != level || !run.arena().bounds().containsColumn(kid.getX(), kid.getZ()))) {
				leave(kid, run, FightRecord.LEFT);
			}
		}

		BossFight fight = run.fight();
		if (fight != null && fight.isOver() && !run.isReturning()) {
			if (!fight.isWon()) {
				runs.remove(run.level());
				return;
			}
			award(run, fight);
			run.returnHomeAt(ticks + RETURN_DELAY_TICKS);
		}
		if (run.isReturning() && ticks >= run.returnAt()) {
			for (ServerPlayer kid : kidsIn(run)) {
				teleport(kid, hub.standAt(), hub.yaw());
			}
			runs.remove(run.level());
			return;
		}
		if (run.kids().isEmpty() && !run.isReturning()) {
			if (fight != null && !fight.isOver()) {
				fights.end(fight);
			}
			runs.remove(run.level());
		}
	}

	/** Stars and a "won" line for every kid still in the arena; a new best gets a message. */
	private void award(ArenaRun run, BossFight fight) {
		for (ServerPlayer kid : kidsIn(run)) {
			int stars = fight.tally(kid.getUUID()).stars(true);
			int before = bestStars(kid, run.level());
			write(kid, run, FightRecord.WON, stars);
			if (stars > before) {
				bestStars.get(kid.getPlainTextName())[run.level().number()] = stars;
				if (TafelBoem.config().stars) {
					kid.sendSystemMessage(Component.translatable("tafelboem.stars.new_best", "★".repeat(stars),
							Component.translatable("tafelboem.level." + run.level().number())).withStyle(ChatFormatting.GOLD));
				}
			}
		}
	}

	private void leave(ServerPlayer kid, ArenaRun run, String result) {
		write(kid, run, result, 0);
		run.remove(kid.getUUID());
	}

	public void onDisconnect(ServerPlayer player) {
		runOf(player).ifPresent(run -> leave(player, run, FightRecord.DISCONNECTED));
	}

	/** A kid who logs in inside an arena without a running fight goes to the hub. */
	public void onJoin(ServerPlayer player) {
		if (player.level() == level && runOf(player).isEmpty() && layout.arenaAt(player.getX(), player.getZ()).isPresent()) {
			teleport(player, hub.standAt(), hub.yaw());
		}
	}

	private void write(ServerPlayer kid, ArenaRun run, String result, int stars) {
		FightTally tally = run.fight() == null ? new FightTally() : run.fight().tally(kid.getUUID());
		long seconds = (System.currentTimeMillis() - run.startedAtMillis()) / 1000;
		fightLog.append(new FightRecord(System.currentTimeMillis(), kid.getPlainTextName(), run.level().number(), run.arena().name(), result,
				tally.answers(), tally.wrong(), tally.dontKnow(), tally.chickenOut(), stars, seconds, run.everyone().size(), TafelBoem.version()));
	}

	/** Falling is never fatal: back to the arena's arrival spot in a fight, otherwise to the hub. */
	private void catchFallingKids() {
		for (ServerPlayer player : level.players()) {
			if (player.getY() >= layout.voidY() || !isOurs(player)) {
				continue;
			}
			PlacedArea to = runOf(player).map(ArenaRun::arena).orElse(hub);
			teleport(player, to.standAt(), to.yaw());
		}
	}

	/** Above the hub or an arena slot: other worlds' players (and other GameTests') are none of our business. */
	private boolean isOurs(ServerPlayer player) {
		return layout.hubSlot().containsColumn(player.getX(), player.getZ()) || layout.arenaAt(player.getX(), player.getZ()).isPresent();
	}

	private void showStarsAtDoors() {
		for (ServerPlayer player : level.players()) {
			if (!hub.bounds().containsColumn(player.getX(), player.getZ())) {
				continue;
			}
			for (FightLevel fightLevel : FightLevel.DOORS) {
				boolean near = hub.doors().getOrDefault(fightLevel.number(), List.of()).stream()
						.anyMatch(door -> Vec3.atCenterOf(door).distanceToSqr(player.position()) <= STARS_DISTANCE * STARS_DISTANCE);
				if (near) {
					Component name = Component.translatable("tafelboem.level." + fightLevel.number());
					client.actionBar(player, TafelBoem.config().stars
							? Component.translatable("tafelboem.door.stars", name, Stars.show(bestStars(player, fightLevel)))
							: name);
					break;
				}
			}
		}
	}

	private int bestStars(ServerPlayer kid, FightLevel fightLevel) {
		int[] stars = bestStars.computeIfAbsent(kid.getPlainTextName(), name -> {
			List<FightRecord> fights = fightLog.read(name);
			return new int[] {0, Stars.best(fights, 1), Stars.best(fights, 2), Stars.best(fights, 3)};
		});
		return stars[fightLevel.number()];
	}

	private List<ServerPlayer> kidsIn(ArenaRun run) {
		return run.kids().stream()
				.map(id -> level.getServer().getPlayerList().getPlayer(id))
				.filter(kid -> kid != null && kid.level() == level && run.arena().bounds().containsColumn(kid.getX(), kid.getZ()))
				.toList();
	}

	private Optional<ArenaRun> runOf(ServerPlayer player) {
		return runs.values().stream().filter(run -> run.kids().contains(player.getUUID())).findFirst();
	}

	private void teleport(ServerPlayer player, BlockPos to, float yaw) {
		player.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.of(), yaw, 0F, true);
		player.resetFallDistance();
	}
}
```

  Note for the implementer: `fights.summon(...)` can return null only when the boss entity can't be
  created; then `run.fight()` stays null and the next tick tries again.

- [ ] **Step 6: Wire it into the mod.** In `TafelBoem`:
  - `private static @Nullable TafelBoemWorld world;`
  - In `SERVER_STARTED`, after creating the fights and the fights log:
    ```java
    			ServerLevel overworld = server.overworld();
    			if (TafelBoemWorlds.isTafelBoemWorld(overworld)) {
    				world = new TafelBoemWorld(overworld, WorldLayout.DEFAULT, fights, fightLog,
    						new ArenaCatalog(server.getStructureManager(), new Random(), ArenaCatalog.DEFAULT_FOLDER), QuizClient.NETWORK);
    				world.setUp();
    				LevelDoorBlock.use(world::enterDoor);
    			}
    ```
  - In `END_SERVER_TICK`, after the fights: `if (world != null) { world.tick(); }`.
  - `ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { if (world != null) { world.onJoin(handler.player); } });`
  - In `DISCONNECT`, first `if (world != null) { world.onDisconnect(handler.player); }`.
  - In `SERVER_STOPPED`: `world = null; LevelDoorBlock.use(LevelDoorBlock.NOBODY); ProtectedAreas.clear();`.
  - `public static Optional<TafelBoemWorld> world() { return Optional.ofNullable(world); }`

- [ ] **Step 7: Lang keys**

  | Key | en_us | nl_nl |
  |---|---|---|
  | `tafelboem.stars.new_best` | `%s on %s!` | `%s op %s!` |
  | `tafelboem.door.stars` | `%s: %s` | `%s: %s` |
  | `tafelboem.door.busy` | `Just a moment, the others are on their way back!` | `Even wachten, de anderen komen net terug!` |
  | `tafelboem.arena.skipped` | `Arena %s has no exit door, so it was skipped.` | `Arena %s heeft geen uitgang en is overgeslagen.` |

- [ ] **Step 8: Run** `./gradlew build` and `./gradlew runGameTest`. Expected: PASS. The won
  fight takes the longest (about 12 hits).

- [ ] **Step 9: Commit**

```bash
git add -A src
git commit -m "Play the TafelBoem world: doors to fights, stars, and back to the hub

Any number of kids join a running fight through the same door. A win
writes the stars and sends everyone home after 5 s; the last kid leaving
ends the fight. The void never kills, and each kid sees their own stars
at the doors.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: A real client, screenshots, and the docs

**Files:**
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/WorldClientGameTest.java` (+ register
  it under `fabric-client-gametest`, next to `PlaythroughClientGameTest`)
- Modify: `README.md` (the TafelBoem world, building an arena)
- Create: `docs/evaluations/R1.md`
- Create: `docs/screenshots/r1-*.png` (copies of the new screenshots)

**Interfaces:**
- Consumes: `TafelBoem.world()`, `TafelBoemWorlds`, `TafelBoemWorld.hub()`, `run(...)` (Task 10).

- [ ] **Step 1: Write the client GameTest.** It creates a TafelBoem world from the World Type
  button and walks (not teleports) through the doors, so the block's `entityInside` is tested for
  real:

```java
package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.client.QuestionScreen;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.world.TafelBoemWorld;
import nl.jeeninga.tafelboem.world.TafelBoemWorlds;

/**
 * R1 in a real client: a TafelBoem world from the World Type button, the hub, a walk through each
 * door into its arena, a fight on level 2, and the exit door home. Screenshots land in
 * {@code build/run/clientGameTest/screenshots}.
 */
@SuppressWarnings("UnstableApiUsage")
public class WorldClientGameTest implements FabricClientGameTest {
	private static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, TafelBoem.id("tafelboem"));

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(WorldClientGameTest::chooseTafelBoem).create()) {
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			check(world.getServer().computeOnServer(server -> TafelBoemWorlds.isTafelBoemWorld(server.overworld())), "a TafelBoem world");
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return tafelBoem().hub().bounds().holds(player.getX(), player.getY(), player.getZ());
			}), "the kid spawns in the hub");
			context.getInput().lookAt(door(world, 2).above(2));
			context.waitTicks(5);
			context.takeScreenshot("r1-01-hub");

			for (FightLevel level : FightLevel.DOORS) {
				walkInto(context, world, door(world, level.number()));
				waitUntil(context, () -> world.getServer().computeOnServer(server -> tafelBoem().run(level).isPresent()), "into arena " + level);
				world.getConnection().waitForChunksRender();
				// He appears after 3 s and throws 3 s later: leave arenas 1 and 3 before his first bomb roots the kid.
				context.waitTicks(70);
				context.takeScreenshot("r1-0" + (level.number() + 1) + "-arena-" + level.number());

				if (level == FightLevel.GEMIDDELD) {
					context.waitForScreen(QuestionScreen.class);
					QuestionScreen screen = (QuestionScreen) context.computeOnClient(client -> client.gui.screen());
					context.getInput().typeChars(Integer.toString(screen.a() * screen.b()));
					context.waitTicks(5);
					context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
					context.waitFor(client -> client.gui.screen() == null);
					context.waitTicks(14);
					context.takeScreenshot("r1-05-yeet-in-arena");
				}

				BlockPos exit = world.getServer().computeOnServer(server -> tafelBoem().run(level).orElseThrow().arena().doors().get(0).getFirst());
				walkInto(context, world, exit);
				waitUntil(context, () -> world.getServer().computeOnServer(server -> tafelBoem().run(level).isEmpty()), "out of arena " + level);
			}
		}
	}

	/** Stands the kid two blocks in front of the door, facing it, and walks forward. */
	private static void walkInto(ClientGameTestContext context, TestSingleplayerContext world, BlockPos door) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos front = door.relative(server.overworld().getBlockState(door).getValue(nl.jeeninga.tafelboem.block.LevelDoorBlock.FACING), 2);
			player.teleportTo(front.getX() + 0.5, door.getY(), front.getZ() + 0.5);
		});
		context.waitTicks(5);
		context.getInput().lookAt(door);
		context.getInput().holdKeyFor(options -> options.keyUp, 30);
	}

	private static BlockPos door(TestSingleplayerContext world, int level) {
		return world.getServer().computeOnServer(server -> {
			List<BlockPos> blocks = tafelBoem().hub().doors().get(level);
			return blocks.stream().min((a, b) -> Integer.compare(a.getY(), b.getY())).orElseThrow();
		});
	}

	private static TafelBoemWorld tafelBoem() {
		return TafelBoem.world().orElseThrow(() -> new AssertionError("no TafelBoem world running"));
	}

	/** Polls from the test thread, as the playthrough test does: asking the server from a client wait can deadlock. */
	private static void waitUntil(ClientGameTestContext context, java.util.function.BooleanSupplier condition, String what) {
		for (int tick = 0; !condition.getAsBoolean(); tick++) {
			check(tick < 200, "timed out waiting: " + what);
			context.waitTicks(1);
		}
	}

	private static void chooseTafelBoem(WorldCreationUiState state) {
		WorldCreationUiState.WorldTypeEntry tafelboem = state.getNormalPresetList().stream()
				.filter(type -> type.preset().is(PRESET))
				.findFirst()
				.orElseThrow(() -> new AssertionError("TafelBoem is not on the World Type button"));
		state.setWorldType(tafelboem);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
```

  Note: the hub's door blocks face south, so `walkInto` stands the kid 2 blocks south of the
  door's lowest block; the arena exit faces into the arena. If walking forward doesn't trigger the
  door, that's a real bug in Task 8 (`noCollision` plus `entityInside`), not a test problem.

- [ ] **Step 2: Run** `./gradlew runClientGameTest`. Expected: PASS for both client tests. Look at
  `r1-01-hub`, `r1-02-arena-1`, `r1-03-arena-2`, `r1-04-arena-3` and `r1-05-yeet-in-arena` with
  the Read tool: doors with signs and bombs, the three arenas look different, open sky, obsidian
  walls. Copy them to `docs/screenshots/`.

- [ ] **Step 3: README.** Add a section "The TafelBoem world" after the roadmap: how to create one
  (World Type: TafelBoem), the hub and its doors, stars, the exit door, that falling is safe, and
  that other worlds still work with the summon item and `/tafelboem arena`. Add "Build your own
  arena" with the steps and rules from the spec's *Hand-built hub and arenas* (structure block
  Corner/Save/Detect, the names `tafelboem:level_<n>/<name>` and `tafelboem:hub`, at most 48 ×
  48 × 48, open sky, exactly one exit door where the kids come in, bedrock or obsidian for what must
  never break, an uneven floor is fine; to ship one, copy the `.nbt` into
  `src/main/resources/data/tafelboem/structure/`). Update the R1 roadmap row's status to
  "🚧 built, not published".

- [ ] **Step 4: `docs/evaluations/R1.md`.** The R1 evaluation, in the shape of `R0.1.md`:
  hypothesis; date, version, who, PCs, world; section 1 the spec's 7 acceptance criteria as a
  checklist with "Woonkamer PC | Second PC | Notes" columns; section 2 the numbers from the logs per
  kid (levels picked in order, fights won/left, stars per level, answers per minute, % correct,
  re-ask accuracy, "?" and ESC); section 3 Jesper's typed notes (laughter, confusion, frustration
  with what about, saw the penalty, saw the yeet, the reward arrived) and the three questions;
  section 4 findings (P18 onwards); section 5 the go criteria from the spec; section 6 backlog
  changes. Add a line under the title: "No video: the numbers come from `latest.log` and the logs,
  the rest is typed by Jesper."

- [ ] **Step 5: Commit**

```bash
git add -A src README.md docs
git commit -m "Test the TafelBoem world in a real client, and document it

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Release 1.0.0

- [ ] **Step 1: Full check.** Run `./gradlew build runClientGameTest`. Expected: all green. Review
  the diff of the whole release (`git log --oneline main@{u}..main`) once more for leftover debug
  code.
- [ ] **Step 2: Delete the spike branch:** `git branch -D spike/r1-world` (never pushed).
- [ ] **Step 3: Ask Jesper** whether to publish. With his go: `git tag v1.0.0`,
  `./gradlew publishPack`, wait for "Live", then `git push origin main`. Without it, stop here.
- [ ] **Step 4: Manual checklist (Jesper)** on the woonkamer PC and a second PC over LAN, recorded
  in `docs/evaluations/R1.md` section 1.
