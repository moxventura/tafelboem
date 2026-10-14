package nl.jeeninga.tafelboem.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * The parent's switches, in {@code config/tafelboem.json}. Every new feature gets one, so it can be
 * turned off quickly if the kids don't like it. Missing keys keep their defaults, and a broken file
 * never stops the game.
 */
public final class TafelBoemConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** After a miss, make the child retype the correct fact (R0 behaviour) instead of showing it on the HUD. */
	public boolean correctionRetype = false;

	/** Turn the view towards Graaf Fout during a fight question and while he gets yeeted. */
	public boolean cameraAssist = true;

	/** Stars at the doors, after a win and in the stats. When off they are still in the fights log. */
	public boolean stars = true;

	/** In an arena the kicked bomb breaks blocks (never bedrock, obsidian or the floor near a kid). */
	public boolean arenaCraters = true;

	public static TafelBoemConfig load(Path file) {
		if (!Files.exists(file)) {
			TafelBoemConfig defaults = new TafelBoemConfig();
			defaults.save(file);
			return defaults;
		}
		try {
			TafelBoemConfig config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), TafelBoemConfig.class);
			return config == null ? new TafelBoemConfig() : config;
		} catch (IOException | JsonParseException e) {
			// Keep playing with the defaults; the parent can fix the file later.
			return new TafelBoemConfig();
		}
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
