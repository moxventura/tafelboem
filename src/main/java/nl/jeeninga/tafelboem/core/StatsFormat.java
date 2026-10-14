package nl.jeeninga.tafelboem.core;

import java.util.Locale;

/**
 * Numbers in the stats follow the game language of whoever reads them: playtest 2 showed "4,0 s"
 * next to English text, because the PC was Dutch.
 */
public final class StatsFormat {
	private StatsFormat() {
	}

	/** @param language a Minecraft language code such as {@code nl_nl}; null for the server console */
	public static String seconds(long millis, String language) {
		Locale locale = language == null || language.isBlank() ? Locale.ROOT : Locale.forLanguageTag(language.replace('_', '-'));
		return String.format(locale, "%.1f", millis / 1000.0);
	}
}
