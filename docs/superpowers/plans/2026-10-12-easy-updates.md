# Easy updates (Prism Launcher + packwiz) Implementation Plan

> **For agentic workers:** steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new version, press Play, test or play again, on both PCs. No copying of jars,
and both PCs always run the same version for LAN play.

**Architecture:** A public pack repo,
[`moxventura/tafelboem-pack`](https://github.com/moxventura/tafelboem-pack), holds only the built
jars plus packwiz metadata. Each PC plays in a Prism Launcher instance whose pre-launch command runs
packwiz-installer against that repo's raw `main/pack.toml`. `./gradlew publishPack` builds, tests,
publishes, and waits until GitHub's raw CDN serves every file.

**Decisions (2026-10-12):**
- **Hosting.** A public GitHub repo; the Raspberry Pi was the other option.
- **Updating.** Automatic on every Play, with packwiz (the standard tool) rather than our own
  script.
- **Waiting.** Up to ~5 minutes after publishing is fine.
- **Timing.** Build it before playtest 2.

**Why this simple:**
- **The cache.** raw.githubusercontent caches each URL for up to 5 minutes, separately per URL.
- **Pressing Play during that window.** You can get a mix of old and new files. packwiz's hash
  check refuses the mix and leaves the installed version untouched, because the file names are
  fixed and nothing is deleted first.
- **The rule.** Wait for "Live", then press Play.
- **Dropped.** We considered sha-pinning with a PowerShell wrapper and a new file name per publish,
  and dropped both: their only purpose was to make updates faster than the cache allows.

## Tasks

### Task 1: Pack repo
- [x] `gh repo create moxventura/tafelboem-pack --public`, README with usage and licence notices
      (TafelBoem all rights reserved, Fabric API Apache-2.0, packwiz MIT).
- [x] Clone to `/home/jesper/claude-mc/tafelboem-pack` with the repo-local git identity.

### Task 2: Publishing from the build
- [x] `buildSrc` holds `Packwiz`, which hashes files and writes `index.toml` and `pack.toml` with
      `[versions]` minecraft and fabric from `gradle.properties`. TDD: `PackwizTest` (5 tests).
- [x] `PublishPack` is a typed task with `ExecOperations`, so it works with the configuration
      cache. It:
  - depends on `build`;
  - requires a clean pack clone and fast-forwards it;
  - copies the jars to `mods/tafelboem.jar` and `mods/fabric-api.jar`;
  - writes the packwiz files, then commits "TafelBoem <version> (tafelboem@<sha>[-dirty])" and pushes;
  - polls raw every 20 s until every file matches, then prints "Live after N s".
- [x] `DownloadVerified` and `prismKit` download the pinned packwiz-installer v0.5.14 and
      bootstrap v0.0.3 into `build/prism`, refusing them on a sha256 mismatch.
- [x] `deployToWindows` is removed; it would fight packwiz over the mods folder.

### Task 3: End-to-end in WSL (Java 25, CLI mode `-g`, throwaway `e2e` branch, fake `mmc-pack.json`)
- [x] **Publish A, then install.** Both jars arrive with matching hashes, `packwiz.json` is written,
      and `mmc-pack.json` is unchanged.
- [x] **Publish B (cache warm).** It went "Live" after 263 s. At 242 s the index was already new
      while `pack.toml` and the jar were still old, which is why `publishPack` checks every file.
- [x] **Update to B.** Only `tafelboem.jar` downloads, and Fabric API stays cached. A second run says
      "already up to date".
- [x] **Stale mixes from a local server:**
  - `pack` B + `index` A gives "index file hash is invalid", and A stays installed.
  - `pack`/`index` B + jar A gives "Hash invalid!", and A stays installed.
  - All B then installs B.
- [x] Delete the `e2e` branch.

### Task 4: The instance template, built without Prism (revised 2026-10-12)
This PC doesn't play, so it gets no Prism. The kids play on the woonkamer PC and a second PC.
- [x] `PrismInstance` (TDD, `PrismInstanceTest`, 4 tests) writes `TafelBoem.zip`. It is
      byte-for-byte reproducible and contains:
  - `instance.cfg`, plain `key=value` with no backslashes: `OverrideCommands=true` and the packwiz
    pre-launch command;
  - `mmc-pack.json`: Minecraft 26.2, intermediary 26.2, Fabric Loader 0.19.5;
  - both packwiz jars in `minecraft/`.
- [x] `./gradlew prismInstance` builds it. `publishPack` puts it in the pack repo next to the pack,
      not in its index.
- [x] Checked with real Prism 11.1.1. The Linux AppImage, verified against the release's SHA-1, ran
      in WSL with a throwaway data folder, and `xdotool` pressed Enter on its dialogs.
  - The import recognised the zip and unpacked all four files.
  - The re-saved `instance.cfg` kept the command and `OverrideCommands=true`.
  - Prism's catalogue has all three component versions.
  - The instance is named after the zip, so the zip is called `TafelBoem.zip`.
  - Not covered: launching, which needs a Microsoft account. The packwiz side was tested in Task 3.

### Task 5: The two play PCs (Jesper does the clicks)
- [ ] Woonkamer PC (wake it with `ssh nas "~/wake_pc.sh"`):
  - `winget install PrismLauncher.PrismLauncher`, then log in.
  - Add Instance → Import →
    `https://raw.githubusercontent.com/moxventura/tafelboem-pack/main/TafelBoem.zip`, then Play.
  - The packwiz window installs the mods, and `/tafelboem stats` works.
- [ ] Second PC: the same three steps.
- [ ] A LAN game between both shows no version warning (R0.1 checklist row 10).
- [ ] With the network off, "Continue without updating" starts the installed version.

### Task 6: Docs
- [x] Spec: the "Deploy to the kids' PCs" section, the R0.1 "Easy updates" bullet, acceptance
      criterion 10, the definition of done, and the deploy lines.
- [x] README: playing, publishing, rules of thumb, and the instance template.
- [x] `docs/evaluations/R0.1.md`: checklist row 10; columns are now "Woonkamer PC" and "Second PC".

## Known limits
- **Wait for "Live" after publishing** (measured: 263 s). For an instant look, `./gradlew runClient`
  runs the game in WSL.
- **Internet.** Updates need it; offline play needs one click in the packwiz window.
- **Public repo.** Anyone can download the jars (accepted).
- **Version bumps.** A Minecraft or Fabric Loader bump takes two Plays, because Prism reads
  `mmc-pack.json` before packwiz updates it.
