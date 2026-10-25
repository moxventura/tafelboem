package nl.jeeninga.tafelboem.gametest.mixin;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/** How the GameTest server runs the tests: see {@link GameTestHarness}. */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
	@Inject(method = "initServer", at = @At("RETURN"))
	private void tafelboem$neverSave(CallbackInfoReturnable<Boolean> cir) {
		GameTestHarness.neverSave((MinecraftServer) (Object) this);
	}
}
