package nl.jeeninga.tafelboem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.core.CameraNudge;
import nl.jeeninga.tafelboem.net.CameraS2C;

/**
 * Playtest 1: after being hit, kids sometimes looked the other way and missed the yeet they had
 * earned. While a fight question is open the view turns smoothly to Graaf Fout, and after a correct
 * answer it keeps following him for a moment. The kick also gives the camera a small bump.
 *
 * <p>Playtest 3 (P29): following him all the way took the view away from the kids. With
 * {@code cameraNudge} on, the view only gets a short nudge after the question closes (see
 * {@link CameraNudge}), and a landing that asks for the view only nudges when he is out of view.
 */
public final class CameraAssist {
	private static final float TURN = 0.25F;
	/** Pitch change per tick for the kick bump: dip down, then spring back. */
	private static final float[] BUMP = {2.5F, 1.5F, -1.5F, -2.5F};

	private static int entityId = -1;
	private static int ticksAfterScreen;
	private static boolean kickPending;
	private static int bumpTick = -1;
	/** Ticks this nudge has turned the view so far. */
	private static int nudgeTicks;
	/** A request after he landed: nudge only if he is out of view, which the first tick checks. */
	private static boolean landingCheck;

	private CameraAssist() {
	}

	static void follow(CameraS2C request) {
		entityId = request.entityId();
		ticksAfterScreen = request.ticksAfterScreen();
		kickPending |= request.kick();
		nudgeTicks = 0;
		landingCheck = !request.kick() && request.ticksAfterScreen() > 0;
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
		Vec3 point = target.position().add(0, target.getBbHeight() * 0.6, 0);
		if (!questionOpen && TafelBoem.config().cameraNudge && !keepNudging(player, point)) {
			entityId = -1;
			return;
		}
		turnTowards(player, point);
	}

	/** Whether the nudge towards {@code point} goes on this tick; counts the tick when it does. */
	private static boolean keepNudging(LocalPlayer player, Vec3 point) {
		float offBy = CameraNudge.offBy(player.getYRot(), player.getXRot(), yawTo(player, point), pitchTo(player, point));
		if (landingCheck) {
			landingCheck = false;
			if (!CameraNudge.landingNeedsNudge(offBy)) {
				return false;
			}
		}
		return !CameraNudge.done(nudgeTicks++, offBy);
	}

	private static float yawTo(LocalPlayer player, Vec3 point) {
		Vec3 to = point.subtract(player.getEyePosition());
		return (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90F;
	}

	private static float pitchTo(LocalPlayer player, Vec3 point) {
		Vec3 to = point.subtract(player.getEyePosition());
		double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
		return (float) -(Mth.atan2(to.y, horizontal) * Mth.RAD_TO_DEG);
	}

	private static void turnTowards(LocalPlayer player, Vec3 point) {
		float yaw = yawTo(player, point);
		float pitch = pitchTo(player, point);

		float newYaw = player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()) * TURN;
		player.setYRot(newYaw);
		player.setYHeadRot(newYaw);
		player.setXRot(Mth.clamp(player.getXRot() + (pitch - player.getXRot()) * TURN, -90F, 90F));
	}
}
