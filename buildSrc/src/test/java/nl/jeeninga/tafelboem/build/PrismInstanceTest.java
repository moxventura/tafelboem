package nl.jeeninga.tafelboem.build;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Prism instance a new PC imports from a URL: Minecraft and Fabric at the pack's versions, and
 * packwiz run before every launch.
 */
class PrismInstanceTest {
	private static final String PACK_URL = "https://raw.githubusercontent.com/moxventura/tafelboem-pack/main/pack.toml";

	@TempDir
	Path dir;
	private Path bootstrap;
	private Path installer;

	@BeforeEach
	void packwizJars() throws IOException {
		bootstrap = Files.writeString(dir.resolve("packwiz-installer-bootstrap.jar"), "bootstrap");
		installer = Files.writeString(dir.resolve("packwiz-installer.jar"), "installer");
	}

	@Test
	void theZipHoldsTheInstanceFilesAndBothPackwizJarsInTheGameFolder() throws IOException {
		Map<String, byte[]> zip = read(build("a.zip"));

		assertEquals(java.util.List.of("instance.cfg", "mmc-pack.json", "minecraft/packwiz-installer-bootstrap.jar", "minecraft/packwiz-installer.jar"),
				java.util.List.copyOf(zip.keySet()));
		assertArrayEquals("bootstrap".getBytes(StandardCharsets.UTF_8), zip.get("minecraft/packwiz-installer-bootstrap.jar"));
	}

	@Test
	void packwizRunsBeforeEveryLaunch() throws IOException {
		String cfg = text(build("a.zip"), "instance.cfg");

		assertTrue(cfg.contains("\nOverrideCommands=true\n"), cfg);
		assertTrue(cfg.contains("\nPreLaunchCommand=\"$INST_JAVA\" -jar packwiz-installer-bootstrap.jar --bootstrap-no-update " + PACK_URL + "\n"), cfg);
		assertTrue(cfg.contains("\nname=TafelBoem\n"), cfg);
		// Prism's plain key=value reader treats backslashes as escapes; there must be none.
		assertFalse(cfg.contains("\\"), cfg);
	}

	@Test
	void minecraftAndFabricMatchThePack() throws IOException {
		String pack = text(build("a.zip"), "mmc-pack.json");

		assertTrue(pack.contains("\"uid\": \"net.minecraft\",\n            \"version\": \"26.2\""), pack);
		assertTrue(pack.contains("\"uid\": \"net.fabricmc.intermediary\",\n            \"version\": \"26.2\""), pack);
		assertTrue(pack.contains("\"uid\": \"net.fabricmc.fabric-loader\",\n            \"version\": \"0.19.5\""), pack);
	}

	@Test
	void buildingTwiceGivesTheSameBytes() throws IOException {
		// Otherwise every publish would commit a "changed" zip.
		assertArrayEquals(Files.readAllBytes(build("a.zip")), Files.readAllBytes(build("b.zip")));
	}

	private Path build(String name) throws IOException {
		Path zip = dir.resolve(name);
		PrismInstance.writeZip(zip, "TafelBoem", "26.2", "0.19.5", PACK_URL, bootstrap, installer);
		return zip;
	}

	private static String text(Path zip, String entry) throws IOException {
		return new String(read(zip).get(entry), StandardCharsets.UTF_8);
	}

	private static Map<String, byte[]> read(Path zip) throws IOException {
		Map<String, byte[]> entries = new LinkedHashMap<>();
		try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
			for (ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
				entries.put(entry.getName(), in.readAllBytes());
			}
		}
		return entries;
	}
}
