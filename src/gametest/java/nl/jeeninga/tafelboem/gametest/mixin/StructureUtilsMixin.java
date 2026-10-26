package nl.jeeninga.tafelboem.gametest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import nl.jeeninga.tafelboem.gametest.GameTestHarness;

/** Clearing a test's padded box skips the blocks that already are what they would become. */
@Mixin(StructureUtils.class)
public abstract class StructureUtilsMixin {
	@Inject(method = "clearBlock", at = @At("HEAD"), cancellable = true)
	private static void tafelboem$skipWhatIsClear(int airIfAboveThisY, BlockPos pos, ServerLevel level, CallbackInfo ci) {
		if (GameTestHarness.alreadyClear(airIfAboveThisY, pos, level)) {
			ci.cancel();
		}
	}
}
