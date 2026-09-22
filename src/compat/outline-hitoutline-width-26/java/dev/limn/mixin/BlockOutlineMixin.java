package dev.limn.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.1-26.1.2: still draws immediately, same shape as the 1.21.11 pack (renderHitOutline takes
 * the camera position and a per-vertex width), but BlockOutlineRenderState moved one package
 * level deeper (renderer.state.level instead of renderer.state). The deferred submit-node model
 * only starts at 26.2.
 */
@Mixin(LevelRenderer.class)
abstract class BlockOutlineMixin {
    @WrapOperation(
            method = "renderBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                            + "DDD"
                            + "Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;"
                            + "IF)V",
                    ordinal = 1))
    private void limn$blockOutline(
            LevelRenderer instance,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            double camX,
            double camY,
            double camZ,
            BlockOutlineRenderState state,
            int color,
            float width,
            Operation<Void> original) {
        if (LimnClient.config().enabled()) {
            poseStack.pushPose();
            poseStack.translate(
                    state.pos().getX() - camX,
                    state.pos().getY() - camY,
                    state.pos().getZ() - camZ);
            LimnRuntime.draw(poseStack.last(), vertexConsumer, state.shape(), width);
            poseStack.popPose();
        } else {
            original.call(instance, poseStack, vertexConsumer, camX, camY, camZ, state, color, width);
        }
    }
}
