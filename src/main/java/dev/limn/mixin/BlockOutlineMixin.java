package dev.limn.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import net.minecraft.client.renderer.LevelRenderer;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
abstract class BlockOutlineMixin {
    //? if >=26.1 {
    // 26.1 introduced the deferred submit-node rendering model: outlines are described here
    // and drawn later, so there is no camera-relative offset to apply ourselves.
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
    //?} else {
    /*// Pre-26.1 draws outlines immediately. renderHitOutline receives the camera position
    // separately from the pose stack, so the block's camera-relative offset has to be pushed
    // onto the stack by hand before handing off to the shared drawing code.
    @WrapOperation(
            method = "renderBlockOutline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
                            + "DDD"
                            + "Lnet/minecraft/client/renderer/state/BlockOutlineRenderState;"
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
    *///?}
}
