package dev.limn.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.1+: outlines are described here through the deferred submit-node rendering model and
 * drawn later, so there is no camera-relative offset to apply ourselves.
 */
@Mixin(LevelRenderer.class)
abstract class BlockOutlineMixin {
    @WrapOperation(
            method = "submitBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                            + "Lnet/minecraft/client/renderer/rendertype/RenderType;"
                            + "Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;"
                            + "IFZ)V",
                    ordinal = 1))
    private void limn$blockOutline(
            LevelRenderer instance,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            RenderType renderType,
            BlockOutlineRenderState state,
            int color,
            float width,
            boolean afterTerrain,
            Operation<Void> original) {
        if (LimnClient.config().enabled()) {
            submitNodeCollector.submitCustomGeometry(poseStack, renderType,
                    (pose, builder) -> LimnRuntime.draw(pose, builder, state.shape(), width));
        } else {
            original.call(instance, poseStack, submitNodeCollector, renderType, state, color, width, afterTerrain);
        }
    }
}
