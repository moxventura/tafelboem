# TafelBoem 💥

A Minecraft Java Edition mod (Fabric, Minecraft 26.2) that helps kids practise the multiplication
tables 1–10 in a playful way: every fuse is a sum.

Right-click a bomb and you are rooted in place while a sum pops up. Answer correctly and you escape
the boom, or kick it back at the villain **Graaf Fout**. Answer wrong and the bomb goes off on you,
and then you see the correct fact big on screen and type it once.

The full design lives in
[`docs/superpowers/specs/2026-10-07-tafelboem-design.md`](docs/superpowers/specs/2026-10-07-tafelboem-design.md).

## Roadmap

The mod is delivered in small releases. Each one is play-tested with the kids and ends in a
go/no-go (see the spec):

| Release | Content | Status |
|---|---|---|
| **R0 "Graaf Fout komt!"** | Core question loop, 3 bombs, basic Graaf Fout (yeeted on every hit), LAN, answer log | 🚧 in progress |
| R1 "Leren dat blijft" | Fact states, spaced practice, Tafelkaart, parent report, levels 1–3, more reactions | backlog |
| R2 "Meer boem" | Bombs 4–9, craters, particle shows, lives, co-op rescue | backlog |
| R3 own world | TafelBoem world type and arena (only if needed) | backlog |
| R4 new villains | Tante Toverfout, Robo-Rekenaar 3000 | backlog |
| R5 endgame | Endless mode, hardcore | backlog |

Evaluations are recorded in [`docs/evaluations/`](docs/evaluations/).

## Building

Requirements: JDK 25 (`sudo apt install openjdk-25-jdk`).

```bash
./gradlew build          # compile, unit tests, GameTests
./gradlew test           # unit tests only (core learning logic)
./gradlew runGameTest    # headless in-game tests
```

## Deploying to the kids' PCs

**One-time setup per Windows PC:**
1. Install Fabric Loader for Minecraft 26.2 with the Fabric installer.
2. In the Minecraft Launcher, edit the Fabric installation and set its game directory to
   `%APPDATA%\.minecraft-tafelboem`.

**Every update:**
- `./gradlew deployToWindows` copies the mod to this PC's `.minecraft-tafelboem\mods`.
- `./gradlew publishToNas` publishes the mod to the NAS share.
- On each kids' PC, double-click `tools/tafelboem-update.bat` to pull the latest version.

The `deployToWindows` and `publishToNas` tasks and the update script are planned for R0 and do not
exist yet.

To play together, one PC opens its world to LAN and the others join. Every PC needs the same mod
version, and the mod warns when they don't match.
