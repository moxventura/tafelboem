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
