package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
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
	 * The one place where traps, the hit counter and the finale meet: a real fight is played with right
	 * answers until he has two hits left, then cacti stand all over the arena. That kick takes one hit and
	 * the cactus he lands in the other, so the trap's hit is the one that beats him. He must fly off at
	 * once, not start a dizzy landing, and the win is rewarded.
	 */
	@GameTest(maxTicks = 6000, padding = 48)
	@Fight
	public void aTrapsExtraHitCanBeTheLastAndTheFightEndsWithAWin(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		// Craters would break the walls that keep him over the cacti.
		config.arenaCraters = false;
		ArenaFight arena = new ArenaFight(helper, config);
		walls(arena);

		Set<Integer> answered = new HashSet<>();
		boolean[] cacti = {false};
		long[] wonAt = {-1};
		long[] goneAt = {-1};
		LandingStyle[] landingWhenBeaten = {null};
		helper.onEachTick(() -> {
			for (QuizS2C question : arena.questionsTo(arena.kid)) {
				if (answered.add(question.sessionId())) {
					arena.answerRight(arena.kid, question);
				}
			}
			// Only once the yeet before has landed: it must not find a cactus, or the hits would not add up.
			if (!cacti[0] && arena.fight.hitsRemaining() == 2 && !arena.fight.boss().isReacting()) {
				cacti[0] = true;
				cactiEverywhere(arena);
			}
			if (arena.fight.isWon() && wonAt[0] < 0) {
				wonAt[0] = helper.getTick();
				landingWhenBeaten[0] = arena.fight.boss().landingStyle();
			}
			if (arena.fight.isOver() && goneAt[0] < 0) {
				goneAt[0] = helper.getTick();
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.isOver(), "wait until he is beaten and gone");
			helper.assertTrue(cacti[0], "the cacti stood there for the last kick");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).trapped().equals(Map.of("cactus", 1)),
					"the trap gave the last hit: " + arena.fight.tally(arena.kid.getUUID()).trapped());
			helper.assertTrue(answered.size() == 10, "10 kicks did it: 9 hits, and 1 kick with the cactus: " + answered.size());
			helper.assertTrue(landingWhenBeaten[0] == null, "he flies off at once, no landing in the finale: " + landingWhenBeaten[0]);
			helper.assertTrue(goneAt[0] - wonAt[0] <= 65, "he was gone " + (goneAt[0] - wonAt[0]) + " ticks after his last hit");
			helper.assertTrue(arena.kid.getInventory().countItem(Items.COOKED_CHICKEN) == 4, "the win is rewarded");
		});
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
		return cactusAt(arena, BlockPos.containing(boss.getX() + east, arena.floorY, boss.getZ()));
	}

	private static BlockPos cactusAt(ArenaFight arena, BlockPos cactus) {
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

	/**
	 * A cactus every 3 blocks over the floor, so any flight comes down within reach of one; none within
	 * 2.5 blocks of the kid, where his next bomb may land.
	 */
	private static void cactiEverywhere(ArenaFight arena) {
		int middleX = (int) Math.floor(arena.box.middleX());
		int middleZ = (int) Math.floor(arena.box.middleZ());
		for (int dx = -9; dx <= 9; dx += 3) {
			for (int dz = -9; dz <= 9; dz += 3) {
				double kidX = arena.kid.getX() - (middleX + dx + 0.5);
				double kidZ = arena.kid.getZ() - (middleZ + dz + 0.5);
				if (kidX * kidX + kidZ * kidZ >= 2.5 * 2.5) {
					cactusAt(arena, new BlockPos(middleX + dx, arena.floorY, middleZ + dz));
				}
			}
		}
	}
}
