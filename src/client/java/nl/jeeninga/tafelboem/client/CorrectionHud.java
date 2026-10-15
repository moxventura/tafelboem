package nl.jeeninga.tafelboem.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * After a miss the child is free straight away; the correct fact appears big on the HUD, like a
 * title, while the game goes on. It waits a moment so the penalty can be seen first.
 */
public final class CorrectionHud {
	static final int DELAY_TICKS = 20;
	/** 6 s: at 3.5 s it was gone before it could be read in full (Jesper's playthrough of 1.0.1). */
	static final int SHOW_TICKS = 120;
	private static final int FADE_TICKS = 5;
	private static final int LINE = 11;
	private static final int WHITE = 0xFFFFFF;
	private static final int GREEN = 0x6BFF6B;

	private static @Nullable QuizS2C shown;
	private static int age;

	private CorrectionHud() {
	}

	public static void show(QuizS2C feedback) {
		shown = feedback;
		age = 0;
	}

	public static void clear() {
		shown = null;
	}

	/** True while the fact is on screen (not during the delay before it). */
	public static boolean isShowing() {
		return shown != null && age >= DELAY_TICKS;
	}

	static void tick() {
		if (shown != null && ++age > DELAY_TICKS + SHOW_TICKS) {
			shown = null;
		}
	}

	static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		QuizS2C feedback = shown;
		if (feedback == null || age < DELAY_TICKS) {
			return;
		}
		int visible = age - DELAY_TICKS;
		float fade = Math.min(1f, Math.min(visible, SHOW_TICKS - visible) / (float) FADE_TICKS);
		int alpha = Mth.clamp(Math.round(fade * 255), 8, 255) << 24;

		Font font = Minecraft.getInstance().font;
		int centerX = graphics.guiWidth() / 2;
		int maxWidth = graphics.guiWidth() - 20;
		int a = feedback.a();
		int b = feedback.b();

		List<FormattedCharSequence> above = font.split(Component.translatable(reasonKey(feedback.reason())), maxWidth);
		List<FormattedCharSequence> below = new ArrayList<>(font.split(tip(feedback), maxWidth));
		if (a != b) {
			below.addAll(font.split(Component.translatable("tafelboem.screen.same", b, a), maxWidth));
		}

		float scale = graphics.guiHeight() < 300 ? 3.0f : 4.0f;
		int bigHeight = Math.round(font.lineHeight * scale);
		int y = graphics.guiHeight() / 3 - (above.size() * LINE + bigHeight) / 2;

		for (FormattedCharSequence line : above) {
			graphics.centeredText(font, line, centerX, y, alpha | WHITE);
			y += LINE;
		}
		y += 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, y);
		graphics.pose().scale(scale, scale);
		graphics.centeredText(font, a + " × " + b + " = " + (a * b), 0, 0, alpha | GREEN);
		graphics.pose().popMatrix();
		y += bigHeight + 4;
		for (FormattedCharSequence line : below) {
			graphics.centeredText(font, line, centerX, y, alpha | WHITE);
			y += LINE;
		}
	}

	private static String reasonKey(int reason) {
		return switch (reason) {
			case QuizS2C.REASON_WRONG -> "tafelboem.screen.reason.wrong";
			case QuizS2C.REASON_CHICKEN_OUT -> "tafelboem.screen.reason.chicken_out";
			default -> "tafelboem.screen.reason.dont_know";
		};
	}

	private static Component tip(QuizS2C feedback) {
		if (feedback.tipKey().isEmpty()) {
			return Component.empty();
		}
		return Component.translatable(feedback.tipKey(), feedback.tipArgs().toArray());
	}
}
