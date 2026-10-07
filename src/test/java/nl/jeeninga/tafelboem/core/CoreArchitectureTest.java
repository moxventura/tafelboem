package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The learning core must stay pure Java so it can be unit-tested without booting Minecraft.
 */
class CoreArchitectureTest {
	private static final Path CORE_SOURCES = Path.of("src/main/java/nl/jeeninga/tafelboem/core");
	private static final List<String> FORBIDDEN = List.of("import net.minecraft", "import com.mojang", "import net.fabricmc");

	@Test
	void coreDoesNotImportMinecraftOrFabric() throws IOException {
		if (!Files.isDirectory(CORE_SOURCES)) {
			return;
		}

		try (Stream<Path> files = Files.walk(CORE_SOURCES)) {
			List<String> violations = files
					.filter(path -> path.toString().endsWith(".java"))
					.flatMap(path -> lines(path).stream()
							.filter(line -> FORBIDDEN.stream().anyMatch(line.trim()::startsWith))
							.map(line -> path.getFileName() + ": " + line.trim()))
					.toList();

			assertTrue(violations.isEmpty(), "core/ must not depend on Minecraft or Fabric:\n" + String.join("\n", violations));
		}
	}

	private static List<String> lines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
