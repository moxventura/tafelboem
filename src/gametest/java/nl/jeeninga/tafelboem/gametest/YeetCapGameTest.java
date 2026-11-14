package nl.jeeninga.tafelboem.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import nl.jeeninga.tafelboem.boss.BossFight;
import nl.jeeninga.tafelboem.boss.GraafFout;
import nl.jeeninga.tafelboem.core.ArenaBox;
import nl.jeeninga.tafelboem.core.FightLevel;
import nl.jeeninga.tafelboem.core.YeetPower;
import nl.jeeninga.tafelboem.core.YeetStyle;
import nl.jeeninga.tafelboem.world.ArenaRun;

/**
 * R1.4's cap on throw-back and super yeets by the room ahead ({@link YeetPower#capForRoom}): the classic yeet, the
 * longest flight these get, at the power that fits the room from the middle of a level 3 arena to its floor's edge,
 * must land on that floor, not beyond its wall. This checks {@link YeetPower#BLOCKS_PER_POWER} in a real flight.
 */
public class YeetCapGameTest {
	/** The floor of a level 3 arena ends 22.5 blocks from its middle; he must land a block short of that at least. */
	private static final double LANDS_WITHIN = 21.5;
	private static final double FLOOR_ENDS = 22.5;

	@GameTest(maxTicks = 400, padding = TestWorld.PADDING)
	@Fight
	public void aClassicYeetAtTheCapOnLevel3LandsInsideTheArena(GameTestHelper helper) {
		TestWorld test = new TestWorld(helper).running();
		ServerPlayer kid = test.kidInTheHub();
		test.world.enterDoor(kid, 3);

		boolean[] sent = {false};
		boolean[] landed = {false};
		double[] farthest = {0};
		helper.onEachTick(() -> {
			BossFight fight = test.world.run(FightLevel.MOEILIJK).map(ArenaRun::fight).orElse(null);
			if (fight == null || landed[0]) {
				return;
			}
			GraafFout boss = fight.boss();
			ArenaBox box = fight.arena();
			if (!sent[0]) {
				sent[0] = true;
				// From the middle, where the arena is widest, with the kicker 3 blocks behind him: he flies straight away from them.
				int ground = test.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) box.middleX(), (int) box.middleZ());
				boss.teleportTo(box.middleX(), ground + 1.5, box.middleZ());
				Vec3 kicker = new Vec3(box.middleX(), ground, box.middleZ() + 3);
				boss.yeet(kicker, YeetPower.capForRoom(FLOOR_ENDS - YeetPower.ROOM_MARGIN), YeetStyle.CLASSIC, kicker);
				return;
			}
			farthest[0] = Math.max(farthest[0], Math.hypot(boss.getX() - box.middleX(), boss.getZ() - box.middleZ()));
			// Down: the yeet has handed him to a landing.
			landed[0] = boss.landingStyle() != null;
		});

		helper.succeedWhen(() -> {
			helper.assertTrue(landed[0], "wait for him to land");
			helper.assertTrue(farthest[0] < LANDS_WITHIN, "he landed " + farthest[0] + " blocks from the middle, outside the floor");
			test.close();
		});
	}
}
