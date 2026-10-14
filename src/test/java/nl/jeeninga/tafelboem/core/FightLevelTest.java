package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FightLevelTest {
	/** The 55 sum pairs: 7 × 8 and 8 × 7 are one pair. */
	private static Set<Fact> families(Predicate<Fact> filter) {
		return Fact.all().stream().filter(fact -> fact.a() <= fact.b()).filter(filter).collect(Collectors.toSet());
	}

	private static List<BombTier> throwsOf(FightLevel level, int count) {
		List<BombTier> bombs = new ArrayList<>();
		for (int throwCount = 1; throwCount <= count; throwCount++) {
			bombs.add(level.bombForThrow(throwCount));
		}
		return bombs;
	}

	@Test
	void eachLevelAddsTables() {
		assertEquals(34, families(FightLevel.MAKKELIJK::includes).size());
		assertEquals(49, families(FightLevel.GEMIDDELD::includes).size());
		assertEquals(55, families(FightLevel.MOEILIJK::includes).size());
		assertEquals(55, families(FightLevel.CLASSIC::includes).size());
	}

	@Test
	void aSumBelongsToATableWhenEitherFactorIsThatTable() {
		assertTrue(FightLevel.MAKKELIJK.includes(new Fact(7, 2)));
		assertTrue(FightLevel.MAKKELIJK.includes(new Fact(2, 7)));
		assertFalse(FightLevel.MAKKELIJK.includes(new Fact(7, 3)));
	}

	@Test
	void theAddedSumsAreTheOnesTheLevelBeforeDidNotAsk() {
		assertEquals(15, families(FightLevel.GEMIDDELD::isAdded).size());
		assertEquals(Set.of(new Fact(6, 6), new Fact(6, 7), new Fact(6, 8), new Fact(7, 7), new Fact(7, 8), new Fact(8, 8)),
				families(FightLevel.MOEILIJK::isAdded));
		assertEquals(0, families(FightLevel.MAKKELIJK::isAdded).size());
		assertEquals(0, families(FightLevel.CLASSIC::isAdded).size());
	}

	@Test
	void onlyLevelsTwoAndThreeFocusOnTheirAddedTables() {
		assertFalse(FightLevel.CLASSIC.focusesOnAdded());
		assertFalse(FightLevel.MAKKELIJK.focusesOnAdded());
		assertTrue(FightLevel.GEMIDDELD.focusesOnAdded());
		assertTrue(FightLevel.MOEILIJK.focusesOnAdded());
	}

	@Test
	void everyThirdThrowStartingWithTheSecondIsTheBiggerBomb() {
		assertEquals(List.of(BombTier.KNALLETJE, BombTier.GEWONE_TNT, BombTier.KNALLETJE, BombTier.KNALLETJE, BombTier.GEWONE_TNT),
				throwsOf(FightLevel.MAKKELIJK, 5));
		assertEquals(List.of(BombTier.GEWONE_TNT, BombTier.KIPPENBOM, BombTier.GEWONE_TNT, BombTier.GEWONE_TNT, BombTier.KIPPENBOM),
				throwsOf(FightLevel.GEMIDDELD, 5));
		assertEquals(List.of(BombTier.KIPPENBOM, BombTier.KIPPENBOM, BombTier.KIPPENBOM, BombTier.KIPPENBOM, BombTier.KIPPENBOM),
				throwsOf(FightLevel.MOEILIJK, 5));
		assertEquals(throwsOf(FightLevel.MAKKELIJK, 5), throwsOf(FightLevel.CLASSIC, 5), "the R0.1 fight keeps its bombs");
	}

	@Test
	void theBombsBesideTheDoor() {
		assertEquals(List.of(BombTier.KNALLETJE, BombTier.GEWONE_TNT), FightLevel.MAKKELIJK.bombs());
		assertEquals(List.of(BombTier.GEWONE_TNT, BombTier.KIPPENBOM), FightLevel.GEMIDDELD.bombs());
		assertEquals(List.of(BombTier.KIPPENBOM), FightLevel.MOEILIJK.bombs());
	}

	@ParameterizedTest
	@EnumSource(FightLevel.class)
	void theLevelSetsTheHits(FightLevel level) {
		int expected = switch (level) {
			case CLASSIC, MAKKELIJK -> 11;
			case GEMIDDELD -> 12;
			case MOEILIJK -> 13;
		};
		assertEquals(expected, level.hitsRequired());
		assertEquals(expected, Fight.of(level).hitsRequired());
	}

	@Test
	void levelsByNumber() {
		assertEquals(FightLevel.GEMIDDELD, FightLevel.byNumber(2));
		assertEquals(FightLevel.CLASSIC, FightLevel.byNumber(0));
		assertThrows(IllegalArgumentException.class, () -> FightLevel.byNumber(4));
		assertEquals(List.of(FightLevel.MAKKELIJK, FightLevel.GEMIDDELD, FightLevel.MOEILIJK), FightLevel.DOORS);
	}
}
