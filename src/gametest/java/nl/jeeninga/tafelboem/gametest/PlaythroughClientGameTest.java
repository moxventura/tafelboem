package nl.jeeninga.tafelboem.gametest;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.client.CorrectionHud;
import nl.jeeninga.tafelboem.client.QuestionScreen;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.YeetPose;
import nl.jeeninga.tafelboem.core.YeetStyle;
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
			context.takeScreenshot("tafelboem-09-dizzy");

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
			world.getServer().runOnServer(server -> TafelBoem.fights().endAll());
		}
	}

	private static boolean faceDown(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			GraafFout boss = TafelBoem.fights().active().getFirst().boss();
			return boss.onGround() && boss.yeetPose() == YeetPose.FACEPLANT;
		});
	}

	/**
	 * A fresh Graaf Fout 8 blocks ahead, yeeted sideways across the view, with the camera assist
	 * following him the way it does after a kick.
	 */
	private static void yeetOnCue(TestSingleplayerContext world, YeetStyle style) {
		world.getServer().runOnServer(server -> {
			TafelBoem.fights().endAll();
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Direction facing = player.getDirection();
			Direction side = facing.getClockWise();
			Vec3 spot = player.position().add(facing.getStepX() * 8, 1.5, facing.getStepZ() * 8);
			TafelBoem.fights().summon(player.level(), spot);
			GraafFout boss = TafelBoem.fights().active().getFirst().boss();
			Vec3 from = spot.subtract(side.getStepX() * 2, 0, side.getStepZ() * 2);
			boss.yeet(from, 1.2, style, player.position());
			TafelBoem.sessions().client().track(player, boss, 60, false);
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
