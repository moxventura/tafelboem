package nl.jeeninga.tafelboem.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * Everything the session engine tells a player's client. Behind an interface so GameTests can use
 * mock players, which have no real network connection.
 */
public interface QuizClient {
	QuizClient NETWORK = new QuizClient() {
		@Override
		public boolean canReceive(ServerPlayer player) {
			return ServerPlayNetworking.canSend(player, QuizS2C.TYPE);
		}

		@Override
		public void send(ServerPlayer player, QuizS2C payload) {
			ServerPlayNetworking.send(player, payload);
		}

		@Override
		public void actionBar(ServerPlayer player, Component message) {
			player.connection.send(new ClientboundSetActionBarTextPacket(message));
		}
	};

	/** False when the player's client doesn't have TafelBoem installed. */
	boolean canReceive(ServerPlayer player);

	void send(ServerPlayer player, QuizS2C payload);

	void actionBar(ServerPlayer player, Component message);
}
