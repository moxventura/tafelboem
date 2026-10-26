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

## The client test

`./gradlew runClientGameTest` runs a real client in real time, so a fixed `waitTicks` costs its full
50 ms a tick. Every `waitTicks(n)` with n above 1 in the two client tests, from
`grep -n "waitTicks([0-9]*)" src/gametest/java/nl/jeeninga/tafelboem/gametest/*ClientGameTest.java | grep -v "waitTicks(1)"`,
is classed by what follows it:

- **(a)** the timing of a screenshot: the shot is taken a set time after something happened, and a
  condition would move the picture;
- **(b)** time to show that something does not happen: an absence check follows, and nothing
  signals that "nothing" has been going on long enough;
- **(c)** a settle before an action or a check that has a condition (a screen, a block, an entity, a
  pose). Only (c) can become `context.waitFor(…)`. Where a wait could go either way it is put in (c),
  so the (c) total is the generous one.

A wait in a helper counts once per call. `P` is `PlaythroughClientGameTest`, `W` is
`WorldClientGameTest`; "passes" is how often the line runs in one test run, counted from the
callers (a minimum where it depends on the run).

| Line | Ticks | Passes | Total | Class | Why |
|---|---|---|---|---|---|
| P:51 | 20 | 1 | 20 | c | settle after `time set day` and `weather clear`, before the first bomb |
| P:65 | 5 | 1 | 5 | a | after the screen closes, for the soot cloud shot |
| P:68 | 10 | 1 | 10 | a | after the correction shows, for its shot |
| P:80 | 15 | 1 | 15 | a | the Kippenbom's run shot |
| P:82 | 80 | 1 | 80 | a | the shot catches the Kippenbom's boom; "first chicken exists" is earlier than "chickens everywhere" |
| P:99 | 30 | 1 | 30 | a | Graaf Fout's arrival shot |
| P:110 | 20 | 1 | 20 | a | the boss question shot, once the camera assist has turned us |
| P:115 | 14 | 1 | 14 | a | the shot catches him mid-air after the kick |
| P:117 | 30 | 1 | 30 | a | the landing shot |
| P:122 | 10 | 1 | 10 | a | the tumble shot |
| P:124 | 4 | 1 | 4 | a | the later tumble shot |
| P:132 | 8 | 1 | 8 | a | the faceplant shot, after the poll for "face down" |
| P:141 | 2 | 1 | 2 | c | teleport reaches the client before `lookAt` |
| P:143 | 2 | 1 | 2 | a | camera settles for the side shot |
| P:157 | 10 | 5 | 50 | a | the pose shows for the landing shot (5 landings), after the poll for the landing |
| P:167 | 10 | 2 | 20 | a | the "-after" shot (head first, soot), after the poll for the end of the landing |
| P:177 | 4 | 1 | 4 | a | the splat settles for its shot, after the poll for the pose |
| P:186 | 2 | 1 | 2 | c | teleport reaches the client before `lookAt` |
| P:188 | 2 | 1 | 2 | a | camera settles for the splat shot |
| P:243 | 2 | 7 | 14 | c | `lookAtBoss`: teleport reaches the client before `lookAt` (5 landings + 2 "-after" shots) |
| P:245 | 2 | 7 | 14 | a | `lookAtBoss`: camera settles for the shot |
| P:259 | 2 | 8 | 16 | c | `closeQuestion`: the screen closes (7 calls from `lookAtBoss`, 1 at the splat) |
| P:275 | 20 | 1 | 20 | a | the counting shot is defined as 20 ticks after the last pop |
| P:293 | 4 | 1 | 4 | c | restored game mode and position reach the client |
| P:313 | 2 | 1 | 2 | c | teleport reaches the client before `lookAt` |
| P:315 | 2 | 1 | 2 | a | camera settles for the counting shot |
| P:377 | 3 | 1 | 3 | c | typed text enables OK, which the next line checks |
| P:384 | 2 | 1 | 2 | c | backspace is processed before the answer is typed |
| P:409 | 5 | 3 | 15 | c | `answer`: typed text enables OK before Enter (wrong Knalletje, Kippenbom, boss) |
| W:66 | 20 | 1 | 20 | c | settle after the world renders, before the server-side checks |
| W:73 | 5 | 1 | 5 | a | camera settles for the hub shot |
| W:83 | 10 | 3 | 30 | a | after Graaf Fout appears, for the arena shot (3 doors) |
| W:93 | 24 | 1 | 24 | a | the yeet shot after the answer |
| W:112 | 40 | 1 | 40 | b | then "no item of the last visit in arena 1" |
| W:132 | 5 | 1 | 5 | c | the server ticks the blocks and the chicken before the world closes |
| W:141 | 40 | 1 | 40 | b | then "still exactly 5 bomb frames after a reload, not 10" |
| W:150 | 5 | 1 | 5 | a | camera settles for the hub-after-reload shot |
| W:156 | 40 | 1 | 40 | b | then "no item of the last session in arena 1" |
| W:209 | 5 | 5 | 25 | c | `walkOut`: after the walk, before the loop asks if the kid is out (3 arenas, 1 re-entry, 1 after the reload; one attempt each) |
| W:216 | 5 | 1 | 5 | c | `answerQuestion`: typed text enables OK before Enter (level 2; more if `walkOut` meets a question) |
| W:228 | 5 | 10 | 50 | c | `walkInto`: teleport reaches the client before `lookAt` (3 + 1 + 1 entries, 5 `walkOut`s) |

Totals, in ticks (20 ticks = 1 s):

| Class | `P` | `W` | Together | Seconds |
|---|---|---|---|---|
| (a) screenshot timing | 340 | 64 | 404 | 20.2 |
| (b) showing that nothing happens | 0 | 120 | 120 | 6.0 |
| (c) has a condition | 80 | 105 | 185 | 9.3 |
| all | 420 | 289 | 709 | 35.5 |

The static sums in the brief (322 and 204) are the same lines counted once; the passes add 98 and 85.

One more fixed wait is not a `waitTicks`: `W:230`, `holdKeyFor(keyUp, 30)` in `walkInto`, 10 passes,
300 ticks. The door fires when the kid's box overlaps it (`LevelDoorBlock.entityInside`): he stands
2 blocks in front and walks 0.22 blocks a tick, so he is through in about 6 to 8 ticks and walks on
for the rest. For the 3 arena entries the walk-on sets where he stands in the arena shot (a). For
the other 7 (5 exits, 2 re-entries) it only costs time, and a release on the door's firing would save
at most about 22 ticks each, 150 in all. That needs a held key that the test lets go of by hand, not
`holdKeyFor`.

### Decision: not worth it

(c) adds up to 185 ticks (9.3 s), below the 300 ticks (15 s) the spec asks for. The ticks in (c) are
also not the saving: a `waitFor` still takes at least a tick, and (c) has 43 passes, so the most it
can save is 142 ticks. Add the 7 releasable key holds (150 ticks at most, and only with a hand-made
release) and the ceiling is about 290 ticks, 14.6 s: still under 15 s, and reachable only if every
condition held at the first tick it was asked, which a `waitFor` never does. The 404 + 120 ticks in (a)
and (b) have no condition to wait for: (a) times a picture, (b) shows an absence. Not worth it: the
client runs in real time, and its waits time screenshots or show that something doesn't happen; the
saving is in running it less often (the working rules). No test changed.
