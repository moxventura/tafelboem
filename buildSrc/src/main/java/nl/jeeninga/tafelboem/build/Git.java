package nl.jeeninga.tafelboem.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.gradle.api.GradleException;
import org.gradle.process.ExecOperations;

/** Runs git in one repo for the release tasks; a failure carries git's own message. */
final class Git {
	private final ExecOperations exec;
	private final File dir;

	Git(ExecOperations exec, File dir) {
		this.exec = exec;
		this.dir = dir;
	}

	/** The trimmed output; fails the build when git fails. */
	String run(String... args) {
		Result result = exec(args);
		if (result.exit != 0) {
			throw new GradleException(String.join(" ", result.command) + " failed (" + result.exit + "): " + result.err);
		}
		return result.out;
	}

	/** The trimmed output, or empty when git fails, for questions like "is HEAD tagged?". */
	Optional<String> tryRun(String... args) {
		Result result = exec(args);
		return result.exit == 0 ? Optional.of(result.out) : Optional.empty();
	}

	private Result exec(String... args) {
		List<String> command = new ArrayList<>(List.of("git", "-C", dir.getAbsolutePath()));
		command.addAll(List.of(args));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		int exit = exec.exec(spec -> {
			spec.commandLine(command);
			spec.setStandardOutput(out);
			spec.setErrorOutput(err);
			spec.setIgnoreExitValue(true);
		}).getExitValue();
		return new Result(command, exit, out.toString(StandardCharsets.UTF_8).trim(), err.toString(StandardCharsets.UTF_8).trim());
	}

	private record Result(List<String> command, int exit, String out, String err) {
	}
}
