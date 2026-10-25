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

### Notes

- fresh world, never saved: Runs after this step: 49.4, 56.3, 47.5 s. The baseline ran 70 tests, this run 71 (one GameTest added
  since). The test world is now 660 KB instead of 448 MB. Run-to-run spread is about 9 s, as large as
  the gain, so the saving is only roughly measured.
- one batch: runs after this step: 40.9, 41.7, 43.0 s, spread 2 s. The 71 tests now start at once
  (`Running test environment 'minecraft:default' batch 0 (71 tests)`) instead of two batches, and
  all six runs (three to check, three timed) passed. The tests themselves now take about 13 s of
  each run, from "Running test environment" to "All 71 required tests passed".
