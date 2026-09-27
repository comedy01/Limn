package dev.limn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.limn.config.OutlineConfig;
import dev.limn.outline.OutlinePolicy;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LimnRuntime {
    public static final boolean SUPPORTS_LINE_WIDTH = false;

    private LimnRuntime() {
    }

    public static void draw(
            PoseStack.Pose pose,
            VertexConsumer builder,
            VoxelShape shape,
            float vanillaWidth) {
        OutlineConfig config = LimnClient.config();
        int solid = OutlinePolicy.solidColor(config.color());
        if (!OutlinePolicy.isRainbow(config.mode())) {
            shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                vertex(builder, pose, x1, y1, z1, solid);
                vertex(builder, pose, x2, y2, z2, solid);
            });
            return;
        }
        int alpha = solid >>> 24;
        double seconds = System.nanoTime() * 1.0E-9D;
        double speed = config.rainbowSpeed();
        double spread = config.rainbowSpread();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            int pieces = OutlinePolicy.segmentCount(Math.sqrt(dx * dx + dy * dy + dz * dz));
            double px = x1;
            double py = y1;
            double pz = z1;
            int pieceColor = OutlinePolicy.rainbowArgb(
                    OutlinePolicy.hue(seconds, speed, spread, px, py, pz), alpha);
            for (int i = 1; i <= pieces; i++) {
                double t = (double) i / pieces;
                double nx = x1 + dx * t;
                double ny = y1 + dy * t;
                double nz = z1 + dz * t;
                int nextColor = OutlinePolicy.rainbowArgb(
                        OutlinePolicy.hue(seconds, speed, spread, nx, ny, nz), alpha);
                vertex(builder, pose, px, py, pz, pieceColor);
                vertex(builder, pose, nx, ny, nz, nextColor);
                px = nx;
                py = ny;
                pz = nz;
                pieceColor = nextColor;
            }
        });
    }

    private static void vertex(
            VertexConsumer builder,
            PoseStack.Pose pose,
            double x,
            double y,
            double z,
            int argb) {
        builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24)
                .endVertex();
    }
}
