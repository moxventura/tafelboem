package nl.jeeninga.tafelboem.build;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Downloads a pinned file and refuses it unless its sha256 matches, so the packwiz jars we hand
 * out with the Prism instance are exactly the release we checked.
 */
public abstract class DownloadVerified extends DefaultTask {
	@Input
	public abstract Property<String> getUrl();

	@Input
	public abstract Property<String> getSha256();

	@OutputFile
	public abstract RegularFileProperty getOutput();

	@TaskAction
	public void download() throws IOException, InterruptedException {
		Path target = getOutput().get().getAsFile().toPath();
		Path partial = target.resolveSibling(target.getFileName() + ".download");
		Files.createDirectories(target.getParent());
		try (HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build()) {
			HttpResponse<Path> response = http.send(HttpRequest.newBuilder(URI.create(getUrl().get())).build(), HttpResponse.BodyHandlers.ofFile(partial));
			if (response.statusCode() != 200) {
				throw new GradleException("Downloading " + getUrl().get() + " failed: HTTP " + response.statusCode());
			}
		}
		String actual = Packwiz.sha256(partial);
		if (!actual.equals(getSha256().get())) {
			Files.delete(partial);
			throw new GradleException("Refusing " + getUrl().get() + ": sha256 " + actual + ", expected " + getSha256().get());
		}
		Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
	}
}
