package nl.jeeninga.tafelboem.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import nl.jeeninga.tafelboem.net.CameraS2C;
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

		@Override
		public void track(ServerPlayer player, Entity target, int ticksAfterScreen, boolean kick) {
			if (ServerPlayNetworking.canSend(player, CameraS2C.TYPE)) {
				ServerPlayNetworking.send(player, new CameraS2C(target.getId(), ticksAfterScreen, kick));
			}
		}

		@Override
		public void sound(ServerPlayer player, SoundEvent sound) {
			player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
					player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F, player.getRandom().nextLong()));
		}
	};

	/** False when the player's client doesn't have TafelBoem installed. */
	boolean canReceive(ServerPlayer player);

	void send(ServerPlayer player, QuizS2C payload);

	void actionBar(ServerPlayer player, Component message);

	/** Turn the player's view towards {@code target}; see {@link CameraS2C}. */
	void track(ServerPlayer player, Entity target, int ticksAfterScreen, boolean kick);

	/** A sound only this player hears, such as the toast of a new record. */
	void sound(ServerPlayer player, SoundEvent sound);
}
