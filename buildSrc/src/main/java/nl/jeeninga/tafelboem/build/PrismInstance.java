package nl.jeeninga.tafelboem.build;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the Prism Launcher instance zip that a new PC imports from the pack repo: Minecraft and
 * Fabric at the pack's versions, both packwiz jars in the game folder, and packwiz as the pre-launch
 * command, so every Play first installs the newest published TafelBoem.
 *
 * <p>Prism finds an instance by its {@code instance.cfg}. Without a {@code ConfigVersion} line it
 * reads plain {@code key=value} lines and treats backslashes as escapes, so the values must not
 * contain any. A missing intermediary would be added by Prism itself; it is listed anyway.
 */
public final class PrismInstance {
	/** A fixed timestamp, so the same input always gives the same zip bytes. */
	private static final LocalDateTime ZIP_TIME = LocalDateTime.of(1980, 2, 1, 0, 0);

	private PrismInstance() {
	}

	public static void writeZip(Path zip, String name, String minecraft, String fabric, String packUrl, Path bootstrap, Path installer) {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
			put(out, "instance.cfg", instanceCfg(name, packUrl).getBytes(StandardCharsets.UTF_8));
			put(out, "mmc-pack.json", mmcPack(minecraft, fabric).getBytes(StandardCharsets.UTF_8));
			put(out, "minecraft/" + bootstrap.getFileName(), Files.readAllBytes(bootstrap));
			put(out, "minecraft/" + installer.getFileName(), Files.readAllBytes(installer));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static String instanceCfg(String name, String packUrl) {
		String command = "\"$INST_JAVA\" -jar packwiz-installer-bootstrap.jar --bootstrap-no-update " + packUrl;
		if ((name + command).contains("\\")) {
			throw new IllegalArgumentException("Prism would read a backslash as an escape: " + command);
		}
		return "InstanceType=OneSix\n"
				+ "name=" + name + "\n"
				+ "OverrideCommands=true\n"
				+ "PreLaunchCommand=" + command + "\n";
	}

	static String mmcPack(String minecraft, String fabric) {
		return """
				{
				    "components": [
				        {
				            "important": true,
				            "uid": "net.minecraft",
				            "version": "%1$s"
				        },
				        {
				            "dependencyOnly": true,
				            "uid": "net.fabricmc.intermediary",
				            "version": "%1$s"
				        },
				        {
				            "uid": "net.fabricmc.fabric-loader",
				            "version": "%2$s"
				        }
				    ],
				    "formatVersion": 1
				}
				""".formatted(minecraft, fabric);
	}

	private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setTimeLocal(ZIP_TIME);
		out.putNextEntry(entry);
		out.write(bytes);
		out.closeEntry();
	}
}
