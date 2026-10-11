package nl.jeeninga.tafelboem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.net.CameraS2C;

/**
 * Playtest 1: after being hit, kids sometimes looked the other way and missed the yeet they had
 * earned. While a fight question is open the view turns smoothly to Graaf Fout, and after a correct
 * answer it keeps following him for a moment. The kick also gives the camera a small bump.
 */
public final class CameraAssist {
	private static final float TURN = 0.25F;
	/** Pitch change per tick for the kick bump: dip down, then spring back. */
	private static final float[] BUMP = {2.5F, 1.5F, -1.5F, -2.5F};

	private static int entityId = -1;
	private static int ticksAfterScreen;
	private static boolean kickPending;
	private static int bumpTick = -1;

	private CameraAssist() {
	}

	static void follow(CameraS2C request) {
		entityId = request.entityId();
		ticksAfterScreen = request.ticksAfterScreen();
		kickPending |= request.kick();
	}

	static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			entityId = -1;
			kickPending = false;
			bumpTick = -1;
			return;
		}
		boolean questionOpen = client.gui.screen() instanceof QuestionScreen;

		if (kickPending && !questionOpen) {
			kickPending = false;
			bumpTick = 0;
		}
		if (bumpTick >= 0) {
			player.setXRot(Mth.clamp(player.getXRot() + BUMP[bumpTick], -90F, 90F));
			bumpTick = bumpTick + 1 < BUMP.length ? bumpTick + 1 : -1;
		}

		if (entityId < 0 || !TafelBoem.config().cameraAssist) {
			return;
		}
		if (!questionOpen && ticksAfterScreen-- <= 0) {
			entityId = -1;
			return;
		}
		Entity target = client.level.getEntity(entityId);
		if (target == null || target.isRemoved()) {
			entityId = -1;
			return;
		}
		turnTowards(player, target.position().add(0, target.getBbHeight() * 0.6, 0));
	}

	private static void turnTowards(LocalPlayer player, Vec3 point) {
		Vec3 to = point.subtract(player.getEyePosition());
		double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
		float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90F;
		float pitch = (float) -(Mth.atan2(to.y, horizontal) * Mth.RAD_TO_DEG);

		float newYaw = player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()) * TURN;
		player.setYRot(newYaw);
		player.setYHeadRot(newYaw);
		player.setXRot(Mth.clamp(player.getXRot() + (pitch - player.getXRot()) * TURN, -90F, 90F));
	}
}
