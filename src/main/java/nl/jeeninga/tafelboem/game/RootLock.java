package nl.jeeninga.tafelboem.game;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Roots a player in place while they answer. Transient modifiers are never saved, so a crash or
 * disconnect can't leave a child stuck.
 */
public final class RootLock {
	private static final AttributeModifier ROOTED = new AttributeModifier(TafelBoem.id("rooted"), -1.0,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final double MAX_DRIFT_SQR = 0.3 * 0.3;

	private RootLock() {
	}

	public static void apply(ServerPlayer player) {
		modify(player, Attributes.MOVEMENT_SPEED, true);
		modify(player, Attributes.JUMP_STRENGTH, true);
		player.stopRiding();
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
	}

	public static void release(ServerPlayer player) {
		modify(player, Attributes.MOVEMENT_SPEED, false);
		modify(player, Attributes.JUMP_STRENGTH, false);
	}

	/**
	 * Client-side prediction and knockback can still nudge a rooted player, so pull them back.
	 */
	public static void pin(ServerPlayer player, Vec3 anchor) {
		double dx = player.getX() - anchor.x;
		double dz = player.getZ() - anchor.z;
		if (dx * dx + dz * dz > MAX_DRIFT_SQR) {
			player.teleportTo(anchor.x, player.getY(), anchor.z);
		}
	}

	private static void modify(ServerPlayer player, Holder<Attribute> attribute, boolean add) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (add) {
			instance.addOrUpdateTransientModifier(ROOTED);
		} else {
			instance.removeModifier(ROOTED.id());
		}
	}
}
