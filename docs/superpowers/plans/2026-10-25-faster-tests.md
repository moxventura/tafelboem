# Faster Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `./gradlew build`, `runGameTest` and a release check quicker without any test
losing what it proves. A quick group and a fights group run on two servers side by side. Every
GameTest that counts chickens counts only its own.

**Architecture:** Everything lives in the test mod (`src/gametest`) and the build, never in the mod
itself. A shared `OwnChickens` helper isolates the chicken counts. The test mod gets its first
mixins, which call into one class, `GameTestHarness`:
- the GameTest server never saves its world, and Gradle deletes that world before each run;
- all of a server's tests run in one batch;
- a run keeps only the chosen group (`@Fight` or not), with Minecraft's own pattern and repeat;
- the runner skips clearing blocks that already are air or stone.

Gradle gets two Loom run configs, one per group, and `runGameTest` runs both.

**Tech Stack:** Java 25, Fabric Loom 1.18.3, Fabric API 0.161.0+26.2 (fabric-gametest-api-v1
4.0.22), Minecraft 26.2 (unobfuscated: no refmaps, mixin targets by their real names), Sponge Mixin
0.17 with MixinExtras, Gradle 9.7 with configuration cache and parallel on, GitHub Actions
(`gradle/actions` v6).

**Spec:** `docs/superpowers/specs/2026-10-07-tafelboem-design.md`, section *Faster tests (detailed
scope)*. It was corrected on 2026-10-25 (commit 18a94dd). Tests stand their padded box plus 5
blocks apart, not 6 blocks, so "less clearing" now means skipping blocks that already are air or
stone, not lowering the padding.

## Global Constraints

- No game code changes: nothing under `src/main` or `src/client` changes. Hooks live in the test mod
  (`src/gametest`), never in the mod itself.
- All 69 TafelBoem GameTests stay, with what they assert. The only change to them is the isolation
  fixes. One GameTest is added, for the isolation helper itself.
- Every GameTest still runs before every publish: `publishPack` runs `build` and the fights group.
- A test is a fight exactly when it carries `@Fight`, so every test is in one group. The run fails
  if `@Fight` sits on something that is no GameTest, or if a pattern matches no test in the group.
- Each step is timed three times with `tools/time-gradle.sh` (the median counts) and written into
  `docs/evaluations/faster-tests.md`. A step that saves no time is taken out again.
- Targets ("Done when"):
  - `./gradlew build`: 57 s → 30 s or less;
  - `./gradlew runGameTest`: 56 s → 35 s or less;
  - one slow test three times: about 3 min → 45 s or less;
  - a release check: about 6 min → 3½ min or less;
  - `./gradlew runClientGameTest`: 2 min 26 s → 2 min or less, or written down as not worth it;
  - five full runs in a row pass.
- Code style (AGENTS.md):
  - tabs;
  - Javadoc on every class;
  - comments only where the why isn't obvious;
  - import groups in this order: `java.*`, `net.minecraft.*`, `org.spongepowered.*`,
    `net.fabricmc.*`, `org.slf4j.*`, `nl.jeeninga.*`.
- Git:
  - work on branch `faster-tests` in `.worktrees/faster-tests`;
  - the subject is an imperative sentence saying what changes for whoever runs the tests; the body
    says why;
  - end with `Co-Authored-By:` naming the model;
  - stage only the paths you changed, never `git add -A`;
  - never push, tag or publish.
- A changed fight test runs three times before it is trusted (AGENTS.md).
- The test JVM writes a `logs/` folder at the repo root; it is ignored, don't commit it.

## File Structure

| File | What it is |
|---|---|
| `tools/time-gradle.sh` (new) | times a Gradle command N times, prints each run and the median |
| `docs/evaluations/faster-tests.md` (new) | the baseline, each step's medians, the "Done when" table |
| `.github/workflows/build.yml` | Gradle cache; later `build runFightGameTests` |
| `src/gametest/java/.../gametest/OwnChickens.java` (new) | which spectacle chickens are this test's |
| `ChickenGameTest`, `BossFightGameTest`, `SessionGameTest` | count with `OwnChickens` |
| `src/gametest/java/.../gametest/GameTestHarness.java` (new) | what the test mod changes in Minecraft's GameTest runner |
| `src/gametest/java/.../gametest/Fight.java` (new) | the mark for the fights group |
| `src/gametest/java/.../gametest/mixin/GameTestServerMixin.java` (new) | never save, one batch, group, repeat |
| `src/gametest/java/.../gametest/mixin/MinecraftServerMixin.java` (new) | stop without saving |
| `src/gametest/java/.../gametest/mixin/StructureUtilsMixin.java` (new) | skip clearing what is clear |
| `src/gametest/resources/tafelboem-test.mixins.json` (new) | the test mod's mixin config |
| `src/gametest/resources/fabric.mod.json` | names the mixin config |
| `build.gradle` | report file, world deletion, the two group runs, `runGameTest`, `check`, `publishPack` |
| `AGENTS.md`, `README.md` | the working rules and commands |

`...` is `nl/jeeninga/tafelboem`.

---

### Task 1: Measure the baseline

**Files:**
- Modify: `build.gradle` (after the `fabricApi { … }` block)
- Create: `tools/time-gradle.sh`
- Create: `docs/evaluations/faster-tests.md`

**Interfaces:**
- Produces:
  - `tools/time-gradle.sh <gradle args…>` (env `RUNS`, default 3) prints `run i: S s` lines and
    `median of N: S s`;
  - `build/reports/gametest-all.xml` after every `runGameTest`.

- [ ] **Step 1: Write the report file into every GameTest run**

In `build.gradle`, directly after the closing `}` of the `fabricApi { … }` block, add:

```groovy
// Each GameTest's time, as JUnit XML, for docs/evaluations/faster-tests.md.
loom.runs.named("gameTest") {
	property("fabric-api.gametest.report-file", file("build/reports/gametest-all.xml").absolutePath)
}
```

- [ ] **Step 2: Run it and check the report**

Run: `./gradlew runGameTest`
Expected: `All 70 required tests passed`, and `build/reports/gametest-all.xml` exists. This command
prints the slowest tests:

```bash
grep -o '<testcase[^>]*>' build/reports/gametest-all.xml | sed -E 's/.*name="([^"]*)".*time="([^"]*)".*/\2 \1/' | sort -rn | head -5
```

Expected: `boss_fight_game_test_beating_graaf_fout_gives_the_kid_four_kippenbommen` near the top,
at 7–10 s.

- [ ] **Step 3: Write the timing script**

Create `tools/time-gradle.sh`:

```bash
#!/usr/bin/env bash
# Times a Gradle command a few times (3 unless RUNS says otherwise) and prints each run and the
# median, for docs/evaluations/faster-tests.md. Warm the daemon first: run the command once before
# timing it. The output of the last run is in build/time-gradle.log.
#
#   tools/time-gradle.sh build
#   RUNS=5 tools/time-gradle.sh runFightGameTests -Ptests='*beating*' -Prepeat=3
set -euo pipefail
cd "$(dirname "$0")/.."
runs=${RUNS:-3}
mkdir -p build
times=()
for i in $(seq "$runs"); do
	start=$(date +%s.%N)
	if ! ./gradlew "$@" > build/time-gradle.log 2>&1; then
		echo "run $i failed; see build/time-gradle.log" >&2
		exit 1
	fi
	end=$(date +%s.%N)
	seconds=$(awk -v s="$start" -v e="$end" 'BEGIN { printf "%.1f", e - s }')
	echo "run $i: $seconds s"
	times+=("$seconds")
done
printf '%s\n' "${times[@]}" | sort -n | awk '{ t[NR] = $1 } END { m = (NR % 2) ? t[(NR + 1) / 2] : (t[NR / 2] + t[NR / 2 + 1]) / 2; printf "median of %d: %.1f s\n", NR, m }'
```

Run: `chmod +x tools/time-gradle.sh && RUNS=1 tools/time-gradle.sh test`
Expected: `run 1: … s` and `median of 1: … s`.

- [ ] **Step 4: Time the baseline**

Run each once to warm the daemon, then time it:

```bash
./gradlew build && tools/time-gradle.sh build
./gradlew runGameTest && tools/time-gradle.sh runGameTest
./gradlew runClientGameTest && tools/time-gradle.sh runClientGameTest
```

`runClientGameTest` needs a display; WSLg works. Three runs take about 7½ min. Write down every
run and every median.

- [ ] **Step 5: Write the evaluation file with the baseline**

Create `docs/evaluations/faster-tests.md`, with the measured numbers in place of `…`:

```markdown
# Faster tests: measurements

What each step of the spec's *Faster tests (detailed scope)* did to the time the tests take.

**How:** Jesper's PC (WSL2, 20 cores, 15 GB). The command runs once to warm the Gradle daemon, then
`tools/time-gradle.sh <task>` times three runs and the median counts. A step that saves no time is
taken out again. Each GameTest's own time is in `build/reports/gametest-*.xml`; the slowest:

    grep -o '<testcase[^>]*>' build/reports/gametest-all.xml | sed -E 's/.*name="([^"]*)".*time="([^"]*)".*/\2 \1/' | sort -rn | head

## Baseline: the 1.1.0 tests

| Run | Runs (s) | Median (s) |
|---|---|---|
| `./gradlew build` | … | … |
| `./gradlew runGameTest` | … | … |
| `./gradlew runClientGameTest` | … | … |
| one slow test three times: 3 × `runGameTest` | | … |
| a release check: 2 × `build`, 2 × `runGameTest`, `runClientGameTest` | | … |

## Steps

| Step | Run | Before (s) | After (s) | Kept |
|---|---|---|---|---|
```

The last two baseline rows are computed from the medians above them: 3 × `runGameTest`, and
2 × `build` + 2 × `runGameTest` + `runClientGameTest`.

- [ ] **Step 6: Commit**

```bash
git add build.gradle tools/time-gradle.sh docs/evaluations/faster-tests.md
git commit -m "Time the tests before making them faster

Each GameTest run now writes every test's time to build/reports, and
tools/time-gradle.sh times a Gradle command three times, so every step of
the faster-tests work is measured against the same baseline.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 2: Cache Gradle in CI

**Files:**
- Modify: `.github/workflows/build.yml`

**Interfaces:** none.

- [ ] **Step 1: Add the setup-gradle step**

In `.github/workflows/build.yml`, between the `setup jdk` step and the `make gradle wrapper
executable` step, add a step at the same indentation as its neighbours:

```yaml
      - name: setup gradle
        # Caches Gradle and the dependencies: on 2026-10-24 a push got "429 Too Many Requests" from
        # Maven Central before it compiled anything.
        uses: gradle/actions/setup-gradle@v6
```

Keep the `validate gradle wrapper` step.

- [ ] **Step 2: Check the YAML**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/build.yml')); print('ok')"`
Expected: `ok`. If PyYAML is missing, compare the indentation with the `setup jdk` step by eye.
GitHub runs the workflow on the next push, which needs Jesper's go. Say so in the report; don't
push.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "Cache Gradle in CI so a busy Maven Central can't fail the build

Every CI run downloaded Gradle and all dependencies; on 2026-10-24 the main
push, 21 s after the v1.1.0 tag's push of the same commit, got 429 Too Many
Requests before it compiled anything.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 3: Count only a test's own chickens

**Files:**
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/OwnChickens.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/ChickenGameTest.java` (whole file below)
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/BossFightGameTest.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/SessionGameTest.java`

**Interfaces:**
- Produces: `OwnChickens(GameTestHelper)`, with:
  - `followPops()`
  - `watch(Supplier<Vec3>)`
  - `tick()`
  - `claimNew(Vec3, double) → List<UUID>`
  - `claimDuring(Runnable, Vec3, double) → List<UUID>`
  - `alive() → int`
  - `SPAWN_RADIUS = 2.5`

**Why it works this way:**
- GameTests share one level, and a fight reaches into the next test's field. A count of every
  spectacle chicken near a spot can count a neighbour's, and in R1.1 a chase test did.
- Every chicken a test causes appears in one of three places:
  - right where it names a spot: where `countChickens` is called, the bomb, Graaf Fout when he is
    hit, the kid on a wrong answer, behind him when the chase begins;
  - where one of its own counting chickens just popped into its group.
- A GameTest's `onEachTick` actions run in **no fixed order**: Minecraft keeps them in a hash map
  (`GameTestInfo.runAtTickTimeMap`). So `tick()` goes in the same `onEachTick` as the
  `sessions.tick()` and the checks that use it.

- [ ] **Step 1: Write the failing test for the helper**

Add this test to `ChickenGameTest` (it doesn't compile yet: `OwnChickens` doesn't exist):

```java
	/**
	 * Another test's chickens next to this test's spot are not this test's: they were there first, and
	 * the ones they pop into appear where none of this test's chickens went.
	 */
	@GameTest(maxTicks = 120, padding = 48)
	public void aNeighboursChickensNextToTheSpotAreNotCounted(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager mine = sessions(helper);
		SessionManager neighbour = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		neighbour.countChickens(helper.getLevel(), at.add(16, 0, 0), new Fact(2, 5), "test");
		OwnChickens chickens = new OwnChickens(helper).followPops();
		helper.onEachTick(() -> {
			mine.tick();
			neighbour.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= 12, "never the neighbour's: " + chickens.alive());
		});
		// The neighbour's chickens are out and popping before this test's appear.
		helper.runAfterDelay(20, () -> chickens.claimDuring(() -> mine.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS));
		helper.runAfterDelay(20 + 45, () -> {
			helper.assertTrue(chickens.alive() == 12, "3 groups of 4, and none of the neighbour's 10: " + chickens.alive());
			helper.succeed();
		});
	}
```

Run: `./gradlew compileGametestJava`
Expected: FAIL with `cannot find symbol … OwnChickens`.

A count of every spectacle chicken within 24 blocks of `at` (the old way) would see 12 + 10 here.

- [ ] **Step 2: Write `OwnChickens`**

Create `src/gametest/java/nl/jeeninga/tafelboem/gametest/OwnChickens.java`:

```java
package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.game.BombEffects;

/**
 * A test's own spectacle chickens, told apart by id. GameTests share one level and run side by side,
 * and a fight reaches into the next test's field, so counting the chickens near a spot can count a
 * neighbour's. A chicken is this test's when it appears where this test's chickens come from: next
 * to a spot the test names, on the tick it appears, or (with {@link #followPops}) where one of the
 * test's own chickens just popped into its group.
 *
 * <p>Call {@link #tick} once every tick, in the same {@code onEachTick} as the checks that use it:
 * a test's {@code onEachTick} actions run in no fixed order.
 */
final class OwnChickens {
	/** A new chicken is at most this far from where it came from on the first tick it is seen. */
	static final double SPAWN_RADIUS = 2.5;
	/** A chicken pops into its group on the tick it goes, or a tick later when something else took it. */
	private static final int POP_TICKS = 3;
	/** Graaf Fout flies up to about 45 blocks, and chickens follow him. */
	private static final double REACH = 64;

	private final GameTestHelper helper;
	private final AABB area;
	private final List<Supplier<Vec3>> spots = new ArrayList<>();
	private final List<Vec3> spotsBefore = new ArrayList<>();
	private boolean followPops;
	/** Every spectacle chicken in the area at the last look, with where it stood. */
	private Map<UUID, Vec3> seen;
	/** The chickens that appeared since the tick before. */
	private Map<UUID, Vec3> fresh = Map.of();
	/** This test's chickens, with where they stand, or stood last. */
	private final Map<UUID, Vec3> own = new LinkedHashMap<>();
	/** This test's chickens that went in the last few ticks, with the ticks since. */
	private final Map<UUID, Integer> gone = new HashMap<>();

	OwnChickens(GameTestHelper helper) {
		this.helper = helper;
		this.area = helper.getBounds().inflate(REACH);
		this.seen = look();
	}

	/** From now on the chickens that appear where one of this test's chickens just went are this test's too. */
	OwnChickens followPops() {
		followPops = true;
		return this;
	}

	/** From now on, every tick, the chickens that appear next to this spot are this test's: where it was a tick ago, or is now. */
	void watch(Supplier<Vec3> spot) {
		spots.add(spot);
		spotsBefore.add(spot.get());
	}

	/** Takes stock, once a tick: which chickens are new, which of this test's went, and which new ones are this test's. */
	void tick() {
		Map<UUID, Vec3> now = look();
		fresh = new LinkedHashMap<>(now);
		fresh.keySet().removeAll(seen.keySet());

		gone.replaceAll((id, ticks) -> ticks + 1);
		gone.values().removeIf(ticks -> ticks > POP_TICKS);
		for (Map.Entry<UUID, Vec3> chicken : own.entrySet()) {
			Vec3 at = now.get(chicken.getKey());
			if (at != null) {
				chicken.setValue(at);
			} else if (seen.containsKey(chicken.getKey())) {
				gone.put(chicken.getKey(), 0);
			}
		}
		if (followPops) {
			claim(at -> gone.keySet().stream().anyMatch(went -> own.get(went).distanceTo(at) <= SPAWN_RADIUS));
		}
		for (int i = 0; i < spots.size(); i++) {
			Vec3 before = spotsBefore.get(i);
			Vec3 spot = spots.get(i).get();
			claim(at -> at.distanceTo(spot) <= SPAWN_RADIUS || at.distanceTo(before) <= SPAWN_RADIUS);
			spotsBefore.set(i, spot);
		}
		seen = now;
	}

	/** Claims the chickens that appeared since the tick before within {@code radius} of {@code at}. */
	List<UUID> claimNew(Vec3 at, double radius) {
		return claim(pos -> pos.distanceTo(at) <= radius);
	}

	/** Runs {@code spawn} and claims the chickens it lets out within {@code radius} of {@code at}. */
	List<UUID> claimDuring(Runnable spawn, Vec3 at, double radius) {
		Set<UUID> before = look().keySet();
		spawn.run();
		List<UUID> claimed = new ArrayList<>();
		look().forEach((id, pos) -> {
			if (!before.contains(id) && pos.distanceTo(at) <= radius && !own.containsKey(id)) {
				own.put(id, pos);
				claimed.add(id);
			}
		});
		// Seen now, so the next tick knows when one of them goes.
		claimed.forEach(id -> seen.put(id, own.get(id)));
		return claimed;
	}

	/** How many of this test's chickens are alive. */
	int alive() {
		return (int) own.keySet().stream()
				.filter(id -> helper.getLevel().getEntity(id) instanceof Chicken chicken && chicken.isAlive())
				.count();
	}

	private List<UUID> claim(Predicate<Vec3> mine) {
		List<UUID> claimed = new ArrayList<>();
		fresh.forEach((id, at) -> {
			if (!own.containsKey(id) && mine.test(at)) {
				own.put(id, at);
				claimed.add(id);
			}
		});
		return claimed;
	}

	private Map<UUID, Vec3> look() {
		Map<UUID, Vec3> found = new LinkedHashMap<>();
		for (Chicken chicken : helper.getLevel().getEntitiesOfClass(Chicken.class, area,
				chicken -> chicken.entityTags().contains(BombEffects.SPECTACLE_TAG))) {
			found.put(chicken.getUUID(), chicken.position());
		}
		return found;
	}
}
```

- [ ] **Step 3: Rewrite `ChickenGameTest` to count with it**

Replace `src/gametest/java/nl/jeeninga/tafelboem/gametest/ChickenGameTest.java` with the file below.
It contains the new test from Step 1. Every assertion is the old one, now on this test's own
chickens. `sessions.tick()` moves into the same `onEachTick` as the count, because the order matters.

```java
package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * The counting Kippenbom: a × b shows as a groups of b, and every chicken goes again, so 10 × 10 on
 * Moeilijk never piles up. Each test counts only its own chickens ({@link OwnChickens}).
 */
public class ChickenGameTest {
	/** The first generation pops where it lands, at the latest 40 ticks after the burst; every chicken then lives 10 s. */
	@GameTest(maxTicks = 320, padding = 48)
	public void threeTimesFourShowsThreeGroupsOfFourThatPoofAway(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		OwnChickens chickens = new OwnChickens(helper).followPops();
		List<UUID> firsts = chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS);

		helper.assertTrue(firsts.size() == 3, "first one chicken per group, not " + firsts.size());
		boolean[] shown = {false};
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			int now = chickens.alive();
			helper.assertTrue(now <= 12, "never more than the sum: " + now);
			shown[0] |= now == 12;
		});
		helper.runAfterDelay(20 * 12 + 40, () -> {
			helper.assertTrue(shown[0], "3 groups of 4 should have been there");
			helper.assertTrue(chickens.alive() == 0, "after 10 s they are gone, not " + chickens.alive());
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void noMoreThan150SpectacleAnimalsLiveAtOnce(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		Vec3 next = at.add(8, 0, 0);
		OwnChickens chickens = new OwnChickens(helper).followPops();
		chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), at, new Fact(10, 10), "test"), at, OwnChickens.SPAWN_RADIUS);
		chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), next, new Fact(10, 10), "test"), next, OwnChickens.SPAWN_RADIUS);
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= SessionManager.MAX_SPECTACLES, "the oldest poof first: " + chickens.alive() + " chickens");
		});

		// Every first chicken has popped by 40 ticks; the cap then keeps a crowd, not a handful.
		helper.runAfterDelay(50, () -> {
			int alive = chickens.alive();
			helper.assertTrue(alive >= 100, "after the cap 100 or more chickens are left: " + alive + " chickens");
			helper.succeed();
		});
	}

	/**
	 * A first chicken that a boom took pops where it was last, and one held in the air pops at the
	 * limit, so the groups still add up to the sum.
	 */
	@GameTest(maxTicks = 120, padding = 48)
	public void aChickenThatIsGoneOrStuckInTheAirStillPopsIntoItsGroup(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		OwnChickens chickens = new OwnChickens(helper).followPops();
		List<UUID> firsts = chickens.claimDuring(() -> sessions.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS);
		helper.assertTrue(firsts.size() == 3, "first one chicken per group");
		helper.getLevel().getEntity(firsts.get(0)).discard();
		helper.getLevel().getEntity(firsts.get(1)).setNoGravity(true);

		int[] tick = {0};
		int[] fullAt = {-1};
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			tick[0]++;
			int now = chickens.alive();
			helper.assertTrue(now <= 12, "never more than the sum: " + now);
			if (now == 12 && fullAt[0] < 0) {
				fullAt[0] = tick[0];
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(fullAt[0] >= 0, "3 groups of 4 in the end, seen " + chickens.alive());
			helper.assertTrue(fullAt[0] >= 38, // the limit is 40 ticks after the burst
					"the chicken in the air pops at the limit, not before: tick " + fullAt[0]);
		});
	}

	/**
	 * Another test's chickens next to this test's spot are not this test's: they were there first, and
	 * the ones they pop into appear where none of this test's chickens went.
	 */
	@GameTest(maxTicks = 120, padding = 48)
	public void aNeighboursChickensNextToTheSpotAreNotCounted(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager mine = sessions(helper);
		SessionManager neighbour = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		neighbour.countChickens(helper.getLevel(), at.add(16, 0, 0), new Fact(2, 5), "test");
		OwnChickens chickens = new OwnChickens(helper).followPops();
		helper.onEachTick(() -> {
			mine.tick();
			neighbour.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= 12, "never the neighbour's: " + chickens.alive());
		});
		// The neighbour's chickens are out and popping before this test's appear.
		helper.runAfterDelay(20, () -> chickens.claimDuring(() -> mine.countChickens(helper.getLevel(), at, new Fact(3, 4), "test"),
				at, OwnChickens.SPAWN_RADIUS));
		helper.runAfterDelay(20 + 45, () -> {
			helper.assertTrue(chickens.alive() == 12, "3 groups of 4, and none of the neighbour's 10: " + chickens.alive());
			helper.succeed();
		});
	}

	/** The whole free-play path: a kid answers right, the bomb goes off after the escape time, the first generation pops on landing. */
	@GameTest(maxTicks = 200, padding = 48)
	public void aPlacedKippenbomCountsOutItsSum(GameTestHelper helper) {
		placedKippenbom(helper, new TafelBoemConfig(), question -> question.a() * question.b());
	}

	@GameTest(maxTicks = 200, padding = 48)
	public void withCountingOffAPlacedKippenbomLets12ChickensFly(GameTestHelper helper) {
		TafelBoemConfig off = new TafelBoemConfig();
		off.countingKippenbom = false;
		placedKippenbom(helper, off, question -> 12);
	}

	/**
	 * A kid answers a placed Kippenbom right; the test passes once the bomb is gone (escape time at
	 * most 57 ticks) and exactly {@code expected} chickens stand (the pop takes at most 40 more), and
	 * fails when ever more do.
	 */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	private static void placedKippenbom(GameTestHelper helper, TafelBoemConfig config, ToIntFunction<QuizS2C> expected) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = sessions(helper, client, config);
		BlockPos bombPos = new BlockPos(2, 1, 2);
		Vec3 bomb = Vec3.atCenterOf(helper.absolutePos(bombPos));
		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(bombPos.east(3)));
		kid.teleportTo(stand.x, stand.y, stand.z);
		// The boom lets the chickens out where the bomb stood.
		OwnChickens chickens = new OwnChickens(helper).followPops();
		chickens.watch(() -> bomb);

		helper.setBlock(bombPos, ModBlocks.bomb(BombTier.KIPPENBOM));
		sessions.startFromBlock(kid, BombTier.KIPPENBOM, helper.absolutePos(bombPos));
		QuizS2C question = client.last();
		helper.assertValueEqual(question.mode(), QuizS2C.ASK, "the bomb asks its question");
		sessions.onClientAction(kid, new QuizC2S(question.sessionId(), QuizC2S.ANSWER, question.a() * question.b(), QuizC2S.VIA_KEY));

		int wanted = expected.applyAsInt(question);
		helper.onEachTick(() -> {
			sessions.tick();
			chickens.tick();
			helper.assertTrue(chickens.alive() <= wanted,
					question.a() + " x " + question.b() + " should show at most " + wanted + " chickens, not " + chickens.alive());
		});
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(ModBlocks.bomb(BombTier.KIPPENBOM), bombPos);
			helper.assertTrue(chickens.alive() == wanted, question.a() + " x " + question.b() + " should show " + wanted
					+ " chickens, not " + chickens.alive());
		});
	}

	private static SessionManager sessions(GameTestHelper helper) {
		return sessions(helper, new RecordingQuizClient(), new TafelBoemConfig());
	}

	private static SessionManager sessions(GameTestHelper helper, RecordingQuizClient client, TafelBoemConfig config) {
		try {
			return new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(Files.createTempDirectory("tafelboem-gametest"))),
					client, config);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
```

- [ ] **Step 4: Count with it in `BossFightGameTest`**

All edits are in `src/gametest/java/nl/jeeninga/tafelboem/gametest/BossFightGameTest.java`.

1. Remove the imports `net.minecraft.world.entity.animal.chicken.Chicken`,
   `net.minecraft.world.phys.AABB` and `nl.jeeninga.tafelboem.game.BombEffects`. After the edits
   below nothing uses them. Keep `java.util.UUID`.
2. Remove the constant `NOT_THE_CHASE_RADIUS` and its Javadoc line. Keep `CHASE_SPAWN_RADIUS`.
3. In `onTheHardLevelHeThrowsOnlyKippenbommenAndCountsEveryAnswer`, keep everything from the method
   start to the `hitsRemaining` assertion as it is. Replace the rest, up to and including the
   `helper.onEachTick(…)` block, with this (the `succeedWhen` block after it stays as it is):

```java
		// A kick lets the counted chickens out where he is hit, a wrong answer one at the kid.
		OwnChickens chickens = new OwnChickens(helper).followPops();
		chickens.watch(() -> fight.boss().position());

		Set<Integer> answered = new HashSet<>();
		int[] chickensAfterWrong = {-1};
		int[] mostChickens = {0};
		int[] firstRightProduct = {-1};
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			chickens.tick();
			for (RecordingQuizClient.Sent sent : List.copyOf(client.sentTo)) {
				QuizS2C question = sent.payload();
				if (question.mode() == QuizS2C.ASK && answered.add(question.sessionId())) {
					helper.assertTrue(question.tier() == BombTier.KIPPENBOM.tier(), "only Kippenbommen on level 3, got tier " + question.tier());
					// The first answer is wrong, the rest right.
					int product = question.a() * question.b();
					int answer = answered.size() == 1 ? product + 1 : product;
					QuizC2S reply = new QuizC2S(question.sessionId(), QuizC2S.ANSWER, answer, QuizC2S.VIA_KEY);
					if (answered.size() == 1) {
						chickensAfterWrong[0] = chickens.claimDuring(() -> sessions.onClientAction(sent.player(), reply), player.position(), 3).size();
					} else {
						sessions.onClientAction(sent.player(), reply);
					}
					if (answered.size() == 2) {
						firstRightProduct[0] = product;
					}
				}
			}
			mostChickens[0] = Math.max(mostChickens[0], chickens.alive());
		});
```

4. In the Javadoc of `theChaseChickensPoofWhenTheChaseEnds`, change `({@link ChaseChickens})` to
   `({@link OwnChickens})`. In its body, replace `ChaseChickens chase = new ChaseChickens(helper,
   fight.boss());` with `OwnChickens chase = new OwnChickens(helper);`. In its `onEachTick`, replace
   the line `if (fight.boss().landingStyle() == LandingStyle.CHASED) {` and the two lines after it
   (`chased[0] = true;` and the `mostDuringTheChase[0] = …` line) with:

```java
			if (fight.boss().landingStyle() == LandingStyle.CHASED) {
				if (!chased[0]) {
					// The chase's four appear behind him on the tick it begins; anyone else's chickens don't.
					List<UUID> four = chase.claimNew(fight.boss().position(), CHASE_SPAWN_RADIUS);
					helper.assertTrue(four.size() == 4, "4 chickens appear with the chase, saw " + four.size());
				}
				chased[0] = true;
				mostDuringTheChase[0] = Math.max(mostDuringTheChase[0], chase.alive());
```

   The `chase.tick();` call after `fights.tick();` stays, and so does the `else if` branch.

5. In `whenTheFightEndsDuringTheChaseTheChickensGoWithIt`, replace `ChaseChickens chase = new
   ChaseChickens(helper, fight.boss());` with these two lines:

```java
		OwnChickens chase = new OwnChickens(helper);
		boolean[] claimed = {false};
```

   In its `onEachTick`, right after `chase.tick();`, insert:

```java
			if (!claimed[0] && fight.boss().landingStyle() == LandingStyle.CHASED) {
				// The chase's four appear behind him on the tick it begins; anyone else's chickens don't.
				claimed[0] = true;
				List<UUID> four = chase.claimNew(fight.boss().position(), CHASE_SPAWN_RADIUS);
				helper.assertTrue(four.size() == 4, "4 chickens appear with the chase, saw " + four.size());
			}
```

6. Delete the class `ChaseChickens`, with its Javadoc.
7. In `watchKippenbommenLand`, replace the `helper.assertTrue(fights.summon(…) != null, "Graaf Fout
   should appear");` line with:

```java
		BossFight fight = fights.summon(helper.getLevel(), stand.add(6, 1.5, 0), FightLevel.MOEILIJK, null);
		helper.assertTrue(fight != null, "Graaf Fout should appear");
		// Every chicken a fight lets out appears where he is hit, behind him for the chase, or at the kid.
		OwnChickens chickens = new OwnChickens(helper).followPops();
		chickens.watch(() -> fight.boss().position());
		chickens.watch(player::position);
```

   In its `onEachTick`, add `chickens.tick();` right after `fights.tick();`. Replace the last line
   of that block with
   `mostChickens[0] = Math.max(mostChickens[0], chickens.alive());`.
8. Delete the method `spectacleChickens(GameTestHelper, Vec3, double)`.

- [ ] **Step 5: Count with it in `SessionGameTest`**

In `src/gametest/java/nl/jeeninga/tafelboem/gametest/SessionGameTest.java`, replace the method
`chickeningOutLetsAChickenOut` with the code below. Then remove the imports
`net.minecraft.world.entity.Entity`, `net.minecraft.world.entity.animal.chicken.Chicken`,
`net.minecraft.world.phys.AABB` and `nl.jeeninga.tafelboem.game.BombEffects`, which nothing uses
any more, and add `import java.util.UUID;`.

```java
	@GameTest
	public void chickeningOutLetsAChickenOut(GameTestHelper helper) {
		Fixture f = new Fixture(helper);
		QuizS2C question = f.ask();

		List<UUID> chickens = new OwnChickens(helper).claimDuring(
				() -> f.manager.onClientAction(f.player, new QuizC2S(question.sessionId(), QuizC2S.CHICKEN_OUT, 0, QuizC2S.VIA_KEY)),
				Vec3.atCenterOf(helper.absolutePos(BOMB)), 3);

		helper.assertValueEqual(f.client.last().mode(), QuizS2C.FEEDBACK, "ESC shows the fact on the HUD");
		helper.assertValueEqual(f.client.last().reason(), QuizS2C.REASON_CHICKEN_OUT, "feedback reason");
		helper.assertFalse(chickens.isEmpty(), "a chicken should pop out");
		helper.succeed();
	}
```

- [ ] **Step 6: Run every GameTest three times**

Run: `./gradlew build` once, then `./gradlew runGameTest` two more times.
Expected each time: `All 71 required tests passed` (70 before, plus the new one). Fight tests
changed, so all three runs must pass. If one fails, find the cause before going on, with
superpowers:systematic-debugging.

Also check: `grep -rn "spectacleChickens\|ChaseChickens\|NOT_THE_CHASE_RADIUS" src/gametest` prints
nothing.

- [ ] **Step 7: Commit**

```bash
git add src/gametest/java/nl/jeeninga/tafelboem/gametest/OwnChickens.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/ChickenGameTest.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/BossFightGameTest.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/SessionGameTest.java
git commit -m "Count only a test's own chickens, so a neighbouring test can't change a count

GameTests share one level and a fight reaches into the next test's field;
a count of every spectacle chicken near a spot could count a neighbour's.
OwnChickens claims only the chickens that appear where this test's come
from. Its own GameTest shows a neighbour's 10 chickens 16 blocks away stay
out of a 3 x 4 count.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 4: A fresh world each run, never saved

**Files:**
- Create: `src/gametest/resources/tafelboem-test.mixins.json`
- Modify: `src/gametest/resources/fabric.mod.json`
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java`
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java`
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/MinecraftServerMixin.java`
- Modify: `build.gradle` (after the Task 1 snippet)
- Modify: `docs/evaluations/faster-tests.md`

**Interfaces:**
- Produces:
  - `GameTestHarness.LOGGER` (slf4j, name `tafelboem-test`);
  - `GameTestHarness.neverSave(MinecraftServer)`;
  - the mixin config `tafelboem-test.mixins.json`, with package `nl.jeeninga.tafelboem.gametest.mixin`.

**Background:**
- At 1.1.0, 8 of the 9 s of a run's shutdown went into `MinecraftServer.stopServer` saving every
  chunk, and the test world had grown to 1.2 GB.
- `GameTestServer` never overrides `stopServer`, and `MinecraftServer.runServer` calls
  `onServerExit()` after it, in a `finally`. There `GameTestServer` calls
  `System.exit(failed required tests)`.
- The test report is written before that, by `GlobalTestReporter.finish()` in `tickServer`.
- Setting `ServerLevel.noSave` (what `/save-off` does) also stops chunk saves while the tests run.
- The test mod is loaded in the client GameTest too. That run's integrated server must keep saving,
  because `WorldClientGameTest` reopens its world. So the mixin acts only on a `GameTestServer`.

- [ ] **Step 1: Write the mixin config and name it in the test mod**

Create `src/gametest/resources/tafelboem-test.mixins.json`:

```json
{
	"required": true,
	"package": "nl.jeeninga.tafelboem.gametest.mixin",
	"compatibilityLevel": "JAVA_25",
	"mixins": [
		"GameTestServerMixin",
		"MinecraftServerMixin"
	],
	"injectors": {
		"defaultRequire": 1
	}
}
```

In `src/gametest/resources/fabric.mod.json`, add after the `"entrypoints": { … },` block:

```json
	"mixins": [
		"tafelboem-test.mixins.json"
	],
```

- [ ] **Step 2: Write `GameTestHarness`**

Create `src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java`:

```java
package nl.jeeninga.tafelboem.gametest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What the test mod changes in how Minecraft runs the GameTests, so they take less time; the mixins
 * in {@code gametest.mixin} call in here. The GameTest server never saves its world: Gradle deletes
 * it before every run.
 */
public final class GameTestHarness {
	public static final Logger LOGGER = LoggerFactory.getLogger("tafelboem-test");

	private GameTestHarness() {
	}

	/** Like {@code /save-off}: no chunk is written while the tests run. */
	public static void neverSave(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			level.noSave = true;
		}
		LOGGER.info("TafelBoem GameTests: the test world is never saved");
	}
}
```

- [ ] **Step 3: Write the two mixins**

Create `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java`:

```java
package nl.jeeninga.tafelboem.gametest.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/** How the GameTest server runs the tests: see {@link GameTestHarness}. */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
	@Inject(method = "initServer", at = @At("RETURN"))
	private void tafelboem$neverSave(CallbackInfoReturnable<Boolean> cir) {
		GameTestHarness.neverSave((MinecraftServer) (Object) this);
	}
}
```

Create `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/MinecraftServerMixin.java`:

```java
package nl.jeeninga.tafelboem.gametest.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/**
 * The GameTest server stops without saving its world (8 of the 9 s its shutdown took): Gradle
 * deletes that world before the next run anyway. Any other server, like the client GameTest's,
 * stops as usual.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "stopServer", at = @At("HEAD"), cancellable = true)
	private void tafelboem$stopWithoutSaving(CallbackInfo ci) {
		if ((Object) this instanceof GameTestServer) {
			GameTestHarness.LOGGER.info("TafelBoem GameTests: stopping without saving the test world");
			ci.cancel();
		}
	}
}
```

- [ ] **Step 4: Let Gradle delete the world before each run**

In `build.gradle`, after the Task 1 snippet, add:

```groovy
// Every GameTest run starts in a fresh world, which the test mod never saves (GameTestHarness).
def deleteGameTestWorld = tasks.register("deleteGameTestWorld", Delete) {
	delete layout.projectDirectory.dir("build/run/gameTest/world")
}
tasks.named("runGameTest") { dependsOn(deleteGameTestWorld) }
```

- [ ] **Step 5: Run it and read the log**

Run: `./gradlew runGameTest 2>&1 | tee build/task4.log | grep -E "TafelBoem GameTests|required tests|Saving chunks|BUILD"`
Expected:
- `TafelBoem GameTests: the test world is never saved`;
- `All 71 required tests passed`;
- `TafelBoem GameTests: stopping without saving the test world`;
- no `Saving chunks for level` line;
- `BUILD SUCCESSFUL`.

Then `du -sh build/run/gameTest/world` should show a few MB at most, not 1.2 GB.

Also run `./gradlew runClientGameTest` once. Expected: `BUILD SUCCESSFUL`. `WorldClientGameTest`
reopens its world, which proves the client's integrated server still saves.

- [ ] **Step 6: Time it and write it down**

Run: `tools/time-gradle.sh runGameTest`
Add a row to the Steps table in `docs/evaluations/faster-tests.md`:
`| fresh world, never saved | runGameTest | <baseline median> | <this median> | yes |`.
If the median is not lower than the baseline, take out `neverSave` (Step 2) first and time again.
If it is still not lower, take the whole step out and write `no` with the reason.

- [ ] **Step 7: Commit**

```bash
git add src/gametest/resources/tafelboem-test.mixins.json src/gametest/resources/fabric.mod.json \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/MinecraftServerMixin.java \
	build.gradle docs/evaluations/faster-tests.md
git commit -m "Start every GameTest run in a fresh world and never save it

Saving the test world took 8 of the 9 s of every run's shutdown, and the
world had grown to 1.2 GB. Gradle now deletes it before each run, and the
test mod's first mixins keep the GameTest server from saving; the client
GameTest's server, which reopens its world, saves as before.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 5: One batch per server

**Files:**
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java`
- Modify: `docs/evaluations/faster-tests.md`

**Interfaces:**
- Consumes: `GameTestHarness` (Task 4).
- Produces: `GameTestHarness.oneBatch(List<GameTestBatch>) → List<GameTestBatch>`.

**Background:**
- `GameTestBatchFactory.divideIntoBatches` splits the tests into batches of at most 50 per test
  environment, and `GameTestRunner` runs the batches one after the other. That's two batches here,
  and each waits for its slowest test.
- `GameTestServer.evaluateTestsToRun(ServerLevel)` (private) returns those batches. The mixin
  replaces its result.
- All our tests use `minecraft:default`, so this gives one batch.

- [ ] **Step 1: Add `oneBatch` to `GameTestHarness`**

Add these imports:

```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
```

Add this method:

```java
	/**
	 * Minecraft runs at most 50 tests at a time and the batches one after the other, so each batch
	 * waits for its slowest test. One batch per test environment starts every test at once.
	 */
	public static List<GameTestBatch> oneBatch(List<GameTestBatch> batches) {
		Map<Holder<TestEnvironmentDefinition<?>>, List<GameTestInfo>> byEnvironment = new LinkedHashMap<>();
		for (GameTestBatch batch : batches) {
			byEnvironment.computeIfAbsent(batch.environment(), environment -> new ArrayList<>()).addAll(batch.gameTestInfos());
		}
		List<GameTestBatch> merged = new ArrayList<>();
		byEnvironment.forEach((environment, tests) -> merged.add(GameTestBatchFactory.toGameTestBatch(tests, environment, merged.size())));
		return merged;
	}
```

Change the class Javadoc's second sentence to: "The GameTest server never saves its world (Gradle
deletes it before every run) and runs all its tests in one batch."

- [ ] **Step 2: Call it from the mixin**

In `GameTestServerMixin`, add the imports `java.util.List`,
`net.minecraft.gametest.framework.GameTestBatch` and `net.minecraft.server.level.ServerLevel`. Then
add:

```java
	@Inject(method = "evaluateTestsToRun", at = @At("RETURN"), cancellable = true)
	private void tafelboem$oneBatch(ServerLevel level, CallbackInfoReturnable<List<GameTestBatch>> cir) {
		cir.setReturnValue(GameTestHarness.oneBatch(cir.getReturnValue()));
	}
```

- [ ] **Step 3: Run it and read the log**

Run: `./gradlew runGameTest 2>&1 | tee build/task5.log | grep -E "Running test environment|required tests"`
Expected: exactly one `Running test environment 'minecraft:default' batch 0 (71 tests)...` line,
and `All 71 required tests passed`. Run `./gradlew runGameTest` twice more. All tests now run at
once, so all three runs must pass.

- [ ] **Step 4: Time it and write it down**

Run: `tools/time-gradle.sh runGameTest`. Add a row `| one batch | runGameTest | <Task 4 median> |
<this median> | yes/no |`. If it saves no time, revert Steps 1–2 and write `no` with the reason.

- [ ] **Step 5: Commit**

```bash
git add src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java \
	docs/evaluations/faster-tests.md
git commit -m "Run all GameTests in one batch, so the slowest test counts once

Minecraft ran at most 50 tests at a time and the batches one after the
other: the 71 tests took two batches, each waiting for its slowest test.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 6: A quick group and a fights group on two servers

**Files:**
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/Fight.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/BossFightGameTest.java` (14 marks)
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/TafelBoemWorldGameTest.java` (4 marks)
- Modify: `build.gradle`
- Modify: `.github/workflows/build.yml`
- Modify: `docs/evaluations/faster-tests.md`

**Interfaces:**
- Consumes: `GameTestHarness.oneBatch` (Task 5).
- Produces:
  - `@Fight`;
  - `GameTestHarness.keepChosenGroup(List<GameTestBatch>, ServerLevel)`;
  - `GameTestHarness.repeat(int)`;
  - system properties `tafelboem.gametest.group` (`quick` | `fights`) and
    `tafelboem.gametest.repeat`;
  - Gradle tasks `runQuickGameTests`, `runFightGameTests` and `runGameTest` (both);
  - properties `-Ptests=<pattern>` and `-Prepeat=<n>`;
  - reports `build/reports/gametest-quick.xml` and `build/reports/gametest-fights.xml`.

**Background:**
- Fabric names a test `tafelboem-test:` plus `ClassName_methodName` in snake case. It inserts `_`
  only between a lower-case letter and an upper-case one, then lower-cases everything
  (`TestAnnotationLocator.TestMethod.identifier`). `GameTestHarness` builds the same ids. The check
  that every `@Fight` id is a registered test catches any mismatch.
- `-Ptests` becomes Fabric's `fabric-api.gametest.filter`, Minecraft's resource selector:
  `namespace:path`, with `*` and `?`, and `minecraft:` when there's no namespace. Minecraft already
  fails the run when the selector matches nothing at all. When it matches tests, but none in this
  group, `keepChosenGroup` fails it. So `-Ptests` goes with the group's own task, not with
  `runGameTest`.
- Minecraft's repeat (`GameTestServer.create(…, int repeatCount)`) works only with a selection, and
  Fabric always passes 0. The mixin swaps in `tafelboem.gametest.repeat`. Gradle refuses `-Prepeat`
  without `-Ptests`.
- With `enableGameTests = false`, Loom makes no `gameTest` run, no `runGameTest` task and no
  `check` link. This task makes all three itself. `RunConfigSettings` has `inherit`, `source(String)`,
  `runDir(String)` and `property(String[, String])`. Loom names a run's task `run` + its name with a
  capital first letter, and registers it as soon as the run is created.
- Gradle runs two tasks of one project side by side only when they don't depend on each other.
  Configuration cache and `org.gradle.parallel` are both on here.

**The fights group:** the tests that play Graaf Fout's fight and took 3 s or more at 1.1.0. These
times are from the baseline report:

| Class | Method | s |
|---|---|---|
| BossFightGameTest | `beatingGraafFoutGivesTheKidFourKippenbommen` | 9.2 |
| BossFightGameTest | `afterEveryLandingHeGetsUpAndThrowsAgain` | 8.6 |
| BossFightGameTest | `aFightGoesOnWhenGraafFoutFreezesMidYeet` | 6.7 |
| BossFightGameTest | `onlyTheKidWhoKickedSeesWhyHeFlewFurther` | 6.6 |
| BossFightGameTest | `onTheHardLevelHeThrowsOnlyKippenbommenAndCountsEveryAnswer` | 6.1 |
| BossFightGameTest | `onTheHardLevelAWrongAnswerCostsTwoHeartsButNeverTheLast` | 5.8 |
| BossFightGameTest | `inMultiplayerOnlyTheKidWhoseBombItIsGetsTheirViewMoved` | 5.6 |
| BossFightGameTest | `withKippenbomChickensOffNoChaseBeginsAndNoChickenAppears` | 5.3 |
| BossFightGameTest | `withKippenbomChickensOnTheChasePlaysWithinTwoLandings` | 5.3 |
| BossFightGameTest | `theChaseChickensPoofWhenTheChaseEnds` | 3.8 |
| BossFightGameTest | `theViewFollowsGraafFoutUntilHeIsDizzyOnTheGround` | 3.6 |
| BossFightGameTest | `whenTheFightEndsDuringTheChaseTheChickensGoWithIt` | 3.4 |
| BossFightGameTest | `aCorrectAnswerKicksTheBombBackAndHitsGraafFout` | 3.4 |
| BossFightGameTest | `afterAYeetThatLandsHimFarAwayHeComesBackAndThrowsAgain` | 3.2 |
| TafelBoemWorldGameTest | `aWonFightPaysTheRewardWritesTheStarsAndSendsEveryoneHome` | 7.1 |
| TafelBoemWorldGameTest | `aKidWhoLeavesRightAfterTheWinIsLoggedOnlyOnce` | 6.7 |
| TafelBoemWorldGameTest | `withEveryR11SwitchOffAFightPlaysAs102` | 4.1 |
| TafelBoemWorldGameTest | `everyKidThroughTheSameDoorJoinsTheSameFight` | 3.2 |

The quick group then has 53 tests: the 52 other TafelBoem tests and Minecraft's `always_pass`. Its
slowest is `ChickenGameTest.threeTimesFourShowsThreeGroupsOfFourThatPoofAway`, at about 5 s.

- [ ] **Step 1: Write the mark**

Create `src/gametest/java/nl/jeeninga/tafelboem/gametest/Fight.java`:

```java
package nl.jeeninga.tafelboem.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A GameTest that plays Graaf Fout's fight for 3 s or more. It runs in the fights group
 * ({@code ./gradlew runFightGameTests}); every other GameTest runs in the quick group, which
 * {@code ./gradlew build} runs. See {@link GameTestHarness}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Fight {
}
```

- [ ] **Step 2: Mark the 18 fights**

Put `@Fight` on its own line directly under the `@GameTest(…)` line of each method in the table
above, in `BossFightGameTest` and `TafelBoemWorldGameTest`. Mark no other method.

Run: `grep -c '@Fight' src/gametest/java/nl/jeeninga/tafelboem/gametest/*.java`
Expected: `BossFightGameTest.java:14`, `TafelBoemWorldGameTest.java:4`, every other file `0`.

- [ ] **Step 3: Add the group, the check and the repeat to `GameTestHarness`**

Add these imports to the right groups:
- `java.lang.reflect.Method`, `java.util.HashSet`, `java.util.Locale`, `java.util.Set`;
- `net.minecraft.core.registries.Registries`, `net.minecraft.resources.Identifier`;
- `net.fabricmc.loader.api.FabricLoader`, `net.fabricmc.loader.api.entrypoint.EntrypointContainer`.

`ServerLevel` is imported already. Add these constants and methods:

```java
	/** Gradle's choice of group, {@value #QUICK} or {@value #FIGHTS}; without it every test runs. */
	public static final String GROUP = "tafelboem.gametest.group";
	/** How many times each selected test runs: Minecraft's own repeat, which Fabric leaves at 0. */
	public static final String REPEAT = "tafelboem.gametest.repeat";
	static final String QUICK = "quick";
	static final String FIGHTS = "fights";
	private static final String TEST_MOD = "tafelboem-test";

	/**
	 * Keeps only the chosen group's tests: the ones marked {@link Fight}, or all the others. Fails the
	 * run when a mark is on something that is no GameTest, or when no test of the group is selected.
	 */
	public static List<GameTestBatch> keepChosenGroup(List<GameTestBatch> batches, ServerLevel level) {
		String group = System.getProperty(GROUP);
		if (group == null) {
			return batches;
		}
		if (!group.equals(QUICK) && !group.equals(FIGHTS)) {
			throw new IllegalStateException(GROUP + " is \"" + group + "\", not \"" + QUICK + "\" or \"" + FIGHTS + "\"");
		}
		Set<Identifier> fights = fights();
		Set<Identifier> tests = level.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE).keySet();
		List<Identifier> noTest = fights.stream().filter(id -> !tests.contains(id)).sorted().toList();
		if (!noTest.isEmpty()) {
			throw new IllegalStateException("@Fight on a method that is no GameTest: " + noTest);
		}
		boolean wantFights = group.equals(FIGHTS);
		List<GameTestBatch> kept = new ArrayList<>();
		int count = 0;
		for (GameTestBatch batch : batches) {
			List<GameTestInfo> inGroup = batch.gameTestInfos().stream().filter(test -> fights.contains(test.id()) == wantFights).toList();
			if (!inGroup.isEmpty()) {
				kept.add(GameTestBatchFactory.toGameTestBatch(inGroup, batch.environment(), kept.size()));
				count += inGroup.size();
			}
		}
		if (kept.isEmpty()) {
			throw new IllegalStateException("No GameTest in the " + group + " group matches "
					+ System.getProperty("fabric-api.gametest.filter", "*:*"));
		}
		LOGGER.info("TafelBoem GameTests: the {} group, {} tests", group, count);
		return kept;
	}

	/** Minecraft's repeat for the selected tests, which Fabric always sets to 0. */
	public static int repeat(int fabricsRepeat) {
		return Integer.getInteger(REPEAT, fabricsRepeat);
	}

	/** The ids Fabric gives the methods marked {@link Fight}: the class and method name in snake case. */
	private static Set<Identifier> fights() {
		Set<Identifier> fights = new HashSet<>();
		for (EntrypointContainer<Object> entrypoint : FabricLoader.getInstance().getEntrypointContainers("fabric-gametest", Object.class)) {
			if (!entrypoint.getProvider().getMetadata().getId().equals(TEST_MOD)) {
				continue;
			}
			Class<?> tests = entrypoint.getEntrypoint().getClass();
			for (Method method : tests.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Fight.class)) {
					String name = (tests.getSimpleName() + "_" + method.getName()).replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
					fights.add(Identifier.fromNamespaceAndPath(TEST_MOD, name));
				}
			}
		}
		return fights;
	}
```

Change the class Javadoc to:

```java
/**
 * What the test mod changes in how Minecraft runs the GameTests, so they take less time; the mixins
 * in {@code gametest.mixin} call in here. The GameTest server never saves its world (Gradle deletes
 * it before every run), runs all its tests in one batch, and runs one group when Gradle names it
 * ({@value #GROUP}): the fights ({@link Fight}) or the quick tests, so the two can run on two
 * servers side by side.
 */
```

- [ ] **Step 4: Call them from the mixin**

In `GameTestServerMixin`, add the import `org.spongepowered.asm.mixin.injection.ModifyVariable`.
Change the `evaluateTestsToRun` injection's body, and rename the method, as below. Then add the
repeat:

```java
	@Inject(method = "evaluateTestsToRun", at = @At("RETURN"), cancellable = true)
	private void tafelboem$chosenGroupInOneBatch(ServerLevel level, CallbackInfoReturnable<List<GameTestBatch>> cir) {
		cir.setReturnValue(GameTestHarness.oneBatch(GameTestHarness.keepChosenGroup(cir.getReturnValue(), level)));
	}

	@ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
	private static int tafelboem$repeat(int repeatCount) {
		return GameTestHarness.repeat(repeatCount);
	}
```

- [ ] **Step 5: Replace the GameTest run in `build.gradle`**

1. In the `fabricApi { configureTests { … } }` block, change `enableGameTests = true` to the two
   lines below:

```groovy
		// The quick and fights groups below replace Loom's one run of every GameTest.
		enableGameTests = false
```

2. Delete the Task 1 snippet (`loom.runs.named("gameTest") { … }` with its comment) and the Task 4
   snippet (`deleteGameTestWorld` and `tasks.named("runGameTest") …`, with its comment). Put this in
   their place:

```groovy
// --- GameTests: a quick group and a fights group ----------------------------------------------
// A GameTest marked @Fight plays a whole fight and runs in the fights group; all others run in the
// quick group. `build` runs the quick group, `runGameTest` both, on two servers side by side.
// -Ptests=<pattern> runs only the matching tests of the group (Minecraft's selector: * and ?), and
// -Prepeat=<n> runs each of them n times in one server. Every run starts in a fresh world that the
// test mod never saves, and writes each test's time to build/reports/gametest-<group>.xml.
def gameTestPattern = providers.gradleProperty("tests")
def gameTestRepeat = providers.gradleProperty("repeat")
if (gameTestRepeat.isPresent() && !gameTestPattern.isPresent()) {
	throw new GradleException("-Prepeat needs -Ptests=<pattern>: Minecraft repeats only selected tests")
}
[quick: "QuickGameTests", fights: "FightGameTests"].each { groupName, runName ->
	def dir = "build/run/gameTest-${groupName}"
	def deleteWorld = tasks.register("delete${runName}World", Delete) {
		delete layout.projectDirectory.dir("${dir}/world")
	}
	loom.runs.create(runName.uncapitalize()) {
		inherit(loom.runs.getByName("server"))
		source("gametest")
		runDir(dir)
		property("fabric-api.gametest")
		property("fabric-api.gametest.report-file", file("build/reports/gametest-${groupName}.xml").absolutePath)
		property("tafelboem.gametest.group", groupName)
		if (gameTestPattern.isPresent()) {
			property("fabric-api.gametest.filter", gameTestPattern.get())
		}
		if (gameTestRepeat.isPresent()) {
			property("tafelboem.gametest.repeat", gameTestRepeat.get())
		}
	}
	tasks.named("run${runName}") {
		group = "tafelboem"
		description = "Runs the ${groupName} GameTests on a headless server."
		dependsOn(deleteWorld)
	}
}
tasks.register("runGameTest") {
	group = "tafelboem"
	description = "Runs every GameTest: the quick and the fights group, on two servers side by side."
	dependsOn("runQuickGameTests", "runFightGameTests")
}
tasks.named("check") { dependsOn("runQuickGameTests") }
```

   The loop's variables are `groupName` and `dir`, not `group` and `runDir`. In Groovy a closure's
   local variable hides the task's `group` property and Loom's `runDir(…)` method.

3. In `tasks.register("publishPack", …)`, change `dependsOn(checkRelease, "build")` to
   `dependsOn(checkRelease, "build", "runFightGameTests")`. Then nothing is published without every
   GameTest.

4. Delete the old run folder once, by hand: `rm -rf build/run/gameTest`. It holds the 1.2 GB world.

- [ ] **Step 6: Run CI's command in CI**

In `.github/workflows/build.yml`, change `run: ./gradlew build` to
`run: ./gradlew build runFightGameTests`.

- [ ] **Step 7: Check the groups, the pattern, the repeat and the failures**

Run each of these and compare with what's expected:

1. `./gradlew runQuickGameTests 2>&1 | grep -E "TafelBoem GameTests: the|required tests"`
   Expected: `the quick group, 53 tests` and `All 53 required tests passed`.
2. `./gradlew runFightGameTests 2>&1 | grep -E "TafelBoem GameTests: the|required tests"`
   Expected: `the fights group, 18 tests` and `All 18 required tests passed`.
3. `./gradlew runGameTest 2>&1 | tee build/task6.log | grep -E "Started game test server|required tests|BUILD"`
   Expected:
   - two `Started game test server` lines, at most 2 s apart;
   - `All 53 …` and `All 18 …`;
   - `BUILD SUCCESSFUL`.

   If the two servers start one after the other (the second after the first's `required tests`
   line), they don't run side by side. Report `DONE_WITH_CONCERNS`, with
   `./gradlew runGameTest --info | grep -iE "parallel|waiting|lock"` in the report, and don't work
   around it.
4. `./gradlew runFightGameTests -Ptests='tafelboem-test:boss_fight_game_test_beating*' -Prepeat=3 2>&1 | grep -E "will be run|required tests"`
   Expected: `Each test that matches … will be run 3 times (total: 3)` and
   `All 3 required tests passed`.
5. `./gradlew runQuickGameTests -Ptests='tafelboem-test:boss_fight_game_test_beating*'`
   Expected: FAIL. The log has
   `No GameTest in the quick group matches tafelboem-test:boss_fight_game_test_beating*`.
6. `./gradlew runQuickGameTests -Prepeat=3`
   Expected: FAIL at configuration with `-Prepeat needs -Ptests=<pattern>`.
7. Put `@Fight` on a test's helper method, such as `BossFightGameTest.tempDir`, then run
   `./gradlew runFightGameTests`.
   Expected: FAIL, with `@Fight on a method that is no GameTest: [tafelboem-test:boss_fight_game_test_temp_dir]`.
   Remove that `@Fight` again.
8. `./gradlew build`
   Expected: `BUILD SUCCESSFUL`, with `runQuickGameTests` in the task list and not
   `runFightGameTests`.

Run `./gradlew runGameTest` two more times: both must pass.

- [ ] **Step 8: Time it and write it down**

Warm each command up once, then:

```bash
tools/time-gradle.sh build
tools/time-gradle.sh runGameTest
tools/time-gradle.sh runFightGameTests -Ptests='tafelboem-test:boss_fight_game_test_beating_graaf_fout_gives_the_kid_four_kippenbommen' -Prepeat=3
tools/time-gradle.sh build runFightGameTests
```

Add rows `| two groups, two servers | build | … | … | yes |`, the same for `runGameTest`, and
`| … | one slow test three times (-Prepeat=3) | <3 × baseline runGameTest> | … | yes |`. Note the
`build runFightGameTests` median: it's `publishPack`'s own part of the release check, and Task 9
uses it.

- [ ] **Step 9: Commit**

```bash
git add src/gametest/java/nl/jeeninga/tafelboem/gametest/Fight.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/GameTestServerMixin.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/BossFightGameTest.java \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/TafelBoemWorldGameTest.java \
	build.gradle .github/workflows/build.yml docs/evaluations/faster-tests.md
git commit -m "Split the GameTests into a quick group for build and a fights group

build now runs only the quick group; the 18 tests that play a fight for 3 s
or more carry @Fight and run in their own group, on a second server side by
side with the first for runGameTest. -Ptests and -Prepeat run one slow test
three times in one server instead of three full runs. publishPack and CI
still run every GameTest.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 7: Skip clearing what is clear already

**Files:**
- Create: `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/StructureUtilsMixin.java`
- Modify: `src/gametest/resources/tafelboem-test.mixins.json`
- Modify: `src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java`
- Modify: `docs/evaluations/faster-tests.md`

**Interfaces:**
- Produces: `GameTestHarness.alreadyClear(int airIfAboveThisY, BlockPos, ServerLevel) → boolean`.

**Background:**
- Before a test, `StructureUtils.clearSpaceForStructure` calls `clearBlock(airIfAboveThisY, pos,
  level)` for every block of the padded box: stone below that height, air above, each with flags 818
  and `updateNeighborsAt`.
- At padding 48 that's 97³ blocks a test, and about 66 million in a run.
- In the fresh world (Task 4), the box starts at the test block's height (y −59), above the flat
  ground, so nearly every block is air already.
- Skipping a block that already is what it would become changes no block, so it can't change a test.
- The padding stays: it also spaces the tests (spec, *Grid*).
- Set-up time is the gap in the log between `… tests are now running at position …` and
  `Running test environment …`.

- [ ] **Step 1: Read the set-up time before the change**

From `build/task6.log` (Task 6, Step 7.3), note the seconds between the
`tests are now running at position` line and the `Running test environment` line for each server.

- [ ] **Step 2: Add `alreadyClear` to `GameTestHarness`**

Add the imports `net.minecraft.core.BlockPos`, `net.minecraft.world.level.block.Blocks` and
`net.minecraft.world.level.block.state.BlockState`, and this method:

```java
	/**
	 * Before each test Minecraft sets every block of its padded box to stone (below the given height)
	 * or air, with block updates. Whether this one already is: in a fresh world nearly all are.
	 */
	public static boolean alreadyClear(int airIfAboveThisY, BlockPos pos, ServerLevel level) {
		BlockState cleared = pos.getY() < airIfAboveThisY ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
		return level.getBlockState(pos) == cleared;
	}
```

Add "and skips clearing blocks that already are air or stone" to the end of the Javadoc's sentence
about what the server does.

- [ ] **Step 3: Write the mixin and add it to the config**

Create `src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/StructureUtilsMixin.java`:

```java
package nl.jeeninga.tafelboem.gametest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/** Clearing a test's padded box skips the blocks that already are what they would become. */
@Mixin(StructureUtils.class)
public abstract class StructureUtilsMixin {
	@Inject(method = "clearBlock", at = @At("HEAD"), cancellable = true)
	private static void tafelboem$skipWhatIsClear(int airIfAboveThisY, BlockPos pos, ServerLevel level, CallbackInfo ci) {
		if (GameTestHarness.alreadyClear(airIfAboveThisY, pos, level)) {
			ci.cancel();
		}
	}
}
```

In `tafelboem-test.mixins.json`, add `"StructureUtilsMixin"` to the `mixins` list. Keep the list
sorted.

- [ ] **Step 4: Run, compare the set-up and time it**

Run: `./gradlew runGameTest 2>&1 | tee build/task7.log | grep -E "now running at|Running test environment|required tests"`
Expected: both groups pass, and the gap between the two lines is smaller than in Step 1. Run
`./gradlew runGameTest` twice more: both must pass.

Then run `tools/time-gradle.sh runGameTest` and `tools/time-gradle.sh build`. Add rows
`| skip clearing what is clear | … |`, including the set-up seconds before and after. If the
medians don't drop, revert Steps 2–3 and write `no` with the numbers.

- [ ] **Step 5: Commit**

```bash
git add src/gametest/java/nl/jeeninga/tafelboem/gametest/mixin/StructureUtilsMixin.java \
	src/gametest/resources/tafelboem-test.mixins.json \
	src/gametest/java/nl/jeeninga/tafelboem/gametest/GameTestHarness.java \
	docs/evaluations/faster-tests.md
git commit -m "Skip clearing test blocks that are air or stone already

Before each test Minecraft set every block of its padded box, about 66
million blocks a run, almost all of them air already in the fresh test
world. Skipping those changes no block; the padding, which also keeps the
tests apart, stays.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

### Task 8: The client test: measure, and decide

**Files:**
- Modify: `docs/evaluations/faster-tests.md`
- Modify (only if the decision below says so):
  `src/gametest/java/nl/jeeninga/tafelboem/gametest/PlaythroughClientGameTest.java`,
  `WorldClientGameTest.java`

**Interfaces:** none.

**Background:**
- The client runs in real time. Its fixed waits add up to 322 ticks (16 s) in
  `PlaythroughClientGameTest` and 204 ticks (10 s) in `WorldClientGameTest`, not counting the
  1-tick polls in loops.
- The spec allows replacing a fixed wait only where there is something to wait for, keeping every
  screenshot and every check. It says to stop if that gains less than 15 s.
- From reading the code, nearly every fixed wait is one of these:
  - **the timing of a screenshot**: the 14 ticks after a kick catch him mid-air, the 80 ticks
    catch the Kippenbom's boom;
  - **time to show that something does not happen**: the 40-tick waits in `WorldClientGameTest`
    before `itemsIn(…) == 0` and "still exactly 5 bomb frames".

  Neither kind has a condition to wait for.

- [ ] **Step 1: List every fixed wait**

Run: `grep -n "waitTicks([0-9]*)" src/gametest/java/nl/jeeninga/tafelboem/gametest/*ClientGameTest.java | grep -v "waitTicks(1)"`
For each line, decide whether it is:
- (a) the timing of a screenshot;
- (b) time to show that something does not happen;
- (c) a wait for something that has a condition (a screen, a block, an entity, a pose).

Only (c) can become `context.waitFor(…)`. Add up the ticks in (c), counting a wait inside a loop
once per pass of that loop.

- [ ] **Step 2: Decide**

If (c) adds up to less than 300 ticks (15 s), change no test. Write a section
`## The client test` in `docs/evaluations/faster-tests.md` with:
- the totals of (a), (b) and (c);
- "not worth it: the client runs in real time, and its waits time screenshots or show that
  something doesn't happen; the saving is in running it less often (the working rules)".

If (c) adds up to 300 ticks or more, replace those waits with `context.waitFor(…)` on their
condition. Run `./gradlew runClientGameTest` three times: all must pass. Compare the screenshots in
`build/run/clientGameTest/screenshots/` with `docs/screenshots/` by eye. Then time it with
`tools/time-gradle.sh runClientGameTest`.

- [ ] **Step 3: Commit**

```bash
git add docs/evaluations/faster-tests.md   # plus the two client tests if Step 2 changed them
git commit -m "Write down why the client test keeps its fixed waits

Co-Authored-By: <model> <noreply@anthropic.com>"
```

If Step 2 changed the tests, the subject is "Wait for what the client test waits for, not a fixed
time" instead.

---

### Task 9: The working rules, the docs and the finish

**Files:**
- Modify: `AGENTS.md`
- Modify: `README.md` (the `## Building` block)
- Modify: `docs/evaluations/faster-tests.md`

**Interfaces:** none; it describes Tasks 1–8.

- [ ] **Step 1: Five full runs in a row**

Run: `for i in 1 2 3 4 5; do ./gradlew runGameTest > build/run-$i.log 2>&1 && echo "run $i ok" || { echo "run $i FAILED"; break; }; done`
Expected: `run 1 ok` … `run 5 ok`. A failure stops the task. Find the cause with
superpowers:systematic-debugging before going on.

- [ ] **Step 2: The final numbers**

Warm each command up once, then time `build`, `runGameTest`, `runClientGameTest`, the slow test
three times (the Task 6 command) and `build runFightGameTests`. Add a `## Done when` section to
`docs/evaluations/faster-tests.md`, with the measured values in place of `…`:

```markdown
## Done when

| Run | 1.1.0 | Target | Now | Met |
|---|---|---|---|---|
| `./gradlew build` | … | 30 s or less | … | … |
| `./gradlew runGameTest` (all) | … | 35 s or less | … | … |
| one slow test three times | … | 45 s or less | … | … |
| a release check | … | 3½ min or less | … | … |
| `./gradlew runClientGameTest` | … | 2 min or less, or not worth it | … | … |

- Every GameTest runs before every publish: `publishPack` runs `build` and `runFightGameTests`.
- All 69 TafelBoem GameTests stay with what they assert; only the counts are isolated
  (`OwnChickens`), and one test of that helper was added (71 with Minecraft's `always_pass`).
- Five full runs in a row passed on <date>.
```

The 1.1.0 column uses the baseline medians from Task 1. A release check is now
`runClientGameTest` plus `build runFightGameTests`, which is what `publishPack` runs. For each
target that's missed, add one line under the table saying what's left and why.

- [ ] **Step 3: `AGENTS.md`**

1. In "Where things are", change the Tests bullet to:

```markdown
- **Tests:** `src/test/java/` (JUnit), `src/gametest/java/` (GameTests and client GameTests). The
  test mod's hooks into Minecraft's GameTest runner are in `gametest/mixin/`; they call
  `GameTestHarness`.
```

2. Replace the `## Build and test` table with this one. Fill in the medians from Step 2 in place of
   `…`.

```markdown
| Command | What | Time |
|---|---|---|
| `./gradlew test` | JUnit | seconds |
| `./gradlew build` | JUnit and the quick GameTests (every one without `@Fight`) | … |
| `./gradlew runGameTest` | every GameTest: the quick and the fights group on two servers side by side; look for "All N required tests passed" twice | … |
| `./gradlew runFightGameTests -Ptests=<id or pattern> -Prepeat=3` | one or a few fight tests, three times in one server | … |
| `./gradlew runClientGameTest` | a real client (needs a display; WSLg works); screenshots in `build/run/clientGameTest/screenshots/` | … |

When to run what:

| When | Run |
|---|---|
| after every change | `./gradlew build` |
| before committing a task that touches `boss/`, `game/` or `world/`, or a fight test | `./gradlew runGameTest` |
| a new or changed slow test | `./gradlew runFightGameTests -Ptests=<pattern> -Prepeat=3`, in place of three full runs |
| client code, a world (re)load, a load-contract row or the screenshots changed; before every release | `./gradlew runClientGameTest` |

`-Ptests` takes Minecraft's selector (`tafelboem-test:boss_fight_game_test_*`, with `*` and `?`)
and goes with the group's own task: a pattern that matches no test of the group fails the run.
Each run writes every test's time to `build/reports/gametest-<group>.xml`.
```

3. In "GameTest pitfalls":
   - Replace the "Keep `padding` small" bullet with:

```markdown
- `padding` is also a test's distance to the next one: Minecraft places each test's padded box 5
  blocks from the next, so a test that yeets Graaf Fout keeps its 48. Before a test the runner
  clears its whole padded box (a cube); the test mod skips the blocks that already are air or stone,
  which in the fresh test world is nearly all. It must be at most 128, or the client GameTests fail
  ("Timeout loading world").
```

   - Replace the "GameTests run in parallel in one shared level" bullet with:

```markdown
- GameTests run in parallel in one shared level, all of a server's tests in one batch: never change
  global state (game rules without restoring them, `ProtectedAreas.clear()`, static hooks).
  TafelBoem-world tests use `TestWorld`. Count only your own entities: `OwnChickens` for spectacle
  chickens.
- A test's `onEachTick` actions run in no fixed order (Minecraft keeps them in a hash map): put
  actions whose order matters, such as `sessions.tick()` and a count, in one `onEachTick`.
```

   - Replace the "Run a new slow GameTest (a whole fight) three times" bullet with:

```markdown
- A test that plays a fight for 3 s or more gets `@Fight`, so `build` stays quick. Run a new or
  changed one three times before trusting it (`runFightGameTests -Ptests=<its id> -Prepeat=3`): a
  1-in-20 flake hid in one for a whole release.
- The GameTest server never saves its world, and Gradle deletes that world before every run. A
  check that needs a saved world goes in `WorldClientGameTest`.
```

4. In "Releases", change "pushes the tag, builds and tests" to "pushes the tag, builds and runs
   every test".

- [ ] **Step 4: `README.md`**

Replace the code block under `## Building` with:

```bash
./gradlew build                # compile, unit tests, the quick GameTests
./gradlew test                 # unit tests only (core learning logic)
./gradlew runGameTest          # every GameTest: the quick and the fights group, side by side
./gradlew runFightGameTests    # only the GameTests that play a whole fight
./gradlew runClientGameTest    # plays through in a real client and takes screenshots
                               # (build/run/clientGameTest/screenshots; needs a display, e.g. WSLg)
```

- [ ] **Step 5: Check and commit**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`.

```bash
git add AGENTS.md README.md docs/evaluations/faster-tests.md
git commit -m "Write down when to run which tests, and what the faster tests gained

AGENTS.md gets the working rules (build after every change, runGameTest
before a fight-related commit, -Prepeat=3 for a new slow test, the client
test for client and load changes and before a release) and the new
pitfalls; the evaluation has the final numbers against the 1.1.0 baseline.

Co-Authored-By: <model> <noreply@anthropic.com>"
```

---

## After the plan

- The branch has no game code changes, so it merges into `main` without a publish (spec,
  *Shipping*). The next published build carries it. Merging and pushing need Jesper's go.
- The CI change is proved on the first push: the run should show `setup gradle` restoring a cache
  from the second run on, and both `All N required tests passed` lines.
