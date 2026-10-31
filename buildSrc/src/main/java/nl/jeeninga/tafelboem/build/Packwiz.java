package nl.jeeninga.tafelboem.build;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Writes the packwiz pack files that packwiz-installer reads before every launch on the kids' PCs:
 * {@code index.toml} with a sha256 per file, and {@code pack.toml} pointing at the index by hash
 * and pinning the Minecraft and Fabric Loader versions. Those two must match the Prism instance
 * exactly, or packwiz rewrites the instance's {@code mmc-pack.json}.
 *
 * <p>Files under {@code saves/} (the ready-made TafelBoem world) are marked {@code preserve}:
 * packwiz-installer then only installs them when they are missing and never overwrites the kids'
 * world. A file that leaves the index is deleted on the PCs, so a preserved file may never leave it.
 */
public final class Packwiz {
	public static final String INDEX = "index.toml";
	public static final String PACK = "pack.toml";
	/** The ready-made world, installed once and then the kids' own. */
	public static final String SAVES = "saves";

	private Packwiz() {
	}

	public static String sha256(Path file) {
		try (DigestInputStream in = new DigestInputStream(Files.newInputStream(file), MessageDigest.getInstance("SHA-256"))) {
			in.transferTo(OutputStream.nullOutputStream());
			return HexFormat.of().formatHex(in.getMessageDigest().digest());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** One {@code [[files]]} entry per path, sorted, so an unchanged pack gives an unchanged file. */
	public static String indexToml(Map<String, String> hashes) {
		StringBuilder toml = new StringBuilder("hash-format = \"sha256\"\n");
		new TreeMap<>(hashes).forEach((file, hash) -> toml
				.append("\n[[files]]\n")
				.append("file = ").append(quote(file)).append('\n')
				.append("hash = ").append(quote(hash)).append('\n')
				.append(isPreserved(file) ? "preserve = true\n" : ""));
		return toml.toString();
	}

	private static boolean isPreserved(String file) {
		return file.startsWith(SAVES + "/");
	}

	private static final Pattern ENTRY = Pattern.compile("\\[\\[files]]\\s*file = \"([^\"]*)\"[^\\[]*?preserve = true");

	/** The files an index marks {@code preserve}, as written by {@link #indexToml}. */
	public static Set<String> preservedFiles(String index) {
		Set<String> files = new LinkedHashSet<>();
		Matcher matcher = ENTRY.matcher(index);
		while (matcher.find()) {
			files.add(matcher.group(1));
		}
		return files;
	}

	public static String packToml(String name, String version, String indexHash, String minecraft, String fabric) {
		return "name = " + quote(name) + "\n"
				+ "version = " + quote(version) + "\n"
				+ "pack-format = \"packwiz:1.1.0\"\n"
				+ "\n[index]\n"
				+ "file = \"" + INDEX + "\"\n"
				+ "hash-format = \"sha256\"\n"
				+ "hash = " + quote(indexHash) + "\n"
				+ "\n[versions]\n"
				+ "minecraft = " + quote(minecraft) + "\n"
				+ "fabric = " + quote(fabric) + "\n";
	}

	/**
	 * Indexes everything under {@code mods/} and {@code saves/} and writes both files into
	 * {@code packDir}. Refuses, and leaves the old index as it was, when a file the old index
	 * preserves is gone.
	 */
	public static void write(Path packDir, String name, String version, String minecraft, String fabric) {
		Map<String, String> hashes = new TreeMap<>();
		try {
			for (String folder : List.of("mods", SAVES)) {
				Path root = packDir.resolve(folder);
				if (!Files.isDirectory(root)) {
					continue;
				}
				try (Stream<Path> files = Files.walk(root)) {
					files.filter(Files::isRegularFile)
							.forEach(file -> hashes.put(packDir.relativize(file).toString().replace('\\', '/'), sha256(file)));
				}
			}
			Path index = packDir.resolve(INDEX);
			if (Files.exists(index)) {
				Set<String> dropped = new LinkedHashSet<>(preservedFiles(Files.readString(index, StandardCharsets.UTF_8)));
				dropped.removeAll(hashes.keySet());
				if (!dropped.isEmpty()) {
					throw new IllegalStateException("These preserved files would leave the pack, and packwiz would delete them"
							+ " from the kids' PCs: " + dropped + ". Put them back.");
				}
			}
			Files.writeString(index, indexToml(hashes), StandardCharsets.UTF_8);
			Files.writeString(packDir.resolve(PACK), packToml(name, version, sha256(index), minecraft, fabric), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String quote(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
