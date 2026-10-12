package nl.jeeninga.tafelboem.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.process.ExecOperations;

/**
 * Publishes the built mod to the public pack repo that Prism's packwiz updater reads before every
 * launch: copies the jars under fixed names, writes the packwiz files, commits, pushes, and then
 * waits until GitHub's raw CDN (cached for up to 5 minutes) serves exactly what was pushed. After
 * "Live", pressing Play on any PC installs the new version.
 */
@UntrackedTask(because = "publishes to GitHub")
public abstract class PublishPack extends DefaultTask {
	private static final Duration POLL_EVERY = Duration.ofSeconds(20);
	private static final Duration GIVE_UP_AFTER = Duration.ofMinutes(8);

	@InputFile
	public abstract RegularFileProperty getModJar();

	@InputFiles
	public abstract ConfigurableFileCollection getFabricApi();

	/** A clone of the pack repo; whatever branch is checked out gets published. */
	@Internal
	public abstract DirectoryProperty getPackDir();

	/** This repo, to note which commit a pack was built from. */
	@Internal
	public abstract DirectoryProperty getSourceDir();

	@Input
	public abstract Property<String> getPackName();

	@Input
	public abstract Property<String> getModVersion();

	@Input
	public abstract Property<String> getMinecraft();

	@Input
	public abstract Property<String> getFabric();

	/** For example {@code https://raw.githubusercontent.com/moxventura/tafelboem-pack}. */
	@Input
	public abstract Property<String> getRawUrl();

	@Inject
	protected abstract ExecOperations getExec();

	@TaskAction
	public void publish() throws IOException, InterruptedException {
		File pack = getPackDir().get().getAsFile();
		if (!new File(pack, ".git").exists()) {
			throw new GradleException("No pack repo clone at " + pack + ". Clone it with: git clone git@github.com:moxventura/tafelboem-pack.git");
		}
		String localChanges = git(pack, "status", "--porcelain");
		if (!localChanges.isEmpty()) {
			throw new GradleException("The pack clone " + pack + " has local changes; commit or discard them first:\n" + localChanges);
		}
		String branch = git(pack, "rev-parse", "--abbrev-ref", "HEAD");
		if (!git(pack, "ls-remote", "--heads", "origin", branch).isEmpty()) {
			git(pack, "pull", "--ff-only", "-q", "origin", branch);
		}

		Path packDir = pack.toPath();
		Path mods = packDir.resolve("mods");
		replaceMods(mods);
		Packwiz.write(packDir, getPackName().get(), getModVersion().get(), getMinecraft().get(), getFabric().get());

		git(pack, "add", "-A", Packwiz.PACK, Packwiz.INDEX, "mods");
		if (git(pack, "status", "--porcelain").isEmpty()) {
			getLogger().lifecycle("Nothing to publish: {} {} is already in the pack ({}).", getPackName().get(), getModVersion().get(), branch);
			return;
		}
		File source = getSourceDir().get().getAsFile();
		String built = "tafelboem@" + git(source, "rev-parse", "--short", "HEAD")
				+ (git(source, "status", "--porcelain").isEmpty() ? "" : "-dirty");
		git(pack, "commit", "-q", "-m", getPackName().get() + " " + getModVersion().get() + " (" + built + ")");
		git(pack, "push", "-q", "origin", "HEAD");
		getLogger().lifecycle("Pushed {} {} ({}) to branch {}. Waiting until GitHub serves it...", getPackName().get(), getModVersion().get(), built, branch);

		waitUntilLive(packDir, branch);
	}

	/** Fixed names, so an update replaces the jars in place and never deletes one first. */
	private void replaceMods(Path mods) throws IOException {
		Files.createDirectories(mods);
		try (Stream<Path> old = Files.list(mods)) {
			for (Path file : old.toList()) {
				Files.delete(file);
			}
		}
		List<File> api = new ArrayList<>(getFabricApi().getFiles());
		if (api.size() != 1) {
			throw new GradleException("Expected exactly one Fabric API jar, got " + api);
		}
		Files.copy(getModJar().get().getAsFile().toPath(), mods.resolve("tafelboem.jar"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(api.getFirst().toPath(), mods.resolve("fabric-api.jar"), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Polls the same URLs the PCs use until every file matches what was pushed. Each URL is cached
	 * separately, so checking only pack.toml could report "live" while an old jar is still served.
	 */
	private void waitUntilLive(Path packDir, String branch) throws IOException, InterruptedException {
		Map<String, String> expected = new TreeMap<>();
		expected.put(Packwiz.PACK, Packwiz.sha256(packDir.resolve(Packwiz.PACK)));
		expected.put(Packwiz.INDEX, Packwiz.sha256(packDir.resolve(Packwiz.INDEX)));
		try (Stream<Path> mods = Files.list(packDir.resolve("mods"))) {
			for (Path jar : mods.toList()) {
				expected.put("mods/" + jar.getFileName(), Packwiz.sha256(jar));
			}
		}

		String base = getRawUrl().get() + "/" + branch + "/";
		long start = System.nanoTime();
		try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build()) {
			while (true) {
				List<String> stale = new ArrayList<>();
				for (Map.Entry<String, String> file : expected.entrySet()) {
					if (!file.getValue().equals(servedHash(http, base + file.getKey()))) {
						stale.add(file.getKey());
					}
				}
				long seconds = Duration.ofNanos(System.nanoTime() - start).toSeconds();
				if (stale.isEmpty()) {
					getLogger().lifecycle("Live after {} s: press Play.", seconds);
					return;
				}
				if (Duration.ofSeconds(seconds).compareTo(GIVE_UP_AFTER) >= 0) {
					getLogger().warn("Not live after {} s; GitHub still serves an old {}. Press Play a bit later.", seconds, String.join(", ", stale));
					return;
				}
				getLogger().lifecycle("  {} s: still old: {}", seconds, String.join(", ", stale));
				Thread.sleep(POLL_EVERY);
			}
		}
	}

	private static String servedHash(HttpClient http, String url) throws InterruptedException {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build();
			HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				return "HTTP " + response.statusCode();
			}
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(response.body()));
		} catch (IOException e) {
			return e.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private String git(File dir, String... args) {
		List<String> command = new ArrayList<>(List.of("git", "-C", dir.getAbsolutePath()));
		command.addAll(List.of(args));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		int exit = getExec().exec(spec -> {
			spec.commandLine(command);
			spec.setStandardOutput(out);
			spec.setErrorOutput(err);
			spec.setIgnoreExitValue(true);
		}).getExitValue();
		if (exit != 0) {
			throw new GradleException(String.join(" ", command) + " failed (" + exit + "): " + err.toString(StandardCharsets.UTF_8).trim());
		}
		return out.toString(StandardCharsets.UTF_8).trim();
	}
}
