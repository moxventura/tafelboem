package nl.jeeninga.tafelboem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import nl.jeeninga.tafelboem.net.QuizS2C;

public class TafelBoemClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(QuizS2C.TYPE, (payload, context) -> handle(context.client(), payload));
	}

	private static void handle(Minecraft client, QuizS2C payload) {
		Screen current = client.gui.screen();
		QuestionScreen question = current instanceof QuestionScreen screen && screen.sessionId() == payload.sessionId() ? screen : null;

		switch (payload.mode()) {
			case QuizS2C.ASK -> client.gui.setScreen(QuestionScreen.ask(payload));
			case QuizS2C.CORRECT -> {
				if (question != null) {
					question.showCorrection(payload);
				} else {
					client.gui.setScreen(QuestionScreen.correction(payload));
				}
			}
			case QuizS2C.CLOSE -> {
				if (question != null) {
					client.gui.setScreen(null);
				}
			}
			default -> {
			}
		}
	}
}
