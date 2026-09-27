package dev.limn.showcase.mixin;

import dev.limn.showcase.Showcase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.limn.client.LimnRuntime", remap = false)
abstract class LimnRuntimeClockMixin {
    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Ljava/lang/System;nanoTime()J"))
    private static long limnShowcase$nanoTime() {
        return Showcase.nanos();
    }
}
