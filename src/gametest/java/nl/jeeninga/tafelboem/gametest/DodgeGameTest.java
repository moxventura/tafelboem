package nl.jeeninga.tafelboem.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.block.BombBlock;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.game.PlayerSafeExplosion;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * R1.3's dodging: a bomb that lands more than 2 blocks from its kid lies still and roots nobody, a
 * kid who touches a lying bomb gets its question, bombs pile up without a cap, craters never set
 * them off, and they all poof when the fight ends.
 */
public class DodgeGameTest {
	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void aBombTheWallStopsLiesStillAndRootsNobody(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		BlockPos wall = wallInFrontOf(arena);

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).dodged() >= 1, "wait for his first throw to hit the wall");
			BlockPos lying = arena.fight.lyingBombs().iterator().next();
			helper.assertTrue(arena.level.getBlockState(lying).getBlock() instanceof BombBlock, "it lies there as a bomb block");
			helper.assertTrue(lying.getY() == arena.floorY && lying.getZ() < wall.getZ(), "on the floor, on his side of the wall: " + lying);
			helper.assertFalse(arena.askedAnything(), "nobody gets a question");
			helper.assertFalse(arena.sessions.isBusy(arena.kid), "nobody is rooted");
			helper.assertTrue(arena.heard(arena.kid, "tafelboem.boss.graaf_fout.dodged"), "he taunts the kid it missed");
			arena.end();
		});
	}

	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void withDodgingOffTheKidIsRootedWhereverTheBombLands(GameTestHelper helper) {
		TafelBoemConfig config = new TafelBoemConfig();
		config.dodging = false;
		ArenaFight arena = new ArenaFight(helper, config);
		wallInFrontOf(arena);

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.askedAnything(), "wait for the question: as in 1.2.0, the wall changes nothing");
			helper.assertTrue(arena.sessions.isBusy(arena.kid), "the kid is rooted");
			helper.assertTrue(arena.fight.lyingBombs().isEmpty(), "nothing lies around");
			arena.end();
		});
	}

	@GameTest(maxTicks = 300, padding = 48)
	@Fight
	public void outsideAnArenaTheKidIsRootedWhereverTheBombLandsEvenWithDodgingOn(GameTestHelper helper) {
		// The summon item's fight, with no arena: 1.2.0's rule stays, there is no floor to leave bombs lying on.
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig(), false);
		wallInFrontOf(arena);

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.askedAnything(), "wait for the question: the wall changes nothing outside an arena");
			helper.assertTrue(arena.sessions.isBusy(arena.kid), "the kid is rooted");
			helper.assertTrue(arena.fight.lyingBombs().isEmpty(), "nothing lies around");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).dodged() == 0, "nothing was dodged");
			arena.end();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void aKidWhoTouchesALyingBombGetsItsQuestion(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		// Before his first throw (60 ticks), so this is the only bomb.
		helper.runAfterDelay(5, () -> arena.fight.placeLyingBomb(arena.kid.blockPosition().east(), BombTier.GEWONE_TNT));

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.asked(arena.kid, BombTier.GEWONE_TNT), "wait for its question, on its own bomb");
			helper.assertTrue(arena.sessions.isBusy(arena.kid), "rooted, as after a hit");
			helper.assertTrue(arena.fight.lyingBombs().isEmpty(), "it no longer lies around");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).pickedUp() == 1, "counted as picked up");
			arena.end();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void bombsPileUpWithoutACapAndAllPoofWhenTheFightEnds(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		BlockPos middle = BlockPos.containing(arena.box.middleX(), arena.floorY, arena.box.middleZ());
		List<BlockPos> bombs = List.of(middle.west(4), middle.east(4), middle.north(4));
		helper.runAfterDelay(5, () -> bombs.forEach(pos -> arena.fight.placeLyingBomb(pos, BombTier.KNALLETJE)));
		// The kid walks into the first one; the other two stay.
		helper.runAfterDelay(10, () -> arena.kid.teleportTo(bombs.getFirst().getX() + 1.5, arena.floorY, bombs.getFirst().getZ() + 0.5));
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(arena.fight.lyingBombs().equals(Set.of(bombs.get(1), bombs.get(2))),
					"one picked up, two still lying: " + arena.fight.lyingBombs());
			arena.end();
			for (BlockPos pos : bombs.subList(1, 3)) {
				helper.assertTrue(arena.level.getBlockState(pos).isAir(), "every lying bomb poofs when the fight ends: " + pos);
			}
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void hittingTheLyingBombFromAfarTouchesItAndNeverBreaksIt(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		BlockPos lying = arena.kid.blockPosition().east(6);
		BlockPos kidsOwn = arena.kid.blockPosition().west(6);
		helper.runAfterDelay(5, () -> {
			arena.fight.placeLyingBomb(lying, BombTier.KIPPENBOM);
			arena.level.setBlockAndUpdate(kidsOwn, ModBlocks.bomb(BombTier.KNALLETJE).defaultBlockState());
		});
		helper.runAfterDelay(10, () -> {
			helper.assertFalse(arena.fights.mayBreak(arena.kid, lying), "the hit breaks nothing: the bomb would drop as an item");
			helper.assertTrue(arena.asked(arena.kid, BombTier.KIPPENBOM), "but it counts as touching: its question comes, from 6 blocks away");
			helper.assertTrue(arena.level.getBlockState(lying).getBlock() instanceof BombBlock, "the bomb block is still there");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).pickedUp() == 1, "counted as picked up");
			helper.assertTrue(arena.fights.mayBreak(arena.kid, kidsOwn), "a bomb no fight has put down breaks as before");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void rightClickingTheLyingBombTouchesItButAnyOtherBombIsStillFreePlay(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 7, arena.box.middleZ() + 5);
		BlockPos lying = arena.kid.blockPosition().west(6);
		BlockPos kidsOwn = sibling.blockPosition().east(2);
		helper.runAfterDelay(5, () -> {
			arena.fight.placeLyingBomb(lying, BombTier.GEWONE_TNT);
			arena.level.setBlockAndUpdate(kidsOwn, ModBlocks.bomb(BombTier.KNALLETJE).defaultBlockState());
		});
		helper.runAfterDelay(10, () -> {
			arena.fights.useBombBlock(arena.kid, lying, BombTier.GEWONE_TNT);
			helper.assertTrue(arena.asked(arena.kid, BombTier.GEWONE_TNT), "the lying bomb's question comes, from 6 blocks away");
			helper.assertTrue(arena.fight.tally(arena.kid.getUUID()).pickedUp() == 1, "as a pick-up: it is the fight's, not free play's");
			helper.assertTrue(arena.fight.lyingBombs().isEmpty(), "it no longer lies around");

			arena.fights.useBombBlock(sibling, kidsOwn, BombTier.KNALLETJE);
			helper.assertTrue(arena.asked(sibling, BombTier.KNALLETJE), "a bomb block no fight put down still asks, as free play");
			helper.assertTrue(arena.fight.tally(sibling.getUUID()).pickedUp() == 0, "and is no pick-up");
			arena.end();
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 400, padding = 48)
	@Fight
	public void oneKidAnswersAThrownBombWhileTheSiblingPicksUpALyingBomb(GameTestHelper helper) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 7, arena.box.middleZ() + 5);
		int startHits = arena.fight.hitsRemaining();
		// Before his first throw (60 ticks): the sibling is already answering, so the bomb comes for the other kid.
		helper.runAfterDelay(5, () -> arena.fight.placeLyingBomb(sibling.blockPosition().east(), BombTier.GEWONE_TNT));

		Set<ServerPlayer> answered = new HashSet<>();
		boolean[] bothAsked = {false};
		helper.onEachTick(() -> {
			bothAsked[0] |= arena.sessions.isBusy(arena.kid) && arena.sessions.isBusy(sibling);
			if (!bothAsked[0]) {
				return;
			}
			// Both questions are open: the thrown kid answers now, the sibling once that kick has hit and he is flying.
			// Only their first questions: what he asks next stays open.
			if (answered.add(arena.kid)) {
				arena.answerRight(arena.kid, arena.questionsTo(arena.kid).getFirst());
			}
			if (arena.fight.hitsRemaining() < startHits && answered.add(sibling)) {
				arena.answerRight(sibling, arena.questionsTo(sibling).getFirst());
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(bothAsked[0], "wait for both questions at once");
			helper.assertTrue(arena.fight.hitsRemaining() == startHits - 2, "both kicks hit him: " + arena.fight.hitsRemaining());
			helper.assertFalse(arena.fight.isOver(), "the fight goes on");
			long questions = arena.questionsTo(arena.kid).size() + arena.questionsTo(sibling).size();
			helper.assertTrue(questions >= 3, "and he throws again: a third question comes (" + questions + ")");
			arena.end();
		});
	}

	@GameTest(maxTicks = 6000, padding = 48)
	@Fight
	public void aSiblingsRightAnswerWhileHeFliesOffDoesNotDerailTheWin(GameTestHelper helper) {
		beatenWhileTheSiblingAnswers(helper, true);
	}

	@GameTest(maxTicks = 6000, padding = 48)
	@Fight
	public void aSiblingsWrongAnswerWhileHeFliesOffDoesNotDerailTheWin(GameTestHelper helper) {
		beatenWhileTheSiblingAnswers(helper, false);
	}

	/**
	 * Two questions open at once must not derail the win: the sibling picks up a lying bomb late in the
	 * fight and answers while Graaf Fout, hit for the last time by the other kid's kick, is flying off.
	 * He must still be gone 60 ticks after that hit, and everyone gets the reward.
	 */
	private static void beatenWhileTheSiblingAnswers(GameTestHelper helper, boolean right) {
		ArenaFight arena = new ArenaFight(helper, new TafelBoemConfig());
		ServerPlayer sibling = arena.addKid(arena.box.middleX() + 7, arena.box.middleZ() + 5);

		Set<Integer> answered = new HashSet<>();
		boolean[] lastHitNear = {false};
		boolean[] siblingAnswered = {false};
		long[] wonAt = {-1};
		long[] goneAt = {-1};
		helper.onEachTick(() -> {
			// Both kids answer everything right until his last hit is near; from then on only the first does.
			boolean wasNear = lastHitNear[0];
			lastHitNear[0] |= arena.fight.hitsRemaining() <= 1;
			for (ServerPlayer kid : List.of(arena.kid, sibling)) {
				for (QuizS2C question : arena.questionsTo(kid)) {
					if ((kid == arena.kid || !lastHitNear[0]) && answered.add(question.sessionId())) {
						arena.answerRight(kid, question);
					}
				}
			}
			// The sibling then sits on a lying bomb's question until he is beaten.
			if (lastHitNear[0] && !wasNear) {
				arena.fight.placeLyingBomb(sibling.blockPosition().east(), BombTier.KNALLETJE);
			}
			if (arena.fight.isWon() && wonAt[0] < 0) {
				wonAt[0] = helper.getTick();
			}
			if (arena.fight.isOver() && goneAt[0] < 0) {
				goneAt[0] = helper.getTick();
			}
			if (arena.fight.isWon() && arena.sessions.isBusy(sibling) && !siblingAnswered[0]) {
				siblingAnswered[0] = true;
				QuizS2C open = arena.questionsTo(sibling).getLast();
				if (right) {
					arena.answerRight(sibling, open);
				} else {
					arena.answerWrong(sibling, open);
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(arena.fight.isOver(), "wait until he is beaten and gone");
			helper.assertTrue(siblingAnswered[0], "the sibling had a question open when he was beaten");
			// His finale takes 60 ticks; a second answer used to start it again, which kept him up 80 more.
			helper.assertTrue(goneAt[0] - wonAt[0] <= 65, "he was gone " + (goneAt[0] - wonAt[0]) + " ticks after his last hit");
			for (ServerPlayer kid : List.of(arena.kid, sibling)) {
				helper.assertTrue(kid.getInventory().countItem(Items.COOKED_CHICKEN) == 4, "the win is rewarded, to both kids");
			}
		});
	}

	@GameTest(padding = 48)
	public void aCraterNeverSetsOffOrBreaksALyingBomb(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 5));
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
			}
		}
		BlockPos bomb = centre.east();
		level.setBlockAndUpdate(bomb, ModBlocks.bomb(BombTier.KNALLETJE).defaultBlockState());

		level.explode(null, null, PlayerSafeExplosion.sparing(List.of(), 3), centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 4.0F,
				false, Level.ExplosionInteraction.TNT);

		helper.assertTrue(level.getBlockState(centre.below()).isAir(), "the crater is there");
		helper.assertTrue(level.getBlockState(bomb).getBlock() instanceof BombBlock, "the bomb beside it still lies there");
		helper.succeed();
	}

	/**
	 * Stone 7 wide and 5 high, 2 blocks in front of the kid towards Graaf Fout: higher than his throws
	 * fly, so every one hits it, 3 blocks from the kid.
	 */
	private static BlockPos wallInFrontOf(ArenaFight arena) {
		BlockPos wall = arena.kid.blockPosition().north(2);
		for (int across = -3; across <= 3; across++) {
			for (int up = 0; up < 5; up++) {
				arena.level.setBlockAndUpdate(wall.east(across).above(up), Blocks.STONE.defaultBlockState());
			}
		}
		return wall;
	}
}
