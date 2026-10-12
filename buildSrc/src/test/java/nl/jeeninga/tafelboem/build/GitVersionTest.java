package nl.jeeninga.tafelboem.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * The version comes from git: only a tagged, clean commit gets a plain number like the kids' PCs
 * show, so a test build can never pass for a published one.
 */
class GitVersionTest {
	private static final String DESCRIBE_TAGGED = "v0.1.3-0-g1a2b3c4";

	@Test
	void aCleanTaggedCommitIsTheTagsNumber() {
		assertEquals("0.1.3", GitVersion.fromDescribe(DESCRIBE_TAGGED));
	}

	@Test
	void commitsAfterTheTagAddHowManyAndWhich() {
		assertEquals("0.1.3+2.g1a2b3c4", GitVersion.fromDescribe("v0.1.3-2-g1a2b3c4"));
	}

	@Test
	void uncommittedChangesAreMarkedEvenOnATag() {
		assertEquals("0.1.3+0.g1a2b3c4.dirty", GitVersion.fromDescribe(DESCRIBE_TAGGED + "-dirty"));
		assertEquals("0.1.3+2.g1a2b3c4.dirty", GitVersion.fromDescribe("v0.1.3-2-g1a2b3c4-dirty"));
	}

	@Test
	void withoutATagOnlyTheCommitIsKnown() {
		// git describe --always prints just the abbreviated commit when no v* tag is reachable.
		assertEquals("0.0.0+g1a2b3c4", GitVersion.fromDescribe("1a2b3c4"));
		assertEquals("0.0.0+g1a2b3c4.dirty", GitVersion.fromDescribe("1a2b3c4-dirty"));
	}

	@Test
	void withoutGitTheVersionIsUnknown() {
		assertEquals("0.0.0+unknown", GitVersion.fromDescribe(""));
		assertEquals("0.0.0+unknown", GitVersion.fromDescribe("fatal: not a git repository"));
	}

	@Test
	void surroundingWhitespaceFromTheCommandIsIgnored() {
		assertEquals("0.1.3", GitVersion.fromDescribe(DESCRIBE_TAGGED + "\n"));
	}

	@Test
	void theNextTagRaisesTheLastNumber() {
		assertEquals("v0.1.3", GitVersion.nextTag("v0.1.2"));
		assertEquals("v1.0.10", GitVersion.nextTag("v1.0.9"));
	}

	@Test
	void theNextTagNeedsAVersionTag() {
		assertThrows(IllegalArgumentException.class, () -> GitVersion.nextTag("r0.1"));
	}
}
