package nl.jeeninga.tafelboem.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import nl.jeeninga.tafelboem.TafelBoem;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(QuizS2C.TYPE, QuizS2C.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(QuizC2S.TYPE, QuizC2S.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(QuizC2S.TYPE,
				(payload, context) -> TafelBoem.sessions().onClientAction(context.player(), payload));
	}
}
