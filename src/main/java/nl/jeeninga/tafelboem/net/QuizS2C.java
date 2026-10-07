package nl.jeeninga.tafelboem.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Server → client: open, switch or close the question screen.
 *
 * @param mode   {@link #ASK}, {@link #CORRECT} or {@link #CLOSE}
 * @param reason for {@link #CORRECT}: why the correction is shown ({@link #REASON_WRONG}, ...)
 */
public record QuizS2C(int sessionId, int mode, int a, int b, int tier, int reason, String tipKey, List<Integer> tipArgs)
		implements CustomPacketPayload {
	public static final int ASK = 0;
	public static final int CORRECT = 1;
	public static final int CLOSE = 2;

	public static final int REASON_NONE = 0;
	public static final int REASON_WRONG = 1;
	public static final int REASON_DONT_KNOW = 2;
	public static final int REASON_CHICKEN_OUT = 3;

	public static final Type<QuizS2C> TYPE = new Type<>(TafelBoem.id("quiz_s2c"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QuizS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, QuizS2C::sessionId,
			ByteBufCodecs.VAR_INT, QuizS2C::mode,
			ByteBufCodecs.VAR_INT, QuizS2C::a,
			ByteBufCodecs.VAR_INT, QuizS2C::b,
			ByteBufCodecs.VAR_INT, QuizS2C::tier,
			ByteBufCodecs.VAR_INT, QuizS2C::reason,
			ByteBufCodecs.STRING_UTF8, QuizS2C::tipKey,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), QuizS2C::tipArgs,
			QuizS2C::new);

	public static QuizS2C ask(int sessionId, int a, int b, int tier) {
		return new QuizS2C(sessionId, ASK, a, b, tier, REASON_NONE, "", List.of());
	}

	public static QuizS2C close(int sessionId) {
		return new QuizS2C(sessionId, CLOSE, 0, 0, 0, REASON_NONE, "", List.of());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
