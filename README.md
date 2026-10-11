# TafelBoem 💥

A Minecraft Java Edition mod (Fabric, Minecraft 26.2) that helps kids practise the multiplication
tables 1–10 in a playful way: every fuse is a sum.

Right-click a bomb and you are rooted in place while a sum pops up. Answer correctly and you escape
the boom, or kick it back at the villain **Graaf Fout** and watch him get yeeted. Answer wrong and
the bomb goes off on you; you're free again straight away, and the correct fact shows big on your
screen while the game goes on. It comes back as a question soon after.

| Question | Correction after a miss | Graaf Fout gets yeeted |
|---|---|---|
| ![Question](docs/screenshots/r0.1-question.png) | ![Correction on the HUD](docs/screenshots/r0.1-hud-correction.png) | ![Yeet-o-meter](docs/screenshots/r0.1-yeet-o-meter.png) |

The full design lives in
[`docs/superpowers/specs/2026-10-07-tafelboem-design.md`](docs/superpowers/specs/2026-10-07-tafelboem-design.md).

## Roadmap

The mod is delivered in small releases. Each one is play-tested with the kids and ends in a
go/no-go (see the spec):

| Release | Content | Status |
|---|---|---|
| **R0 "Graaf Fout komt!"** | Core question loop, 3 bombs, basic Graaf Fout (yeeted on every hit), LAN, answer log | ✅ playtest 1 done |
| **R0.1 "Beter mikken"** | Fixes from playtest 1: clearer question screen, correction on the HUD, distinct bombs, camera assist, a simple kick, more yeets, world setup, better stats | 🚧 in progress |
| R1 "Kies je level" | Hub with level doors, premade arenas (also built by the kids), yeet catalogue, counting Kippenbom, dodgeable bombs, Graaf Fout eats your mistake | backlog |
| R2 "Leren dat blijft" | Fact states, spaced practice, Tafelkaart, parent report, daily limit, "Kies je bom", leg-kick animation | backlog |
| R3 "Meer boem" | Bombs 4–9, craters, particle shows, lives, co-op rescue | backlog |
| R4 new villains | Tante Toverfout, Robo-Rekenaar 3000 | backlog |
| R5 endgame | Endless mode, hardcore | backlog |

Evaluations are recorded in [`docs/evaluations/`](docs/evaluations/).

## Building

Requirements: JDK 25 (`sudo apt install openjdk-25-jdk`).

```bash
./gradlew build          # compile, unit tests, GameTests
./gradlew test           # unit tests only (core learning logic)
./gradlew runGameTest    # headless in-game tests
./gradlew runClientGameTest   # plays through in a real client and takes screenshots
                              # (build/run/clientGameTest/screenshots; needs a display, e.g. WSLg)
```

## Installing on the kids' PCs (Prism Launcher)

**One-time setup per Windows PC:**
1. Install [Prism Launcher](https://prismlauncher.org) (`winget install PrismLauncher.PrismLauncher`)
   and add the Microsoft account that owns Minecraft. Let Prism download Java 25 when it asks.
2. **Add Instance** → **Vanilla** → version **26.2**, mod loader **Fabric 0.19.5**, name it
   `TafelBoem`.
3. Right-click the instance → **Folder**, and copy `tafelboem-<version>.jar` and
   `fabric-api-0.161.0+26.2.jar` into `minecraft\mods`.
4. Start it, create a world with cheats on, stand where the fight should be and type
   `/tafelboem arena`: always day, no weather, no monsters, and an arena floor under your feet.
5. For more PCs: right-click the instance → **Export** → Prism zip, and on the other PC
   **Add Instance** → **Import**. Fabric, the mods and the world come along.

**Every update on this PC:**

```bash
./gradlew deployToWindows -PwindowsGameDir=/mnt/c/Users/jeejes/AppData/Roaming/PrismLauncher/instances/TafelBoem/minecraft
```

This syncs the mod and Fabric API into the instance's `mods` folder. Without `-PwindowsGameDir` it
targets `%APPDATA%\.minecraft-tafelboem`. For the other PCs, export the instance again. A
`publishToNas` task and an update script are planned once the NAS share path is known.

To play together, one PC opens its world to LAN and the others join. Every PC needs the same mod
version, and the mod warns when they don't match.

**Parent switches** live in the instance's `config/tafelboem.json` (written on first start):

| Switch | Default | What it does |
|---|---|---|
| `correctionRetype` | `false` | `true` brings back the R0 screen where the child retypes the correct fact after a miss |
| `cameraAssist` | `true` | Turns the view to Graaf Fout during a fight question and while he gets yeeted |

`/tafelboem stats [player]` shows accuracy and typical time per table, the hardest facts, how often a
missed fact is right when it comes back, and how often "?" was probably pressed by accident.
