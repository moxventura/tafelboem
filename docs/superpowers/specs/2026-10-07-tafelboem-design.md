# TafelBoem — a Minecraft mod for practising times tables with TNT

## Context

Jesper wants a Minecraft Java Edition mod that helps his kids learn the multiplication tables
(1–10) playfully, rewarded by increasingly ridiculous TNT. `/home/jesper/claude-mc` is empty (not a
git repo). The project will live in its own subfolder, `/home/jesper/claude-mc/tafelboem/`, as a
git repo pushed to a **private GitHub repo `tafelboem`**. Minecraft Java up to 26.2 is installed on Windows (Microsoft Store launcher,
`C:\Users\jeejes\AppData\Roaming\.minecraft`, no mod loader). WSL is Debian 13 with no JDK.

The design was shaped with Jesper and then reviewed from three angles: a kid's playthrough, a
teacher's view and gamification theory. Decisions:
- **Delivered in small releases, each evaluated with the kids.**
  - **R0 POC** is the core question loop plus a simple Graaf Fout who gets **yeeted on every
    hit**, played over LAN on the woonkamer PC and the other PCs.
  - The rest of the design is a backlog, re-prioritised after every evaluation.
- **Fabric on Minecraft 26.2.** UI follows the game language (nl_nl + en_us).
- **"Bomb in your lap" loop.** Every fuse is a sum and you are rooted in place while answering.
  Right means you strike back or escape. Wrong means the bomb goes off on you.
- **A TafelBoem world type.** The whole world is a floating arena island, so there is nowhere to
  wander off to.
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
| **R1 "Leren dat blijft"** | fact states and spaced picker, a Tafelkaart screen (GUI), parent report, levels 1–3 with loot, more reactions (frantic run, pancake, dizzy), the product grid, the daily limit | The practice spreads out and facts move toward Fluent; levels and loot add pull | 2 weeks of logs: % Accurate/Fluent up, median time down, sessions spread over days, kids still asking |
| **R2 "Meer boem"** | bombs 4–9, levels 4–9, particle choreographies, craters, escape aids, lethal tiers with 3 lives, inventory stash, signature reactions, co-op rescue sums | Escalation keeps them climbing without frustration | level reached per week, favourite bombs, no tears at levels 7–9, FPS OK on the woonkamer PC |
| R3 own world *(only if needed)* | the TafelBoem world type, arena island, lobby, Tafelkaart wall in blocks | Only if R0–R2 show them actually wandering off | observation |
| R4 new villains *(when they ask for it)* | Tante Toverfout, then Robo-Rekenaar 3000 | Pulled by the kids beating Graaf Fout | |
| R5 endgame | endless mode, hardcore, golden bomb, polish | | |

**Evaluation after every release** (about 30 minutes):
1. Watch a play session without helping. Tally confusion, laughter and frustration moments.
2. Ask each kid 3 questions: *Wat was het leukst? Wat was stom of moeilijk? Wat wil je erbij?*
3. Review the answer log (`/tafelboem stats` plus the JSONL file).
4. Re-prioritise the backlog. The kids vote on the next bomb or villain.

**Definition of done for each release:**
- `./gradlew build` is green: core JUnit tests plus a GameTest smoke test of the session engine.
- The manual checklist for that release is done.
- nl and en are complete.
- It is deployed to every kids' PC through the update script.
- Every new feature has a config toggle, so we can switch it off quickly if the kids dislike it.
- The release has a version tag.

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
  shows a friendly "Update TafelBoem op deze PC" message.

**Out of R0 (backlog):**
- bombs 4–9, craters, choreographies
- the world type and arena
- lethal penalties and lives
- spacing across days, the daily limit, the Tafelkaart
- other reactions, other bosses

**Where to play R0.** In a fresh **superflat** world that you create, opened to LAN. The kids
don't need a custom world type to test the loop.

**Deploy to the kids' PCs:**
- `./gradlew deployToWindows` syncs to this PC's `.minecraft-tafelboem/mods`, for your own testing.
- `./gradlew publishToNas` copies our jar and the Fabric API jar into a NAS share folder. The
  share path gets decided during R0 setup.
- `tools/tafelboem-update.bat` is double-clicked on each kids' PC. It mirrors the NAS folder into
  that PC's `%APPDATA%\.minecraft-tafelboem\mods` using `robocopy /MIR`.
- **One-time setup per PC:** install Fabric 26.2 with the installer, then create a launcher profile
  with game dir `.minecraft-tafelboem`.
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

## Product vision (the backlog for R1+, re-prioritised after every evaluation)

### The TafelBoem world
- **World type.** Pick "TafelBoem" at world creation. You spawn on a **floating arena island**
  (radius ≈ 56, about 24 blocks thick) above the void. The game mode is forced to Survival.
- **Lobby (protected from craters):**
  - The **bell** selects the **boss and level** from anything unlocked. Locked bosses show as
    silhouettes with a one-line story teaser.
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
- **Force field.** A glowing particle wall at the edge pushes you back inward.
- **Void safety.** A fall during a fight returns you to the arena edge. Outside a fight it returns
  you to the lobby. Neither kills you.
- **Safe floor.** No block within about 4 blocks of a player in a fight is ever carved. Craters
  open on the boss's side.
- **Self-repair.** After every fight the arena rebuilds itself procedurally, under a per-tick
  budget, while the victory celebration plays.
- **Arena themes.** Each boss has an arena theme, which is a block palette plus props, applied when
  a fight with that boss starts:
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
  - **Correct:** you **kick it back** for 1 hit.
  - **Clean escape:** if the blast would have reached you and you get out in time, you get +1 hit.
  - **Failed escape:** you are only launched, harmlessly. **A correct answer never hurts you.**
  - **Wrong:** the tier's penalty hits you.
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
- **Shared reaction moves** (used by every boss, with that boss's props):
  - **Yeet:** launched with a spin, landing head-first stuck in the ground, legs wiggling, then pops out.
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
| 3 | Kippenbom / Chicken Bomb | white feathered box, beak, red comb | blast + a flock of chickens flies out | none needed | bucket of water from the sky + slowness, 3 s |
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

### Feedback & the error loop (on the question screen, not in chat)
- **Screen and input:** huge digits, number keys plus Enter, no mouse needed. The number pad stays
  clickable.
- **"?" button (Ik weet het niet).** It does no damage. It shows the fact and a tip, and the fact
  comes back soon. It replaces guessing.
- **After a wrong answer, "?", ESC or a 💀:**
  - The correct fact appears big (`7 × 8 = 56`) and the child **types it once** to continue.
  - A derived-fact tip shows how to work it out, for example "7 × 8 = 5 × 8 + 2 × 8" or
    "×9 = ×10 − ×1".
  - It also shows "8 × 7 is dezelfde!".
  - The fact is asked again 1–2 questions later and once more at the end of the session.
- **Typing baseline.** The retype step measures the child's own typing speed, so response times
  exclude typing.

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
  - how often "?" is used
  - a flag for fast wrong answers on Fluent facts, which suggests deliberate deaths

### Performance guard-rails
- **Craters** are carved over several ticks under a per-tick block budget, in loaded chunks only.
  They drop no items and leave bedrock, block entities, the lobby and player safe zones alone.
  The radius cap is set in the config (default 32).
- **Arena rebuild** and the product grid also run under per-tick budgets.
- **Spectacle entities** are tagged, capped in number, and cleaned up on timeout and when the
  world loads.

### Parked for v2
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
   - `deployToWindows`, `publishToNas` and `tools/tafelboem-update.bat`.
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
   - Run the manual acceptance checklist on both PCs, tag `r0`, and publish.
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
     Fabric API into that profile's `mods`.
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
- **Deploy:** `./gradlew deployToWindows publishToNas`, then run `tafelboem-update.bat` on the
  woonkamer PC.
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
- **Play-test.** `./gradlew build deployToWindows`, then create a new TafelBoem world in the
  Fabric profile on Windows. Check the following:
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
