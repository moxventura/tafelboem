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
	private static final int BUTTON_SIZE = 24;
	private static final int GAP = 4;
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
	private Button dontKnowButton;

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

	void showCorrection(QuizS2C payload) {
		correcting = true;
		reason = payload.reason();
		tipKey = payload.tipKey();
		tipArgs = payload.tipArgs();
		input.setLength(0);
		waitingForServer = false;
		correctionShownAt = System.currentTimeMillis();
		if (dontKnowButton != null) {
			dontKnowButton.visible = false;
		}
	}

	@Override
	protected void init() {
		int padWidth = 3 * BUTTON_SIZE + 2 * GAP;
		int left = (width - padWidth) / 2;
		int top = height / 2 + 4;

		String[] keys = {"7", "8", "9", "4", "5", "6", "1", "2", "3"};
		for (int i = 0; i < keys.length; i++) {
			String digit = keys[i];
			int x = left + (i % 3) * (BUTTON_SIZE + GAP);
			int y = top + (i / 3) * (BUTTON_SIZE + GAP);
			addRenderableWidget(Button.builder(Component.literal(digit), button -> type(digit.charAt(0)))
					.bounds(x, y, BUTTON_SIZE, BUTTON_SIZE).build());
		}

		int lastRow = top + 3 * (BUTTON_SIZE + GAP);
		addRenderableWidget(Button.builder(Component.literal("⌫"), button -> backspace())
				.bounds(left, lastRow, BUTTON_SIZE, BUTTON_SIZE).build());
		addRenderableWidget(Button.builder(Component.literal("0"), button -> type('0'))
				.bounds(left + BUTTON_SIZE + GAP, lastRow, BUTTON_SIZE, BUTTON_SIZE).build());
		addRenderableWidget(Button.builder(Component.literal("OK"), button -> submit())
				.bounds(left + 2 * (BUTTON_SIZE + GAP), lastRow, BUTTON_SIZE, BUTTON_SIZE).build());

		dontKnowButton = addRenderableWidget(Button.builder(Component.translatable("tafelboem.screen.dont_know"), button -> dontKnow())
				.bounds(left - 4, lastRow + BUTTON_SIZE + GAP, padWidth + 8, 20).build());
		dontKnowButton.visible = !correcting;
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
		int panelTop = height / 2 - 92;
		graphics.fill(centerX - 130, panelTop, centerX + 130, height / 2 + 4 + 4 * (BUTTON_SIZE + GAP) + 26, PANEL);

		graphics.centeredText(font, bombName(), centerX, panelTop + 6, YELLOW);

		if (correcting) {
			graphics.centeredText(font, Component.translatable(reasonKey()), centerX, panelTop + 20, WHITE);
			drawBig(graphics, a + " × " + b + " = " + (a * b), centerX, panelTop + 34, 3.0f, GREEN);
			graphics.centeredText(font, tipText(), centerX, panelTop + 66, WHITE);
			if (a != b) {
				graphics.centeredText(font, Component.translatable("tafelboem.screen.same", b, a), centerX, panelTop + 78, WHITE);
			}
			int shake = shakeTicks > 0 ? (shakeTicks % 2 == 0 ? 3 : -3) : 0;
			Component prompt = Component.translatable("tafelboem.screen.retype", a * b);
			graphics.centeredText(font, prompt, centerX + shake, height / 2 - 8, shakeTicks > 0 ? RED : YELLOW);
			drawBig(graphics, input.isEmpty() ? "_" : input.toString(), centerX + 90, height / 2 + 30, 3.0f, WHITE);
		} else {
			String answer = input.isEmpty() ? "?" : input.toString();
			drawBig(graphics, a + " × " + b + " = " + answer, centerX, panelTop + 30, 4.0f, WHITE);
			graphics.centeredText(font, Component.translatable("tafelboem.screen.hint"), centerX, height / 2 - 12, 0xFFBBBBBB);
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
