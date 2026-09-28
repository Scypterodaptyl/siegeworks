package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import me.mss1r.siegeworks.entity.projectile.AbstractBoltProjectile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Renders embedded bolts in the target model, matching vanilla's stuck-arrow approach. */
public final class StuckBoltLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final double SEARCH_PADDING = 2.0D;

    private final CrossbowBoltRenderer<AbstractBoltProjectile> boltRenderer;
    private final List<ModelPart> modelRoots;
    private final Map<Integer, ModelAttachment> attachments = new HashMap<>();

    public StuckBoltLayer(EntityRendererProvider.Context context, LivingEntityRenderer<T, M> parent) {
        super(parent);
        this.boltRenderer = new CrossbowBoltRenderer<>(context);
        this.modelRoots = findModelRoots(parent.getModel());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T target,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (modelRoots.isEmpty()) {
            return;
        }

        List<AbstractBoltProjectile> bolts = target.level().getEntitiesOfClass(
                AbstractBoltProjectile.class,
                target.getBoundingBox().inflate(SEARCH_PADDING),
                bolt -> bolt.isAttachedTo(target));
        for (AbstractBoltProjectile bolt : bolts) {
            renderBolt(poseStack, buffer, packedLight, target, bolt);
        }
    }

    private void renderBolt(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                            LivingEntity target, AbstractBoltProjectile bolt) {
        ModelAttachment attachment = attachments.get(bolt.getId());
        if (attachment == null || attachment.targetId() != target.getId()) {
            attachment = createAttachment(poseStack, target, bolt);
            if (attachment == null) {
                return;
            }
            if (attachments.size() >= 256) {
                attachments.clear();
            }
            attachments.put(bolt.getId(), attachment);
        }

        AttachmentPose pose = findAttachmentPose(poseStack, attachment);
        if (pose == null) {
            attachments.remove(bolt.getId());
            return;
        }

        poseStack.pushPose();
        poseStack.last().pose().set(pose.pose());
        poseStack.last().normal().set(pose.normal());
        poseStack.translate(
                attachment.localPosition().x,
                attachment.localPosition().y,
                attachment.localPosition().z);
        boltRenderer.renderStuck(
                bolt,
                attachment.localDirection().x,
                attachment.localDirection().y,
                attachment.localDirection().z,
                poseStack,
                buffer,
                packedLight);
        poseStack.popPose();
    }

    private ModelAttachment createAttachment(PoseStack poseStack, LivingEntity target,
                                             AbstractBoltProjectile bolt) {
        Vec3 storedPosition = bolt.getTargetLocalPosition();
        Vec3 storedDirection = bolt.getTargetLocalDirection().normalize();
        Vector3f modelPosition = new Vector3f(
                (float) storedPosition.x,
                1.501F - (float) storedPosition.y,
                -(float) storedPosition.z);
        Vector3f modelDirection = new Vector3f(
                (float) storedDirection.x,
                -(float) storedDirection.y,
                -(float) storedDirection.z).normalize();

        Matrix4f basePose = new Matrix4f(poseStack.last().pose());
        Vector3f renderedPosition = basePose.transformPosition(new Vector3f(modelPosition));
        Vector3f renderedDirection = basePose.transformDirection(new Vector3f(modelDirection)).normalize();
        AttachmentCandidate[] closest = new AttachmentCandidate[1];

        for (int rootIndex = 0; rootIndex < modelRoots.size(); rootIndex++) {
            int selectedRoot = rootIndex;
            modelRoots.get(rootIndex).visit(poseStack, (pose, path, cubeIndex, cube) -> {
                Matrix4f inverse = new Matrix4f(pose.pose()).invert();
                Vector3f partPosition = inverse.transformPosition(new Vector3f(renderedPosition));
                Vector3f nearest = new Vector3f(
                        Mth.clamp(partPosition.x * 16.0F, cube.minX, cube.maxX) / 16.0F,
                        Mth.clamp(partPosition.y * 16.0F, cube.minY, cube.maxY) / 16.0F,
                        Mth.clamp(partPosition.z * 16.0F, cube.minZ, cube.maxZ) / 16.0F);
                Vector3f nearestRendered = pose.pose().transformPosition(new Vector3f(nearest));
                float distance = nearestRendered.distanceSquared(renderedPosition);
                if (closest[0] == null || distance < closest[0].distanceSquared()) {
                    Vector3f partDirection = inverse.transformDirection(new Vector3f(renderedDirection)).normalize();
                    closest[0] = new AttachmentCandidate(
                            distance, selectedRoot, path, cubeIndex, nearest, partDirection);
                }
            });
        }

        AttachmentCandidate candidate = closest[0];
        return candidate == null ? null : new ModelAttachment(
                target.getId(),
                candidate.rootIndex(),
                candidate.path(),
                candidate.cubeIndex(),
                candidate.localPosition(),
                candidate.localDirection());
    }

    private AttachmentPose findAttachmentPose(PoseStack poseStack, ModelAttachment attachment) {
        if (attachment.rootIndex() < 0 || attachment.rootIndex() >= modelRoots.size()) {
            return null;
        }

        AttachmentPose[] selected = new AttachmentPose[1];
        modelRoots.get(attachment.rootIndex()).visit(poseStack, (pose, path, cubeIndex, cube) -> {
            if (selected[0] == null && cubeIndex == attachment.cubeIndex() && path.equals(attachment.path())) {
                selected[0] = new AttachmentPose(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()));
            }
        });
        return selected[0];
    }

    private static List<ModelPart> findModelRoots(EntityModel<?> model) {
        if (model instanceof HierarchicalModel<?> hierarchicalModel) {
            return List.of(hierarchicalModel.root());
        }
        if (model instanceof ListModel<?> listModel) {
            List<ModelPart> roots = new ArrayList<>();
            listModel.parts().forEach(roots::add);
            return roots;
        }

        Set<ModelPart> parts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !ModelPart.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(model);
                    if (value instanceof ModelPart part) {
                        parts.add(part);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // A model with inaccessible fields simply does not get stuck-bolt visuals.
                }
            }
        }

        Set<ModelPart> descendants = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart part : parts) {
            part.getAllParts().skip(1).forEach(descendants::add);
        }
        parts.removeAll(descendants);
        return List.copyOf(parts);
    }

    private record AttachmentCandidate(float distanceSquared, int rootIndex, String path, int cubeIndex,
                                       Vector3f localPosition, Vector3f localDirection) {
    }

    private record ModelAttachment(int targetId, int rootIndex, String path, int cubeIndex,
                                   Vector3f localPosition, Vector3f localDirection) {
    }

    private record AttachmentPose(Matrix4f pose, Matrix3f normal) {
    }
}
