package nl.jeeninga.tafelboem.gametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.core.Fact;
import nl.jeeninga.tafelboem.game.BombEffects;
import nl.jeeninga.tafelboem.game.SessionManager;
import nl.jeeninga.tafelboem.log.AnswerLog;
import nl.jeeninga.tafelboem.log.Learners;

/**
 * The counting Kippenbom: a × b shows as a groups of b, and every chicken goes again, so 10 × 10 on
 * Moeilijk never piles up.
 */
public class ChickenGameTest {
	@GameTest(maxTicks = 300, padding = 48)
	public void threeTimesFourShowsThreeGroupsOfFourThatPoofAway(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		sessions.countChickens(helper.getLevel(), at, new Fact(3, 4), "test");
		helper.onEachTick(sessions::tick);

		helper.assertTrue(chickens(helper, at) == 3, "first one chicken per group");
		helper.runAfterDelay(30, () -> helper.assertTrue(chickens(helper, at) == 12, "then 3 groups of 4: " + chickens(helper, at)));
		helper.runAfterDelay(20 * 12, () -> {
			helper.assertTrue(chickens(helper, at) == 0, "after 10 s they are gone, not " + chickens(helper, at));
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100, padding = 48)
	public void noMoreThan150SpectacleAnimalsLiveAtOnce(GameTestHelper helper) {
		OpenField.open(helper);
		SessionManager sessions = sessions(helper);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		sessions.countChickens(helper.getLevel(), at, new Fact(10, 10), "test");
		sessions.countChickens(helper.getLevel(), at.add(8, 0, 0), new Fact(10, 10), "test");
		helper.onEachTick(sessions::tick);

		helper.runAfterDelay(30, () -> {
			int alive = chickens(helper, at);
			helper.assertTrue(alive <= SessionManager.MAX_SPECTACLES, "the oldest poof first: " + alive + " chickens");
			helper.assertTrue(alive >= 100, "the newest burst is whole: " + alive + " chickens");
			helper.succeed();
		});
	}

	private static int chickens(GameTestHelper helper, Vec3 around) {
		return helper.getLevel().getEntitiesOfClass(Chicken.class, new AABB(around, around).inflate(24),
				chicken -> chicken.entityTags().contains(BombEffects.SPECTACLE_TAG)).size();
	}

	private static SessionManager sessions(GameTestHelper helper) {
		try {
			return new SessionManager(helper.getLevel().getServer(), new Learners(new AnswerLog(Files.createTempDirectory("tafelboem-gametest"))),
					new RecordingQuizClient());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
