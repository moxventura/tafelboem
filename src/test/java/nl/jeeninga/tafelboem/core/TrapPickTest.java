package nl.jeeninga.tafelboem.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrapPickTest {
	private static final TrapPick.Candidate CACTUS_3 = new TrapPick.Candidate(3, 64, 0, TrapKind.CACTUS);
	private static final TrapPick.Candidate COBWEB_2 = new TrapPick.Candidate(0, 64, 2, TrapKind.COBWEB);
	private static final TrapPick.Candidate SLIME_4 = new TrapPick.Candidate(-4, 64, 0, TrapKind.SLIME);
	private static final TrapPick.Candidate MAGMA_5 = new TrapPick.Candidate(5, 64, 0, TrapKind.MAGMA);

	@Test
	void theNearestTrapWithinFourBlocksWins() {
		// He would land at (0.5, 0.5); distances are to the middle of each block.
		assertEquals(COBWEB_2, TrapPick.nearest(0.5, 0.5, List.of(CACTUS_3, COBWEB_2, MAGMA_5)));
	}

	@Test
	void fourBlocksIsStillInReachAndFurtherIsNot() {
		assertEquals(SLIME_4, TrapPick.nearest(0.5, 0.5, List.of(SLIME_4, MAGMA_5)), "exactly 4 blocks");
		assertNull(TrapPick.nearest(0.5, 0.5, List.of(MAGMA_5)), "5 blocks: ignored");
		assertNull(TrapPick.nearest(0.5, 0.5, List.of()), "no traps at all");
	}

	@Test
	void atMostOneTrapPerYeet() {
		TrapPick pick = new TrapPick();
		assertNull(pick.pick(0.5, 0.5, List.of(MAGMA_5)), "nothing in reach yet");
		assertEquals(CACTUS_3, pick.pick(0.5, 0.5, List.of(CACTUS_3)), "a miss doesn't use the pick up");
		assertNull(pick.pick(0.5, 0.5, List.of(COBWEB_2)), "one trap per yeet");
	}
}
