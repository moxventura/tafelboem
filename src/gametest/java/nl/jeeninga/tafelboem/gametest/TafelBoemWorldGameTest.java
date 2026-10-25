package nl.jeeninga.tafelboem.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.config.TafelBoemConfig;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.gametest.RecordingQuizClient.Bar;
import nl.jeeninga.tafelboem.registry.ModBlocks;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * Playing a TafelBoem world: through a door into a fight, together, to the stars, and back home.
 */
public class TafelBoemWorldGameTest {
	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void aLevelDoorLeadsToAFightAtThatLevel(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 2);

		helper.assertTrue(test.layout.arenaSlot(FightLevel.GEMIDDELD).containsColumn(kid.getX(), kid.getZ()), "the kid is in the level 2 arena");
		BlockPos below = kid.blockPosition().below();
		helper.assertTrue(test.level.getBlockState(below).is(Blocks.GRASS_BLOCK), "standing on the arena's grass");
		helper.succeedWhen(() -> {
			BossFight fight = test.world.run(FightLevel.GEMIDDELD).map(ArenaRun::fight).orElse(null);
			helper.assertTrue(fight != null, "wait for Graaf Fout");
			helper.assertTrue(fight.fightLevel() == FightLevel.GEMIDDELD, "a level 2 fight");
			helper.assertTrue(fight.arena() != null, "in its arena");
			test.close();
		});
	}

	@GameTest(maxTicks = 2400, padding = TestWorld.PADDING)
	@Fight
	public void everyKidThroughTheSameDoorJoinsTheSameFight(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer first = test.kidInTheHub();
		ServerPlayer second = test.kidInTheHub();
		ServerPlayer third = test.kidInTheHub();
		test.world.enterDoor(first, 1);
		boolean[] othersIn = {false};
		helper.onEachTick(() -> {
			// The other two come in once he's there.
			if (!othersIn[0] && test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).isPresent()) {
				test.world.enterDoor(second, 1);
				test.world.enterDoor(third, 1);
				othersIn[0] = true;
			}
		});

		helper.succeedWhen(() -> {
			BossFight fight = test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).orElse(null);
			helper.assertTrue(fight != null, "wait for Graaf Fout");
			for (ServerPlayer kid : List.of(first, second, third)) {
				helper.assertTrue(fight.tally(kid.getUUID()).answers() >= 1, "wait until every kid has had a turn");
			}
			test.close();
		});
	}

	@GameTest(maxTicks = 9000, padding = TestWorld.PADDING)
	@Fight
	public void aWonFightPaysTheRewardWritesTheStarsAndSendsEveryoneHome(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.answerWrongOnce(kid);
		test.world.enterDoor(kid, 2);

		helper.succeedWhen(() -> {
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().won(), "wait for the win");
			FightRecord won = fights.getFirst();
			helper.assertTrue(won.level() == 2 && won.wrong() == 1 && won.stars() == 2, "one wrong answer is ★★: " + won);
			helper.assertTrue(kid.getInventory().countItem(Items.COOKED_CHICKEN) == 4, "4 cooked chicken, to heal the hearts");
			helper.assertTrue(kid.getInventory().countItem(ModBlocks.bombItem(BombTier.KIPPENBOM)) == 0, "no Kippenbommen any more");
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "wait until everyone is home");
			helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "back in the hub");
			test.close();
		});
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void theLastKidLeavingEndsTheFight(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 2);
		BossFight[] fight = {null};
		helper.onEachTick(() -> {
			if (fight[0] == null) {
				fight[0] = test.world.run(FightLevel.GEMIDDELD).map(ArenaRun::fight).orElse(null);
				if (fight[0] != null) {
					test.world.enterDoor(kid, 0);
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(fight[0] != null && fight[0].isOver(), "the fight is over");
			helper.assertFalse(fight[0].boss().isAlive(), "he poofed away");
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "the arena is free");
			helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "the kid is in the hub");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().result().equals(FightRecord.LEFT), "logged as left: " + fights);
			test.close();
		});
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void aKidFallingIntoTheVoidLandsSafely(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		kid.teleportTo(kid.getX(), test.layout.voidY() - 5, kid.getZ());
		boolean[] inArena = {false};
		helper.onEachTick(() -> {
			if (!inArena[0] && test.world.hub().bounds().holds(kid.getX(), kid.getY(), kid.getZ())) {
				test.world.enterDoor(kid, 1);
				kid.teleportTo(kid.getX(), test.layout.voidY() - 5, kid.getZ());
				inArena[0] = true;
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(inArena[0], "first back in the hub");
			BlockPos arrival = test.world.run(FightLevel.MAKKELIJK).orElseThrow().arena().standAt();
			helper.assertTrue(kid.blockPosition().equals(arrival), "then back at the arena's arrival spot");
			test.close();
		});
	}

	@GameTest(maxTicks = 200, padding = TestWorld.PADDING)
	public void walkingUpToADoorShowsYourOwnStars(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.log.append(new FightRecord(0L, kid.getPlainTextName(), 2, "default", FightRecord.WON, 12, 1, 0, 0, 2, 120, 1, "1.0.0"));
		BlockPos door = test.world.hub().doors().get(2).getFirst();
		kid.teleportTo(door.getX() + 0.5, door.getY(), door.getZ() + 1.5);

		helper.succeedWhen(() -> {
			helper.assertTrue(test.client.actionBars.stream().anyMatch(TafelBoemWorldGameTest::showsTwoStars), "the action bar shows ★★☆");
			test.close();
		});
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aKidWhoLogsInInsideAnEmptyArenaGoesToTheHub(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		kid.teleportTo(test.layout.arenaSlot(FightLevel.MOEILIJK).middleX(), test.layout.hub().getY() + 4,
				test.layout.arenaSlot(FightLevel.MOEILIJK).middleZ());
		test.world.onJoin(kid);
		helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "in the hub");
		test.close();
		helper.succeed();
	}

	@GameTest(padding = TestWorld.PADDING)
	public void aDoorTheKidWalkedIntoIsOnlyEnteredOnTheNextTick(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.queueDoor(kid, 3);
		test.world.queueDoor(kid, 1);
		helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "the door only records: the kid is still in the hub");

		helper.succeedWhen(() -> {
			helper.assertTrue(test.layout.arenaSlot(FightLevel.MAKKELIJK).containsColumn(kid.getX(), kid.getZ()), "wait for the level 1 arena: the last door counts");
			helper.assertTrue(test.world.run(FightLevel.MOEILIJK).isEmpty(), "the door before it was replaced");
			test.close();
		});
	}

	@GameTest(padding = TestWorld.PADDING)
	public void doorsIgnoreSpectatorsAndExitsIgnoreKidsWhoAreNotInAFight(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer builder = test.kidInTheHub();
		double height = builder.getY() + 5;
		builder.teleportTo(builder.getX(), height, builder.getZ());
		test.world.enterDoor(builder, 0);
		helper.assertTrue(builder.getY() == height, "the exit door left a builder where he was");
		test.world.enterDoor(builder, 1);
		helper.assertTrue(test.layout.arenaSlot(FightLevel.MAKKELIJK).containsColumn(builder.getX(), builder.getZ()), "and it didn't use up the door cooldown");

		// The mock from makeMockServerPlayerInLevel is always in creative; this one has the game mode we ask for.
		ServerPlayer spectator = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
		Vec3 before = spectator.position();
		test.world.enterDoor(spectator, 2);
		helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "a spectator opens no arena");
		helper.assertTrue(spectator.position().equals(before), "and stays where he is");
		test.close();
		helper.succeed();
	}

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	public void aFightThatEndsWithoutAWinSendsTheKidsHome(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 2);
		BossFight[] fight = {null};
		helper.onEachTick(() -> {
			if (fight[0] == null) {
				fight[0] = test.world.run(FightLevel.GEMIDDELD).map(ArenaRun::fight).orElse(null);
				if (fight[0] != null) {
					fight[0].boss().discard();
				}
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(fight[0] != null && fight[0].isOver() && !fight[0].isWon(), "he is gone without a win");
			helper.assertTrue(test.world.run(FightLevel.GEMIDDELD).isEmpty(), "the arena is free");
			helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "the kid is in the hub");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().result().equals(FightRecord.LEFT), "logged as left: " + fights);
			test.close();
		});
	}

	@GameTest(maxTicks = 9000, padding = TestWorld.PADDING)
	@Fight
	public void aKidWhoLeavesRightAfterTheWinIsLoggedOnlyOnce(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 1);
		boolean[] left = {false};
		helper.onEachTick(() -> {
			if (!left[0] && test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::isReturning).orElse(false)) {
				test.world.enterDoor(kid, 0);
				left[0] = true;
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(left[0], "wait for the win and the exit door");
			helper.assertTrue(test.world.hub().bounds().containsColumn(kid.getX(), kid.getZ()), "the kid is in the hub");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1 && fights.getFirst().won(), "the win is the only line: " + fights);
			test.close();
		});
	}

	@GameTest(maxTicks = 1200, padding = TestWorld.PADDING)
	public void aYeetFurtherThanTheKidsBestIsANewRecordForThatKid(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.log.append(new FightRecord(0L, kid.getPlainTextName(), 1, "default", FightRecord.WON, 11, 0, 0, 0, 3, 120, 1, "1.1.0", 1));
		test.whenLoaded(() -> test.world.enterDoor(kid, 1));

		helper.succeedWhen(() -> {
			helper.assertTrue(test.client.actionBarsTo.stream().anyMatch(bar -> bar.player() == kid && isRecord(bar.message())),
					"wait for a yeet further than the 1 block on file");
			helper.assertTrue(test.client.sounds.stream().anyMatch(sound -> sound.player() == kid && sound.sound() == SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
					"with a toast only the kid hears");
			test.close();
		});
	}

	@GameTest(maxTicks = 1200, padding = TestWorld.PADDING)
	public void aKidsFirstYeetOnFileIsNoRecordButGoesInTheFightsLog(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.whenLoaded(() -> test.world.enterDoor(kid, 1));
		boolean[] left = {false};
		helper.onEachTick(() -> {
			if (!left[0] && test.client.actionBars.stream().anyMatch(TafelBoemWorldGameTest::isDistanceMeter)) {
				left[0] = true;
				test.world.enterDoor(kid, 0);
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(left[0], "wait for the first yeet that is measured in distance");
			helper.assertFalse(test.client.actionBars.stream().anyMatch(TafelBoemWorldGameTest::isRecord), "the first yeet on file only sets the best");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(!fights.isEmpty() && fights.getLast().farthestYeet() > 0, "the fights log has the farthest yeet: " + fights);
			test.close();
		});
	}

	/**
	 * With all five R1.1 switches off the fight plays as 1.0.2: only the dizzy spell, never a wall
	 * splat, no reason on the meter and no record, even for a kid whose best is beaten.
	 */
	@GameTest(maxTicks = 2400, padding = TestWorld.PADDING)
	@Fight
	public void withEveryR11SwitchOffAFightPlaysAs102(GameTestHelper helper) {
		TafelBoemConfig off = new TafelBoemConfig();
		off.countingKippenbom = false;
		off.yeetBoost = false;
		off.yeetRecords = false;
		off.yeetCatalogue = false;
		off.bombSignatures = false;
		TestWorld test = new TestWorld(helper).withConfig(off).running();
		ServerPlayer kid = test.kidInTheHub();
		// A best of 1 block is on file, so any yeet beats it and a record would show if the switch failed.
		test.log.append(new FightRecord(0L, kid.getPlainTextName(), 1, "default", FightRecord.WON, 11, 0, 0, 0, 3, 120, 1, "1.1.0", 1));
		test.whenLoaded(() -> test.world.enterDoor(kid, 1));

		List<LandingStyle> landings = new ArrayList<>();
		LandingStyle[] last = {null};
		boolean[] splat = {false};
		helper.onEachTick(() -> {
			BossFight fight = test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).orElse(null);
			if (fight != null) {
				LandingStyle now = fight.boss().landingStyle();
				if (now != null && last[0] == null) {
					landings.add(now);
				}
				last[0] = now;
				splat[0] |= fight.boss().yeetPose() == YeetPose.SPLAT;
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(landings.size() >= 4, "wait for four yeets to land, saw " + landings.size());
			helper.assertTrue(landings.stream().allMatch(landing -> landing == LandingStyle.DIZZY), "only the dizzy spell: " + landings);
			helper.assertFalse(splat[0], "no wall splat");
			List<Bar> meters = test.client.actionBarsTo.stream().filter(bar -> bar.player() == kid && isDistanceMeter(bar.message())).toList();
			helper.assertTrue(!meters.isEmpty(), "the kid saw the yeet-o-meter");
			helper.assertTrue(meters.stream().allMatch(bar -> bar.message().getSiblings().isEmpty()), "no reason after the distance: " + meters);
			helper.assertFalse(test.client.actionBars.stream().anyMatch(TafelBoemWorldGameTest::isRecord), "no record");
			helper.assertFalse(test.client.sounds.stream().anyMatch(sound -> sound.sound() == SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), "no record toast");
			test.close();
		});
	}

	private static boolean isRecord(Component message) {
		return message.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals("tafelboem.message.yeet_record");
	}

	private static boolean isDistanceMeter(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& (translatable.getKey().equals("tafelboem.message.yeet_distance") || translatable.getKey().equals("tafelboem.message.yeet_out"));
	}

	private static boolean showsTwoStars(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals("tafelboem.door.stars")
				&& List.of(translatable.getArgs()).contains("★★☆");
	}
}
