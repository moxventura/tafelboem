package nl.jeeninga.tafelboem.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A GameTest that plays Graaf Fout's fight for 3 s or more. It runs in the fights group
 * ({@code ./gradlew runFightGameTests}); every other GameTest runs in the quick group, which
 * {@code ./gradlew build} runs. See {@link GameTestHarness}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Fight {
}
