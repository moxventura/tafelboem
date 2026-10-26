package nl.jeeninga.tafelboem.build;

import java.util.Optional;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.process.ExecOperations;

/**
 * Lets only a clean commit tagged {@code vX.Y.Z} reach the kids' PCs, so every published version
 * can be rebuilt from its tag. Runs before compiling, so a forgotten tag fails in seconds. It also
 * refuses {@code -Ptests} and {@code -Prepeat}, which would filter the GameTests a publish depends on.
 */
@UntrackedTask(because = "checks the current git state")
public abstract class CheckRelease extends DefaultTask {
	@Internal
	public abstract DirectoryProperty getSourceDir();

	/** The version this build computed from git; must be the tag's. */
	@Input
	public abstract Property<String> getModVersion();

	/** The {@code -Ptests} pattern, if one is set: it filters every GameTest run in this invocation. */
	@Input
	@org.gradle.api.tasks.Optional
	public abstract Property<String> getGameTestFilter();

	/** The {@code -Prepeat} count, if one is set. */
	@Input
	@org.gradle.api.tasks.Optional
	public abstract Property<String> getGameTestRepeat();

	@Inject
	protected abstract ExecOperations getExec();

	@TaskAction
	public void check() {
		if (getGameTestFilter().isPresent() || getGameTestRepeat().isPresent()) {
			throw new GradleException("A publish runs every GameTest: drop -Ptests and -Prepeat.");
		}
		Git git = new Git(getExec(), getSourceDir().get().getAsFile());
		String changes = git.run("status", "--porcelain");
		if (!changes.isEmpty()) {
			throw new GradleException("Commit first: only committed code is published, so every version can be rebuilt.\n" + changes);
		}
		Optional<String> tag = git.tryRun("describe", "--exact-match", "--tags", "--match", "v[0-9]*", "HEAD");
		if (tag.isEmpty()) {
			Optional<String> last = git.tryRun("describe", "--tags", "--abbrev=0", "--match", "v[0-9]*", "HEAD");
			throw new GradleException("Tag this commit first: git tag " + last.map(CheckRelease::next).orElse("v0.1.0")
					+ last.map(l -> " (last: " + l + ")").orElse("") + ". A new release starts at X.Y.0, for example v1.0.0 for R1.");
		}
		if (!tag.get().equals("v" + getModVersion().get())) {
			throw new GradleException("This build is " + getModVersion().get() + " but HEAD is tagged " + tag.get() + "; run it again.");
		}
		getLogger().lifecycle("Releasing {}.", tag.get());
	}

	private static String next(String last) {
		try {
			return GitVersion.nextTag(last);
		} catch (IllegalArgumentException e) {
			return "vX.Y.Z";
		}
	}
}
