package nl.jeeninga.tafelboem.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import nl.jeeninga.tafelboem.TafelBoem;

/**
 * Sent on join so a PC with an outdated TafelBoem can tell the child to run the update script.
 */
public record VersionS2C(String version) implements CustomPacketPayload {
	public static final Type<VersionS2C> TYPE = new Type<>(TafelBoem.id("version_s2c"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VersionS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, VersionS2C::version,
			VersionS2C::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
