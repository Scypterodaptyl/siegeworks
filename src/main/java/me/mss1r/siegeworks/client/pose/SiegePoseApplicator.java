package me.mss1r.siegeworks.client.pose;

import me.mss1r.siegeworks.client.aim.SiegeAimController;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class SiegePoseApplicator {
    private static final float LADDER_CYCLE_SPEED = 2.0F;
    private static final float LADDER_CYCLE_AMPLITUDE = 0.35F;
    private static final float LADDER_MIN_LEG_CYCLE_SCALE = 0.25F;

    private SiegePoseApplicator() {
    }

    public static void reset(HumanoidModel<?> model) {
        model.head.resetPose();
        model.hat.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
    }

    public static void apply(HumanoidModel<?> model, LivingEntity rider, @Nullable AbstractSiegeEntity siege,
                             ResolvedSiegePose resolved, float age, float headYaw, float headPitch) {
        PoseSnapshot previousPose = resolved.ladderClimbing() && resolved.blendWeight() < 1.0F
                ? PoseSnapshot.capture(model)
                : null;
        SiegePoseProfile profile = resolved.profile();
        SiegePoseDefinition pose = profile.definition();
        SiegeAimBinding aim = profile.aimBinding();
        float modelPitch = resolved.modelPitch() * Mth.DEG_TO_RAD;
        LadderTransform ladderTransform = resolveLadderTransform(rider, age, resolved);
        SiegePoseRotation upperBody = pose.upperBody()
                .plusX(modelPitch * aim.upperBodyPitchScale())
                .plus(ladderTransform.rotation());
        SiegePoseRotation rightArm = pose.rightArm().plusX(
                modelPitch * (aim.rightArmPitchScale() - aim.upperBodyPitchScale())
        );
        SiegePoseRotation leftArm = pose.leftArm().plusX(
                modelPitch * (aim.leftArmPitchScale() - aim.upperBodyPitchScale())
        );
        RigPivot hip = vanillaHip(model);

        applyUpperPart(model.head, upperBody, pose.head(), hip);
        applyUpperPart(model.body, upperBody, pose.body(), hip);
        applyUpperPart(model.rightArm, upperBody, rightArm, hip);
        applyUpperPart(model.leftArm, upperBody, leftArm, hip);
        if (resolved.ladderClimbing()) {
            applyUpperPart(model.rightLeg, upperBody, pose.rightLeg(), hip);
            applyUpperPart(model.leftLeg, upperBody, pose.leftLeg(), hip);
        } else {
            applyVanillaPivotPart(model.rightLeg, pose.rightLeg());
            applyVanillaPivotPart(model.leftLeg, pose.leftLeg());
        }

        applyLadderMotion(model, rider, age, resolved.ladderClimbing(),
                resolved.ladderInclineRadians());
        applyLegMotion(model, siege, profile.legSwingScale());
        applyHead(model, rider, siege, profile.headPolicy(), resolved.aimPitch(), headYaw, headPitch,
                resolved.ladderClimbing(), ladderTransform.yawCorrection());
        if (resolved.ladderClimbing()) {
            model.head.zRot = 0.0F;
        }
        if (previousPose != null) {
            previousPose.blendInto(model, resolved.blendWeight());
        }
        copyHeadToHat(model);
    }

    private static RigPivot vanillaHip(HumanoidModel<?> model) {
        PartPose rightLeg = model.rightLeg.getInitialPose();
        PartPose leftLeg = model.leftLeg.getInitialPose();
        return new RigPivot(
                (rightLeg.x + leftLeg.x) * 0.5F,
                (rightLeg.y + leftLeg.y) * 0.5F,
                (rightLeg.z + leftLeg.z) * 0.5F
        );
    }

    private static void applyUpperPart(ModelPart part, SiegePoseRotation upperBody,
                                       SiegePoseRotation local, RigPivot hip) {
        PartPose vanilla = part.getInitialPose();
        SiegePoseRotation.Point position = upperBody.rotate(
                vanilla.x - hip.x,
                vanilla.y - hip.y,
                vanilla.z - hip.z
        );
        part.setPos(position.x() + hip.x, position.y() + hip.y, position.z() + hip.z);
        upperBody.plus(local).apply(part);
    }

    private static void applyVanillaPivotPart(ModelPart part, SiegePoseRotation rotation) {
        PartPose vanilla = part.getInitialPose();
        part.setPos(vanilla.x, vanilla.y, vanilla.z);
        rotation.apply(part);
    }

    private static void applyLegMotion(HumanoidModel<?> model, @Nullable AbstractSiegeEntity siege, float scale) {
        if (scale == 0.0F || siege == null) {
            return;
        }
        float movement = (float) siege.getVisualHorizontalTravelSpeed();
        float swing = Mth.sin(siege.getWheelRotation() * Mth.DEG_TO_RAD) * scale * movement;
        model.rightLeg.xRot += swing;
        model.leftLeg.xRot -= swing;
    }

    private static void applyLadderMotion(HumanoidModel<?> model, LivingEntity rider, float age,
                                          boolean ladderClimbing, float inclineRadians) {
        if (!ladderClimbing) {
            return;
        }
        float partialTick = Mth.clamp(age - rider.tickCount, 0.0F, 1.0F);
        float verticalPosition = Mth.lerp(partialTick, (float) rider.yo, (float) rider.getY());
        float cycle = -Mth.cos(verticalPosition * LADDER_CYCLE_SPEED) * LADDER_CYCLE_AMPLITUDE;
        model.rightArm.xRot -= cycle;
        model.leftArm.xRot += cycle;
        float clampedIncline = Mth.clamp(inclineRadians, 0.0F, Mth.HALF_PI);
        float verticalFactor = Mth.cos(clampedIncline);
        float legCycleScale = Mth.lerp(
                verticalFactor,
                LADDER_MIN_LEG_CYCLE_SCALE,
                1.0F
        );
        model.rightLeg.xRot -= cycle * legCycleScale;
        model.leftLeg.xRot += cycle * legCycleScale;
    }

    private static LadderTransform resolveLadderTransform(LivingEntity rider, float age,
                                                          ResolvedSiegePose resolved) {
        if (!resolved.ladderClimbing()) {
            return LadderTransform.ZERO;
        }
        float partialTick = Mth.clamp(age - rider.tickCount, 0.0F, 1.0F);
        float bodyYaw = Mth.rotLerp(partialTick, rider.yBodyRotO, rider.yBodyRot);
        float uphillYaw = resolved.ladderUphillYawDegrees();
        float yawCorrection = Mth.wrapDegrees(uphillYaw - bodyYaw) * Mth.DEG_TO_RAD;
        float pitch = Mth.clamp(resolved.ladderInclineRadians(), 0.0F, Mth.HALF_PI);
        return new LadderTransform(pitch, yawCorrection, 0.0F);
    }

    private static void applyHead(HumanoidModel<?> model, LivingEntity rider, @Nullable AbstractSiegeEntity siege,
                                  SiegeHeadPolicy policy, float aimPitch, float headYaw, float headPitch,
                                  boolean ladderClimbing, float poseYawCorrection) {
        if (ladderClimbing) {
            float correctionDegrees = poseYawCorrection * Mth.RAD_TO_DEG;
            float relativeToPose = Mth.wrapDegrees(headYaw - correctionDegrees);
            float constrainedYaw = foldLookYaw(relativeToPose, policy.maxYaw());
            model.head.xRot = Mth.clamp(headPitch, -policy.maxPitch(), policy.maxPitch()) * Mth.DEG_TO_RAD;
            model.head.yRot = constrainedYaw * Mth.DEG_TO_RAD;
            return;
        }
        if (siege != null && policy.allowFreeLook() && rider instanceof Player player
                && SiegeAimController.isFreeLookActive(player, siege)) {
            float relativeYaw = Mth.wrapDegrees(SiegeAimController.getFreeLookYaw(siege) - rider.yBodyRot);
            setHeadRotation(model.head, relativeYaw, SiegeAimController.getFreeLookPitch(siege),
                    policy.maxYaw(), policy.maxPitch());
            return;
        }

        if (policy.mode() == SiegeHeadPolicy.Mode.AIM) {
            model.head.xRot = Mth.clamp(aimPitch, -policy.maxPitch(), policy.maxPitch()) * Mth.DEG_TO_RAD;
            return;
        }
        setHeadRotation(model.head, headYaw, headPitch, policy.maxYaw(), policy.maxPitch());
    }

    private static float foldLookYaw(float yaw, float maxYaw) {
        float wrapped = Mth.wrapDegrees(yaw);
        float magnitude = Math.abs(wrapped);
        if (magnitude <= maxYaw || maxYaw >= 180.0F) {
            return wrapped;
        }
        float remaining = (180.0F - magnitude) / (180.0F - maxYaw);
        return Math.copySign(maxYaw * Mth.clamp(remaining, 0.0F, 1.0F), wrapped);
    }

    private static void setHeadRotation(ModelPart head, float yaw, float pitch,
                                        float maxYaw, float maxPitch) {
        head.xRot = Mth.clamp(pitch, -maxPitch, maxPitch) * Mth.DEG_TO_RAD;
        head.yRot = Mth.clamp(yaw, -maxYaw, maxYaw) * Mth.DEG_TO_RAD;
    }

    private static void copyHeadToHat(HumanoidModel<?> model) {
        model.hat.x = model.head.x;
        model.hat.y = model.head.y;
        model.hat.z = model.head.z;
        model.hat.xRot = model.head.xRot;
        model.hat.yRot = model.head.yRot;
        model.hat.zRot = model.head.zRot;
    }

    private record LadderTransform(float pitch, float yawCorrection, float roll) {
        private static final LadderTransform ZERO = new LadderTransform(0.0F, 0.0F, 0.0F);

        private SiegePoseRotation rotation() {
            return new SiegePoseRotation(pitch, 0.0F, roll);
        }
    }

    private record RigPivot(float x, float y, float z) {
    }

    private record PartTransform(float x, float y, float z, float xRot, float yRot, float zRot) {
        private static PartTransform capture(ModelPart part) {
            return new PartTransform(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }

        private void blendInto(ModelPart part, float weight) {
            part.x = Mth.lerp(weight, x, part.x);
            part.y = Mth.lerp(weight, y, part.y);
            part.z = Mth.lerp(weight, z, part.z);
            part.xRot = Mth.lerp(weight, xRot, part.xRot);
            part.yRot = Mth.lerp(weight, yRot, part.yRot);
            part.zRot = Mth.lerp(weight, zRot, part.zRot);
        }
    }

    private record PoseSnapshot(PartTransform head, PartTransform body,
                                PartTransform rightArm, PartTransform leftArm,
                                PartTransform rightLeg, PartTransform leftLeg) {
        private static PoseSnapshot capture(HumanoidModel<?> model) {
            return new PoseSnapshot(
                    PartTransform.capture(model.head),
                    PartTransform.capture(model.body),
                    PartTransform.capture(model.rightArm),
                    PartTransform.capture(model.leftArm),
                    PartTransform.capture(model.rightLeg),
                    PartTransform.capture(model.leftLeg)
            );
        }

        private void blendInto(HumanoidModel<?> model, float weight) {
            head.blendInto(model.head, weight);
            body.blendInto(model.body, weight);
            rightArm.blendInto(model.rightArm, weight);
            leftArm.blendInto(model.leftArm, weight);
            rightLeg.blendInto(model.rightLeg, weight);
            leftLeg.blendInto(model.leftLeg, weight);
        }
    }
}
