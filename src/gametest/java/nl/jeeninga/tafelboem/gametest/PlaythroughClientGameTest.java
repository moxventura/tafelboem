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
import nl.jeeninga.tafelboem.client.CorrectionHud;
import nl.jeeninga.tafelboem.client.QuestionScreen;
import nl.jeeninga.tafelboem.core.BombTier;
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

			context.waitForScreen(QuestionScreen.class);
			context.takeScreenshot("tafelboem-07-boss-question");
			screen = (QuestionScreen) context.computeOnClient(client -> client.gui.screen());
			answer(context, screen.a() * screen.b());
			context.waitFor(client -> client.gui.screen() == null);
			context.getInput().lookAt(BlockPos.containing(bossSpot).above(2));
			context.waitTicks(14);
			context.takeScreenshot("tafelboem-08-yeet");
			context.waitTicks(30);
			context.takeScreenshot("tafelboem-09-dizzy");
		}
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
