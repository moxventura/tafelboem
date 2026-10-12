package nl.jeeninga.tafelboem.build;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns {@code git describe} into the mod version. Every published build is a clean commit tagged
 * {@code vX.Y.Z} (X.Y is the release, Z the build within it) and gets exactly that number. Anything
 * else gets SemVer build metadata, such as {@code 0.1.3+2.g1a2b3c4.dirty}, so a test build never
 * looks like a published one and the join-time version check tells them apart.
 */
public final class GitVersion {
	/** The command whose output {@link #fromDescribe} reads. */
	public static final String[] DESCRIBE = {"git", "describe", "--tags", "--match", "v[0-9]*", "--long", "--dirty", "--always"};

	private static final Pattern TAGGED = Pattern.compile("v(.+)-(\\d+)-g([0-9a-f]+)(-dirty)?");
	private static final Pattern UNTAGGED = Pattern.compile("([0-9a-f]+)(-dirty)?");
	private static final Pattern TAG = Pattern.compile("v(\\d+)\\.(\\d+)\\.(\\d+)");

	private GitVersion() {
	}

	public static String fromDescribe(String describe) {
		String text = describe.trim();
		Matcher tagged = TAGGED.matcher(text);
		if (tagged.matches()) {
			boolean dirty = tagged.group(4) != null;
			if (tagged.group(2).equals("0") && !dirty) {
				return tagged.group(1);
			}
			return tagged.group(1) + "+" + tagged.group(2) + ".g" + tagged.group(3) + (dirty ? ".dirty" : "");
		}
		Matcher untagged = UNTAGGED.matcher(text);
		if (untagged.matches()) {
			return "0.0.0+g" + untagged.group(1) + (untagged.group(2) != null ? ".dirty" : "");
		}
		return "0.0.0+unknown";
	}

	/** The tag for the next build in the same release: {@code v0.1.2} → {@code v0.1.3}. */
	public static String nextTag(String tag) {
		Matcher matcher = TAG.matcher(tag);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Not a version tag like v0.1.2: " + tag);
		}
		return "v" + matcher.group(1) + "." + matcher.group(2) + "." + (Integer.parseInt(matcher.group(3)) + 1);
	}
}
