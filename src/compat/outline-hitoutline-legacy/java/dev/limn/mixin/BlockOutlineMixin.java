package dev.limn.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Before the render-state rework (1.21.9), there is no BlockOutlineRenderState: renderHitOutline
 * receives the raw block position and state directly, and the outline shape has to be computed
 * the same way vanilla does, from the block state itself.
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
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "DDD"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;"
                            + "I)V",
                    ordinal = 1))
    private void limn$blockOutline(
            LevelRenderer instance,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            Entity entity,
            double camX,
            double camY,
            double camZ,
            BlockPos pos,
            BlockState blockState,
            int color,
            Operation<Void> original) {
        if (LimnClient.config().enabled()) {
            VoxelShape shape = blockState.getShape(entity.level(), pos, CollisionContext.of(entity));
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
            LimnRuntime.draw(poseStack.last(), vertexConsumer, shape, 1.0F);
            poseStack.popPose();
        } else {
            original.call(instance, poseStack, vertexConsumer, entity, camX, camY, camZ, pos, blockState, color);
        }
    }
}
