package nl.jeeninga.tafelboem.gametest.mixin;

import java.util.List;

import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/** How the GameTest server runs the tests: see {@link GameTestHarness}. */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
	@Inject(method = "initServer", at = @At("RETURN"))
	private void tafelboem$neverSave(CallbackInfoReturnable<Boolean> cir) {
		GameTestHarness.neverSave((MinecraftServer) (Object) this);
	}

	@Inject(method = "evaluateTestsToRun", at = @At("RETURN"), cancellable = true)
	private void tafelboem$chosenGroupInOneBatch(ServerLevel level, CallbackInfoReturnable<List<GameTestBatch>> cir) {
		cir.setReturnValue(GameTestHarness.oneBatch(GameTestHarness.keepChosenGroup(cir.getReturnValue(), level)));
	}

	@ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true)
	private static int tafelboem$repeat(int repeatCount) {
		return GameTestHarness.repeat(repeatCount);
	}
}
