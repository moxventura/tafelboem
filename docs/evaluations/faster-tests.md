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
