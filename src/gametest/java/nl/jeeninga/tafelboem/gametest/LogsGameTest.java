package nl.jeeninga.tafelboem.gametest;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.core.AnswerRecord;
import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.FightRecord;
import nl.jeeninga.tafelboem.core.YeetKind;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * Playtest 6 is judged from the game's log lines, so the numbers must really get there: a Terugbom caught, thrown
 * back and answered right shows in the fight line's counts, in the answer log's context and in the yeet line's mark.
 */
public class LogsGameTest {
	@GameTest(maxTicks = 600, padding = TestWorld.PADDING)
	@Fight
	public void aCaughtThrownBackAndAnsweredTerugbomEndsUpInTheLogs(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 1);
		BossFight[] seen = {null};
		int[] step = {0};
		helper.onEachTick(() -> {
			BossFight fight = test.world.run(FightLevel.MAKKELIJK).map(ArenaRun::fight).orElse(null);
			if (fight == null) {
				return;
			}
			seen[0] = fight;
			if (step[0] == 0) {
				// He is there: a Terugbom drops next to the kid, who catches it (the selected slot, the hand).
				fight.dropTerugbom(kid.position().add(1, 0, 0), BombTier.KNALLETJE);
				step[0] = 1;
			} else if (step[0] == 1 && fight.tally(kid.getUUID()).caught() == 1) {
				ArenaFight.aimAt(kid, fight.boss().position().add(0, fight.boss().getBbHeight() / 2, 0));
				fight.throwBack(kid, InteractionHand.MAIN_HAND);
				step[0] = 2;
			} else if (step[0] == 2 && fight.tally(kid.getUUID()).terugRight() == 1 && fight.lastYeet() != null) {
				// TestWorld answered the Terug! question right; his flight is over. The exit door writes the kid's fight line.
				step[0] = 3;
				test.world.enterDoor(kid, 0);
			}
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(step[0] == 3, "wait for the throw-back, its answer and his landing");
			List<FightRecord> fights = test.log.read(kid.getPlainTextName());
			helper.assertTrue(fights.size() == 1, "the exit door wrote the fight line: " + fights);
			FightRecord line = fights.getFirst();
			helper.assertTrue(line.caught() == 1 && line.thrownBack() == 1 && line.backHits() == 1 && line.juggles() == 0, "the counts: " + line.describe());
			helper.assertTrue(line.terugRight() == 1 && line.terugWrong() == 0, "the Terug! answers: " + line.describe());
			helper.assertTrue(line.describe().contains("caught 1, threw back 1 (1 hit), Terug! 1 right 0 wrong"), "and the line says so: " + line.describe());
			helper.assertTrue(seen[0].lastYeet().kind() == YeetKind.BACK, "the yeet line marks the throw-back: " + seen[0].lastYeet().describe());
			helper.assertTrue(seen[0].lastYeet().high() || line.farthestBack() > 0, "and the fight line has the farthest throw-back: " + line.describe());
			List<AnswerRecord> answers = test.sessions.learners().log().read(kid.getPlainTextName());
			helper.assertTrue(answers.stream().anyMatch(answer -> answer.context().equals("terug") && answer.correct() && answer.millis() >= 0),
					"the answer log has the sum with its time and the context terug: " + answers);
			test.close();
		});
	}
}
