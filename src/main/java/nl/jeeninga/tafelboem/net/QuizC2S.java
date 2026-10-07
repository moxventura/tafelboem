package nl.jeeninga.tafelboem.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Client → server: what the child did on the question screen.
 *
 * @param value the typed answer for {@link #ANSWER}; the retype time in ms for {@link #CORRECTION_DONE}
 */
public record QuizC2S(int sessionId, int action, int value) implements CustomPacketPayload {
	public static final int ANSWER = 0;
	public static final int DONT_KNOW = 1;
	public static final int CHICKEN_OUT = 2;
	public static final int CORRECTION_DONE = 3;

	public static final Type<QuizC2S> TYPE = new Type<>(TafelBoem.id("quiz_c2s"));
	public static final StreamCodec<RegistryFriendlyByteBuf, QuizC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, QuizC2S::sessionId,
			ByteBufCodecs.VAR_INT, QuizC2S::action,
			ByteBufCodecs.VAR_INT, QuizC2S::value,
			QuizC2S::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
