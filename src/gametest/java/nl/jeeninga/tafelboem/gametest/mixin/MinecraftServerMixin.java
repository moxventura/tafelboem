package nl.jeeninga.tafelboem.gametest.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/**
 * The GameTest server stops without saving its world (8 of the 9 s its shutdown took): Gradle
 * deletes that world before the next run anyway. Any other server, like the client GameTest's,
 * stops as usual. Cancelling {@code stopServer} also skips Fabric's SERVER_STOPPING and
 * SERVER_STOPPED events and the mod's own teardown; that is harmless, because the GameTest
 * server's {@code onServerExit} then exits the JVM.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Inject(method = "stopServer", at = @At("HEAD"), cancellable = true)
	private void tafelboem$stopWithoutSaving(CallbackInfo ci) {
		if ((Object) this instanceof GameTestServer) {
			GameTestHarness.LOGGER.info("TafelBoem GameTests: stopping without saving the test world");
			ci.cancel();
		}
	}
}
