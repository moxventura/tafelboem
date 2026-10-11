package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.BossFights;
import nl.jeeninga.tafelboem.boss.GraafFout;
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

	@GameTest(maxTicks = 200, padding = PADDING)
	public void classicYeetLands(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.CLASSIC, (kid, boss) -> { });
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void bounceYeetLands(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.BOUNCE, (kid, boss) -> { });
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void helicopterYeetLands(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.HELICOPTER, (kid, boss) -> { });
	}

	@GameTest(maxTicks = 200, padding = PADDING)
	public void boomerangYeetLandsNearTheKid(GameTestHelper helper) {
		yeetAndLand(helper, YeetStyle.BOOMERANG, (kid, boss) ->
				helper.assertTrue(boss.position().distanceTo(kid.position()) < 8, "the boomerang comes back to the kid"));
	}

	@SuppressWarnings("removal") // mock players are still the only way to test without a client
	private static void yeetAndLand(GameTestHelper helper, YeetStyle style, BiConsumer<ServerPlayer, GraafFout> alsoCheck) {
		RecordingQuizClient client = new RecordingQuizClient();
		SessionManager sessions = new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(tempDir())), client);
		BossFights fights = new BossFights(sessions);
		helper.onEachTick(() -> {
			sessions.tick();
			fights.tick();
		});

		ServerPlayer kid = helper.makeMockServerPlayerInLevel();
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		kid.teleportTo(stand.x, stand.y, stand.z);
		helper.assertTrue(fights.summon(helper.getLevel(), stand.add(5, 0, 0)), "Graaf Fout should appear");
		BossFight fight = fights.active().getFirst();

		fight.boss().yeet(kid.position(), 1.0, style, kid.position());

		helper.succeedWhen(() -> {
			helper.assertFalse(fight.boss().isReacting(), style + ": he should be back on his feet");
			helper.assertTrue(client.actionBars.stream().anyMatch(YeetGameTest::isYeetDistance),
					style + ": the yeet-o-meter should say how far he flew");
			alsoCheck.accept(kid, fight.boss());
			fights.endAll();
		});
	}

	private static boolean isYeetDistance(Component message) {
		return message.getContents() instanceof TranslatableContents translatable
				&& translatable.getKey().equals("tafelboem.message.yeet_distance");
	}

	private static java.nio.file.Path tempDir() {
		try {
			return Files.createTempDirectory("tafelboem-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
