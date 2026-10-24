package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.YeetBoost;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;

/**
 * Every yeet must end with Graaf Fout back on the ground and the yeet-o-meter telling the kids how
 * far he flew; a style that leaves him floating would stall the fight.
 */
public class YeetGameTest {
	/** Yeets fly well outside the test structure; keep other tests out of the way. */
	private static final int PADDING = 48;
	/** The yeet power range of a Knalletje and of a Gewone TNT (random 0.9–1.4 times the bomb's power). */
	private static final double WEAKEST_KNALLETJE = 0.9;
	private static final double STRONGEST_GEWONE_TNT = 1.4 * 1.5;
	/** A boosted Kippenbom: the strongest yeet there is. */
	private static final double FULL_POWER = 1.8 * 1.4 * YeetBoost.MAX;

	private static final String DISTANCE = "tafelboem.message.yeet_distance";
	private static final String HEIGHT = "tafelboem.message.yeet_height";

	@GameTest(maxTicks = 200, padding = PADDING)
	public void classicYeetLands(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.CLASSIC, 1.0, landed -> landed.assertMeter(DISTANCE));
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void bounceYeetLands(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.BOUNCE, 1.0, landed -> landed.assertMeter(DISTANCE));
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void aGewoneTntBounceStaysNearTheArena(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.BOUNCE, STRONGEST_GEWONE_TNT, landed -> {
			int blocks = landed.assertMeter(DISTANCE);
			helper.assertTrue(blocks >= 10 && blocks <= 20, "the strongest bounce should land 10–20 blocks away, not " + blocks);
		});
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void aKnalletjeHelicopterSaysHowHighHeFlew(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.HELICOPTER, WEAKEST_KNALLETJE, landed -> {
			int blocks = landed.assertMeter(HEIGHT);
			helper.assertTrue(blocks >= 4 && blocks <= 8, "a weak helicopter should rise 4–8 blocks, not " + blocks);
		});
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void aGewoneTntHelicopterFliesHigher(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.HELICOPTER, STRONGEST_GEWONE_TNT, landed -> {
			int blocks = landed.assertMeter(HEIGHT);
			helper.assertTrue(blocks >= 9, "a strong helicopter should rise at least 9 blocks, not " + blocks);
		});
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void aFullPowerHelicopterComesDownBeforeTheYeetTimesOut(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.HELICOPTER, FULL_POWER, landed -> {
			landed.assertMeter(HEIGHT);
			helper.assertTrue(landed.onGroundWhenMeasured(), "he lands on the ground, not in mid-air when his 5 s are up");
		});
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void boomerangFaceplantsInFrontOfTheKid(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.BOOMERANG, 1.0, landed -> {
			landed.assertMeter(DISTANCE);
			double distance = landed.boss().position().distanceTo(landed.kid().position());
			helper.assertTrue(distance < 8, "the boomerang comes back to the kid");
			helper.assertTrue(distance >= 3, "he lands in front of the kid, not on top of them");
			helper.assertTrue(landed.faceplanted(), "he lies on his face once he is down");
			helper.assertTrue(landed.facedTheKid(), "he falls on his face towards the kid");
			helper.assertTrue(landed.boss().yeetPose() == YeetPose.NONE, "he gets back up after being dizzy");
		});
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void tumbleYeetTumblesThroughTheAirAndLandsUpright(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.TUMBLE, 1.0, landed -> {
			landed.assertMeter(DISTANCE);
			helper.assertTrue(landed.posesInFlight().contains(YeetPose.TUMBLE), "he rolls and flips in the air");
			helper.assertTrue(landed.boss().yeetPose() == YeetPose.NONE, "he is upright again after landing");
		});
	}

	/** The wall's distance from the kid: 6 blocks behind Graaf Fout, the way he gets kicked. */
	private static final int WALL = 11;

	@GameTest(maxTicks = 200, padding = PADDING)
	public void theWallSplatSticksToTheWallAndSlidesDown(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.WALL_SPLAT, 1.0, stand -> {
			// In an arena this is the obsidian wall.
			for (int up = 0; up < 4; up++) {
				for (int side = -4; side <= 4; side++) {
					helper.getLevel().setBlockAndUpdate(BlockPos.containing(stand.x + WALL, stand.y + up, stand.z + side),
							Blocks.OBSIDIAN.defaultBlockState());
				}
			}
		}, landed -> {
			landed.assertMeter(DISTANCE);
			helper.assertTrue(landed.posesInFlight().contains(YeetPose.SPLAT), "he goes flat against the wall");
			helper.assertTrue(landed.kid().getX() + WALL - landed.boss().getX() < 2, "he slid down the wall, he didn't land somewhere else");
			helper.assertTrue(landed.boss().yeetPose() == YeetPose.NONE, "he is back in shape after the landing");
		});
	}

	/** What a yeet looked like, for the checks after he is back on his feet. */
	private record Landed(GameTestHelper helper, ServerPlayer kid, GraafFout boss, Component meter, Set<YeetPose> posesInFlight,
			boolean faceplanted, boolean facedTheKid, boolean onGroundWhenMeasured) {
		/** Checks which yeet-o-meter message he got and returns the number of blocks. */
		int assertMeter(String key) {
			TranslatableContents contents = (TranslatableContents) meter.getContents();
			helper.assertTrue(contents.getKey().equals(key), "the yeet-o-meter should use " + key + ", not " + contents.getKey());
			return (Integer) contents.getArgs()[1];
		}
	}

	private static void yeetAndLand(GameTestHelper helper, YeetStyle style, double power, Consumer<Landed> check) {
		yeetAndLand(helper, style, power, stand -> {
		}, check);
	}

	/** @param scenery builds what the yeet needs around the spot where the kid stands */
	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	private static void yeetAndLand(GameTestHelper helper, YeetStyle style, double power, Consumer<Vec3> scenery, Consumer<Landed> check) {
		OpenField.open(helper);
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);

		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		kid.teleportTo(stand.x, stand.y, stand.z);
		scenery.accept(stand);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(5, 0, 0)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();
		GraafFout boss = fight.boss();

		Set<YeetPose> poses = EnumSet.noneOf(YeetPose.class);
		boolean[] faceplanted = {false};
		boolean[] facedTheKid = {false};
		boolean[] measured = {false};
		boolean[] onGround = {false};
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
			if (boss.isReacting()) {
				poses.add(boss.yeetPose());
			}
			if (boss.onGround() && boss.yeetPose() == YeetPose.FACEPLANT) {
				faceplanted[0] = true;
				Vec3 facing = Vec3.directionFromRotation(0, boss.getYRot());
				Vec3 toKid = kid.position().subtract(boss.position()).multiply(1, 0, 1).normalize();
				facedTheKid[0] |= facing.dot(toKid) > 0.7;
			}
			if (!measured[0] && client.actionBars.stream().anyMatch(YeetGameTest::isYeetOMeter)) {
				measured[0] = true;
				onGround[0] = boss.onGround();
			}
		});

		boss.yeet(kid.position(), power, style, kid.position());

		helper.succeedWhen(() -> {
			helper.assertFalse(boss.isReacting(), style + ": he should be back on his feet");
			Component meter = client.actionBars.stream().filter(YeetGameTest::isYeetOMeter).reduce((first, second) -> second)
					.orElseThrow(() -> helper.assertionException(Component.literal(style + ": the yeet-o-meter should say how far he flew")));
			check.accept(new Landed(helper, kid, boss, meter, poses, faceplanted[0], facedTheKid[0], onGround[0]));
			fights.endAll();
		});
	}

	private static boolean isYeetOMeter(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& (translatable.getKey().equals(DISTANCE) || translatable.getKey().equals(HEIGHT));
	}

	private static java.nio.file.Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
