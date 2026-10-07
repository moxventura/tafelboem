package nl.jeeninga.tafelboem.client;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import nl.jeeninga.tafelboem.core.BombTier;
import nl.jeeninga.tafelboem.net.QuizC2S;
import nl.jeeninga.tafelboem.net.QuizS2C;

/**
 * The sum, big and readable for an 8-year-old: type with the number keys or click the number pad.
 * After a miss the same screen shows the correct fact, a tip, and asks the child to type it once.
 */
public final class QuestionScreen extends Screen {
	private static final int MAX_DIGITS = 3;
	private static final int BUTTON_SIZE = 20;
	private static final int GAP = 3;
	private static final int PAD_WIDTH = 3 * BUTTON_SIZE + 2 * GAP;
	private static final int PAD_HEIGHT = 4 * BUTTON_SIZE + 3 * GAP;
	private static final int LINE = 11;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int YELLOW = 0xFFFFE04A;
	private static final int GREEN = 0xFF6BFF6B;
	private static final int RED = 0xFFFF6B6B;
	private static final int PANEL = 0xC0101018;
	private static final long CLOSE_WAIT_MILLIS = 5000;

	private final int sessionId;
	private final int a;
	private final int b;
	private final int tier;
	private boolean correcting;
	private int reason;
	private String tipKey = "";
	private List<Integer> tipArgs = List.of();

	private final StringBuilder input = new StringBuilder();
	private boolean waitingForServer;
	private long waitingSince;
	private long correctionShownAt;
	private int shakeTicks;

	/** Vertical layout, recomputed in {@link #init()} so everything fits even on small windows. */
	private float bigScale;
	private int top;
	private int padTop;

	private QuestionScreen(QuizS2C payload) {
		super(Component.translatable("tafelboem.screen.title"));
		this.sessionId = payload.sessionId();
		this.a = payload.a();
		this.b = payload.b();
		this.tier = payload.tier();
	}

	public static QuestionScreen ask(QuizS2C payload) {
		return new QuestionScreen(payload);
	}

	public static QuestionScreen correction(QuizS2C payload) {
		QuestionScreen screen = new QuestionScreen(payload);
		screen.showCorrection(payload);
		return screen;
	}

	public int sessionId() {
		return sessionId;
	}

	public int a() {
		return a;
	}

	public int b() {
		return b;
	}

	public boolean isCorrecting() {
		return correcting;
	}

	void showCorrection(QuizS2C payload) {
		correcting = true;
		reason = payload.reason();
		tipKey = payload.tipKey();
		tipArgs = payload.tipArgs();
		input.setLength(0);
		waitingForServer = false;
		correctionShownAt = System.currentTimeMillis();
		if (minecraft != null) {
			rebuildWidgets();
		}
	}

	@Override
	protected void init() {
		bigScale = height < 300 ? 3.0f : 4.0f;
		int bigHeight = Math.round(font.lineHeight * bigScale);
		// Question: name, big sum, hint. Correction: name, reason, big fact, tip, same, prompt, typed answer.
		int textHeight = correcting ? 6 * LINE + bigHeight + Math.round(font.lineHeight * 2.0f) + 4 : LINE + bigHeight + LINE + 6;
		int contentHeight = textHeight + PAD_HEIGHT;
		top = Math.max(4, (height - contentHeight) / 2);
		padTop = top + textHeight;

		int left = (width - PAD_WIDTH) / 2;
		String[] keys = {"7", "8", "9", "4", "5", "6", "1", "2", "3"};
		for (int i = 0; i < keys.length; i++) {
			String digit = keys[i];
			int x = left + (i % 3) * (BUTTON_SIZE + GAP);
			int y = padTop + (i / 3) * (BUTTON_SIZE + GAP);
			addRenderableWidget(Button.builder(Component.literal(digit), button -> type(digit.charAt(0)))
					.bounds(x, y, BUTTON_SIZE, BUTTON_SIZE).build());
		}

		int lastRow = padTop + 3 * (BUTTON_SIZE + GAP);
		addRenderableWidget(Button.builder(Component.literal("←"), button -> backspace())
				.bounds(left, lastRow, BUTTON_SIZE, BUTTON_SIZE).build());
		addRenderableWidget(Button.builder(Component.literal("0"), button -> type('0'))
				.bounds(left + BUTTON_SIZE + GAP, lastRow, BUTTON_SIZE, BUTTON_SIZE).build());
		addRenderableWidget(Button.builder(Component.literal("OK"), button -> submit())
				.bounds(left + 2 * (BUTTON_SIZE + GAP), lastRow, BUTTON_SIZE, BUTTON_SIZE).build());

		if (!correcting) {
			// Next to the pad instead of below it, so it never falls off a small screen.
			int dontKnowWidth = Math.max(70, font.width(Component.translatable("tafelboem.screen.dont_know")) + 12);
			addRenderableWidget(Button.builder(Component.translatable("tafelboem.screen.dont_know"), button -> dontKnow())
					.bounds(left + PAD_WIDTH + 12, padTop + PAD_HEIGHT - BUTTON_SIZE, dontKnowWidth, BUTTON_SIZE).build());
		}
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		int codepoint = event.codepoint();
		if (codepoint >= '0' && codepoint <= '9') {
			type((char) codepoint);
			return true;
		}
		if (codepoint == '?' && !correcting) {
			dontKnow();
			return true;
		}
		return super.charTyped(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		switch (event.key()) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				backspace();
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				submit();
				return true;
			}
			default -> {
				return super.keyPressed(event);
			}
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// The correction is short and must be typed once; ESC only works while the question is open.
		return !correcting;
	}

	@Override
	public void onClose() {
		if (!correcting && !waitingForServer) {
			send(QuizC2S.CHICKEN_OUT, 0);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public void tick() {
		if (shakeTicks > 0) {
			shakeTicks--;
		}
		if (waitingForServer && System.currentTimeMillis() - waitingSince > CLOSE_WAIT_MILLIS) {
			minecraft.gui.setScreen(null);
		}
	}

	private void type(char digit) {
		if (!waitingForServer && input.length() < MAX_DIGITS) {
			input.append(digit);
		}
	}

	private void backspace() {
		if (!waitingForServer && !input.isEmpty()) {
			input.setLength(input.length() - 1);
		}
	}

	private void dontKnow() {
		if (!correcting && !waitingForServer) {
			send(QuizC2S.DONT_KNOW, 0);
		}
	}

	private void submit() {
		if (waitingForServer || input.isEmpty()) {
			return;
		}
		int value = Integer.parseInt(input.toString());

		if (correcting) {
			if (value == a * b) {
				send(QuizC2S.CORRECTION_DONE, (int) (System.currentTimeMillis() - correctionShownAt));
			} else {
				input.setLength(0);
				shakeTicks = 8;
			}
			return;
		}
		send(QuizC2S.ANSWER, value);
	}

	private void send(int action, int value) {
		waitingForServer = true;
		waitingSince = System.currentTimeMillis();
		ClientPlayNetworking.send(new QuizC2S(sessionId, action, value));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int centerX = width / 2;
		int panelHalfWidth = Math.min(width / 2 - 4, 150);
		graphics.fill(centerX - panelHalfWidth, top - 4, centerX + panelHalfWidth, padTop + PAD_HEIGHT + 4, PANEL);

		int y = top;
		graphics.centeredText(font, bombName(), centerX, y, YELLOW);
		y += LINE;

		if (correcting) {
			graphics.centeredText(font, Component.translatable(reasonKey()), centerX, y, WHITE);
			y += LINE;
			drawBig(graphics, a + " × " + b + " = " + (a * b), centerX, y, bigScale, GREEN);
			y += Math.round(font.lineHeight * bigScale) + 2;
			graphics.centeredText(font, tipText(), centerX, y, WHITE);
			y += LINE;
			if (a != b) {
				graphics.centeredText(font, Component.translatable("tafelboem.screen.same", b, a), centerX, y, WHITE);
			}
			y += LINE;
			int shake = shakeTicks > 0 ? (shakeTicks % 2 == 0 ? 3 : -3) : 0;
			graphics.centeredText(font, Component.translatable("tafelboem.screen.retype", a * b), centerX + shake, y,
					shakeTicks > 0 ? RED : YELLOW);
			y += LINE;
			drawBig(graphics, input.isEmpty() ? "_" : input.toString(), centerX, y, 2.0f, WHITE);
		} else {
			String answer = input.isEmpty() ? "?" : input.toString();
			drawBig(graphics, a + " × " + b + " = " + answer, centerX, y + 2, bigScale, WHITE);
			y += Math.round(font.lineHeight * bigScale) + 4;
			graphics.centeredText(font, Component.translatable("tafelboem.screen.hint"), centerX, y, 0xFFBBBBBB);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void drawBig(GuiGraphicsExtractor graphics, String text, int centerX, int y, float scale, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, y);
		graphics.pose().scale(scale, scale);
		graphics.centeredText(font, text, 0, 0, color);
		graphics.pose().popMatrix();
	}

	private Component bombName() {
		String id = BombTier.byTier(tier).id();
		return Component.translatable("block.tafelboem." + id);
	}

	private String reasonKey() {
		return switch (reason) {
			case QuizS2C.REASON_WRONG -> "tafelboem.screen.reason.wrong";
			case QuizS2C.REASON_CHICKEN_OUT -> "tafelboem.screen.reason.chicken_out";
			default -> "tafelboem.screen.reason.dont_know";
		};
	}

	private Component tipText() {
		if (tipKey.isEmpty()) {
			return Component.empty();
		}
		return Component.translatable(tipKey, tipArgs.toArray());
	}
}
