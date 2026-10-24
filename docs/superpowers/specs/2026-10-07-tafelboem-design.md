# TafelBoem — a Minecraft mod for practising times tables with TNT

## Context

Jesper wants a Minecraft Java Edition mod that helps his kids learn the multiplication tables
(1–10) playfully, rewarded by increasingly ridiculous TNT. `/home/jesper/claude-mc` is empty (not a
git repo). The project will live in its own subfolder, `/home/jesper/claude-mc/tafelboem/`, as a
git repo pushed to a **private GitHub repo `tafelboem`**. Minecraft Java up to 26.2 is installed on Windows (Microsoft Store launcher,
`C:\Users\jeejes\AppData\Roaming\.minecraft`, no mod loader). WSL is Debian 13 with no JDK.

The design was shaped with Jesper and then reviewed from three angles: a kid's playthrough, a
teacher's view and gamification theory. It was revised after R0 playtest 1 on 2026-10-11 and after
R0.1 playtest 2 on 2026-10-14; see [`docs/evaluations/`](../../evaluations/). Decisions:
- **Delivered in small releases, each evaluated with the kids.**
  - **R0 POC** is the core question loop plus a simple Graaf Fout who gets **yeeted on every
    hit**, played over LAN on the woonkamer PC and the other PCs.
  - The rest of the design is a backlog, re-prioritised after every evaluation.
- **Fabric on Minecraft 26.2.** UI follows the game language (nl_nl + en_us).
- **"Bomb in your lap" loop.** Every fuse is a sum and you are rooted in place while answering.
  Right means you strike back or escape. Wrong means the bomb goes off on you, you're free again,
  and the correct fact shows big on your HUD while the game goes on.
- **Premade levels and a hub.** Arenas are premade (some built by the kids themselves) and placed
  in a void world. A hub with a door per level is where you pick a level. This replaces the
  generated arena island; the kids disliked the creative superflat world.
- **Three bosses, each with 9 levels, played in order:**
  1. **Graaf Fout** (Count Wrong), who builds accuracy.
  2. **Tante Toverfout**, who builds flexibility with missing-factor questions.
  3. **Robo-Rekenaar 3000**, who builds fluency by letting the kid race the robot.

  Each boss has its own skin, story and arena theme. That makes about 450 questions over 4–8 weeks.
- **Every hit on a boss triggers a hilarious reaction:** yeeted away, frantic running, pancaked,
  stuck head-first in the ground, and so on.
- **9 bombs, each with its own look, earned as loot.** Questions come from the *child's own
  learning progress* within the level's band. The bomb sets the show and the stakes.
- **Go wild + spectacle.** Real big craters plus silly effects, plus elaborate particles on every
  bomb. The arena repairs itself, and a hard performance cap applies.
- **Penalties escalate with tier.** Wrong answers are harmless, then hurt, then cause a silly
  death at tiers 7–9. **3 lives per fight.** Items are always kept. A wrong answer is always
  shorter and less fun than a right one.
- **No timer in normal play.** A **hardcore mode** with "fuse burns while thinking" unlocks
  after beating the final boss (Robo-Rekenaar 3000, level 9).
- **Daily dose.** The bosses go to sleep after about 15 min or 60 fight questions per day.
  Free TNT play and the practice field stay open. There are no guilt messages and no streaks that
  break.

## Delivery approach: small releases, each evaluated with the kids

A QA review of the first version of this plan found these problems:
- the first value for the kids only arrived after about two-thirds of the build
- the riskiest assumptions were untested (a question pop-up is fun, the boom motivates, a villain
  adds pull, kids wander off)
- there were no acceptance criteria for fun or learning
- the whole scope was fixed before anyone had played

So the game design below is the **product vision and backlog**, not a commitment. We ship in
releases. Each release is playable by the kids, tests a hypothesis, and ends at a go/no-go.

**Customers.**
- The **kids** want fun. Measured by: they ask to play again, answers per session, what you
  observe.
- **You, as parent**, want learning. Measured by: accuracy and median time per fact in the answer
  log, and later % Fluent.

**Kids' setup.** They play on the **woonkamer PC and other PCs together over LAN**. So
multiplayer sessions and a one-step deploy to every PC are part of R0.

| Release | The kids get | Hypothesis | Go/no-go evidence |
|---|---|---|---|
| **R0 POC "Graaf Fout komt!"** (~1 week) | see the R0 scope below | The rooted-question loop with a yeetable villain is fun, and an 8-year-old can use the question screen | see the R0 evaluation below |
| **R0.1 "Beter mikken"** (patch) | see the R0.1 scope below | With the playtest-1 blockers fixed, the kids see every consequence of their answers, "?" is only pressed on purpose, and a correction on the HUD sticks as well as the retype did | see the R0.1 evaluation below |
| **R1 "Kies je level"** | see the R1 scope below: the TafelBoem world with a hub and a door per level; levels 1–3 (easy, medium, hard) with their own tables, bombs and hits; arenas built from code or from structure templates, picked at random per level; stars | With a world of their own, a hub and three levels to choose from, the kids keep asking to play, and over time they pick harder levels and collect stars | see the R1 evaluation below |
| **R1.1 "Meer yeets"** | see the R1.1 scope below: a flight plus a landing per hit (wall splat, head-first, pancake, frantic running, bomb signatures), the full yeet-o-meter (fast answers and streaks yeet further, a personal best), the counting Kippenbom; the kids' own arenas as they come | More variety keeps them coming back | see the R1.1 evaluation below |
| *Faster tests, right after R1.1* | nothing new: the same tests, quicker. At 1.1.0 a `./gradlew build` takes about 1 min (JUnit and 68 GameTests), each extra `runGameTest` 1¼–1½ min and `runClientGameTest` 2½ min, and a slow fight test is run three times before it is trusted. Also make tests that count entities count only their own: the GameTest grid puts tests a few blocks apart in one level, so the exact chicken counts (`ChickenGameTest`) only stay apart by luck | A suite that is quicker to run keeps releases small and cheap to check | time per `build`, `runGameTest` and `runClientGameTest` before and after; no test loses what it proves |
| R1.2 *(after R1.1)* | experiments behind toggles: dodgeable bombs and "Graaf Fout eats your mistake" | Dodging and healing don't cut answers per minute or add frustration | answers per minute vs R1, frustration tally |
| **R2 "Leren dat blijft"** | fact states and spaced picker, a Tafelkaart screen (GUI), parent report, the daily limit, "Kies je bom", the full leg kick in third person | The practice spreads out and facts move toward Fluent | 2 weeks of logs: % Accurate/Fluent up, median time down, sessions spread over days, kids still asking |
| **R3 "Meer boem"** | bombs 4–9, levels 4–9, particle choreographies, craters, escape aids, lethal tiers with 3 lives, inventory stash, signature reactions for bombs 4–9, co-op rescue sums, the lobby extras in the hub (Tafelkaart wall, trophy wall, animal pen) | Escalation keeps them climbing without frustration | level reached per week, favourite bombs, no tears at levels 7–9, FPS OK on the woonkamer PC |
| R4 new villains *(when they ask for it)* | Tante Toverfout, then Robo-Rekenaar 3000 | Pulled by the kids beating Graaf Fout | |
| R5 endgame | endless mode, hardcore, golden bomb, polish | | |
| *Infra, after R2, only if needed* | a Fabric dedicated server on the NAS: one world and one answer log for every PC | Only if hosting on the woonkamer PC or logs spread over PCs become a problem | |

The learning model moved from R1 to R2 after playtest 1, because the kids' feedback was all about
agency and variety. No data is lost by this: every answer has been logged since R0, so the R2 model
can be replayed and tuned on real history.

**Evaluation after every release** (about 30 minutes):
1. Watch a play session without helping. Tally confusion, laughter and frustration moments.
2. Ask each kid 3 questions: *Wat was het leukst? Wat was stom of moeilijk? Wat wil je erbij?*
3. Review the answer log (`/tafelboem stats` plus the JSONL file).
4. Re-prioritise the backlog. The kids vote on the next bomb or villain.

**Definition of done for each release:**
- `./gradlew build` is green: core JUnit tests plus a GameTest smoke test of the session engine.
- The manual checklist for that release is done.
- nl and en are complete.
- It is published with `./gradlew publishPack`, and every kids' PC picks it up on the next Play.
- Every new feature has a config toggle, so we can switch it off quickly if the kids dislike it.
- Every publish is a clean commit tagged `vX.Y.Z`. `X.Y` is the release (R0.1 → 0.1, R1 → 1.0)
  and `Z` counts its published builds. The evaluation names the version that passed (decided
  2026-10-12).

**Testing strategy.**
- Heavy unit tests go on what is stable: the learning logic and the fight maths.
- GameTests cover only the session engine and the critical flows.
- Fun is checked by exploratory play, not automated.
- We don't write GameTests for features that may be cut after an evaluation.

## R0 POC: "Graaf Fout komt!" (detailed scope)

**In scope:**
- **Bombs.** 3 placeable bombs: Knalletje, Gewone TNT and Kippenbom.
  - They use simple textures (recoloured TNT plus a symbol) and simple vanilla-particle bursts.
  - Correct: the boom, with generous escape time.
  - Wrong: a harmless short penalty (soot, egg splat, water bucket).
- **Question loop.**
  - Right-click (or a thrown bomb lands) → rooted → question screen.
  - The screen has big digits, number keys plus Enter, a clickable number pad and the "?" button.
  - After a wrong answer, "?" or ESC: the correct fact appears big, the kid **types it once**, a
    derived-fact tip is shown, and the fact is **asked again within 1–2 questions**.
  - ESC: a chicken pops out, the bomb fizzles, and the fact counts as shaky. At most 3 per fight.
- **Simple picker.** Tables 1–10, weighted toward missed and slow facts. Commuted pairs are linked.
  The full fact-state model comes in R1.
- **Graaf Fout, basic.**
  - **Look:** the vanilla illager model with a simple custom texture (purple cape, red ✗).
  - **Summoning:** a spawn item, "Roep Graaf Fout op!".
  - **Fight:** he hovers and teleports and throws Knalletjes and Gewone TNT at players **in turn**.
    A correct answer kicks the bomb back for 1 hit.
  - **Every hit = a YEET:** he's launched up and away with a spin, flailing, a squeaky sound, and
    a dust ring on landing. It's done with motion plus a server-side spin, so no custom render
    poses are needed. He doesn't throw during it.
  - **Rest:** a boss bar, 1 level of 11 hits, and a defeat finale (one big yeet into the sky plus
    fireworks).
  - **Reward:** **4 Kippenbommen** for each player, as the first taste of loot.
  - **Taunts:** never a wrong fact, never about the kid's ability.
- **LAN.** Each player gets questions from their own progress and has their own log. The boss
  bar is shared. Booms never hurt other players.
- **Answer log from day 1.** One JSONL line per answer, stored in
  `<world>/tafelboem/answers-<player>.jsonl`:

  ```
  {ts, player, a, b, format, answer, correct, ms, dontKnow, chickenOut, bombTier, context: fight|free}
  ```

  `/tafelboem stats [player]` gives a simple summary: accuracy and median time per table, plus the
  5 hardest facts. Everything stays local; nothing is sent over the network.
- **Language.** nl_nl and en_us from the start.
- **Version check.** On join, the server compares the mod version with the client's. A mismatch
  shows a friendly "Update TafelBoem op deze PC" message. Since 0.1.3 the version comes from git
  (`0.1.3` on a tag, `0.1.3+2.g1a2b3c4` otherwise), so even builds one commit apart differ.

**Out of R0 (backlog):**
- bombs 4–9, craters, choreographies
- the world type and arena
- lethal penalties and lives
- spacing across days, the daily limit, the Tafelkaart
- other reactions, other bosses

**Where to play R0.** In a fresh **superflat** world that you create, opened to LAN. The kids
don't need a custom world type to test the loop.

**Deploy to the kids' PCs** (revised 2026-10-12; replaces the never-built NAS share and `.bat`):
- **Prism Launcher plus packwiz.** Every PC plays in a Prism instance `TafelBoem` (26.2 + Fabric
  0.19.5). Its pre-launch command runs packwiz-installer, which reads a public pack repo,
  [`moxventura/tafelboem-pack`](https://github.com/moxventura/tafelboem-pack), and updates the mods
  before every Play. Only the changed files are downloaded, and every file is hash-checked.
- **Versions.** The version comes from `git describe`. `publishPack` only publishes a clean commit
  tagged `vX.Y.Z`, and pushes that tag first, so every version on the PCs can be rebuilt.
- **Publishing.** `./gradlew publishPack` builds and tests first. It then puts our jar and Fabric API
  in the pack under fixed names (`mods/tafelboem.jar`, `mods/fabric-api.jar`), writes the packwiz
  `index.toml` and `pack.toml`, commits and pushes. Finally it waits until GitHub serves every file
  and prints "Live after N s".
- **Wait for "Live".** GitHub's raw CDN caches each file for up to 5 minutes, separately. Measured on
  2026-10-12: 263 s. Pressing Play earlier can meet a mix of old and new files. packwiz then refuses
  on the hash check and leaves the installed version untouched, so it is harmless.
- **One-time setup per PC.** Install Prism and log in, then Add Instance → Import with the URL of
  `TafelBoem.zip` from the pack repo.
- **Rules.** Never copy jars into the instance by hand. Offline, choose "Continue without updating"
  in the packwiz window. A Minecraft or Fabric version bump takes two Plays.
- The woonkamer PC can be woken with `ssh nas "~/wake_pc.sh"`.

**R0 acceptance criteria** (checked manually on this PC plus the woonkamer PC over LAN):
1. **Summoning.** Using the summon item in a superflat world spawns Graaf Fout, and a boss bar
   appears for every player within 32 blocks.
2. **Throws.** He throws at players in turn. The bomb lands within 2 blocks of the target, and
   that player is rooted with the question screen open within 0.5 s.
3. **Correct answer.** The bomb flies back and hits him. He is yeeted (spin, launch, squeak, dust
   ring), and the boss bar drops by one. He doesn't throw during the reaction.
4. **Wrong answer.**
   - A harmless penalty plays.
   - The correct fact appears big and must be retyped.
   - A tip is shown.
   - The same fact is asked again within 2 questions.
5. **"?" and ESC.**
   - "?" causes no penalty, shows the fact and the tip, and asks for the retype.
   - ESC pops out a chicken and fizzles the bomb. The 4th ESC in a fight counts as "?".
6. **Win.** After 11 hits: the defeat finale, then 4 Kippenbommen for each player.
7. **Free play.** Placed bombs work outside a fight: rooted, question, escape time, boom.
8. **LAN.** Two players on two PCs each get their own questions, and each gets their own log file.
   A boom never damages the other player.
9. **Language.** Switching the game language to Nederlands and back to English shows every string
   correctly.
10. **Version mismatch.** An outdated client sees the update message.
11. **Performance.** No noticeable lag on the woonkamer PC during a fight.

**R0 evaluation (go/no-go):**
- **Go** when all of these hold:
  - both kids ask to play again within 2 days, without being prompted
  - each does at least 30 answers in a 20-minute session
  - no UX blockers (they can read and answer without help)
  - the log is useful to you
- **Pivot** if the question pop-up feels like an interruption ("stom!"). Then shorten or reshape the
  loop before building more.
- **Record:** answers per session, accuracy, median time, "?" and ESC counts, the laugh tally, and
  the kids' 3 answers. Store them in `docs/evaluations/R0.md`.

## R0.1 "Beter mikken" (detailed scope)

A patch on R0 that fixes what playtest 1 found (P-numbers refer to `docs/evaluations/R0.md`).

**In scope:**
- **Question screen in vanilla style (P2).** No new colours, only vanilla buttons:
  - OK is a wide button under the pad, where Minecraft puts its "Klaar" button. It is greyed out
    until a digit is typed, and then gets the focus outline that shows Enter will press it.
  - Keys grow from 20 to 24 px, and 0 is double-width like a real numpad.
  - "? Weet ik niet" moves to the top-right corner of the panel, as far from OK as possible.
- **Correction on the HUD instead of a retype screen (P3).**
  - After a wrong answer, "?" or ESC, the question screen closes and the kid is free to move.
  - The penalty, fizzle or chicken plays where the kid can see it.
  - Then the correct fact shows big on the HUD for about 3 s, with the tip and "8 × 7 is
    dezelfde!" under it.
  - The fact is re-asked after one other question, as before. That re-ask is the real check.
  - The retype screen stays behind the config toggle `correctionRetype` (default off).
- **Distinct bombs (P7).**
  - Knalletje gets its own yellow-orange firecracker texture; Gewone TNT stays red with a white
    band and a black ×.
  - A thrown bomb trails particles in its own colour, and Graaf Fout shouts its name.
  - A kicked bomb's blast and the yeet's power grow with the bomb.
- **Camera assist (P8).** While a fight question is open, the view turns smoothly to Graaf Fout.
  After a correct answer it keeps following him until he has landed and lies dizzy, then lets go
  (at most 7 s, in case the kick never arrives). Config toggle `cameraAssist`.
- **More yeets (P1).** Besides the classic yeet:
  - a bounce yeet: three bounces with a boing, each one lower and shorter, so even the strongest
    Gewone TNT lands about 18 blocks away instead of 30+;
  - a helicopter yeet: spins up slowly, then drops; a bigger bomb sends him higher (about 6 blocks
    for a Knalletje, up to 12 for a Gewone TNT);
  - a boomerang yeet: flies off, comes back and falls flat on his face just in front of the kid,
    lying there dizzy with the stars at his head;
  - a tumble yeet: rolls and flips head over heels while he spins (yeet review, 2026-10-11).
  Every yeet spins, and the same one never comes twice in a row.
- **Yeet-o-meter, simple version.** When he lands, everyone in the fight sees "Graaf Fout vloog
  23 blokken!", or for the helicopter "Graaf Fout vloog 12 blokken hoog!".
- **Kick, simple version.** When a correct answer kicks the bomb back, the kid's arm swings the
  vanilla way, with a "whoosh" sound and a small camera bump. No custom animation.
- **Easy updates (playtest prep).** Prism Launcher on both PCs with a packwiz updater that installs
  the newest published version on every Play; `./gradlew publishPack` publishes (see "Deploy to the
  kids' PCs").
- **World setup (P11).** `/tafelboem arena` makes the world fit for playing: always day, no
  weather, no mob spawning, keep inventory, and it removes hostile mobs nearby. It also lays a round
  arena floor around the player.
- **Measurement.**
  - Each answer records how it was given (Enter or the OK button), and each "?" records how many
    digits were already typed. A "?" with digits typed counts as a likely mis-click.
  - `/tafelboem stats` adds re-ask accuracy (how often a missed fact is right when it comes back)
    and the likely mis-clicks.
  - Re-ask accuracy is computed from the answer order, so it also works on the R0 logs.

**R0.1 acceptance criteria** (manual, on both play PCs, the woonkamer PC and a second PC, over LAN):
1. All R0 criteria still hold, except the retype (now off by default).
2. OK is greyed out with no input, and a kid can press it without hitting "?".
3. After a wrong answer, the kid sees the penalty, then the fact on the HUD, and can move straight
   away.
4. Knalletje and Gewone TNT can be told apart in flight and when kicked.
5. During a fight question the view turns to Graaf Fout, and after a correct answer the view
   follows him until he has landed.
6. At least three different yeets show up in one fight, never the same one twice in a row.
7. A correct answer in a fight gives an arm swing, a whoosh and a camera bump as the bomb leaves.
8. `/tafelboem arena` stops day/night and mob spawning in an existing world.
9. `/tafelboem stats` shows re-ask accuracy and likely mis-clicks.
10. After `./gradlew publishPack` reports "Live", pressing Play installs the new version on both PCs
    without copying any file; offline, "Continue without updating" starts the installed version.

**R0.1 evaluation (playtest 2).** Record it in `docs/evaluations/R0.1.md`:
- the R0 numbers that were missed: answers per 20 minutes, % correct, median time
- re-ask accuracy without the retype; keep the HUD correction when it is about 80% or higher
- likely mis-clicks: aim for none
- did they see the penalty and the yeet? did they notice the different bombs?
- record the session on video, so laughs can be counted afterwards

## R1 "Kies je level" (detailed scope)

Decided with Jesper on 2026-10-14, after R0.1 playtest 2 (P-numbers refer to
`docs/evaluations/R0.1.md`). R1 combines the level choice with the TafelBoem world, its hub and its
arenas, because that is what the kids asked for: an easy way to choose the difficulty (P4) and a
"server-style" world instead of creative superflat (P11). The yeet catalogue, the full yeet-o-meter
and the counting Kippenbom moved to R1.1; the two toggle experiments to R1.2.

**Hypothesis:** with a world of their own, a hub to come back to and three levels to choose from,
the kids keep asking to play, and over time they pick harder levels and collect stars.

### The TafelBoem world
- **World type.** "Create New World → World Type: TafelBoem". It is a void world with the void biome
  (no mobs): a `world_preset` data file plus an entry in the `minecraft:normal` world-preset tag, so
  it shows on the World Type button.
- **First load.** The mod places the hub at spawn, makes the hub the world spawn, and sets the
  world rules of `/tafelboem arena`: always day, no weather, no mob spawning, keep inventory.
- **Game mode** is not forced, so the kids can also build there in creative.
- **No toggle needed.** The world type is a choice at world creation; other worlds play as in R0.1.
- **Void safety.** A player who falls below the hub or an arena is teleported back without damage:
  in a fight to that arena's arrival spot, otherwise to the hub. A player who logs in inside an
  arena without a running fight goes to the hub.
- **Other worlds keep working as in R0.1.** The summon item still starts the R0.1 fight there (all
  tables, Knalletjes with every 3rd a Gewone TNT, 11 hits), and `/tafelboem arena` stays.

### The hub
- **Built from code**, unless a structure template `tafelboem:hub` exists (see "Hand-built hub and
  arenas"). It is placed again every time the world loads, so it repairs itself.
- **Default layout:** a round stone platform with the spawn on the south side, facing three doors
  on the north side:

  ```
                          N
      ┌─────────┐   ┌─────────┐   ┌─────────┐
      │1 Makkel.│   │2 Gemidd.│   │3 Moeil. │   sign above each door
      │  ▒▒▒▒▒  │   │  ▒▒▒▒▒  │   │  ▒▒▒▒▒  │   ▒ = walk-through level door
     [K][G]      [G][C]          [C]            fixed item frames: that level's bombs
         .   .   .   .   .   .   .   .   .
       .                                   .
      .           stone platform            .
       .                                   .
         .   .   .   .   ☺   .   .   .   .     spawn, facing the doors
                          S
  ```

- **Level door:** a TafelBoem block with a level (1–3; level 0 is the exit door in the arenas),
  walk-through, with a portal-like look in vanilla style. Walking into it takes you to that level's
  arena. The signs use translatable text, so they follow each PC's game language. The item frames
  are fixed, so their bombs can't be taken.
- **Stars at the door.** Within 3 blocks of a level door, a kid's own action bar shows the level
  and their best stars on it, for example "Gemiddeld: ★★☆". Each kid sees only their own stars: no
  ranking between siblings.
- **Protected.** Bombs placed in the hub still go boom (bang, chickens, knockback) but break no hub
  blocks.

### The arenas
- **One arena slot per level**, far from the hub and from each other (512 blocks apart), so the
  arenas can't see each other and their fights never mix.
- **Default arenas, built from code,** at most 48 × 48 × 48 so they match one structure block:
  level 1 round, level 2 square, level 3 round with a darker floor. Each has a bedrock bottom layer,
  dirt, a top layer of its own (grass on levels 1 and 2), obsidian walls 4 high, open sky, and an
  exit door in the middle of the south wall:

  ```
       level 1: round            level 2: square            level 3: round
         ▓▓▓▓▓▓▓▓▓               ▓▓▓▓▓▓▓▓▓▓▓▓▓              (darker floor)
       ▓▓         ▓▓             ▓           ▓
      ▓      G      ▓            ▓     G     ▓              ▓ obsidian wall, 4 high
      ▓             ▓            ▓           ▓              G Graaf Fout appears
       ▓▓    ☺    ▓▓             ▓     ☺     ▓              ☺ kids arrive, facing G
         ▓▓▓▓⌂▓▓▓▓               ▓▓▓▓▓▓⌂▓▓▓▓▓▓              ⌂ exit door, back to the hub
                  S
  ```

- **Exit door:** the same door block with level 0, "back to the hub". The kids arrive 3 blocks in
  front of it, on the highest solid block, facing away from it.
- **Several arenas per level.** Templates for a level are named `tafelboem:level_<n>/<name>`. When
  the first kid walks into an empty level, one of its templates is picked at random, never the same
  one twice in a row. The plain arena from code is used only when a level has no template.
- **Placed on entry.** Placing the chosen arena first clears the slot and removes leftover items,
  bombs and chickens, so the craters of the last fight are gone too.
- **Destructible inside, never outside.** Explosions follow vanilla blast resistance: bedrock and
  obsidian never break, everything else can. The kicked bomb's boom on Graaf Fout now breaks blocks
  (it broke nothing in R0.1), but never a block within 3 blocks of a kid in the fight. Config toggle
  `arenaCraters`.

### The course of a fight
1. **Entering an empty level.** The arena is placed and the kid arrives at the arrival spot. About
   3 s later Graaf Fout appears in the middle of the arena (hovering over the highest block there)
   and the fight starts at that level; his first throw comes 3 s after that, as now.
2. **Joining.** Any kid who walks through a level's door while its fight runs joins that fight,
   however many kids there are. They take turns as now.
3. **Flying out.** His strongest yeets reach 30+ blocks, so he sometimes flies over a wall. When he
   drops below the bottom of the arena (inside it the bedrock stops him, so this means he flew out),
   the yeet-o-meter says *"Graaf Fout vloog 34
   blokken, de arena uit!"* and he poofs back near the kids, dizzy, as after a normal landing. A
   fight is never lost because he fell into the void.
4. **Win.** The finale, then 4 Kippenbommen for every kid still in the arena, and stars for every
   kid still in the arena who answered at least one question in this fight. A kid with a new best
   sees it, for example "★★ op Gemiddeld!". 5 s later everyone in the arena is back in the hub.
5. **Leaving.** A kid who walks through the exit door goes back to the hub, without reward or
   stars; the others play on. When the last kid has left or disconnected, Graaf Fout poofs away and
   the fight ends without reward.

### Levels and questions
| Level | Tables | Bombs he throws | Hits |
|---|---|---|---|
| 1 Makkelijk | ×1 ×2 ×5 ×10 | Knalletjes, every 3rd a Gewone TNT | 11 |
| 2 Gemiddeld | + ×3 ×4 ×9 | Gewone TNT, every 3rd a Kippenbom | 12 |
| 3 Moeilijk | all ten | only Kippenbommen | 13 |

- **A sum belongs to a table when either factor is that table**, so 7 × 2 is in the table of 2, as
  at school. Of the 55 sum pairs (7 × 8 and 8 × 7 are one pair), level 1 asks 34, level 2 adds 15
  and level 3 adds the last 6: 6×6, 6×7, 6×8, 7×7, 7×8 and 8×8.
- **Half from the added tables.** On levels 2 and 3, about half of the questions come from the
  pairs that level adds; the other half repeats the easier tables. Within each half the R0 weighting
  stays: missed and slow sums come more often.
- **Re-asks still come first.** A re-ask for a sum outside the current level waits until a level
  that has it.
- **Reward:** 4 Kippenbommen for a win on every level (4 cooked chicken from 1.0.2). Paying out the
  next bomb tier waits until bomb 4 exists.

### Stars
- **Per kid, per level; the best result counts.**
- ★ for a win, ★★ for a win with at most 2 wrong answers, ★★★ for a win without a wrong answer.
- **Only typed wrong answers count.** "?" and ESC don't, so stars reward "?" over guessing.
- Config toggle `stars`. When it is off, no stars are shown or announced; the fights log still has
  them, for the evaluation.

### Logs and stats
- **Fights log.** `<world>/tafelboem/fights-<player>.jsonl`, next to the answer log, one line per
  kid per fight, written when the fight ends for that kid (a win, the exit door or disconnecting):
  time, level, arena name, result (`won`, `left` or `disconnected`), answers, wrong answers, "?"
  and ESC, stars, seconds, how many kids took part, and the version. The stars at the doors come
  from this log; there is no other saved state.
- **Answer log.** Each answer also records its level: 1–3 in an arena, 0 elsewhere.
- **`/tafelboem stats`:**
  - a line per level played, for example "Gemiddeld: ★★☆, 3 of 4 won"
  - **P16:** decimals in the game language of whoever runs the command ("4,0 s" in Dutch, "4.0 s" in
    English); English from the server console
  - **P17:** "Still practising" lists only sums that were missed or took more than 6 s (the picker's
    "slow"), and says "nothing yet" otherwise

### Hand-built hub and arenas
The kids build in creative, in any world with cheats, and save with vanilla structure blocks:
1. Build. Both door blocks are in the TafelBoem creative tab.
2. `/give @s minecraft:structure_block`. Put one in each of two opposite corners in *Corner* mode,
   and one beside the build in *Save* mode, all with the same name, such as
   `tafelboem:level_2/kasteel`. Press **Detect**, then **Save**.
3. Minecraft writes `saves/<world>/generated/tafelboem/structure/<name>.nbt`. Built in the TafelBoem
   world itself, it is used there from the next entry. To ship it to every PC, the file goes into
   the mod's `data/tafelboem/structure/`, and the door can carry the builders' names.

**Rules for an arena:** at most 48 × 48 × 48; open sky over the play area (under a roof he would
hover on the roof); exactly one exit door, where the kids should come in; bedrock or obsidian for
what must never break. The floor may be uneven: the kick always counts, even when it hits a wall, and
he follows the ground. An arena template without an exit door is skipped, with a chat message to the
players that names it; another template or the plain arena is used instead. **Rules for a hub:**
the three level doors. The kids spawn in its middle.

### R1 patch 1.0.2 (before playtest 3)
Jesper played 1.0.1 through once himself; findings P18–P21 are in `docs/evaluations/R1.md`.
- **Walls on obsidian (P18).** The plain arenas have obsidian under their walls, down to the
  bedrock, so a crater beside a wall never opens a way out. Kid-built arenas follow the same rule.
- **No leftovers (P19).** Anything but a player that loads from disk in an arena slot is a leftover
  from an earlier fight and goes. The load contract in `AGENTS.md` lists every rule of this kind.
- **Hearts.** A typed wrong answer costs 1 heart on Gemiddeld and 2 on Moeilijk, nothing on
  Makkelijk or in the R0.1 fight, never the last half heart, and only in survival. "?" and ESC never
  cost hearts, so "?" stays better than guessing. Config toggle `wrongAnswerDamage`.
- **Food as the reward.** A win on a door level gives 4 cooked chicken instead of 4 Kippenbommen,
  so lost hearts heal (vanilla: a full hunger bar brings hearts back). The R0.1 fight keeps its
  Kippenbommen. Config toggle `foodReward`.
- **Kippenbom chickens.** A Kippenbom kicked into Graaf Fout lets 12 chickens fly; a wrong answer on
  a Kippenbom pops 3 chickens out at the kid, hopping only a little, so a wrong answer stays less fun.
  They poof after a minute like every spectacle animal. Config toggle `kippenbomChickens`.
- **Correction.** The fact stays on the HUD 6 s instead of 3.5 s, and after a miss Graaf Fout waits
  as long before his next bomb, so the next question doesn't cut it short.
- **Music (P21).** Each arena slot gets its level's biome when an arena is placed, and the biome
  sets the background music: Otherside on Makkelijk, Pigstep on Gemiddeld, "Boss" (`music.dragon`)
  on Moeilijk, back to back. The hub's biome brings the normal music back. Config toggle
  `arenaMusic`.

### Out of R1
- the kids' own arena: a later 1.0.Z build, once they have built one
- R1.1: the yeet catalogue, the full yeet-o-meter (personal best; fast answers and streaks yeet
  further), the counting Kippenbom
- R1.2: dodgeable bombs and "Graaf Fout eats your mistake", behind toggles
- the learning model (R2), and P13: the summon item isn't needed in the TafelBoem world, and its
  cause is still open

### Spike results (2026-10-14)
A throwaway spike (local branch `spike/r1-world`, one client game test) checked the new Minecraft
parts. All five worked, so the fallback (a `/tafelboem hub` command in vanilla's "The Void" world)
is not needed:
1. **World type.** A `world_preset` file `tafelboem:tafelboem` (flat generator, one layer of air,
   `"features": false`, biome `tafelboem:void`) plus an entry in the `minecraft:normal` tag puts
   "TafelBoem" on the World Type button. The name comes from the lang key
   `generator.tafelboem.tafelboem`.
2. **Recognising the world and placing at spawn.** A world doesn't remember its world type, so the
   mod recognises a TafelBoem world by its overworld: a flat generator with the `tafelboem:void`
   biome. That biome is vanilla's `the_void` without its stone start platform. Placing the hub on
   `SERVER_STARTED` and moving the spawn there (`setRespawnData`, game rule `respawn_radius` 0) puts
   a new kid in the middle of the hub.
3. **Templates.** A template saved the way a structure block saves lands in
   `<world>/generated/tafelboem/structure/level_2/<name>.nbt`, and `listTemplates()` finds it by
   the `level_2/` prefix.
4. **Far arenas.** After a teleport 512 blocks away, the fight there runs: he threw, the question
   came, and the kick cost him a hit.
5. **Client game test.** `adjustSettings` with `getNormalPresetList()` and `setWorldType` creates a
   TafelBoem world, so the R1 client game test can take screenshots of the hub and the arenas.

### R1 tests
- **Unit tests** (pure Java): each level's tables, bombs and hits; the picker (only the level's
  sums, about half from the added tables, re-asks outside the level wait); the star rule (0, ≤ 2 or
  more wrong gives 3, 2 or 1 stars; "?" and ESC don't count; no answers gives no stars); the arena
  shapes (within 48³, bedrock bottom, obsidian walls, exit and arrival spots free); the random arena
  choice (never twice in a row); the fights log and best stars; the stats lines for P16 and P17.
- **Game tests:** door → arena → a fight at that level; three kids through one door are in one
  fight; a won fight on level 2 pays the reward, writes the stars and sends everyone to the hub; the
  last kid leaving ends the fight; flying out brings him back; a crater spares obsidian, bedrock and
  the floor near a kid; with two templates in `level_2`, one of them is used and not the plain arena.
- **Client game test:** creates a TafelBoem world and takes screenshots of the hub and an arena.

**R1 acceptance criteria** (manual, on the woonkamer PC and a second PC over LAN):
1. A new TafelBoem world starts in the hub: always day, no monsters.
2. Each door leads to its level, and level 3 throws only Kippenbommen.
3. Two PCs through the same door end up in the same fight.
4. A win gives 4 Kippenbommen and stars and returns everyone to the hub; the stars show at the door.
5. No crater reaches the void, and nobody can fall into it.
6. An arena saved with a structure block in the TafelBoem world is used.
7. After `./gradlew publishPack` says "Live", Play installs 1.0.0 on both PCs.

**R1 evaluation (playtest 3).** Record it in `docs/evaluations/R1.md`, in two parts. The numbers
come from `/tafelboem stats` and the fights log (share `latest.log`, as with playtest 2). Jesper
types the rest: who played, laughter, confusion and frustration as noticed, the three questions, and
whether they asked to play again. There is no video session.

**Go when:**
- the kids ask to play again within 2 days, without being prompted
- the fights log shows a harder level picked later on
- answers per minute are at least R0.1's (about 5)
- no frustration about the stars
- the checks R0.1 couldn't measure hold: they see the penalty and the yeet, and the reward arrives

## R1.1 "Meer yeets" (detailed scope)

Decided with Jesper on 2026-10-24, before the R1 evaluation (playtest 3 hasn't been played yet;
Jesper chose to build R1.1 first). The plan was then reviewed from a developer's, an architect's and
a tester's view. R1.1 is the yeet catalogue, the full yeet-o-meter and the counting Kippenbom. The
meteor, P20 (a reaction to a sword hit) and the kids' own arenas wait.

**Hypothesis:** more variety keeps them coming back.

### A hit is a flight plus a landing
Up to R1 every hit was one of five flights and the same 1.5 s dizzy spell. From R1.1 the flight is
picked at the kick and the landing when he lands.
- **The wall splat** (a new flight, only in an arena): he flies low and fast, slaps flat against the
  first wall he hits, sticks for a moment and slides down. With no wall in the way he just falls.
- **The landings,** each 2 s (40 ticks) at most:

  | Landing | What he does |
  |---|---|
  | Dizzy | stars circle his head (the R1 landing, 1.5 s) |
  | Head-first | stuck head-first in the ground, legs wiggling, then he pops out |
  | Pancake | squashed flat, waddles, then *boing* back into shape |
  | Frantic running | runs a small circle, smoke coming off his pants, arms waving; over ground only, at least 2 blocks from every kid, and he never pushes anyone |

- **Bomb signatures** (the landing for that bomb):

  | Bomb | Signature |
  |---|---|
  | Knalletje | hops on one leg holding his toe, "au!", confetti around his head |
  | Gewone TNT | soot face with white eyes, and he coughs smoke |
  | Kippenbom | runs from 4 angry chickens that appear where he lands, and poof when the chase ends |

- **Picking rules:**
  - Each bomb remembers its own last kick. Its signature plays when that kick didn't get it and
    the signature doesn't look like the last landing of any bomb; otherwise a generic landing
    comes from a shuffled bag. So each bomb gets its signature every other kick of its own,
    whatever the other bombs did in between.
  - The same landing never plays twice in a row; frantic running and the chicken chase count as
    one for this rule.
  - The boomerang (face down) and the wall splat (slide down) keep their own ending, followed by
    the dizzy spell. That counts as the landing that played, and as "no signature" for that bomb,
    so its next kick gets the signature.
  - Flying out of the arena cancels any landing; he comes back dizzy, as in R1.
- **Pacing:** the cooldown after a landing is shortened by however much the landing runs past the
  dizzy spell's 30 ticks, so the time from a kick to the next throw stays what it was in R1. If he
  is still reacting about 15 s after a kick (he can only freeze, never take that long), the fight
  brings him back next to a kid and logs a `TafelBoem stuck:` line.
- **Toggles:** `yeetCatalogue` (the wall splat and the new landings) and `bombSignatures`. With both
  off he flies and lands as in 1.0.2.

### The full yeet-o-meter
- **Fast:** the answer took less than 0.8 × the kid's own median of their last 5 right answers on
  this sum (3 × 7 and 7 × 3 count together). Under 3 earlier right answers on that sum there is no
  bonus. The verdict is made before the answer is logged, so it never counts towards its own median.
- **Streak:** right answers in a row by this kid in this fight. Only a typed wrong answer ends it;
  "?", ESC and a timeout don't, so "?" stays better than guessing.
- **Boost:** `factor = min(1.3, 1 + (fast ? 0.15 : 0) + 0.05 × (streak − 1))`, times the R1 power.

  | Fast | Streak | Factor |
  |---|---|---|
  | no | 1 | 1.00 |
  | yes | 1 | 1.15 |
  | no | 4 | 1.15 |
  | yes | 4 | 1.30 |
  | yes | 9 | 1.30 |

- **The helicopter's climb is capped** at its R1 maximum (55 ticks), so every flight lands within
  the 5 s limit, even at full power. The strongest R0.1-fight yeet stays within the 96 blocks he
  comes back from.
- **The kicker** sees why on the meter, for example *"Graaf Fout vloog 31 blokken! (snel, 4 op een
  rij!)"*, with the real streak. The other kids see the plain meter: no ranking between siblings.
- **Personal best** (TafelBoem world only): each kid's farthest yeet, fly-outs included; the
  helicopter's height doesn't count. A new best replaces the meter on the kicker's action bar,
  *"Nieuw record! Graaf Fout vloog 41 blokken!"*, with a toast sound that only the kicker hears. The
  first yeet on file sets the best silently, so the first kick after the update isn't a "record".
  The best comes from the fights log (new field `farthestYeet`), so there is no other saved state.
- **Toggles:** `yeetBoost` and `yeetRecords`.

### The counting Kippenbom
- A right answer on a Kippenbom for a × b sends **a** chickens out in a ring where the bomb goes off
  (where it hits Graaf Fout, or where it stood in free play). When it lands (about 1 s later, 2 s
  at most) each one pops into a tight group of **b** chickens that stands on the ground, so 3 × 4
  shows as 12 chickens in 3 groups: two generations at most. The groups form while he flies,
  between the kicker and him, where the camera looks. (A group that popped in mid-air fluttered
  down spread out and read as a flock. On 10 × 10 the ring is so full that neighbouring groups
  touch; the playtest judges where that starts to matter.)
- Counting chickens poof after 10 s, and no more than 150 spectacle animals live at once (the
  oldest poof first), so 10 × 10 on Moeilijk can't pile up.
- A wrong answer on a Kippenbom lets **1** chicken hop out while counting is on: never more than the
  smallest right answer (1 × 1), and it only hops, while a right answer's chickens fly.
- **Toggle:** `countingKippenbom`. Off, a Kippenbom lets 12 chickens fly as in 1.0.2;
  `kippenbomChickens` off still means no chickens in a fight.

### Outside a TafelBoem world
An exception to "R0.1 behaviour stays": the catalogue (without the wall splat), the signatures, the
boost and the counting work in every world. Records are TafelBoem-world only.

### Logs for the evaluation
The action bar never reaches `latest.log`, so the host's log gets:
- `TafelBoem config:` once per world, with every toggle
- `TafelBoem yeet:` per kick: kicker, bomb, sum, answer time and usual time, fast, streak, factor,
  flight, landing (signature or not), blocks, flew out, record and the previous best
- `TafelBoem chickens:` per counting burst: the sum, the groups, and fight or free play
- the `TafelBoem fight:` line adds the kid's farthest yeet

### R1.1 tests
- **Unit tests:** the reaction picker (signatures alternate, nothing twice in a row with running and
  chase as one, the wall splat only in an arena, own endings, toggles off), the boost table, the
  answer times (last 5, at least 3, the current answer not counted), the chicken count for all 100
  sums (never fewer than a wrong answer's 1), the streak rules, an old fights-log line, the pose
  transforms, the running circle (inside the arena, away from kids), the pacing.
- **Game tests:** a fight forced through every landing never stalls; a full-power helicopter (the
  one flight whose time in the air grows with the power) lands within the limit; a 3 × 4 burst gives 12 chickens and none are left after 12 s; the
  boost text and the record reach only the kicker; a fly-out during a landing cancels it. Slow
  fight tests run three times.
- **Client game test:** screenshots of head-first, pancake, splat, soot face and toe hop.

**R1.1 evaluation (one playtest, compared with R0.1).** The same session fills in the numbers of
`R1.md`; R1's and R1.1's effects can't be told apart, the yeet lines show what R1.1 did. Record it in
`docs/evaluations/R1.1.md`.

**Go when:**
- answers per minute are about 5 or more (R0.1), and % right and "?" are no worse than R0.1
- the kids still ask to play
- each kid names a favourite reaction
- they noticed the record or the boost
- the counting chickens were seen as groups
- the yeet lines show the fast bonus on a minority of kicks, not on half

## Product vision (the backlog for R1+, re-prioritised after every evaluation)

### The TafelBoem world
- **World type.** Pick "TafelBoem" at world creation. You spawn in the **hub** above the void.
  The game mode is not forced, so the kids can also build there in creative (decided 2026-10-14).
- **Premade arenas (R1).** Each level is played in a premade arena, saved as a vanilla structure
  template (`.nbt`) and placed by the mod in the void world.
  - Placing the arena fresh when the first kid walks in resets it (R1), which replaces the
    procedural rebuild. A level with several arenas picks one at random.
  - **The kids build arenas too.** They build one in creative together and save it with a
    structure block. It ships in the next version as a level, with their names on the door
    (from a 1.0.Z build).
  - The `.slime` format was considered and dropped: its implementation (Advanced Slime Paper) is a
    Paper server fork that runs plugins only, not Fabric mods.
- **Hub (protected from craters):**
  - **A door per level**, like the lobby of a minigame server. Each door shows the level's bombs
    and the stars earned on it. **Any unlocked level can be picked, and the easy level is always
    open.** The kids asked for an easy way to choose the difficulty.
  - Locked bosses show as silhouettes with a one-line story teaser.
  - A **story lectern** has a short illustrated book per boss, unlocked as you go.
  - A **Knalletje dispenser** gives free starter bombs.
  - A **Tafelkaart wall** for each player. It is a 10×10 grid of blocks showing every fact's
    state: stone → copper → iron → gold → diamond. The goal is "Maak het bord goud!".
  - A **trophy wall** with one banner per level beaten. Locked bombs show as silhouettes.
  - A **practice field (oefenveld)** where the kid picks a table on a lectern. It starts as the
    ordered rijtje, then mixes. It uses free Knalletjes, gives no loot, and counts toward the
    Tafelkaart.
  - An **animal pen**: one animal from every Farm Frenzy boom moves in, up to 10, and they can be
    named.
  - The **hardcore lectern** appears after beating the final boss.
  - **Picture signs** explain how to arm a bomb and what a super attack is, alongside Graaf
    Fout's intro speech at level 1.
- **Force field.** A glowing particle wall at the edge pushes you back inward. Running away was
  accepted in R0; the premade arenas bound it naturally.
- **Void safety.** A fall during a fight returns you to the arena edge. Outside a fight it returns
  you to the hub. Neither kills you.
- **Safe floor.** No block within about 4 blocks of a player in a fight is ever carved. Craters
  open on the boss's side.
- **Self-repair.** The arena is placed fresh when the next fight starts (R1). Bedrock and obsidian
  keep the outside intact during a fight.
- **Arena themes.** Each boss has an arena theme, a set of premade arenas with its own palette and
  props:
  - Graaf Fout: castle courtyard with purple banners
  - Tante Toverfout: swamp with mud, lily pads, cauldrons and purple fog particles
  - Robo-Rekenaar: lab and factory with iron, redstone lamps and pipes
- **World rules** are set on creation:
  - always day, no weather, no hostile mob spawning
  - keepInventory on, and no hunger (constant saturation)
  - `tnt_explodes` on
- **Outside the game.** On the kids' PC, the Fabric profile is the **only** launcher profile.
  Their existing worlds can move into it.

### The fight (same rules for every boss)
- **The boss is the joke, never the kid.** Taunts never state a wrong fact and never comment on
  the child's ability: *"Nog niet! 7 × 8 = 56… die gooi ik zo nóg een keer!"*. When a fact
  becomes Fluent he groans *"Nee! Je kent 7 × 8 nu!"*.
- **Thrown bombs.** The boss throws a bomb of the level's tier. It lands at your feet, you are rooted,
  and the question pops up.
  - **Correct:** you **kick it back** for 1 hit. The blast and his yeet grow with the bomb.
  - **Clean escape:** if the blast would have reached you and you get out in time, you get +1 hit.
  - **Failed escape:** you are only launched, harmlessly. **A correct answer never hurts you.**
  - **Wrong:** the tier's penalty hits you, outside the question screen so you can see it.
  - **Camera assist:** while answering, the view turns to the boss, and after a correct answer it
    follows him until he has landed, so the kid never misses the reaction.
  - **Kick animation.** The kid who answered correctly visibly kicks the bomb back, so the hit
    feels like their own doing.
    - Simple version (R0.1): the vanilla arm swing, a "whoosh" sound and a small camera bump when
      the kick leaves. No custom animation needed.
    - Full version (R2): the right leg swings forward in third person, so
      siblings on LAN see each other kick. This needs a player-model pose plus a payload to the
      other clients. In first person your own legs aren't visible; a boot swinging into view is
      later polish.
- **Dodgeable bombs (R1.2 experiment, toggle).** The kids wanted to dodge.
  - A bomb you dodge lies on the ground with a visible fuse.
  - Running into it starts the question as before.
  - If the fuse runs out, it pops harmlessly: a soot puff, no question, no damage to the boss.
  - Damage only comes from answered kicks, so dodging never replaces sums. Watch answers per minute.
- **"Kies je bom" (R2).** Several bombs lie on the ground and the kid picks which one to run into.
  - A big bomb asks a fact the kid is still learning, deals more damage and gives a bigger yeet.
  - A small bomb asks a fact they know and deals 1 hit.
  - The kid chooses the challenge in the moment; the picker still decides *which* facts come up.
- **Graaf Fout eats your mistake (R1.2 experiment, toggle).** The kids' idea.
  - He loves mistakes, so on a wrong answer he catches the wrong number and eats it, and gets one
    hit back. The boss bar visibly goes up.
  - "?" and ESC never heal him: *"Hè, geen fout om op te eten!"* This makes "?" better than
    guessing, which is what the "?" button is for.
  - At most 3 times per fight, never above full health. The pacing test must show that a
    60%-accurate child still wins before this ships.
- **Combo.** Correct answers in a row raise the sound pitch. 5 in a row gives a **golden kick**
  for +1 hit.
- **Super attacks.**
  - They charge after 3 kick-backs in a row. Then arm one of your earned bombs and answer
    correctly.
  - A bomb of the fight's tier or higher deals **3 hits**. A lower-tier bomb deals 1 hit
    ("plop!").
  - At most 4 per fight. Spent bombs are refunded if the fight is lost.
- **Boss HP.**
  - Graaf Fout needs **10 + level** hits: 11 at level 1, 19 at level 9, 135 in total. That is
    about **150–180 questions**.
  - Bosses 2 and 3 need **8 + level** hits: 117 each.
  - About **450 questions for the whole trilogy**. With the daily dose that is at least 8 days,
    and realistically 4–8 weeks.
- **Lives.** You have 3 lives per fight. A 💀 penalty costs a life and you respawn at the arena
  edge. **The boss keeps his damage.** After 3 deaths the fight is lost and he heals.
- **ESC while rooted** means "chickened out". A chicken pops out, the answer is shown, the fact
  is marked shaky, and the bomb does no damage. At most 3 per fight; after that ESC counts as "?".
- **Levels 1–9 per boss** keep the difficulty bands:

  | Level | Band |
  |---|---|
  | 1–2 | easy |
  | 3–4 | easy + medium |
  | 5 | medium |
  | 6 | medium + shaky |
  | 7 | medium + hard |
  | 8 | hard + shaky |
  | 9 | hardest / shakiest |

  All fight questions use the fight's band, including super attacks. The picker chooses within it.
- **Daily dose.** After about 15 min or 60 fight questions, the bosses go to sleep until
  tomorrow (*"Zzz…"*). The bell rests, and free play and the practice field stay open. The limit
  is configurable.
- **Co-op on LAN.**
  - Siblings share one boss bar.
  - Each player gets questions from *their own* progress and has their own lives and penalties.
  - A knocked-out sibling is revived when the other answers a **rescue sum**.
  - Booms never hurt other players. There are no rankings.
- **Endgame, after beating the final boss.**
  - **"Ze komen terug!"** is an endless mode: waves with all three bosses taking turns, mixed
    tiers, and a personal best.
  - The **hardcore** toggle turns on "fuse burns while thinking" for tiers 7–9.
    - The fuse lasts the longer of 6 s or twice the child's own median time for that fact.
    - Running out counts as wrong.
    - A visible fuse bar shows on the question screen.

### Boss reactions: every hit is hilarious
- **Every hit triggers a reaction lasting 2–4 s.** This covers kick-backs, clean-escape bonuses,
  golden kicks and super attacks. The boss doesn't throw while reacting, which gives each fight its
  rhythm.
- **Spin is the core.** Playtest 1 showed the spinning yeet was the funniest thing in the game, so
  every reaction includes a spin.
- **Yeet-o-meter.** After every yeet, everyone in the fight sees how far he flew: *"Graaf Fout
  vloog 23 blokken!"* (R0.1). For the helicopter it says how high: *"…vloog 12 blokken hoog!"*,
  so the helicopter doesn't always look like the weakest yeet. From R1.1 there is a personal best, and fast answers and streaks yeet
  him further. Only personal bests, no ranking between siblings.
- **Shared reaction moves** (used by every boss, with that boss's props):
  - **Yeet:** launched with a spin, landing head-first stuck in the ground, legs wiggling, then pops out.
  - **Bounce yeet:** bounces three times like a ball, each bounce lower, with a boing on each
    bounce (R0.1).
  - **Helicopter yeet:** spins up slowly on his cape like a propeller, higher for a bigger bomb,
    then drops (R0.1).
  - **Boomerang yeet:** flies off, curves back and falls flat on his face in front of the kid (R0.1).
  - **Tumble yeet:** rolls and flips head over heels while he spins (R0.1).
  - **Wall splat:** flies into the arena wall, sticks for a moment and slides down.
  - **Frantic running:** circles the arena with his pants smoking, waving his arms.
  - **Pancake:** squashed flat, waddles, then *boing* back into shape.
  - **Dizzy spin:** spins like a top with stars circling his head.
  - **Chases his blown-off cape or hat.**
  - **Hair on end:** a lightning zap.
  - **Pinball:** bounced between mini blasts.
  - **Meteor:** blown into orbit (a twinkle), comes back down like a meteor and leaves a crater in
    his own shape.
- **Bomb signature reactions** (shown for that tier's kick-backs and super attacks):

  | Bomb | Reaction |
  |---|---|
  | Knalletje | hops around holding his toe, with confetti in his hair |
  | Classic TNT | soot face, coughs a smoke ring, blinks white eyes |
  | Kippenbom | chased around the arena by angry chickens |
  | Boerderijbom | a cow lands on him; he rides away on a pig and falls off |
  | Clusterbom | pinball |
  | Aambeeldregen | anvil pancake |
  | Raket-TNT | rides the rocket into the sky and parachutes down on his cape |
  | Megakrater | yeeted, stuck head-first |
  | Armageddon | meteor |

- **Golden kick:** a slow-motion yeet.
- **No repeats.** A reaction is never shown twice in a row. A generic move is mixed in when the
  signature move was just shown.
- **Damage states.** As HP drops, the skin gets scruffier in three stages (clean → singed → sooty,
  torn cape), with idle tics such as an eye twitch and smoke puffs.
- **A unique defeat finale for each boss.**

### The three bosses
1. **Graaf Fout (Count Wrong).** Accuracy.
   - **Look:** an illager-style villain with a purple cape and a big red ✗. He hovers and teleports
     in particle puffs.
   - **Story:** he loves mistakes and wants everyone to get their sums wrong.
   - **Defeat:** he flees on a pig, swearing his aunt will get revenge.
2. **Tante Toverfout (Aunt Spellwrong).** Flexibility.
   - **Look:** a swamp witch who flies on a broom and throws potion-bottle bombs (same tiers,
     different projectile skin).
   - **Story:** *"Ik heb de sommen betoverd — er zijn getallen verdwenen!"*
   - **Twist:** missing-factor questions (`? × 7 = 42`), used only on facts that are already
     Accurate or better. Other facts are asked in the normal format.
   - **Own reactions:** turns into a frog for a moment, crashes her broom, chases her hat, gets
     covered in green goo when her cauldron backfires.
   - **Defeat:** her broom sputters and she spirals off into the fog.
3. **Robo-Rekenaar 3000.** Fluency.
   - **Look:** a robot built by the two of them, using the iron-golem body with a robot texture.
   - **Story:** *"Mensen hoeven niet meer te rekenen!"* The kid proves that people can.
   - **Twist:** race the robot. A "computing…" bar runs during each question, timed to **the
     child's own median for that fact × 1.2**. Beating the bar deals double damage, and being
     slower is a normal hit. Speed only ever adds; it never takes away.
   - **Own reactions:** short-circuits in a shower of sparks, head spins 360° as bolts pop off,
     blue-screen face, reboot jingle.
   - **Defeat:** he falls apart into a pile of parts, and Graaf Fout and Tante Toverfout row away
     in a little boat. Then a finale with fireworks and the **"Tafelkampioen"** title.
- **Unlock order.** Beating a boss's level 9 unlocks the next boss. Beating Robo-Rekenaar unlocks
  the endless and hardcore modes.
- **Story delivery.** Short title-screen intros, 3–4 speech lines at each boss's first level and at
  its defeat, and a one-liner per level. Everything is in both languages.

### Bomb economy (loot)
- **Knalletje** (tier 1) is free. It comes from the dispenser and a cheap recipe, and it is the
  only bomb in the creative tab.
- **First clear of level L** (of any boss) pays out **4** tier L+1 bombs. It pays **5** if your median answer
  time in that fight beat your own personal median, so the bonus is fair for a 7-year-old too.
- **Replays** pay out 2 tier L+1 bombs, so a kid can never get stuck without the bombs a level
  needs.
- The payout comes with a fanfare: a toast, fireworks and "🎁 Kippenbom ×4!".
- About 1 in 50 loot bombs is a **golden** variant. It is cosmetic only.
- Beating each boss earns a trophy and the next boss. Beating the final boss earns the
  "Tafelkampioen" celebration plus the endless and hardcore modes.
- **Anti-cheat:**
  - Creative pick-block gives nothing.
  - A bomb broken without being armed drops itself.
  - Fire and redstone cannot ignite a bomb, because it is a plain `Block`.
  - `/tafelboem give` only works when the config has `allowGive=true`, which the parent sets.
  - Arming your last Armageddon asks for confirmation.

### The 9 bombs

| # | NL / EN | Look | ✅ Boom (correct) | 🏃 Escape aid | ❌ Penalty (wrong, short) |
|---|---|---|---|---|---|
| 1 | Knalletje / Firecracker | bundle of red-yellow firecracker sticks (small model) | small pop + confetti fireworks | none needed | soot puff, blind 1.5 s |
| 2 | Gewone TNT / Classic TNT | TNT-style block with "×" instead of "TNT" | vanilla-size blast | none needed | egg splat on screen, 2 s |
| 3 | Kippenbom / Chicken Bomb | white feathered box, beak, red comb | blast + *a* chickens fly out, and each bursts into *b* more: 3 × 4 shows as 12 chickens in 3 groups (R1.1; at most 2 generations) | none needed | bucket of water from the sky + slowness, 3 s |
| 4 | Boerderijbom / Farm Frenzy | red barn planks, white X-brace, hay on top | ~16 random farm animals burst out (cow, pig, random-colour sheep, goat, horse, rabbit, rare mooshroom), land unharmed, poof after 60 s; one moves to the lobby pen | none needed | a cow drops on your head (1 heart) |
| 5 | Clusterbom / Cluster TNT | 8 mini-TNTs roped together (custom model) | splits into 8 bouncing bomblets | Speed I | slimed: slowness + green overlay, 2 hearts |
| 6 | Aambeeldregen / Anvil Rain | iron-grey crate with an anvil on top | crater + anvils rain down | Speed II | anvils on your head (hurts, not lethal) |
| 7 | Raket-TNT / Rocket TNT | **3D rocket on a launch pad** (TNT-striped body, red nose cone, fins, nozzle) | countdown, lift-off with flame and smoke trail, arcs up, plunges nose-first into a mega crater | Speed II + Jump | strapped to the rocket… splat 💀 (costs a life) |
| 8 | Megakrater / Mega Crater | black with glowing orange cracks, "MEGA" | huge crater (r≈24) + lightning | firework launch + slow fall (front-row seat) | "evaporated" 💀 |
| 9 | Armageddon | obsidian black, glowing purple runes, skull | TNT rain over 40×40 + crater (r≈32) | firework launch + slow fall | "the sky fell on you" 💀 |

- **The answer is the payload.** Every correct boom first shows the **product grid**: for 7 × 8,
  that is 7 rows of 8 glowing cubes (particles or block displays) over the target. They detonate in
  a wave, followed by the bomb's own spectacle. The grid is the rechthoekmodel, so the kid sees
  what the sum means.
- **Escape time** is generous at every tier: the time needed to sprint out of the blast radius × 1.5.
  A **fast bonus** applies when the answer beat the child's own usual time for that fact.
- **💀 deaths** use custom damage types with funny translatable death messages, followed by the
  correct fact shown big. They also work in creative mode. Inventory and XP are stashed and
  restored on respawn.
- **Textures.** v1 uses simple pixel-art textures that I generate, and the kids could redraw them.
  Bombs 1, 5 and 7 get custom JSON block models.

### Feedback & the error loop (on the HUD, not in chat)
- **Screen and input:** huge digits, number keys plus Enter, no mouse needed. The number pad stays
  clickable. Vanilla buttons only: a wide OK under the pad, greyed out until something is typed,
  and "?" in the far corner of the panel.
- **"?" button (Ik weet het niet).** It does no damage. It shows the fact and a tip, and the fact
  comes back soon. It replaces guessing.
- **After a wrong answer, "?", ESC or a 💀:**
  - The question screen closes and the kid is free. The penalty plays where the kid can see it.
  - Then the correct fact appears big on the HUD (`7 × 8 = 56`) for about 3 s, while the game goes
    on.
  - A derived-fact tip shows how to work it out, for example "7 × 8 = 5 × 8 + 2 × 8" or
    "×9 = ×10 − ×1".
  - It also shows "8 × 7 is dezelfde!".
  - The fact is asked again 1–2 questions later and once more at the end of the session.
- **Why no retype.** The evidence-based parts of error correction are showing the correct answer
  (Pashler et al. 2005; Bangert-Drowns et al. 1991) and recalling it again a bit later (the testing
  effect and spacing). Retyping while the answer is on screen is copying, not recalling; its main
  value was making sure the kid looked. So the re-ask is the check, and **re-ask accuracy** in the
  stats tells whether the HUD correction lands. The retype stays available behind the
  `correctionRetype` toggle.
- **Typing baseline.** Without the retype, the child's typing speed is estimated from their fastest
  correct answers, so response times can exclude typing.

### Particle effects (elaborate, on every bomb, to make it rewarding)

Every bomb has its own particle signature, shown at each of these moments:
- **Idle:** a placed bomb gives off a small signature wisp.
- **Armed:** a spark climbs the fuse, and a tier-coloured ring of "chains" circles the rooted player.
- **Answer:** correct gives a green sparkle burst. Wrong gives a red puff.
- **Escape:** the fuse sparks get faster and brighter as the boom gets close.
- **Product grid:** a glowing wave of a × b cubes.
- **Boom:** a choreographed sequence lasting 1–4 s, described per bomb below.
- **Loot fanfare:** a golden fountain and a spiral of the earned bomb's signature particles.
- **"Mastered!":** a burst when a fact becomes Fluent.
- **Bosses:**
  - Graaf Fout: teleport puffs and a purple trail on thrown bombs.
  - Tante Toverfout: green bubbles and a broom trail.
  - Robo-Rekenaar: sparks and steam.
  - Every reaction move has its own FX: dizzy stars, smoke pants, a dust ring on landing, a
    meteor trail.
  - Each defeat finale ends in ✗-shaped particles.

| # | Bomb | Signature particles |
|---|---|---|
| 1 | Knalletje | sparkler fizz on the fuse; a burst of multicolour confetti fluttering down |
| 2 | Classic TNT | expanding smoke shockwave ring + ember sparks |
| 3 | Kippenbom | feather storm spiralling outward + bits of eggshell |
| 4 | Boerderijbom | a fountain of hay and wheat bits; hearts over every animal as it lands |
| 5 | Clusterbom | each bomblet trails sparks and pops into its own mini shockwave ring |
| 6 | Aambeeldregen | shower of metal sparks on every anvil impact + dust clouds |
| 7 | Raket-TNT | steam cloud on the launch pad during the countdown; flame and smoke trail; fireball and smoke column on impact |
| 8 | Megakrater | ground-level shockwave ring, lightning sparks, glowing ember rain, rising dust column |
| 9 | Armageddon | purple vortex sucking inward before the TNT rain, rune glyphs, end-rod star bursts, darkening swirl |

- **How it works.** The server sends `FxS2C(kind, tier, pos, seed, a, b)` to every player tracking
  that chunk. A client-side `FxDirector` plays the timed choreography. The same seed on every client
  means everyone sees the same show.
- **Custom particles.** `confetti`, `feather`, `ember`, `rune`, `spark` and `cross` are custom
  particle types with their own sprites. Everything else uses vanilla particle types.
- **Limits.** Effects respect Minecraft's Particles video setting and a config `fxIntensity`. Each
  choreography has a hard per-tick particle budget.

### Learning model
- **Facts.** There are 100 facts. Commuted pairs (3×7 and 7×3) are linked: both orders are asked,
  and progress on one counts toward the other.
- **Bands.** Each fact has a built-in difficulty: ×1 ×2 ×5 ×10 are easy, ×3 ×4 ×9 medium,
  ×6 ×7 ×8 hard. The level's band decides which facts may be *introduced*. Due reviews can come
  from any band already unlocked.
- **Fact states.** Each fact is tracked per child. Times are the median of the last 5 answers,
  minus the typing baseline.

  | State | Rule | Tafelkaart block |
  |---|---|---|
  | New | never asked | stone |
  | Learning | asked, but not yet 3 correct in a row | copper |
  | Accurate | 3 correct in a row | iron |
  | Fluent | the last 3 correct, each ≤ 3 s, spread over ≥ 2 days | gold |
  | Vast | still Fluent ≥ 7 days later | diamond |

  - A wrong answer drops a fact back to Learning.
  - A correct answer slower than 6 s drops it to Accurate.
- **Picker.**
  - The mix aims for 85–90% success: about 70% known, 20% learning and 10% new facts, with at
    most 3–4 not-yet-known facts in play.
  - Due spaced reviews come first. Fluent facts return after 1, 3, 7 and 14 days.
  - After 2 wrong answers in a row, the next question is an easy one.
  - **Formats per boss** follow the learning arc accuracy → flexibility → fluency:
    - Boss 1 uses the plain format only.
    - Boss 2 adds the missing-factor format (`? × 8 = 56`) for Accurate+ facts.
    - Boss 3 adds the race bar, which is positive-only.
    - Endless and hardcore mix everything.
- **Table learned.** A table counts as learned when all its facts are Fluent, which earns a banner.
- **Parent view** (`/tafelboem stats [player]`):
  - a 10×10 heat grid of fact states
  - % Fluent per table
  - questions and minutes per day, plus a weekly trend of median time
  - the 5 hardest facts, each with the child's usual wrong answer (e.g., 7×8 → 54)
  - how often "?" is used, and how often it was likely a mis-click
  - re-ask accuracy: how often a missed fact is right when it comes back
  - a flag for fast wrong answers on Fluent facts, which suggests deliberate deaths

### Performance guard-rails
- **Craters** are carved over several ticks under a per-tick block budget, in loaded chunks only.
  They drop no items and leave bedrock, block entities, the lobby and player safe zones alone.
  The radius cap is set in the config (default 32).
- **Arena rebuild** and the product grid also run under per-tick budgets.
- **Spectacle entities** are tagged, capped in number, and cleaned up on timeout and when the
  world loads.

### Parked for v2
- a Fabric dedicated server on the NAS (after R2, only if needed): one world and one answer log for
  every PC, and the woonkamer PC no longer has to host
- a tactical role for each bomb (anvil rain stuns, the rocket homes in on him)
- daily "Fout's gril" modifiers
- division and deeltafels
- a printable tafeldiploma

## Technical approach

Versions were verified against the 26.2 jar, the Fabric API `26.2` branch and Loom source:
- JDK 25 (`openjdk-25-jdk`)
- Loom **1.18.3**, plugin `net.fabricmc.fabric-loom`, no mappings line
- Gradle 9.7.1 (wrapper)
- Loader **0.19.5**
- Fabric API **0.161.0+26.2**

Start from the `fabric-example-mod` `26.2` branch. Dependencies use `implementation`, the build
outputs `jar` (there is no `remapJar`), and `splitEnvironmentSourceSets()` is on. The package is
`nl.jeeninga.tafelboem` and the mod id is `tafelboem`.

### Layout
- `src/main/java/nl/jeeninga/tafelboem/`
  - `core/`: pure Java with no Minecraft imports, covered by JUnit.
    - `Fact`, `FactFamily`, `FactStats`, `MasteryModel` (fact states)
    - `SpacingScheduler`
    - `QuestionPicker` (band, mix, due reviews, re-asks, easy-after-2-misses, missing-factor format)
    - `DerivedFactTips`
    - `PlayerProgress`, `DailyDose`
    - `BombTier` (the data-driven catalogue)
    - `FightPolicy` (HP per boss, combo, super attacks, lives, ESC cap, loot, replays, endless waves)
    - `BossCatalog` (data per boss: HP formula, question formats, twist, arena theme, reaction
      pool, story line keys, unlock chain)
    - `ReactionPicker` (signature or generic, never the same twice in a row) and `RaceBar` (the
      robot timer from the child's own median)
    - `EscapeModel`
    - `ArenaShape`, `CraterShape`
    - `StatsReport`, `ConfigModel`
    - `FxShapes` (point generators for ring, spiral, sphere shell, fountain, vortex and the product
      grid)
  - `registry/`: blocks, items, entities, creative tab, damage-type keys, attachments, custom
    particle types
  - `block/BombBlock`: one class parameterised by `BombTier`. Its `useWithoutItem` starts a session
    and `getCloneItemStack` is guarded.
  - `net/`: `QuestionS2C`, `ResultS2C`, `FxS2C`, `AnswerC2S`, `DontKnowC2S`, `ChickenOutC2S`.
    These are `CustomPacketPayload` records registered via
    `PayloadTypeRegistry.clientboundPlay/serverboundPlay`.
  - `game/`: `SessionManager` (server-authoritative state machine per player: ASKING →
    ESCAPING | KICKBACK | PUNISHING → CORRECTING (after a wrong answer, "?" or ESC) → DONE, or
    CANCELLED), `RootLock` (transient
    `MOVEMENT_SPEED`/`JUMP_STRENGTH` modifiers plus per-tick pinning), `LootPayout`
  - `arena/`
    - `ArenaBuilder` (budgeted build and rebuild, with a theme palette per boss)
    - `ArenaRules` (force field, void safety, safe zones, world rules, forced Survival)
    - `Lobby`: bell for boss and level select, dispenser, `TafelkaartWall`, trophy wall, story
      lectern, `PracticeField`, `AnimalPen`, hardcore lectern, signs
  - `boss/`
    - `AbstractBoss`: a `Monster` with hover and teleport AI, `ServerBossEvent`, damage only from
      TafelBoem sources, and synced `reactionId`/`reactionStart`/`damageStage` via
      `SynchedEntityData`.
    - `GraafFout`, `TanteToverfout` (broom flight) and `RoboRekenaar`, each a thin subclass plus a
      `BossCatalog` entry.
    - `ReactionDirector` (server side): runs the motion for each reaction move, such as yeet
      velocity, the panic-run path, head-in-ground, or riding a pig or rocket, and sends `FxS2C`.
      The frog transform hides the witch and spawns a named frog for 2 s.
    - `ThrownBomb`: a projectile carrying the tier, with a skin per boss (bomb or potion bottle).
      It starts a lap session when it lands.
    - `FightManager` (bosses, levels, cadence, co-op turns, lives, rescue sums, win and loss,
      daily sleep, endless, hardcore, story lines)
  - `boom/`: `CraterBuilder`, `ProductGrid`, `Spectacle` (one per tier), `RocketFlight` (a block
    display moved each tick)
  - `penalty/`: `Penalties` (one per tier), `InventoryStash` (stash in `ALLOW_DEATH`, restore in
    `COPY_FROM`/`AFTER_RESPAWN`, persisted as a `copyOnDeath` attachment)
  - `data/` (codecs), `command/` (`stats | reset | give | fight <boss> <level>`), `config/` (Gson,
    `config/tafelboem.json`: daily dose, HP, payouts, `allowGive`, crater cap, `fxIntensity`)
- `src/client/java/.../client/`
  - `QuestionScreen`
    - Shows the big digits, the bomb at stake, the "?" button, the correction and retype step, the
      tip, the missing-factor format, the robot race bar, and the hardcore fuse bar.
    - Uses the 26.2 `extractRenderState(GuiGraphicsExtractor …)` API, with `isPauseScreen() = false`.
  - `EscapeHud` and `ComboHud`
  - **Boss renderers** reuse vanilla models with custom textures (3 damage stages each):
    - `GraafFoutRenderer` uses the illager model.
    - `TanteToverfoutRenderer` uses the witch model.
    - `RoboRekenaarRenderer` uses the iron golem model.

    A shared `ReactionPose` layer applies squash and stretch (pancake), spin, upside-down sink
    (stuck head-first) and wobble from the synced reaction state.
  - `ThrownBombRenderer`
  - `fx/`: `FxDirector`, one choreography per bomb, the particle providers, and idle wisps
- `src/main/resources/`
  - `assets/tafelboem/{lang/en_us.json, lang/nl_nl.json, blockstates, models, items, particles, textures}`
  - `data/tafelboem/{damage_type, recipe, loot_table, worldgen/world_preset, dimension_type}`
  - `data/minecraft/tags/damage_type/bypasses_*.json`
- `src/test/java/`: JUnit 5 for `core`, plus an architecture test that fails if `core` imports
  `net.minecraft`, `com.mojang` or `net.fabricmc`.
- `src/gametest/java/`: Fabric GameTests (`@GameTest` with `GameTestHelper`, task `runGameTest`,
  headless).

These API names still need verifying for 26.2 at the start of the phase that uses them:
- the world preset and void generator JSON
- entity registration and the renderer render-state API
- `ServerBossEvent`
- particle provider registration

### Build steps for R0 (later releases get their own step list after each evaluation)

**R0 subset of the layout:**
- `core/`: `Fact`, `FactFamily`, `QuestionPicker` (simple), `DerivedFactTips`, `FightPolicy` (one
  level), `EscapeModel`
- `block/BombBlock`, `net/`, `game/`
- `boss/AbstractBoss` and `GraafFout`, `ThrownBomb`, `FightManager`, plus a yeet-only
  `ReactionDirector`
- `log/AnswerLog`, `command/` (`stats`), and a version handshake
- `QuestionScreen` on the client

The steps:
1. **Project and repository setup** (the first thing done after approval).
   - Create the project folder **`/home/jesper/claude-mc/tafelboem/`**. All source and docs live
     there.
   - Save this plan in it as `docs/superpowers/specs/2026-10-07-tafelboem-design.md`, plus a short
     `README.md` (what it is, how to build and deploy, the release roadmap).
   - `git init` with a **repo-local personal identity**. I'll ask Jesper for the name and email
     first, then run `git config user.name/user.email` in this repo only. Add a `.gitignore`
     covering Gradle, build, run, IDE files and `.minecraft-*`.
   - Initial commit: the plan, README and .gitignore.
   - Jesper installs the tools with `! sudo apt install openjdk-25-jdk gh`, then runs
     `! gh auth login`.
   - `gh repo create tafelboem --private --source . --remote origin --push`.
2. **Mod skeleton.**
   - Copy the fabric-example-mod `26.2` template into the folder without its git history, and
     rename it to the `tafelboem` id and package.
   - Empty mod builds, `runGameTest` is green. Commit and push.
   - Fabric profiles on this PC and the woonkamer PC.
   - `deployToWindows` (R0.1 replaced it, and the never-built NAS share, with `publishPack`).
   - The mod loads on both PCs.
3. **Session engine.**
   - Knalletje block → `RootLock` → `QuestionScreen` (digits, "?", correction and retype, tip) →
     answer → `level.explode` or a harmless penalty → release.
   - ESC → chicken.
   - JUnit for the picker and tips. A GameTest smoke test of the lock and session.
4. **Answer log and stats.**
   - JSONL writer, `/tafelboem stats`, and the version handshake.
5. **Graaf Fout.**
   - The entity on the illager model with a texture, the summon item, hover and teleport, turn-based
     throwing of `ThrownBomb`, kick-back, the boss bar, and the yeet reaction.
   - The defeat finale, the Kippenbom loot, and LAN turns.
6. **Bombs 2–3 and nl/en polish.**
   - Gewone TNT and Kippenbom with simple particle bursts.
   - Run the manual acceptance checklist on both PCs, tag `r0`, and publish. (R0 was never tagged;
     from R0.1 every publish is tagged `vX.Y.Z`.)
7. **Evaluation session with the kids.** Write it up in `docs/evaluations/R0.md`, then hold a
   backlog review with Jesper.

### Original full build order (kept as reference for planning R1+)
0. **Setup.**
   - Install JDK 25. This needs sudo, so Jesper runs `! sudo apt install openjdk-25-jdk`.
   - Run `git init`, pull in the template, and save this design as
     `docs/superpowers/specs/2026-10-07-tafelboem-design.md`.
   - Get an empty mod to build, with `runGameTest` green.
   - Jesper installs Fabric 26.2 with the Windows installer and creates a launcher profile with
     its own game dir `.minecraft-tafelboem`. The Gradle task `deployToWindows` syncs our jar and
     Fabric API into that profile's `mods`. (R0.1 moved to Prism Launcher and `publishPack`.)
   - Check that the mod loads in game.
1. **Slice.**
   - Knalletje block → root lock → question screen with a fixed question, "?" and the retype step.
   - Correct: delayed `level.explode` at the block. Wrong: a harmless pop on the player.
   - ESC: a chicken.
   - Add one GameTest.
2. **Learning core.**
   - `MasteryModel`, `SpacingScheduler`, `QuestionPicker`, `DerivedFactTips`, `DailyDose`,
     `BombTier`, `FightPolicy`, `EscapeModel` and `ArenaShape`, built TDD with JUnit.
   - The progress attachment.
   - The `/tafelboem stats|reset|give` commands with the parent report.
3. **Arena world.**
   - The world preset and void island, `ArenaBuilder` with budgeted rebuild.
   - The force field, void safety, safe zones and world rules.
   - The lobby: bell, dispenser, Tafelkaart wall, trophy wall, practice field, signs.
4. **Graaf Fout.**
   - `AbstractBoss` and `GraafFout`, the renderer and AI, `ThrownBomb`, kick-back, combo and super
     attacks.
   - Lives, ESC cap, boss bar, levels, replays, daily sleep, loot payout and fanfare.
   - Co-op with rescue sums.
   - `ReactionDirector` and `ReactionPose` with the shared reaction moves (yeet, frantic run,
     pancake, dizzy spin, cape chase), so every hit is already funny.
5. **Escape + FX foundation.**
   - Escape aids, the personal fast bonus, clean-escape and failed-escape rules.
   - `FxS2C` and `FxDirector`, `FxShapes`, the custom particles, the product grid, and the idle,
     armed, answer, mastered and loot effects.
6. **Penalties.**
   - Damage types and death messages, the inventory stash, the tiered penalties, lives.
7. **Booms.**
   - `CraterBuilder`, and for each tier its spectacle together with its particle choreography,
     including the rocket flight and the animal pen.
   - Each bomb's signature boss reaction (chicken chase, cow and pig ride, anvil pancake, rocket
     ride, meteor), damage stages and Graaf Fout's defeat finale.
   - Caps, cleanup and the config file.
8. **Bosses 2 & 3.**
   - **Tante Toverfout:** witch model and texture, broom flight, potion-bottle projectile, swamp
     arena theme, missing-factor twist, her reactions (frog, broom crash, hat chase, goo), defeat
     finale.
   - **Robo-Rekenaar 3000:** iron-golem model and texture, lab arena theme, race bar, his reactions
     (short-circuit, head spin with bolts, blue-screen reboot), and the trilogy finale with the
     "Tafelkampioen" title.
   - The boss-select bell, story lectern, story lines in both languages, and the unlock chain.
9. **Endgame + looks + polish.**
   - Endless "Ze komen terug!" mode, the hardcore lectern with the fuse bar, and the golden bomb.
   - Textures for all 9 bombs and the 3 bosses (with damage stages), plus the rocket, cluster and
     firecracker models.
   - Sounds and taunts, nl_nl and en_us, a particle polish pass, balancing.

After phase 4 the game is fully playable and teaches well, with Graaf Fout and plain booms.
Phases 5–7 make it spectacular, phase 8 adds weeks of content, and phase 9 polishes it.

### Risks to watch
- **Movement lock.** Client prediction and knockback mean the per-tick pinning is required. Ender
  pearls and elytra need guarding too.
- **Crater and rebuild cost.** The lighting and network cost of radius 32 craters, the product grid
  and the arena rebuild need measuring at tier 9.
- **Particle load.** At tier 9, particles add to the crater cost and both happen in the same window.
  The per-tick budgets and the Particles setting must keep FPS playable.
- **New APIs.** The world preset and void generator, custom entity rendering and particle
  registration are new for 26.x. Each one gets a short spike at the start of its phase.
- **Reaction poses.** The pose transforms (squash, spin, head-in-ground) depend on the 26.2
  render-state pipeline accepting extra transforms on vanilla models. This gets a spike at the start
  of phase 4. The fallback is motion plus particles only.
  The same spike covers the player's kick pose. Its fallback is the vanilla arm swing.
- **Inventory stash.** The `ItemStack` serialisation round-trip needs a GameTest.
- **Mock players.** It is unverified whether GameTest mock players survive death and respawn.
- **Wall-clock dates.** Spacing and the daily dose depend on the system date, so changing the PC
  clock games them. That is accepted.
- **LAN.** Every player needs the mod installed.

## Verification

**R0** (what we run now):
- **Build:** `./gradlew build`.
  - JUnit covers the picker (re-ask within 2, weighting toward misses, commuted pairs), the tips
    (correct for all 100 facts), the one-level fight maths, and the architecture test.
  - The GameTest smoke test covers: lock applied → correct answer → lock released and boom
    scheduled; wrong → penalty and correction state; ESC cap.
- **Deploy:** `./gradlew publishPack`; after "Live", Play on each PC installs it.
- **Manual:** the R0 acceptance checklist (11 items above) on this PC plus the woonkamer PC over LAN.
- **Evaluation:** a play session with the kids, written up in `docs/evaluations/R0.md`, then the
  go/no-go.

**Full-vision verification** (reference for R1+, to be pruned per release):
- **Unit tests.** `./gradlew test` runs JUnit on `core`:
  - mastery state transitions
  - spacing schedule
  - picker mix: about 85–90% expected success on a simulated child, at most 4 unknown facts in play,
    re-ask within 1–2 questions, easy after 2 misses, commuted pairs linked
  - derived-fact tips are correct for all 100 facts
  - daily dose
  - fight maths: HP per boss, combo, supers, lives, payouts and replays
  - the boss unlock chain and the format per boss (missing-factor only on Accurate+ facts)
  - `ReactionPicker`: never repeats, and the signature reaction is used for its tier
  - `RaceBar`: derived from the child's own median, and slower never reduces damage
  - a pacing simulation: a 60%-accurate child and a 90%-accurate child both beat Graaf Fout
    within about 150–250 questions, and the trilogy within about 400–650
  - escape maths
  - arena, crater and `FxShapes` geometry
  - the architecture test
- **GameTests.** `./gradlew runGameTest` runs headless. It covers:
  - the lock being applied and released
  - correct answer → kick-back hit → reaction plays and the boss doesn't throw during it
  - wrong answer → penalty
  - "?" → no damage
  - ESC cap
  - a 💀 costs a life and the boss keeps his damage
  - items restored after a 💀
  - void fall → arena edge
  - safe zone never carved
  - beating a level → loot payout
  - daily dose puts the bosses to sleep
  - beating Graaf Fout level 9 unlocks Tante Toverfout
  - crater cap respected (test config radius 3)
- **Play-test.** `./gradlew publishPack`, wait for "Live", press Play in the Prism instance, then
  create a new TafelBoem world. Check the following:
  - Nederlands and English
  - a first-time onboarding walk-through
  - all three bosses, levels 1–9 (`/tafelboem fight <boss> <level>` to jump ahead)
  - every reaction move and signature reaction, and that none repeats twice in a row
  - the damage stages and the three defeat finales
  - every bomb's look, product grid, boom and penalty
  - particles on both All and Minimal
  - the Tafelkaart wall changing colour
  - the parent report
  - the daily sleep
  - co-op on LAN with two players
  - FPS/TPS during Armageddon
  - **A real play-test with the kids.** Watch where they get stuck and what they laugh at, and
    tune the config.
