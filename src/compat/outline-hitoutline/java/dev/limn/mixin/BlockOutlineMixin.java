package dev.limn.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Before 1.21.11, renderHitOutline has no width parameter at all - vanilla draws the outline at
 * a fixed GL line width, so this pack pairs with the width-fixed LimnRuntime, which never calls
 * VertexConsumer.setLineWidth. The camera-relative offset still has to be pushed onto the pose
 * stack by hand, same as the 1.21.11 pack.
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
                            + "Lnet/minecraft/client/renderer/state/BlockOutlineRenderState;"
                            + "I)V",
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
            Operation<Void> original) {
        if (LimnClient.config().enabled()) {
            poseStack.pushPose();
            poseStack.translate(
                    state.pos().getX() - camX,
                    state.pos().getY() - camY,
                    state.pos().getZ() - camZ);
            LimnRuntime.draw(poseStack.last(), vertexConsumer, state.shape(), 1.0F);
            poseStack.popPose();
        } else {
            original.call(instance, poseStack, vertexConsumer, camX, camY, camZ, state, color);
        }
    }
}
