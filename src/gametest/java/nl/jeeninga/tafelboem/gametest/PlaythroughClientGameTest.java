package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.client.CorrectionHud;
import nl.jeeninga.tafelboem.client.QuestionScreen;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.core.LandingStyle;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.registry.ModBlocks;

/**
 * Plays through R0 in a real client and takes screenshots of every step, so the look and feel can
 * be reviewed without starting the game by hand. Run with {@code ./gradlew runClientGameTest};
 * screenshots land in {@code build/run/clientGameTest/screenshots}.
 */
@SuppressWarnings("UnstableApiUsage")
public class PlaythroughClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set day");
			world.getServer().runCommand("weather clear");
			context.waitTicks(20);

			// 1. A Knalletje, answered wrong: the screen closes, the soot cloud, then the fact on the HUD.
			BlockPos knalletje = placeBomb(world, BombTier.KNALLETJE, 3);
			context.getInput().lookAt(knalletje);
			startQuestion(world, BombTier.KNALLETJE, knalletje);
			context.waitForScreen(QuestionScreen.class);
			context.takeScreenshot("tafelboem-01-question");

			QuestionScreen screen = (QuestionScreen) context.computeOnClient(client -> client.gui.screen());
			int product = screen.a() * screen.b();
			checkButtons(context, screen);
			answer(context, product + 1);
			context.waitFor(client -> client.gui.screen() == null);
			context.waitTicks(5);
			context.takeScreenshot("tafelboem-02-penalty");
			context.waitFor(client -> CorrectionHud.isShowing());
			context.waitTicks(10);
			context.takeScreenshot("tafelboem-03-hud-correction");
			context.waitFor(client -> !CorrectionHud.isShowing());

			// 2. A Kippenbom, answered right: run, then chickens everywhere.
			BlockPos kippenbom = placeBomb(world, BombTier.KIPPENBOM, 5);
			context.getInput().lookAt(kippenbom);
			startQuestion(world, BombTier.KIPPENBOM, kippenbom);
			context.waitForScreen(QuestionScreen.class);
			screen = (QuestionScreen) context.computeOnClient(client -> client.gui.screen());
			answer(context, screen.a() * screen.b());
			context.waitFor(client -> client.gui.screen() == null);
			context.waitTicks(15);
			context.takeScreenshot("tafelboem-04-run");
			context.waitTicks(80);
			context.takeScreenshot("tafelboem-05-kippenbom-boom");

			// 2b. The counting Kippenbom on cue: the question is random, so a sum with 3 groups of 4 is
			// counted out directly, once the first Kippenbom's chickens have poofed. The first 3 chickens
			// pop into their groups 20 ticks after they fly out.
			for (int tick = 0; spectacleChickens(world, BlockPos.containing(spotAhead(world, 6))) > 0; tick++) {
				check(tick < 300, "the first Kippenbom's chickens should poof");
				context.waitTicks(1);
			}
			BlockPos counting = BlockPos.containing(spotAhead(world, 10));
			world.getServer().runOnServer(server -> TafelBoem.sessions().countChickens(server.overworld(),
					Vec3.atCenterOf(counting).add(0, 1, 0), new Fact(3, 4), "client test"));
			for (int tick = 0; spectacleChickens(world, counting) < 12; tick++) {
				check(tick < 100, "the 3 chickens should pop into 12");
				context.waitTicks(1);
			}
			lookAtChickens(context, world, counting);
			// Just after the pop the groups still stand apart; by 15 ticks they have spread into a flock.
			context.waitTicks(6);
			context.takeScreenshot("tafelboem-r1.1-counting");

			// 3. Graaf Fout: summon, wait for his bomb, kick it back, watch him fly.
			Vec3 bossSpot = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Direction facing = player.getDirection();
				Vec3 spot = player.position().add(facing.getStepX() * 9, 1.5, facing.getStepZ() * 9);
				TafelBoem.fights().summon(player.level(), spot);
				return spot;
			});
			context.getInput().lookAt(BlockPos.containing(bossSpot).above());
			context.waitTicks(30);
			context.takeScreenshot("tafelboem-06-graaf-fout");

			// Look the other way: the camera assist must turn us back to him (playtest 1, P8).
			Vec3 behind = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				return player.position().multiply(2, 1, 2).subtract(bossSpot.x, 0, bossSpot.z);
			});
			context.getInput().lookAt(BlockPos.containing(behind));

			context.waitForScreen(QuestionScreen.class);
			context.waitTicks(20);
			context.takeScreenshot("tafelboem-07-boss-question");
			screen = (QuestionScreen) context.computeOnClient(client -> client.gui.screen());
			answer(context, screen.a() * screen.b());
			context.waitFor(client -> client.gui.screen() == null);
			context.waitTicks(14);
			context.takeScreenshot("tafelboem-08-yeet");
			context.waitTicks(30);
			context.takeScreenshot("tafelboem-09-landing");

			// 4. The tumble and the boomerang's faceplant, on cue, since a fight picks yeets at random.
			yeetOnCue(world, YeetStyle.TUMBLE);
			context.waitTicks(10);
			context.takeScreenshot("tafelboem-10-tumble");
			context.waitTicks(4);
			context.takeScreenshot("tafelboem-11-tumble-later");

			yeetOnCue(world, YeetStyle.BOOMERANG);
			for (int tick = 0; !faceDown(world); tick++) {
				check(tick < 100, "the boomerang should end with Graaf Fout face down on the ground");
				context.waitTicks(1);
			}
			context.waitTicks(8);
			context.takeScreenshot("tafelboem-12-faceplant");
			BlockPos faceDownAt = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				GraafFout boss = TafelBoem.fights().active().getFirst().boss();
				Vec3 side = Vec3.directionFromRotation(0, boss.getYRot() + 90F).scale(4);
				player.teleportTo(boss.getX() + side.x, boss.getY(), boss.getZ() + side.z);
				return boss.blockPosition();
			});
			context.waitTicks(2);
			context.getInput().lookAt(faceDownAt);
			context.waitTicks(2);
			context.takeScreenshot("tafelboem-13-faceplant-side");

			// 5. The R1.1 landings and the wall splat, on cue, since a fight picks them at random. Chat
			// is hidden: Graaf Fout's lines would cover the low poses.
			context.runOnClient(client -> client.options.chatVisibility().set(ChatVisiblity.HIDDEN));
			for (LandingStyle landing : List.of(LandingStyle.HEAD_FIRST, LandingStyle.PANCAKE, LandingStyle.TOE_HOP, LandingStyle.SOOT,
					LandingStyle.CHASED)) {
				yeetOnCue(world, YeetStyle.CLASSIC, landing);
				for (int tick = 0; !landedIn(world, landing); tick++) {
					check(tick < 150, "the yeet should end in " + landing);
					context.waitTicks(1);
				}
				context.waitTicks(10);
				String name = "tafelboem-r1.1-" + landing.id().replace('_', '-');
				lookAtBoss(context, world);
				context.takeScreenshot(name);
				if (landing == LandingStyle.HEAD_FIRST || landing == LandingStyle.SOOT) {
					// Both leave something on him (upside down, a black face) that must be gone once he is up.
					for (int tick = 0; landedIn(world, landing); tick++) {
						check(tick < 300, "the " + landing + " landing should end");
						context.waitTicks(1);
					}
					context.waitTicks(10);
					lookAtBoss(context, world);
					context.takeScreenshot(name + "-after");
				}
			}
			Direction toWall = splatOnCue(world);
			for (int tick = 0; !posed(world, YeetPose.SPLAT); tick++) {
				check(tick < 100, "the wall splat should end flat against the wall");
				context.waitTicks(1);
			}
			context.waitTicks(4);
			closeQuestion(context, world);
			// 4 blocks from him on the open side of the wall, so he reads against the obsidian.
			BlockPos splatted = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				GraafFout boss = TafelBoem.fights().active().getFirst().boss();
				player.teleportTo(boss.getX() - toWall.getStepX() * 4, boss.getY(), boss.getZ() - toWall.getStepZ() * 4);
				return boss.blockPosition();
			});
			context.waitTicks(2);
			context.getInput().lookAt(splatted);
			context.waitTicks(2);
			context.takeScreenshot("tafelboem-r1.1-wall-splat");
			context.runOnClient(client -> client.options.chatVisibility().set(ChatVisiblity.FULL));
			world.getServer().runOnServer(server -> TafelBoem.fights().endAll());
		}
	}

	private static boolean faceDown(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			GraafFout boss = TafelBoem.fights().active().getFirst().boss();
			return boss.onGround() && boss.yeetPose() == YeetPose.FACEPLANT;
		});
	}

	private static void yeetOnCue(TestSingleplayerContext world, YeetStyle style) {
		yeetOnCue(world, style, null);
	}

	/**
	 * A fresh Graaf Fout 8 blocks ahead, yeeted sideways across the view, with the camera assist
	 * following him the way it does after a kick.
	 *
	 * @param landing how he lands; null lets the fight pick
	 */
	private static void yeetOnCue(TestSingleplayerContext world, YeetStyle style, LandingStyle landing) {
		world.getServer().runOnServer(server -> {
			TafelBoem.fights().endAll();
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Direction facing = player.getDirection();
			Direction side = facing.getClockWise();
			Vec3 spot = player.position().add(facing.getStepX() * 8, 1.5, facing.getStepZ() * 8);
			TafelBoem.fights().summon(player.level(), spot);
			BossFight fight = TafelBoem.fights().active().getFirst();
			if (landing != null) {
				fight.queueLanding(landing);
			}
			Vec3 from = spot.subtract(side.getStepX() * 2, 0, side.getStepZ() * 2);
			fight.boss().yeet(from, 1.2, style, player.position());
			TafelBoem.sessions().client().track(player, fight.boss(), 100, false);
		});
	}

	/**
	 * A fight that goes on throws a bomb at us, and its question covers the screen and roots us; the
	 * session ends, then we stand 4 blocks beside Graaf Fout, looking at him, so the pose can be seen.
	 */
	private static void lookAtBoss(ClientGameTestContext context, TestSingleplayerContext world) {
		closeQuestion(context, world);
		BlockPos at = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			GraafFout boss = TafelBoem.fights().active().getFirst().boss();
			Vec3 side = Vec3.directionFromRotation(0, boss.getYRot() + 90F).scale(4);
			player.teleportTo(boss.getX() + side.x, boss.getY(), boss.getZ() + side.z);
			return boss.blockPosition();
		});
		context.waitTicks(2);
		context.getInput().lookAt(at);
		context.waitTicks(2);
	}

	/**
	 * The question a fight's bomb opened would root us in place, so drop the session on the server and
	 * close the screen. This is a test shortcut; the kid's own ESC would count as a chicken-out.
	 */
	private static void closeQuestion(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> TafelBoem.sessions().onDisconnect(server.getPlayerList().getPlayers().getFirst()));
		context.runOnClient(client -> {
			if (client.gui.screen() instanceof QuestionScreen) {
				client.gui.setScreen(null);
			}
		});
		context.waitTicks(2);
	}

	/** The counting chickens' groups fill the view: stand 8 blocks from the middle of them and look at it. */
	private static void lookAtChickens(ClientGameTestContext context, TestSingleplayerContext world, BlockPos bomb) {
		BlockPos middle = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Vec3 sum = Vec3.ZERO;
			List<Chicken> chickens = spectacles(player.level(), bomb);
			for (Chicken chicken : chickens) {
				sum = sum.add(chicken.position());
			}
			Vec3 centre = sum.scale(1.0 / chickens.size());
			Vec3 away = Vec3.atBottomCenterOf(bomb).subtract(player.position()).multiply(1, 0, 1).normalize().scale(-8);
			player.teleportTo(centre.x + away.x, bomb.getY(), centre.z + away.z);
			return BlockPos.containing(centre);
		});
		context.waitTicks(2);
		context.getInput().lookAt(middle);
		context.waitTicks(2);
	}

	private static Vec3 spotAhead(TestSingleplayerContext world, int distance) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			return player.position().add(player.getDirection().getStepX() * distance, 0, player.getDirection().getStepZ() * distance);
		});
	}

	private static int spectacleChickens(TestSingleplayerContext world, BlockPos bomb) {
		return world.getServer().computeOnServer(server -> spectacles(server.overworld(), bomb).size());
	}

	private static List<Chicken> spectacles(Level level, BlockPos around) {
		return level.getEntitiesOfClass(Chicken.class, new AABB(around).inflate(30),
				chicken -> chicken.entityTags().contains(BombEffects.SPECTACLE_TAG));
	}

	private static boolean landedIn(TestSingleplayerContext world, LandingStyle landing) {
		return world.getServer().computeOnServer(server -> TafelBoem.fights().active().getFirst().boss().landingStyle() == landing);
	}

	private static boolean posed(TestSingleplayerContext world, YeetPose pose) {
		return world.getServer().computeOnServer(server -> TafelBoem.fights().active().getFirst().boss().yeetPose() == pose);
	}

	/** Graaf Fout 8 blocks ahead with an obsidian wall 5 blocks to his side, splatted against it. Returns the way to the wall. */
	private static Direction splatOnCue(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			TafelBoem.fights().endAll();
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Direction facing = player.getDirection();
			Direction side = facing.getClockWise();
			Vec3 spot = player.position().add(facing.getStepX() * 8, 1.5, facing.getStepZ() * 8);
			BlockPos wall = BlockPos.containing(spot.add(side.getStepX() * 5, -1.5, side.getStepZ() * 5));
			for (int along = -3; along <= 3; along++) {
				for (int up = 0; up < 4; up++) {
					player.level().setBlockAndUpdate(wall.relative(facing, along).above(up), Blocks.OBSIDIAN.defaultBlockState());
				}
			}
			TafelBoem.fights().summon(player.level(), spot);
			GraafFout boss = TafelBoem.fights().active().getFirst().boss();
			boss.yeet(spot.subtract(side.getStepX() * 2, 0, side.getStepZ() * 2), 1.0, YeetStyle.WALL_SPLAT, player.position());
			TafelBoem.sessions().client().track(player, boss, 60, false);
			return side;
		});
	}

	/** Playtest 1: the OK button was too small and the kids hit "?" next to it instead. */
	private static void checkButtons(ClientGameTestContext context, QuestionScreen screen) {
		context.runOnClient(client -> {
			Button ok = screen.okButton();
			Button dontKnow = screen.dontKnowButton();
			check(ok != null && dontKnow != null, "both OK and ? are on the screen");
			check(!ok.active, "OK is greyed out until something is typed");
			check(ok.getWidth() >= 48, "OK is wide, like Minecraft's Klaar button: " + ok.getWidth());
			int dx = Math.max(0, Math.max(ok.getX() - (dontKnow.getX() + dontKnow.getWidth()), dontKnow.getX() - (ok.getX() + ok.getWidth())));
			int dy = Math.max(0, Math.max(ok.getY() - (dontKnow.getY() + dontKnow.getHeight()), dontKnow.getY() - (ok.getY() + ok.getHeight())));
			check(Math.hypot(dx, dy) >= 40, "? is far away from OK: " + Math.hypot(dx, dy));
		});
		context.getInput().typeChars("1");
		context.waitTicks(3);
		context.runOnClient(client -> {
			Button ok = screen.okButton();
			check(ok != null && ok.active && ok.isFocused(), "after typing, OK is active and has the focus outline");
		});
		context.takeScreenshot("tafelboem-01b-typed");
		context.getInput().pressKey(GLFW.GLFW_KEY_BACKSPACE);
		context.waitTicks(2);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static BlockPos placeBomb(TestSingleplayerContext world, BombTier tier, int distance) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos pos = player.blockPosition().relative(player.getDirection(), distance);
			player.level().setBlock(pos, ModBlocks.bomb(tier).defaultBlockState(), Block.UPDATE_ALL);
			return pos;
		});
	}

	private static void startQuestion(TestSingleplayerContext world, BombTier tier, BlockPos pos) {
		world.getServer().runOnServer(server ->
				TafelBoem.sessions().startFromBlock(server.getPlayerList().getPlayers().getFirst(), tier, pos));
	}

	private static void answer(ClientGameTestContext context, int value) {
		context.getInput().typeChars(Integer.toString(value));
		context.waitTicks(5);
		context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
	}
}
