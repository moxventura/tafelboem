package nl.jeeninga.tafelboem.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The pack files packwiz-installer reads on the kids' PCs. A wrong hash or a missing version line
 * means no update, or worse, a broken Prism instance.
 */
class PackwizTest {
	@Test
	void sha256IsLowercaseHex() throws IOException {
		Path file = Files.createTempFile("packwiz", ".txt");
		Files.writeString(file, "abc", StandardCharsets.UTF_8);

		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Packwiz.sha256(file));
	}

	@Test
	void theIndexListsEveryFileWithItsHashSortedByPath() {
		String index = Packwiz.indexToml(Map.of(
				"mods/tafelboem.jar", "bbbb",
				"mods/fabric-api.jar", "aaaa"));

		assertEquals("""
				hash-format = "sha256"

				[[files]]
				file = "mods/fabric-api.jar"
				hash = "aaaa"

				[[files]]
				file = "mods/tafelboem.jar"
				hash = "bbbb"
				""", index);
	}

	@Test
	void thePackPointsAtTheIndexByHashAndPinsMinecraftAndFabric() {
		String pack = Packwiz.packToml("TafelBoem", "0.1.2", "cccc", "26.2", "0.19.5");

		assertEquals("""
				name = "TafelBoem"
				version = "0.1.2"
				pack-format = "packwiz:1.1.0"

				[index]
				file = "index.toml"
				hash-format = "sha256"
				hash = "cccc"

				[versions]
				minecraft = "26.2"
				fabric = "0.19.5"
				""", pack);
	}

	@Test
	void quotesAndBackslashesAreEscaped() {
		String pack = Packwiz.packToml("Tafel \"Boem\" \\o/", "1", "h", "26.2", "0.19.5");

		assertTrue(pack.contains("name = \"Tafel \\\"Boem\\\" \\\\o/\""), pack);
	}

	@Test
	void writingThePackHashesTheIndexItJustWrote(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir.resolve("mods"));
		Files.writeString(dir.resolve("mods/tafelboem.jar"), "mod");
		Files.writeString(dir.resolve("mods/fabric-api.jar"), "api");

		Packwiz.write(dir, "TafelBoem", "0.1.2", "26.2", "0.19.5");

		String index = Files.readString(dir.resolve("index.toml"));
		assertTrue(index.contains("hash = \"" + Packwiz.sha256(dir.resolve("mods/tafelboem.jar")) + "\""), index);
		assertTrue(Files.readString(dir.resolve("pack.toml")).contains("hash = \"" + Packwiz.sha256(dir.resolve("index.toml")) + "\""));
	}

	@Test
	void aFileUnderSavesIsPreservedSoAKidsWorldIsNeverOverwritten() {
		String index = Packwiz.indexToml(Map.of(
				"mods/tafelboem.jar", "bbbb",
				"saves/TafelBoem/level.dat", "dddd"));

		assertEquals("""
				hash-format = "sha256"

				[[files]]
				file = "mods/tafelboem.jar"
				hash = "bbbb"

				[[files]]
				file = "saves/TafelBoem/level.dat"
				hash = "dddd"
				preserve = true
				""", index);
	}

	@Test
	void thePreservedFilesAreReadBackFromAnIndex() {
		String index = Packwiz.indexToml(Map.of(
				"mods/tafelboem.jar", "bbbb",
				"saves/TafelBoem/level.dat", "dddd"));

		assertEquals(Set.of("saves/TafelBoem/level.dat"), Packwiz.preservedFiles(index));
	}

	@Test
	void writingThePackIndexesTheShippedWorld(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir.resolve("mods"));
		Files.writeString(dir.resolve("mods/tafelboem.jar"), "mod");
		Files.createDirectories(dir.resolve("saves/TafelBoem"));
		Files.writeString(dir.resolve("saves/TafelBoem/level.dat"), "world");

		Packwiz.write(dir, "TafelBoem", "1.1.2", "26.2", "0.19.5");

		assertEquals(Set.of("saves/TafelBoem/level.dat"), Packwiz.preservedFiles(Files.readString(dir.resolve("index.toml"))));
	}

	@Test
	void aPreservedFileNeverLeavesTheIndexBecausePackwizWouldDeleteItOnThePcs(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir.resolve("mods"));
		Files.writeString(dir.resolve("mods/tafelboem.jar"), "mod");
		Files.createDirectories(dir.resolve("saves/TafelBoem"));
		Files.writeString(dir.resolve("saves/TafelBoem/level.dat"), "world");
		Packwiz.write(dir, "TafelBoem", "1.1.2", "26.2", "0.19.5");
		String before = Files.readString(dir.resolve("index.toml"));

		Files.delete(dir.resolve("saves/TafelBoem/level.dat"));
		IllegalStateException refused = assertThrows(IllegalStateException.class,
				() -> Packwiz.write(dir, "TafelBoem", "1.1.3", "26.2", "0.19.5"));

		assertTrue(refused.getMessage().contains("saves/TafelBoem/level.dat"), refused.getMessage());
		assertEquals(before, Files.readString(dir.resolve("index.toml")), "the old index stays");
	}
}
