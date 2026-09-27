package dev.limn.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.limn.client.LimnClient;
import dev.limn.client.LimnRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.common.MinecraftForge;

final class OutlineEvents {
    private OutlineEvents() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(OutlineEvents::onHighlight);
    }

    private static void onHighlight(DrawHighlightEvent.HighlightBlock event) {
        Level level = Minecraft.getInstance().level;
        if (!LimnClient.config().enabled() || level == null) {
            return;
        }
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir(level, pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        Camera camera = event.getInfo();
        Vec3 cam = camera.getPosition();
        VoxelShape shape = state.getShape(level, pos, CollisionContext.of(camera.getEntity()));
        PoseStack poseStack = event.getMatrix();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
        LimnRuntime.draw(poseStack.last(), event.getBuffers().getBuffer(RenderType.lines()), shape, 1.0F);
        poseStack.popPose();
        event.setCanceled(true);
    }
}
