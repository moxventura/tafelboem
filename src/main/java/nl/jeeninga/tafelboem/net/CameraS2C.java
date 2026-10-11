package nl.jeeninga.tafelboem.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Server → client: turn the view towards an entity (Graaf Fout) while the question is open, and
 * keep following it for a while after the screen closes, so the kid never misses the yeet.
 *
 * @param ticksAfterScreen how long to keep following once no question screen is open
 * @param kick             the kid just kicked the bomb back: give the camera a small bump
 */
public record CameraS2C(int entityId, int ticksAfterScreen, boolean kick) implements CustomPacketPayload {
	public static final Type<CameraS2C> TYPE = new Type<>(TafelBoem.id("camera_s2c"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CameraS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CameraS2C::entityId,
			ByteBufCodecs.VAR_INT, CameraS2C::ticksAfterScreen,
			ByteBufCodecs.BOOL, CameraS2C::kick,
			CameraS2C::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
