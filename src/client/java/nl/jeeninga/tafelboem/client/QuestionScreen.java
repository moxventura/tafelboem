package nl.jeeninga.tafelboem.client;

import java.util.List;

import org.jspecify.annotations.Nullable;
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
 * Vanilla buttons only, laid out like Minecraft's own menus: a wide OK under the pad (where
 * "Klaar" always is), greyed out until something is typed, and "?" far away in the corner.
 * With {@code correctionRetype} on, a miss shows the correct fact here and asks to type it once.
 */
public final class QuestionScreen extends Screen {
	private static final int MAX_DIGITS = 3;
	private static final int GAP = 3;
	private static final int OK_GAP = 5;
	private static final int OK_HEIGHT = 20;
	private static final int HEADER = 20;
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

	/** Layout, recomputed in {@link #init()} so everything fits even on small windows. */
	private float bigScale;
	private int key;
	private int top;
	private int padTop;
	private int padWidth;
	private int padHeight;
	private int panelHalfWidth;
	private boolean nameOnOwnLine;

	private @Nullable Button okButton;
	private @Nullable Button dontKnowButton;
	private boolean focusOkNextTick;

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

	/** For the client tests: the OK button. */
	public @Nullable Button okButton() {
		return okButton;
	}

	/** For the client tests: the "?" button, absent while correcting. */
	public @Nullable Button dontKnowButton() {
		return dontKnowButton;
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
		panelHalfWidth = Math.min(width / 2 - 4, 150);

		Component dontKnow = Component.translatable("tafelboem.screen.dont_know");
		int dontKnowWidth = font.width(dontKnow) + 12;
		// The bomb name is centred; "?" sits in the top-right corner unless the panel is too narrow.
		nameOnOwnLine = !correcting && font.width(bombName()) / 2 + dontKnowWidth + 10 > panelHalfWidth;

		// Question: header, big sum, hint. Correction: name, reason, big fact, tip, same, prompt, typed answer.
		int textHeight = correcting
				? 6 * LINE + bigHeight + Math.round(font.lineHeight * 2.0f) + 4
				: HEADER + (nameOnOwnLine ? LINE : 0) + 4 + bigHeight + 4 + LINE + 6;
		key = 24;
		if (textHeight + padHeight(key) + 8 > height) {
			key = 20;
		}
		padWidth = 3 * key + 2 * GAP;
		padHeight = padHeight(key);
		top = Math.max(4, (height - textHeight - padHeight) / 2);
		padTop = top + textHeight;

		int left = (width - padWidth) / 2;
		String[] keys = {"7", "8", "9", "4", "5", "6", "1", "2", "3"};
		for (int i = 0; i < keys.length; i++) {
			String digit = keys[i];
			int x = left + (i % 3) * (key + GAP);
			int y = padTop + (i / 3) * (key + GAP);
			addRenderableWidget(Button.builder(Component.literal(digit), button -> type(digit.charAt(0)))
					.bounds(x, y, key, key).build());
		}

		int zeroRow = padTop + 3 * (key + GAP);
		addRenderableWidget(Button.builder(Component.literal("0"), button -> type('0'))
				.bounds(left, zeroRow, 2 * key + GAP, key).build());
		addRenderableWidget(Button.builder(Component.literal("←"), button -> backspace())
				.bounds(left + 2 * (key + GAP), zeroRow, key, key).build());
		okButton = addRenderableWidget(Button.builder(Component.literal("OK"), button -> submit(QuizC2S.VIA_CLICK))
				.bounds(left, zeroRow + key + OK_GAP, padWidth, OK_HEIGHT).build());
		updateOkButton();

		dontKnowButton = null;
		if (!correcting) {
			int right = width / 2 + panelHalfWidth - 4;
			dontKnowButton = addRenderableWidget(Button.builder(dontKnow, button -> dontKnow(QuizC2S.VIA_CLICK))
					.bounds(right - dontKnowWidth, top, dontKnowWidth, 20).build());
		}
	}

	private static int padHeight(int key) {
		return 4 * key + 3 * GAP + OK_GAP + OK_HEIGHT;
	}

	/** OK is greyed out until something is typed; then it takes the focus, so Enter visibly presses it. */
	private void updateOkButton() {
		if (okButton == null) {
			return;
		}
		boolean wasActive = okButton.active;
		okButton.active = !input.isEmpty() && !waitingForServer;
		if (okButton.active && !wasActive) {
			// A click on a digit focuses that digit after its handler runs, so move the focus next tick.
			focusOkNextTick = true;
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
			dontKnow(QuizC2S.VIA_KEY);
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
				submit(QuizC2S.VIA_KEY);
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
			send(QuizC2S.CHICKEN_OUT, 0, QuizC2S.VIA_KEY);
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
		if (focusOkNextTick && okButton != null && okButton.active) {
			setFocused(okButton);
		}
		focusOkNextTick = false;
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
			updateOkButton();
		}
	}

	private void backspace() {
		if (!waitingForServer && !input.isEmpty()) {
			input.setLength(input.length() - 1);
			updateOkButton();
		}
	}

	private void dontKnow(int via) {
		if (!correcting && !waitingForServer) {
			// The digits already typed tell us afterwards whether "?" was pressed on purpose.
			send(QuizC2S.DONT_KNOW, input.length(), via);
		}
	}

	private void submit(int via) {
		if (waitingForServer || input.isEmpty()) {
			return;
		}
		int value = Integer.parseInt(input.toString());

		if (correcting) {
			if (value == a * b) {
				send(QuizC2S.CORRECTION_DONE, (int) (System.currentTimeMillis() - correctionShownAt), via);
			} else {
				input.setLength(0);
				updateOkButton();
				shakeTicks = 8;
			}
			return;
		}
		send(QuizC2S.ANSWER, value, via);
	}

	private void send(int action, int value, int via) {
		waitingForServer = true;
		waitingSince = System.currentTimeMillis();
		updateOkButton();
		ClientPlayNetworking.send(new QuizC2S(sessionId, action, value, via));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int centerX = width / 2;
		graphics.fill(centerX - panelHalfWidth, top - 4, centerX + panelHalfWidth, padTop + padHeight + 4, PANEL);

		int y = top;
		if (correcting) {
			graphics.centeredText(font, bombName(), centerX, y, YELLOW);
			y += LINE;
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
			// The bomb name shares the header row with "?", or gets its own line on a narrow window.
			int nameY = nameOnOwnLine ? top + HEADER : top + (HEADER - font.lineHeight) / 2;
			graphics.centeredText(font, bombName(), centerX, nameY, YELLOW);
			y = top + HEADER + (nameOnOwnLine ? LINE : 0) + 4;
			String answer = input.isEmpty() ? "?" : input.toString();
			drawBig(graphics, a + " × " + b + " = " + answer, centerX, y, bigScale, WHITE);
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
