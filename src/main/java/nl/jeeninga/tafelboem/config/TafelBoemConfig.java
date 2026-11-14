package nl.jeeninga.tafelboem.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import nl.jeeninga.tafelboem.core.Aim;
import nl.jeeninga.tafelboem.core.FightLevel;

/**
 * The parent's switches, in {@code config/tafelboem.json}. Every new feature gets one, so it can be
 * turned off quickly if the kids don't like it. Missing keys keep their defaults, and a broken file
 * never stops the game.
 */
public final class TafelBoemConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	/** The file's version now: 1 since R1.4, which turned {@link #buildPhase} and {@link #traps} off on every PC once. */
	static final int VERSION = 1;

	/**
	 * Which version of this file the parent's values come from; a file without it is from before R1.4 (0). Not a switch:
	 * the game sets it when it writes the file.
	 */
	public int configVersion = VERSION;

	/** After a miss, make the child retype the correct fact (R0 behaviour) instead of showing it on the HUD. */
	public boolean correctionRetype = false;

	/** Turn the view towards Graaf Fout during a fight question and while he gets yeeted. */
	public boolean cameraAssist = true;

	/**
	 * With {@link #cameraAssist}: after a right answer the view gets a short nudge towards Graaf Fout at the kick, and
	 * one when he lands out of view, and is the kid's again in between. Off, it follows him until he has landed (1.1.0).
	 */
	public boolean cameraNudge = true;

	/** Stars at the doors, after a win and in the stats. When off they are still in the fights log. */
	public boolean stars = true;

	/** In an arena the kicked bomb breaks blocks (never bedrock, obsidian or the floor near a kid). */
	public boolean arenaCraters = true;

	/** A typed wrong answer costs 1 heart on Gemiddeld and 2 on Moeilijk, never the last half heart. */
	public boolean wrongAnswerDamage = true;

	/** A win on a door level gives 4 cooked chicken, to heal the hearts; off, it gives 4 Kippenbommen as in R1.0. */
	public boolean foodReward = true;

	/**
	 * A Kippenbom kicked into Graaf Fout lets its chickens loose, and a wrong answer on one lets some hop out at
	 * the kid. Off, neither happens, whatever {@link #countingKippenbom} says, and the Kippenbom has no signature
	 * landing: no chase, so no chickens in a fight. A placed Kippenbom's boom in free play doesn't depend on it.
	 */
	public boolean kippenbomChickens = true;

	/** A Kippenbom's right answer on a × b shows a groups of b chickens; off, 12 fly as in 1.0.2. */
	public boolean countingKippenbom = true;

	/** A fast answer or right answers in a row yeet Graaf Fout further, and the kicker's meter says why. */
	public boolean yeetBoost = true;

	/** In a TafelBoem world, a yeet further than the kid's own best says "New record!" to that kid. */
	public boolean yeetRecords = true;

	/** After a yeet he may land head-first, as a pancake or running round and fly into a wall; off, the R1 yeets and dizzy spell. */
	public boolean yeetCatalogue = true;

	/** Each bomb has a landing of its own: a sore toe (Knalletje), a soot face (Gewone TNT), chasing chickens (Kippenbom). */
	public boolean bombSignatures = true;

	/** Each level's arena plays its own music: Otherside, Pigstep, "Boss". Applies from the next arena placed. */
	public boolean arenaMusic = true;

	/**
	 * Through a level door the kids first build in the plain arena, with a chest of blocks and a bell, until a
	 * countdown (5, 4 or 3 minutes by level) or the bell brings Graaf Fout. Off, as 1.1.2: a random arena, he
	 * comes 3 s after the first kid. Applies from the next world load.
	 */
	public boolean buildPhase = false;

	/**
	 * R1.3, R1.4: in an arena, a bomb that lands more than 3 blocks from the kid it was thrown at (they ran, or a wall
	 * stopped it) drops as a Terugbom a kid can catch and throw back at him, and he aims ahead of a running kid. Off, as
	 * 1.2.0: wherever it lands, its kid answers, and there is no lead, ring or throw-back. A fight outside a TafelBoem
	 * world always plays as 1.2.0.
	 */
	public boolean dodging = true;

	/**
	 * R1.3: after a right answer Graaf Fout lands in the nearest trap within 4 blocks of where he would land
	 * (cactus, sweet berry bush, magma block, cobweb, slime block, a lying bomb) for one more hit, and the trap
	 * goes. Off: no pull and no bonus; the trap blocks stay in the chest.
	 */
	public boolean traps = false;

	/**
	 * R1.3: on Peaceful and Easy nothing takes a kid in an arena (build phase and fight) below half a heart.
	 * On Normal and Hard damage is vanilla, and dying in an arena counts as leaving. Off: vanilla damage
	 * everywhere, on every difficulty. Our booms and wrong answers never hurt that much either way.
	 */
	public boolean arenaSafety = true;

	/**
	 * R1.4: kids in the arena of a running fight are in adventure mode, so nobody breaks or places blocks; their own
	 * mode comes back when the fight ends, they leave, die or log in again. Off: as 1.3.0.
	 */
	public boolean noBuildInFight = true;

	/** R1.4: when he throws, a ring of dust shows where the bomb will land (with {@link #dodging}). */
	public boolean landingRing = true;

	/** R1.4: how much of a running kid's movement he aims ahead on level 1 (0 to 1). */
	public double dodgeLead1 = 0.70;

	/** R1.4: how much of a running kid's movement he aims ahead on level 2 (0 to 1). */
	public double dodgeLead2 = 0.85;

	/** R1.4: how much of a running kid's movement he aims ahead on level 3 (0 to 1). */
	public double dodgeLead3 = 0.95;

	/** R1.4: his bomb's flight time on level 1, times 1.3.0's (smaller is quicker, harder to dodge). */
	public double dodgeFlight1 = 0.85;

	/** R1.4: his bomb's flight time on level 2, times 1.3.0's. */
	public double dodgeFlight2 = 0.75;

	/** R1.4: his bomb's flight time on level 3, times 1.3.0's. */
	public double dodgeFlight3 = 0.68;

	/** R1.4: his bomb never flies shorter than this many ticks, so there is always time to react. */
	public int dodgeMinFlightTicks = 12;

	/** What a level asks of his throw, from the parent's numbers; absurd ones are kept sane. The R0.1 fight uses level 1's. */
	public Aim.Tuning aimFor(FightLevel level) {
		int number = Math.clamp(level.number(), 1, 3);
		double lead = switch (number) {
			case 1 -> dodgeLead1;
			case 2 -> dodgeLead2;
			default -> dodgeLead3;
		};
		double flight = switch (number) {
			case 1 -> dodgeFlight1;
			case 2 -> dodgeFlight2;
			default -> dodgeFlight3;
		};
		return new Aim.Tuning(Math.clamp(lead, 0.0, 1.0), Math.clamp(flight, 0.3, 2.0), Math.max(1, dodgeMinFlightTicks));
	}

	public static TafelBoemConfig load(Path file) {
		if (!Files.exists(file)) {
			TafelBoemConfig defaults = new TafelBoemConfig();
			defaults.save(file);
			return defaults;
		}
		try {
			JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			TafelBoemConfig read = GSON.fromJson(json, TafelBoemConfig.class);
			TafelBoemConfig config = read == null ? new TafelBoemConfig() : read;
			config.migrateFrom(versionOf(json));
			// Written back, so a file from an older version shows the parent the switches added since.
			config.save(file);
			return config;
		} catch (IOException | JsonParseException | NumberFormatException e) {
			// Keep playing with the defaults; the parent can fix the file later.
			return new TafelBoemConfig();
		}
	}

	/** The version a file was written at: 0 when it says none (before R1.4). */
	private static int versionOf(JsonElement json) {
		if (json instanceof JsonObject object && object.get("configVersion") instanceof JsonElement version && version.isJsonPrimitive()) {
			return version.getAsInt();
		}
		return 0;
	}

	/**
	 * Once per PC: R1.4 turned the build phase and the traps off by default, and a file from 1.2 or 1.3 still says
	 * true, so the kids would build on. From version 1 on, the parent's own values stay.
	 */
	private void migrateFrom(int version) {
		if (version < 1) {
			buildPhase = false;
			traps = false;
		}
		configVersion = VERSION;
	}

	/** Every switch on one line, for the game log the parent shares after a playtest. */
	public String describe() {
		return new Gson().toJson(this);
	}

	private void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// Not being able to write the defaults is no reason to stop the game.
		}
	}
}
