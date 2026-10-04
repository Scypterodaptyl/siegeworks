package me.mss1r.siegeworks.client.harness;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.rope.RopeRenderer;
import me.mss1r.siegeworks.client.rope.SiegeRopeAnchors;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.model.GeoModel;

import java.util.Optional;

public final class TowShaftRenderer {
    private static final ResourceLocation SHAFT_TEXTURE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/tow_shaft.png");

    private static final float SHAFT_WIDTH = 1.5F / 16.0F;

    private TowShaftRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, GeoModel<?> model,
                              Vec3 mountOffset, LivingEntity mount, int packedLight) {
        render(poseStack, bufferSource, model, "shaft_left", "shaft_right", mountOffset,
                180.0F, mount, packedLight);
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, GeoModel<?> model,
                              String leftAnchor, String rightAnchor, Vec3 mountOffset,
                              float modelTurnDegrees, LivingEntity mount, int packedLight) {
        Optional<Vec3> left = SiegeRopeAnchors.modelPosition(model, leftAnchor);
        Optional<Vec3> right = SiegeRopeAnchors.modelPosition(model, rightAnchor);
        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        MountHarness harness = MountHarness.forMount(mount);
        float scale = MountHarness.mountScale(mount);
        Vec3 mountLeft = harnessAnchor(harness.leftAnchor().scale(scale), mountOffset,
                modelTurnDegrees);
        Vec3 mountRight = harnessAnchor(harness.rightAnchor().scale(scale), mountOffset,
                modelTurnDegrees);

        poseStack.pushPose();
        draw(poseStack, bufferSource, left.get(), mountLeft, packedLight);
        draw(poseStack, bufferSource, right.get(), mountRight, packedLight);
        poseStack.popPose();
    }

    private static void draw(PoseStack poseStack, MultiBufferSource bufferSource,
                             Vec3 start, Vec3 end, int packedLight) {
        RopeRenderer.render(poseStack, bufferSource, SHAFT_TEXTURE, start, end,
                start.distanceTo(end), packedLight, SHAFT_WIDTH);
    }

    private static Vec3 harnessAnchor(Vec3 harnessLocal, Vec3 mountOffset,
                                      float modelTurnDegrees) {
        double longitudinalDirection = Math.cos(Math.toRadians(modelTurnDegrees));
        return new Vec3(
                -longitudinalDirection * mountOffset.x - harnessLocal.x,
                harnessLocal.y + mountOffset.y,
                longitudinalDirection * (harnessLocal.z + mountOffset.z));
    }
}
