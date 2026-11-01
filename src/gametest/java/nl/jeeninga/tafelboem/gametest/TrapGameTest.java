package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetReport;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * R1.3's traps: a yeet that comes down near a trap the kids built bends into it, for one more hit,
 * and the trap goes; a trap out of reach, the switch off, or a fight outside an arena changes
 * nothing. The yeets on cue barely move him, so only the pull can take him to the cactus.
 */
public class TrapGameTest {
	@GameTest(maxTicks = 200, padding = 48)
	public void aYeetBendsIntoACactusThreeBlocksOffForOneMoreHit(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		BlockPos cactus = cactus(arena, 3);
		int hits = arena.fight.hitsRemaining();
		List<LandingStyle> landings = yeetOnCue(arena);

		helper.succeedWhen(() -> {
			helper.assertTrue(landings.contains(LandingStyle.PRICKLED), "wait until he lands in it, hopping: " + landings);
			helper.assertTrue(arena.fight.hitsRemaining() == hits - 1, "one more hit: " + arena.fight.hitsRemaining() + " left of " + hits);
			helper.assertTrue(arena.level.getBlockState(cactus).isAir(), "the cactus poofed: a trap counts once");
			arena.end();
		});
	}

	@GameTest(maxTicks = 200, padding = 48)
	public void aCactusSixBlocksOffIsIgnored(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		noPull(helper, arena, cactus(arena, 6));
	}

	@GameTest(maxTicks = 200, padding = 48)
	public void withTrapsOffACactusThreeBlocksOffChangesNothing(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.traps = false;
		ArenaFight arena = new ArenaFight(helper, config);
		noPull(helper, arena, cactus(arena, 3));
	}

	@GameTest(maxTicks = 200, padding = 48)
	public void outsideAnArenaACactusThreeBlocksOffChangesNothing(GameTestHelper helper) {
		// The summon item's fight: 1.2.0's yeets, whatever the switch says.
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), false);
		noPull(helper, arena, cactus(arena, 3));
	}

	/**
	 * A real kick, so the yeet line is written. Lying bombs every 3 blocks over the arena and walls
	 * too high to fly over: whatever flight the kick picks, he comes down within reach of one.
	 */
	@GameTest(maxTicks = 1200, padding = 48)
	@Fight
	public void aLyingBombTrapGoesOffUnderHimAndIsLoggedAsTheTrapNeverASignature(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		// Craters would break the walls that keep him over the bombs.
		config.arenaCraters = false;
		ArenaFight arena = new ArenaFight(helper, config);
		walls(arena);
		helper.runAfterDelay(5, () -> lyingBombsEverywhere(arena));

		Set<Integer> answered = new HashSet<>();
		List<YeetReport> reports = new ArrayList<>();
		helper.onEachTick(() -> {
			// Every question he asks gets a right answer, until a yeet lands in a bomb.
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (answered.add(question.sessionId())) {
					arena.answerRight(arena.kid, question);
				}
			}
			YeetReport last = arena.fight.lastYeet();
			if (last != null && (reports.isEmpty() || reports.getLast() != last)) {
				reports.add(last);
			}
		});

		helper.succeedWhen(() -> {
			YeetReport trapped = reports.stream().filter(report -> report.trap() != null).findFirst().orElse(null);
			helper.assertTrue(trapped != null, "wait for a yeet into a lying bomb: " + reports);
			helper.assertTrue("lying_bomb".equals(trapped.trap()), "the trap is the lying bomb: " + trapped.describe());
			helper.assertTrue(trapped.landing().equals(LandingStyle.SOOT.id()), "he gets the soot face: " + trapped.describe());
			helper.assertFalse(trapped.signature(), "the trap's soot, never the bomb's signature: " + trapped.describe());
			String line = trapped.describe();
			helper.assertTrue(line.contains("trap=lying_bomb") && !line.contains("(signature)"), "the log line: " + line);
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).trapped().equals(Map.of("lying_bomb", 1)),
					"counted for the kicker: " + arena.fight.tally(arena.kid.getUUID()).trapped());
			arena.end();
		});
	}

	private static void noPull(GameTestHelper helper, ArenaFight arena, BlockPos cactus) {
		int hits = arena.fight.hitsRemaining();
		List<LandingStyle> landings = yeetOnCue(arena);
		helper.succeedWhen(() -> {
			helper.assertFalse(landings.isEmpty(), "wait until he has landed");
			helper.assertFalse(landings.contains(LandingStyle.PRICKLED), "not in the cactus: " + landings);
			helper.assertTrue(arena.fight.hitsRemaining() == hits, "no extra hit");
			helper.assertTrue(arena.level.getBlockState(cactus).is(Blocks.CACTUS), "the cactus is still there");
			arena.end();
		});
	}

	/** A cactus on sand, this many blocks east of Graaf Fout. */
	private static BlockPos cactus(ArenaFight arena, int east) {
		GraafFout boss = arena.fight.boss();
		BlockPos cactus = BlockPos.containing(boss.getX() + east, arena.floorY, boss.getZ());
		// The floor floats: without a block under it, the sand falls and the cactus breaks.
		arena.level.setBlockAndUpdate(cactus.below(2), Blocks.STONE.defaultBlockState());
		arena.level.setBlockAndUpdate(cactus.below(), Blocks.SAND.defaultBlockState());
		arena.level.setBlockAndUpdate(cactus, Blocks.CACTUS.defaultBlockState());
		return cactus;
	}

	/**
	 * A classic yeet with almost no power, as after a kick: up and down on the spot. Returns the
	 * landings he goes through, in order.
	 */
	private static List<LandingStyle> yeetOnCue(ArenaFight arena) {
		GraafFout boss = arena.fight.boss();
		boss.yeet(boss.position(), 0.01, YeetStyle.CLASSIC, arena.kid.position(), arena.fight.traps());
		List<LandingStyle> landings = new ArrayList<>();
		arena.helper.onEachTick(() -> {
			LandingStyle landing = boss.landingStyle();
			if (landing != null && !landings.contains(landing)) {
				landings.add(landing);
			}
		});
		return landings;
	}

	/** Stone walls round the arena, higher than any yeet goes. */
	private static void walls(ArenaFight arena) {
		int minX = (int) Math.floor(arena.box.middleX()) - 11;
		int minZ = (int) Math.floor(arena.box.middleZ()) - 11;
		for (int y = arena.floorY; y < arena.floorY + 20; y++) {
			for (int i = 0; i <= 22; i++) {
				arena.level.setBlockAndUpdate(new BlockPos(minX + i, y, minZ), Blocks.STONE.defaultBlockState());
				arena.level.setBlockAndUpdate(new BlockPos(minX + i, y, minZ + 22), Blocks.STONE.defaultBlockState());
				arena.level.setBlockAndUpdate(new BlockPos(minX, y, minZ + i), Blocks.STONE.defaultBlockState());
				arena.level.setBlockAndUpdate(new BlockPos(minX + 22, y, minZ + i), Blocks.STONE.defaultBlockState());
			}
		}
	}

	/**
	 * A Knalletje next to the kid, which they pick up at once, and lying bombs every 3 blocks over the
	 * rest of the floor, kept clear of the kid so they touch no other.
	 */
	private static void lyingBombsEverywhere(ArenaFight arena) {
		BlockPos kid = arena.kid.blockPosition();
		arena.fight.placeLyingBomb(kid.east(), BombTier.KNALLETJE);
		int minX = (int) Math.floor(arena.box.middleX()) - 9;
		int minZ = (int) Math.floor(arena.box.middleZ()) - 9;
		for (int dx = 0; dx <= 18; dx += 3) {
			for (int dz = 0; dz <= 18; dz += 3) {
				BlockPos pos = new BlockPos(minX + dx, arena.floorY, minZ + dz);
				if (Math.abs(pos.getX() - kid.getX()) > 3 || Math.abs(pos.getZ() - kid.getZ()) > 3) {
					arena.fight.placeLyingBomb(pos, BombTier.KNALLETJE);
				}
			}
		}
	}
}
