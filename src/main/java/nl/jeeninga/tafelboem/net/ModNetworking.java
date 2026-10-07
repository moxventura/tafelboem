package nl.jeeninga.tafelboem.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import nl.jeeninga.tafelboem.TafelBoem;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(QuizS2C.TYPE, QuizS2C.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(QuizC2S.TYPE, QuizC2S.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VersionS2C.TYPE, VersionS2C.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(QuizC2S.TYPE,
				(payload, context) -> TafelBoem.sessions().onClientAction(context.player(), payload));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler.player, VersionS2C.TYPE)) {
				sender.sendPacket(new VersionS2C(TafelBoem.version()));
			}
		});
	}
}
