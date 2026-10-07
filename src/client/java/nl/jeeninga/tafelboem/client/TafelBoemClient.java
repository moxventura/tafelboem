package nl.jeeninga.tafelboem.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import nl.jeeninga.tafelboem.TafelBoem;
import nl.jeeninga.tafelboem.net.QuizS2C;
import nl.jeeninga.tafelboem.net.VersionS2C;
import nl.jeeninga.tafelboem.registry.ModEntities;

public class TafelBoemClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.GRAAF_FOUT, GraafFoutRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWN_BOMB, ThrownItemRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(QuizS2C.TYPE, (payload, context) -> handle(context.client(), payload));
		ClientPlayNetworking.registerGlobalReceiver(VersionS2C.TYPE, (payload, context) -> {
			String mine = TafelBoem.version();
			if (!mine.equals(payload.version())) {
				context.player().sendSystemMessage(Component.translatable("tafelboem.message.version_mismatch", mine, payload.version())
						.withStyle(ChatFormatting.GOLD));
			}
		});
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
