# Working on TafelBoem

TafelBoem is a Fabric mod (Minecraft 26.2, Java 25) that teaches Jesper's kids the times tables 1–10
with TNT. They play on Windows PCs in a Prism Launcher instance that updates itself, often two PCs
over LAN. The README has the player and builder side; this file is for anyone changing the mod,
human or agent.

## Where things are

- **Spec (the authority):** `docs/superpowers/specs/2026-10-07-tafelboem-design.md`, one detailed
  scope section per release. **Plans:** `docs/superpowers/plans/`. **Evaluations:**
  `docs/evaluations/R<x>.md`, with playtest findings numbered P1, P2, … across releases.
- **Code:** `src/main/java/nl/jeeninga/tafelboem/`
  - `core/`: pure Java rules (levels, picker, stars, shapes, stats). It imports nothing from
    `net.minecraft`, `com.mojang` or `net.fabricmc`; `CoreArchitectureTest` fails otherwise. Put
    logic here when you can, and test it with JUnit.
  - Minecraft side: `boss/` (Graaf Fout and his fight; one class per flight in `boss/yeet/` and per
    landing in `boss/landing/`), `game/` (questions, bombs, explosions), `world/` (the TafelBoem
    world: hub, arenas, doors), `block/`, `item/`, `registry/`, `command/`, `log/` (answer and
    fights logs), `config/`; client code in `src/client/java/`.
- **Tests:** `src/test/java/` (JUnit), `src/gametest/java/` (GameTests and client GameTests). The
  test mod's hooks into Minecraft's GameTest runner are in `gametest/mixin/`; they call
  `GameTestHarness`.

## Rules for the game

- The villain never states a wrong fact and never comments on a kid's ability.
- No timers in normal play. A wrong answer is always shorter and less fun than a right one.
- Our booms never hurt players (`PlayerSafeExplosion`); a correct answer never hurts the kid.
- A typed wrong answer costs hearts on Gemiddeld and Moeilijk (`FightLevel.wrongAnswerDamage`), never
  the last half heart; "?" and ESC never do.
- A boost only ever adds: a fast answer or a streak yeets Graaf Fout further, a slow answer still
  gets a full yeet. Only a typed wrong answer ends a streak.
- No ranking between siblings: stars, the boost's reasons and records show only to the kid they
  belong to.
- Vanilla look: vanilla blocks, widgets and menu conventions, no new colours on screens.
- Every user-facing string goes in both `en_us.json` and `nl_nl.json` with the same arguments
  (`LanguageFilesTest`). Game terms stay Dutch in code: Knalletje, Gewone TNT, Kippenbom, Graaf Fout.
- Every new feature gets a switch in `config/tafelboem.json` (`TafelBoemConfig`), default on.
- Outside a TafelBoem world, R0.1 behaviour stays, unless a change says otherwise.
- Work in small releases, each one played by the kids and evaluated before the next. Don't build
  backlog features ahead of an evaluation.

## The load contract

Most late R1 bugs sat where Minecraft's save and the mod's rebuild meet: a world load, a chunk that
loads again, a kid walking into an arena. Every rule for those moments is in this table, with the
test that checks it on a real reload. A change that rebuilds or throws away something new adds a row
and a check. Put new world work on arena entry rather than on world load.

| When | What | Rule | Checked by |
|---|---|---|---|
| world load | hub blocks | placed again; the kids' builds beside it stay | `WorldClientGameTest` |
| world load | hub frames | exactly 5; frames from disk go (`Hub.isLeftOverFrame`) | `WorldClientGameTest` |
| world load | spawn | in the hub | `WorldClientGameTest` |
| world load | arena slots | all blocks cleared | `HubAndArenasGameTest` |
| chunk or world load | anything but a player in an arena slot | loaded from disk = a leftover; it goes | `WorldClientGameTest` |
| chunk or world load | spectacle animals, anywhere | loaded from disk, they go | `WorldClientGameTest` |
| arena entry | the arena | cleared where the last one stood, placed fresh; plain arenas have obsidian under the walls | `HubAndArenasGameTest`, `ArenaShapeTest` |
| arena entry | arrival | 3 blocks from the exit door, inside the arena | `HubAndArenasGameTest` |
| arena entry | music | the slot gets its level's biome (`ArenaBiomes`) | `WorldClientGameTest` |
| login | a kid inside an arena without a fight | to the hub | `TafelBoemWorldGameTest` |
| any time | the void | everyone who falls is caught, never fatally | `TafelBoemWorldGameTest` |

Spectacle animals that appear after the boom (the counting Kippenbom's groups, the chase) get their
poof timer from `SessionManager.track` when they spawn; `collectSpectacles` only finds what is
there at the boom.

## Code style

Tabs. Javadoc on every class; comments only where the why isn't obvious. Match the surrounding
code. Minecraft 26.2 is unobfuscated (`Identifier`, `EntityTypes`, `ServerLevel.setRespawnData`);
when unsure of a signature, check the jar in the Loom cache:
`javap -cp .gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-*/26.2/*.jar <class>`.

## Build and test

| Command | What | Time |
|---|---|---|
| `./gradlew test` | JUnit | seconds |
| `./gradlew build` | JUnit and the quick GameTests (every one without `@Fight`) | ~22 s |
| `./gradlew runGameTest` | every GameTest, on one server; look for "All N required tests passed" | ~36 s |
| `./gradlew runFightGameTests -Ptests=<id or pattern> -Prepeat=3` | one or a few fight tests, three times in one server | ~21 s |
| `./gradlew runClientGameTest` | a real client (needs a display; WSLg works); screenshots in `build/run/clientGameTest/screenshots/` | ~2 min 15 s |

When to run what:

| When | Run |
|---|---|
| after every change | `./gradlew build` |
| before committing a task that touches `boss/`, `game/` or `world/`, or a fight test | `./gradlew runGameTest` |
| a new or changed slow test | `./gradlew runFightGameTests -Ptests=<pattern> -Prepeat=3`, in place of three full runs |
| client code, a world (re)load, a load-contract row or the screenshots changed; before every release | `./gradlew runClientGameTest` |

`-Ptests` takes Minecraft's selector (`tafelboem-test:boss_fight_game_test_*`, with `*` and `?`).
It works with `runGameTest` and with a group's own task (`runQuickGameTests`, `runFightGameTests`).
A pattern that matches no test fails the run: with a group task the harness fails it (no test of
that group is selected), with `runGameTest` Minecraft itself does. `-Prepeat=<n>` needs `-Ptests`.
Each run writes every test's time to `build/reports/gametest-<group>.xml` (`gametest-all.xml` for
`runGameTest`).

GameTest pitfalls:
- `padding` is also a test's distance to the next one: Minecraft places each test's padded box 5
  blocks from the next, so a test that yeets Graaf Fout keeps its 48. Before a test the runner
  clears its whole padded box (a cube); the test mod skips the blocks that already are air or stone,
  which in the fresh test world is nearly all. It must be at most 128, or the client GameTests fail
  ("Timeout loading world").
- An entity spawned in a chunk that isn't loaded yet can't be found; when a test spawns something
  outside its own chunks at tick 0, look for it a few ticks later (`TestWorld.whenLoaded`).
- GameTests run in parallel in one shared level, all of a server's tests in one batch: never change
  global state (game rules without restoring them, `ProtectedAreas.clear()`, static hooks).
  TafelBoem-world tests use `TestWorld`. Count only your own entities: `OwnChickens` for spectacle
  chickens.
- A test's `onEachTick` actions run in no fixed order (Minecraft keeps them in a hash map): put
  actions whose order matters, such as `sessions.tick()` and a count, in one `onEachTick`.
- Mock players from `makeMockServerPlayerInLevel()` are in creative; use
  `makeMockServerPlayer(GameType.…)` for another game mode.
- Mock players keep no chunks simulated: an entity that leaves the chunks the test has tickets for
  freezes mid-air. `TestWorld` and `OpenField` keep the chunks around Graaf Fout ticking.
- A test that plays a fight for 3 s or more gets `@Fight`, so `build` stays quick. Run a new or
  changed one three times before trusting it (`runFightGameTests -Ptests=<its id> -Prepeat=3`): a
  1-in-20 flake hid in one for a whole release.
- The GameTest server never saves its world, and Gradle deletes that world before every run. A
  check that needs a saved world goes in `WorldClientGameTest`.
- GameTests can't load a world twice. Check anything that happens on a second load (blocks placed
  again, entities loaded from disk) in `WorldClientGameTest`, which closes its world and opens it
  again (`getWorldSave().open()`).
- The test JVM writes a `logs/` folder at the repo root; it is ignored, don't commit it.

## Git

- Subject: a sentence saying what changes for the kids or the parent, in the imperative; the body
  says why. End with a `Co-Authored-By:` trailer naming the model that wrote the commit.
- Stage only the paths you changed; never a bare `git add -A`.
- Don't push, tag or publish without Jesper's go.
- Bigger work goes on a branch in a worktree under `.worktrees/` (ignored).

## Releases

- Versions come from git tags `vX.Y.Z`: `X.Y` is the release (R0.1 → 0.1, R1 → 1.0, R1.1 → 1.1),
  `Z` counts its published builds. A pushed tag is never moved or reused; a mistake goes out as the
  next `Z`.
- `git tag vX.Y.Z && ./gradlew publishPack`. It refuses an unclean or untagged commit, pushes the
  tag, builds and runs JUnit and every headless GameTest (not the client GameTest: run
  `./gradlew runClientGameTest` before tagging), publishes to `../tafelboem-pack` (GitHub
  `moxventura/tafelboem-pack`), and waits until GitHub serves it ("Live"). From a worktree, pass
  `-PpackDir=/home/jesper/claude-mc/tafelboem-pack`.
- Jesper tests a release by publishing it and pressing Play in the Prism instance; the kids' PCs
  update on their next Play too. Keep `main` equal to what is published.

## Playtests

- Jesper shares the game's `latest.log`, sometimes as a `https://logshare.in/#<room>` link. That is a
  live editor room, not a static page: read it over socket.io polling
  (`/socket.io/?EIO=4&transport=polling`: handshake, `40`, then
  `42["join-room",{"roomId":"<room>","token":null}]`; the log is the `code` field of the
  `room-data` event). Only read, never send `code-change`.
- From 1.0.1 on, the host PC's `latest.log` has a `TafelBoem fight:` line per fight and a
  `TafelBoem session:` line per kid, with the evaluation's numbers.
- From 1.1.0 on, it also has a `TafelBoem config:` line per world (the switches), a
  `TafelBoem yeet:` line per kick (answer time, boost, flight, landing, record), a
  `TafelBoem chickens:` line per counting Kippenbom and, if he ever froze mid-yeet and the fight
  brought him back, a `TafelBoem stuck:` line.
- Record each playtest in `docs/evaluations/R<x>.md`: findings with their cause checked in the
  code, then the go/no-go and backlog changes.
