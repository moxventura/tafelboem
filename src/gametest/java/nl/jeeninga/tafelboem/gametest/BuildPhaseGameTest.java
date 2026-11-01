package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.core.BuildKit;
import nl.jeeninga.tafelboem.core.BuildPhase;
import nl.jeeninga.tafelboem.core.BuildRecord;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.world.ArenaRun;
import nl.jeeninga.tafelboem.world.BuildCorner;
import nl.jeeninga.tafelboem.world.PlacedArea;

/**
 * R1.2's build phase: the plain arena with a chest and a bell, a countdown in the boss bar, the bell
 * bringing him 10 s later, and leaving before he came. GameTests can't wait minutes: the long times
 * are only checked as numbers, the waiting ones ring the bell or use a short build time.
 */
public class BuildPhaseGameTest {
	@GameTest(maxTicks = 100, padding = TestWorld.PADDING)
	public void entryPlacesThePlainArenaWithTheKitTheBellAndTheCountdown(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.STANDARD).running();
		ServerPlayer kid = test.kidInTheHub();
		ServerPlayer late = test.kidInTheHub();
		test.whenLoaded(() -> {
			test.world.enterDoor(kid, 1);
			ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElseThrow();
			PlacedArea arena = run.arena();
			helper.assertTrue(arena.name().equals("default"), "the plain arena, not " + arena.name());

			BlockPos chest = BuildCorner.chestAt(arena);
			helper.assertTrue(test.level.getBlockState(chest).is(Blocks.CHEST), "a chest 3 blocks ahead of the arrival spot");
			Container container = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, test.level.getBlockState(chest), test.level, chest, true);
			helper.assertTrue(container != null && container.getContainerSize() == 54, "a double chest");
			for (int slot = 0; slot < BuildKit.CHEST.size(); slot++) {
				BuildKit.Stack expected = BuildKit.CHEST.get(slot);
				ItemStack found = container.getItem(slot);
				helper.assertTrue(BuiltInRegistries.ITEM.getKey(found.getItem()).getPath().equals(expected.item()) && found.getCount() == expected.count(),
						"slot " + slot + " holds " + expected + ", not " + found);
			}
			helper.assertTrue(test.level.getBlockState(BuildCorner.bellAt(arena)).is(Blocks.BELL), "a bell beside the chest");
			helper.assertTrue(run.countdown().secondsLeft() == 300, "5 minutes on Makkelijk");
		});
		helper.runAfterDelay(15, () -> test.world.enterDoor(late, 1));

		helper.succeedWhen(() -> {
			// Only GameTest assertions are retried; orElseThrow before the kid is in would fail the test.
			helper.assertTrue(test.world.run(FightLevel.MAKKELIJK).isPresent(), "wait for the kid to go in");
			ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElseThrow();
			helper.assertTrue(run.kids().contains(late.getUUID()), "the second kid joins the same run");
			helper.assertTrue(run.building().isShownTo(kid) && run.building().isShownTo(late), "both see the countdown");
			helper.assertTrue(run.building().title().getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals("tafelboem.build.countdown") && contents.getArgs()[0].equals("4:59"),
					"the boss bar counts down: " + run.building().title());
			helper.assertTrue(run.fight() == null, "he doesn't come yet");
			test.close();
		});
	}

	@GameTest(maxTicks = 100, padding = TestWorld.PADDING)
	public void aClickVanillaWouldNotRingLeavesTheCountdownAlone(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.STANDARD).running();
		ServerPlayer kid = test.kidInTheHub();
		test.whenLoaded(() -> test.world.enterDoor(kid, 1));
		helper.runAfterDelay(15, () -> {
			ArenaRun run = test.world.run(FightLevel.MAKKELIJK).orElseThrow();
			BlockPos bell = BuildCorner.bellAt(run.arena());
			BlockHitResult front = frontOf(test, bell);
			Direction across = front.getDirection().getClockWise();

			BlockHitResult top = new BlockHitResult(new Vec3(bell.getX() + 0.5, bell.getY() + 1.0, bell.getZ() + 0.5), Direction.UP, bell, false);
			test.world.onUseBlock(kid, InteractionHand.MAIN_HAND, top);
			assertNotRung(helper, test, kid, "a click on the top");

			test.world.onUseBlock(kid, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(bell), across, bell, false));
			assertNotRung(helper, test, kid, "a click on a side across the way the bell faces");

			test.world.onUseBlock(kid, InteractionHand.OFF_HAND, front);
			assertNotRung(helper, test, kid, "a click with the off hand, which vanilla never uses a block with");

			// Sneaking with a block in hand places the block against the bell instead of using the bell.
			kid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
			kid.setShiftKeyDown(true);
			helper.assertTrue(kid.isSecondaryUseActive(), "the kid is sneaking");
			test.world.onUseBlock(kid, InteractionHand.MAIN_HAND, front);
			assertNotRung(helper, test, kid, "a sneak-click with a block in hand");

			kid.setShiftKeyDown(false);
			kid.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			test.world.onUseBlock(kid, InteractionHand.MAIN_HAND, front);
			helper.assertTrue(run.countdown().secondsLeft() == 10, "a click on the front with an empty hand rings: " + run.countdown().secondsLeft());
			helper.assertTrue(keys(test, kid).stream().filter("tafelboem.build.bell"::equals).count() == 1, "and says so: " + keys(test, kid));
			test.close();
			helper.succeed();
		});
	}

	private static void assertNotRung(GameTestHelper helper, TestWorld test, ServerPlayer kid, String click) {
		helper.assertTrue(test.world.run(FightLevel.MAKKELIJK).orElseThrow().countdown().secondsLeft() > 10, click + " leaves the countdown alone");
		helper.assertTrue(!keys(test, kid).contains("tafelboem.build.bell"), click + " says nothing about a bell: " + keys(test, kid));
	}

	/** A click halfway up the face of the bell that the kids see from the arrival spot, the way the bell faces. */
	private static BlockHitResult frontOf(TestWorld test, BlockPos bell) {
		return new BlockHitResult(Vec3.atCenterOf(bell), test.level.getBlockState(bell).getValue(BellBlock.FACING), bell, false);
	}

	@GameTest(maxTicks = 600, padding = TestWorld.PADDING)
	@Fight
	public void theBellBringsHimTenSecondsLaterAndOnlyThatArenaHearsIt(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.STANDARD).running();
		ServerPlayer ringer = test.kidInTheHub();
		ServerPlayer friend = test.kidInTheHub();
		ServerPlayer elsewhere = test.kidInTheHub();
		long[] rangAt = {-1};
		long[] cameAt = {-1};
		test.whenLoaded(() -> {
			test.world.enterDoor(ringer, 1);
			test.world.enterDoor(friend, 1);
			test.world.enterDoor(elsewhere, 2);
		});
		helper.runAfterDelay(20, () -> {
			BlockPos bell = BuildCorner.bellAt(test.world.run(FightLevel.MAKKELIJK).orElseThrow().arena());
			test.world.onUseBlock(ringer, InteractionHand.MAIN_HAND, frontOf(test, bell));
			rangAt[0] = helper.getTick();
			helper.assertTrue(test.world.run(FightLevel.MAKKELIJK).orElseThrow().countdown().secondsLeft() == 10, "10 s left after the bell");
			test.world.onUseBlock(friend, InteractionHand.MAIN_HAND, frontOf(test, bell));
		});
		helper.onEachTick(() -> {
			if (cameAt[0] < 0 && test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).isPresent()) {
				cameAt[0] = helper.getTick();
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(cameAt[0] > 0, "wait for Graaf Fout");
			long after = cameAt[0] - rangAt[0];
			helper.assertTrue(after >= 195 && after <= 205, "he comes 10 s after the bell, not after " + after + " ticks");
			for (ServerPlayer kid : List.of(ringer, friend)) {
				helper.assertTrue(keys(test, kid).stream().filter("tafelboem.build.bell"::equals).count() == 1,
						"one bell line for each kid in the arena, also when rung twice: " + keys(test, kid));
				helper.assertTrue(calls(test, kid).equals(List.of(5, 3, 2, 1)), "then 5, 3, 2, 1: " + calls(test, kid));
				helper.assertFalse(test.world.run(FightLevel.MAKKELIJK).orElseThrow().building().isShownTo(kid), "the countdown is gone");
			}
			helper.assertTrue(keys(test, elsewhere).stream().noneMatch(key -> key.startsWith("tafelboem.build.")),
					"a kid in another arena hears nothing: " + keys(test, elsewhere));
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).orElseThrow().fight() == null, "the other arena still builds");
			helper.assertTrue(ringer.getPlainTextName().equals(test.world.run(FightLevel.MAKKELIJK).orElseThrow().building().rangBy()),
					"the first kid who rang");
			test.close();
		});
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	@Fight
	public void whenTheTimeRunsOutTheChatCallsTheLastSeconds(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.everyLevel(12)).running();
		ServerPlayer kid = test.kidInTheHub();
		test.whenLoaded(() -> test.world.enterDoor(kid, 3));

		helper.succeedWhen(() -> {
			helper.assertTrue(test.world.run(FightLevel.MOEILIJK).map(ArenaRun::fight).isPresent(), "wait for Graaf Fout");
			helper.assertTrue(calls(test, kid).equals(List.of(10, 5, 3, 2, 1)), "10, 5, 3, 2, 1: " + calls(test, kid));
			helper.assertTrue(!keys(test, kid).contains("tafelboem.build.bell"), "nobody rang");
			test.close();
		});
	}

	@GameTest(maxTicks = 300, padding = TestWorld.PADDING)
	public void leavingDuringTheBuildEndsTheRunWithoutAFightAndTheNextEntryClearsTheBuild(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.STANDARD).running();
		ServerPlayer kid = test.kidInTheHub();
		BlockPos[] tower = {null};
		test.whenLoaded(() -> {
			test.world.enterDoor(kid, 2);
			// A block high above the walls, where only the build phase's clearing reaches.
			tower[0] = BlockPos.containing(test.world.run(FightLevel.GEMIDDELD).orElseThrow().arena().middle()).above(30);
			test.level.setBlockAndUpdate(tower[0], Blocks.OAK_PLANKS.defaultBlockState());
		});
		// The door cooldown is 40 ticks.
		helper.runAfterDelay(5 + 45, () -> test.world.enterDoor(kid, 0));
		helper.runAfterDelay(5 + 45 + 10, () -> {
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "the run ended");
			helper.assertTrue(test.fights.active().isEmpty(), "he never came");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().result().equals(FightRecord.LEFT) && fights.getFirst().answers() == 0
					&& fights.getFirst().seconds() == 0 && fights.getFirst().buildSeconds() >= 2, "logged as left, built ~2 s: " + fights);
			helper.assertTrue(keys(test, kid).contains("tafelboem.message.left_no_stars"), "the P28 line");
			helper.assertTrue(test.builds.size() == 1 && test.builds.getFirst().ending().equals(BuildRecord.EVERYONE_LEFT),
					"a build line: everyone left: " + test.builds);
		});
		// Back in once the cooldown is over again: a fresh arena, the tower gone.
		helper.runAfterDelay(5 + 45 + 45, () -> test.world.enterDoor(kid, 2));
		helper.succeedWhen(() -> {
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isPresent(), "wait until the kid is back in");
			helper.assertTrue(test.level.getBlockState(tower[0]).isAir(), "the last build is gone, also high above the walls");
			test.close();
		});
	}

	@GameTest(maxTicks = 600, padding = TestWorld.PADDING)
	@Fight
	public void theBuildLineCountsWhatEachKidPlacedAndTheFightTimeStartsWhenHeComes(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).withBuildPhase(BuildPhase.STANDARD).running();
		ServerPlayer kid = test.kidInTheHub();
		test.whenLoaded(() -> test.world.enterDoor(kid, 1));
		helper.runAfterDelay(10, () -> {
			// Minecraft counts a placed block as "used"; the mock kid can't place, so it gets the count.
			kid.awardStat(Stats.ITEM_USED.get(Items.OAK_PLANKS), 3);
			kid.awardStat(Stats.ITEM_USED.get(Items.GLASS), 2);
			test.world.onUseBlock(kid, InteractionHand.MAIN_HAND, frontOf(test, BuildCorner.bellAt(test.world.run(FightLevel.MAKKELIJK).orElseThrow().arena())));
		});
		boolean[] out = {false};
		helper.onEachTick(() -> {
			if (!out[0] && test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).isPresent()) {
				out[0] = true;
				helper.runAfterDelay(45, () -> test.world.enterDoor(kid, 0));
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(test.builds.size() == 1, "wait for the build line");
			BuildRecord build = test.builds.getFirst();
			helper.assertTrue(build.ending().equals(BuildRecord.BELL) && kid.getPlainTextName().equals(build.rangBy()), "the bell, by the kid: " + build);
			helper.assertTrue(build.placed().equals(Map.of(kid.getPlainTextName(), Map.of("oak_planks", 3, "glass", 2))),
					"what the kid placed: " + build.placed());
			helper.assertTrue(build.seconds() >= 10 && build.seconds() <= 12, "built ~11 s: " + build);
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1, "wait until the kid left the fight");
			helper.assertTrue(fights.getFirst().seconds() <= 3 && fights.getFirst().buildSeconds() >= 10,
					"the fight's time counts from his arrival: " + fights.getFirst());
			test.close();
		});
	}

	/** The translation keys of this kid's chat lines, in order. */
	private static List<String> keys(TestWorld test, ServerPlayer kid) {
		return test.client.chatsTo.stream()
				.filter(chat -> chat.player() == kid)
				.map(chat -> chat.message().getContents())
				.filter(TranslatableContents.class::isInstance)
				.map(contents -> ((TranslatableContents) contents).getKey())
				.toList();
	}

	/** The seconds the arena's chat called out to this kid, in order. */
	private static List<Integer> calls(TestWorld test, ServerPlayer kid) {
		List<Integer> calls = new ArrayList<>();
		for (RecordingQuizClient.Bar chat : test.client.chatsTo) {
			if (chat.player() == kid && chat.message().getContents() instanceof TranslatableContents contents) {
				if (contents.getKey().equals("tafelboem.build.seconds")) {
					calls.add((Integer) contents.getArgs()[0]);
				} else if (contents.getKey().equals("tafelboem.build.one_second")) {
					calls.add(1);
				}
			}
		}
		return calls;
	}
}
