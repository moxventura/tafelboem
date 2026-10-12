package nl.jeeninga.tafelboem.build;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * launch: pushes the version tag of this repo (checked by {@link CheckRelease}), copies the jars
 * under fixed names, writes the packwiz files, commits, pushes, and then waits until GitHub's raw
 * CDN (cached for up to 5 minutes) serves exactly what was pushed. After "Live", pressing Play on
 * any PC installs the new version.
 */
@UntrackedTask(because = "publishes to GitHub")
public abstract class PublishPack extends DefaultTask {
	private static final Duration POLL_EVERY = Duration.ofSeconds(20);
	private static final Duration GIVE_UP_AFTER = Duration.ofMinutes(8);
	private static final String INSTANCE_ZIP = "TafelBoem.zip";

	@InputFile
	public abstract RegularFileProperty getModJar();

	@InputFiles
	public abstract ConfigurableFileCollection getFabricApi();

	/** The Prism instance new PCs import; published next to the pack, not in its index. */
	@InputFile
	public abstract RegularFileProperty getInstanceZip();

	/** A clone of the pack repo; whatever branch is checked out gets published. */
	@Internal
	public abstract DirectoryProperty getPackDir();

	/** This repo: its version tag is pushed, and the pack notes which commit it was built from. */
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
		File packClone = getPackDir().get().getAsFile();
		if (!new File(packClone, ".git").exists()) {
			throw new GradleException("No pack repo clone at " + packClone + ". Clone it with: git clone git@github.com:moxventura/tafelboem-pack.git");
		}
		Git pack = new Git(getExec(), packClone);
		String localChanges = pack.run("status", "--porcelain");
		if (!localChanges.isEmpty()) {
			throw new GradleException("The pack clone " + packClone + " has local changes; commit or discard them first:\n" + localChanges);
		}
		String branch = pack.run("rev-parse", "--abbrev-ref", "HEAD");
		if (!pack.run("ls-remote", "--heads", "origin", branch).isEmpty()) {
			pack.run("pull", "--ff-only", "-q", "origin", branch);
		}

		// The tag first: every version the PCs can download then has its tag (and commit) on GitHub.
		Git source = new Git(getExec(), getSourceDir().get().getAsFile());
		String tag = "v" + getModVersion().get();
		source.run("push", "-q", "origin", "refs/tags/" + tag);

		Path packDir = packClone.toPath();
		Path mods = packDir.resolve("mods");
		replaceMods(mods);
		Packwiz.write(packDir, getPackName().get(), getModVersion().get(), getMinecraft().get(), getFabric().get());
		Files.copy(getInstanceZip().get().getAsFile().toPath(), packDir.resolve(INSTANCE_ZIP), StandardCopyOption.REPLACE_EXISTING);

		pack.run("add", "-A", Packwiz.PACK, Packwiz.INDEX, "mods", INSTANCE_ZIP);
		if (pack.run("status", "--porcelain").isEmpty()) {
			getLogger().lifecycle("Nothing to publish: {} {} is already in the pack ({}).", getPackName().get(), getModVersion().get(), branch);
			return;
		}
		String built = "tafelboem@" + source.run("rev-parse", "--short", "HEAD");
		pack.run("commit", "-q", "-m", getPackName().get() + " " + getModVersion().get() + " (" + built + ")");
		pack.run("push", "-q", "origin", "HEAD");
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
		expected.put(INSTANCE_ZIP, Packwiz.sha256(packDir.resolve(INSTANCE_ZIP)));
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
}
