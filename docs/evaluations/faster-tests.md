# Faster tests: measurements

What each step of the spec's *Faster tests (detailed scope)* did to the time the tests take.

**How:** Jesper's PC (WSL2, 20 cores, 15 GB). The command runs once to warm the Gradle daemon, then
`tools/time-gradle.sh <task>` times three runs and the median counts. A step that saves no time is
taken out again. Each GameTest's own time is in `build/reports/gametest-*.xml`; the slowest:

    grep -o '<testcase[^>]*>' build/reports/gametest-all.xml | sed -E 's/.*name="([^"]*)".*time="([^"]*)".*/\2 \1/' | sort -rn | head

## Baseline: the 1.1.0 tests

| Run | Runs (s) | Median (s) |
|---|---|---|
| `./gradlew build` | 86.8, 52.4, 55.5 | 55.5 |
| `./gradlew runGameTest` | 67.7, 57.5, 50.0 | 57.5 |
| `./gradlew runClientGameTest` | 156.3, 135.7, 129.3 | 135.7 |
| one slow test three times: 3 × `runGameTest` | | 172.5 |
| a release check: 2 × `build`, 2 × `runGameTest`, `runClientGameTest` | | 361.7 |

## Steps

| Step | Run | Before (s) | After (s) | Kept |
|---|---|---|---|---|
| fresh world, never saved | `runGameTest` | 57.5 | 49.4 | yes |
| one batch | `runGameTest` | 49.4 | 41.7 | yes |
| two groups, two servers | `build` | 55.5 | 26.9 | yes |
| two groups, two servers | `runGameTest` | 41.7 | 44.3 | yes |
| two groups, two servers | one slow test three times (`-Prepeat=3`) | 172.5 | 14.4 | yes |
| skip clearing what is clear | `build` | 26.9 | 24.0 | yes |
| skip clearing what is clear | `runGameTest` | 44.3 | 42.6 | yes |
| runGameTest on one server | `runGameTest` | 43.1 | 38.5 | yes |

### Notes

- fresh world, never saved: Runs after this step: 49.4, 56.3, 47.5 s. The baseline ran 70 tests, this run 71 (one GameTest added
  since). The test world is now 660 KB instead of 448 MB. Run-to-run spread is about 9 s, as large as
  the gain, so the saving is only roughly measured.
- one batch: runs after this step: 40.9, 41.7, 43.0 s, spread 2 s. The 71 tests now start at once
  (`Running test environment 'minecraft:default' batch 0 (71 tests)`) instead of two batches, and
  all six runs (three to check, three timed) passed. The tests themselves now take about 13 s of
  each run, from "Running test environment" to "All 71 required tests passed".
- two groups, two servers: `build` runs only the quick group (53 tests): 26.4, 26.9, 27.4 s; its
  "before" is the baseline, as the steps before this one timed only `runGameTest`. `runGameTest`
  runs the quick group (53) and the fights group (18) on two servers that start within a second of
  each other: 44.3, 49.8, 40.4 s, so it saves nothing against one server with all 71 (41.7 s; the
  spread is 9 s). Started together, each server needs about 18 s from loading to "Started game test
  server" instead of 8 to 9 s alone, and the setup before the tests run takes longer too, so the
  two servers cost about what they save. The step stays for `build` and the repeat. One slow test
  three times (`-Ptests=…beating_graaf_fout_gives_the_kid_four_kippenbommen -Prepeat=3`, one
  server): 14.4, 15.0, 14.4 s; each copy takes about 2.4 s instead of 9.2 s in the baseline's full
  run. `build runFightGameTests`, `publishPack`'s own part of the release check: 39.2, 44.8,
  50.3 s, median 44.8 s.
- skip clearing what is clear: the GameTest world is Minecraft's flat preset (bedrock, 2 dirt, 1 grass: ground up to y -61),
  and a test's box starts at y -59, so everything in it is air except what the tests build; below the
  box's lowest row `clearBlock` wants stone, which is a few blocks of the padding at most. The set-up
  (from "tests are now running at position" to "Running test environment", `build/task6.log` against
  `build/task7-1.log` to `-3.log`) took 15 s (quick group) and 7 s (fights group) before and 8, 7, 8 s
  and 4, 4, 4 s after. Runs after this step: `build` 20.6, 24.0, 24.5 s; `runGameTest` 36.8, 42.6,
  48.9 s (the spread, 12 s, is larger than the saving, so the medians drop only by what the set-up
  gap says: about 7 s on one server and 3 s on the other, partly hidden when two servers start
  together). All three checking runs of `runGameTest` passed (53 and 18 tests). Alone, on one
  server: `runQuickGameTests` 27.1, 29.0, 26.4 s, median 27.1 s; `runFightGameTests` 31.6, 35.5,
  22.1 s, median 31.6 s (the slow runs follow a task that had just run; the spread is 13 s). These
  alone-runs include Gradle start-up and noise, so they don't settle one server against two; the
  next step does.
- runGameTest on one server: `runGameTest` ran the quick and the fights group on two servers side
  by side. Timed again as it was (five runs, the daemon warm): 29.8, 33.9, 43.1, 48.3, 52.2 s,
  median 43.1 s. Then `runGameTest` became a third Loom run, `gameTest`, with every test on one
  server (no `tafelboem.gametest.group`, so `keepChosenGroup` returns the batches unchanged; its
  own run folder `build/run/gameTest-all`, report `build/reports/gametest-all.xml`). One run showed
  one server, no group line and `All 71 required tests passed`; `runQuickGameTests` still ran 53.
  Five timed runs: 33.0, 36.5, 38.5, 42.3, 47.9 s, median 38.5 s, all passing 71 tests. The
  median is 4.6 s lower, with a spread of 15 to 22 s, so one server is no worse and likely
  somewhat quicker; kept. `check` stays on `runQuickGameTests`; `publishPack` and CI stay on
  `build runFightGameTests`.
