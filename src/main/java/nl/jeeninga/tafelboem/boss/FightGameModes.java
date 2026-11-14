package nl.jeeninga.tafelboem.boss;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * No breaking or placing in a fight (R1.4): a ring of dirt dodged every bomb in 1.3.0. Kids in the arena of a
 * running fight are in adventure mode; their own mode comes back when the fight ends, they leave the arena, die
 * or go offline. A kid who is already in adventure mode or spectating is left alone, and nothing is remembered
 * for them.
 */
final class FightGameModes {
	private final Map<UUID, GameType> own = new HashMap<>();

	/** Once a tick: every kid in the fight goes to adventure mode; every kid who left, died or went offline gets their own mode back. */
	void update(MinecraftServer server, List<ServerPlayer> inFight) {
		for (ServerPlayer kid : inFight) {
			GameType mode = kid.gameMode.getGameModeForPlayer();
			if (kid.isAlive() && !own.containsKey(kid.getUUID()) && (mode == GameType.SURVIVAL || mode == GameType.CREATIVE)) {
				own.put(kid.getUUID(), mode);
				kid.setGameMode(GameType.ADVENTURE);
			}
		}
		for (UUID id : List.copyOf(own.keySet())) {
			ServerPlayer kid = server.getPlayerList().getPlayer(id);
			if (kid == null) {
				own.remove(id);
			} else if (!kid.isAlive() || !inFight.contains(kid)) {
				restore(kid);
			}
		}
	}

	/** The fight is over. */
	void restoreAll(MinecraftServer server) {
		for (UUID id : List.copyOf(own.keySet())) {
			ServerPlayer kid = server.getPlayerList().getPlayer(id);
			if (kid == null) {
				own.remove(id);
			} else {
				restore(kid);
			}
		}
	}

	private void restore(ServerPlayer kid) {
		GameType mode = own.remove(kid.getUUID());
		// Only from adventure: a kid the parent put in another mode meanwhile keeps that one.
		if (mode != null && kid.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
			kid.setGameMode(mode);
		}
	}

	/**
	 * A kid who logs in while no fight holds them but is in adventure mode was saved there by a fight that never got
	 * to end (a crash, the process killed): Minecraft saved the mode before that as the previous one, or the
	 * server's default stands in.
	 */
	static void afterLogin(ServerPlayer kid) {
		if (kid.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
			return;
		}
		GameType before = kid.gameMode.getPreviousGameModeForPlayer();
		kid.setGameMode(before != null && before != GameType.ADVENTURE ? before : kid.level().getServer().getDefaultGameType());
	}
}
